package com.owotest;

import com.owo.utils.Json;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonTest {

    @Test
    void roundTripsControlCharactersAndQuotes() {
        String value = "tab\there \"quoted\" back\\slash\nnewline";

        Map<String, Object> parsed = Json.parseObject(Json.obj().put("v", value).toString());

        assertEquals(value, parsed.get("v"));
    }

    @Test
    void escapesLineSeparatorsThatBreakJavaScriptSource() {
        String encoded = Json.obj().put("v", "a b c").toString();

        assertTrue(encoded.contains("\\u2028"));
        assertTrue(encoded.contains("\\u2029"));
    }

    @Test
    void writesNumbersWithoutTheDefaultLocaleSeparator() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("id-ID"));

            assertEquals("1500000.0", Json.number(1_500_000.0d));
            assertEquals("1234.5", Json.number(1234.5d));
            assertEquals("42", Json.number(42));
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void parsesNestedObjectsAndArrays() {
        Map<String, Object> parsed = Json.parseObject(
                "{\"a\": {\"b\": [1, 2, {\"c\": true}]}, \"d\": null}");

        @SuppressWarnings("unchecked")
        Map<String, Object> a = (Map<String, Object>) parsed.get("a");
        @SuppressWarnings("unchecked")
        List<Object> b = (List<Object>) a.get("b");

        assertEquals(3, b.size());
        assertEquals(1.0d, (Double) b.get(0), 0.0001);
        assertNull(parsed.get("d"));
    }

    @Test
    void parsesAnEmptyPayloadAsAnEmptyObject() {
        assertTrue(Json.parseObject("").isEmpty());
        assertTrue(Json.parseObject(null).isEmpty());
    }

    @Test
    void requireString_failsOnAMissingField() {
        Map<String, Object> args = Json.parseObject("{\"a\": \"x\"}");

        assertEquals("x", Json.requireString(args, "a"));
        assertThrows(Json.JsonException.class, () -> Json.requireString(args, "b"));
    }

    @Test
    void requireInt_acceptsNumbersAndNumericStrings() {
        Map<String, Object> args = Json.parseObject("{\"a\": 7, \"b\": \"8\", \"c\": \"x\"}");

        assertEquals(7, Json.requireInt(args, "a"));
        assertEquals(8, Json.requireInt(args, "b"));
        assertThrows(Json.JsonException.class, () -> Json.requireInt(args, "c"));
    }

    @Test
    void rejectsMalformedInput() {
        assertThrows(Json.JsonException.class, () -> Json.parseObject("{\"a\": }"));
        assertThrows(Json.JsonException.class, () -> Json.parseObject("[1,2]"));
        assertThrows(Json.JsonException.class, () -> Json.parseObject("{\"a\": \"unterminated"));
    }
}
