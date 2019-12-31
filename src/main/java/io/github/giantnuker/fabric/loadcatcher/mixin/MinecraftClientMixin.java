package io.github.giantnuker.fabric.loadcatcher.mixin;

import io.github.giantnuker.fabric.loadcatcher.EntrypointCatcher;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.File;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/fabricmc/loader/entrypoint/minecraft/hooks/EntrypointClient;start(Ljava/io/File;Ljava/lang/Object;)V", remap = false))
    private void stopFabricInit(File runDir, Object gameInstance) {
        EntrypointCatcher.runEntrypointRedirection(runDir, gameInstance);
    }
}
