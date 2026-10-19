/*
 * Copyright (c) 2026, Hitesh Soliwal (Synexia) and contributors. All rights reserved.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */
package m3.swing.api;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.stream.*;

import javax.swing.M3Patch;
import org.junit.jupiter.api.Test;

/**
 * Contract of javax.swing.M3Patch (Swing, leaf S8, the one-model seam). Headless. Oracles: an ArrayList of ids edited
 * through the Target callbacks, and a quadratic LIS for the minimal size of a diff.
 */
public class M3PatchTest {

	private static long [] edit(Random random, long [] ids, long next) {
		ArrayList<Long> list = LongStream.of(ids).boxed().collect(Collectors.toCollection(ArrayList::new));
		for(int k = random.nextInt(6); k > 0 && !list.isEmpty(); k--) list.remove(random.nextInt(list.size()));
		for(int k = random.nextInt(6); k > 0; k--) list.add(random.nextInt(list.size() + 1), next++);
		for(int k = random.nextInt(4); k > 0 && list.size() > 1; k--) list.add(random.nextInt(list.size()), list.remove(random.nextInt(list.size())));
		return list.stream().mapToLong(Long::longValue).toArray();
	}

	private static int lis(long [] oldIds, long [] newIds) {
		Map<Long, Integer> position = new HashMap<>();
		for(int i = 0; i < oldIds.length; i++) position.put(oldIds [i], i);
		int [] sequence = LongStream.of(newIds).filter(position::containsKey).mapToInt(id -> position.get(id)).toArray();
		int [] best = new int [sequence.length];
		int longest = 0;
		for(int i = 0; i < sequence.length; i++) {
			best [i] = 1;
			for(int j = 0; j < i; j++) if(sequence [j] < sequence [i]) best [i] = Math.max(best [i], best [j] + 1);
			longest = Math.max(longest, best [i]);
		}
		return longest;
	}

	/* replays a patch through the Target callbacks onto a list oracle */
	private static long [] replay(M3Patch patch, long [] before) {
		ArrayList<Long> list = LongStream.of(before).boxed().collect(Collectors.toCollection(ArrayList::new));
		int [] run = new int [1];
		patch.applyTo(new M3Patch.Target() {
			@Override
			public void remove(int index, int count) {
				list.subList(index, index + count).clear();
			}

			@Override
			public void insert(int index, int count) {
				for(int k = 0; k < count; k++) list.add(index + k, patch.insertedId(run [0], k));
				run [0]++;
			}

			@Override
			public void update(int index, int count) {
				assertTrue(index + count <= list.size());
			}
		});
		return list.stream().mapToLong(Long::longValue).toArray();
	}

	@Test
	public void diffIsMinimalAndReplays() {
		Random random = new Random(19);
		long [] ids = LongStream.range(0, 40).toArray();
		long next = 1000;
		for(int round = 0; round < 300; round++) {
			long [] wanted = edit(random, ids, next);
			next += 10;
			M3Patch patch = M3Patch.diff(M3Patch.ROOT, ids, wanted);
			assertArrayEquals(wanted, patch.apply(ids), "round " + round);
			assertArrayEquals(wanted, replay(patch, ids), "round " + round);
			int kept = lis(ids, wanted);
			assertEquals(ids.length - kept, patch.removedCount());
			assertEquals(wanted.length - kept, patch.insertedCount());
			assertEquals(0, patch.updatedCount());
			for(int i = 0; i < patch.moveCount(); i++) assertEquals(ids [patch.moveFrom(i)], wanted [patch.moveTo(i)], "a move carries one id");
			assertEquals(patch.isEmpty(), Arrays.equals(ids, wanted));
			ids = wanted;
		}
	}

	@Test
	public void builderRunsAndUpdates() {
		M3Patch patch = M3Patch.builder(7).remove(8, 2).remove(1, 1).insert(0, 100, 101).insert(5, 102).update(3, 2).update(9, 1).build();
		assertEquals(7, patch.parent());
		assertEquals(2, patch.removeRuns());
		assertEquals(8, patch.removeAt(0));
		assertEquals(2, patch.removeCount(0));
		assertEquals(2, patch.insertRuns());
		assertEquals(5, patch.insertAt(1));
		assertEquals(1, patch.insertCount(1));
		assertEquals(101, patch.insertedId(0, 1));
		assertEquals(102, patch.insertedId(1, 0));
		assertEquals(2, patch.updateRuns());
		assertEquals(9, patch.updateAt(1));
		assertEquals(1, patch.updateCount(1));
		assertEquals(3, patch.removedCount());
		assertEquals(3, patch.insertedCount());
		assertEquals(3, patch.updatedCount());
		long [] before = LongStream.range(0, 10).toArray();
		long [] after = patch.apply(before);
		assertArrayEquals(new long [] {100, 101, 0, 2, 3, 102, 4, 5, 6, 7}, after);
		assertArrayEquals(after, replay(patch, before));
		assertEquals("M3Patch [parent 7, -3 +3 ~3, 0 moves]", patch.toString());
		M3Patch moved = M3Patch.builder(M3Patch.ROOT).remove(0, 1).insert(2, 0).move(0, 2).build();
		assertEquals(1, moved.moveCount());
		assertArrayEquals(new long [] {1, 2, 0}, moved.apply(new long [] {0, 1, 2}));
		M3Patch empty = M3Patch.builder(M3Patch.ROOT).build();
		assertTrue(empty.isEmpty());
		long [] same = {4, 5};
		assertArrayEquals(same, empty.apply(same));
		assertNotSame(same, empty.apply(same));
		assertFalse(M3Patch.builder(M3Patch.ROOT).update(0, 1).build().isEmpty());
	}

	@Test
	public void argumentsAreChecked() {
		M3Patch.Builder builder = M3Patch.builder(M3Patch.ROOT).remove(5, 2);
		assertThrows(IllegalArgumentException.class,() -> builder.remove(6, 1), "removes go down");
		assertThrows(IllegalArgumentException.class,() -> builder.remove(-1, 1));
		assertThrows(IllegalArgumentException.class,() -> builder.remove(0, 0));
		builder.insert(3, 9);
		assertThrows(IllegalArgumentException.class,() -> builder.insert(3, 10), "inserts go up");
		assertThrows(IllegalArgumentException.class,() -> builder.insert(4));
		assertThrows(IllegalArgumentException.class,() -> builder.insert(-1, 1));
		assertThrows(NullPointerException.class,() -> builder.insert(8,(long []) null));
		builder.update(2, 2);
		assertThrows(IllegalArgumentException.class,() -> builder.update(3, 1), "updates go up");
		assertThrows(IllegalArgumentException.class,() -> builder.update(-1, 1));
		assertThrows(IllegalArgumentException.class,() -> builder.update(9, 0));
		assertThrows(IllegalArgumentException.class,() -> builder.move(0, 3), "0 is not removed");
		assertThrows(IllegalArgumentException.class,() -> builder.move(5, 4), "4 is not inserted");
		M3Patch patch = builder.move(6, 3).build();
		assertThrows(IndexOutOfBoundsException.class,() -> patch.insertedId(0, 1));
		assertThrows(IndexOutOfBoundsException.class,() -> patch.apply(new long [3]), "remove beyond the list");
		assertThrows(IndexOutOfBoundsException.class,() -> M3Patch.builder(M3Patch.ROOT).insert(2, 1).build().apply(new long [1]));
		assertThrows(IndexOutOfBoundsException.class,() -> M3Patch.builder(M3Patch.ROOT).update(0, 2).build().apply(new long [1]));
		assertThrows(NullPointerException.class,() -> patch.apply(null));
		assertThrows(NullPointerException.class,() -> patch.applyTo(null));
		assertThrows(NullPointerException.class,() -> M3Patch.diff(M3Patch.ROOT, null, new long [0]));
		assertThrows(NullPointerException.class,() -> M3Patch.diff(M3Patch.ROOT, new long [0], null));
		assertThrows(IllegalArgumentException.class,() -> M3Patch.diff(M3Patch.ROOT, new long [] {1, 1}, new long [0]));
		assertThrows(IllegalArgumentException.class,() -> M3Patch.diff(M3Patch.ROOT, new long [0], new long [] {2, 2}));
	}
}
