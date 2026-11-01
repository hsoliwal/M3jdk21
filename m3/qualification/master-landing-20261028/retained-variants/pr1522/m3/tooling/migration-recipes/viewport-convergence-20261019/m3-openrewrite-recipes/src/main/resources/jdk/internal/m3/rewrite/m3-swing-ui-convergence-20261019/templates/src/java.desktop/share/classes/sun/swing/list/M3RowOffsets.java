/*
 * Copyright (c) 2026, Hitesh Soliwal (Synexia) and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package sun.swing.list;

/**
 * Offsets of rows over a dense lane of row heights: where a row starts and which row holds an
 * offset. This is the seam between {@link M3ListRows} and the prefix-sum owner behind it:
 * {@link M3PrefixRowOffsets} today, an {@code m3/collections} owner when one ships in the JDK
 * (m3/docs/swing-viewport/DESIGN.md section 9, ALGORITHM_CATALOGUE.tsv).
 */
public interface M3RowOffsets {

    /** The number of rows indexed. */
    int rowCount();

    /**
     * The sum of the heights of rows {@code 0 .. row - 1} in {@code int} arithmetic, wrapping
     * exactly as a running {@code int} sum does; {@code 0 <= row <= rowCount()}.
     */
    int startOf(int row);

    /**
     * Whether every height is non-negative and the sum of all heights fits an {@code int}, so
     * {@link #startOf} never decreases and {@link #firstEndingAfter} is defined.
     */
    boolean monotone();

    /**
     * For monotone offsets and {@code offset >= 0}: the first row whose end,
     * {@code startOf(row + 1)}, is greater than {@code offset}; {@code rowCount()} when none is.
     */
    int firstEndingAfter(long offset);
}
