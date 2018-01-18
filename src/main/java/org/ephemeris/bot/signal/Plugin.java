package org.ephemeris.bot.signal;

import de.thoffbauer.signal4j.store.Group;
import de.thoffbauer.signal4j.store.User;
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage;

import java.io.IOException;

public abstract class Plugin {
    protected SignalBot signalBot;

    protected boolean isEnabled = true;

    public Plugin(SignalBot bot) {
        signalBot = bot;
    }

    public String getName() {
        return this.getClass().getSimpleName().toLowerCase();
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public abstract boolean onMessage(User sender, Group group, SignalServiceDataMessage message) throws IOException;
}
