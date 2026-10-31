// Copyright 2026 Hitesh Soliwal and contributors
// Modified 2026-10-18: map the pinned Synexia CPU tool into its existing M3JDK21 module name.
// SPDX-License-Identifier: Apache-2.0
package com.m3.tools.text.normalize;
import java.text.Normalizer;
import java.util.Objects;
/** Unicode normalization is explicit: no hidden alteration of M3 String identity. */
public final class UnicodeNormalizer {
    private UnicodeNormalizer() { }
    public static String normalize(String text, Normalizer.Form form) {
        return Normalizer.normalize(Objects.requireNonNull(text), Objects.requireNonNull(form));
    }
    public static String nfkc(String text) { return normalize(text, Normalizer.Form.NFKC); }
    public static void main(String[] args) {
        if (args.length == 1 && args[0].equals("--self-test")) {
            if (!nfkc("Ａ̊").equals("Å")) throw new AssertionError("NFKC");
            if (!normalize("é", Normalizer.Form.NFC).equals("é")) throw new AssertionError("NFC");
            System.out.println("M3_JMOD_TEST_PASS text.normalize");
        } else { System.out.println(nfkc(String.join(" ", args))); }
    }
}
