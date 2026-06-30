package com.vn.sodu.ui.service;

import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class StaticPageHtmlSanitizer {

    private static final Set<String> ALLOWED_TAGS = Set.of(
            "p", "br", "strong", "b", "em", "i", "u", "s",
            "blockquote", "ol", "ul", "li", "span",
            "h1", "h2", "h3", "h4", "h5", "h6",
            "a", "pre", "code", "img"
    );
    private static final Set<String> VOID_TAGS = Set.of("br", "img");
    private static final Set<String> ALLOWED_ATTRIBUTES = Set.of(
            "href", "target", "title", "class", "src", "alt", "width", "height"
    );
    private static final Pattern DANGEROUS_BLOCKS = Pattern.compile(
            "(?is)<\\s*(script|style|iframe|object|embed|meta|link|svg|math)[^>]*>.*?<\\s*/\\s*\\1\\s*>"
    );
    private static final Pattern DANGEROUS_VOID_TAGS = Pattern.compile(
            "(?is)<\\s*(script|style|iframe|object|embed|meta|link|svg|math)[^>]*?/?>"
    );
    private static final Pattern TAG_PATTERN = Pattern.compile("(?is)<\\s*(/)?\\s*([a-z0-9]+)([^>]*)>");
    private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile(
            "([a-zA-Z_:][-a-zA-Z0-9_:.]*)\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s\"'>/]+)"
    );

    public String sanitize(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        String withoutDangerousBlocks = DANGEROUS_BLOCKS.matcher(html).replaceAll("");
        withoutDangerousBlocks = DANGEROUS_VOID_TAGS.matcher(withoutDangerousBlocks).replaceAll("");
        Matcher matcher = TAG_PATTERN.matcher(withoutDangerousBlocks);
        StringBuffer sanitized = new StringBuffer();

        while (matcher.find()) {
            String closing = matcher.group(1);
            String tag = matcher.group(2).toLowerCase(Locale.ROOT);
            String attributes = matcher.group(3);

            String replacement;
            if (!ALLOWED_TAGS.contains(tag)) {
                replacement = "";
            } else if (closing != null) {
                replacement = VOID_TAGS.contains(tag) ? "" : "</" + tag + ">";
            } else {
                replacement = "<" + tag + sanitizeAttributes(tag, attributes) + (VOID_TAGS.contains(tag) ? ">" : ">");
            }
            matcher.appendReplacement(sanitized, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sanitized);
        return sanitized.toString().trim();
    }

    private String sanitizeAttributes(String tag, String attributes) {
        if (attributes == null || attributes.isBlank()) {
            return tag.equals("a") ? " rel=\"noopener noreferrer\"" : "";
        }

        Matcher matcher = ATTRIBUTE_PATTERN.matcher(attributes);
        StringBuilder sanitized = new StringBuilder();
        while (matcher.find()) {
            String name = matcher.group(1).toLowerCase(Locale.ROOT);
            if (name.startsWith("on") || !ALLOWED_ATTRIBUTES.contains(name) || !isAttributeAllowedForTag(tag, name)) {
                continue;
            }

            String rawValue = matcher.group(2);
            String value = unquote(rawValue).trim();
            if (isUnsafeUrlAttribute(name, value)) {
                continue;
            }
            sanitized.append(' ')
                    .append(name)
                    .append("=\"")
                    .append(HtmlUtils.htmlEscape(value))
                    .append('"');
        }

        if (tag.equals("a")) {
            sanitized.append(" rel=\"noopener noreferrer\"");
        }
        return sanitized.toString();
    }

    private boolean isAttributeAllowedForTag(String tag, String name) {
        if (name.equals("href") || name.equals("target")) {
            return tag.equals("a");
        }
        if (name.equals("src") || name.equals("alt") || name.equals("width") || name.equals("height")) {
            return tag.equals("img");
        }
        return true;
    }

    private boolean isUnsafeUrlAttribute(String name, String value) {
        if (!name.equals("href") && !name.equals("src")) {
            return false;
        }
        String normalized = value.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
        return normalized.startsWith("javascript:")
                || normalized.startsWith("vbscript:")
                || normalized.startsWith("data:");
    }

    private String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
