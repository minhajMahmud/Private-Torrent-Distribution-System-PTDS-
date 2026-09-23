package com.ptds.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BencodeEncoderTest {

    @Test
    void encodesStrings() {
        assertThat(new String(BencodeEncoder.encode("spam"), StandardCharsets.US_ASCII)).isEqualTo("4:spam");
    }

    @Test
    void encodesIntegers() {
        assertThat(new String(BencodeEncoder.encode(42), StandardCharsets.US_ASCII)).isEqualTo("i42e");
    }

    @Test
    void encodesLists() {
        assertThat(new String(BencodeEncoder.encode(List.of("spam", "eggs")), StandardCharsets.US_ASCII))
                .isEqualTo("l4:spam4:eggse");
    }

    @Test
    void encodesDictionariesWithSortedKeys() {
        Map<String, Object> dict = new LinkedHashMap<>();
        dict.put("spam", "eggs");
        dict.put("cow", "moo");

        // Keys must be sorted lexicographically regardless of insertion order (BEP 3).
        assertThat(new String(BencodeEncoder.encode(dict), StandardCharsets.US_ASCII))
                .isEqualTo("d3:cow3:moo4:spam4:eggse");
    }

    @Test
    void encodesByteArraysByLength() {
        byte[] pieces = new byte[]{1, 2, 3};
        byte[] encoded = BencodeEncoder.encode(pieces);
        // "3:" prefix followed by the 3 raw bytes
        assertThat(encoded).isEqualTo(new byte[]{'3', ':', 1, 2, 3});
    }
}
