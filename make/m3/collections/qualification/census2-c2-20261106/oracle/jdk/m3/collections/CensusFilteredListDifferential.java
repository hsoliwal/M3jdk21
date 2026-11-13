// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * Census pass-2 C2 before/after differential of M3FilteredLists (leaf P2-043): seeded episodes of filtered-view
 * cursor traffic (next, previous, hasNext, hasPrevious, remove, set, add, filtered-list add/remove/set) over four
 * backing kinds, with and without same-thread structural writes to the backing list between cursor steps. Every
 * operation prints one line (operation, result or exception class, backing list after it); run on the master
 * classes and on the edited classes and compare. The only documented difference: a cursor write (remove, set, add)
 * after a structural change of a fail-fast backing list now throws ConcurrentModificationException without writing.
 *
 * usage: java CensusFilteredListDifferential EPISODES OPS_PER_EPISODE   (prints the transcript, then a digest line)
 */
public final class CensusFilteredListDifferential {
    private static final long SEED = 0x43325032303433L; // "C2P2043"
    private static final Predicate<Integer> EVEN = n -> (n & 1) == 0;

    private CensusFilteredListDifferential() { }

    private static List<Integer> backing(int kind, List<Integer> values) {
        return switch (kind) {
            case 0 -> new ArrayList<>(values);
            case 1 -> new LinkedList<>(values);
            case 2 -> Collections.synchronizedList(new ArrayList<>(values));
            default -> new CopyOnWriteArrayList<>(values);
        };
    }

    private static String step(Random random, List<Integer> source, List<Integer> view, ListIterator<Integer>[] cursor,
            boolean external) {
        int op = random.nextInt(external ? 14 : 11);
        int value = random.nextInt(20);
        try {
            Object result = switch (op) {
                case 0, 1 -> cursor[0].hasNext() ? cursor[0].next() : "END";
                case 2 -> cursor[0].hasPrevious() ? cursor[0].previous() : "BEGIN";
                case 3 -> cursor[0].nextIndex() + "/" + cursor[0].previousIndex();
                case 4 -> { cursor[0].remove(); yield "removed"; }
                case 5 -> { cursor[0].set(value & ~1); yield "set"; }
                case 6 -> { cursor[0].add(value & ~1); yield "added"; }
                case 7 -> view.size();
                case 8 -> { view.add(value & ~1); yield "view.add"; }
                case 9 -> view.isEmpty() ? "empty" : view.remove(random.nextInt(view.size()));
                case 10 -> { cursor[0] = view.listIterator(random.nextInt(view.size() + 1)); yield "reopen"; }
                case 11 -> { source.add(random.nextInt(source.size() + 1), value); yield "source.add"; }
                case 12 -> source.isEmpty() ? "empty" : source.remove(random.nextInt(source.size()));
                default -> source.isEmpty() ? "empty" : source.set(random.nextInt(source.size()), value);
            };
            return op + " " + result + " " + source;
        } catch (RuntimeException failure) {
            return op + " " + failure.getClass().getSimpleName() + " " + source;
        }
    }

    public static void main(String[] args) throws Exception {
        int episodes = Integer.parseInt(args[0]);
        int ops = Integer.parseInt(args[1]);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        StringBuilder out = new StringBuilder(1 << 16);
        long lines = 0;
        for (int episode = 0; episode < episodes; episode++) {
            Random random = new Random(SEED + 0x9E3779B97F4A7C15L * episode); // per-episode stream: no cross-episode cascade
            int kind = episode & 3;
            boolean external = (episode & 4) != 0;
            List<Integer> initial = new ArrayList<>();
            for (int i = random.nextInt(12); i > 0; i--) initial.add(random.nextInt(20));
            List<Integer> source = backing(kind, initial);
            List<Integer> view = M3FilteredLists.filtered(source, EVEN);
            @SuppressWarnings({"unchecked", "rawtypes"})
            ListIterator<Integer>[] cursor = new ListIterator[] {view.listIterator()};
            out.append("E ").append(episode).append(' ').append(kind).append(' ').append(external).append(' ')
                    .append(source).append('\n');
            for (int i = 0; i < ops; i++) {
                out.append(step(random, source, view, cursor, external)).append('\n');
                lines++;
            }
            if (out.length() > (1 << 15)) {
                byte[] bytes = out.toString().getBytes(StandardCharsets.UTF_8);
                digest.update(bytes);
                System.out.print(out);
                out.setLength(0);
            }
        }
        byte[] bytes = out.toString().getBytes(StandardCharsets.UTF_8);
        digest.update(bytes);
        System.out.print(out);
        StringBuilder hex = new StringBuilder();
        for (byte b : digest.digest()) hex.append(String.format("%02x", b));
        System.out.println("DIGEST episodes=" + episodes + " ops=" + lines + " sha256=" + hex);
    }
}
