package com.cms.util;

import java.util.regex.Pattern;

/**
 * Normalises phone numbers to an optional leading '+' followed by digits so that
 * "+1 (555) 123-4567" and "+15551234567" are treated as the same number.
 */
public final class PhoneNormalizer {

    private static final Pattern VALID = Pattern.compile("^\\+?[0-9]{7,15}$");
    private static final Pattern ALLOWED_INPUT = Pattern.compile("^\\+?[0-9\\s().\\-]+$");

    private PhoneNormalizer() {
    }

    /** Returns the normalised number, or the trimmed input unchanged when it contains unsupported characters. */
    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (!ALLOWED_INPUT.matcher(trimmed).matches()) {
            return trimmed;
        }
        String digits = trimmed.replaceAll("\\D", "");
        return trimmed.startsWith("+") ? "+" + digits : digits;
    }

    public static boolean isValid(String raw) {
        String normalized = normalize(raw);
        return normalized != null && VALID.matcher(normalized).matches();
    }
}
