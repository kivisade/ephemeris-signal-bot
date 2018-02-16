package org.ephemeris.bot.signal.components;

import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

public class VersionInfo {
    protected final String version,
            revision,
            buildDate,
            javaVersion;

    protected static final Pattern jarred = Pattern.compile("jar:file:.+\\.jar!.+");

    protected static class NoManifest extends Exception {
        protected final String path;
        protected NoManifest(String path) {
            this.path = path;
        }

        @Override
        public String toString() {
            return "Failed to locate `META-INF/MANIFEST.MF`: probably not running from a jar file. " +
                    "Current class is loaded from: " + path;
        }
    }

    protected static class ManifestReadFailed extends Exception {
        protected final String mfPath;
        protected ManifestReadFailed(String mfPath) {
            this.mfPath = mfPath;
        }

        @Override
        public String toString() {
            return "Failed to read build metadata from embedded `META-INF/MANIFEST.MF`.";
        }
    }

    protected String orDefault(String value) {
        if (value == null || value.isEmpty()) {
            return "unknown";
        }
        return value;
    }

    public VersionInfo() {
        this(null, null, null, null);
    }

    public VersionInfo(String version, String revision, String buildDate, String javaVersion) {
        this.version = orDefault(version);
        this.revision = orDefault(revision);
        this.buildDate = orDefault(buildDate);
        this.javaVersion = orDefault(javaVersion);
    }

    public VersionInfo(Class target) throws Exception {
        String path = target.getResource(target.getSimpleName() + ".class").toString();

        if (!jarred.matcher(path).matches()) {
            throw new NoManifest(path);
        }

        String mfPath = path.substring(0, path.lastIndexOf("!") + 1) + "/META-INF/MANIFEST.MF";
        // String pkg = target.getPackage().getName().replaceAll("\\.", "/");
        Manifest manifest = new Manifest(new URL(mfPath).openStream());

        // log("Reading manifest from: " + mfPath);

        Attributes attr = manifest.getMainAttributes();

        if (attr == null) {
            throw new ManifestReadFailed(mfPath);
        }

        this.version = orDefault(target.getPackage().getImplementationVersion());
        this.revision = orDefault(attr.getValue("Git-Commit"));
        this.buildDate = orDefault(attr.getValue("Build-Date"));
        this.javaVersion = orDefault(attr.getValue("Java-Version"));
    }

    public String getVersion() {
        return version;
    }

    public String getRevision() {
        return revision;
    }

    public String getBuildDate() {
        return buildDate;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public String toString() {
        return String.format("Ephemeris Signal Bot %s (ревизия %s)\nСборка %s, Java %s", version, revision, buildDate, javaVersion);
    }
}