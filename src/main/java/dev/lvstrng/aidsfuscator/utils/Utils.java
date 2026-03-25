package dev.lvstrng.aidsfuscator.utils;

public class Utils {
    public static double bytesToKB(long bytes) {
        return Double.parseDouble(String.format("%.2f", bytes / 1024.0));
    }
}
