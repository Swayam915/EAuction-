package com.eauction.util;

import java.util.regex.Pattern;

/**
 * Centralised input validation helpers used by servlets before
 * data reaches the DAO layer.
 */
public final class ValidationUtil {

    private static final Pattern EMAIL_RE =
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Pattern PHONE_RE =
        Pattern.compile("^[6-9]\\d{9}$");   // Indian 10-digit mobile numbers

    private static final Pattern NAME_RE =
        Pattern.compile("^[A-Za-z .'\\-]{2,100}$");

    private ValidationUtil() {}

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_RE.matcher(email.trim()).matches();
    }

    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.isBlank()) return true; // optional field
        return PHONE_RE.matcher(phone.trim()).matches();
    }

    public static boolean isValidName(String name) {
        return name != null && NAME_RE.matcher(name.trim()).matches();
    }

    public static boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }

    /**
     * HTML-encodes a string to prevent XSS when embedding user-supplied
     * data inside JSP scriptlets (where JSTL c:out is not available).
     */
    public static String escapeHtml(String input) {
        if (input == null) return "";
        return input
            .replace("&",  "&amp;")
            .replace("<",  "&lt;")
            .replace(">",  "&gt;")
            .replace("\"", "&quot;")
            .replace("'",  "&#x27;");
    }

    /**
     * Returns a trimmed, non-null string from a request parameter.
     * Returns "" if the parameter is absent or null.
     */
    public static String param(javax.servlet.http.HttpServletRequest req, String name) {
        String v = req.getParameter(name);
        return v == null ? "" : v.trim();
    }
}
