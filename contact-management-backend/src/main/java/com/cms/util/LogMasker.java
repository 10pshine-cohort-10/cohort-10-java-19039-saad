package com.cms.util;

/** Masks personal identifiers before they are written to logs. */
public final class LogMasker {

    private LogMasker() {
    }

    public static String maskIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return "<empty>";
        }
        String value = identifier.trim();
        int at = value.indexOf('@');
        if (at > 0) {
            return value.charAt(0) + "***" + value.substring(at);
        }
        int visible = Math.min(2, value.length());
        return "***" + value.substring(value.length() - visible);
    }
}
