package com.hongshaoluobotou.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

/**
 * 玩家执行命令时触发的事件
 */
public final class PlayerCommandEvents {
    private PlayerCommandEvents() {}

    /**
     * 当任何命令被执行时触发（包括玩家、命令方块、控制台等）
     * 如果 source 是玩家，可以通过 source.getPlayer() 获取
     */
    public static final Event<Execute> EXECUTE = EventFactory.createArrayBacked(Execute.class, callbacks -> (source, commandString) -> {
        for (Execute callback : callbacks) {
            callback.onExecute(source, commandString);
        }
    });

    @FunctionalInterface
    public interface Execute {
        /**
         * 当命令被执行时调用
         *
         * @param source 命令执行源
         * @param commandString 完整的命令字符串（不含前导 /）
         */
        void onExecute(CommandSourceStack source, String commandString);
    }
}