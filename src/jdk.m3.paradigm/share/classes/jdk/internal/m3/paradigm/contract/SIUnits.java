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
 * SI Units - Base and derived units.
 *
 * @since IOP 1.1
 * @m3.pattern synexia:iop
 * @m3.algorithm Groups two enums whose constants bind literal unit names, symbols, dimension or base-unit text and quantity text; accessors return stored references without conversion or dimension arithmetic.
 * @m3.dataStructures Two nested enum types with seven base and four derived singleton values, four final String fields per value and a Lombok-generated static logger.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/crystalline/SIUnits.java}
 * blob {@code 56b1e5d9098c0987fa102c877a4fa25cbedaf801} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class SIUnits {
    private static final PlaceholderLogger log = PlaceholderLogger.of(SIUnits.class);


    /**
     * Private constructor for the static declaration owner.
     * @m3.pattern synexia:iop
     * @m3.algorithm Executes an empty private constructor body; ordinary callers cannot construct this owner.
     * @m3.dataStructures No declared instance fields or constructor parameters.
     */
    private SIUnits() {}

    /** SI Base Units - The 7 fundamental units.
     * @m3.pattern synexia:iop
     * @m3.algorithm Defines 7 enum values in source order, assigns four constructor-supplied String references, and exposes them through direct getters without validation or unit conversion.
     * @m3.dataStructures 7 enum singleton values with final name, symbol, dimension and quantity String fields.
     */
    public enum BaseUnit {
        /**
         * Meter base unit descriptor value.
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the METER enum singleton from the exact literals "meter", "m", "L" and "Length" in that order.
         * @m3.dataStructures One BaseUnit singleton and four stored String references.
         */
        METER("meter", "m", "L", "Length"),
        /**
         * Kilogram base unit descriptor value.
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the KILOGRAM enum singleton from the exact literals "kilogram", "kg", "M" and "Mass" in that order.
         * @m3.dataStructures One BaseUnit singleton and four stored String references.
         */
        KILOGRAM("kilogram", "kg", "M", "Mass"),
        /**
         * Second base unit descriptor value.
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the SECOND enum singleton from the exact literals "second", "s", "T" and "Time" in that order.
         * @m3.dataStructures One BaseUnit singleton and four stored String references.
         */
        SECOND("second", "s", "T", "Time"),
        /**
         * Ampere base unit descriptor value.
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the AMPERE enum singleton from the exact literals "ampere", "A", "I" and "Electric Current" in that order.
         * @m3.dataStructures One BaseUnit singleton and four stored String references.
         */
        AMPERE("ampere", "A", "I", "Electric Current"),
        /**
         * Kelvin base unit descriptor value.
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the KELVIN enum singleton from the exact literals "kelvin", "K", "Θ" and "Thermodynamic Temperature" in that order.
         * @m3.dataStructures One BaseUnit singleton and four stored String references.
         */
        KELVIN("kelvin", "K", "Θ", "Thermodynamic Temperature"),
        /**
         * Mole base unit descriptor value.
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the MOLE enum singleton from the exact literals "mole", "mol", "N" and "Amount of Substance" in that order.
         * @m3.dataStructures One BaseUnit singleton and four stored String references.
         */
        MOLE("mole", "mol", "N", "Amount of Substance"),
        /**
         * Candela base unit descriptor value.
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the CANDELA enum singleton from the exact literals "candela", "cd", "J" and "Luminous Intensity" in that order.
         * @m3.dataStructures One BaseUnit singleton and four stored String references.
         */
        CANDELA("candela", "cd", "J", "Luminous Intensity");

        /**
         * Immutable unit-name text field for a base unit value.
         * @m3.pattern synexia:iop
         * @m3.algorithm Retains the corresponding constructor-supplied name String reference with no inline initializer, validation or normalization.
         * @m3.dataStructures One final String field per BaseUnit singleton.
         */
        private final String name;
        /**
         * Immutable unit-symbol text field for a base unit value.
         * @m3.pattern synexia:iop
         * @m3.algorithm Retains the corresponding constructor-supplied symbol String reference with no inline initializer, validation or normalization.
         * @m3.dataStructures One final String field per BaseUnit singleton.
         */
        private final String symbol;
        /**
         * Immutable dimension-label text field for a base unit value.
         * @m3.pattern synexia:iop
         * @m3.algorithm Retains the corresponding constructor-supplied dimension String reference with no inline initializer, validation or normalization.
         * @m3.dataStructures One final String field per BaseUnit singleton.
         */
        private final String dimension;
        /**
         * Immutable quantity-name text field for a base unit value.
         * @m3.pattern synexia:iop
         * @m3.algorithm Retains the corresponding constructor-supplied quantity String reference with no inline initializer, validation or normalization.
         * @m3.dataStructures One final String field per BaseUnit singleton.
         */
        private final String quantity;

        /**
         * Initializes the four text fields of one base unit descriptor.
         * @m3.pattern synexia:iop
         * @m3.algorithm Assigns the supplied name, symbol, dimension and quantity String references to their corresponding final fields in source order without validation or copying.
         * @m3.dataStructures Four String parameters and four final String instance fields.
         */
        BaseUnit(String name, String symbol, String dimension, String quantity) {
            this.name = name;
            this.symbol = symbol;
            this.dimension = dimension;
            this.quantity = quantity;
        }

        /**
         * Accessor for the stored name text of one base unit value.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the existing name String field directly without copying, normalization or unit conversion.
         * @m3.dataStructures One stored String reference and its returned reference.
         */
        public String unitName() {
            return name;
        }

        /**
         * Accessor for the stored symbol text of one base unit value.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the existing symbol String field directly without copying, normalization or unit conversion.
         * @m3.dataStructures One stored String reference and its returned reference.
         */
        public String symbol() {
            return symbol;
        }

        /**
         * Accessor for the stored dimension text of one base unit value.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the existing dimension String field directly without copying, normalization or unit conversion.
         * @m3.dataStructures One stored String reference and its returned reference.
         */
        public String dimension() {
            return dimension;
        }

        /**
         * Accessor for the stored quantity text of one base unit value.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the existing quantity String field directly without copying, normalization or unit conversion.
         * @m3.dataStructures One stored String reference and its returned reference.
         */
        public String quantity() {
            return quantity;
        }
    }

    /** SI Derived Units.
     * @m3.pattern synexia:iop
     * @m3.algorithm Defines 4 enum values in source order, assigns four constructor-supplied String references, and exposes them through direct getters without validation or unit conversion.
     * @m3.dataStructures 4 enum singleton values with final name, symbol, baseUnits and quantity String fields.
     */
    public enum DerivedUnit {
        /**
         * Newton derived unit descriptor value.
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the NEWTON enum singleton from the exact literals "newton", "N", "kg⋅m/s²" and "Force" in that order.
         * @m3.dataStructures One DerivedUnit singleton and four stored String references.
         */
        NEWTON("newton", "N", "kg⋅m/s²", "Force"),
        /**
         * Joule derived unit descriptor value.
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the JOULE enum singleton from the exact literals "joule", "J", "kg⋅m²/s²" and "Energy" in that order.
         * @m3.dataStructures One DerivedUnit singleton and four stored String references.
         */
        JOULE("joule", "J", "kg⋅m²/s²", "Energy"),
        /**
         * Watt derived unit descriptor value.
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the WATT enum singleton from the exact literals "watt", "W", "J/s" and "Power" in that order.
         * @m3.dataStructures One DerivedUnit singleton and four stored String references.
         */
        WATT("watt", "W", "J/s", "Power"),
        /**
         * Pascal derived unit descriptor value.
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the PASCAL enum singleton from the exact literals "pascal", "Pa", "N/m²" and "Pressure" in that order.
         * @m3.dataStructures One DerivedUnit singleton and four stored String references.
         */
        PASCAL("pascal", "Pa", "N/m²", "Pressure");

        /**
         * Immutable unit-name text field for a derived unit value.
         * @m3.pattern synexia:iop
         * @m3.algorithm Retains the corresponding constructor-supplied name String reference with no inline initializer, validation or normalization.
         * @m3.dataStructures One final String field per DerivedUnit singleton.
         */
        private final String name;
        /**
         * Immutable unit-symbol text field for a derived unit value.
         * @m3.pattern synexia:iop
         * @m3.algorithm Retains the corresponding constructor-supplied symbol String reference with no inline initializer, validation or normalization.
         * @m3.dataStructures One final String field per DerivedUnit singleton.
         */
        private final String symbol;
        /**
         * Immutable base-unit expression text field for a derived unit value.
         * @m3.pattern synexia:iop
         * @m3.algorithm Retains the corresponding constructor-supplied baseUnits String reference with no inline initializer, validation or normalization.
         * @m3.dataStructures One final String field per DerivedUnit singleton.
         */
        private final String baseUnits;
        /**
         * Immutable quantity-name text field for a derived unit value.
         * @m3.pattern synexia:iop
         * @m3.algorithm Retains the corresponding constructor-supplied quantity String reference with no inline initializer, validation or normalization.
         * @m3.dataStructures One final String field per DerivedUnit singleton.
         */
        private final String quantity;

        /**
         * Initializes the four text fields of one derived unit descriptor.
         * @m3.pattern synexia:iop
         * @m3.algorithm Assigns the supplied name, symbol, baseUnits and quantity String references to their corresponding final fields in source order without validation or copying.
         * @m3.dataStructures Four String parameters and four final String instance fields.
         */
        DerivedUnit(String name, String symbol, String baseUnits, String quantity) {
            this.name = name;
            this.symbol = symbol;
            this.baseUnits = baseUnits;
            this.quantity = quantity;
        }

        /**
         * Accessor for the stored name text of one derived unit value.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the existing name String field directly without copying, normalization or unit conversion.
         * @m3.dataStructures One stored String reference and its returned reference.
         */
        public String unitName() {
            return name;
        }

        /**
         * Accessor for the stored symbol text of one derived unit value.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the existing symbol String field directly without copying, normalization or unit conversion.
         * @m3.dataStructures One stored String reference and its returned reference.
         */
        public String symbol() {
            return symbol;
        }

        /**
         * Accessor for the stored baseUnits text of one derived unit value.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the existing baseUnits String field directly without copying, normalization or unit conversion.
         * @m3.dataStructures One stored String reference and its returned reference.
         */
        public String baseUnits() {
            return baseUnits;
        }

        /**
         * Accessor for the stored quantity text of one derived unit value.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the existing quantity String field directly without copying, normalization or unit conversion.
         * @m3.dataStructures One stored String reference and its returned reference.
         */
        public String quantity() {
            return quantity;
        }
    }
}
