package com.booking.notification.service;

import com.booking.common.exception.ApiException;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Popunjava parametre u tekstu šablona. Parametar se piše kao ime unutar dvostrukih
 * vitičastih zagrada, npr. imeKlijenta.
 *
 * <p>Namerno nije uveden pun template engine (Thymeleaf, Freemarker): tekst šablona upisuje
 * administrator kroz API, pa jednostavna zamena imena bez izvršavanja izraza ne može da
 * posluži kao vektor za izvršavanje koda na serveru.
 */
@Component
public class TemplateRenderer {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_]+)\\s*}}");

    /**
     * @throws ApiException 400 ako tekst traži parametar koji pozivalac nije dao. Greška je
     *                      deterministička - ponovni pokušaj slanja ne bi pomogao, pa potrošač
     *                      poruku odmah odbacuje na dead-letter red.
     */
    public String render(String text, Map<String, String> parameters) {
        return PLACEHOLDER.matcher(text).replaceAll(match -> {
            String name = match.group(1);
            String value = parameters.get(name);
            if (value == null) {
                throw ApiException.badRequest("Šablon zahteva parametar " + name + ", a vrednost nije data");
            }
            // Vrednost može da sadrži $ ili \, što bi se inače tumačilo kao referenca na grupu.
            return Matcher.quoteReplacement(value);
        });
    }

    /** Imena parametara u tekstu, u redosledu pojavljivanja. */
    public Set<String> placeholders(String text) {
        Set<String> names = new LinkedHashSet<>();
        var matcher = PLACEHOLDER.matcher(text);
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names;
    }
}
