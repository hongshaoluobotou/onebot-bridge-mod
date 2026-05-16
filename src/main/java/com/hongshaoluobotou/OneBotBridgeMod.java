package com.hongshaoluobotou;

import com.hongshaoluobotou.event.PlayerAdvancementEvents;
import com.hongshaoluobotou.event.PlayerCommandEvents;
import com.hongshaoluobotou.util.AdvancementUtil;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OneBotBridgeMod implements ModInitializer {
	public static final String MOD_ID = "onebot-bridge-mod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static OneBotThread oneBotThread;
	private static MinecraftServer serverInstance;

	@Override
	public void onInitialize() {
		LOGGER.info("OneBot Bridge Mod 正在初始化...");

		Config config = Config.get();

		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			serverInstance = server;
			createOneBotThread(config);
		});

		// 玩家加入->QQ群
		ServerPlayerEvents.JOIN.register((player) -> {
			sendToQQ(player.getName().getString() + " 加入了游戏");
		});

		// 玩家离开->QQ群
		ServerPlayerEvents.LEAVE.register((player) -> {
			sendToQQ(player.getName().getString() + " 离开了游戏");
		});

		// 玩家重生->QQ群
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			sendToQQ(oldPlayer.getName().getString() + " 重生好了");
		});

		// 玩家死亡->QQ群
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
			if (entity instanceof Player player) {
				String msg = damageSource.getLocalizedDeathMessage(entity).getString();
				sendToQQ(msg);
			}
		});

		// 获得成就->QQ群
		PlayerAdvancementEvents.GRANT.register((player, advancement) -> {
			String id = advancement.id().toString();
			if (id.startsWith("minecraft:recipes/")) {
				return;
			}
			String title = AdvancementUtil.getTitle(advancement);
			String desc = AdvancementUtil.getDescription(advancement);
			String msg = player.getName().getString() + " 取得了进度 [" + title + "] " + desc;
			sendToQQ(msg);
		});

		// 玩家指令->控制台
		PlayerCommandEvents.EXECUTE.register((source, commandString) -> {
			if (source.getPlayer() == null) return;
			String msg = source.getPlayer().getName().getString() + " issued server command: /" + commandString;
			LOGGER.info(msg);
		});

		// 玩家聊天消息->QQ群
		ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
			String msg = "<" + sender.getName().getString() + "> " + message.decoratedContent().getString();
			sendToQQ(msg);
		});

		// 重载命令：断开旧连接 -> 重载配置 -> 重建连接
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(Commands.literal(MOD_ID)
					.then(Commands.literal("reload")
//							.requires(source -> source.hasPermission(Permissions.COMMANDS_ADMIN))
							.executes(context -> {
								if (oneBotThread != null) {
									oneBotThread.stop();
									oneBotThread = null;
								}
								Config.get().reload();
								createOneBotThread(Config.get());
								context.getSource().sendSuccess(
										() -> Component.literal("[OneBot Bridge] 配置文件已重载，OneBot 客户端已重建"),
										true
								);
								return 1;
							})
					)
			);
		});

		// 服务器关闭
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			if (oneBotThread != null) {
				oneBotThread.stop();
				oneBotThread = null;
			}
			serverInstance = null;
			LOGGER.info("[OneBot Bridge] 服务器已关闭，OneBot 连接已清理");
		});
	}

	private static void createOneBotThread(Config config) {
		if (serverInstance == null) {
			LOGGER.error("无法创建 OneBotThread：服务器实例为空");
			return;
		}
		oneBotThread = new OneBotThread(
				serverInstance,
				config.url,
				config.token,
				config.groupId,
				false,
				true,
				5,
				10
		);
	}

	/**
	 * 安全发送消息到QQ，避免reload期间的NPE
	 */
	private static void sendToQQ(String message) {
		if (oneBotThread != null) {
			oneBotThread.addPendingSendMessage(message);
		}
	}
}