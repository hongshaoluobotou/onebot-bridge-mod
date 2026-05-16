package com.hongshaoluobotou.mixin;

import com.hongshaoluobotou.event.PlayerAdvancementEvents;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(PlayerAdvancements.class)
public class PlayerAdvancementsMixin {

    @Shadow
    private ServerPlayer player;

    /**
     * 注入到 award() 方法中，在成就首次完成时触发事件
     *
     * 目标代码位置：
     * if (!wasDone && progress.isDone()) {
     *     holder.value().rewards().grant(this.player);
     *     holder.value().display().ifPresent(display -> { ... });
     *     <-- 在这里注入
     * }
     */
    @Inject(
            method = "award",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/advancements/AdvancementHolder;value()Lnet/minecraft/advancements/Advancement;",
                    ordinal = 1  // 第二个 value() 调用（在 rewards().grant 之后）
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void onGrant(
            AdvancementHolder holder,
            String criterion,
            CallbackInfoReturnable<Boolean> cir,
            boolean result,
            AdvancementProgress progress,
            boolean wasDone
    ) {
        // 只在成就首次完成时触发（wasDone 为 false 且现在 isDone）
        if (!wasDone && progress.isDone()) {
            PlayerAdvancementEvents.GRANT.invoker().onGrant(this.player, holder);
        }
    }
}