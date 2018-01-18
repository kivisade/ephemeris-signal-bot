package org.ephemeris.bot.signal.plugins;

import de.thoffbauer.signal4j.store.Group;
import de.thoffbauer.signal4j.store.User;
import org.ephemeris.bot.signal.Plugin;
import org.ephemeris.bot.signal.SignalBot;
import org.ephemeris.bot.signal.api.LookupResponse;
import org.ephemeris.bot.signal.api.Method;
import org.ephemeris.bot.signal.components.Config;
import org.ephemeris.bot.signal.components.Signal;
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage;

import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Pattern;

import static org.ephemeris.bot.signal.utils.Stream.readFullyAsString;

public class Lookup extends Plugin {
    public Lookup(SignalBot bot) {
        super(bot);
    }

    @Override
    public boolean onMessage(User sender, Group group, SignalServiceDataMessage message) throws IOException {
        String term = message.getBody().get();

        Pattern exactMatch = Pattern.compile(
                "([\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}\\x{30FB}\\x{30FC}\\p{IsLatin}\\d、]+)" +
                        "(?:【([\\p{IsHiragana}\\p{IsKatakana}\\x{30FB}\\x{30FC}\\p{IsLatin}\\d、]+)】)?");

        if (!exactMatch.matcher(term).matches()) return false;

        String
                endpoint = Config.getInstance().getAPIEndpoint(Method.WORD_LOOKUP),
                lookupURL = endpoint + URLEncoder.encode(term, "UTF-8"),
                reply;

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(lookupURL).openConnection();
            conn.setRequestMethod("GET");
            conn.connect();

            int code = conn.getResponseCode();
            String rawResponse = readFullyAsString(code / 100 == 2 ? conn.getInputStream() : conn.getErrorStream(), "UTF-8");
            LookupResponse apiResponse = LookupResponse.fromJSON(rawResponse);

            if (!apiResponse.isSuccess()) {
                reply = "Ephemeris API ответил ошибкой:\r\n" + apiResponse.getError();
            } else if (!apiResponse.hasResults()) {
                reply = String.format("Слово '%s' не найдено -(", apiResponse.getTerm());
            } else {
                for (String word : apiResponse.getResults()) {
                    Signal.getInstance().sendMessage(sender, group, word);
                }
                return true;
            }
        } catch (ConnectException ex) {
            reply = "Не удаётся подключиться к Ephemeris API, возможно он не запущен?";
        } catch (Exception ex) {
            reply = "Произошла ошибка при выполнении запроса к Ephemeris API или при разборе ответа:\r\n" + ex.toString();
        }

        Signal.getInstance().sendMessage(sender, group, reply);
        return true;
    }
}
