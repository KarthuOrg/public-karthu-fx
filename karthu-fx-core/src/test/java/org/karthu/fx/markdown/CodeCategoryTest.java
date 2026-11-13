package org.karthu.fx.markdown;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The categories of coloured code: nine base categories with a colour of their own, fourteen fine ones that fall
 * back to a base category, to another fine one or to the code text.
 */
class CodeCategoryTest {

    @Test
    @DisplayName("nine base categories, each with a colour of its own and no fallback")
    void baseCategories() {
        List<CodeCategory> base = new ArrayList<>();
        for (CodeCategory category : CodeCategory.values()) {
            if (category.isBase()) {
                base.add(category);
            }
        }
        assertEquals(List.of(CodeCategory.COMMENT, CodeCategory.KEYWORD, CodeCategory.STRING, CodeCategory.NUMBER,
                CodeCategory.TYPE, CodeCategory.FUNCTION, CodeCategory.VARIABLE, CodeCategory.PROPERTY,
                CodeCategory.TAG), base);
        for (CodeCategory category : base) {
            assertEquals(Optional.empty(), category.fallback(), category.name());
        }
    }

    @Test
    @DisplayName("fourteen fine categories, each naming the category it falls back to, none for code text")
    void fineCategoriesAndTheirFallbacks() {
        assertEquals(23, CodeCategory.values().length);
        assertFallback(CodeCategory.DOC, CodeCategory.COMMENT);
        assertFallback(CodeCategory.ANNOTATION, CodeCategory.KEYWORD);
        assertFallback(CodeCategory.LITERAL, CodeCategory.NUMBER);
        assertFallback(CodeCategory.CONSTANT, CodeCategory.LITERAL);
        assertFallback(CodeCategory.ESCAPE, CodeCategory.STRING);
        assertFallback(CodeCategory.ATTRIBUTE, CodeCategory.NUMBER);
        assertFallback(CodeCategory.OPERATOR, null);
        assertFallback(CodeCategory.PUNCTUATION, null);
        assertFallback(CodeCategory.HEADING, CodeCategory.FUNCTION);
        assertFallback(CodeCategory.LINK, CodeCategory.PROPERTY);
        assertFallback(CodeCategory.CODE, CodeCategory.STRING);
        assertFallback(CodeCategory.LIST, CodeCategory.KEYWORD);
        assertFallback(CodeCategory.QUOTE, CodeCategory.COMMENT);
        assertFallback(CodeCategory.EMPHASIS, null);
    }

    @Test
    @DisplayName("each category has a style class with the prefix code- and a CSS variable named like -fx-code-text-color")
    void styleClassAndVariable() {
        assertEquals("code-keyword", CodeCategory.KEYWORD.styleClass());
        assertEquals("-fx-code-keyword-color", CodeCategory.KEYWORD.colorVariable());
        assertEquals("code-annotation", CodeCategory.ANNOTATION.styleClass());
        assertEquals("-fx-code-annotation-color", CodeCategory.ANNOTATION.colorVariable());
        assertEquals("code-punctuation", CodeCategory.PUNCTUATION.styleClass());
        assertEquals("-fx-code-punctuation-color", CodeCategory.PUNCTUATION.colorVariable());
        assertEquals("code-tag", CodeCategory.TAG.styleClass());
        assertEquals("-fx-code-tag-color", CodeCategory.TAG.colorVariable());
    }

    private static void assertFallback(CodeCategory fine, CodeCategory expected) {
        assertEquals(false, fine.isBase(), fine.name() + " is fine");
        assertEquals(Optional.ofNullable(expected), fine.fallback(), fine.name());
    }
}
