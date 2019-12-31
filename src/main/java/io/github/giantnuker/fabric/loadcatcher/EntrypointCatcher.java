package io.github.giantnuker.fabric.loadcatcher;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.SimpleMessage;

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
	private static Runnable runner = null;
	private static String prevModId = null;

	/**
	 * Overwrite the entrypoint handler completely. You should know what you are doing...
	 * @param modId The id of the mod redirecting the handler
	 * @param handler The new handler
	 */
	public static void redirectEntrypointHandler(String modId, Runnable handler) {
		if (runner != null) {
			LOGGER.error(String.format("%s is re-overwriting the entrypoint handler! It was already overwritten by %s. Expect serious issues!", modId, prevModId));
		} else {
			LOGGER.warn(String.format("%s is overwriting the entrypoint handler! Issues may occur.", modId));
		}
		runner = handler;
		prevModId = modId;
	}
}
