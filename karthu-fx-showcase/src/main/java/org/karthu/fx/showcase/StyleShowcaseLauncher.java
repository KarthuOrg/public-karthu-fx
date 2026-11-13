package org.karthu.fx.showcase;

/**
 * Plain launcher class for jpackage Windows executables.
 * Works around the JavaFX module classloader restriction.
 */
public class StyleShowcaseLauncher {

    public static void main(String[] args) {
        StyleShowcaseApp.main(args);
    }
}
