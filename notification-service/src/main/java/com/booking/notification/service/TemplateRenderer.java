package com.booking.notification.service;

import com.booking.common.exception.ApiException;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TemplateRenderer {
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_]+)\\s*}}");

    public String render(String text, Map<String, String> parameters) {
        return PLACEHOLDER.matcher(text).replaceAll(match -> {
            String name = match.group(1);
            String value = parameters.get(name);
            if (value == null) {
                throw ApiException.badRequest("Šablon zahteva parametar " + name + ", a vrednost nije data");
            }

            return Matcher.quoteReplacement(value);
        });
    }

    public Set<String> placeholders(String text) {
        Set<String> names = new LinkedHashSet<>();
        var matcher = PLACEHOLDER.matcher(text);
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names;
    }
}
