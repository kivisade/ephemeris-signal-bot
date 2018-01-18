package org.ephemeris.bot.signal.plugins;

import de.thoffbauer.signal4j.store.Group;
import de.thoffbauer.signal4j.store.User;
import org.ephemeris.bot.signal.Plugin;
import org.ephemeris.bot.signal.api.LookupResponse;
import org.ephemeris.bot.signal.api.Method;
import org.ephemeris.bot.signal.api.VersionInfo;
import org.ephemeris.bot.signal.components.Config;
import org.ephemeris.bot.signal.components.Signal;
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage;

import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.regex.Pattern;

import static org.ephemeris.bot.signal.utils.Stream.readFullyAsString;

public class Ephemeris extends Plugin {
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss z");

    private void log(String s, Object... objects) {
        System.out.printf("%s | %s\n", dateFormatter.format(Date.from(Instant.now())), String.format(s, objects));
    }

    @Override
    public boolean accepts(User sender, Group group, SignalServiceDataMessage message) {
        if (sender.getNumber().equals(Signal.getInstance().getPhoneNumber())) {
            return false; // ignore messages from self
        }

        long mts = message.getTimestamp();

        if (mts < Signal.getInstance().getStartedAt()) {
            log("ignoring message from [%s] <%s> sent at %s",
                    sender.getName(), sender.getNumber(),
                    dateFormatter.format(Date.from(Instant.ofEpochMilli(mts))));
            return false;
        }

        return true;
    }

    @Override
    public void onMessage(User user, Group group, SignalServiceDataMessage message) throws IOException {

        String term = message.getBody().get();

        Pattern exactMatch = Pattern.compile(
                "([\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}\\x{30FB}\\x{30FC}\\p{IsLatin}\\d、]+)" +
                        "(?:【([\\p{IsHiragana}\\p{IsKatakana}\\x{30FB}\\x{30FC}\\p{IsLatin}\\d、]+)】)?");

        if (preprocess(user, group, term)) return;

        if (!exactMatch.matcher(term).matches()) return;

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
                    Signal.getInstance().sendMessage(user, group, word);
                }
                return;
            }
        } catch (ConnectException ex) {
            reply = "Не удаётся подключиться к Ephemeris API, возможно он не запущен?";
        } catch (Exception ex) {
            reply = "Произошла ошибка при выполнении запроса к Ephemeris API или при разборе ответа:\r\n" + ex.toString();
        }

        Signal.getInstance().sendMessage(user, group, reply);
    }

    boolean preprocess(User user, Group group, String term) throws IOException {
        switch (term) {
            case "версия":
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

                Signal.getInstance().sendMessage(user, group, reply);
                break;
            default:
                return false;
        }
        return true;
    }
}
