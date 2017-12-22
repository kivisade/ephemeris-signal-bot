package de.nerdclubtfg.signalbot.plugins;

import de.nerdclubtfg.signalbot.Plugin;
import de.nerdclubtfg.signalbot.api.LookupResponse;
import de.nerdclubtfg.signalbot.components.Signal;
import de.thoffbauer.signal4j.store.Group;
import de.thoffbauer.signal4j.store.User;
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage;

import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;

import static de.nerdclubtfg.signalbot.utils.Stream.readFullyAsString;

public class Ephemeris extends Plugin {
    @Override
    public boolean accepts(User sender, Group group, SignalServiceDataMessage message) {
        return true;
    }

    @Override
    public void onMessage(User user, Group group, SignalServiceDataMessage message) throws IOException {
        String term = message.getBody().get(), reply;

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL("http://localhost:8080/word/" + term).openConnection();
            conn.setRequestMethod("GET");
            conn.connect();

            int code = conn.getResponseCode();
            String rawResponse = readFullyAsString(code / 100 == 2 ? conn.getInputStream() : conn.getErrorStream(), "UTF-8");
            reply = new LookupResponse(rawResponse).getFormattedReply();
        } catch (ConnectException ex) {
            reply = "Не удаётся подключиться к Ephemeris API, возможно он не запущен?";
        } catch (Exception ex) {
            reply = "Произошла ошибка при выполнении запроса к Ephemeris API или при разборе ответа:\r\n" + ex.toString();
        }

        Signal.getInstance().sendMessage(user, group, reply);
    }
}
