package com.hongshaoluobotou;

import cn.evole.onebot.client.OneBotClient;
import cn.evole.onebot.client.core.BotConfig;
import cn.evole.onebot.sdk.util.MsgUtils;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class OneBotThread {
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(r -> new Thread(r, "OneBot-Bridge-Sender"));
    private final Logger LOGGER = LoggerFactory.getLogger(OneBotThread.class);

    private OneBotClient oneBotClient;
    private final long groupId;

    private static final int MAX_QUEUE_SIZE = 100;
    private final LinkedBlockingDeque<String> queuedMessageSend = new LinkedBlockingDeque<>(MAX_QUEUE_SIZE);

    private volatile boolean running = true;

    // ========== 智能模式参数 ==========
    // 密度统计窗口
    private static final long DENSITY_WINDOW_MS = 1000;
    private static final int BATCH_THRESHOLD = 3;
    // 探测与聚合窗口
    private static final long PROBE_MS = 100;   // 频率探测：100ms 内有无新消息
    private static final long BATCH_MS = 300;   // 聚合窗口：高频时最多聚合 300ms

    private final AtomicLong lastMsgTime = new AtomicLong(0);
    private final AtomicInteger msgDensity = new AtomicInteger(0);

    public OneBotThread(MinecraftServer server, String url, String token, long groupId,
                        boolean mirai, boolean reconnect, int reconnectInterval, int reconnectMaxTimes) {
        this.groupId = groupId;

        executorService.submit(() -> {
            try {
                oneBotClient = OneBotClient.create(new BotConfig(url, token, 0L, mirai, reconnect, reconnectInterval, reconnectMaxTimes));
                oneBotClient.open().registerEvents(new EventListeners(server, oneBotClient, groupId));

                while (running && !Thread.currentThread().isInterrupted()) {
                    try {
                        // ========== 阶段1: 阻塞等待第一条消息 ==========
                        String first = queuedMessageSend.take();
                        long receiveTime = System.currentTimeMillis();
                        updateDensity(receiveTime);

                        // ========== 阶段2: 模式探测 ==========
                        // 等待 PROBE_MS 看是否有更多消息到来
                        List<String> batch = new ArrayList<>();
                        batch.add(first);

                        String next = queuedMessageSend.poll(PROBE_MS, TimeUnit.MILLISECONDS);

                        if (next == null) {
                            // ========== 模式A: 即时直通 ==========
                            // 探测期内无后续，直接单发，零聚合延迟
                            sendToGroup(first);

                        } else {
                            // ========== 模式B: 聚合+压缩 ==========
                            batch.add(next);

                            // 高频判断：密度超过阈值 或 队列仍有积压
                            boolean highFreq = msgDensity.get() >= BATCH_THRESHOLD || !queuedMessageSend.isEmpty();

                            if (highFreq) {
                                long deadline = receiveTime + BATCH_MS;
                                while (System.currentTimeMillis() < deadline) {
                                    long remain = deadline - System.currentTimeMillis();
                                    if (remain <= 0) break;
                                    String more = queuedMessageSend.poll(remain, TimeUnit.MILLISECONDS);
                                    if (more == null) break;
                                    batch.add(more);
                                }
                            }

                            // 压缩连续重复消息并发送
                            List<String> compressed = compressConsecutive(batch);
                            if (!compressed.isEmpty()) {
                                sendToGroup(String.join("\n", compressed));
                            }
                        }

                        // 密度衰减：2秒无消息则重置
                        if (System.currentTimeMillis() - lastMsgTime.get() > 2000) {
                            msgDensity.set(0);
                        }

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        LOGGER.error("消息发送循环异常", e);
                        try { Thread.sleep(1000); }
                        catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("OneBot 客户端启动失败", e);
            } finally {
                // 退出前flush队列剩余消息
                List<String> remaining = new ArrayList<>();
                queuedMessageSend.drainTo(remaining);
                for (String text : remaining) {
                    sendToGroup(text);
                }
                LOGGER.info("OneBot 发送线程已退出");
            }
        });
    }

    private void updateDensity(long now) {
        long last = lastMsgTime.getAndSet(now);
        if (now - last < DENSITY_WINDOW_MS) {
            msgDensity.incrementAndGet();
        } else {
            msgDensity.set(1);
        }
    }

    /**
     * 添加消息到发送队列
     */
    public boolean addPendingSendMessage(String message) {
        if (!running || executorService.isShutdown()) {
            LOGGER.warn("OneBot 发送服务已停止，消息丢弃: {}", message);
            return false;
        }
        boolean success = queuedMessageSend.offer(message);
        if (!success) {
            LOGGER.warn("消息队列已满，消息丢弃: {}", message);
        }
        return success;
    }

    /**
     * 压缩连续重复消息
     * 例如: ["A", "A", "B", "A"] -> ["2 × A", "B", "A"]
     * 仅合并相邻且完全相同的行，避免误判
     */
    private List<String> compressConsecutive(List<String> messages) {
        if (messages == null || messages.isEmpty()) return List.of();

        List<String> result = new ArrayList<>();
        String current = messages.get(0);
        int count = 1;

        for (int i = 1; i < messages.size(); i++) {
            if (messages.get(i).equals(current)) {
                count++;
            } else {
                result.add(count > 1 ? count + " × " + current : current);
                current = messages.get(i);
                count = 1;
            }
        }
        result.add(count > 1 ? count + " × " + current : current);
        return result;
    }

    private void sendToGroup(String text) {
        if (text == null || text.isEmpty()) return;
        try {
            oneBotClient.getBot().sendGroupMsg(groupId, MsgUtils.builder().text(text).build(), true);
        } catch (Exception e) {
            LOGGER.error("发送消息到群 {} 失败: {}", groupId, e.getMessage());
        }
    }

    public void stop() {
        if (!running) return;
        running = false;

        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        if (oneBotClient != null) {
            try {
                oneBotClient.close();
            } catch (Exception e) {
                LOGGER.error("关闭 OneBot 连接异常", e);
            }
            oneBotClient = null;
        }

        LOGGER.info("[OneBot Bridge] OneBot 客户端已关闭");
    }
}