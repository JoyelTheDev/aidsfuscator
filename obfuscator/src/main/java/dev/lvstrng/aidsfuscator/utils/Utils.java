package dev.lvstrng.aidsfuscator.utils;

public class Utils {
    public static double bytesToKB(long bytes) {
        return Math.round((bytes / 1024.0) * 100.0) / 100.0;
    }
}
