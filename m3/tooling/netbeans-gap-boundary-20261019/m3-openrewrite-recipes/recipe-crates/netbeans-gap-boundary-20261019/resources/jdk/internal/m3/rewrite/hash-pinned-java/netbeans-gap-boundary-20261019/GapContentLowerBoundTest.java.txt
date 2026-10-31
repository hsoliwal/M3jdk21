/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 */

/*
 * @test
 * @summary Preserve GapContent mark boundaries, position identity and undo after duplicate collapse
 * @modules java.desktop/javax.swing.text
 * @compile --patch-module java.desktop=${test.src} GapContentLowerBoundTest.java
 * @run main/othervm -Djava.awt.headless=true --patch-module java.desktop=${test.classes} javax.swing.text.GapContentLowerBoundTest
 */
package javax.swing.text;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.undo.UndoableEdit;

public final class GapContentLowerBoundTest {
    private static final Field MARKS = field("marks");
    private static final Field SEARCH = field("search");
    private static int checks;
    private static long trace = 17;

    private GapContentLowerBoundTest() { }

    private static Field field(String name) {
        try {
            Field result = GapContent.class.getDeclaredField(name);
            result.setAccessible(true);
            return result;
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }

    private static void equal(long actual, long expected) {
        if (actual != expected) throw new AssertionError(actual + " != " + expected);
        trace = trace * 31 + actual;
        checks++;
    }

    private static void require(boolean condition) {
        if (!condition) throw new AssertionError("condition failed");
        checks++;
    }

    public static GapContent fixture(int[] offsets) throws Exception {
        GapContent content = new GapContent();
        GapContent.MarkVector marks = new GapContent.MarkVector(offsets.length + 2);
        for (int offset : offsets) marks.addElement(content.new MarkData(offset));
        MARKS.set(content, marks);
        return content;
    }

    public static int boundary(GapContent content, int key) {
        return content.findMarkAdjustIndex(key);
    }

    private static void boundaries(int[] offsets) throws Exception {
        GapContent content = fixture(offsets);
        GapContent.MarkVector original = (GapContent.MarkVector) MARKS.get(content);
        Object backing = original.getArray();
        GapContent.MarkData search = (GapContent.MarkData) SEARCH.get(content);
        int[] keys = {Integer.MIN_VALUE, -1, 0, 1, 2, 3, 4, 5, 16,
                Integer.MAX_VALUE - 1, Integer.MAX_VALUE};
        for (int key : keys) {
            int expected = 0;
            while (expected < offsets.length && offsets[expected] < Math.max(1, key)) expected++;
            equal(boundary(content, key), expected);
            equal(search.index, Math.max(1, key));
            require(MARKS.get(content) == original);
            require(original.getArray() == backing);
        }
        for (int i = 0; i < offsets.length; i++) equal(original.elementAt(i).index, offsets[i]);
    }

    private static void enumerate(int[] offsets, int index, int minimum) throws Exception {
        int[] values = {-1, 0, 1, 2, 4, Integer.MAX_VALUE};
        if (index == offsets.length) {
            boundaries(offsets);
            return;
        }
        for (int i = minimum; i < values.length; i++) {
            offsets[index] = values[i];
            enumerate(offsets, index + 1, i);
        }
    }

    private static void text(GapContent content, String expected) throws Exception {
        require(content.getString(0, content.length()).equals(expected));
        for (boolean partial : new boolean[] {false, true}) {
            Segment segment = new Segment();
            segment.setPartialReturn(partial);
            StringBuilder result = new StringBuilder();
            int cursor = 0;
            while (cursor < content.length()) {
                content.getChars(cursor, content.length() - cursor, segment);
                require(segment.count > 0);
                result.append(segment.array, segment.offset, segment.count);
                cursor += segment.count;
            }
            require(result.toString().equals(expected));
        }
        for (int i = 0; i < expected.length(); i++) equal(expected.charAt(i), content.getString(i, 1).charAt(0));
    }

    private static int[] offsets(List<Position> positions) {
        return positions.stream().mapToInt(Position::getOffset).toArray();
    }

    private static void positions(List<Position> actual, int[] expected) {
        equal(actual.size(), expected.length);
        for (int i = 0; i < expected.length; i++) equal(actual.get(i).getOffset(), expected[i]);
    }

    private static void editAndUndo() throws Exception {
        String seed = "a\u0000\uD83D\uDE00b\uD800c\uDC00defghijklmnopqrstuvwxyz";
        for (int capacity : new int[] {2, 3, 10, 64}) {
            for (int start = 0; start < seed.length(); start++) {
                GapContent content = new GapContent(capacity);
                content.insertString(0, seed);
                List<Position> marks = new ArrayList<>();
                for (int i = 0; i <= seed.length(); i++) {
                    Position p = content.createPosition(i);
                    require(p == content.createPosition(i));
                    marks.add(p);
                }
                int[] before = offsets(marks);
                int length = seed.length() - start;
                UndoableEdit removal = content.remove(start, length);
                int[] collapsed = before.clone();
                for (int i = 0; i < collapsed.length; i++) {
                    if (collapsed[i] >= start) collapsed[i] = Math.max(start, collapsed[i] - length);
                }
                positions(marks, collapsed);
                text(content, seed.substring(0, start) + "\n");
                for (int cycle = 0; cycle < 3; cycle++) {
                    removal.undo();
                    positions(marks, before);
                    text(content, seed + "\n");
                    removal.redo();
                    positions(marks, collapsed);
                    text(content, seed.substring(0, start) + "\n");
                }
                UndoableEdit insertion = content.insertString(start, "XY");
                int[] inserted = collapsed.clone();
                for (int i = 0; i < inserted.length; i++) {
                    if (inserted[i] != 0 && inserted[i] >= start) inserted[i] += 2;
                }
                positions(marks, inserted);
                insertion.undo();
                positions(marks, collapsed);
                insertion.redo();
                positions(marks, inserted);
                require(content.createPosition(start) == content.createPosition(start));
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                try (ObjectOutputStream out = new ObjectOutputStream(bytes)) { out.writeObject(content); }
                for (byte b : bytes.toByteArray()) trace = trace * 31 + b;
                GapContent restored;
                try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
                    restored = (GapContent) in.readObject();
                }
                text(restored, content.getString(0, content.length()));
                equal(restored.createPosition(0).getOffset(), 0);
            }
        }
    }

    private static void failures() throws Exception {
        GapContent content = new GapContent();
        for (int location : new int[] {-1, 2, Integer.MAX_VALUE}) {
            try {
                content.insertString(location, "x");
                throw new AssertionError("accepted invalid insert");
            } catch (BadLocationException expected) {
                equal(expected.offsetRequested(), 1);
                require(expected.getMessage().equals("Invalid insert"));
            }
        }
        try {
            content.insertString(0, null);
            throw new AssertionError("accepted null");
        } catch (NullPointerException expected) { checks++; }
        try {
            content.remove(0, 1);
            throw new AssertionError("removed sentinel");
        } catch (BadLocationException expected) { equal(expected.offsetRequested(), 2); }
        text(content, "\n");
    }

    private static void documentTrace() throws Exception {
        PlainDocument document = new PlainDocument(new GapContent(2));
        javax.swing.undo.UndoManager undo = new javax.swing.undo.UndoManager();
        document.addUndoableEditListener(undo);
        StringBuilder events = new StringBuilder();
        document.addDocumentListener(new javax.swing.event.DocumentListener() {
            private void record(char kind, javax.swing.event.DocumentEvent event) {
                events.append(kind).append(event.getOffset()).append(':').append(event.getLength()).append(';');
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { record('I', e); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { record('R', e); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { record('C', e); }
        });
        document.insertString(0, "abcdef", null);
        List<Position> marks = new ArrayList<>();
        for (int i = 0; i <= 6; i++) marks.add(document.createPosition(i));
        document.remove(1, 4);
        positions(marks, new int[] {0, 1, 1, 1, 1, 1, 2});
        undo.undo();
        positions(marks, new int[] {0, 1, 2, 3, 4, 5, 6});
        undo.redo();
        positions(marks, new int[] {0, 1, 1, 1, 1, 1, 2});
        require(events.toString().equals("I0:6;R1:4;I1:4;R1:4;"));
    }

    public static void main(String[] arguments) throws Exception {
        for (int length = 0; length <= 8; length++) enumerate(new int[length], 0, 0);
        for (int size : new int[] {1, 2, 3, 63, 64, 65, 1024, 65536, 131072}) {
            int[] values = new int[size];
            Arrays.fill(values, 4);
            boundaries(values);
            for (int i = 0; i < size; i++) values[i] = i / 64;
            boundaries(values);
        }
        editAndUndo();
        failures();
        documentTrace();
        System.out.println("GapContent checks=" + checks + " trace=" + Long.toHexString(trace));
    }
}
