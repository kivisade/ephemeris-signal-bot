package org.ephemeris.bot.signal.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class LookupResponse {
    @JsonProperty
    private String term;

    @JsonProperty
    private int result;

    @JsonProperty
    private String[] found;

    @JsonProperty
    private String error;

    public static LookupResponse fromJSON(String rawJSON) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(rawJSON, LookupResponse.class);
    }

    public String getTerm() {
        return term;
    }

    public boolean isSuccess() {
        return result == 200;
    }

    public String getError() {
        return error;
    }

    public boolean hasResults() {
        return found != null && found.length > 0;
    }

    public String[] getResults() {
        return found;
    }
}
