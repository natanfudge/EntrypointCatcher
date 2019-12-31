package io.github.giantnuker.fabric.loadcatcher;

import net.fabricmc.loader.api.ModContainer;

public interface EntrypointHandler {
    /**
     * Called before modloading begins
     */
    void onBegin();

    /**
     * Called when a mod container is iterated over
     * @param container The mod
     */
    void processContainer(ModContainer container);

    /**
     * Called after <code>FabricLoader.prepareModInit</code> is called
     * @param throwable Any exception that may have been thrown
     */
    void onModsInstanced(Throwable throwable);

    /**
     * Called before a mod is initialized
     * @param container The mod
     */
    void onModInitializeBegin(ModContainer container);

    /**
     * Called after a mod is initialized
     * @param container
     * @param throwable Any exception that may have been thrown
     */
    void onModInitializedEnd(ModContainer container, Throwable throwable);

    /**
     * Called at the beginning of client mod initialization
     */
    void onClientInitializerBegin();
    /**
     * Called at the beginning of common mod initialization
     */
    void onCommonInitializerBegin();

    /**
     * Called after modloading ends
     */
    void onEnd();
}
