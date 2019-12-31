package io.github.giantnuker.fabric.loadcatcher;


import com.google.common.collect.Lists;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.entrypoint.minecraft.hooks.EntrypointUtils;
import net.fabricmc.loader.metadata.EntrypointMetadata;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.SimpleMessage;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntrypointCatcher {
	public static final Logger LOGGER = LogManager.getLogger("Entrypoint Catcher", new MessageFactory() {
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
	private static EntrypointRunnalbe runner = null;
	private static String prevModId = null;

	/**
	 * Overwrite the entrypoint handler completely. You should know what you are doing...
	 * @param modId The id of the mod redirecting the handler
	 * @param handler The new handler
	 */
	public static void redirectEntrypointHandler(String modId, EntrypointRunnalbe handler) {
		if (runner != null) {
			LOGGER.error(String.format("%s is re-overwriting the entrypoint handler! It was already overwritten by %s. Expect serious issues!", modId, prevModId));
		} else {
			LOGGER.warn(String.format("%s is overwriting the entrypoint handler! Issues may occur.", modId));
		}
		runner = handler;
		prevModId = modId;
	}
	public static void runEntrypointRedirection(File newRunDir, Object gameInstance) {
		if (runner != null) {
			LOGGER.warn("Running Mod Entrypoint Redirector from " + prevModId);
			runner.run(newRunDir, gameInstance);
		} else {
			LOGGER.info("Running Mod Entrypoints Normally");
			NormalOperations.runNormally(newRunDir, gameInstance);
		}
		LOGGER.info("Mod Initialization complete");
	}
	public static class NormalOperations {
		public static void runNormally(File newRunDir, Object gameInstance) {

			runBegins();
			Map<String, ModContainer> mainToContainer = new HashMap<>();
			Map<String, ModContainer> clientToContainer = new HashMap<>();
			runContainerChecks(mainToContainer, clientToContainer);
			instantiateMods(newRunDir, gameInstance);
			LOGGER.info("Running Entrypoints");
			runEntrypoints(mainToContainer, clientToContainer);
			runEnd();
		}

		public static void runBegins() {
			for (EntrypointHandler handler : getHandlerEntrypoints()) {
				handler.onBegin();
			}
		}

		public static void runContainerChecks(Map<String, ModContainer> mainToContainer, Map<String, ModContainer> clientToContainer) {
			ArrayList<ModContainer> containers = Lists.newArrayList(FabricLoader.INSTANCE.getAllMods());
			for (ModContainer container : containers) {
				for (EntrypointHandler handler : getHandlerEntrypoints()) {
					handler.processContainer(container);
				}
				if (container instanceof net.fabricmc.loader.ModContainer) {
					net.fabricmc.loader.ModContainer mod = (net.fabricmc.loader.ModContainer) container;
					for (EntrypointMetadata entrypoint : mod.getInfo().getEntrypoints("main")) {
						mainToContainer.put(entrypoint.getValue(), container);
					}
					for (EntrypointMetadata entrypoint : mod.getInfo().getEntrypoints("client")) {
						clientToContainer.put(entrypoint.getValue(), container);
					}
				}
			}
			LOGGER.info(String.format("Found %d common entrypoints and %d client entrypoints", mainToContainer.size(), clientToContainer.size()));
		}
		public static void runCommonBegins() {
			for (EntrypointHandler handler : getHandlerEntrypoints()) {
				handler.onCommonInitializerBegin();
			}
		}
		public static void runClientBegins() {
			for (EntrypointHandler handler : getHandlerEntrypoints()) {
				handler.onClientInitializerBegin();
			}
		}
		public static void runEntrypoints(Map<String, ModContainer> mainToContainer, Map<String, ModContainer> clientToContainer) {
			runCommonBegins();
			EntrypointUtils.invoke("main", ModInitializer.class, it -> {
				String id = it.getClass().getName();
				for (EntrypointHandler handler : getHandlerEntrypoints()) {
					handler.onModInitializeBegin(mainToContainer.get(id), EnvType.SERVER);
				}
				it.onInitialize();
				Throwable error = null;
				try {
					it.onInitialize();
				} catch (Throwable e) {
					error = e;
				}
				for (EntrypointHandler handler : getHandlerEntrypoints()) {
					handler.onModInitializeEnd(mainToContainer.get(id), EnvType.SERVER, error);
				}
			});
			runClientBegins();
			EntrypointUtils.invoke("client", ClientModInitializer.class, it -> {
				String id = it.getClass().getName();
				for (EntrypointHandler handler : getHandlerEntrypoints()) {
					handler.onModInitializeBegin(clientToContainer.get(id), EnvType.CLIENT);
				}
				it.onInitializeClient();
				Throwable error = null;
				try {
					it.onInitializeClient();
				} catch (Throwable e) {
					error = e;
				}
				for (EntrypointHandler handler : getHandlerEntrypoints()) {
					handler.onModInitializeEnd(clientToContainer.get(id), EnvType.CLIENT, error);
				}
			});
		}
		public static void runEnd() {
			for (EntrypointHandler handler : getHandlerEntrypoints()) {
				handler.onEnd();
			}
		}
	}

	public static void instantiateMods(File newRunDir, Object gameInstance) {
		Throwable error = null;
		try {
			FabricLoader.INSTANCE.prepareModInit(newRunDir, gameInstance);
		} catch (Throwable e) {
			error = e;
		}
		for (EntrypointHandler handler : getHandlerEntrypoints()) {
			handler.onModsInstanced(error);
		}
	}

	private static List<EntrypointHandler> entrypointHandlers = null;

	/**
	 * @return The entrypoint handlers
	 */
	public static List<EntrypointHandler> getHandlerEntrypoints() {
		if (entrypointHandlers == null) {
			entrypointHandlers = FabricLoader.INSTANCE.getEntrypoints("entry_handler", EntrypointHandler.class);
		}
		return entrypointHandlers;
	}
}
