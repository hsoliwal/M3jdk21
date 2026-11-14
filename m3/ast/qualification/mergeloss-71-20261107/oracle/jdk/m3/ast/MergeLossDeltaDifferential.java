// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.ast;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.SplittableRandom;

/**
 * Merge-loss leaf 71 differential for M3SyntaxDelta.match. Seeded tree pairs over one source (unchanged map) or a
 * one-span edit, with nested nodes, tokens and runs of zero to three equivalent zero-width MISSING recovery tokens at
 * the same byte, so new nodes can have several equivalent old occurrences and the reverse. One line per pair:
 * candidates, oldToNew and newToOld, plus AMBIG (new nodes whose unique-candidate old set has two or more members,
 * computed by brute force with the matcher's equivalence: mapped span, flags, kind, field, source bytes and
 * M3SyntaxIndex.sameCandidateSubtree). Compare the files of the master and restored classes: every differing pair
 * must be a pair the restore intends (head mapping = master mapping minus the AMBIG new nodes' pairs).
 *
 * usage: java -cp ast-classes;oracle jdk.m3.ast.MergeLossDeltaDifferential pairs seed out.txt
 */
public final class MergeLossDeltaDifferential {
    private record Edit(String oldSha256, String newSha256, int from, int to, int insertedBytes)
            implements M3SyntaxDelta.SourceEditMap {
        public boolean touchesOldSpan(int start, int end) {
            if (from == to) return start <= from && from <= end;
            return start < to && from < end;
        }
        public int oldToNew(int p, boolean rightAffinity) {
            if (p < from) return p;
            if (p == from) return rightAffinity ? from + insertedBytes : from;
            if (p < to) return -1;
            return p + insertedBytes - (to - from);
        }
    }

    public static void main(String[] args) throws IOException {
        int pairs = Integer.parseInt(args[0]);
        SplittableRandom random = new SplittableRandom(Long.decode(args[1]));
        long ambiguousNodes = 0;
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(Path.of(args[2]), StandardCharsets.UTF_8))) {
            for (int pair = 0; pair < pairs; pair++) {
                byte[] oldSource = source(random);
                long shape = random.nextLong();
                M3SyntaxTree before = tree(oldSource, new SplittableRandom(shape), random.nextInt(4));
                byte[] newSource = oldSource;
                Edit edit;
                if (random.nextInt(3) == 0 && oldSource.length > 1) {
                    int from = random.nextInt(oldSource.length), to = from + random.nextInt(oldSource.length - from + 1);
                    byte[] inserted = new byte[random.nextInt(3)];
                    Arrays.fill(inserted, (byte) 'z');
                    newSource = new byte[oldSource.length - (to - from) + inserted.length];
                    System.arraycopy(oldSource, 0, newSource, 0, from);
                    System.arraycopy(inserted, 0, newSource, from, inserted.length);
                    System.arraycopy(oldSource, to, newSource, from + inserted.length, oldSource.length - to);
                    M3SyntaxTree after = tree(newSource, new SplittableRandom(shape), random.nextInt(4));
                    edit = new Edit(before.sourceSha256(), after.sourceSha256(), from, to, inserted.length);
                    ambiguousNodes += record(out, pair, before, after, edit);
                } else {
                    M3SyntaxTree after = tree(newSource, new SplittableRandom(shape), random.nextInt(4));
                    edit = new Edit(before.sourceSha256(), after.sourceSha256(), -1, -1, 0);
                    ambiguousNodes += record(out, pair, before, after, edit);
                }
            }
        }
        System.out.printf("PAIRS %d ambiguous_new_nodes=%d%n", pairs, ambiguousNodes);
    }

    private static int record(PrintWriter out, int pair, M3SyntaxTree before, M3SyntaxTree after, Edit edit) {
        M3SyntaxIndex oldIndex = new M3SyntaxIndex(before), newIndex = new M3SyntaxIndex(after);
        M3SyntaxDelta delta = M3SyntaxDelta.match(oldIndex, newIndex, edit);
        int[] oldToNew = new int[delta.oldNodes()], newToOld = new int[delta.newNodes()];
        for (int i = 0; i < oldToNew.length; i++) oldToNew[i] = delta.newNodeForOld(i);
        for (int i = 0; i < newToOld.length; i++) newToOld[i] = delta.oldNodeForNew(i);
        int[] owners = new int[newToOld.length];
        for (int node = 0; node < before.size(); node++) {
            int unique = uniqueCandidate(oldIndex, newIndex, node, edit);
            if (unique >= 0) owners[unique]++;
        }
        StringBuilder ambiguous = new StringBuilder();
        int count = 0;
        for (int n = 0; n < owners.length; n++) {
            if (owners[n] > 1) {
                ambiguous.append(n).append(',');
                count++;
            }
        }
        out.printf("%d c=%d o2n=%s n2o=%s AMBIG=%s%n", pair, delta.candidates(), Arrays.toString(oldToNew),
                Arrays.toString(newToOld), ambiguous);
        return count;
    }

    /** The matcher's candidate rule, by brute force over every new node: the one equivalent node, or -1. */
    private static int uniqueCandidate(M3SyntaxIndex oldIndex, M3SyntaxIndex newIndex, int node, Edit edit) {
        M3SyntaxTree prior = oldIndex.tree(), next = newIndex.tree();
        int from = prior.startByte(node), to = prior.endByte(node);
        if (edit.touchesOldSpan(from, to)) return -1;
        int mappedStart = edit.oldToNew(from, true), mappedEnd = edit.oldToNew(to, false);
        if (mappedStart < 0 || mappedEnd < mappedStart || mappedEnd > next.sourceLength()
                || mappedEnd - mappedStart != to - from) return -1;
        byte[] oldBytes = prior.sourceBytes(), newBytes = next.sourceBytes();
        int found = -1;
        for (int candidate = 0; candidate < next.size(); candidate++) {
            if (next.startByte(candidate) != mappedStart || next.endByte(candidate) != mappedEnd
                    || prior.flags(node) != next.flags(candidate) || !prior.kind(node).equals(next.kind(candidate))
                    || !prior.field(node).equals(next.field(candidate))
                    || !oldIndex.sameCandidateSubtree(node, newIndex, candidate)
                    || !Arrays.equals(oldBytes, from, to, newBytes, mappedStart, mappedEnd)) continue;
            if (found >= 0) return -1;
            found = candidate;
        }
        return found;
    }

    private static byte[] source(SplittableRandom random) {
        byte[] source = new byte[1 + random.nextInt(24)];
        for (int i = 0; i < source.length; i++) source[i] = (byte) ('a' + random.nextInt(3));
        return source;
    }

    /** Root over the source; children are tokens, nested groups and recovery-token runs (shape from the seed). */
    private static M3SyntaxTree tree(byte[] source, SplittableRandom shape, int extraMissing) {
        M3SyntaxTree.Builder builder = M3SyntaxTree.builder("partial/error", source);
        builder.open("file", "", 0, M3SyntaxTree.NAMED);
        int at = 0, depth = 0;
        while (at < source.length) {
            int choice = shape.nextInt(6);
            if (choice == 0 && depth < 3) {
                builder.open("group", shape.nextBoolean() ? "body" : "", at, M3SyntaxTree.NAMED);
                depth++;
            } else if (choice == 1 && depth > 0) {
                builder.close(at);
                depth--;
            } else if (choice == 2) {
                int run = shape.nextInt(4) + (shape.nextInt(4) == 0 ? extraMissing : 0);
                for (int i = 0; i < run; i++)
                    builder.leaf("missing", "expected", at, at, M3SyntaxTree.TOKEN | M3SyntaxTree.MISSING);
            } else {
                int to = Math.min(source.length, at + 1 + shape.nextInt(3));
                builder.leaf("token", "", at, to, M3SyntaxTree.TOKEN);
                at = to;
            }
        }
        for (; depth > 0; depth--) builder.close(source.length);
        builder.close(source.length);
        return builder.build();
    }
}
