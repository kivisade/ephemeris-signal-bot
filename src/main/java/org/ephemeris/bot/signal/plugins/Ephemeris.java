package org.ephemeris.bot.signal.plugins;

import org.ephemeris.bot.signal.Plugin;
import org.ephemeris.bot.signal.api.LookupResponse;
import org.ephemeris.bot.signal.api.Method;
import org.ephemeris.bot.signal.components.Config;
import org.ephemeris.bot.signal.components.Signal;
import de.thoffbauer.signal4j.store.Group;
import de.thoffbauer.signal4j.store.User;
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage;

import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.ephemeris.bot.signal.utils.Stream.readFullyAsString;

public class Ephemeris extends Plugin {
    @Override
    public boolean accepts(User sender, Group group, SignalServiceDataMessage message) {
        return true;
    }

    @Override
    public void onMessage(User user, Group group, SignalServiceDataMessage message) throws IOException {

        String
                endpoint = Config.getInstance().getAPIEndpoint(Method.WORD_LOOKUP),
                term = message.getBody().get(),
                reply;

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(endpoint + term).openConnection();
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
