package org.ephemeris.bot.signal.plugins;

import java.io.IOException;

import org.ephemeris.bot.signal.components.Signal;
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage;

import org.ephemeris.bot.signal.PrefixedPlugin;
import de.thoffbauer.signal4j.store.Group;
import de.thoffbauer.signal4j.store.User;

public class Echo extends PrefixedPlugin {

	public Echo() {
		super("!echo");
	}

	@Override
	public void onMessage(User user, Group group, SignalServiceDataMessage message) throws IOException {
		Signal.getInstance().sendMessage(user, group, stripPrefix(message.getBody().get()));
	}

}
