package com.ptds.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Tiny bencode encoder — just enough to build valid single-file .torrent
 * metainfo dictionaries (BEP 3) without pulling in a third-party torrent
 * library. Supports the subset bencode needs: byte strings, integers,
 * lists, and dictionaries (with lexicographically sorted keys, as the
 * spec requires).
 */
public final class BencodeEncoder {

    private BencodeEncoder() {}

    public static byte[] encode(Object value) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            write(value, out);
        } catch (IOException e) {
            throw new IllegalStateException("Bencode encoding failed", e);
        }
        return out.toByteArray();
    }

    @SuppressWarnings("unchecked")
    private static void write(Object value, ByteArrayOutputStream out) throws IOException {
        if (value instanceof byte[] bytes) {
            out.write(String.valueOf(bytes.length).getBytes(StandardCharsets.US_ASCII));
            out.write(':');
            out.write(bytes);
        } else if (value instanceof String s) {
            byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
            out.write(String.valueOf(bytes.length).getBytes(StandardCharsets.US_ASCII));
            out.write(':');
            out.write(bytes);
        } else if (value instanceof Number n) {
            out.write('i');
            out.write(String.valueOf(n.longValue()).getBytes(StandardCharsets.US_ASCII));
            out.write('e');
        } else if (value instanceof List<?> list) {
            out.write('l');
            for (Object item : list) write(item, out);
            out.write('e');
        } else if (value instanceof Map<?, ?> map) {
            // bencode dictionaries MUST have lexicographically sorted keys
            TreeMap<String, Object> sorted = new TreeMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                sorted.put(String.valueOf(e.getKey()), e.getValue());
            }
            out.write('d');
            for (Map.Entry<String, Object> e : sorted.entrySet()) {
                write(e.getKey(), out);
                write(e.getValue(), out);
            }
            out.write('e');
        } else {
            throw new IllegalArgumentException("Unsupported bencode value type: " + value.getClass());
        }
    }
}
