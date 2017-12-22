package org.ephemeris.bot.signal.api;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class LookupResponse {
    private String term;
    private int result;
    private String[] found;
    private String error;

    public LookupResponse(String rawJSON) {
        JSONObject o = new JSONObject(rawJSON);

        ArrayList<String> foundList = new ArrayList<>();

        if (o.has("found") && !o.isNull("found")) {
            JSONArray found = o.getJSONArray("found");
            for (int i = 0; i < found.length(); i++) {
                foundList.add(found.getString(i));
            }
        }

        this.term = o.getString("term");
        this.result = o.getInt("result");
        this.found = foundList.toArray(new String[0]);
        this.error = o.getString("error");
    }

    public String getFormattedReply() {
        if (this.result != 200) {
            return "Ephemeris API ответил ошибкой:\r\n" + this.error;
        }

        switch (this.found.length) {
            case 0:
                return String.format("Слово '%s' не найдено -(", this.term);
            case 1:
                return this.found[0];
            default:
                return Arrays.stream(this.found).map(s -> "* " + s).collect(Collectors.joining("\r\n"));
        }
    }
}
