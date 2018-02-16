package org.ephemeris.bot.signal.plugins;

import de.thoffbauer.signal4j.store.Group;
import de.thoffbauer.signal4j.store.User;
import org.ephemeris.bot.signal.Plugin;
import org.ephemeris.bot.signal.SignalBot;
import org.ephemeris.bot.signal.api.Method;
import org.ephemeris.bot.signal.api.VersionInfo;
import org.ephemeris.bot.signal.components.Config;
import org.ephemeris.bot.signal.components.Signal;
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage;

import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.ephemeris.bot.signal.utils.Stream.readFullyAsString;

// This plugin should be the second in chain
public class CoreCommands extends Plugin {
    public CoreCommands(SignalBot bot) {
        super(bot);
    }

    @Override
    public boolean onMessage(User sender, Group group, SignalServiceDataMessage message) throws IOException {
        String term = message.getBody().get();

        switch (term.trim().toLowerCase()) {
            case "версия":
                Signal.getInstance().sendMessage(sender, group, signalBot.versionInfo.toString());

                String versionInfoURL = Config.getInstance().getAPIEndpoint(Method.API_VERSION_INFO), reply;

                try {
                    HttpURLConnection conn = (HttpURLConnection) new URL(versionInfoURL).openConnection();
                    conn.setRequestMethod("GET");
                    conn.connect();

                    int code = conn.getResponseCode();
                    String rawResponse = readFullyAsString(code / 100 == 2 ? conn.getInputStream() : conn.getErrorStream(), "UTF-8");
                    VersionInfo vi = VersionInfo.fromJSON(rawResponse);
                    reply = vi.toString();
                } catch (ConnectException ex) {
                    reply = "Не удаётся подключиться к Ephemeris API, возможно он не запущен?";
                } catch (Exception ex) {
                    reply = "Произошла ошибка при выполнении запроса к Ephemeris API или при разборе ответа:\r\n" + ex.toString();
                }

                Signal.getInstance().sendMessage(sender, group, reply);
                return true;
        }

        return false;
    }
}
