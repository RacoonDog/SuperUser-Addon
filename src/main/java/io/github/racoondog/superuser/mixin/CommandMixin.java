package io.github.racoondog.superuser.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Ensure command chat feedback is sent from the render thread to prevent CMEs
 */
@Mixin(value = Command.class, remap = false)
public class CommandMixin {
    @WrapMethod(method = "info(Lnet/minecraft/text/Text;)V")
    private void wrap(Text message, Operation<Void> original) {
        if (MinecraftClient.getInstance().isOnThread()) {
            original.call(message);
        } else {
            MinecraftClient.getInstance().submit(() -> original.call(message));
        }
    }

    @WrapMethod(method = {"info(Ljava/lang/String;[Ljava/lang/Object;)V", "warning", "error"})
    private void wrap(String message, Object[] args, Operation<Void> original) {
        if (MinecraftClient.getInstance().isOnThread()) {
            original.call(message, args);
        } else {
            MinecraftClient.getInstance().submit(() -> original.call(message, args));
        }
    }
}
