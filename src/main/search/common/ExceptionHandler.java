package search.common;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Centralized exception handling utility for the search engine.
 * Provides consistent logging and error reporting across all modules.
 * Uses a fixed English format to avoid locale-dependent output.
 */
public final class ExceptionHandler {

    private static final Logger LOGGER = Logger.getLogger("CSIT5930-SearchEngine");

    static {
        LOGGER.setUseParentHandlers(false);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new Formatter() {
            private final SimpleDateFormat dateFormat =
                new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

            @Override
            public String format(LogRecord record) {
                return String.format("[%s] [%s] %s%n",
                    dateFormat.format(new Date(record.getMillis())),
                    record.getLevel().getName(),
                    record.getMessage());
            }
        });
        LOGGER.addHandler(handler);
    }

    private ExceptionHandler() {
        // Prevent instantiation
    }

    /**
     * Logs an error with context message and exception details.
     *
     * @param context description of the operation that failed
     * @param e       the exception that occurred
     */
    public static void handleError(String context, Exception e) {
        LOGGER.log(Level.SEVERE, context + ": " + e.getMessage(), e);
    }

    /**
     * Logs a warning message.
     *
     * @param message the warning message
     */
    public static void warn(String message) {
        LOGGER.log(Level.WARNING, message);
    }

    /**
     * Logs an informational message.
     *
     * @param message the information message
     */
    public static void info(String message) {
        LOGGER.log(Level.INFO, message);
    }
}
