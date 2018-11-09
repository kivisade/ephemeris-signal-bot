package org.ephemeris.bot.signal;

import de.thoffbauer.signal4j.listener.ConversationListener;
import de.thoffbauer.signal4j.store.Group;
import de.thoffbauer.signal4j.store.User;
import org.ephemeris.bot.signal.components.*;
import org.ephemeris.bot.signal.plugins.CoreCommands;
import org.ephemeris.bot.signal.plugins.Ignore;
import org.ephemeris.bot.signal.plugins.Lookup;
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage;
import org.whispersystems.signalservice.api.messages.multidevice.ReadMessage;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;

public class SignalBot implements ConversationListener {
    private long startedAt = System.currentTimeMillis();
    private static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss z");

    protected List<Plugin> pluginsList = new ArrayList<>(); // plugins in order in which they were added (registered)
    protected Map<String, Plugin> pluginsMap = new HashMap<>(); // plugins accessible by name


    public final VersionInfo versionInfo;

    public SignalBot() {
        VersionInfo vi = null;
        try {
            vi = new VersionInfo(SignalBot.class); // might fail, e.g. if not running from a jar file
        } catch (Exception ex) {
            log(ex.toString());
        } finally {
            versionInfo = vi != null ? vi : new VersionInfo(); // if "try" block failed, initialize with empty VersionInfo()
        }
    }

    public static void main(String[] args) throws Exception {
        SignalBot bot = new SignalBot();

        log("Running on " + System.getProperty("os.name"));
        log("System encoding is: " + System.getProperty("file.encoding"));

        log("Version    : " + bot.versionInfo.getVersion());
        log("Revision   : " + bot.versionInfo.getRevision());
        log("Build date : " + bot.versionInfo.getBuildDate());
        log("Built with : " + bot.versionInfo.getJavaVersion());

        bot.RegisterPlugins(new Ignore(bot), new CoreCommands(bot), new Lookup(bot));

        boolean offline = args.length != 0 && args[0].equals("offline");

        bot.start(offline);
    }

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

        Signal signal = Signal.getInstance();
        signal.addConversationListener(this);

        while (true) {
            try {
                signal.pull(60 * 1000);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private SignalBot RegisterPlugins(Plugin... plugins) throws Exception {
        for (Plugin plugin : plugins) {
            String name = plugin.getName();
            if (pluginsMap.containsKey(name)) {
                throw new Exception(String.format("Plugin '%s' is already registered.", name));
            }
            pluginsList.add(plugin);
            pluginsMap.put(name, plugin);
            log("Registered plugin %s", name);
        }
        return this;
    }

    public boolean hasPlugin(String name) {
        return pluginsMap.containsKey(name);
    }

    public Plugin getPlugin(String name) {
        return pluginsMap.get(name);
    }

    @Override
    public void onMessage(User sender, SignalServiceDataMessage message, Group group) {
        for (Plugin plugin : pluginsList) {
            if (!plugin.isEnabled()) continue;
            try {
                if (plugin.onMessage(sender, group, message)) { // onMessage() returns true if this message was processed by this plugin
                    log("%s: %s --> %s", sender.getNumber(), message.getBody().or("no body").replace("\n", "\\n"),
                            plugin.getName());
                    break;
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

    public long getStartedAt() {
        return startedAt;
    }

    public String getFormattedDate(Date date) {
        return dateFormatter.format(date);
    }

    public static void log(final String s, final Object... objects) {
        System.out.printf("%s | %s\n", dateFormatter.format(Date.from(Instant.now())), String.format(s, objects));
    }
}
