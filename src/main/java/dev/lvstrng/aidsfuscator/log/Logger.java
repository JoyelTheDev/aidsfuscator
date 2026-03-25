package dev.lvstrng.aidsfuscator.log;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

public class Logger {
    private static final Supplier<String> timeString = () -> {
        var date = LocalDateTime.now();
        var formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        return "[" + date.format(formatter) + "]";
    };

    private static void log(LogLevel level, String msg, Object... args) {
        System.out.printf(String.format("%s %s %s%n", timeString.get(), level.tag(), msg), args);
    }

    public static void warn(String msg, Object... args) {
        log(LogLevel.WARN, msg, args);
    }

    public static void error(String msg, Object... args) {
        log(LogLevel.ERROR, msg, args);
    }

    public static void info(String msg, Object... args) {
        log(LogLevel.INFO, msg, args);
    }
}
