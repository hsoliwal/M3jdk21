// Copyright 2026 Hitesh Soliwal and contributors
// Modified 2026-10-18: map the pinned Synexia CPU tool into its existing M3JDK21 module name.
// SPDX-License-Identifier: Apache-2.0
package com.m3.tools.text.diff;
import java.util.Objects;
/** Minimal single replacement bounds, not a general Myers or semantic diff. */
public final class TextDelta {
    private TextDelta() { }
    public record Replacement(int fromStart,int fromEnd,int toStart,int toEnd) { }
    public static Replacement changedSpan(String before,String after) {
        Objects.requireNonNull(before);Objects.requireNonNull(after);
        int prefix=0,limit=Math.min(before.length(),after.length());
        while(prefix<limit&&before.charAt(prefix)==after.charAt(prefix))prefix++;
        int suffix=0,remaining=Math.min(before.length()-prefix,after.length()-prefix);
        while(suffix<remaining&&before.charAt(before.length()-1-suffix)==after.charAt(after.length()-1-suffix))suffix++;
        return new Replacement(prefix,before.length()-suffix,prefix,after.length()-suffix);
    }
    public static void main(String[] args) {
        if(args.length==1&&args[0].equals("--self-test")){
            if(!changedSpan("abcde","abXde").equals(new Replacement(2,3,2,3)))throw new AssertionError("span");
            if(!changedSpan("abc","abc").equals(new Replacement(3,3,3,3)))throw new AssertionError("identical");
            System.out.println("M3_JMOD_TEST_PASS text.diff");
        }else if(args.length==2)System.out.println(changedSpan(args[0],args[1]));
        else throw new IllegalArgumentException("before and after required");
    }
}
