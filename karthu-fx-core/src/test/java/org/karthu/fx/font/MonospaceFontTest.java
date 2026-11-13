package org.karthu.fx.font;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MonospaceFontTest {

    @Test
    @DisplayName("Cascadia Mono wins over Cascadia Code, which draws ligatures")
    void cascadiaMonoBeforeCascadiaCode() {
        assertEquals("Cascadia Mono", MonospaceFont.firstInstalled(List.of("Cascadia Code", "Cascadia Mono", "Consolas")));
    }

    @Test
    @DisplayName("JetBrains Mono NL comes first when it is installed")
    void jetBrainsMonoNoLigaturesFirst() {
        assertEquals("JetBrains Mono NL",
                MonospaceFont.firstInstalled(List.of("Consolas", "Cascadia Mono", "JetBrains Mono NL")));
    }

    @Test
    @DisplayName("families with ligatures are never chosen: Consolas follows")
    void ligatureFamiliesSkipped() {
        assertEquals("Consolas", MonospaceFont.firstInstalled(List.of("JetBrains Mono", "Cascadia Code", "Consolas")));
    }

    @Test
    @DisplayName("without any preferred family the logical Monospaced font is used")
    void logicalFallback() {
        assertEquals("Monospaced", MonospaceFont.firstInstalled(List.of("Cascadia Code", "Arial")));
    }
}
