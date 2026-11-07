package com.booking.notification.service;

import com.booking.common.exception.ApiException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TemplateRendererTest {

    private final TemplateRenderer renderer = new TemplateRenderer();

    @Test
    void replacesEveryParameter() {
        String text = renderer.render("Zdravo {{ime}}, termin je {{vreme}}.",
                Map.of("ime", "Marko", "vreme", "01.08.2026. 10:00"));

        assertThat(text).isEqualTo("Zdravo Marko, termin je 01.08.2026. 10:00.");
    }

    @Test
    void toleratesWhitespaceInsideThePlaceholder() {
        assertThat(renderer.render("Zdravo {{  ime  }}", Map.of("ime", "Ana"))).isEqualTo("Zdravo Ana");
    }

    @Test
    void treatsDollarSignsInValuesAsPlainText() {
        // $1 bi inače bilo protumačeno kao referenca na grupu u regex zameni.
        assertThat(renderer.render("Cena: {{cena}}", Map.of("cena", "$1,00")))
                .isEqualTo("Cena: $1,00");
    }

    @Test
    void rejectsTemplateWhoseParameterHasNoValue() {
        assertThatThrownBy(() -> renderer.render("Zdravo {{ime}}", Map.of()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("ime");
    }

    @Test
    void listsPlaceholdersInOrderWithoutDuplicates() {
        assertThat(renderer.placeholders("{{ime}} {{vreme}} {{ime}}"))
                .containsExactly("ime", "vreme");
    }
}
