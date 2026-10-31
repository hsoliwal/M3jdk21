// Copyright 2026 Hitesh Soliwal and contributors
// Modified 2026-10-18: map the pinned Synexia CPU tool into its existing M3JDK21 module name.
// SPDX-License-Identifier: Apache-2.0
package com.m3.tools.text.regex;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
/** Uses the canonical JDK Pattern/Matcher contract, including zero-length matches. */
public final class RegexMatcher {
    private RegexMatcher() { }
    public record Hit(int start, int end, String group) { }
    public static List<Hit> find(String regex, CharSequence input, int maxMatches) {
        Objects.requireNonNull(regex); Objects.requireNonNull(input);
        if (maxMatches < 0) throw new IllegalArgumentException("negative match budget");
        var matcher = Pattern.compile(regex).matcher(input);
        List<Hit> hits = new ArrayList<>();
        while (hits.size() < maxMatches && matcher.find())
            hits.add(new Hit(matcher.start(), matcher.end(), matcher.group()));
        return List.copyOf(hits);
    }
    public static void main(String[] args) {
        if (args.length == 1 && args[0].equals("--self-test")) {
            if (find("a+", "baaa ac", 3).size() != 2) throw new AssertionError("regex count");
            if (find("", "ab", 9).size() != 3) throw new AssertionError("empty pattern");
            if (!find("a", "aba", 0).isEmpty()) throw new AssertionError("bounded");
            System.out.println("M3_JMOD_TEST_PASS text.regex");
        } else if (args.length >= 2) {
            find(args[0], args[1], 1000).forEach(h -> System.out.println(h.start()+"\t"+h.end()+"\t"+h.group()));
        } else { throw new IllegalArgumentException("regex and text arguments required"); }
    }
}
