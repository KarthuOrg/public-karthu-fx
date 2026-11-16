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
import java.util.List;
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
    @DisplayName("no declaration of the theme's stylesheets sets a property to a lookup of itself")
    void noDeclarationLooksUpItself() throws IOException {
        Scene scene = new Scene(new Region());
        ThemeManager.apply(scene);
        assertFalse(scene.getStylesheets().isEmpty(), "the theme put no stylesheet on the scene");

        List<String> loops = new ArrayList<>();
        for (String url : scene.getStylesheets()) {
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
