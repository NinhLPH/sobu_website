package com.vn.sodu.request;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlainTextJsonConverterTest {

    private final PlainTextJsonConverter converter = new PlainTextJsonConverter();

    @Test
    void serializesPlainTextToValidJsonString() {
        String json = converter.convertToDatabaseColumn("Need fast delivery");

        assertThat(json).isEqualTo("\"Need fast delivery\"");
    }

    @Test
    void deserializesJsonStringBackToPlainText() {
        String value = converter.convertToEntityAttribute("\"Need fast delivery\"");

        assertThat(value).isEqualTo("Need fast delivery");
    }

    @Test
    void preservesLegacyPlainTextRowsWhenJsonParsingFails() {
        String value = converter.convertToEntityAttribute("Need fast delivery");

        assertThat(value).isEqualTo("Need fast delivery");
    }
}
