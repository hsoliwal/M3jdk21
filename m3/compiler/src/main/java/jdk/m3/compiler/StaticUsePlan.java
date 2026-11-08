// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compiler;

import java.util.Objects;

/**
 * Pure, deterministic compile-time admission decision from compiler-proven facts.
 *
 * <p>Annotations are candidate intent, never evidence. Static reference sites count source
 * uses, NOT runtime references or GC reachability. The compiler must supply attributed
 * escape, effect, identity, monitor, reflection and native/JNI evidence. Unknown is refused.
 * No ordinary Java bytecode is rewritten by this class.</p>
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/d5ad5090d87ef8477a56317e60ac0d29c65029f7/synexia-mindex/runtime/src/main/java/com/synexia/mindex/virtual/M3StaticUsePlan.java blob ce3c88da3307; recipe m3/compiler/static-use/recipe/static-use-plan-20261107/relocate.py v1
public final class StaticUsePlan {
    public enum Escape { PROVEN_LOCAL, PUBLISHED, UNKNOWN }
    public enum Effect { PARENT_BITS_ONLY, READONLY_PARENT, EXTERNAL_EFFECT, UNKNOWN }
    public enum Decision { DIRECT_PARENT, LAZY_HANDLE, MATERIALIZE, REFUSE }

    public record Facts(int staticUseSites, Escape escape, Effect effect,
            boolean identityObserved, boolean monitorObserved, boolean reflectionObserved,
            boolean nativeOrJniObserved, boolean serializationObserved,
            boolean referenceReachabilityObserved, boolean subclassHookObserved,
            boolean argumentOrExceptionOrderingProven, boolean ownerLifetimeProven) {
        public Facts {
            if (staticUseSites < 0) throw new IllegalArgumentException("negative static sites");
            Objects.requireNonNull(escape, "escape");
            Objects.requireNonNull(effect, "effect");
        }

        public boolean needsConcreteJavaObject() {
            return identityObserved || monitorObserved || reflectionObserved
                    || nativeOrJniObserved || serializationObserved
                    || referenceReachabilityObserved || subclassHookObserved;
        }
    }

    public record Plan(Decision decision, int staticUseSites, String reason,
            boolean runtimeMemorySampleRequired) {
        public Plan {
            Objects.requireNonNull(decision, "decision");
            Objects.requireNonNull(reason, "reason");
            if (staticUseSites < 0) throw new IllegalArgumentException("negative static sites");
        }
    }

    private StaticUsePlan() {}

    /** Compile once; runtime does not recalculate effect or escape eligibility per call. */
    public static Plan evaluate(Facts facts) {
        Objects.requireNonNull(facts, "facts");
        if (!facts.argumentOrExceptionOrderingProven() || !facts.ownerLifetimeProven()) {
            return new Plan(Decision.REFUSE, facts.staticUseSites(),
                    "ordering-or-owner-lifetime-unproven", false);
        }
        if (facts.escape() == Escape.UNKNOWN || facts.effect() == Effect.UNKNOWN) {
            return new Plan(Decision.REFUSE, facts.staticUseSites(),
                    "escape-or-effect-unproven", false);
        }
        if (facts.needsConcreteJavaObject() || facts.escape() == Escape.PUBLISHED) {
            return new Plan(Decision.MATERIALIZE, facts.staticUseSites(),
                    "observable-java-reference", false);
        }
        if (facts.effect() == Effect.PARENT_BITS_ONLY
                && facts.escape() == Escape.PROVEN_LOCAL) {
            return new Plan(Decision.DIRECT_PARENT, facts.staticUseSites(),
                    "proven-parent-bit-effect", false);
        }
        if (facts.effect() == Effect.READONLY_PARENT
                && facts.escape() == Escape.PROVEN_LOCAL) {
            return new Plan(Decision.LAZY_HANDLE, facts.staticUseSites(),
                    "read-only-lazy-projection", false);
        }
        return new Plan(Decision.MATERIALIZE, facts.staticUseSites(),
                "external-effect-requires-original-execution", false);
    }

    /** A runtime pressure sample belongs at physical growth/page boundaries, not every add. */
    public static boolean mustSampleAtGrowth(int required, int physicalCapacity) {
        if (required < 0 || physicalCapacity < 0)
            throw new IllegalArgumentException("negative capacity");
        return required > physicalCapacity;
    }

    /** Opt-in compiler analysis request; this annotation makes no ownership claim. */
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.SOURCE)
    @java.lang.annotation.Target(java.lang.annotation.ElementType.METHOD)
    public @interface Analyze { }

    /**
     * Generated once per method, on a companion class, never per runtime object.
     * CLASS retention makes this available to class-file tools without reflection.
     * Source identity is not semantic proof: consumers must revalidate the analyzer,
     * source and dependencies before using any claim for a transformation.
     */
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.CLASS)
    @java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE)
    public @interface CompilerSummary {
        String analyzer();
        String sourceSha256();
        String methodKey();
        String[] loopBounds();
        String[] dynamicInputs();
        String[] unresolved();
    }

    public enum Relation { LT, LE, GT, GE }

    /**
     * Immutable int-induction shape. The bound alone may be supplied at runtime.
     * This is a condition-only body-entry bound per loop entry, not an assertion
     * that the body terminates, cannot throw, or has no earlier exits.
     */
    public record IntLoop(int start, int step, Relation relation) {
        public IntLoop {
            Objects.requireNonNull(relation, "relation");
            boolean ascending = relation == Relation.LT || relation == Relation.LE;
            if (step == 0 || ascending != (step > 0))
                throw new IllegalArgumentException("induction direction does not approach exit");
        }

        /** O(1), no allocation. Refuses wraparound rather than inventing a bound. */
        public long conditionBound(int bound) {
            boolean ascending = step > 0;
            boolean inclusive = relation == Relation.LE || relation == Relation.GE;
            long distance = ascending ? (long) bound - start : (long) start - bound;
            if (distance < 0 || distance == 0 && !inclusive) return 0;
            long magnitude = ascending ? step : -(long) step;
            long entries = inclusive ? distance / magnitude + 1
                    : (distance + magnitude - 1) / magnitude;
            // The update after the last body execution must also be representable.
            long exit = start + entries * step;
            if (exit < Integer.MIN_VALUE || exit > Integer.MAX_VALUE)
                throw new ArithmeticException("int induction wraps before condition exit");
            return entries;
        }
    }

    public enum ReferenceMode {
        NO_WRAPPER_COUNT, OWNER_EDGE_COUNT_REQUIRED, KEEP_JVM_REACHABILITY, UNKNOWN
    }

    /**
     * Reuse the existing escape/effect/ordering owner; do not turn source-use counts
     * into dynamic counts. Even an eliminated wrapper may share backing whose owner
     * edges still need counting. This decision never frees a Java object or backing.
     */
    public static ReferenceMode referenceMode(Facts facts, boolean sharedBacking) {
        return switch (evaluate(facts).decision()) {
            case REFUSE -> ReferenceMode.UNKNOWN;
            case MATERIALIZE -> ReferenceMode.KEEP_JVM_REACHABILITY;
            case DIRECT_PARENT, LAZY_HANDLE -> sharedBacking
                    ? ReferenceMode.OWNER_EDGE_COUNT_REQUIRED : ReferenceMode.NO_WRAPPER_COUNT;
        };
    }
}
