package org.ephemeris.bot.signal.plugins;

import de.thoffbauer.signal4j.store.Group;
import de.thoffbauer.signal4j.store.User;
import org.ephemeris.bot.signal.Plugin;
import org.ephemeris.bot.signal.SignalBot;
import org.ephemeris.bot.signal.components.Signal;
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage;

import java.time.Instant;
import java.util.Date;

// This plugin should be the first in chain
public class Ignore extends Plugin {
    public Ignore(SignalBot bot) {
        super(bot);
    }

    @Override
    public boolean onMessage(User sender, Group group, SignalServiceDataMessage message) {
        if (sender.getNumber().equals(Signal.getInstance().getPhoneNumber())) {
            return true; // ignore messages from self (true means that message was processed, so stop further processing)
        }

        long mts = message.getTimestamp();

        if (mts < signalBot.getStartedAt()) {
            signalBot.log("ignoring message from [%s] <%s> sent at %s",
                    sender.getName(), sender.getNumber(),
                    signalBot.getFormattedDate(Date.from(Instant.ofEpochMilli(mts))));
            return true; // ignore messages sent before this bot was started (true means that message was processed, so stop further processing)
        }

        return false;
    }
}
