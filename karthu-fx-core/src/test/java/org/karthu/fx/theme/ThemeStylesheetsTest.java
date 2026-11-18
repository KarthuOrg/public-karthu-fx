package org.karthu.fx.theme;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The theme's stylesheets as text: the checks a rule of the cascade cannot show, read from every stylesheet that
 * {@link ThemeManager#apply(Scene)} puts on a scene.
 */
class ThemeStylesheetsTest {

    private static final Pattern COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern STRING = Pattern.compile("\"(?:[^\"\\\\]|\\\\.)*\"|'(?:[^'\\\\]|\\\\.)*'");
    private static final Pattern RULE = Pattern.compile("([^{}]*)\\{([^{}]*)}");
    private static final Pattern DECLARATION = Pattern.compile("([A-Za-z_-][A-Za-z0-9_-]*)\\s*:([^;]*)");
    private static final Pattern NAME = Pattern.compile("[A-Za-z_-][A-Za-z0-9_-]*");

    @BeforeAll
    static void initToolkit() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException alreadyStarted) {
            // toolkit already running from another test class
        }
    }

    /**
     * A value names a looked-up colour or other value by its name, and JavaFX looks the name up on the node and its
     * parents. A declaration whose value names the property it sets finds itself on every node it applies to: JavaFX
     * reports a lookup loop and drops the declaration, so the node keeps no value at all.
     */
    @Test
    @DisplayName("no declaration of the theme's stylesheets, in either scheme, sets a property to a lookup of itself")
    void noDeclarationLooksUpItself() throws IOException {
        Set<String> stylesheets = new LinkedHashSet<>(stylesheets(Appearance.DARK));
        stylesheets.addAll(stylesheets(Appearance.LIGHT));
        assertEquals(3, stylesheets.size(), "base, dark and light: " + stylesheets);

        List<String> loops = new ArrayList<>();
        for (String url : stylesheets) {
            String css = read(url);
            int declarations = 0;
            Matcher rule = RULE.matcher(blank(blank(css, COMMENT), STRING));
            while (rule.find()) {
                Matcher declaration = DECLARATION.matcher(rule.group(2));
                while (declaration.find()) {
                    declarations++;
                    String property = declaration.group(1);
                    if (namesItself(property, declaration.group(2))) {
                        int line = lineOf(css, rule.start(2) + declaration.start());
                        String selector = rule.group(1).trim().replaceAll("\\s+", " ");
                        loops.add(fileName(url) + ":" + line + " " + selector + " { " + property + ": "
                                + declaration.group(2).trim() + " }");
                    }
                }
            }
            assertTrue(declarations > 0, "no declaration read from " + url);
        }

        assertEquals(List.of(), loops, "declarations that look up the property they set");
    }

    /**
     * Nebula Light replaces Nebula Dark on a scene, it is never loaded on top of it: a token or rule that only one of
     * them had would keep a value of the other scheme, or none at all.
     */
    @Test
    @DisplayName("light.css defines exactly the tokens of dark.css, in its order, and has the counterpart of each of its"
            + " other rules")
    void lightMatchesDark() throws IOException {
        String dark = schemeStylesheet(Appearance.DARK, "/dark.css");
        String light = schemeStylesheet(Appearance.LIGHT, "/light.css");

        List<String> darkRules = rules(read(dark));
        List<String> lightRules = rules(read(light));

        assertTrue(darkRules.getFirst().startsWith(".root {"), darkRules.getFirst());
        assertTrue(darkRules.size() > 1, "dark.css has rules beyond .root: " + darkRules);
        assertEquals(String.join("\n", darkRules), String.join("\n", lightRules));
    }

    /**
     * The stylesheets the theme puts on a scene with the appearance chosen. The choice is Dark again afterwards.
     */
    private static List<String> stylesheets(Appearance appearance) {
        ThemeManager.setAppearance(appearance);
        try {
            Scene scene = new Scene(new Region());
            ThemeManager.apply(scene);
            assertFalse(scene.getStylesheets().isEmpty(), "the theme put no stylesheet on the scene");
            return new ArrayList<>(scene.getStylesheets());
        } finally {
            ThemeManager.setAppearance(Appearance.DARK);
        }
    }

    private static String schemeStylesheet(Appearance appearance, String name) {
        List<String> stylesheets = stylesheets(appearance);
        String scheme = stylesheets.getLast();
        assertTrue(scheme.endsWith(name), "the scheme's stylesheet comes last: " + stylesheets);
        return scheme;
    }

    /**
     * Each rule of a stylesheet as its selector and the names of the properties it declares, in order, one per line:
     * {@code .root { -fx-text-color -fx-surface-color }}.
     */
    private static List<String> rules(String css) {
        List<String> rules = new ArrayList<>();
        Matcher rule = RULE.matcher(blank(blank(css, COMMENT), STRING));
        while (rule.find()) {
            StringBuilder names = new StringBuilder(rule.group(1).trim().replaceAll("\\s+", " ")).append(" {");
            Matcher declaration = DECLARATION.matcher(rule.group(2));
            while (declaration.find()) {
                names.append(' ').append(declaration.group(1));
            }
            rules.add(names.append(" }").toString());
        }
        return rules;
    }

    private static boolean namesItself(String property, String value) {
        Matcher name = NAME.matcher(value);
        while (name.find()) {
            if (name.group().equalsIgnoreCase(property)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Replaces every match with spaces, its line breaks kept: what stays keeps its offsets and lines.
     */
    private static String blank(String css, Pattern pattern) {
        StringBuilder blanked = new StringBuilder(css);
        Matcher matcher = pattern.matcher(css);
        while (matcher.find()) {
            for (int i = matcher.start(); i < matcher.end(); i++) {
                if (css.charAt(i) != '\n' && css.charAt(i) != '\r') {
                    blanked.setCharAt(i, ' ');
                }
            }
        }
        return blanked.toString();
    }

    private static int lineOf(String css, int offset) {
        int line = 1;
        for (int i = 0; i < offset; i++) {
            if (css.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    private static String fileName(String url) {
        return url.substring(url.lastIndexOf('/') + 1);
    }

    private static String read(String url) throws IOException {
        try (InputStream in = URI.create(url).toURL().openStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
