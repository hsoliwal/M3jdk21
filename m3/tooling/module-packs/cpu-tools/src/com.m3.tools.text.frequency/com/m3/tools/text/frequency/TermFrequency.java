// Copyright 2026 Hitesh Soliwal and contributors
// Modified 2026-10-18: map the pinned Synexia CPU tool into its existing M3JDK21 module name.
// SPDX-License-Identifier: Apache-2.0
package com.m3.tools.text.frequency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
/** Term counts over caller-supplied tokens; canonical tokenization remains separate. */
public final class TermFrequency {
    private TermFrequency() { }
    public static Map<String,Integer> count(List<String> tokens) {
        Objects.requireNonNull(tokens);
        Map<String,Integer> output=new LinkedHashMap<>();
        for(String token:tokens) output.merge(Objects.requireNonNull(token),1,Math::addExact);
        return Map.copyOf(output);
    }
    public static void main(String[] args) {
        if(args.length==1&&args[0].equals("--self-test")) {
            if(count(List.of("a","b","a")).get("a")!=2) throw new AssertionError("term count");
            if(!count(List.of()).isEmpty()) throw new AssertionError("empty");
            System.out.println("M3_JMOD_TEST_PASS text.frequency");
        } else System.out.println(count(List.of(args)));
    }
}
