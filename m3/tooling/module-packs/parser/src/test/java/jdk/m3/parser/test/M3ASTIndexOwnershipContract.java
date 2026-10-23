// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser.test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import jdk.m3.parser.M3ASTIndex;
import jdk.m3.parser.M3File;

/** Ownership/contract oracle: a caller may retain a sealed Builder indefinitely. */
public final class M3ASTIndexOwnershipContract {
    private static final String[] LANES = {
            "kinds", "parents", "starts", "ends", "flags", "first", "next", "childCounts" };
    private static int checks;
    private M3ASTIndexOwnershipContract() { }
    private static void check(boolean accepted, String description) {
        checks++;
        if (!accepted) throw new AssertionError(description);
    }
    private static Object field(Object owner, String name) throws Exception {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(owner);
    }
    private static void sealedError(Runnable task) {
        try { task.run(); throw new AssertionError("sealed builder accepted mutation"); }
        catch (IllegalStateException expected) { checks++; }
    }
    private static void illegal(Runnable task) {
        try { task.run(); throw new AssertionError("bad contract input accepted"); }
        catch (IllegalArgumentException expected) { checks++; }
    }
    private static void released(M3ASTIndex.Builder builder) throws Exception {
        for (Field field : builder.getClass().getDeclaredFields()) {
            if (!field.getType().isPrimitive() && !Modifier.isStatic(field.getModifiers())) {
                field.setAccessible(true);
                check(field.get(builder) == null, "sealed builder retains " + field.getName());
            }
        }
        // Validate error precedence even with released arrays and deliberately bad arguments.
        sealedError(() -> builder.add(-7, null, -100, 100, -1));
        sealedError(() -> builder.completeRange(-7, -100, 100, true));
        sealedError(builder::seal);
    }
    private static void publication(int size) throws Exception {
        M3File source = M3File.of(URI.create("memory:/Ownership.java"), "abcdefghijklmnopqrstuvwxyz");
        M3ASTIndex.Builder builder = M3ASTIndex.builder(source, M3ASTIndex.Domain.ECLIPSE_JDT, 100);
        for (int row = 0; row < size; row++) {
            builder.add(row == 0 ? -1 : (row - 1) / 2, "K" + row % 16,
                    0, source.length(), M3ASTIndex.EXACT_RANGE | M3ASTIndex.SYNTAX_ONLY);
        }
        Object[] before = new Object[LANES.length];
        for (int i = 0; i < before.length; i++) before[i] = field(builder, LANES[i]);
        Object names = field(builder, "names");
        M3ASTIndex index = builder.seal();
        check(index.source() == source, "one source owner survives");
        check(index.nodeCount() == size, "published size");
        check(index.primitiveBytes() == (long) size * 32, "eight original coordinate planes");
        for (int i = 0; i < LANES.length; i++) {
            int[] actual = (int[]) field(index, LANES[i]);
            check(actual.length == size, "only initialized coordinates survive");
            if (((int[]) before[i]).length == size) {
                check(actual == before[i], "full " + LANES[i] + " buffer transferred");
            } else {
                check(actual != before[i], "partial " + LANES[i] + " buffer trimmed");
            }
        }
        if (size >= 16) check(field(index, "names") == names, "full kind dictionary transferred");
        released(builder);
        for (int row = 0; row < size; row++) {
            check(index.kind(row).equals("K" + row % 16), "kind IDs preserve dictionary identity");
            check(index.parent(row) == (row == 0 ? -1 : (row - 1) / 2), "parent coordinate");
            check(index.sourceText(row).equals(source.text()), "exact source span survives builder release");
            List<Integer> actual = new ArrayList<>();
            for (int child = index.firstChild(row); child != -1; child = index.nextSibling(child)) actual.add(child);
            List<Integer> expected = new ArrayList<>();
            for (int child = 1; child < size; child++) if ((child - 1) / 2 == row) expected.add(child);
            check(actual.equals(expected) && actual.size() == index.childCount(row), "sibling/child topology");
        }
    }
    private static void errorsAndUnknownRanges() throws Exception {
        M3File source = M3File.of(URI.create("memory:/Ranges.java"), "abcdef");
        M3ASTIndex.Builder builder = M3ASTIndex.builder(source, M3ASTIndex.Domain.ECLIPSE_JDT, 3);
        sealedError(builder::seal); // Empty refusal leaves this open for valid later input.
        illegal(() -> builder.add(0, "root", -1, -1, 0));
        int root = builder.add(-1, "root", -1, -1, 0);
        int child = builder.add(root, "child", 1, 5, M3ASTIndex.EXACT_RANGE);
        illegal(() -> builder.completeRange(root, 2, 4, true));
        builder.completeRange(root, 0, 6, true);
        illegal(() -> builder.completeRange(root, 0, 6, true));
        illegal(() -> builder.add(child, "escape", 0, 6, M3ASTIndex.EXACT_RANGE));
        illegal(() -> builder.add(root, "unknown", -1, -1, M3ASTIndex.EXACT_RANGE));
        builder.add(root, "unknown", -1, -1, M3ASTIndex.SYNTAX_ONLY);
        illegal(() -> builder.add(root, "overflow", -1, -1, 0));
        M3ASTIndex index = builder.seal();
        released(builder);
        check(index.sourceText(root).equals("abcdef") && index.sourceText(child).equals("bcde"), "completed range retained");
        check(!index.hasExactRange(2), "unknown range contract retained");
        try { index.sourceText(2); throw new AssertionError("unknown source text available"); }
        catch (IllegalStateException expected) { checks++; }
        try { index.kind(3); throw new AssertionError("invalid coordinate accepted"); }
        catch (IndexOutOfBoundsException expected) { checks++; }
    }
    public static void main(String[] args) throws Exception {
        for (int size : new int[] { 1, 16, 17, 31, 32, 33, 49, 50, 74, 75, 100 }) publication(size);
        errorsAndUnknownRanges();
        System.out.println("M3_AST_INDEX_OWNERSHIP_PASS checks=" + checks);
    }
}
