package com.hongshaoluobotou;

import cn.evole.onebot.client.OneBotClient;
import cn.evole.onebot.client.annotations.SubscribeEvent;
import cn.evole.onebot.client.interfaces.Listener;
import cn.evole.onebot.sdk.entity.ArrayMsg;
import cn.evole.onebot.sdk.enums.MsgType;
import cn.evole.onebot.sdk.event.message.GroupMessageEvent;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class EventListeners implements Listener {

    public static final Logger LOGGER = LoggerFactory.getLogger(EventListeners.class);
    public final MinecraftServer server;
    private final OneBotClient oneBotClient;
    private final long groupId;
    private static final Gson GSON = new Gson();

    EventListeners(MinecraftServer server, OneBotClient onebot, long groupId) {
        this.server = server;
        this.oneBotClient = onebot;
        this.groupId = groupId;
    }

    @SubscribeEvent
    public void onGroup(GroupMessageEvent event) {
        Config config = Config.get();
        if (event.getGroupId() != config.groupId) {
            return;
        }

        String senderName = event.getSender().getCard();
        if (senderName == null || senderName.isEmpty()) {
            senderName = event.getSender().getNickname();
        }
        if (senderName == null || senderName.isEmpty()) {
            senderName = "未知用户";
        }

        // 获取消息段：优先用 SDK 解析的 arrayMsg，失败则手动解析 message 字符串
        List<ArrayMsg> arrayMsgs = event.getArrayMsg();
        if (arrayMsgs == null || arrayMsgs.isEmpty()) {
            String msgStr = event.getMessage();
            if (msgStr != null && msgStr.trim().startsWith("[")) {
                try {
                    arrayMsgs = GSON.fromJson(msgStr, new TypeToken<List<ArrayMsg>>() {}.getType());
                } catch (Exception e) {
                    LOGGER.warn("无法解析 message 为 ArrayMsg: {}", msgStr);
                }
            }
        }

        String formattedMessage;
        if (arrayMsgs != null && !arrayMsgs.isEmpty()) {
            formattedMessage = formatArrayMsg(arrayMsgs);
        } else {
            formattedMessage = event.getMessage();
        }

        if (formattedMessage == null || formattedMessage.isEmpty()) {
            return;
        }

        String text = "<" + senderName + "> " + formattedMessage;

        if (server != null) {
            server.execute(() -> {
                server.getPlayerList().broadcastSystemMessage(
                        Component.literal(text),
                        false
                );
            });
        }
    }

    private String formatArrayMsg(List<ArrayMsg> arrayMsgs) {
        StringBuilder sb = new StringBuilder();

        for (ArrayMsg msg : arrayMsgs) {
            MsgType type = msg.getType();
            Map<String, String> data = msg.getData();
            if (type == null) continue;

            switch (type) {
                case text -> appendIfPresent(sb, data, "text");
                case at -> {
                    String qq = data.get("qq");
                    if ("all".equals(qq)) {
                        sb.append("@全体成员 ");
                    } else {
                        sb.append("@").append(qq != null ? qq : "?").append(" ");
                    }
                }
                case face -> {
                    String id = data.get("id");
                    sb.append("[表情").append(id != null ? ":" + id : "").append("]");
                }
                case image -> {
                    String summary = data.get("summary");
                    String subType = data.get("sub_type");
                    if ("1".equals(subType) || (summary != null && summary.contains("表情"))) {
                        sb.append("[动画表情]");
                    } else {
                        sb.append("[图片]");
                    }
                }
                case mface, marketface -> sb.append("[大表情]");
                case basketball -> sb.append("[篮球表情]");
                case record -> sb.append("[语音]");
                case video -> sb.append("[视频]");
                case rps, new_rps -> sb.append("[猜拳]");
                case dice, new_dice -> sb.append("[骰子]");
                case shake -> sb.append("[窗口抖动]");
                case anonymous -> sb.append("[匿名消息]");
                case share -> {
                    String title = data.get("title");
                    sb.append("[链接").append(title != null ? ":" + title : "").append("]");
                }
                case contact -> {
                    String contactType = data.get("type");
                    sb.append("[推荐").append("group".equals(contactType) ? "群" : "好友").append("]");
                }
                case location -> {
                    String address = data.get("address");
                    sb.append("[位置").append(address != null ? ":" + address : "").append("]");
                }
                case music -> {
                    String title = data.get("title");
                    sb.append("[音乐").append(title != null ? ":" + title : "").append("]");
                }
                case reply -> sb.append("[回复]");
                case redbag -> sb.append("[红包]");
                case poke -> sb.append("[戳一戳]");
                case gift -> sb.append("[礼物]");
                case forward -> sb.append("[合并转发]");
                case markdown -> sb.append("[Markdown]");
                case keyboard -> sb.append("[键盘]");
                case node -> sb.append("[转发节点]");
                case xml -> sb.append("[XML消息]");
                case json -> sb.append("[JSON消息]");
                case cardimage -> sb.append("[卡片图片]");
                case tts -> sb.append("[语音转文字]");
                case longmsg -> sb.append("[长消息]");
                case unknown -> sb.append("[未知消息]");
            }
        }

        return sb.toString().trim();
    }

    private void appendIfPresent(StringBuilder sb, Map<String, String> data, String key) {
        String value = data.get(key);
        if (value != null) sb.append(value);
    }
}