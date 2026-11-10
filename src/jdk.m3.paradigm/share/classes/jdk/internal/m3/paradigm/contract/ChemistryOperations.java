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
 * Chemistry operations - thermodynamic laws.
 *
 * @since IOP 1.1
 * @m3.pattern synexia:bop
 * @m3.algorithm Provides literal constants, zero-volume guarded gas-pressure division, enthalpy-minus-temperature-times-entropy, its negative-result predicate and guarded negative base-ten logarithms using Java doubles.
 * @m3.dataStructures Three static final double fields, double parameters/results, a boolean result and a Lombok-generated static logger.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/crystalline/ChemistryOperations.java}
 * blob {@code 34a2609e7b7c40172a9bca816750f55e6968c7d6} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class ChemistryOperations {
    private static final PlaceholderLogger log = PlaceholderLogger.of(ChemistryOperations.class);


    /**
     * Private constructor for the static declaration owner.
     * @m3.pattern synexia:bop
     * @m3.algorithm Executes an empty private constructor body; ordinary callers cannot construct this owner.
     * @m3.dataStructures No declared instance fields or constructor parameters.
     */
    private ChemistryOperations() {}

    /** Gas constant R (J/(mol⋅K)).
     * @m3.pattern synexia:iop
     * @m3.algorithm Initializes the double field from the decimal literal 8.314462618; retains its binary floating-point representation.
     * @m3.dataStructures One static final double field.
     */
    public static final double R = 8.314462618;

    /** Standard temperature (K).
     * @m3.pattern synexia:iop
     * @m3.algorithm Initializes the double field from the decimal literal 298.15; retains its binary floating-point representation.
     * @m3.dataStructures One static final double field.
     */
    public static final double STANDARD_TEMP = 298.15;

    /** Faraday constant (C/mol).
     * @m3.pattern synexia:iop
     * @m3.algorithm Initializes the double field from the decimal literal 96485.33212; retains its binary floating-point representation.
     * @m3.dataStructures One static final double field.
     */
    public static final double FARADAY = 96485.33212;

    /**
     * Ideal gas law: PV = nRT.
     *
     * @param moles
     *            number of moles
     * @param temperature
     *            temperature (K)
     * @param volume
     *            volume (m³)
     * @return pressure (Pa)
     * @m3.pattern synexia:bop
     * @m3.algorithm Throws ArithmeticException when volume compares equal to zero, including either zero sign; otherwise evaluates left-associated moles*R*temperature/volume without sign or finiteness validation.
     * @m3.dataStructures Three double inputs, the R field, a double result and a possible ArithmeticException.
     */
    public static double idealGasPressure(double moles, double temperature, double volume) {
        if (volume == 0) throw new ArithmeticException("Volume cannot be zero");
        return moles * R * temperature / volume;
    }

    /**
     * Gibbs free energy: ΔG = ΔH - TΔS.
     *
     * @param deltaH
     *            enthalpy change (J)
     * @param temperature
     *            temperature (K)
     * @param deltaS
     *            entropy change (J/K)
     * @return Gibbs free energy change (J)
     * @m3.pattern synexia:bop
     * @m3.algorithm Evaluates deltaH-temperature*deltaS with double multiplication before subtraction, retaining rounding, overflow and NaN behavior without validation.
     * @m3.dataStructures Three double inputs, one intermediate double product and one double result.
     */
    public static double gibbsFreeEnergy(double deltaH, double temperature, double deltaS) {
        return deltaH - temperature * deltaS;
    }

    /**
     * Is reaction spontaneous? (ΔG &lt; 0).
     *
     * @param deltaH
     *            enthalpy change (J)
     * @param temperature
     *            temperature (K)
     * @param deltaS
     *            entropy change (J/K)
     * @return true if spontaneous
     * @m3.pattern synexia:bop
     * @m3.algorithm Calls gibbsFreeEnergy once and tests whether its double result is strictly below zero; zero and NaN results yield false.
     * @m3.dataStructures Three double inputs, the computed double energy and one boolean result.
     */
    public static boolean isSpontaneous(double deltaH, double temperature, double deltaS) {
        return gibbsFreeEnergy(deltaH, temperature, deltaS) < 0;
    }

    /**
     * pH from hydrogen ion concentration.
     *
     * @param hydrogenConcentration
     *            [H+] in mol/L
     * @return pH value
     * @m3.pattern synexia:bop
     * @m3.algorithm Throws IllegalArgumentException when concentration compares less than or equal to zero; otherwise returns -Math.log10 of the input, allowing NaN and positive infinity to follow the logarithm semantics.
     * @m3.dataStructures One double input/result and a possible IllegalArgumentException.
     */
    public static double pH(double hydrogenConcentration) {
        if (hydrogenConcentration <= 0) throw new IllegalArgumentException("Concentration must be positive");
        return -Math.log10(hydrogenConcentration);
    }
}
