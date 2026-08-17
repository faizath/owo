package com.owotest;

import com.owo.utils.BridgeInstaller;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
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
    private static List<String> routableScreens() {
        return BridgeInstaller.routableScreens();
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
    void everyChromeLinkLeadsSomewhere() throws Exception {
        Path boundary = resources();
        String screensJs = Files.readString(
                boundary.resolve("screens.js"), StandardCharsets.UTF_8);

        // Labels that wireChrome must recognise. Nine of these used to call
        // preventDefault and return, so the link looked live and did nothing; a label
        // added to a fragment without a route here would silently join them.
        List<String> labels = List.of(
                "Tentang Kami", "Profil Perusahaan", "Kontak", "Bantuan dan Keluhan",
                "Syarat &amp; Ketentuan", "Kebijakan Privasi", "Kebijakan Pembatalan",
                "Pengaturan Akun", "Pengaturan");

        List<String> unrouted = new ArrayList<>();
        for (String label : labels) {
            String lower = label.replace("&amp;", "&").toLowerCase(java.util.Locale.ROOT);
            if (!screensJs.contains("'" + lower + "'")) {
                unrouted.add(label);
            }
        }

        assertTrue(unrouted.isEmpty(), "chrome labels wireChrome does not route: " + unrouted);
    }

    @Test
    void everyChromeLabelInAFragmentIsRouted() throws Exception {
        Path boundary = resources();
        String screensJs = Files.readString(
                boundary.resolve("screens.js"), StandardCharsets.UTF_8);
        Path screens = boundary.resolve("screens");

        // Anchors carrying a chrome class are wired by wireChrome and by nothing else,
        // so one whose text it does not recognise is a link that goes nowhere.
        Pattern chromeLink = Pattern.compile(
                "<a[^>]*class=\"[^\"]*(?:nav-item|footer-link)[^\"]*\"[^>]*>([^<]+)</a>");

        List<String> unrouted = new ArrayList<>();
        int examined = 0;
        for (String screen : routableScreens()) {
            String html = Files.readString(
                    screens.resolve(screen + ".html"), StandardCharsets.UTF_8);
            Matcher m = chromeLink.matcher(html);
            while (m.find()) {
                String label = m.group(1).trim().replace("&amp;", "&")
                        .toLowerCase(java.util.Locale.ROOT);
                if (label.isEmpty() || label.equals("log out")) {
                    continue;
                }
                examined++;
                if (!screensJs.contains("'" + label + "'")) {
                    unrouted.add(screen + " → \"" + label + "\"");
                }
            }
        }

        assertTrue(unrouted.isEmpty(), "chrome links with no destination: " + unrouted);
        // A regex that matched nothing would pass silently and check nothing at all.
        assertTrue(examined > 20, "only " + examined + " chrome links found; the pattern "
                + "no longer matches the markup");
    }

    @Test
    void everyClassNameScreensJsEmitsIsStyledSomewhere() throws Exception {
        Path boundary = resources();
        Path screens = boundary.resolve("screens");

        // Fragments carry their own styles, and App.html carries the shared ones.
        StringBuilder css = new StringBuilder(
                Files.readString(boundary.resolve("App.html"), StandardCharsets.UTF_8));
        for (String screen : routableScreens()) {
            css.append(Files.readString(
                    screens.resolve(screen + ".html"), StandardCharsets.UTF_8));
        }

        List<String> defined = new ArrayList<>();
        Matcher selector = Pattern.compile("\\.([A-Za-z][A-Za-z0-9_-]*)").matcher(css);
        while (selector.find()) {
            defined.add(selector.group(1));
        }

        // Result cards are built in JavaScript, so their class names are only connected to
        // the stylesheet by spelling. Both availability screens emitted a whole parallel
        // set — flight-main, hotel-side, select-button — that nothing styled, and every
        // result rendered as bare text with a default browser button. No behavioural test
        // can see that; the class names at least can be checked.
        List<String> unstyled = new ArrayList<>();
        String js = Files.readString(boundary.resolve("screens.js"), StandardCharsets.UTF_8);
        Matcher emitted = Pattern.compile("class=\"([^\"'<>+]*)\"").matcher(js);
        while (emitted.find()) {
            for (String name : emitted.group(1).trim().split("\\s+")) {
                if (!name.isEmpty() && !defined.contains(name) && !unstyled.contains(name)) {
                    unstyled.add(name);
                }
            }
        }

        assertTrue(unstyled.isEmpty(), "class names no stylesheet defines: " + unstyled);
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
