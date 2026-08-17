package com.owotest;

import com.owo.utils.JavaScriptBridge;
import com.owo.utils.Json;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the constraints the WebView bridge imposes but cannot check at compile time.
 *
 * <p>Whether a method is callable from JavaScript cannot be answered by calling it from
 * Java, so these assertions are reflective.
 */
class JavaScriptBridgeContractTest {

    /**
     * The only Java types WebView's JS→Java marshaller can bind an argument to. Notably
     * absent: {@code java.util.Map} and {@code java.util.function.Consumer} — a JS object
     * or function arrives as {@code JSObject} and nothing adapts it further.
     */
    private static final Set<Class<?>> MARSHALLABLE = Set.of(
            String.class, int.class, Integer.class, double.class, Double.class,
            float.class, Float.class, long.class, Long.class,
            boolean.class, Boolean.class, netscape.javascript.JSObject.class, Object.class);

    private static List<Method> publicBridgeMethods() {
        List<Method> methods = new ArrayList<>();
        for (Method m : JavaScriptBridge.class.getDeclaredMethods()) {
            if (Modifier.isPublic(m.getModifiers()) && !m.isSynthetic()) {
                methods.add(m);
            }
        }
        return methods;
    }

    @Test
    void allPublicMethods_useJsMarshallableParameterTypes() {
        List<String> offenders = new ArrayList<>();

        for (Method method : publicBridgeMethods()) {
            for (Class<?> parameterType : method.getParameterTypes()) {
                if (!MARSHALLABLE.contains(parameterType)) {
                    offenders.add(method.getName() + " takes " + parameterType.getSimpleName());
                }
            }
        }

        assertTrue(offenders.isEmpty(),
                "these methods cannot be invoked from JavaScript: " + offenders);
    }

    @Test
    void noPublicMethod_acceptsAUserIdParameter() {
        List<String> offenders = new ArrayList<>();

        for (Method method : publicBridgeMethods()) {
            for (var parameter : method.getParameters()) {
                String name = parameter.getName().toLowerCase(Locale.ROOT);
                if (name.contains("userid") || name.contains("customerid")) {
                    offenders.add(method.getName() + "(" + parameter.getName() + ")");
                }
            }
        }

        // Identity comes from the Java-side session. A method that accepts it as an
        // argument lets any client act as any user.
        assertTrue(offenders.isEmpty(),
                "these methods take an identity from the client: " + offenders);
    }

    @Test
    void asyncMethods_takeACallbackName() {
        // Every void method must end with a String callback name, since it cannot return
        // its result and cannot accept a function.
        for (Method method : publicBridgeMethods()) {
            if (method.getReturnType() != void.class || method.getName().equals("shutdown")
                    || method.getName().equals("setJSObject")
                    || method.getName().equals("showNotification")) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            assertTrue(params.length >= 1 && params[params.length - 1] == String.class,
                    method.getName() + " must take a callback name as its last parameter");
        }
    }

    @Test
    void responsesAreValidJson_forValuesContainingQuotesAndBackslashes() {
        String hostile = "Jo\"hn\\Doe\nline";

        String encoded = Json.obj().put("nama", hostile).toString();
        Map<String, Object> reparsed = Json.parseObject(encoded);

        assertEquals(hostile, reparsed.get("nama"));
    }

    @Test
    void numericFormatting_isLocaleIndependent() {
        Locale original = Locale.getDefault();
        try {
            // On id-ID, "%.2f" emits 1234,50 — which is not valid JSON.
            Locale.setDefault(Locale.forLanguageTag("id-ID"));

            String encoded = Json.obj().put("price", 1234.50d).toString();

            assertFalse(encoded.contains(","), "locale decimal comma leaked into JSON: " + encoded);
            assertEquals(1234.50d, (Double) Json.parseObject(encoded).get("price"), 0.001);
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void unauthenticatedSession_reportsAStableErrorCode() {
        JavaScriptBridge bridge = new JavaScriptBridge();

        Map<String, Object> response = Json.parseObject(bridge.getSession());

        assertEquals(Boolean.FALSE, response.get("success"));
        assertEquals("ERR_UNAUTHENTICATED", response.get("code"));
        // The UI branches on the code, never on the human-readable message.
        assertTrue(response.containsKey("message"));
    }

    @Test
    void getVersion_isDirectlyInvocable() {
        assertTrue(new JavaScriptBridge().getVersion().startsWith("OwO"));
    }
}
