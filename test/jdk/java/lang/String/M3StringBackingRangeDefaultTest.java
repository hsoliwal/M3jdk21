/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
 */

/*
 * @test
 * @summary M3 String backing range defaults preserve UTF-16 and legacy operations
 * @modules java.base/jdk.internal.mindex
 * @run main M3StringBackingRangeDefaultTest
 */

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import jdk.internal.mindex.M3StringBacking;

public class M3StringBackingRangeDefaultTest {
    private static final String TEXT = "ab\uD83D\uDE00z";

    public static void main(String[] args) {
        Backing backing = new Backing();
        require(backing.contentEquals(1L, 1, 4, new StringBuilder("b\uD83D\uDE00")),
                "UTF-16 range equality");
        require(backing.contentEquals(1L, 2, 3, "\uD83D"),
                "surrogate-half range equality");
        require(backing.contentEquals(1L, 3, 3, ""), "empty range equality");
        require(!backing.contentEquals(1L, 1, 4, "bX"), "length mismatch");
        require(!backing.contentEquals(1L, 1, 4, null), "null comparison");
        require(!backing.contentEquals(1L, 1, 4, "a\uD83D\uDE00"),
                "equal length but different content");

        expectInvalid(backing, -1, 2);
        expectInvalid(backing, 4, 6);
        expectInvalid(backing, 3, 2);

        require(backing.contentEquals(1L, TEXT), "legacy whole-string equality");
        require(backing.compare(1L, 1L) == 0, "legacy compare");
        require(backing.materializations == 0, "range comparison must not materialize");
        backing.close();
    }

    private static void expectInvalid(Backing backing, int start, int end) {
        try {
            backing.contentEquals(1L, start, end, "x");
            throw new AssertionError("invalid range accepted: " + start + ".." + end);
        } catch (IndexOutOfBoundsException expected) {
            // Expected checked range behavior.
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class Backing implements M3StringBacking {
        private int materializations;

        @Override public void close() { }
        @Override public int length(long id) { return TEXT.length(); }
        @Override public int utf8Length(long id) {
            return TEXT.getBytes(StandardCharsets.UTF_8).length;
        }
        @Override public int codePointCount(long id) {
            return TEXT.codePointCount(0, TEXT.length());
        }
        @Override public int unpairedSurrogateCount(long id) { return 0; }
        @Override public int hashCode(long id) { return TEXT.hashCode(); }
        @Override public char charAt(long id, int index) { return TEXT.charAt(index); }
        @Override public CharBuffer utf16View(long id) {
            return CharBuffer.wrap(TEXT).asReadOnlyBuffer();
        }
        @Override public ByteBuffer utf8View(long id) {
            return ByteBuffer.wrap(TEXT.getBytes(StandardCharsets.UTF_8)).asReadOnlyBuffer();
        }
        @Override public String materialize(long id) {
            materializations++;
            return TEXT;
        }
    }
}
