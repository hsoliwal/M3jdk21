// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tooling.atoms;

import java.lang.module.ModuleFinder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Dependency-free Java 21 main proof, also executed in the Maven test lifecycle. */
public final class M3CompanionPlanTest {
    private static int checked;

    private M3CompanionPlanTest() { }

    private static void require(boolean condition, String description) {
        checked++;
        if (!condition) {
            throw new AssertionError(description);
        }
    }

    private static M3CompanionPlan.Request req(String module, String type) {
        return new M3CompanionPlan.Request(module, type);
    }

    private static M3CompanionPlan.Row sole(M3CompanionPlan.Request input) {
        return M3CompanionPlan.inspect(List.of(input), Set.of()).getFirst();
    }

    public static void main(String[] arguments) {
        var string = sole(req("java.base", "java.lang.String"));
        require(string.decision() == M3CompanionPlan.Decision.REVIEW, "String is candidate");
        require(string.companionName().equals("jdk.m3.companion.java.lang.M3StringOps"),
                "String is additive and not in java.lang");
        require(string.imageFingerprint().matches("[0-9a-f]{64}"), "host identity is hashed");

        var mapEntry = sole(req("java.base", "java.util.Map$Entry"));
        require(mapEntry.decision() == M3CompanionPlan.Decision.REVIEW, "nested public interface");
        require(mapEntry.companionName().equals("jdk.m3.companion.java.util.M3Map_EntryOps"),
                "nested name remains distinct from M3MapOps");

        var collection = sole(req("java.base", "java.util.ArrayList"));
        require(collection.decision() == M3CompanionPlan.Decision.REVIEW, "ArrayList candidate");
        require(collection.companionName().endsWith("M3ArrayListOps"), "conventional type suffix");

        var gui = sole(req("java.desktop", "javax.swing.JButton"));
        require(gui.decision() == M3CompanionPlan.Decision.REVIEW, "java.desktop candidate");

        var xmlApi = sole(req("java.xml", "org.w3c.dom.Node"));
        require(xmlApi.decision() == M3CompanionPlan.Decision.REVIEW,
                "java.xml exports public org.w3c.dom even without a java.* prefix");
        require(xmlApi.companionName().equals("jdk.m3.companion.org.w3c.dom.M3NodeOps"),
                "non-java package API uses the same deterministic name law");

        var saxApi = sole(req("java.xml", "org.xml.sax.InputSource"));
        require(saxApi.decision() == M3CompanionPlan.Decision.REVIEW,
                "java.xml exports org.xml.sax as a public API");

        // A full Java 21 image can install more modules than the boot layer resolves.
        var unresolved = ModuleFinder.ofSystem().findAll().stream()
                .map(reference -> reference.descriptor().name())
                .filter(name -> ModuleLayer.boot().findModule(name).isEmpty())
                .sorted().findFirst();
        unresolved.ifPresent(moduleName -> {
            var held = sole(req(moduleName, "java.lang.String"));
            require(held.decision() == M3CompanionPlan.Decision.HOLD_MODULE_NOT_RESOLVED,
                    "installed but unresolved module must be held, not marked nonexistent");
            require(held.imageFingerprint().isEmpty(),
                    "no unobserved source hash or image proof may be invented for a hold");
        });

        var internal = sole(req("java.base", "jdk.internal.misc.Unsafe"));
        require(internal.decision() == M3CompanionPlan.Decision.REFUSED_NOT_EXPORTED,
                "jdk internal must not be exported by candidate generation");

        var nonpublic = sole(req("java.base", "java.util.ArrayList$SubList"));
        require(nonpublic.decision() == M3CompanionPlan.Decision.REFUSED_NOT_PUBLIC,
                "nested internal type must not be exported");

        var wrongModule = sole(req("java.desktop", "java.lang.String"));
        require(wrongModule.decision() == M3CompanionPlan.Decision.REFUSED_TYPE,
                "wrong module is refused");

        var absentModule = sole(req("not.a.real.module", "java.lang.String"));
        require(absentModule.decision() == M3CompanionPlan.Decision.REFUSED_MODULE,
                "absent module is refused");

        var absentType = sole(req("java.base", "java.util.TypeNotPresentAnywhere"));
        require(absentType.decision() == M3CompanionPlan.Decision.REFUSED_TYPE,
                "absent class is refused");

        var reserved = M3CompanionPlan.inspect(List.of(req("java.base", "java.lang.String")),
                Set.of("jdk.m3.companion.java.lang.M3StringOps")).getFirst();
        require(reserved.decision() == M3CompanionPlan.Decision.REFUSED_EXISTING,
                "cannot create competing companion owner");

        var inputs = List.of(req("java.base", "java.lang.String"),
                req("java.base", "java.util.List"),
                req("java.base", "java.util.Map$Entry"),
                req("java.desktop", "javax.swing.JButton"),
                req("java.base", "java.lang.String"));
        var original = M3CompanionPlan.inspect(inputs, Set.of());
        var reverse = new ArrayList<>(inputs);
        Collections.reverse(reverse);
        var permuted = M3CompanionPlan.inspect(reverse, Set.of());
        require(original.equals(permuted), "input permutation does not change plan");
        require(original.size() == 4, "duplicates do not generate duplicate output");
        require(new HashSet<>(original.stream().map(M3CompanionPlan.Row::companionName).toList())
                .size() == original.size(), "companion FQNs distinct");
        require(original.equals(M3CompanionPlan.inspect(inputs, Set.of())),
                "fixed-point read-only planning");

        try {
            req("java.base", "java.lang.String;bad");
            throw new AssertionError("invalid source accepted");
        } catch (IllegalArgumentException expected) {
            require(true, "invalid input refused");
        }
        try {
            M3CompanionPlan.companionName("String");
            throw new AssertionError("ambiguous simple type accepted");
        } catch (IllegalArgumentException expected) {
            require(true, "unqualified type refused");
        }
        require(M3CompanionPlan.SCHEMA.equals("M3_COMPANION_CANDIDATE_V1"),
                "schema remains versioned");
        System.out.println("M3CompanionPlanTest PASS assertions=" + checked);
    }
}
