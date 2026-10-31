/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 *
 * Independent public-API discriminator for a dense-relocation boundary in the
 * pinned Guava Android 33.4.8 donor. No donor implementation body is copied.
 * Compilation and actual Android/JRE provider runs belong to the root runner.
 */
package com.m3.collections.guava;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Records both bodies before independently enforcing legal bijection results. */
public final class InverseForcePutDenseOrderDiscriminator {
    private static int checks;
    private static int scenarios;
    private static int violations;

    private InverseForcePutDenseOrderDiscriminator() { }

    private record Token(String domain, int id) {
        @Override public int hashCode() { return 1; }
        @Override public String toString() { return domain + id; }
    }

    private record Attempt(String value, String exception) { }
    private record Observation(int size, List<String> forward, List<String> inverse,
            List<String> lookups, List<String> failures) { }

    @FunctionalInterface private interface Call { Object run(); }

    private static Token key(int id) { return new Token("k", id); }
    private static Token value(int id) { return new Token("v", id); }

    private static Attempt capture(Call call) {
        try { return new Attempt(String.valueOf(call.run()), null); }
        catch (RuntimeException failure) { return new Attempt(null, failure.getClass().getName()); }
    }

    private static void require(boolean condition, String name, List<String> failures) {
        checks++;
        if (!condition) { violations++; failures.add(name); }
    }

    private static List<String> entries(BiMap<Token, Token> map, String direction, List<String> failures) {
        List<String> result = new ArrayList<>();
        try {
            Iterator<Map.Entry<Token, Token>> iterator = map.entrySet().iterator();
            int limit = Math.max(0, map.size()) + 2;
            while (iterator.hasNext() && result.size() < limit) {
                Map.Entry<Token, Token> entry = iterator.next();
                result.add(entry.getKey() + "=" + entry.getValue());
            }
            if (iterator.hasNext()) failures.add(direction + ":iteration exceeds size+2 bound");
        } catch (RuntimeException failure) {
            failures.add(direction + ":" + failure.getClass().getName());
        }
        return result;
    }

    private static Observation observe(BiMap<Token, Token> map) {
        List<String> failures = new ArrayList<>();
        List<String> forward = entries(map, "forward", failures);
        List<String> inverse = entries(map.inverse(), "inverse", failures);
        List<String> lookups = new ArrayList<>();
        for (int id : new int[] {0, 1, 2, 3, 20, 99}) {
            Attempt get = capture(() -> map.get(key(id)));
            Attempt contains = capture(() -> map.containsKey(key(id)));
            Attempt inverseGet = capture(() -> map.inverse().get(value(id)));
            Attempt inverseContains = capture(() -> map.containsValue(value(id)));
            lookups.add("k" + id + ":get=" + get + ",contains=" + contains
                    + ";v" + id + ":inverseGet=" + inverseGet + ",contains=" + inverseContains);
        }
        return new Observation(map.size(), forward, inverse, lookups, failures);
    }

    private static LinkedHashMap<Token, Token> initialModel() {
        LinkedHashMap<Token, Token> result = new LinkedHashMap<>();
        for (int id = 0; id < 4; id++) result.put(key(id), value(id));
        return result;
    }

    private static BiMap<Token, Token> initial(boolean candidate) {
        BiMap<Token, Token> result = candidate ? M3HashBiMap.create(0) : HashBiMap.create(0);
        result.putAll(initialModel());
        return result;
    }

    private static List<String> expectedEntries(Map<Token, Token> model, boolean inverse) {
        List<String> result = new ArrayList<>();
        for (Map.Entry<Token, Token> entry : model.entrySet()) {
            result.add(inverse ? entry.getValue() + "=" + entry.getKey() : entry.getKey() + "=" + entry.getValue());
        }
        return result;
    }

    private static void validate(String at, BiMap<Token, Token> map, Map<Token, Token> model) {
        List<String> failures = new ArrayList<>();
        Observation observed = observe(map);
        require(observed.failures().isEmpty(), "iteration completes", failures);
        require(observed.size() == model.size(), "map size equals legal mapping count", failures);
        require(observed.forward().equals(expectedEntries(model, false)), "complete ordered forward tuples", failures);
        require(observed.inverse().equals(expectedEntries(model, true)), "complete ordered inverse tuples", failures);
        require(map.inverse().inverse() == map, "inverse involution identity", failures);
        for (int id : new int[] {0, 1, 2, 3, 20, 99}) {
            Token expectedValue = model.get(key(id));
            Token expectedKey = null;
            for (Map.Entry<Token, Token> entry : model.entrySet()) {
                if (Objects.equals(entry.getValue(), value(id))) expectedKey = entry.getKey();
            }
            Token inverseExpected = expectedKey;
            Attempt get = capture(() -> map.get(key(id)));
            Attempt contains = capture(() -> map.containsKey(key(id)));
            Attempt inverseGet = capture(() -> map.inverse().get(value(id)));
            Attempt inverseContains = capture(() -> map.containsValue(value(id)));
            require(get.equals(new Attempt(String.valueOf(expectedValue), null)), "forward lookup k" + id, failures);
            require(contains.equals(new Attempt(String.valueOf(model.containsKey(key(id))), null)), "forward presence k" + id, failures);
            require(inverseGet.equals(new Attempt(String.valueOf(inverseExpected), null)), "inverse lookup v" + id, failures);
            require(inverseContains.equals(new Attempt(String.valueOf(inverseExpected != null), null)), "inverse presence v" + id, failures);
        }
        System.out.println(at + " observation=" + observed + " legalExpected=" + expectedEntries(model, false)
                + " violations=" + failures);
    }

    private static LinkedHashMap<Token, Token> inverseForceModel(int owner, int replacement) {
        if (owner == replacement) return initialModel();
        LinkedHashMap<Token, Token> result = new LinkedHashMap<>();
        for (int slot = 0; slot < 4; slot++) {
            if (slot == replacement) result.put(key(replacement), value(owner));
            else if (slot != owner) result.put(key(slot), value(slot));
        }
        return result;
    }

    private static void existingValueCases(String runtime) {
        for (int owner = 0; owner < 4; owner++) {
            for (int replacement = 0; replacement < 4; replacement++) {
                scenarios++;
                String scenario = "existing-value owner=" + owner + " replacement=" + replacement;
                int suppliedValue = owner; int suppliedKey = replacement;
                BiMap<Token, Token> candidate = initial(true);
                BiMap<Token, Token> provider = initial(false);
                LinkedHashMap<Token, Token> model = initialModel();
                for (BiMap<Token, Token> map : List.of(candidate, provider)) {
                    String body = map == candidate ? "candidate" : "provider:" + runtime;
                    validate(scenario + " " + body + " before", map, model);
                    Observation before = observe(map);
                    Attempt ordinary = capture(() -> map.inverse().put(value(suppliedValue), key(suppliedKey)));
                    List<String> failures = new ArrayList<>();
                    String expectedError = suppliedValue == suppliedKey ? null : IllegalArgumentException.class.getName();
                    require(Objects.equals(ordinary.exception(), expectedError), "ordinary put rejection class", failures);
                    require(before.equals(observe(map)), "ordinary rejection/same mapping preserves complete state", failures);
                    Attempt forced = capture(() -> map.inverse().forcePut(value(suppliedValue), key(suppliedKey)));
                    require(forced.equals(new Attempt("k" + suppliedValue, null)), "forcePut succeeds and returns former key", failures);
                    System.out.println(scenario + " " + body + " ordinary=" + ordinary + " forced=" + forced
                            + " actionViolations=" + failures);
                }
                model = inverseForceModel(owner, replacement);
                validate(scenario + " candidate afterForce", candidate, model);
                validate(scenario + " provider:" + runtime + " afterForce", provider, model);
                System.out.println(scenario + " pairedAfterForceEqual=" + observe(candidate).equals(observe(provider)));
                Attempt candidateAppend = capture(() -> candidate.put(key(20), value(20)));
                Attempt providerAppend = capture(() -> provider.put(key(20), value(20)));
                List<String> failures = new ArrayList<>();
                require(candidateAppend.equals(new Attempt("null", null)), "candidate append succeeds", failures);
                require(providerAppend.equals(new Attempt("null", null)), "provider append succeeds", failures);
                model.put(key(20), value(20));
                System.out.println(scenario + " appendCandidate=" + candidateAppend + " appendProvider=" + providerAppend
                        + " actionViolations=" + failures);
                validate(scenario + " candidate afterAppend", candidate, model);
                validate(scenario + " provider:" + runtime + " afterAppend", provider, model);
                System.out.println(scenario + " pairedAfterAppendEqual=" + observe(candidate).equals(observe(provider)));
            }
        }
    }

    private static void absentValueRelocatedPredecessor(String runtime) {
        scenarios++;
        String scenario = "absent-value relocated predecessor: put0..3/remove0/inverse.forcePut(v99,k3)";
        LinkedHashMap<Token, Token> model = initialModel();
        model.remove(key(0));
        for (boolean owned : List.of(true, false)) {
            BiMap<Token, Token> map = initial(owned);
            String body = owned ? "candidate" : "provider:" + runtime;
            Attempt removed = capture(() -> map.remove(key(0)));
            List<String> failures = new ArrayList<>();
            require(removed.equals(new Attempt("v0", null)), "initial remove succeeds", failures);
            validate(scenario + " " + body + " beforeForce", map, model);
            Attempt forced = capture(() -> map.inverse().forcePut(value(99), key(3)));
            require(forced.equals(new Attempt("null", null)), "absent-value forcePut succeeds", failures);
            LinkedHashMap<Token, Token> after = new LinkedHashMap<>(model);
            after.put(key(3), value(99));
            System.out.println(scenario + " " + body + " forced=" + forced + " actionViolations=" + failures);
            validate(scenario + " " + body + " afterForce", map, after);
        }
    }

    public static void main(String[] args) {
        if (args.length > 1) throw new IllegalArgumentException("optional one actual artifact label");
        String runtime = args.length == 0 ? "UNSPECIFIED_PROVIDER_CLASSPATH" : args[0];
        System.out.println("BEGIN InverseForcePutDenseOrderDiscriminator runtime=" + runtime);
        existingValueCases(runtime);
        absentValueRelocatedPredecessor(runtime);
        System.out.println((violations == 0 ? "PASS" : "FAIL") + " InverseForcePutDenseOrderDiscriminator runtime="
                + runtime + " scenarios=" + scenarios + " independentChecks=" + checks + " violations=" + violations);
        if (violations != 0) throw new AssertionError("legal BiMap sequence violated; observations retained above");
    }
}
