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

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.entrypoint.minecraft.hooks.EntrypointUtils;
import net.fabricmc.loader.metadata.EntrypointMetadata;

// Notes:
// Logger initializer - why is this needed?
// EnvType --> EntrypointKind - they're both on the client
// RedirectEntrypoint - not needed
// getHandlerEntrypoints - not needed
// Stuff in the entrypoint API renamed
// Internal loader API - is that needed?
// Error handling - need to rethrow in cases no one wants to stop the exceptions
// Default methods in interface - to not need a huge interface
// The mod initializer class is not always the mod initializer values (initializer can be a method reference)

/**
 * INTERNAL CLASS DO NOT USE
 */
public class EntrypointCatcher {
    private static final Logger LOGGER = LogManager.getLogger("Entrypoint Catcher");

    public static void runEntrypointRedirection(File newRunDir, Object gameInstance) {
        LoaderClientReplacement.run(newRunDir, gameInstance);
    }

    private static class LoaderClientReplacement {
        private static final List<EntrypointHandler> entrypointHandlers = FabricLoader.getInstance().getEntrypoints("entry_handler", EntrypointHandler.class);

        private static void run(File newRunDir, Object gameInstance) {
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
