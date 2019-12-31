package io.github.giantnuker.fabric.loadcatcher;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;

public enum InitializationKind {
    /**
     * When all ModInitializer/ClientModInitializer classes are instantiated. This happens ONCE.
     */
    INSTANTIATION,
    /**
     * When {@link ModInitializer#onInitialize()} is called. This happens FOR EVERY MOD.
     */
    COMMON_ENTRYPOINT,
    /**
     * When {@link ClientModInitializer#onInitializeClient()} is called. This happens FOR EVERY MOD.
     */
    CLIENT_ENTRYPOINT,

    /**
     * After ALL common initializers are called.
     * The {@link EntrypointHandler#onModInitializationThrowed(Throwable, InitializationKind)} callback will not be called
     * if the exception was determined to not be rethrown (by returning true with COMMON_ENTRYPOINT)
     */
    ALL_COMMON_ENTRIES,
    /**
     * After ALL client initializers are called.
     * The {@link EntrypointHandler#onModInitializationThrowed(Throwable, InitializationKind)} callback will not be called
     * if the exception was determined to not be rethrown (by returning true with CLIENT_ENTRYPOINT)
     */
    ALL_CLIENT_ENTRIES

}
