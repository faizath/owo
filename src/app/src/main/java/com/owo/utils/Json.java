package com.owo.utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Minimal JSON reader and writer for the JavaScript bridge.
 *
 * <p>Replaces hand-built {@code String.format} JSON, which produced invalid output for
 * any value containing a quote or backslash, and which emitted {@code 1234,50} for
 * {@code %.2f} on an {@code id-ID} machine — the target locale. Every number written
 * here uses {@link Locale#ROOT}.
 *
 * <p>Deliberately small: the project already declares Guava, but Guava has no JSON
 * support, and the bridge payloads do not justify a new third-party dependency.
 */
public final class Json {

    private Json() {
    }

    // ---------------------------------------------------------------- writing

    /** A JSON object under construction. Insertion-ordered so output is stable. */
    public static final class Obj {
        private final Map<String, String> members = new LinkedHashMap<>();

        public Obj put(String key, String value) {
            members.put(key, value == null ? "null" : quote(value));
            return this;
        }

        public Obj put(String key, Number value) {
            members.put(key, value == null ? "null" : number(value));
            return this;
        }

        public Obj put(String key, boolean value) {
            members.put(key, Boolean.toString(value));
            return this;
        }

        public Obj put(String key, Obj value) {
            members.put(key, value == null ? "null" : value.toString());
            return this;
        }

        public Obj put(String key, Arr value) {
            members.put(key, value == null ? "null" : value.toString());
            return this;
        }

        public Obj putNull(String key) {
            members.put(key, "null");
            return this;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, String> entry : members.entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                sb.append(quote(entry.getKey())).append(':').append(entry.getValue());
            }
            return sb.append('}').toString();
        }
    }

    /** A JSON array under construction. */
    public static final class Arr {
        private final List<String> elements = new ArrayList<>();

        public Arr add(Obj value) {
            elements.add(value == null ? "null" : value.toString());
            return this;
        }

        public Arr add(String value) {
            elements.add(value == null ? "null" : quote(value));
            return this;
        }

        public Arr add(Number value) {
            elements.add(value == null ? "null" : number(value));
            return this;
        }

        public int size() {
            return elements.size();
        }

        @Override
        public String toString() {
            return "[" + String.join(",", elements) + "]";
        }
    }

    public static Obj obj() {
        return new Obj();
    }

    public static Arr arr() {
        return new Arr();
    }

    /** Formats a number for JSON, never using the default locale's decimal separator. */
    public static String number(Number value) {
        if (value instanceof Double || value instanceof Float) {
            double d = value.doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                return "null";
            }
            if (d == Math.rint(d) && Math.abs(d) < 1e15) {
                return String.format(Locale.ROOT, "%.1f", d);
            }
            return String.format(Locale.ROOT, "%s", Double.toString(d));
        }
        return value.toString();
    }

    /** Escapes a string and wraps it in quotes. */
    public static String quote(String value) {
        StringBuilder sb = new StringBuilder(value.length() + 2).append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    // Control characters, and the line terminators that are legal in JSON
                    // but not in JavaScript source, must be escaped.
                    if (c < 0x20 || c == '\u2028' || c == '\u2029') {
                        sb.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }

    // ---------------------------------------------------------------- reading

    /** Thrown when input is not the JSON the caller expected. */
    public static class JsonException extends RuntimeException {
        public JsonException(String message) {
            super(message);
        }
    }

    /**
     * Parses a JSON object into a map of {@code String}, {@code Double}, {@code Boolean},
     * {@code null}, {@code List} and nested {@code Map} values.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        Object parsed = new Parser(json).parseValue();
        if (!(parsed instanceof Map)) {
            throw new JsonException("Expected a JSON object");
        }
        return (Map<String, Object>) parsed;
    }

    private static final class Parser {
        private final String src;
        private int pos;

        Parser(String src) {
            this.src = src;
        }

        Object parseValue() {
            skipWhitespace();
            if (pos >= src.length()) {
                throw new JsonException("Unexpected end of input");
            }
            char c = src.charAt(pos);
            return switch (c) {
                case '{' -> parseObj();
                case '[' -> parseArr();
                case '"' -> parseString();
                case 't', 'f' -> parseBoolean();
                case 'n' -> parseNull();
                default -> parseNumber();
            };
        }

        private Map<String, Object> parseObj() {
            Map<String, Object> result = new LinkedHashMap<>();
            expect('{');
            skipWhitespace();
            if (peek() == '}') {
                pos++;
                return result;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                result.put(key, parseValue());
                skipWhitespace();
                char c = next();
                if (c == '}') {
                    return result;
                }
                if (c != ',') {
                    throw new JsonException("Expected ',' or '}' at position " + (pos - 1));
                }
            }
        }

        private List<Object> parseArr() {
            List<Object> result = new ArrayList<>();
            expect('[');
            skipWhitespace();
            if (peek() == ']') {
                pos++;
                return result;
            }
            while (true) {
                result.add(parseValue());
                skipWhitespace();
                char c = next();
                if (c == ']') {
                    return result;
                }
                if (c != ',') {
                    throw new JsonException("Expected ',' or ']' at position " + (pos - 1));
                }
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (pos >= src.length()) {
                    throw new JsonException("Unterminated string");
                }
                char c = src.charAt(pos++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c != '\\') {
                    sb.append(c);
                    continue;
                }
                char esc = next();
                switch (esc) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        if (pos + 4 > src.length()) {
                            throw new JsonException("Truncated unicode escape");
                        }
                        sb.append((char) Integer.parseInt(src.substring(pos, pos + 4), 16));
                        pos += 4;
                    }
                    default -> throw new JsonException("Invalid escape: \\" + esc);
                }
            }
        }

        private Boolean parseBoolean() {
            if (src.startsWith("true", pos)) {
                pos += 4;
                return Boolean.TRUE;
            }
            if (src.startsWith("false", pos)) {
                pos += 5;
                return Boolean.FALSE;
            }
            throw new JsonException("Invalid literal at position " + pos);
        }

        private Object parseNull() {
            if (src.startsWith("null", pos)) {
                pos += 4;
                return null;
            }
            throw new JsonException("Invalid literal at position " + pos);
        }

        private Double parseNumber() {
            int start = pos;
            while (pos < src.length() && "+-0123456789.eE".indexOf(src.charAt(pos)) >= 0) {
                pos++;
            }
            if (start == pos) {
                throw new JsonException("Expected a value at position " + pos);
            }
            try {
                return Double.valueOf(src.substring(start, pos));
            } catch (NumberFormatException e) {
                throw new JsonException("Invalid number: " + src.substring(start, pos));
            }
        }

        private void skipWhitespace() {
            while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
                pos++;
            }
        }

        private char peek() {
            if (pos >= src.length()) {
                throw new JsonException("Unexpected end of input");
            }
            return src.charAt(pos);
        }

        private char next() {
            if (pos >= src.length()) {
                throw new JsonException("Unexpected end of input");
            }
            return src.charAt(pos++);
        }

        private void expect(char expected) {
            skipWhitespace();
            char c = next();
            if (c != expected) {
                throw new JsonException("Expected '" + expected + "' at position " + (pos - 1));
            }
        }
    }

    // ------------------------------------------------------- typed field access

    /** @throws JsonException if the field is absent, null, or blank */
    public static String requireString(Map<String, Object> args, String key) {
        Object value = args.get(key);
        if (!(value instanceof String s) || s.isBlank()) {
            throw new JsonException("Missing required field: " + key);
        }
        return s;
    }

    public static String optString(Map<String, Object> args, String key, String fallback) {
        Object value = args.get(key);
        return value instanceof String s && !s.isBlank() ? s : fallback;
    }

    /** @throws JsonException if the field is absent or not a number */
    public static int requireInt(Map<String, Object> args, String key) {
        Object value = args.get(key);
        if (value instanceof Number n) {
            return n.intValue();
        }
        if (value instanceof String s) {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException ignored) {
                // fall through to the shared failure below
            }
        }
        throw new JsonException("Missing or non-numeric field: " + key);
    }

    public static int optInt(Map<String, Object> args, String key, int fallback) {
        Object value = args.get(key);
        if (value instanceof Number n) {
            return n.intValue();
        }
        if (value instanceof String s) {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }
}
