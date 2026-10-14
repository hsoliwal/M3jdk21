// SPDX-License-Identifier: Apache-2.0
package com.m3.compiletime;

/** Deterministic, reversible TSV cell escaping for arbitrary UTF-16 text. */
public final class M3Tsv {
    private M3Tsv() {}

    /** Escapes backslash, controls, non-ASCII and lone surrogates as Java-style escapes. */
    public static String escape(String text) {
        StringBuilder out = new StringBuilder(text.length() + 8);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '\t' -> out.append("\\t");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                default -> {
                    if (c < 0x20 || c > 0x7e) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }

    /** Inverse of {@link #escape(String)}. */
    public static String unescape(String cell) {
        StringBuilder out = new StringBuilder(cell.length());
        for (int i = 0; i < cell.length(); i++) {
            char c = cell.charAt(i);
            if (c != '\\' || i + 1 >= cell.length()) {
                out.append(c);
                continue;
            }
            char e = cell.charAt(++i);
            switch (e) {
                case 't' -> out.append('\t');
                case 'n' -> out.append('\n');
                case 'r' -> out.append('\r');
                case 'u' -> {
                    out.append((char) Integer.parseInt(cell.substring(i + 1, i + 5), 16));
                    i += 4;
                }
                default -> out.append(e);
            }
        }
        return out.toString();
    }
}
