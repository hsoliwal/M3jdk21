/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: GPL-2.0-only
 *
 * @test
 * @summary Failed indexed-array growth preserves all planes and remains usable after retry
 * @modules java.base/jdk.internal.m3:+open
 *          java.base/jdk.internal.org.objectweb.asm
 *          java.instrument
 * @build M3IndexedArrayGrowthAgent M3IndexedArrayGrowthFault
 * @run driver M3IndexedArrayGrowthFailureTest build-agent
 * @run main/othervm -Xint --add-exports=java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED -javaagent:M3IndexedArrayGrowthAgent.jar=M3IndexedArrayGrowthHook.jar M3IndexedArrayGrowthFailureTest
 * @run main/othervm -Xmixed --add-exports=java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED -javaagent:M3IndexedArrayGrowthAgent.jar=M3IndexedArrayGrowthHook.jar M3IndexedArrayGrowthFailureTest
 */

import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import jdk.internal.m3.M3IndexedArray;

public final class M3IndexedArrayGrowthFailureTest {
    private static final String[] PLANE_NAMES = {
        "values", "states", "generations", "leaseCounts", "freeLinks", "positions", "order"
    };
    private static int checks;
    private static int failures;

    private record Item(int rank, int sequence) { }

    private M3IndexedArrayGrowthFailureTest() { }

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("build-agent")) {
            buildAgent();
            return;
        }
        for (int capacity : new int[] {8, 12, 27}) {
            for (int failureAt = 1; failureAt <= 7; failureAt++) {
                verifyFailure(capacity, failureAt);
            }
        }
        if (failures != 0) {
            throw new AssertionError("GROWTH_FAILURE_ATOMICITY violations=" + failures
                    + " checks=" + checks);
        }
        System.out.println("M3_INDEXED_ARRAY_GROWTH_FAILURE_PASS checks=" + checks + " cases=21");
    }

    private static void verifyFailure(int capacity, int failureAt) throws Exception {
        M3IndexedArray<Item> owner = new M3IndexedArray<>();
        ArrayList<Item> expected = new ArrayList<>();
        ArrayList<Long> handles = new ArrayList<>();
        for (int i = 0; i < capacity; i++) {
            Item item = new Item(i % 4, i);
            expected.add(item);
            handles.add(owner.append(item));
        }
        require(M3IndexedArrayGrowthAgent.failure == null, "fault transformer failed: "
                + M3IndexedArrayGrowthAgent.failure);
        require(M3IndexedArrayGrowthAgent.sites == 7, "actual owner was not instrumented");
        long stale = handles.remove(2);
        expected.remove(2);
        require(owner.remove(stale), "initial remove");
        Item replacement = new Item(2, capacity);
        expected.add(replacement);
        long reused = owner.append(replacement);
        handles.add(reused);
        require((int) reused == (int) stale && reused != stale, "generation-qualified reuse");
        owner.retain(handles.get(0));
        owner.retain(handles.get(0));
        owner.retain(handles.get(1));
        List<Item> filtered = owner.filtered(item -> (item.rank() & 1) == 0);
        List<Item> sorted = owner.sorted(Comparator.comparingInt(Item::rank));
        List<Item> unleased = owner.maskedStates(M3IndexedArray.LIVE, M3IndexedArray.LEASED);
        List<Item> expectedFiltered = expected.stream().filter(item -> (item.rank() & 1) == 0).toList();
        ArrayList<Item> expectedSorted = new ArrayList<>(expected);
        expectedSorted.sort(Comparator.comparingInt(Item::rank));
        List<Item> expectedUnleased = expected.subList(2, expected.size());
        same(expectedFiltered, filtered, "initial filtered");
        same(expectedSorted, sorted, "initial sorted");
        same(expectedUnleased, unleased, "initial unleased");
        Iterator<Item> bound = sorted.iterator();
        List<Item> range = sorted.subList(1, sorted.size() - 1);
        Object[] planes = planes(owner);
        Object[] contents = new Object[planes.length];
        for (int i = 0; i < planes.length; i++) {
            contents[i] = planes[i] instanceof int[] integers ? integers.clone() : ((Object[]) planes[i]).clone();
        }
        long allocated = number(owner, "allocated");
        long freeHead = number(owner, "freeHead");
        long stateVersion = number(owner, "stateVersion");
        int[] lengths = Arrays.stream(planes).mapToInt(Array::getLength).toArray();
        require(Arrays.stream(lengths).allMatch(length -> length == capacity), "growth boundary fixture");
        long version = owner.version();
        Item added = new Item(3, capacity + 1);
        M3IndexedArrayGrowthFault.arm(failureAt);
        try {
            owner.append(added);
            throw new AssertionError("allocation fault was not reached");
        } catch (OutOfMemoryError failure) {
            require(failure == M3IndexedArrayGrowthFault.FAILURE, "unexpected allocator failure");
        } finally {
            M3IndexedArrayGrowthFault.disarm();
        }
        require(M3IndexedArrayGrowthFault.visits() == failureAt, "fault site was not exact");
        require(owner.size() == capacity && owner.version() == version, "failure changed size/version");
        require(number(owner, "allocated") == allocated, "failure changed allocated slot frontier");
        require(number(owner, "freeHead") == freeHead, "failure changed free-list head");
        require(number(owner, "stateVersion") == stateVersion, "failure changed state epoch");
        for (int i = 0; i < planes.length; i++) {
            if (planes[i] instanceof int[] integers) {
                require(Arrays.equals(integers, (int[]) contents[i]), "failure mutated " + PLANE_NAMES[i]);
            } else {
                Object[] references = (Object[]) planes[i];
                Object[] saved = (Object[]) contents[i];
                for (int row = 0; row < references.length; row++) {
                    require(references[row] == saved[row], "failure mutated payload reference plane");
                }
            }
        }
        for (int i = 0; i < capacity; i++) {
            require(owner.at(i) == expected.get(i), "failure changed logical reference");
            require(owner.handleAt(i) == handles.get(i), "failure changed handle/order");
            require(owner.get(handles.get(i)) == expected.get(i), "failure changed handle resolution");
        }
        require(owner.ownerCount(handles.get(0)) == 2 && owner.ownerCount(handles.get(1)) == 1,
                "failure changed lease counts");
        require(owner.stateMask(handles.get(0)) == (M3IndexedArray.LIVE | M3IndexedArray.LEASED
                | M3IndexedArray.SHARED), "failure changed shared state");
        expect(IllegalArgumentException.class, () -> owner.get(stale), "stale handle after failure");
        same(expectedFiltered, filtered, "failure filtered view");
        same(expectedSorted, sorted, "failure sorted view");
        same(expectedUnleased, unleased, "failure state view");
        same(expectedSorted.subList(1, expectedSorted.size() - 1), range, "bound range after failure");
        int visited = 0;
        while (bound.hasNext()) require(bound.next() == expectedSorted.get(visited++), "bound cursor after failure");
        require(visited == capacity, "failure truncated bound cursor");

        Object[] after = planes(owner);
        int published = 0;
        for (int i = 0; i < planes.length; i++) if (planes[i] != after[i]) published++;
        Throwable recovery = null;
        try {
            long next = owner.append(added);
            require(owner.size() == capacity + 1, "retry size");
            require(owner.at(capacity) == added && owner.get(next) == added, "retry reference");
            require(owner.version() == version + 1, "retry epoch increments exactly once");
            for (int i = 0; i < capacity; i++) {
                require(owner.at(i) == expected.get(i) && owner.handleAt(i) == handles.get(i),
                        "retry preserves old offsets/generations");
            }
            expect(ConcurrentModificationException.class, range::size, "retry invalidates old range");
            expected.add(added);
            expectedSorted.add(added);
            expectedSorted.sort(Comparator.comparingInt(Item::rank));
            same(expectedSorted, sorted, "retry refreshes sorted view");
            require(owner.remove(next), "retry handle removable");
            expect(IllegalArgumentException.class, () -> owner.get(next), "retry removed handle stale");
        } catch (Throwable problem) {
            recovery = problem;
        }
        System.out.println("GROWTH_CASE capacity=" + capacity + " allocation=" + failureAt
                + " planes_published_on_failure=" + published
                + " failure_lengths=" + Arrays.toString(Arrays.stream(after).mapToInt(Array::getLength).toArray())
                + " retry=" + (recovery == null ? "PASS" : recovery.toString()));
        if (published != 0 || recovery != null) failures++;
    }

    private static Object[] planes(Object owner) throws Exception {
        Object[] arrays = new Object[PLANE_NAMES.length];
        for (int i = 0; i < PLANE_NAMES.length; i++) {
            Field field = M3IndexedArray.class.getDeclaredField(PLANE_NAMES[i]);
            field.setAccessible(true);
            arrays[i] = field.get(owner);
        }
        return arrays;
    }

    private static long number(Object owner, String name) throws Exception {
        Field field = M3IndexedArray.class.getDeclaredField(name);
        field.setAccessible(true);
        return ((Number) field.get(owner)).longValue();
    }

    private static void same(List<Item> expected, List<Item> actual, String reason) {
        require(expected.size() == actual.size(), reason + " size");
        for (int i = 0; i < expected.size(); i++) require(expected.get(i) == actual.get(i), reason + " identity");
    }

    private static void expect(Class<? extends Throwable> type, Runnable action, String reason) {
        try {
            action.run();
        } catch (Throwable problem) {
            require(type.isInstance(problem), reason + " unexpected " + problem);
            return;
        }
        throw new AssertionError(reason + " did not fail");
    }

    private static void require(boolean good, String reason) {
        checks++;
        if (!good) throw new AssertionError(reason);
    }

    private static void buildAgent() throws Exception {
        Path classes = Path.of(System.getProperty("test.classes", "."));
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().putValue("Premain-Class", "M3IndexedArrayGrowthAgent");
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(
                Path.of("M3IndexedArrayGrowthAgent.jar")), manifest);
             Stream<Path> entries = Files.list(classes)) {
            for (Path entry : entries.filter(path -> path.getFileName().toString()
                    .startsWith("M3IndexedArrayGrowthAgent") && path.toString().endsWith(".class")).sorted().toList()) {
                jar.putNextEntry(new JarEntry(entry.getFileName().toString()));
                Files.copy(entry, jar);
                jar.closeEntry();
            }
        }
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(Path.of("M3IndexedArrayGrowthHook.jar")))) {
            jar.putNextEntry(new JarEntry("M3IndexedArrayGrowthFault.class"));
            try (InputStream bytes = Files.newInputStream(classes.resolve("M3IndexedArrayGrowthFault.class"))) {
                bytes.transferTo(jar);
            }
            jar.closeEntry();
        }
    }
}
