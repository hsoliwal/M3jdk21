/*******************************************************************************
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
package org.eclipse.nebula.widgets.grid;

import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Native Grid and stock ArrayList oracles for duplicate selection occurrences and read compaction. */
public class GridSelectionOccurrence_Test {
	private Display display;
	private Shell shell;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
	}

	@After
	public void tearDown() {
		shell.dispose();
		while (display.readAndDispatch()) { }
	}

	private GridItem[] items(final int count) {
		final Grid grid = new Grid(shell, SWT.MULTI);
		final GridItem[] items = new GridItem[count];
		for (int i = 0; i < count; i++) {
			items[i] = new GridItem(grid, SWT.NONE);
		}
		return items;
	}

	@Test
	public void publicDuplicateDeselectKeepsRemainingItemSelectedAndInStockOrder() {
		final GridItem[] rows = items(3);
		final Grid grid = rows[0].getParent();
		grid.setSelection(new GridItem[] { rows[0], rows[1], rows[0], rows[2], rows[0] });
		grid.deselect(0);
		assertTrue("another occurrence still selects this row for painting", grid.isSelected(0));
		assertArrayEquals(new GridItem[] { rows[1], rows[0], rows[2], rows[0] }, grid.getSelection());
		grid.deselect(0);
		assertTrue(grid.isSelected(0));
		assertArrayEquals(new GridItem[] { rows[1], rows[2], rows[0] }, grid.getSelection());
		grid.deselect(0);
		assertFalse(grid.isSelected(0));
		assertArrayEquals(new GridItem[] { rows[1], rows[2] }, grid.getSelection());
	}

	@Test
	public void duplicateFirstLastAndIndexedRemovalAnswerAsAnArrayList() {
		final GridItem[] rows = items(3);
		final GridSelectionList actual = new GridSelectionList();
		final List<GridItem> expected = new ArrayList<>(Arrays.asList(rows[0], rows[1], rows[0], rows[2], rows[0]));
		actual.addAll(expected);
		assertEquals(0, actual.indexOf(rows[0]));
		assertEquals(4, actual.lastIndexOf(rows[0]));
		assertSame(expected.remove(4), actual.remove(4));
		assertArrayEquals(expected.toArray(), actual.toArray());
		assertEquals(expected.remove(rows[0]), actual.remove(rows[0]));
		assertTrue(actual.contains(rows[0]));
		assertArrayEquals(expected.toArray(), actual.toArray());
		assertEquals(expected.indexOf(rows[0]), actual.indexOf(rows[0]));
		assertEquals(expected.lastIndexOf(rows[0]), actual.lastIndexOf(rows[0]));
	}

	@Test
	public void iteratorUsesLogicalRowsWhenReadersCompactItsHoles() {
		final GridItem[] rows = items(6);
		final GridSelectionList actual = new GridSelectionList();
		actual.addAll(Arrays.asList(rows));
		actual.remove(rows[1]);
		actual.remove(rows[3]);
		final Iterator<GridItem> iterator = actual.iterator();
		assertSame(rows[0], iterator.next());
		actual.toArray();
		assertSame(rows[2], iterator.next());
		actual.indexOf(rows[5]);
		assertSame(rows[4], iterator.next());
		assertSame(rows[5], iterator.next());
		assertFalse(iterator.hasNext());
	}

	@Test
	public void iteratorRemovalAndBulkPredicatesKeepOccurrenceMembership() {
		final GridItem[] rows = items(3);
		final GridSelectionList actual = new GridSelectionList();
		actual.addAll(Arrays.asList(rows[0], rows[1], rows[0], rows[2], rows[0]));
		final Iterator<GridItem> iterator = actual.iterator();
		assertSame(rows[0], iterator.next());
		iterator.remove();
		assertTrue(actual.contains(rows[0]));
		assertSame(rows[1], iterator.next());
		actual.removeIf(item -> item == rows[0]);
		assertFalse(actual.contains(rows[0]));
		assertArrayEquals(new GridItem[] { rows[1], rows[2] }, actual.toArray());
		assertTrue(actual.retainAll(Arrays.asList(rows[2])));
		assertArrayEquals(new GridItem[] { rows[2] }, actual.toArray());
	}

	@Test
	public void nonStructuralSetDoesNotInvalidateAnIterator() {
		final GridItem[] rows = items(3);
		final GridSelectionList actual = new GridSelectionList();
		actual.addAll(Arrays.asList(rows[0], rows[1], rows[0]));
		final Iterator<GridItem> iterator = actual.iterator();
		assertSame(rows[0], iterator.next());
		actual.set(1, rows[2]);
		assertSame(rows[2], iterator.next());
		assertSame(rows[0], iterator.next());
		actual.add(rows[1]);
		assertThrows(ConcurrentModificationException.class, iterator::next);
	}

	@Test
	public void repeatedSelfBulkAppendRemainsFiniteAndPreservesDuplicates() {
		final GridItem[] rows = items(3);
		final GridSelectionList actual = new GridSelectionList();
		final List<GridItem> expected = new ArrayList<>(Arrays.asList(rows));
		actual.addAll(expected);
		for (int round = 0; round < 4; round++) {
			assertTrue(actual.addAll(actual));
			expected.addAll(new ArrayList<>(expected));
			assertArrayEquals(expected.toArray(), actual.toArray());
			assertEquals(expected.lastIndexOf(rows[1]), actual.lastIndexOf(rows[1]));
		}
	}

	@Test
	public void duplicateMutationsAndCompactionMatchSeededArrayListOracle() {
		final GridItem[] rows = items(64);
		final GridSelectionList actual = new GridSelectionList();
		final List<GridItem> expected = new ArrayList<>();
		final Random random = new Random(0x0CC077E1L);
		for (int step = 0; step < 20_000; step++) {
			final GridItem item = rows[random.nextInt(rows.length)];
			final int operation = expected.size() > 320 ? 4 : random.nextInt(10);
			switch (operation) {
			case 0:
			case 1:
				actual.add(item);
				expected.add(item);
				break;
			case 2:
				final int insertion = random.nextInt(expected.size() + 1);
				actual.add(insertion, item);
				expected.add(insertion, item);
				break;
			case 3:
				if (!expected.isEmpty()) {
					final int at = random.nextInt(expected.size());
					assertSame(expected.set(at, item), actual.set(at, item));
				}
				break;
			case 4:
			case 5:
				assertEquals(expected.remove(item), actual.remove(item));
				break;
			case 6:
				if (!expected.isEmpty()) {
					final int at = random.nextInt(expected.size());
					assertSame(expected.remove(at), actual.remove(at));
				}
				break;
			case 7:
				assertEquals(expected.removeIf(row -> row == item), actual.removeIf(row -> row == item));
				break;
			case 8:
				final List<GridItem> some = Arrays.asList(item, rows[random.nextInt(rows.length)], item);
				actual.addAll(some);
				expected.addAll(some);
				break;
			default:
				if (random.nextInt(50) == 0) {
					actual.clear();
					expected.clear();
				}
			}
			assertEquals("size at step " + step, expected.size(), actual.size());
			assertEquals("membership at step " + step, expected.contains(item), actual.contains(item));
			assertEquals("first at step " + step, expected.indexOf(item), actual.indexOf(item));
			assertEquals("last at step " + step, expected.lastIndexOf(item), actual.lastIndexOf(item));
			if (step % 37 == 0) {
				assertArrayEquals(expected.toArray(), actual.toArray());
				assertEquals(expected, new ArrayList<>(actual));
			}
		}
	}

	@Test
	public void duplicateDeselectRendersSameNativePixelsAsRemainingSelection() throws Exception {
		shell.setLayout(new FillLayout());
		final Grid grid = new Grid(shell, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		grid.setHeaderVisible(true);
		final GridColumn column = new GridColumn(grid, SWT.NONE);
		column.setText("Viewport selection occurrences");
		column.setWidth(330);
		for (int i = 0; i < 30; i++) {
			new GridItem(grid, SWT.NONE).setText("Row " + i + " - stable item identity");
		}
		final GridItem first = grid.getItem(0);
		grid.setSelection(new GridItem[] { first, first });
		grid.deselect(0);
		shell.setBounds(40, 40, 410, 310);
		shell.open();
		grid.setFocus();
		settle(grid);
		final String configured = System.getProperty("nebula.selection.evidence.dir");
		final Path evidence = configured == null ? Files.createTempDirectory("nebula-selection-occurrence-") : Path.of(configured);
		Files.createDirectories(evidence);
		final Path actualPath = evidence.resolve("selection-after-duplicate-deselect.png");
		final Path expectedPath = evidence.resolve("selection-reference-selected.png");
		assertEquals(GridSwtScreenshotCapture.Method.DISPLAY_COPY_AREA,
				GridSwtScreenshotCapture.captureScreenControl(grid, actualPath).method());
		grid.setSelection(new GridItem[] { first });
		settle(grid);
		assertEquals(GridSwtScreenshotCapture.Method.DISPLAY_COPY_AREA,
				GridSwtScreenshotCapture.captureScreenControl(grid, expectedPath).method());
		final ImageData actual = new ImageLoader().load(actualPath.toString())[0];
		final ImageData expected = new ImageLoader().load(expectedPath.toString())[0];
		assertEquals(expected.width, actual.width);
		assertEquals(expected.height, actual.height);
		assertTrue("capture must include the actual native widget", actual.width > 300 && actual.height > 200);
		final HashSet<Integer> colors = new HashSet<>();
		long differences = 0;
		for (int y = 0; y < expected.height; y++) {
			for (int x = 0; x < expected.width; x++) {
				final int pixel = actual.getPixel(x, y);
				colors.add(pixel);
				if (pixel != expected.getPixel(x, y)) {
					differences++;
				}
			}
		}
		assertTrue("visible capture must not be a blank or flat frame", colors.size() > 4);
		assertEquals("native selected-row pixel mismatch", 0, differences);
	}

	private void settle(final Grid grid) throws InterruptedException {
		grid.redraw();
		for (int round = 0; round < 8; round++) {
			while (display.readAndDispatch()) { }
			grid.update();
			display.update();
			Thread.sleep(10);
		}
	}
}
