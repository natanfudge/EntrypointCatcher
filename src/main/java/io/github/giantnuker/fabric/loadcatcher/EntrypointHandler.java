package io.github.giantnuker.fabric.loadcatcher;

import java.io.File;

import org.checkerframework.checker.nullness.qual.Nullable;

import net.fabricmc.loader.api.ModContainer;

public interface EntrypointHandler {
    /**
     * Called before modloading begins
     */
    default void beforeModsLoaded() {
    }

    /**
     * Called before mod initialization starts to retrieve mod metadata
     */
    default void proprocessMod(ModContainer mod) {
    }

    /**
     * Called after {@link net.fabricmc.loader.FabricLoader#prepareModInit(File, Object)} is called
     */
    default void afterModsInstantiated() {
    }

    /**
     * @return TRUE to prevent the throwable from being rethrowed, FALSE to let it be rethrowed
     */
    default boolean onModInitializationThrowed(Throwable throwable, InitializationKind initializationKind) {
        return false;
    }

    /**
     * Called before a mod is initialized
     * @param mod Might be null in the case the initializer doesn't use a normal class reference for the initializer
     */
    default void beforeModInitEntrypoint(@Nullable ModContainer mod, EntrypointKind entrypointKind) {
    }

    /**
     * Called after a mod is initialized
     * @param mod Might be null in the case the initializer doesn't use a normal class reference for the initializer
     */
    default void afterModInitEntrypoint(@Nullable ModContainer mod, EntrypointKind entrypointKind) {
    }

    /**
     * Called TWICE -
     * ONCE before ALL mods are initialized on the client, and ONCE before ALL mods are initialized in the common init.
     */
    default void beforeModsEntrypoints(EntrypointKind entrypointKind) {
    }

    /**
     * Called after modloading ends
     */
    default void afterModsLoaded() {
    }
}
