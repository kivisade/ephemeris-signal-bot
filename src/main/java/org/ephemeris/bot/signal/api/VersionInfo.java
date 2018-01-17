package org.ephemeris.bot.signal.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class VersionInfo {
    @JsonProperty("ver")
    private String AppVersion;

    @JsonProperty("rev")
    private String AppRevision;

    @JsonProperty("msg")
    private String RevMessage;

    @JsonProperty("build_date")
    private String BuildDate;

    @JsonProperty("go_ver")
    private String GoVersion;

    public static VersionInfo fromJSON(String rawJSON) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(rawJSON, VersionInfo.class);
    }

    public String toString() {
        return String.format("Ephemeris API %s (ревизия %s)\nСборка %s, Golang %s", AppVersion, AppRevision, BuildDate, GoVersion);
    }
}
