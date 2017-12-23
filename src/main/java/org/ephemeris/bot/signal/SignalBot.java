package org.ephemeris.bot.signal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.ephemeris.bot.signal.components.Config;
import org.ephemeris.bot.signal.components.Signal;
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage;
import org.whispersystems.signalservice.api.messages.multidevice.ReadMessage;

import org.ephemeris.bot.signal.components.SignalConnection;
import org.ephemeris.bot.signal.components.SignalConsole;
import de.thoffbauer.signal4j.listener.ConversationListener;
import de.thoffbauer.signal4j.store.Group;
import de.thoffbauer.signal4j.store.User;

public class SignalBot implements ConversationListener {
    private void start(boolean offline) throws IOException, Exception {
        if (offline) {
            Signal.setInstance(new SignalConsole());
        } else {
            Signal.setInstance(new SignalConnection());
        }
        Config.load();

        Config config = Config.getInstance();

        if (!config.validate()) {
            throw new Exception("Invalid configuration, cannot start.");
        }

        for (Plugin plugin : Plugin.PLUGINS) {
            plugin.setEnabled(config.isEnabled(plugin));
        }

        Signal signal = Signal.getInstance();
        signal.addConversationListener(this);

        System.out.println("Running");
        System.out.println("System encoding is: " + System.getProperty("file.encoding"));

        while (true) {
            try {
                signal.pull(60 * 1000);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onMessage(User sender, SignalServiceDataMessage message, Group group) {
        ArrayList<String> executed = new ArrayList<>();
        for (Plugin plugin : Plugin.PLUGINS) {
            try {
                if (plugin.isEnabled() && plugin.accepts(sender, group, message)) {
                    plugin.onMessage(sender, group, message);
                    executed.add(plugin.getName());
                }
            } catch (Exception e) {
                try {
                    Signal.getInstance().sendMessage(sender, group,
                            SignalServiceDataMessage.newBuilder().withBody("Internal Error!"));
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                e.printStackTrace();
            }
        }
        if (executed.size() > 0) {
            System.out.println(sender.getNumber() + ": " + message.getBody().or("no body").replace("\n", "\\n")
                    + " forwarded to " + String.join(", ", executed));
        }
    }

    public static void main(String[] args) throws IOException, Exception {
        boolean offline = args.length != 0 && args[0].equals("offline");
        new SignalBot().start(offline);
    }

    @Override
    public void onContactUpdate(User contact) {
    }

    @Override
    public void onGroupUpdate(User sender, Group group) {
    }

    @Override
    public void onReadUpdate(List<ReadMessage> readList) {
    }
}
