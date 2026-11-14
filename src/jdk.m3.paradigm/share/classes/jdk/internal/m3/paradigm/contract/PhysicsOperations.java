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
package jdk.internal.m3.paradigm.contract;

import jdk.internal.m3.collections.PlaceholderLogger;

/**
 * Physics operations - deterministic physical laws.
 *
 * @since IOP 1.1
 * @m3.pattern synexia:bop
 * @m3.algorithm Provides stored physical constants, source-ordered double multiplication for force and energies, and a Lorentz-factor calculation guarded only by Math.abs(velocity) compared with C; nonfinite arithmetic follows Java double semantics.
 * @m3.dataStructures Eight static final double fields, double scalar arguments/results, a possible IllegalArgumentException and a Lombok-generated static logger.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/crystalline/PhysicsOperations.java}
 * blob {@code 4875a4a1c6daaee530a48414e83fabefbe21e903} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PhysicsOperations {
    private static final PlaceholderLogger log = PlaceholderLogger.of(PhysicsOperations.class);


    /**
     * Private constructor for the static declaration owner.
     * @m3.pattern synexia:bop
     * @m3.algorithm Executes an empty private constructor body; ordinary callers cannot construct this owner.
     * @m3.dataStructures No declared instance fields or constructor parameters.
     */
    private PhysicsOperations() {}

    /** Speed of light in vacuum (m/s).
     * @m3.pattern synexia:iop
     * @m3.algorithm Initializes the double field from the exactly representable integer literal 299_792_458.
     * @m3.dataStructures One static final double field.
     */
    public static final double C = 299_792_458;

    /** Planck constant (J⋅s).
     * @m3.pattern synexia:iop
     * @m3.algorithm Initializes the double field from the decimal literal 6.62607015e-34; retains its binary floating-point representation.
     * @m3.dataStructures One static final double field.
     */
    public static final double H = 6.62607015e-34;

    /** Reduced Planck constant ℏ = h/2π (J⋅s).
     * @m3.pattern synexia:iop
     * @m3.algorithm Initializes the double field as H/(2*Math.PI) using the stored H and JDK pi constant with double multiplication and division.
     * @m3.dataStructures One static final double field.
     */
    public static final double HBAR = H / (2 * Math.PI);

    /** Gravitational constant (m³/(kg⋅s²)).
     * @m3.pattern synexia:iop
     * @m3.algorithm Initializes the double field from the decimal literal 6.67430e-11; retains its binary floating-point representation.
     * @m3.dataStructures One static final double field.
     */
    public static final double G = 6.67430e-11;

    /** Boltzmann constant (J/K).
     * @m3.pattern synexia:iop
     * @m3.algorithm Initializes the double field from the decimal literal 1.380649e-23; retains its binary floating-point representation.
     * @m3.dataStructures One static final double field.
     */
    public static final double K_B = 1.380649e-23;

    /** Elementary charge (C).
     * @m3.pattern synexia:iop
     * @m3.algorithm Initializes the double field from the decimal literal 1.602176634e-19; retains its binary floating-point representation.
     * @m3.dataStructures One static final double field.
     */
    public static final double ELEMENTARY_CHARGE = 1.602176634e-19;

    /** Avogadro constant (1/mol).
     * @m3.pattern synexia:iop
     * @m3.algorithm Initializes the double field from the decimal literal 6.02214076e23; retains its binary floating-point representation.
     * @m3.dataStructures One static final double field.
     */
    public static final double N_A = 6.02214076e23;

    /** Standard gravity (m/s²).
     * @m3.pattern synexia:iop
     * @m3.algorithm Initializes the double field from the decimal literal 9.80665; retains its binary floating-point representation.
     * @m3.dataStructures One static final double field.
     */
    public static final double G_EARTH = 9.80665;

    /**
     * Newton's Second Law: F = ma.
     *
     * @param mass
     *            mass in kg
     * @param acceleration
     *            acceleration in m/s²
     * @return force in N
     * @m3.pattern synexia:bop
     * @m3.algorithm Returns mass*acceleration with one double multiplication and no validation, retaining signed-zero, overflow, infinity and NaN behavior.
     * @m3.dataStructures Two double arguments and one double result.
     */
    public static double force(double mass, double acceleration) {
        return mass * acceleration;
    }

    /**
     * Kinetic energy: KE = ½mv².
     *
     * @param mass
     *            mass in kg
     * @param velocity
     *            velocity in m/s
     * @return kinetic energy in J
     * @m3.pattern synexia:bop
     * @m3.algorithm Evaluates left-associated 0.5*mass*velocity*velocity with double multiplication and no validation; reordering to square velocity first could alter rounding or overflow.
     * @m3.dataStructures Two double arguments, a half literal, intermediate double products and one double result.
     */
    public static double kineticEnergy(double mass, double velocity) {
        return 0.5 * mass * velocity * velocity;
    }

    /**
     * Mass-energy equivalence: E = mc².
     *
     * @param mass
     *            mass (kg)
     * @return energy (J)
     * @m3.pattern synexia:bop
     * @m3.algorithm Evaluates left-associated mass*C*C with double multiplication and no validation; retains the stored C value and source-order rounding or overflow.
     * @m3.dataStructures One double argument, the shared C field, intermediate double products and one double result.
     */
    public static double massEnergy(double mass) {
        return mass * C * C;
    }

    /**
     * Lorentz factor: γ = 1/√(1 - v²/c²).
     *
     * @param velocity
     *            velocity (m/s)
     * @return Lorentz factor
     * @m3.pattern synexia:bop
     * @m3.algorithm Throws IllegalArgumentException when Math.abs(velocity) is greater than or equal to C, including infinite velocities; otherwise computes beta=velocity/C and 1.0/Math.sqrt(1-beta*beta). NaN passes the comparison and propagates.
     * @m3.dataStructures One double input, the C field, one local beta double, arithmetic/square-root results and a possible IllegalArgumentException.
     */
    public static double lorentzFactor(double velocity) {
        if (Math.abs(velocity) >= C) throw new IllegalArgumentException("Velocity must be less than c");
        double beta = velocity / C;
        return 1.0 / Math.sqrt(1 - beta * beta);
    }
}
