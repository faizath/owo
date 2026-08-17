package com.owotest;

import com.owo.utils.JavaScriptBridge;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the boundary resources without a browser.
 *
 * <p>Asset references are matched case-sensitively on purpose. A reference to
 * {@code Email.png} against a file named {@code email.png} works on a case-insensitive
 * filesystem and breaks on Linux and inside the packaged jar, which is exactly how it
 * survived review.
 */
class BoundaryResourceTest {

    private static final Pattern ASSET_REFERENCE =
            Pattern.compile("(?:\\.\\./)?assets/([A-Za-z0-9 _.\\-]+)");

    /**
     * Screens the router may load, read from {@code JavaScriptBridge}'s own allow-list.
     *
     * <p>This used to be a hand-maintained copy of that list. A screen added to the bridge
     * and not to the copy was routable but exempt from every check below, which is exactly
     * the drift these tests exist to catch.
     */
    private static List<String> routableScreens() throws Exception {
        Field field = JavaScriptBridge.class.getDeclaredField("SCREENS");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> screens = (List<String>) field.get(null);
        return screens;
    }

    private static Path resources() throws URISyntaxException {
        return Paths.get(BoundaryResourceTest.class.getResource("/com/owo/boundary").toURI());
    }

    @Test
    void everyFragmentOnDiskIsRoutable() throws Exception {
        Path screens = resources().resolve("screens");
        List<String> known = routableScreens();

        List<String> orphans = new ArrayList<>();
        try (Stream<Path> files = Files.list(screens)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                String name = file.getFileName().toString();
                if (!name.endsWith(".html")) {
                    continue;
                }
                String screen = name.substring(0, name.length() - ".html".length());
                if (!known.contains(screen)) {
                    orphans.add(screen);
                }
            }
        }

        // A fragment the bridge will not serve is dead weight that still looks live.
        assertTrue(orphans.isEmpty(), "fragments no route can reach: " + orphans);
    }

    @Test
    void everyReferencedAssetExistsWithExactlyThatName() throws Exception {
        Path boundary = resources();
        Path assets = boundary.resolveSibling("assets");

        List<String> missing = new ArrayList<>();
        try (Stream<Path> files = Files.walk(boundary)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                String name = file.getFileName().toString();
                if (!name.endsWith(".html") && !name.endsWith(".js")) {
                    continue;
                }
                String content = Files.readString(file, StandardCharsets.UTF_8);
                Matcher m = ASSET_REFERENCE.matcher(content);
                while (m.find()) {
                    String asset = m.group(1);
                    if (!exactlyNamedFileExists(assets, asset)) {
                        missing.add(name + " → assets/" + asset);
                    }
                }
            }
        }

        assertTrue(missing.isEmpty(), "referenced assets that do not exist: " + missing);
    }

    /** Compares against the directory listing, since the filesystem may be case-insensitive. */
    private boolean exactlyNamedFileExists(Path directory, String name) throws IOException {
        try (Stream<Path> entries = Files.list(directory)) {
            return entries.anyMatch(p -> p.getFileName().toString().equals(name));
        }
    }

    @Test
    void everyRoutableScreenHasAFragment() throws Exception {
        Path screens = resources().resolve("screens");

        List<String> missing = new ArrayList<>();
        for (String screen : routableScreens()) {
            if (!Files.exists(screens.resolve(screen + ".html"))) {
                missing.add(screen);
            }
        }

        assertTrue(missing.isEmpty(), "screens with no fragment: " + missing);
    }

    @Test
    void screenFragmentsCarryNoInlineScriptsOrHandlers() throws Exception {
        Path screens = resources().resolve("screens");

        List<String> offenders = new ArrayList<>();
        for (String screen : routableScreens()) {
            String html = Files.readString(screens.resolve(screen + ".html"), StandardCharsets.UTF_8);
            if (html.contains("<script")) {
                offenders.add(screen + " has an inline script");
            }
            // Inline handlers execute attribute text as code, so any interpolated value
            // becomes script. Behaviour belongs in screens.js.
            Matcher m = Pattern.compile("\\son[a-z]+\\s*=\\s*\"").matcher(html);
            if (m.find()) {
                offenders.add(screen + " has an inline event handler");
            }
        }

        assertTrue(offenders.isEmpty(), String.join(", ", offenders));
    }

    @Test
    void theRefundFormCollectsNoCardDetails() throws Exception {
        String html = Files.readString(
                resources().resolve("screens/RefundForm.html"), StandardCharsets.UTF_8);

        // Card entry is allowed on the payment screen for the simulated flow, where the
        // values stay in the browser. A refund needs a payee and an account, and the
        // CVV this form used to collect must never be captured for storage.
        List<String> offenders = new ArrayList<>();
        for (String field : List.of("id=\"cvv\"", "id=\"cardNumber\"",
                "id=\"expiryMonth\"", "id=\"expiryYear\"")) {
            if (html.contains(field)) {
                offenders.add(field);
            }
        }

        assertTrue(offenders.isEmpty(), "refund form still collects: " + offenders);
        assertTrue(html.contains("id=\"namaPenerima\"") && html.contains("id=\"rekeningTujuan\""),
                "refund form does not collect disbursement details");
    }

    @Test
    void noCardFieldCrossesTheBridge() throws Exception {
        Path boundary = resources();

        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(boundary)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                if (!file.getFileName().toString().endsWith(".js")) {
                    continue;
                }
                String js = Files.readString(file, StandardCharsets.UTF_8);
                // A card value appearing in a bridge payload would send it to Java.
                Matcher m = Pattern.compile(
                        "(cvv|cardNumber|expiryYear)\\s*:").matcher(js);
                if (m.find()) {
                    offenders.add(file.getFileName() + " sends " + m.group(1));
                }
            }
        }

        assertTrue(offenders.isEmpty(), String.join(", ", offenders));
    }

    @Test
    void noProductionScreenReferencesTheMockDataFile() throws Exception {
        Path boundary = resources();

        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(boundary)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                String content = Files.readString(file, StandardCharsets.UTF_8);
                // Matches a script tag or an actual use, not the word in a comment.
                if (content.contains("src=\"mockup.js\"") || content.contains("mockupData")) {
                    offenders.add(file.getFileName().toString());
                }
            }
        }

        assertTrue(offenders.isEmpty(), "files still referencing mock data: " + offenders);
    }
}
