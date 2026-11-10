/* ******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
/*
 * TLOUOOIACI: The Law Of Universals Order Of Informational And Constructional Invariance
 *
 * The ordering from Crystalline to Amorphous is FIXED and INVARIANT.
 * This is the OrdinalSpine - the backbone ordering of all knowledge domains.
 *
 * CRYSTALLINE: Same input → ALWAYS same output (deterministic)
 * AMORPHOUS: Same input → Statistically similar output (stochastic)
 *
 * "Mass occurs before force can be made sense of.
 *  Acceleration occurs before force can be made sense of."
 *  - F = ma requires understanding m and a FIRST.
 */
package jdk.internal.m3.paradigm.contract;

import jdk.internal.m3.collections.PlaceholderLogger;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
/**
 * OrdinalSpine - The fixed ordering from Crystalline to Amorphous domains.
 *
 * <p>
 * Based on TLOUOOIACI (The Law Of Universals Order Of Informational And
 * Constructional Invariance), knowledge domains have a natural ordering
 * determined by their level of determinism.
 * </p>
 *
 * <h2>The Ordering Principle</h2>
 *
 * <pre>
 * CRYSTALLINE (C=1.0) ────────────────────► AMORPHOUS (C=0.0)
 *
 * Mathematics → Physics → Chemistry → Biology → Psychology → Philosophy
 *
 * Where C = Crystallinity coefficient:
 *   C = 1.0 : Pure determinism (same input → ALWAYS same output)
 *   C = 0.0 : Pure stochasticity (same input → unpredictable output)
 * </pre>
 *
 * <h2>Why This Order is INVARIANT</h2>
 * <ul>
 * <li>Physics REQUIRES mathematics (F=ma needs m, a defined first)</li>
 * <li>Chemistry REQUIRES physics (bonds need electron orbitals)</li>
 * <li>Biology REQUIRES chemistry (life needs molecules)</li>
 * <li>Psychology REQUIRES biology (mind needs brain)</li>
 * <li>Philosophy REQUIRES psychology (thought needs mind)</li>
 * </ul>
 *
 * <p>
 * This is the DEPENDENCY ORDER - you cannot understand later domains without
 * first understanding earlier domains. The order is constructional.
 * </p>
 *
 * @m3.pattern synexia:iop
 * @m3.algorithm Expose the 22 declared domain values, filter by stored tier or strict crystallinity comparisons, index by ordinal, and print the existing values.
 * @m3.dataStructures Noninstantiable utility class with Domain, Tier and MITCourse enums, scalar String constants and stream projections.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/OrdinalSpine.java}
 * blob {@code 14ab680d6307dffead4fc868e9db55bf4d36eff2} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class OrdinalSpine {
    private static final PlaceholderLogger log = PlaceholderLogger.of(OrdinalSpine.class);


    /**
     * Retain the existing separator text for spine log output.
     * @m3.pattern synexia:iop
     * @m3.algorithm Retain the existing separator text for spine log output.
     * @m3.dataStructures One static final String literal.
     */
    private static final String SEPARATOR_LINE = "═══════════════════════════════════════════════════════════════";
    /**
     * Retain the existing domain format text supplied unchanged to SLF4J logging.
     * @m3.pattern synexia:iop
     * @m3.algorithm Retain the existing domain format text supplied unchanged to SLF4J logging.
     * @m3.dataStructures One static final String literal containing percent-format placeholders.
     */
    private static final String FORMAT_DOMAIN = "  [%2d] %-20s (C=%.2f) %s";
    /**
     * Retain the existing tier format text supplied unchanged to SLF4J logging.
     * @m3.pattern synexia:iop
     * @m3.algorithm Retain the existing tier format text supplied unchanged to SLF4J logging.
     * @m3.dataStructures One static final String literal containing percent-format placeholders.
     */
    private static final String FORMAT_TIER = "── %s (C: %.2f-%.2f) ──";

    /**
     * Prevent ordinary external instantiation with a private no-op constructor.
     * @m3.pattern synexia:iop
     * @m3.algorithm Prevent ordinary external instantiation with a private no-op constructor.
     * @m3.dataStructures No instance fields or constructor arguments.
     */
    private OrdinalSpine() {
        // Utility class - no instantiation
    }

    // ════════════════════════════════════════════════════════════════════════
    // CRYSTALLINITY COEFFICIENT
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Crystallinity coefficient determines how deterministic a domain is.
     *
     * @param domain
     *            the knowledge domain
     * @return crystallinity coefficient [0.0, 1.0]
     * @m3.pattern synexia:fop
     * @m3.algorithm Return the supplied domain stored crystallinity without normalization; a null domain fails on field access.
     * @m3.dataStructures One Domain reference and its primitive double crystallinity field.
     */
    public static double crystallinity(Domain domain) {
        return domain.crystallinity;
    }

    // ════════════════════════════════════════════════════════════════════════
    // DOMAIN ENUMERATION - ORDERED FROM CRYSTALLINE TO AMORPHOUS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Knowledge domains ordered from Crystalline (deterministic) to Amorphous
     * (stochastic).
     *
     * <p>
     * The ordinal position IS the ordering. Lower ordinal = more crystalline.
     * </p>
      * @author Synexia
      * @since 1.0
     * @m3.pattern synexia:iop
     * @m3.algorithm Construct 22 domain values in declaration order; compare ordinal position separately from strict stored crystallinity, and retain each declared tier and example list.
     * @m3.dataStructures Enum singletons with final String name and description, double crystallinity, Tier reference and immutable List&lt;String> examples.
     */
    public enum Domain {
        // ══════════════════════════════════════════════════════════════════
        // TIER 0: PURE CRYSTALLINE (C = 1.0) - Axiomatic, Deterministic
        // ══════════════════════════════════════════════════════════════════

        /** Pure logic, axioms, proofs. No empirical uncertainty.
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish MATHEMATICS at domain ordinal 0 with stored crystallinity 1.0 and tier PURE_CRYSTALLINE; ordinal position and coefficient comparisons remain distinct.
         * @m3.dataStructures One Domain enum singleton with final descriptive strings, double coefficient, Tier reference and immutable example list.
         */
        MATHEMATICS(
                "Mathematics",
                1.0,
                Tier.PURE_CRYSTALLINE,
                "The language of the universe. Axioms → Theorems → Proofs.",
                "2 + 2 = 4",
                "∀ε>0, ∃δ>0",
                "Gödel incompleteness"),

        /** Universal physical constants. c, G, h, etc.
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish UNIVERSAL_CONSTANTS at domain ordinal 1 with stored crystallinity 1.0 and tier PURE_CRYSTALLINE; ordinal position and coefficient comparisons remain distinct.
         * @m3.dataStructures One Domain enum singleton with final descriptive strings, double coefficient, Tier reference and immutable example list.
         */
        UNIVERSAL_CONSTANTS(
                "Universal Constants",
                1.0,
                Tier.PURE_CRYSTALLINE,
                "Values that never change. Crystalline anchors of reality.",
                "c = 299,792,458 m/s",
                "G = 6.674×10⁻¹¹",
                "h = 6.626×10⁻³⁴"),

        // ══════════════════════════════════════════════════════════════════
        // TIER 1: HIGH CRYSTALLINE (C = 0.95-0.99) - Physical Laws
        // ══════════════════════════════════════════════════════════════════

        /** F=ma, E=mc², Maxwell's equations. Deterministic laws.
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish PHYSICS at domain ordinal 2 with stored crystallinity 0.98 and tier HIGH_CRYSTALLINE; ordinal position and coefficient comparisons remain distinct.
         * @m3.dataStructures One Domain enum singleton with final descriptive strings, double coefficient, Tier reference and immutable example list.
         */
        PHYSICS(
                "Physics",
                0.98,
                Tier.HIGH_CRYSTALLINE,
                "Deterministic laws governing matter and energy.",
                "F = ma",
                "E = mc²",
                "∇·E = ρ/ε₀"),

        /** Thermodynamics, quantum chemistry. Deterministic foundations.
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish CHEMISTRY at domain ordinal 3 with stored crystallinity 0.95 and tier HIGH_CRYSTALLINE; ordinal position and coefficient comparisons remain distinct.
         * @m3.dataStructures One Domain enum singleton with final descriptive strings, double coefficient, Tier reference and immutable example list.
         */
        CHEMISTRY(
                "Chemistry",
                0.95,
                Tier.HIGH_CRYSTALLINE,
                "Laws of atomic and molecular behavior.",
                "ΔG = ΔH - TΔS",
                "PV = nRT",
                "E = hν"),

        /** Planetary physics, geophysics.
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish EARTH_SCIENCES at domain ordinal 4 with stored crystallinity 0.9 and tier HIGH_CRYSTALLINE; ordinal position and coefficient comparisons remain distinct.
         * @m3.dataStructures One Domain enum singleton with final descriptive strings, double coefficient, Tier reference and immutable example list.
         */
        EARTH_SCIENCES(
                "Earth Sciences",
                0.90,
                Tier.HIGH_CRYSTALLINE,
                "Physical laws applied to Earth systems.",
                "Plate tectonics",
                "Atmospheric physics",
                "Oceanography"),

        // ══════════════════════════════════════════════════════════════════
        // TIER 2: SEMI-CRYSTALLINE (C = 0.70-0.89) - Statistical Laws
        // ══════════════════════════════════════════════════════════════════

        /** Applied physical sciences with engineering tolerances.
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish ENGINEERING at domain ordinal 5 with stored crystallinity 0.85 and tier SEMI_CRYSTALLINE; ordinal position and coefficient comparisons remain distinct.
         * @m3.dataStructures One Domain enum singleton with final descriptive strings, double coefficient, Tier reference and immutable example list.
         */
        ENGINEERING(
                "Engineering",
                0.85,
                Tier.SEMI_CRYSTALLINE,
                "Applied physics with practical tolerances.",
                "Mechanical",
                "Electrical",
                "Civil",
                "Chemical"),

        /** Evolution, genetics. Statistical mechanics of life.
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish BIOLOGY at domain ordinal 6 with stored crystallinity 0.75 and tier SEMI_CRYSTALLINE; ordinal position and coefficient comparisons remain distinct.
         * @m3.dataStructures One Domain enum singleton with final descriptive strings, double coefficient, Tier reference and immutable example list.
         */
        BIOLOGY(
                "Biology",
                0.75,
                Tier.SEMI_CRYSTALLINE,
                "Statistical mechanics of living systems.",
                "Natural selection",
                "Genetic inheritance",
                "Homeostasis"),

        /** Information as physical quantity. Shannon entropy.
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish INFORMATION_THEORY at domain ordinal 7 with stored crystallinity 0.8 and tier SEMI_CRYSTALLINE; ordinal position and coefficient comparisons remain distinct.
         * @m3.dataStructures One Domain enum singleton with final descriptive strings, double coefficient, Tier reference and immutable example list.
         */
        INFORMATION_THEORY(
                "Information Theory",
                0.80,
                Tier.SEMI_CRYSTALLINE,
                "Mathematical theory of communication.",
                "H = -Σp log p",
                "Channel capacity",
                "Compression"),

        // ══════════════════════════════════════════════════════════════════
        // TIER 3: TRANSITIONAL (C = 0.50-0.69) - Bridge Domains
        // ══════════════════════════════════════════════════════════════════

        /** Neuroscience. Brain → Mind bridge.
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish COGNITIVE_SCIENCE at domain ordinal 8 with stored crystallinity 0.6 and tier TRANSITIONAL; ordinal position and coefficient comparisons remain distinct.
         * @m3.dataStructures One Domain enum singleton with final descriptive strings, double coefficient, Tier reference and immutable example list.
         */
        COGNITIVE_SCIENCE(
                "Cognitive Science",
                0.60,
                Tier.TRANSITIONAL,
                "Bridge between brain and mind.",
                "Neural networks",
                "Memory",
                "Perception"),

        /** Computation theory. Turing, Church, Lambda calculus.
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish COMPUTER_SCIENCE at domain ordinal 9 with stored crystallinity 0.65 and tier TRANSITIONAL; ordinal position and coefficient comparisons remain distinct.
         * @m3.dataStructures One Domain enum singleton with final descriptive strings, double coefficient, Tier reference and immutable example list.
         */
        COMPUTER_SCIENCE(
                "Computer Science",
                0.65,
                Tier.TRANSITIONAL,
                "Theory of computation and algorithms.",
                "Turing machines",
                "P vs NP",
                "Lambda calculus"),

        /** Game theory, behavioral economics.
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish ECONOMICS at domain ordinal 10 with stored crystallinity 0.55 and tier TRANSITIONAL; ordinal position and coefficient comparisons remain distinct.
         * @m3.dataStructures One Domain enum singleton with final descriptive strings, double coefficient, Tier reference and immutable example list.
         */
        ECONOMICS(
                "Economics",
                0.55,
                Tier.TRANSITIONAL,
                "Mathematical models of human resource allocation.",
                "Supply and demand",
                "Nash equilibrium",
                "Market efficiency"),

        // ══════════════════════════════════════════════════════════════════
        // TIER 4: SEMI-AMORPHOUS (C = 0.30-0.49) - Human Behavior
        // ══════════════════════════════════════════════════════════════════

        /** Statistical models of human behavior.
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish PSYCHOLOGY at domain ordinal 11 with stored crystallinity 0.45 and tier SEMI_AMORPHOUS; ordinal position and coefficient comparisons remain distinct.
         * @m3.dataStructures One Domain enum singleton with final descriptive strings, double coefficient, Tier reference and immutable example list.
         */
        PSYCHOLOGY(
                "Psychology",
                0.45,
                Tier.SEMI_AMORPHOUS,
                "Statistical patterns in human behavior.",
                "Conditioning",
                "Cognitive biases",
                "Motivation"),

        /** Human societies and governance.
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish POLITICAL_SCIENCE at domain ordinal 12 with stored crystallinity 0.4 and tier SEMI_AMORPHOUS; ordinal position and coefficient comparisons remain distinct.
         * @m3.dataStructures One Domain enum singleton with final descriptive strings, double coefficient, Tier reference and immutable example list.
         */
        POLITICAL_SCIENCE(
                "Political Science",
                0.40,
                Tier.SEMI_AMORPHOUS,
                "Patterns in human governance and power.",
                "Voting theory",
                "Power dynamics",
                "Institutions"),

        /** Human cultures and adaptation.
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish ANTHROPOLOGY at domain ordinal 13 with stored crystallinity 0.35 and tier SEMI_AMORPHOUS; ordinal position and coefficient comparisons remain distinct.
         * @m3.dataStructures One Domain enum singleton with final descriptive strings, double coefficient, Tier reference and immutable example list.
         */
        ANTHROPOLOGY(
                "Anthropology",
                0.35,
                Tier.SEMI_AMORPHOUS,
                "Patterns in human cultural evolution.",
                "Cultural evolution",
                "Social structures",
                "Rituals"),

        /** Human communication systems.
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish LINGUISTICS at domain ordinal 14 with stored crystallinity 0.4 and tier SEMI_AMORPHOUS; ordinal position and coefficient comparisons remain distinct.
         * @m3.dataStructures One Domain enum singleton with final descriptive strings, double coefficient, Tier reference and immutable example list.
         */
        LINGUISTICS(
                "Linguistics",
                0.40,
                Tier.SEMI_AMORPHOUS,
                "Structure and evolution of human language.",
                "Grammar",
                "Semantics",
                "Pragmatics"),

        // ══════════════════════════════════════════════════════════════════
        // TIER 5: HIGH AMORPHOUS (C = 0.10-0.29) - Human Expression
        // ══════════════════════════════════════════════════════════════════

        /** Records of human events. Interpretation-dependent.
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish HISTORY at domain ordinal 15 with stored crystallinity 0.25 and tier HIGH_AMORPHOUS; ordinal position and coefficient comparisons remain distinct.
         * @m3.dataStructures One Domain enum singleton with final descriptive strings, double coefficient, Tier reference and immutable example list.
         */
        HISTORY(
                "History",
                0.25,
                Tier.HIGH_AMORPHOUS,
                "Interpretation of past human events.",
                "Causation debates",
                "Narrative construction",
                "Source criticism"),

        /** Human narrative and meaning-making.
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish LITERATURE at domain ordinal 16 with stored crystallinity 0.2 and tier HIGH_AMORPHOUS; ordinal position and coefficient comparisons remain distinct.
         * @m3.dataStructures One Domain enum singleton with final descriptive strings, double coefficient, Tier reference and immutable example list.
         */
        LITERATURE(
                "Literature",
                0.20,
                Tier.HIGH_AMORPHOUS,
                "Human expression through narrative.",
                "Metaphor",
                "Narrative structure",
                "Interpretation"),

        /** Aesthetic experience. Subjective by nature.
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish ARTS at domain ordinal 17 with stored crystallinity 0.15 and tier HIGH_AMORPHOUS; ordinal position and coefficient comparisons remain distinct.
         * @m3.dataStructures One Domain enum singleton with final descriptive strings, double coefficient, Tier reference and immutable example list.
         */
        ARTS("Arts", 0.15, Tier.HIGH_AMORPHOUS, "Human aesthetic expression.", "Visual arts", "Music", "Theater"),

        /** Designed environments.
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish ARCHITECTURE at domain ordinal 18 with stored crystallinity 0.25 and tier HIGH_AMORPHOUS; ordinal position and coefficient comparisons remain distinct.
         * @m3.dataStructures One Domain enum singleton with final descriptive strings, double coefficient, Tier reference and immutable example list.
         */
        ARCHITECTURE(
                "Architecture",
                0.25,
                Tier.HIGH_AMORPHOUS,
                "Design of human spaces.",
                "Form follows function",
                "Aesthetics",
                "Urban design"),

        // ══════════════════════════════════════════════════════════════════
        // TIER 6: PURE AMORPHOUS (C = 0.0-0.09) - Pure Abstraction
        // ══════════════════════════════════════════════════════════════════

        /** Questions about existence, knowledge, values.
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish PHILOSOPHY at domain ordinal 19 with stored crystallinity 0.05 and tier PURE_AMORPHOUS; ordinal position and coefficient comparisons remain distinct.
         * @m3.dataStructures One Domain enum singleton with final descriptive strings, double coefficient, Tier reference and immutable example list.
         */
        PHILOSOPHY(
                "Philosophy",
                0.05,
                Tier.PURE_AMORPHOUS,
                "Fundamental questions about reality and knowledge.",
                "Epistemology",
                "Ontology",
                "Ethics"),

        /** Belief systems. Faith-based.
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish THEOLOGY at domain ordinal 20 with stored crystallinity 0.02 and tier PURE_AMORPHOUS; ordinal position and coefficient comparisons remain distinct.
         * @m3.dataStructures One Domain enum singleton with final descriptive strings, double coefficient, Tier reference and immutable example list.
         */
        THEOLOGY(
                "Theology",
                0.02,
                Tier.PURE_AMORPHOUS,
                "Study of religious belief systems.",
                "Doctrine",
                "Faith",
                "Spirituality"),

        /** Questions beyond physics. Ultimate abstractions.
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish METAPHYSICS at domain ordinal 21 with stored crystallinity 0.01 and tier PURE_AMORPHOUS; ordinal position and coefficient comparisons remain distinct.
         * @m3.dataStructures One Domain enum singleton with final descriptive strings, double coefficient, Tier reference and immutable example list.
         */
        METAPHYSICS(
                "Metaphysics",
                0.01,
                Tier.PURE_AMORPHOUS,
                "Questions about the nature of reality itself.",
                "Being",
                "Time",
                "Causation");

        /** Human-readable name.
         * @m3.pattern synexia:iop
         * @m3.algorithm Retain the constructor-supplied human-readable domain name.
         * @m3.dataStructures One public final String reference.
         */
        public final String name;
        /** Crystallinity coefficient [0.0, 1.0].
         * @m3.pattern synexia:iop
         * @m3.algorithm Retain the constructor-supplied coefficient used by strict prerequisite and dependent comparisons without normalization.
         * @m3.dataStructures One public final double field.
         */
        public final double crystallinity;
        /** Classification tier.
         * @m3.pattern synexia:iop
         * @m3.algorithm Retain the constructor-supplied tier used by reference-equality filtering and tier headings.
         * @m3.dataStructures One public final Tier reference.
         */
        public final Tier tier;
        /** Brief description.
         * @m3.pattern synexia:iop
         * @m3.algorithm Retain the constructor-supplied domain description for logging.
         * @m3.dataStructures One public final String reference.
         */
        public final String description;
        /** Example concepts.
         * @m3.pattern synexia:iop
         * @m3.algorithm Retain the immutable list constructed from the constructor examples array with List.of.
         * @m3.dataStructures One public final List&lt;String> reference to an immutable list.
         */
        public final List<String> examples;

        /**
         * Assign name, crystallinity, tier and description directly, and construct an immutable example list with List.of(examples).
         * @m3.pattern synexia:iop
         * @m3.algorithm Assign name, crystallinity, tier and description directly, and construct an immutable example list with List.of(examples).
         * @m3.dataStructures String, double and Tier arguments, a String varargs array, final fields and immutable List&lt;String> examples.
         */
        Domain(String name, double crystallinity, Tier tier, String description, String... examples) {
            this.name = name;
            this.crystallinity = crystallinity;
            this.tier = tier;
            this.description = description;
            this.examples = List.of(examples);
        }

        /**
         * Returns true if this domain is more crystalline than another.
         *
         * @param other
         *            domain to compare
         * @return true if this is more crystalline (lower ordinal)
         * @m3.pattern synexia:fop
         * @m3.algorithm Return whether this ordinal is strictly less than the other ordinal; this method does not compare the stored crystallinity coefficients.
         * @m3.dataStructures Two Domain enum references and primitive int ordinals producing a boolean.
         */
        public boolean isMoreCrystallineThan(Domain other) {
            return this.ordinal() < other.ordinal();
        }

        /**
         * Returns true if this domain is a prerequisite for another. Crystalline
         * domains are prerequisites for amorphous domains.
         *
         * @param other
         *            domain to check
         * @return true if this domain must be understood before other
         * @m3.pattern synexia:fop
         * @m3.algorithm Return whether this stored crystallinity is strictly greater than the other coefficient; equal coefficients return false and null other fails on field access.
         * @m3.dataStructures Two Domain references and primitive double crystallinity fields producing a boolean.
         */
        public boolean isPrerequisiteFor(Domain other) {
            return this.crystallinity > other.crystallinity;
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // TIER CLASSIFICATION
    // ════════════════════════════════════════════════════════════════════════

    /** Tier classification for domains based on crystallinity.
     * @m3.pattern synexia:iop
     * @m3.algorithm Construct seven classification values with the declared minimum, maximum and description; these stored bounds perform no classification by themselves.
     * @m3.dataStructures Seven enum singletons with two final double bounds and one final String description.
     */
    public enum Tier {

        /** C = 1.0: Pure determinism. Axiomatic truths.
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish PURE_CRYSTALLINE at tier ordinal 0 with stored lower bound 1.0, upper bound 1.0 and its declared description.
         * @m3.dataStructures One Tier enum singleton with two final double bounds and a String description.
         */
        PURE_CRYSTALLINE(1.0, 1.0, "Axiomatic, deterministic, no empirical uncertainty"),
        /** C = 0.90-0.99: Physical laws. Universal constants.
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish HIGH_CRYSTALLINE at tier ordinal 1 with stored lower bound 0.9, upper bound 0.99 and its declared description.
         * @m3.dataStructures One Tier enum singleton with two final double bounds and a String description.
         */
        HIGH_CRYSTALLINE(0.90, 0.99, "Physical laws, universal constants"),
        /** C = 0.70-0.89: Statistical laws. Engineering tolerances.
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish SEMI_CRYSTALLINE at tier ordinal 2 with stored lower bound 0.7, upper bound 0.89 and its declared description.
         * @m3.dataStructures One Tier enum singleton with two final double bounds and a String description.
         */
        SEMI_CRYSTALLINE(0.70, 0.89, "Statistical laws, well-defined variance"),
        /** C = 0.50-0.69: Bridge domains. Math meets behavior.
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish TRANSITIONAL at tier ordinal 3 with stored lower bound 0.5, upper bound 0.69 and its declared description.
         * @m3.dataStructures One Tier enum singleton with two final double bounds and a String description.
         */
        TRANSITIONAL(0.50, 0.69, "Bridge between physical and behavioral"),
        /** C = 0.30-0.49: Human behavior. Statistical patterns.
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish SEMI_AMORPHOUS at tier ordinal 4 with stored lower bound 0.3, upper bound 0.49 and its declared description.
         * @m3.dataStructures One Tier enum singleton with two final double bounds and a String description.
         */
        SEMI_AMORPHOUS(0.30, 0.49, "Human behavior, cultural patterns"),
        /** C = 0.10-0.29: Human expression. Interpretation-dependent.
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish HIGH_AMORPHOUS at tier ordinal 5 with stored lower bound 0.1, upper bound 0.29 and its declared description.
         * @m3.dataStructures One Tier enum singleton with two final double bounds and a String description.
         */
        HIGH_AMORPHOUS(0.10, 0.29, "Human expression, subjective interpretation"),
        /** C = 0.0-0.09: Pure abstraction. Faith and meaning.
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish PURE_AMORPHOUS at tier ordinal 6 with stored lower bound 0.0, upper bound 0.09 and its declared description.
         * @m3.dataStructures One Tier enum singleton with two final double bounds and a String description.
         */
        PURE_AMORPHOUS(0.0, 0.09, "Pure abstraction, metaphysical questions");

        /**
         * Retain the constructor-supplied lower coefficient bound for tier metadata and logging.
         * @m3.pattern synexia:iop
         * @m3.algorithm Retain the constructor-supplied lower coefficient bound for tier metadata and logging.
         * @m3.dataStructures One public final double field.
         */
        public final double minCrystallinity;
        /**
         * Retain the constructor-supplied upper coefficient bound for tier metadata and logging.
         * @m3.pattern synexia:iop
         * @m3.algorithm Retain the constructor-supplied upper coefficient bound for tier metadata and logging.
         * @m3.dataStructures One public final double field.
         */
        public final double maxCrystallinity;
        /**
         * Retain the constructor-supplied classification description.
         * @m3.pattern synexia:iop
         * @m3.algorithm Retain the constructor-supplied classification description.
         * @m3.dataStructures One public final String reference.
         */
        public final String description;

        /**
         * Assign the supplied minimum, maximum and description directly to the corresponding final fields.
         * @m3.pattern synexia:iop
         * @m3.algorithm Assign the supplied minimum, maximum and description directly to the corresponding final fields.
         * @m3.dataStructures Two double arguments, one String argument and corresponding final fields.
         */
        Tier(double min, double max, String description) {
            this.minCrystallinity = min;
            this.maxCrystallinity = max;
            this.description = description;
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // ORDINAL SPINE OPERATIONS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Returns all domains in order from Crystalline to Amorphous.
     *
     * @return stream of domains in ordinal order
     * @m3.pattern synexia:fop
     * @m3.algorithm Return a sequential stream over Domain.values() in enum declaration order without sorting by crystallinity.
     * @m3.dataStructures Fresh Domain[] values array and Stream&lt;Domain> encounter order.
     */
    public static Stream<Domain> inOrder() {
        return Arrays.stream(Domain.values());
    }

    /**
     * Returns all domains in a given tier.
     *
     * @param tier
     *            the tier to filter by
     * @return stream of domains in that tier
     * @m3.pattern synexia:fop
     * @m3.algorithm Return an inOrder() stream filtered by reference equality with the requested tier; null matches no declared domain tier.
     * @m3.dataStructures Lazy Stream&lt;Domain> filter capturing one Tier reference.
     */
    public static Stream<Domain> byTier(Tier tier) {
        return inOrder().filter(d -> d.tier == tier);
    }

    /**
     * Returns the domain at a given ordinal position.
     *
     * @param ordinal
     *            the position (0 = most crystalline)
     * @return the domain at that position
     * @m3.pattern synexia:fop
     * @m3.algorithm Index the fresh Domain.values() array directly with the supplied ordinal; invalid indices retain array bounds exceptions.
     * @m3.dataStructures Fresh Domain[] values array, primitive int index and one Domain result.
     */
    public static Domain at(int ordinal) {
        return Domain.values()[ordinal];
    }

    /**
     * Returns total number of domains in the spine.
     *
     * @return domain count
     * @m3.pattern synexia:fop
     * @m3.algorithm Return the length of the fresh Domain.values() array.
     * @m3.dataStructures Fresh Domain[] values array and its primitive int length.
     */
    public static int size() {
        return Domain.values().length;
    }

    /**
     * Finds the prerequisite chain for a domain. All domains more crystalline than
     * the target are prerequisites.
     *
     * @param target
     *            the domain to find prerequisites for
     * @return stream of prerequisite domains (more crystalline)
     * @m3.pattern synexia:fop
     * @m3.algorithm Return domains whose stored crystallinity is strictly greater than the target coefficient in declaration order; null target fails when the filter is traversed.
     * @m3.dataStructures Lazy ordered Stream&lt;Domain> filter capturing one target Domain reference.
     */
    public static Stream<Domain> prerequisites(Domain target) {
        return inOrder().filter(d -> d.crystallinity > target.crystallinity);
    }

    /**
     * Finds domains that depend on a given domain. All domains less crystalline
     * than the source depend on it.
     *
     * @param source
     *            the domain to find dependents for
     * @return stream of dependent domains (less crystalline)
     * @m3.pattern synexia:fop
     * @m3.algorithm Return domains whose stored crystallinity is strictly less than the source coefficient in declaration order; null source fails when the filter is traversed.
     * @m3.dataStructures Lazy ordered Stream&lt;Domain> filter capturing one source Domain reference.
     */
    public static Stream<Domain> dependents(Domain source) {
        return inOrder().filter(d -> d.crystallinity < source.crystallinity);
    }

    // ════════════════════════════════════════════════════════════════════════
    // MIT COURSE MAPPING (Reference Implementation)
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Maps MIT course numbers to OrdinalSpine domains. This is a reference mapping
     * based on MIT's curriculum structure.
     * @m3.pattern synexia:iop
     * @m3.algorithm Construct 23 course mappings in declaration order and return each mapped domain crystallinity; course numbers may repeat.
     * @m3.dataStructures Enum singletons with final int number, String name and Domain reference.
     */
    public enum MITCourse {

        /** Course 18 - Mathematics
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish COURSE_18 at course-mapping ordinal 0 with course number 18 and mapped domain MATHEMATICS.
         * @m3.dataStructures One MITCourse enum singleton with final int number, String name and Domain reference.
         */
        COURSE_18(18, "Mathematics", Domain.MATHEMATICS),
        /** Course 8 - Physics
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish COURSE_8 at course-mapping ordinal 1 with course number 8 and mapped domain PHYSICS.
         * @m3.dataStructures One MITCourse enum singleton with final int number, String name and Domain reference.
         */
        COURSE_8(8, "Physics", Domain.PHYSICS),
        /** Course 5 - Chemistry
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish COURSE_5 at course-mapping ordinal 2 with course number 5 and mapped domain CHEMISTRY.
         * @m3.dataStructures One MITCourse enum singleton with final int number, String name and Domain reference.
         */
        COURSE_5(5, "Chemistry", Domain.CHEMISTRY),
        /** Course 12 - Earth, Atmospheric, Planetary Sciences
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish COURSE_12 at course-mapping ordinal 3 with course number 12 and mapped domain EARTH_SCIENCES.
         * @m3.dataStructures One MITCourse enum singleton with final int number, String name and Domain reference.
         */
        COURSE_12(12, "Earth Sciences", Domain.EARTH_SCIENCES),
        /** Course 22 - Nuclear Science and Engineering
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish COURSE_22 at course-mapping ordinal 4 with course number 22 and mapped domain ENGINEERING.
         * @m3.dataStructures One MITCourse enum singleton with final int number, String name and Domain reference.
         */
        COURSE_22(22, "Nuclear Engineering", Domain.ENGINEERING),
        /** Course 3 - Materials Science and Engineering
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish COURSE_3 at course-mapping ordinal 5 with course number 3 and mapped domain ENGINEERING.
         * @m3.dataStructures One MITCourse enum singleton with final int number, String name and Domain reference.
         */
        COURSE_3(3, "Materials Science", Domain.ENGINEERING),
        /** Course 2 - Mechanical Engineering
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish COURSE_2 at course-mapping ordinal 6 with course number 2 and mapped domain ENGINEERING.
         * @m3.dataStructures One MITCourse enum singleton with final int number, String name and Domain reference.
         */
        COURSE_2(2, "Mechanical Engineering", Domain.ENGINEERING),
        /** Course 6 - EECS
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish COURSE_6 at course-mapping ordinal 7 with course number 6 and mapped domain COMPUTER_SCIENCE.
         * @m3.dataStructures One MITCourse enum singleton with final int number, String name and Domain reference.
         */
        COURSE_6(6, "EECS", Domain.COMPUTER_SCIENCE),
        /** Course 1 - Civil and Environmental Engineering
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish COURSE_1 at course-mapping ordinal 8 with course number 1 and mapped domain ENGINEERING.
         * @m3.dataStructures One MITCourse enum singleton with final int number, String name and Domain reference.
         */
        COURSE_1(1, "Civil Engineering", Domain.ENGINEERING),
        /** Course 16 - Aeronautics and Astronautics
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish COURSE_16 at course-mapping ordinal 9 with course number 16 and mapped domain ENGINEERING.
         * @m3.dataStructures One MITCourse enum singleton with final int number, String name and Domain reference.
         */
        COURSE_16(16, "Aeronautics", Domain.ENGINEERING),
        /** Course 10 - Chemical Engineering
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish COURSE_10 at course-mapping ordinal 10 with course number 10 and mapped domain ENGINEERING.
         * @m3.dataStructures One MITCourse enum singleton with final int number, String name and Domain reference.
         */
        COURSE_10(10, "Chemical Engineering", Domain.ENGINEERING),
        /** Course 7 - Biology
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish COURSE_7 at course-mapping ordinal 11 with course number 7 and mapped domain BIOLOGY.
         * @m3.dataStructures One MITCourse enum singleton with final int number, String name and Domain reference.
         */
        COURSE_7(7, "Biology", Domain.BIOLOGY),
        /** Course 20 - Biological Engineering
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish COURSE_20 at course-mapping ordinal 12 with course number 20 and mapped domain BIOLOGY.
         * @m3.dataStructures One MITCourse enum singleton with final int number, String name and Domain reference.
         */
        COURSE_20(20, "Biological Engineering", Domain.BIOLOGY),
        /** Course 9 - Brain and Cognitive Sciences
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish COURSE_9 at course-mapping ordinal 13 with course number 9 and mapped domain COGNITIVE_SCIENCE.
         * @m3.dataStructures One MITCourse enum singleton with final int number, String name and Domain reference.
         */
        COURSE_9(9, "Cognitive Science", Domain.COGNITIVE_SCIENCE),
        /** Course 14 - Economics
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish COURSE_14 at course-mapping ordinal 14 with course number 14 and mapped domain ECONOMICS.
         * @m3.dataStructures One MITCourse enum singleton with final int number, String name and Domain reference.
         */
        COURSE_14(14, "Economics", Domain.ECONOMICS),
        /** Course 15 - Management (Sloan)
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish COURSE_15 at course-mapping ordinal 15 with course number 15 and mapped domain ECONOMICS.
         * @m3.dataStructures One MITCourse enum singleton with final int number, String name and Domain reference.
         */
        COURSE_15(15, "Management", Domain.ECONOMICS),
        /** Course 17 - Political Science
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish COURSE_17 at course-mapping ordinal 16 with course number 17 and mapped domain POLITICAL_SCIENCE.
         * @m3.dataStructures One MITCourse enum singleton with final int number, String name and Domain reference.
         */
        COURSE_17(17, "Political Science", Domain.POLITICAL_SCIENCE),
        /** Course 21A - Anthropology
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish COURSE_21A at course-mapping ordinal 17 with course number 21 and mapped domain ANTHROPOLOGY.
         * @m3.dataStructures One MITCourse enum singleton with final int number, String name and Domain reference.
         */
        COURSE_21A(21, "Anthropology", Domain.ANTHROPOLOGY),
        /** Course 24 - Linguistics and Philosophy
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish COURSE_24 at course-mapping ordinal 18 with course number 24 and mapped domain PHILOSOPHY.
         * @m3.dataStructures One MITCourse enum singleton with final int number, String name and Domain reference.
         */
        COURSE_24(24, "Linguistics & Philosophy", Domain.PHILOSOPHY),
        /** Course 21H - History
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish COURSE_21H at course-mapping ordinal 19 with course number 21 and mapped domain HISTORY.
         * @m3.dataStructures One MITCourse enum singleton with final int number, String name and Domain reference.
         */
        COURSE_21H(21, "History", Domain.HISTORY),
        /** Course 21L - Literature
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish COURSE_21L at course-mapping ordinal 20 with course number 21 and mapped domain LITERATURE.
         * @m3.dataStructures One MITCourse enum singleton with final int number, String name and Domain reference.
         */
        COURSE_21L(21, "Literature", Domain.LITERATURE),
        /** Course 21M - Music
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish COURSE_21M at course-mapping ordinal 21 with course number 21 and mapped domain ARTS.
         * @m3.dataStructures One MITCourse enum singleton with final int number, String name and Domain reference.
         */
        COURSE_21M(21, "Music", Domain.ARTS),
        /** Course 4 - Architecture
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish COURSE_4 at course-mapping ordinal 22 with course number 4 and mapped domain ARCHITECTURE.
         * @m3.dataStructures One MITCourse enum singleton with final int number, String name and Domain reference.
         */
        COURSE_4(4, "Architecture", Domain.ARCHITECTURE);

        /**
         * Retain the constructor-supplied integer course number, including repeated number 21 mappings.
         * @m3.pattern synexia:iop
         * @m3.algorithm Retain the constructor-supplied integer course number, including repeated number 21 mappings.
         * @m3.dataStructures One public final int field.
         */
        public final int number;
        /**
         * Retain the constructor-supplied course name.
         * @m3.pattern synexia:iop
         * @m3.algorithm Retain the constructor-supplied course name.
         * @m3.dataStructures One public final String reference.
         */
        public final String name;
        /**
         * Retain the constructor-supplied domain mapping used by crystallinity().
         * @m3.pattern synexia:iop
         * @m3.algorithm Retain the constructor-supplied domain mapping used by crystallinity().
         * @m3.dataStructures One public final Domain reference.
         */
        public final Domain domain;

        /**
         * Assign the supplied number, name and domain directly to the corresponding final fields.
         * @m3.pattern synexia:iop
         * @m3.algorithm Assign the supplied number, name and domain directly to the corresponding final fields.
         * @m3.dataStructures One int argument, one String argument, one Domain argument and corresponding final fields.
         */
        MITCourse(int number, String name, Domain domain) {
            this.number = number;
            this.name = name;
            this.domain = domain;
        }

        /**
         * Returns the crystallinity of this MIT course.
         *
         * @return crystallinity coefficient [0.0, 1.0]
         * @m3.pattern synexia:fop
         * @m3.algorithm Return the stored mapped domain crystallinity coefficient.
         * @m3.dataStructures One final Domain reference and its primitive double crystallinity field.
         */
        public double crystallinity() {
            return domain.crystallinity;
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // TLOUOOIACI PRINCIPLE
    // ════════════════════════════════════════════════════════════════════════

    /**
     * The Law Of Universals Order Of Informational And Constructional Invariance.
     *
     * <p>
     * The ordering from Crystalline to Amorphous is FIXED because:
     * </p>
     * <ol>
     * <li>Physics requires mathematics (F=ma needs m, a defined)</li>
     * <li>Chemistry requires physics (bonds need electron orbitals)</li>
     * <li>Biology requires chemistry (life needs molecules)</li>
     * <li>Psychology requires biology (mind needs brain)</li>
     * <li>Philosophy requires psychology (thought needs mind)</li>
     * </ol>
     *
     * <p>
     * This is the CONSTRUCTIONAL order - you cannot build later concepts without
     * first having earlier concepts.
     * </p>
     * @m3.pattern synexia:iop
     * @m3.algorithm Expose the existing concatenated law explanation text unchanged.
     * @m3.dataStructures One public static final String initialized from concatenated literals.
     */
    public static final String TLOUOOIACI =
            "The Law Of Universals Order Of Informational And Constructional Invariance:\n"
                    + "The ordering Crystalline → Amorphous is FIXED and INVARIANT.\n"
                    + "You cannot understand later domains without first understanding earlier domains.\n"
                    + "This is the dependency order. This is the constructional order.\n"
                    + "This order cannot be changed. It is built into reality itself.";

    /** Prints the OrdinalSpine showing all domains in order.
     * @m3.pattern synexia:bop
     * @m3.algorithm Log the existing introductory message, then print the heading, domains and footer in that order.
     * @m3.dataStructures Existing private printing methods and SLF4J logger calls.
     */
    public static void printSpine() {
        log.info("Printing OrdinalSpine");
        printHeader();
        printDomains();
        printFooter();
    }

    /**
     * Log the existing separator, spine title, law title and separator in that order.
     * @m3.pattern synexia:bop
     * @m3.algorithm Log the existing separator, spine title, law title and separator in that order.
     * @m3.dataStructures Four SLF4J logger calls using existing String literals and SEPARATOR_LINE.
     */
    private static void printHeader() {
        log.info(SEPARATOR_LINE);
        log.info("  ORDINAL SPINE: Crystalline → Amorphous");
        log.info("  TLOUOOIACI - The Law Of Universals Order");
        log.info(SEPARATOR_LINE);
    }

    /**
     * Visit Domain.values() in declaration order, print a tier heading whenever the stored tier changes, then print the domain.
     * @m3.pattern synexia:bop
     * @m3.algorithm Visit Domain.values() in declaration order, print a tier heading whenever the stored tier changes, then print the domain.
     * @m3.dataStructures Temporary Domain[] array and sequential stream, local AtomicReference&lt;Tier> initially null, and private print methods.
     */
    private static void printDomains() {
        java.util.concurrent.atomic.AtomicReference<Tier> currentTier =
                new java.util.concurrent.atomic.AtomicReference<>();
        java.util.Arrays.stream(Domain.values()).forEach(d -> {
            if (d.tier != currentTier.get()) {
                currentTier.set(d.tier);
                printTierHeader(d.tier);
            }
            printDomain(d);
        });
    }

    /**
     * Log FORMAT_TIER with the supplied tier name and its stored minimum and maximum crystallinity.
     * @m3.pattern synexia:bop
     * @m3.algorithm Log FORMAT_TIER with the supplied tier name and its stored minimum and maximum crystallinity.
     * @m3.dataStructures One Tier reference, String format constant and double bounds passed to SLF4J.
     */
    private static void printTierHeader(Tier tier) {
        log.info(FORMAT_TIER, tier.name(), tier.minCrystallinity, tier.maxCrystallinity);
    }

    /**
     * Log FORMAT_DOMAIN with the supplied domain ordinal, stored name, crystallinity and description.
     * @m3.pattern synexia:bop
     * @m3.algorithm Log FORMAT_DOMAIN with the supplied domain ordinal, stored name, crystallinity and description.
     * @m3.dataStructures One Domain reference and four SLF4J message arguments with an existing String format constant.
     */
    private static void printDomain(Domain d) {
        log.info(FORMAT_DOMAIN, d.ordinal(), d.name, d.crystallinity, d.description);
    }

    /**
     * Log the separator, domain and tier counts, fixed-order statement, then the separator.
     * @m3.pattern synexia:bop
     * @m3.algorithm Log the separator, domain and tier counts, fixed-order statement, then the separator.
     * @m3.dataStructures Fresh Domain[] and Tier[] values arrays for counts and existing SLF4J message arguments.
     */
    private static void printFooter() {

        log.info(SEPARATOR_LINE);
        log.info("  Total Domains: {} | Tiers: {}", Domain.values().length, Tier.values().length);
        log.info("  Order: FIXED | Authority: TLOUOOIACI");
        log.info(SEPARATOR_LINE);
    }

    /**
     * Main method for testing the OrdinalSpine.
     *
     * @param args
     *            command line arguments (ignored)
     * @m3.pattern synexia:bop
     * @m3.algorithm Invoke printSpine() once; the args reference is unused.
     * @m3.dataStructures Unused String[] argument and the existing printSpine utility method.
     */
    public static void main(String[] args) {
        printSpine();
    }
}
