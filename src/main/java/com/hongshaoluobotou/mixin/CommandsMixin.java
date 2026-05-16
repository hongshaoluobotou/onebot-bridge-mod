package com.hongshaoluobotou.mixin;

import com.hongshaoluobotou.event.PlayerCommandEvents;
import com.mojang.brigadier.ParseResults;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Commands.class)
public class CommandsMixin {

    /**
     * 注入到 performCommand 方法开头
     * 拦截所有命令执行（包括 performPrefixedCommand 调用的）
     */
    @Inject(
            method = "performCommand",
            at = @At("HEAD")
    )
    private void onExecute(
            ParseResults<CommandSourceStack> command,
            String commandString,
            CallbackInfo ci
    ) {
        CommandSourceStack source = command.getContext().getSource();
        PlayerCommandEvents.EXECUTE.invoker().onExecute(source, commandString);
    }
}