package dev.lvstrng.aidsfuscator.log;

public enum LogLevel {
    INFO("[INFO]"),
    WARN("[WARN]"),
    ERROR("[ERROR]");

    private final String tag;
    LogLevel(String tag) {
        this.tag = tag;
    }

    public String tag() {
        return tag;
    }
}
