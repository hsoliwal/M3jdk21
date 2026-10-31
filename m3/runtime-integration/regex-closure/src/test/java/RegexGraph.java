// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Copyright 2026 Hitesh Soliwal and contributors

/** Independent finite graph oracle. It never calls a candidate closure or transition. */
final class RegexGraph {
    static final int STAR = 1 << 16;

    private RegexGraph() {}

    static long stars(int[] atoms) {
        long mask = 0;
        for (int at = 0; at < atoms.length; at++) {
            if ((atoms[at] & STAR) != 0) mask |= 1L << at;
        }
        return mask;
    }

    static long close(long frontier, long stars) {
        long previous;
        do {
            previous = frontier;
            frontier |= (frontier & stars) << 1;
        } while (frontier != previous);
        return frontier;
    }

    static long[] closures(int[] atoms) {
        long mask = stars(atoms);
        long[] rows = new long[atoms.length + 1];
        for (int at = 0; at < rows.length; at++) rows[at] = close(1L << at, mask);
        return rows;
    }

    static int[] atoms(int count, long stars, int style) {
        int[] atoms = new int[count];
        for (int at = 0; at < count; at++) {
            char unit = switch (style) {
                case 0 -> 'a';
                case 1 -> '.';
                default -> at % 3 == 0 ? 'a' : at % 3 == 1 ? 'b' : '.';
            };
            atoms[at] = unit | (((stars >>> at) & 1L) != 0 ? STAR : 0);
        }
        return atoms;
    }

    static long advance(CharSequence input, int start, int length,
                        long frontier, int[] atoms) {
        long stars = stars(atoms);
        for (int at = start; at < start + length; at++) {
            char unit = input.charAt(at);
            if (Character.isSurrogate(unit)) return -1L;
            long next = 0;
            for (int token = 0; token < atoms.length; token++) {
                char literal = (char) atoms[token];
                boolean accepts = literal == '.' ? !lineTerminator(unit) : literal == unit;
                if (accepts && (frontier & (1L << token)) != 0) {
                    int destination = (atoms[token] & STAR) != 0 ? token : token + 1;
                    next |= 1L << destination;
                }
            }
            frontier = close(next, stars);
        }
        return frontier;
    }

    private static boolean lineTerminator(char unit) {
        return unit == 10 || unit == 13 || unit == 133 || unit == 8232 || unit == 8233;
    }
}
