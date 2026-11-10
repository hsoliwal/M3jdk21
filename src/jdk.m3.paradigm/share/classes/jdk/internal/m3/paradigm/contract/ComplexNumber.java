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

/**
 * Complex number for Mandelbrot computation.
 *
 * @param real
 *            real part
 * @param imag
 *            imaginary part
 * @since IOP 1.1
 * @m3.pattern synexia:fop
 * @m3.algorithm Represent an immutable pair of unrestricted Java double components; operations use ordinary floating-point arithmetic and preserve its rounding, overflow, NaN and signed-zero behavior.
 * @m3.dataStructures Java record with real and imag double components and three shared constants.
 * @m3.member {@code FIELD:real} pattern=synexia:fop; algorithm=Store the real component without validation or normalization.; data.structures=One immutable double record component.; description=Unrestricted real component.
 * @m3.member {@code FIELD:imag} pattern=synexia:fop; algorithm=Store the imaginary component without validation or normalization.; data.structures=One immutable double record component.; description=Unrestricted imag component.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/crystalline/ComplexNumber.java}
 * blob {@code 11b3f87004bdc7999e30b09e4c50e03a96d77844} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public record ComplexNumber(double real, double imag) {

    /** Zero complex number.
     * @m3.pattern synexia:fop
     * @m3.algorithm Expose the shared complex value with real positive zero and imaginary positive zero.
     * @m3.dataStructures Static final ComplexNumber record instance initialized from integer zeros.
     */
    public static final ComplexNumber ZERO = new ComplexNumber(0, 0);

    /** One complex number.
     * @m3.pattern synexia:fop
     * @m3.algorithm Expose the shared complex value with real one and imaginary positive zero.
     * @m3.dataStructures Static final ComplexNumber record instance initialized from one and zero.
     */
    public static final ComplexNumber ONE = new ComplexNumber(1, 0);

    /** Imaginary unit i.
     * @m3.pattern synexia:fop
     * @m3.algorithm Expose the shared imaginary unit with real positive zero and imaginary one.
     * @m3.dataStructures Static final ComplexNumber record instance initialized from zero and one.
     */
    public static final ComplexNumber I = new ComplexNumber(0, 1);

    /**
     * Add two complex numbers.
     *
     * @param other
     *            the other complex number
     * @return sum z + other
     * @m3.pattern synexia:fop
     * @m3.algorithm Allocate a result whose components are real plus other.real and imag plus other.imag; dereferencing a null operand fails with NullPointerException.
     * @m3.dataStructures Two component-wise double additions and a new ComplexNumber record.
     */
    public ComplexNumber add(ComplexNumber other) {
        return new ComplexNumber(real + other.real, imag + other.imag);
    }

    /**
     * Subtract two complex numbers.
     *
     * @param other
     *            the other complex number
     * @return difference z - other
     * @m3.pattern synexia:fop
     * @m3.algorithm Allocate a result whose components are real minus other.real and imag minus other.imag; dereferencing a null operand fails with NullPointerException.
     * @m3.dataStructures Two component-wise double subtractions and a new ComplexNumber record.
     */
    public ComplexNumber subtract(ComplexNumber other) {
        return new ComplexNumber(real - other.real, imag - other.imag);
    }

    /**
     * Multiply two complex numbers.
     *
     * @param other
     *            the other complex number
     * @return product z * other
     * @m3.pattern synexia:fop
     * @m3.algorithm Allocate a result with real component real*other.real-imag*other.imag and imaginary component real*other.imag+imag*other.real, in source evaluation order; null operands fail on dereference.
     * @m3.dataStructures Four double products, one subtraction, one addition and a new ComplexNumber record.
     */
    public ComplexNumber multiply(ComplexNumber other) {
        return new ComplexNumber(real * other.real - imag * other.imag, real * other.imag + imag * other.real);
    }

    /**
     * Compute z² (complex squaring).
     *
     * @return z squared
     * @m3.pattern synexia:fop
     * @m3.algorithm Allocate a result with real*real-imag*imag and left-associated 2*real*imag; preserve ordinary floating-point overflow and rounding.
     * @m3.dataStructures Double component arithmetic and a new ComplexNumber record.
     */
    public ComplexNumber square() {
        return new ComplexNumber(real * real - imag * imag, 2 * real * imag);
    }

    /**
     * Compute |z| (modulus/magnitude).
     *
     * @return modulus
     * @m3.pattern synexia:fop
     * @m3.algorithm Return Math.sqrt(real*real+imag*imag); the intermediate sum can overflow and is not a scaled hypot implementation.
     * @m3.dataStructures Two squared double components, their sum and Math.sqrt result.
     */
    public double modulus() {
        return Math.sqrt(real * real + imag * imag);
    }

    /**
     * Compute |z|² (squared modulus - faster).
     *
     * @return squared modulus
     * @m3.pattern synexia:fop
     * @m3.algorithm Return real*real+imag*imag using ordinary double multiplication and addition, including overflow and NaN propagation.
     * @m3.dataStructures Two double products and one sum.
     */
    public double modulusSquared() {
        return real * real + imag * imag;
    }

    /**
     * Compute arg(z) (argument/phase angle).
     *
     * @return angle in radians
     * @m3.pattern synexia:fop
     * @m3.algorithm Return Math.atan2(imag,real) in radians, preserving its signed-zero and nonfinite-input semantics.
     * @m3.dataStructures Stored imaginary and real doubles passed to Math.atan2.
     */
    public double argument() {
        return Math.atan2(imag, real);
    }

    /**
     * Compute complex conjugate z*.
     *
     * @return conjugate
     * @m3.pattern synexia:fop
     * @m3.algorithm Allocate a result retaining real and applying unary negation to imag, including reversal of the imaginary zero sign.
     * @m3.dataStructures Existing real double, negated imaginary double and a new ComplexNumber record.
     */
    public ComplexNumber conjugate() {
        return new ComplexNumber(real, -imag);
    }

    /**
     * Create from polar coordinates.
     *
     * @param r
     *            modulus
     * @param theta
     *            angle in radians
     * @return complex number
     * @m3.pattern synexia:fop
     * @m3.algorithm Allocate a result with r*Math.cos(theta) and r*Math.sin(theta), without validating radius or angle; negative radii and nonfinite inputs follow the underlying double operations.
     * @m3.dataStructures Two double arguments, Math trigonometric results and a new ComplexNumber record.
     */
    public static ComplexNumber fromPolar(double r, double theta) {
        return new ComplexNumber(r * Math.cos(theta), r * Math.sin(theta));
    }
}
