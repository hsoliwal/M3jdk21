/*
 * Copyright 2018-2020 Raffaello Giulietti
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.fasterxml.jackson.core.io.schubfach;

//FLLDCIM: M3 - JACKSON_NUMBERS_TO_JDK v2: doubles and floats render in java.base, the JDK's one Schubfach

/**
 * This class exposes a method to render a {@code double} as a string.
 *
 * <p>M3JDK overlay JACKSON_NUMBERS_TO_JDK v2: {@link #toString(double)} returns
 * {@link Double#toString(double)}, which runs the same Schubfach algorithm in java.base. The copied
 * algorithm is retained under upstream-excluded/.
 *
 * @author Raffaello Giulietti
 */
final public class DoubleToDecimal {

    /** The longest string {@link #toString(double)} returns (upstream: H + 7, H = 17). */
    public final int MAX_CHARS = 24;

    private DoubleToDecimal() {
    }

    /**
     * Returns the string rendering of a {@code double}: {@link Double#toString(double)}.
     *
     * @param v the {@code double} to be rendered.
     * @return a string rendering of the argument.
     */
    public static String toString(double v) {
        return Double.toString(v);
    }
}
