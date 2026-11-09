/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.regex.Pattern;

/** Diagnoses the exact prerequisite of an inherited optional-facts assertion. */
public final class M3RegexOptionalFactsProbe {
    public static void main(String[] args) throws Exception {
        String source = String.join("", "a", "\ud83d", "\ude42", "b");
        Class<?> storage = Class.forName("java.lang.M3String");
        Field backing = String.class.getDeclaredField("m3");
        backing.setAccessible(true);
        Object owner = backing.get(source);
        if (owner == null) throw new AssertionError("required split-pair M3 owner");
        Method decision = Class.forName("java.lang.M3StringRegexDp")
                .getDeclaredMethod("tryMatch", storage, String.class);
        decision.setAccessible(true);
        Method facts = storage.getDeclaredMethod("facts");
        facts.setAccessible(true);
        int possible = (Integer) decision.invoke(null, owner, "a.*b");
        int before = (Integer) decision.invoke(null, owner, "a*Q.*");
        facts.invoke(owner);
        int after = (Integer) decision.invoke(null, owner, "a*Q.*");
        boolean oracle = Pattern.compile("a*Q.*").matcher(new StringBuilder(source)).matches();
        if (possible != -1 || before != -1 || after != 0 || oracle || source.matches("a*Q.*")) {
            throw new AssertionError("optional facts did not preserve exact UNKNOWN/reject contract");
        }
        System.out.println("M3_OPTIONAL_FACTS_PRECONDITION_PASS possible=" + possible
                + " before=" + before + " prepared=" + after + " pattern=" + oracle);
    }
}
