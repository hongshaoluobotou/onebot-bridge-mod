package com.hongshaoluobotou.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerPlayer;

/**
 * 玩家获得成就时触发的事件
 */
public final class PlayerAdvancementEvents {
    private PlayerAdvancementEvents() {}

    /**
     * 当玩家首次完成（授予）一个成就时触发
     */
    public static final Event<Grant> GRANT = EventFactory.createArrayBacked(Grant.class, callbacks -> (player, advancement) -> {
        for (Grant callback : callbacks) {
            callback.onGrant(player, advancement);
        }
    });

    @FunctionalInterface
    public interface Grant {
        /**
         * 当玩家获得成就时调用
         *
         * @param player 获得成就的玩家
         * @param advancement 获得的成就（AdvancementHolder）
         */
        void onGrant(ServerPlayer player, AdvancementHolder advancement);
    }
}