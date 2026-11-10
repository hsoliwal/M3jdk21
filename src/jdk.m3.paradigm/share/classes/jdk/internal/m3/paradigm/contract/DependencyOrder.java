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
 * The ordering from Crystalline to Amorphous IS the dependency order.
 * You CANNOT understand later concepts without understanding earlier ones.
 *
 * F = ma requires:
 *   - Understanding of mass (m) - needs SI units
 *   - Understanding of acceleration (a) - needs velocity
 *   - Velocity needs distance and time - needs SI units
 *   - SI units need mathematics
 *
 * The chain is FIXED: Math → Units → Distance/Time → Velocity → Acceleration → Force
 */
package jdk.internal.m3.paradigm.contract;

import jdk.internal.m3.collections.PlaceholderLogger;

import java.util.List;
import java.util.stream.Stream;
/**
 * DependencyOrder - The constructional invariance of knowledge.
 *
 * <p>
 * TLOUOOIACI captures the simple truth: Crystalline subjects have deeper
 * prerequisite chains than Amorphous subjects.
 * </p>
 *
 * <h2>The Evidence</h2>
 *
 * <pre>
 * MIT Physics 8.02: Prereq: Calculus I + Physics I
 * MIT Physics 8.03: Prereq: Calculus II + Physics II
 * MIT Biology 7.05: Prereq: Biology GIR + Chemistry 5.12
 * MIT Biology 7.06: Prereq: 7.03 + 7.05
 *
 * MIT Philosophy 24.00: Prereq: None
 * MIT Philosophy 24.01: Prereq: None
 * MIT Philosophy 24.09: Prereq: None
 * </pre>
 *
 * <h2>The Principle</h2>
 * <p>
 * Crystalline subjects REQUIRE crystalline foundations. Amorphous subjects can
 * stand alone (they deal with abstract concepts).
 * </p>
 *
 * <h2>The F = ma Example</h2>
 *
 * <pre>
 * To understand F = ma, you need:
 *   1. Mathematics (arithmetic, algebra)
 *   2. SI Units (kg, m, s)
 *   3. Distance (d) and Time (t)
 *   4. Velocity (v = d/t)
 *   5. Acceleration (a = Δv/Δt = (v² - u²)/2s)
 *   6. Mass (m) - fundamental SI unit
 *   7. THEN you can understand Force (F = ma)
 *
 * The order is FIXED. You cannot skip steps.
 * This is CONSTRUCTIONAL INVARIANCE.
 * </pre>
 *
 * @see OrdinalSpine for domain ordering
 * @m3.pattern synexia:iop
 * @m3.algorithm Expose the declared force-concept chain and prerequisite-depth table, compare force concepts by enum ordinal, and print their existing values.
 * @m3.dataStructures Noninstantiable utility class, two nested enums, two scalar constants and one String text block.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/DependencyOrder.java}
 * blob {@code b39d7311604b0f0d5d4fd5d13dceb0e87247bb7f} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class DependencyOrder {
    private static final PlaceholderLogger log = PlaceholderLogger.of(DependencyOrder.class);


    /**
     * Retain the existing separator text for the force-chain log output.
     * @m3.pattern synexia:iop
     * @m3.algorithm Retain the existing separator text for the force-chain log output.
     * @m3.dataStructures One static final String literal.
     */
    private static final String SEPARATOR_LINE = "═══════════════════════════════════════════════════════════════";
    /**
     * Retain the value one used to display zero-based enum ordinals as one-based step numbers.
     * @m3.pattern synexia:iop
     * @m3.algorithm Retain the value one used to display zero-based enum ordinals as one-based step numbers.
     * @m3.dataStructures One static final int initialized to 1.
     */
    private static final int STEP_OFFSET = 1;

    /**
     * Prevent ordinary external instantiation with a private no-op constructor.
     * @m3.pattern synexia:iop
     * @m3.algorithm Prevent ordinary external instantiation with a private no-op constructor.
     * @m3.dataStructures No instance fields or constructor arguments.
     */
    private DependencyOrder() {
        // Utility class
    }

    // ════════════════════════════════════════════════════════════════════════
    // THE CONSTRUCTIONAL CHAIN: F = ma
    // ════════════════════════════════════════════════════════════════════════

    /**
     * The dependency chain for understanding F = ma.
     *
     * <p>
     * Each concept requires understanding of all previous concepts. This is the
     * CONSTRUCTIONAL ORDER - it cannot be changed.
     * </p>
     * @m3.pattern synexia:iop
     * @m3.algorithm Construct nine force-concept values in declaration order; prerequisite checks compare ordinals and prerequisite lists select the preceding prefix.
     * @m3.dataStructures Nine enum singletons with final name, description and formula String fields, plus the generated values array.
     */
    public enum ForceChain {

        /** Step 1: Mathematics - arithmetic, algebra, calculus.
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish MATHEMATICS at force-chain ordinal 0 with its declared name, description and formula text; this position determines prerequisite comparisons.
         * @m3.dataStructures One ForceChain enum singleton with three final String fields.
         */
        MATHEMATICS("Mathematics", "Arithmetic, algebra, calculus", "2 + 2 = 4, x² + y² = r²"),

        /** Step 2: SI Base Units - the foundation of measurement.
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish SI_UNITS at force-chain ordinal 1 with its declared name, description and formula text; this position determines prerequisite comparisons.
         * @m3.dataStructures One ForceChain enum singleton with three final String fields.
         */
        SI_UNITS("SI Base Units", "meter, second, kilogram, ampere, kelvin, mole, candela", "m, s, kg, A, K, mol, cd"),

        /** Step 3: Distance - requires SI units (meters).
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish DISTANCE at force-chain ordinal 2 with its declared name, description and formula text; this position determines prerequisite comparisons.
         * @m3.dataStructures One ForceChain enum singleton with three final String fields.
         */
        DISTANCE("Distance", "Spatial separation", "d [m] - requires meter"),

        /** Step 4: Time - requires SI units (seconds).
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish TIME at force-chain ordinal 3 with its declared name, description and formula text; this position determines prerequisite comparisons.
         * @m3.dataStructures One ForceChain enum singleton with three final String fields.
         */
        TIME("Time", "Duration", "t [s] - requires second"),

        /** Step 5: Displacement - distance with direction.
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish DISPLACEMENT at force-chain ordinal 4 with its declared name, description and formula text; this position determines prerequisite comparisons.
         * @m3.dataStructures One ForceChain enum singleton with three final String fields.
         */
        DISPLACEMENT("Displacement", "Vector distance", "Δx [m] - requires distance + direction"),

        /** Step 6: Velocity - requires distance and time.
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish VELOCITY at force-chain ordinal 5 with its declared name, description and formula text; this position determines prerequisite comparisons.
         * @m3.dataStructures One ForceChain enum singleton with three final String fields.
         */
        VELOCITY("Velocity", "Rate of change of position", "v = d/t [m/s] - requires distance, time"),

        /** Step 7: Acceleration - requires velocity and time.
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish ACCELERATION at force-chain ordinal 6 with its declared name, description and formula text; this position determines prerequisite comparisons.
         * @m3.dataStructures One ForceChain enum singleton with three final String fields.
         */
        ACCELERATION("Acceleration", "Rate of change of velocity", "a = Δv/Δt [m/s²] - requires velocity, time"),

        /** Step 8: Mass - fundamental SI unit.
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish MASS at force-chain ordinal 7 with its declared name, description and formula text; this position determines prerequisite comparisons.
         * @m3.dataStructures One ForceChain enum singleton with three final String fields.
         */
        MASS("Mass", "Amount of matter", "m [kg] - SI base unit"),

        /** Step 9: Force - requires mass and acceleration.
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish FORCE at force-chain ordinal 8 with its declared name, description and formula text; this position determines prerequisite comparisons.
         * @m3.dataStructures One ForceChain enum singleton with three final String fields.
         */
        FORCE("Force", "Mass times acceleration", "F = ma [N = kg·m/s²] - requires mass, acceleration");

        /**
         * Retain the constructor-supplied force-concept name without transformation.
         * @m3.pattern synexia:iop
         * @m3.algorithm Retain the constructor-supplied force-concept name without transformation.
         * @m3.dataStructures One public final String reference.
         */
        public final String name;
        /**
         * Retain the constructor-supplied force-concept description without transformation.
         * @m3.pattern synexia:iop
         * @m3.algorithm Retain the constructor-supplied force-concept description without transformation.
         * @m3.dataStructures One public final String reference.
         */
        public final String description;
        /**
         * Retain the constructor-supplied formula text for step printing without evaluating it.
         * @m3.pattern synexia:iop
         * @m3.algorithm Retain the constructor-supplied formula text for step printing without evaluating it.
         * @m3.dataStructures One public final String reference.
         */
        public final String formula;

        /**
         * Assign the supplied name, description and formula references to their corresponding final fields without validation.
         * @m3.pattern synexia:iop
         * @m3.algorithm Assign the supplied name, description and formula references to their corresponding final fields without validation.
         * @m3.dataStructures Three String arguments and three final String instance fields.
         */
        ForceChain(String name, String description, String formula) {
            this.name = name;
            this.description = description;
            this.formula = formula;
        }

        /**
         * Returns true if this concept is a prerequisite for another.
         *
         * @param other
         *            the concept to check
         * @return true if this comes before other in the chain
         * @m3.pattern synexia:fop
         * @m3.algorithm Return whether this ordinal is strictly less than the other ordinal; a null other fails when ordinal() is invoked.
         * @m3.dataStructures Two ForceChain enum references and primitive int ordinals producing a boolean.
         */
        public boolean isPrerequisiteFor(ForceChain other) {
            return this.ordinal() < other.ordinal();
        }

        /**
         * Returns all prerequisites for this concept.
         *
         * @return list of prerequisite concepts
         * @m3.pattern synexia:fop
         * @m3.algorithm Create a list from all ForceChain values and return the unmodifiable subList from zero through this ordinal, excluding this value.
         * @m3.dataStructures Temporary ForceChain[] values array, immutable List&lt;ForceChain> and prefix sublist.
         */
        public List<ForceChain> prerequisites() {
            return List.of(ForceChain.values()).subList(0, this.ordinal());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // PREREQUISITE DEPTH BY DOMAIN
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Prerequisite depth by domain.
     *
     * <p>
     * Crystalline subjects have DEEP prerequisite chains. Amorphous subjects have
     * SHALLOW prerequisite chains.
     * </p>
     * @m3.pattern synexia:iop
     * @m3.algorithm Construct eight domain depth entries with their declared depth and chain text; printing sorts the values by descending depth.
     * @m3.dataStructures Eight enum singletons with one final int depth field and one final String chain field.
     */
    public enum PrerequisiteDepth {

        /** Mathematics - foundational, no prerequisites.
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish MATHEMATICS at depth-table ordinal 0 with stored depth 0 and its declared prerequisite-chain text.
         * @m3.dataStructures One PrerequisiteDepth enum singleton with final int depth and String chain fields.
         */
        MATHEMATICS(0, "None - foundational"),

        /** Physics - requires calculus, builds on itself.
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish PHYSICS at depth-table ordinal 1 with stored depth 3 and its declared prerequisite-chain text.
         * @m3.dataStructures One PrerequisiteDepth enum singleton with final int depth and String chain fields.
         */
        PHYSICS(3, "Calculus I → Physics I → Physics II → ..."),

        /** Chemistry - requires physics and calculus.
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish CHEMISTRY at depth-table ordinal 2 with stored depth 2 and its declared prerequisite-chain text.
         * @m3.dataStructures One PrerequisiteDepth enum singleton with final int depth and String chain fields.
         */
        CHEMISTRY(2, "Calculus → Chemistry I → Chemistry II"),

        /** Biology - requires chemistry and biology foundations.
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish BIOLOGY at depth-table ordinal 3 with stored depth 4 and its declared prerequisite-chain text.
         * @m3.dataStructures One PrerequisiteDepth enum singleton with final int depth and String chain fields.
         */
        BIOLOGY(4, "Biology GIR + Chemistry → Genetics + Biochemistry → Cell Biology"),

        /** Computer Science - requires calculus, discrete math.
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish COMPUTER_SCIENCE at depth-table ordinal 4 with stored depth 2 and its declared prerequisite-chain text.
         * @m3.dataStructures One PrerequisiteDepth enum singleton with final int depth and String chain fields.
         */
        COMPUTER_SCIENCE(2, "Calculus, Discrete Math → Algorithms"),

        /** Economics - requires calculus, statistics.
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish ECONOMICS at depth-table ordinal 5 with stored depth 2 and its declared prerequisite-chain text.
         * @m3.dataStructures One PrerequisiteDepth enum singleton with final int depth and String chain fields.
         */
        ECONOMICS(2, "Calculus, Statistics → Microeconomics"),

        /** Psychology - minimal prerequisites.
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish PSYCHOLOGY at depth-table ordinal 6 with stored depth 1 and its declared prerequisite-chain text.
         * @m3.dataStructures One PrerequisiteDepth enum singleton with final int depth and String chain fields.
         */
        PSYCHOLOGY(1, "Intro course → Advanced"),

        /** Philosophy - typically no prerequisites.
         * @m3.pattern synexia:iop
         * @m3.algorithm Publish PHILOSOPHY at depth-table ordinal 7 with stored depth 0 and its declared prerequisite-chain text.
         * @m3.dataStructures One PrerequisiteDepth enum singleton with final int depth and String chain fields.
         */
        PHILOSOPHY(0, "None - can start directly");

        /** Depth of prerequisite chain (0 = no prereqs).
         * @m3.pattern synexia:iop
         * @m3.algorithm Retain the constructor-supplied depth used by descending depth comparisons.
         * @m3.dataStructures One public final int field.
         */
        public final int depth;
        /** Description of the prerequisite chain.
         * @m3.pattern synexia:iop
         * @m3.algorithm Retain the constructor-supplied prerequisite-chain text for logging.
         * @m3.dataStructures One public final String reference.
         */
        public final String chain;

        /**
         * Assign the supplied depth and chain directly to their corresponding final fields.
         * @m3.pattern synexia:iop
         * @m3.algorithm Assign the supplied depth and chain directly to their corresponding final fields.
         * @m3.dataStructures One int argument, one String argument and corresponding final fields.
         */
        PrerequisiteDepth(int depth, String chain) {
            this.depth = depth;
            this.chain = chain;
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // THE INVARIANT TRUTH
    // ════════════════════════════════════════════════════════════════════════

    /**
     * TLOUOOIACI - The Law Of Universals Order Of Informational And Constructional
     * Invariance.
     *
     * <p>
     * The ordering from Crystalline to Amorphous IS the dependency order. This is
     * not a choice - it is built into reality itself.
     * </p>
     * @m3.pattern synexia:iop
     * @m3.algorithm Expose the existing law explanation text block unchanged for demonstration logging.
     * @m3.dataStructures One public static final String text block.
     */
    public static final String TLOUOOIACI = """
			╔═══════════════════════════════════════════════════════════════════════════╗
			║                                                                           ║
			║   🔒 TLOUOOIACI - The Dependency Order is FIXED 🔒                        ║
			║                                                                           ║
			╠═══════════════════════════════════════════════════════════════════════════╣
			║                                                                           ║
			║   CRYSTALLINE → AMORPHOUS                                                 ║
			║   (Deep Prerequisites) → (Shallow Prerequisites)                          ║
			║                                                                           ║
			║   F = ma requires:                                                        ║
			║     Math → SI Units → Distance/Time → Velocity → Acceleration → Force    ║
			║                                                                           ║
			║   MIT Evidence:                                                           ║
			║     Physics 8.02: Prereq: Calculus I + Physics I (DEEP)                   ║
			║     Philosophy 24.00: Prereq: None (SHALLOW)                              ║
			║                                                                           ║
			║   The order CANNOT be changed.                                            ║
			║   You CANNOT understand Force without understanding Mass and Acceleration.║
			║   You CANNOT understand Acceleration without understanding Velocity.      ║
			║   You CANNOT understand Velocity without understanding Distance and Time. ║
			║                                                                           ║
			║   This is CONSTRUCTIONAL INVARIANCE.                                      ║
			║   This is the simple order of dependencies.                               ║
			║                                                                           ║
			╚═══════════════════════════════════════════════════════════════════════════╝
			""";

    /** Prints the F = ma dependency chain.
     * @m3.pattern synexia:bop
     * @m3.algorithm Log the existing chain heading, visit ForceChain.values() in declaration order, print each step, then log the chain footer.
     * @m3.dataStructures Temporary ForceChain values array, sequential stream and SLF4J logger calls.
     */
    public static void printForceChain() {
        log.info("Printing F=ma dependency chain");
        log.info(SEPARATOR_LINE);
        log.info("  THE F = ma DEPENDENCY CHAIN (Constructional Order)");
        log.info(SEPARATOR_LINE);

        java.util.Arrays.stream(ForceChain.values()).forEach(step -> printForceChainStep(step));

        printForceChainFooter();
    }

    /**
     * Log one step using its ordinal plus STEP_OFFSET, stored name and stored formula; a null step fails during field access.
     * @m3.pattern synexia:bop
     * @m3.algorithm Log one step using its ordinal plus STEP_OFFSET, stored name and stored formula; a null step fails during field access.
     * @m3.dataStructures One ForceChain reference, integer step number and SLF4J varargs message arguments.
     */
    private static void printForceChainStep(ForceChain step) {
        log.info("  Step {}: {} → {}", step.ordinal() + STEP_OFFSET, step.name, step.formula);
    }

    /**
     * Log the two existing fixed-order statements followed by SEPARATOR_LINE.
     * @m3.pattern synexia:bop
     * @m3.algorithm Log the two existing fixed-order statements followed by SEPARATOR_LINE.
     * @m3.dataStructures Three SLF4J logger calls with existing String constants and literals.
     */
    private static void printForceChainFooter() {

        log.info("  The order is FIXED. You cannot skip steps.");
        log.info("  This is TLOUOOIACI - Constructional Invariance.");
        log.info(SEPARATOR_LINE);
    }

    /** Prints prerequisite depth comparison.
     * @m3.pattern synexia:bop
     * @m3.algorithm Log the heading, sort the declared depth values with b.depth - a.depth, log each entry, then log the separator.
     * @m3.dataStructures PrerequisiteDepth values array, ordered object stream, descending int comparator and SLF4J logger calls.
     */
    public static void printPrerequisiteDepth() {
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("  PREREQUISITE DEPTH BY DOMAIN");
        log.info("  (Crystalline = Deep, Amorphous = Shallow)");
        log.info("═══════════════════════════════════════════════════════════════");

        Stream.of(PrerequisiteDepth.values())
                .sorted((a, b) -> b.depth - a.depth)
                .forEach(d -> log.info("  {} Depth: {}  Chain: {}", d.name(), d.depth, d.chain));

        log.info("═══════════════════════════════════════════════════════════════");
    }

    /**
     * Main method for demonstration.
     *
     * @param args
     *            command line arguments (ignored)
     * @m3.pattern synexia:bop
     * @m3.algorithm Log TLOUOOIACI, print the force chain, then print the prerequisite-depth comparison; the args reference is unused.
     * @m3.dataStructures Unused String[] argument and the existing utility printing methods.
     */
    public static void main(String[] args) {
        log.info(TLOUOOIACI);

        printForceChain();

        printPrerequisiteDepth();
    }
}
