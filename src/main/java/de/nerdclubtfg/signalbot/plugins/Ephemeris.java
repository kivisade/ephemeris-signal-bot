package de.nerdclubtfg.signalbot.plugins;

import de.nerdclubtfg.signalbot.Plugin;
import de.nerdclubtfg.signalbot.components.Signal;
import de.thoffbauer.signal4j.store.Group;
import de.thoffbauer.signal4j.store.User;
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.json.*;

public class Ephemeris extends Plugin {
    @Override
    public boolean accepts(User sender, Group group, SignalServiceDataMessage message) {
        return true;
    }

    @Override
    public void onMessage(User user, Group group, SignalServiceDataMessage message) throws IOException {
        String term = message.getBody().get(), reply;

//        try {
            URL endpoint = new URL("http://localhost:8080/word/" + term);
            HttpURLConnection conn = (HttpURLConnection) endpoint.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();

            int code = conn.getResponseCode();
            String rawResponse = readFullyAsString(code / 100 == 2 ? conn.getInputStream() : conn.getErrorStream(), "UTF-8");
            EphemerisAPIResponse re = parseJSONResponse(rawResponse);

            if (re.result == 200) {
                if (re.found.length == 1) {
                    reply = re.found[0];
                } else {
                    reply = Arrays.stream(re.found).map(s -> "* " + s).collect(Collectors.joining("\r\n"));
                }
            } else {
                reply = re.error;
            }
//        } catch (Exception ex) {
//            reply = ex.getMessage();
//        }

        Signal.getInstance().sendMessage(user, group, reply);
    }


    public String readFullyAsString(InputStream inputStream, String encoding)
            throws IOException {
        return readFully(inputStream).toString(encoding);
    }

    public byte[] readFullyAsBytes(InputStream inputStream)
            throws IOException {
        return readFully(inputStream).toByteArray();
    }

    private ByteArrayOutputStream readFully(InputStream inputStream)
            throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length = 0;
        while ((length = inputStream.read(buffer)) != -1) {
            baos.write(buffer, 0, length);
        }
        return baos;
    }

    public static class EphemerisAPIResponse {
        private String term;
        private int result;
        private String[] found;
        private String error;

        public EphemerisAPIResponse(String term, int result, String[] found, String error) {
            this.term = term;
            this.result = result;
            this.found = found;
            this.error = error;
        }
    }

    private EphemerisAPIResponse parseJSONResponse(String rawResponse) {
        JSONObject o = new JSONObject(rawResponse);

        ArrayList<String> foundList = new ArrayList<>();

        if (o.has("found") && !o.isNull("found")) {
            JSONArray found = o.getJSONArray("found");
            for (int i = 0; i < found.length(); i++) {
                foundList.add(found.getString(i));
            }
        }

        return new EphemerisAPIResponse(
                o.getString("term"),
                o.getInt("result"),
                foundList.toArray(new String[0]),
                o.getString("error")
        );
    }

}
