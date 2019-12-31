package io.github.giantnuker.fabric.loadcatcher;


import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.google.common.collect.Lists;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.SimpleMessage;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.entrypoint.minecraft.hooks.EntrypointUtils;
import net.fabricmc.loader.metadata.EntrypointMetadata;


public class EntrypointCatcher {
    private static final Logger LOGGER = LogManager.getLogger("Entrypoint Catcher", new MessageFactory() {
        @Override
        public Message newMessage(Object message) {
            return new SimpleMessage("[Entrypoint Catcher] " + message);
        }

        @Override
        public Message newMessage(String message) {
            return new SimpleMessage("[Entrypoint Catcher] " + message);
        }

        @Override
        public Message newMessage(String message, Object... params) {
            return new SimpleMessage("[Entrypoint Catcher] " + message);
        }
    });

    /**
     * For internal use only
     */
    public static void runEntrypointRedirection(File newRunDir, Object gameInstance) {
        if (modEntrypointReplacement != null) {
            LOGGER.warn("Running Mod Entrypoint Redirector from " + replacingMod);
            modEntrypointReplacement.run(newRunDir, gameInstance);
        } else {
            LOGGER.info("Running Mod Entrypoints Normally");
            LoaderClientReplacement.run(newRunDir, gameInstance);
        }
        LOGGER.info("Mod Initialization complete");    }

    private static EntrypointRunnable modEntrypointReplacement = null;
    private static String replacingMod = null;

    /**
     * Overwrite the entrypoint handler completely. You should know what you are doing...
     * @param modId The id of the mod redirecting the handler
     * @param handler The new handler
     */
    public static void redirectEntrypointHandler(String modId, EntrypointRunnable handler) {
        if (modEntrypointReplacement != null) {
            LOGGER.error(String.format("%s is re-overwriting the entrypoint handler! It was already overwritten by %s. Expect serious issues!", modId, replacingMod));
        } else {
            LOGGER.warn(String.format("%s is overwriting the entrypoint handler! Issues may occur.", modId));
        }
        modEntrypointReplacement = handler;
        replacingMod = modId;
    }

    public static class LoaderClientReplacement {

        private static final List<EntrypointHandler> entrypointHandlers = FabricLoader.getInstance().getEntrypoints("entry_handler", EntrypointHandler.class);

        public static void run(File newRunDir, Object gameInstance) {
            runBeforeAllCallbacks();
            Map<String, ModContainer> mainToMod = new HashMap<>();
            Map<String, ModContainer> clientToMod = new HashMap<>();
            preprocessMods(mainToMod, clientToMod);
            instantiateMods(newRunDir, gameInstance);
            LOGGER.info("Running Entrypoints");
            runEntrypoints(mainToMod, clientToMod);
            runAfterAllCallbacks();
            LOGGER.info("Mod Initialization complete");
        }

        private static void runBeforeAllCallbacks() {
            for (EntrypointHandler handler : entrypointHandlers) {
                handler.beforeModsLoaded();
            }
        }

        private static void preprocessMods(Map<String, ModContainer> mainToMod, Map<String, ModContainer> clientToMod) {
            ArrayList<ModContainer> containers = Lists.newArrayList(FabricLoader.getInstance().getAllMods());
            for (ModContainer container : containers) {
                for (EntrypointHandler handler : entrypointHandlers) {
                    handler.proprocessMod(container);
                }
                if (container instanceof net.fabricmc.loader.ModContainer) {
                    net.fabricmc.loader.ModContainer mod = (net.fabricmc.loader.ModContainer) container;
                    for (EntrypointMetadata entrypoint : mod.getInfo().getEntrypoints("main")) {
                        mainToMod.put(entrypoint.getValue(), container);
                    }
                    for (EntrypointMetadata entrypoint : mod.getInfo().getEntrypoints("client")) {
                        clientToMod.put(entrypoint.getValue(), container);
                    }
                }
            }
            LOGGER.info(String.format("Found %d common entrypoints and %d client entrypoints", mainToMod.size(), clientToMod.size()));
        }

        private static void runPreEntrypointsCallbacks(EntrypointKind kind) {
            for (EntrypointHandler handler : entrypointHandlers) {
                handler.beforeModsEntrypoints(kind);
            }
        }


        private static void runEntrypoints(Map<String, ModContainer> mainToMod, Map<String, ModContainer> clientToMod) {
            invokeEntrypoints("main", ModInitializer.class, ModInitializer::onInitialize, EntrypointKind.COMMON, mainToMod);
            invokeEntrypoints("client", ClientModInitializer.class, ClientModInitializer::onInitializeClient,
                            EntrypointKind.CLIENT, clientToMod);

        }

        private static <T> void invokeEntrypoints(String name, Class<T> type, Consumer<? super T> invoker, EntrypointKind entrypointKind,
                                                  Map<String, ModContainer> entrypointModGetter) {
            runPreEntrypointsCallbacks(entrypointKind);
            try {
                EntrypointUtils.invoke(name, type, modInitializer -> {
                    //TODO: this won't work when using method references or other fancy language adapter stuff
                    String id = modInitializer.getClass().getName();

                    for (EntrypointHandler handler : entrypointHandlers) {
                        handler.beforeModInitEntrypoint(entrypointModGetter.get(id), entrypointKind);
                    }
                    try {
                        invoker.accept(modInitializer);
                    } catch (Throwable e) {
                        if (handleInitializationError(e,
                                        entrypointKind == EntrypointKind.CLIENT ? InitializationKind.CLIENT_ENTRYPOINT : InitializationKind.COMMON_ENTRYPOINT)) {
                            throw e;
                        }
                    }
                    for (EntrypointHandler handler : entrypointHandlers) {
                        handler.afterModInitEntrypoint(entrypointModGetter.get(id), entrypointKind);
                    }
                });
            } catch (Throwable e) {
                if (handleInitializationError(e,
                                entrypointKind == EntrypointKind.CLIENT ? InitializationKind.ALL_CLIENT_ENTRIES : InitializationKind.ALL_COMMON_ENTRIES)) {
                    throw e;
                }
            }

        }

        private static void runAfterAllCallbacks() {
            for (EntrypointHandler handler : entrypointHandlers) {
                handler.afterModsLoaded();
            }
        }

        private static void instantiateMods(File newRunDir, Object gameInstance) {
            try {
                net.fabricmc.loader.FabricLoader.INSTANCE.prepareModInit(newRunDir, gameInstance);
            } catch (Throwable e) {
                if (handleInitializationError(e, InitializationKind.INSTANTIATION)) throw e;
            }
            for (EntrypointHandler handler : entrypointHandlers) {
                handler.afterModsInstantiated();
            }
        }

        /**
         * Returns true if the error should be rethrowed
         */
        private static boolean handleInitializationError(Throwable e, InitializationKind kind) {
            boolean rethrow = true;
            for (EntrypointHandler handler : entrypointHandlers) {
                if (handler.onModInitializationThrowed(e, kind)) rethrow = false;
            }
            return rethrow;
        }
    }


}
