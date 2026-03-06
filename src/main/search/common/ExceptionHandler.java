package search.common;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Centralized exception handling utility for the search engine.
 * Provides consistent logging and error reporting across all modules.
 */
public final class ExceptionHandler {

    private static final Logger LOGGER = Logger.getLogger("CSIT5930-SearchEngine");

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
