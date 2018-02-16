package org.ephemeris.bot.signal;

import de.thoffbauer.signal4j.listener.ConversationListener;
import de.thoffbauer.signal4j.store.Group;
import de.thoffbauer.signal4j.store.User;
import org.ephemeris.bot.signal.components.Config;
import org.ephemeris.bot.signal.components.Signal;
import org.ephemeris.bot.signal.components.SignalConnection;
import org.ephemeris.bot.signal.components.SignalConsole;
import org.ephemeris.bot.signal.plugins.CoreCommands;
import org.ephemeris.bot.signal.plugins.Ignore;
import org.ephemeris.bot.signal.plugins.Lookup;
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage;
import org.whispersystems.signalservice.api.messages.multidevice.ReadMessage;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

public class SignalBot implements ConversationListener {
    private long startedAt = System.currentTimeMillis();
    private static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss z");

    protected static class VersionInfo {
        public String version = "unknown",
                revision = "unknown",
                buildDate = "unknown",
                javaVersion = "unknown";
    }

    protected static final VersionInfo versionInfo = new VersionInfo();

    protected List<Plugin> pluginsList = new ArrayList<>(); // plugins in order in which they were added (registered)
    protected Map<String, Plugin> pluginsMap = new HashMap<>(); // plugins accessible by name

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

    protected static void readMainfest() throws IOException {
        Pattern jarred = Pattern.compile("jar:file:.+\\.jar!.+");

        Class self = SignalBot.class;
        String path = self.getResource(self.getSimpleName() + ".class").toString();

        if (!jarred.matcher(path).matches()) {
            log("Failed to locate `META-INF/MANIFEST.MF`: probably not running from a jar file.");
            log("Current class is loaded from: " + path);
            return;
        }

        String mfPath = path.substring(0, path.lastIndexOf("!") + 1) + "/META-INF/MANIFEST.MF";
        // String pkg = self.getPackage().getName().replaceAll("\\.", "/");
        Manifest manifest = new Manifest(new URL(mfPath).openStream());

        log("Reading manifest from: " + mfPath);

        Attributes attr = manifest.getMainAttributes();

        if (attr == null) {
            log("Failed to read build metadata from embedded `META-INF/MANIFEST.MF`.");
            return;
        }

        versionInfo.version = self.getPackage().getImplementationVersion();
        versionInfo.revision = attr.getValue("Git-Commit");
        versionInfo.buildDate = attr.getValue("Build-Date");
        versionInfo.javaVersion = attr.getValue("Java-Version");
    }

    public static void main(String[] args) throws Exception {
        boolean offline = args.length != 0 && args[0].equals("offline");

        SignalBot bot = new SignalBot();

        log("Running on " + System.getProperty("os.name"));
        log("System encoding is: " + System.getProperty("file.encoding"));

        readMainfest();

        log("Version    : " + versionInfo.version);
        log("Revision   : " + versionInfo.revision);
        log("Build date : " + versionInfo.buildDate);
        log("Built with : " + versionInfo.javaVersion);

        bot.RegisterPlugins(new Ignore(bot), new CoreCommands(bot), new Lookup(bot));

        bot.start(offline);
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
