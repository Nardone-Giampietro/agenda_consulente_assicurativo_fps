package xyz.nardone.agenda_fps.applicazione.utility;

/**
 * Normalizes user-facing strings for consistent capitalization.
 */
public class Normalize {
    public static String normalize(String input) {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("Input string must be non-empty and not null");
        }

        input = input.toLowerCase().trim(); // Convert to lowercase and trim whitespace
        return input.substring(0, 1).toUpperCase() + input.substring(1); // Capitalize the first letter
    }
}
