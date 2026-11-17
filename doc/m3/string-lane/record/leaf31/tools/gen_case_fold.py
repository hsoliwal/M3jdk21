# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others
"""gen_case_fold.py <CaseFolding.txt> <case_methods.txt> <case_family.txt>

CASE_FOLD_TABLE v1 (2026-11-17). Writes <case_family.txt> = <case_methods.txt> + the private holder class
String.CaseFolding generated from Unicode's CaseFolding.txt (15.0.0): status C and S folds as runs (first code point,
count, stride 1 or 2, fold minus code point; a code point whose only fold is status F gets a run entry with difference
0), status F folds as (code point, fold of 2 or 3 code points, 0 padded). Status T is left out. Asserts: no simple fold
leaves the code point's plane, every F fold is 2..3 BMP code points, and the run expansion gives back exactly the C+S
map. CPU only, deterministic."""
import sys

src, methods, out = sys.argv[1:4]
simple, full = {}, {}
for line in open(src, encoding="utf-8"):
    line = line.split("#")[0].strip()
    if not line:
        continue
    code, status, mapping = [x.strip() for x in line.split(";")[:3]]
    cp, targets = int(code, 16), [int(x, 16) for x in mapping.split()]
    if status in ("C", "S"):
        assert len(targets) == 1 and cp not in simple
        simple[cp] = targets[0]
    elif status == "F":
        assert 2 <= len(targets) <= 3 and all(t <= 0xFFFF for t in targets) and cp not in full
        full[cp] = targets
assert all((c > 0xFFFF) == (t > 0xFFFF) for c, t in simple.items()), "a simple fold leaves its plane"
deltas = dict((c, t - c) for c, t in simple.items())
for c in full:
    deltas.setdefault(c, 0)
left, runs = set(deltas), []
for c in sorted(deltas):
    if c not in left:
        continue
    best = (1, 1)
    for stride in (1, 2):
        n = 1
        while c + n * stride in left and deltas[c + n * stride] == deltas[c]:
            n += 1
        if n > best[0]:
            best = (n, stride)
    for k in range(best[0]):
        left.discard(c + k * best[1])
    runs.append((c, best[0], best[1], deltas[c]))
back = {}
for c, n, stride, d in runs:
    for k in range(n):
        back[c + k * stride] = c + k * stride + d
assert back == {c: simple.get(c, c) for c in deltas}


def hexcp(v):
    return "0x%04X" % v if v <= 0xFFFF else "0x%05X" % v


def rows(items, per):
    """The items packed greedily into lines of at most 100 columns, at most per items a line."""
    lines, line, n = [], "           ", 0
    for item in items:
        if n == per or len(line) + 1 + len(item) > 100:
            lines.append(line)
            line, n = "           ", 0
        line += " " + item
        n += 1
    lines.append(line)
    return lines


run_items = ["%s, %d, %d, %d," % (hexcp(c), n, s, d) for c, n, s, d in runs]
full_items = ["%s, %s," % (hexcp(c), ", ".join(hexcp(t) for t in (full[c] + [0])[:3])) for c in sorted(full)]
run_items[-1] = run_items[-1].rstrip(",")
full_items[-1] = full_items[-1].rstrip(",")
holder = """
    /**
     * Unicode 15.0.0 case folding as {@code CaseFolding.txt} gives it: the folds of status C and
     * S (simple folding) and of status F (full folding), built once. Generated from that file;
     * the Turkic folds (status T) are left out.
     */
    private static final class CaseFolding {
        /**
         * Runs of simple folds: first code point, count, stride, and the fold minus the code
         * point. A difference of 0 marks a code point whose only fold is a full one.
         */
        private static final int[] RUNS = {
%s
        };
        /** Full folds: a code point and the two or three code points of its fold, 0 padded. */
        private static final int[] FULL_FOLDS = {
%s
        };
        /** {@link #LATIN1}: the fold of the unit is more than one code point. */
        static final char EXPANDS = (char) 0xFFFF;
        /** The code points with a fold, ascending. */
        static final int[] KEYS;
        /** The simple fold of each key. */
        static final int[] SIMPLE;
        /** The full fold of each key as {@code offset << 2 | length} in EXPANSIONS, or -1. */
        static final int[] FULL;
        /** The units of the full folds, all in the Basic Multilingual Plane. */
        static final char[] EXPANSIONS;
        /** The full fold of each Latin-1 unit, or {@link #EXPANDS}. */
        static final char[] LATIN1 = new char[256];

        static {
            int count = 0;
            for (int r = 1; r < RUNS.length; r += 4) {
                count += RUNS[r];
            }
            long[] entries = new long[count];
            int n = 0;
            for (int r = 0; r < RUNS.length; r += 4) {
                for (int k = 0; k < RUNS[r + 1]; k++) {
                    int point = RUNS[r] + k * RUNS[r + 2];
                    entries[n++] = (long) point << 32 | (point + RUNS[r + 3]);
                }
            }
            java.util.Arrays.sort(entries);
            KEYS = new int[count];
            SIMPLE = new int[count];
            FULL = new int[count];
            for (int i = 0; i < count; i++) {
                KEYS[i] = (int) (entries[i] >>> 32);
                SIMPLE[i] = (int) entries[i];
                FULL[i] = -1;
            }
            EXPANSIONS = new char[FULL_FOLDS.length / 4 * 3];
            int used = 0;
            for (int f = 0; f < FULL_FOLDS.length; f += 4) {
                int length = FULL_FOLDS[f + 3] == 0 ? 2 : 3;
                for (int k = 0; k < length; k++) {
                    EXPANSIONS[used + k] = (char) FULL_FOLDS[f + 1 + k];
                }
                FULL[find(FULL_FOLDS[f])] = used << 2 | length;
                used += length;
            }
            for (int unit = 0; unit < LATIN1.length; unit++) {
                int i = find(unit);
                LATIN1[unit] = i < 0 ? (char) unit : FULL[i] >= 0 ? EXPANDS : (char) SIMPLE[i];
            }
        }

        /** The index of {@code point} among the keys, or a negative value. */
        static int find(int point) {
            return java.util.Arrays.binarySearch(KEYS, point);
        }

        /** The simple fold of {@code point}. */
        static int simpleFold(int point) {
            int i = find(point);
            return i < 0 ? point : SIMPLE[i];
        }

        /** The full fold of {@code point} when it is one code point, else -1. */
        static int fullFold(int point) {
            if (point < LATIN1.length && LATIN1[point] != EXPANDS) return LATIN1[point];
            int i = find(point);
            return i < 0 ? point : FULL[i] < 0 ? SIMPLE[i] : -1;
        }

        /** Appends the full fold of {@code point}, which is more than one code point. */
        static void appendFullFold(StringBuilder out, int point) {
            int code = FULL[find(point)];
            out.append(EXPANSIONS, code >>> 2, code & 3);
        }
    }
""" % ("\n".join(rows(run_items, 4)), "\n".join(rows(full_items, 3)))
text = open(methods, encoding="utf-8", newline="").read().replace("\r", "")
open(out, "w", encoding="utf-8", newline="").write(text.rstrip("\n") + "\n" + holder)
print("runs\t%d\tsimple\t%d\tfull\t%d\tkeys\t%d" % (len(runs), len(simple), len(full), len(deltas)))
