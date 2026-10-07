// SPDX-License-Identifier: Apache-2.0
package com.m3.arrays;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Factory and composition operations for immutable M3 array views. */
public final class M3Arrays {
    private M3Arrays() {}

    public static M3ByteArrayView bytes(byte[] source) {
        return new ByteSnapshot(Objects.requireNonNull(source, "source").clone());
    }

    public static M3Utf16ArrayView chars(char[] source) {
        return new CharSnapshot(Objects.requireNonNull(source, "source").clone());
    }

    public static M3Utf16ArrayView chars(CharSequence source) {
        Objects.requireNonNull(source, "source");
        char[] snapshot = new char[source.length()];
        for (int index = 0; index < snapshot.length; index++) snapshot[index] = source.charAt(index);
        return new CharSnapshot(snapshot);
    }

    public static M3IntArrayView ints(int[] source) {
        return new IntSnapshot(Objects.requireNonNull(source, "source").clone());
    }

    public static M3LongArrayView longs(long[] source) {
        return new LongSnapshot(Objects.requireNonNull(source, "source").clone());
    }

    public static M3ByteArrayView join(M3ByteArrayView... values) {
        M3ByteArrayView[] segments = checked(values, M3ByteArrayView[]::new);
        if (segments.length == 0) return bytes(new byte[0]);
        if (segments.length == 1) return segments[0];
        return new ByteJoin(segments);
    }

    public static M3Utf16ArrayView join(M3Utf16ArrayView... values) {
        M3Utf16ArrayView[] segments = checked(values, M3Utf16ArrayView[]::new);
        if (segments.length == 0) return chars(new char[0]);
        if (segments.length == 1) return segments[0];
        return new CharJoin(segments);
    }

    public static M3IntArrayView join(M3IntArrayView... values) {
        M3IntArrayView[] segments = checked(values, M3IntArrayView[]::new);
        if (segments.length == 0) return ints(new int[0]);
        if (segments.length == 1) return segments[0];
        return new IntJoin(segments);
    }

    public static M3LongArrayView join(M3LongArrayView... values) {
        M3LongArrayView[] segments = checked(values, M3LongArrayView[]::new);
        if (segments.length == 0) return longs(new long[0]);
        if (segments.length == 1) return segments[0];
        return new LongJoin(segments);
    }

    private static <T> T[] checked(T[] values, java.util.function.IntFunction<T[]> arrayFactory) {
        Objects.requireNonNull(values, "values");
        ArrayList<T> flattened = new ArrayList<>(values.length);
        for (T value : values) flattened.add(Objects.requireNonNull(value, "segment"));
        return flattened.toArray(arrayFactory);
    }

    private abstract static class SliceBase {
        final int start;
        final int length;

        SliceBase(int sourceLength, int start, int end) {
            Objects.checkFromToIndex(start, end, sourceLength);
            this.start = start;
            this.length = end - start;
        }
    }

    private static final class ByteSnapshot implements M3ByteArrayView {
        private final byte[] data;

        ByteSnapshot(byte[] data) {
            this.data = data;
        }

        public int length() { return data.length; }
        public byte byteAt(int index) { return data[Objects.checkIndex(index, data.length)]; }
        public int segmentCount() { return data.length == 0 ? 0 : 1; }
        public M3ByteArrayView slice(int start, int end) {
            Objects.checkFromToIndex(start, end, data.length);
            return start == 0 && end == data.length ? this : new ByteSlice(this, start, end);
        }
        public ByteBuffer[] asReadOnlyBuffers() {
            return data.length == 0 ? new ByteBuffer[0] : new ByteBuffer[] {ByteBuffer.wrap(data).asReadOnlyBuffer()};
        }
    }

    private static final class ByteSlice extends SliceBase implements M3ByteArrayView {
        private final M3ByteArrayView source;
        ByteSlice(M3ByteArrayView source, int start, int end) {
            super(source.length(), start, end);
            this.source = source;
        }
        public int length() { return length; }
        public byte byteAt(int index) { return source.byteAt(start + Objects.checkIndex(index, length)); }
        public int segmentCount() { return buffers(source, start, length).size(); }
        public M3ByteArrayView slice(int from, int to) {
            Objects.checkFromToIndex(from, to, length);
            return from == 0 && to == length ? this : new ByteSlice(source, start + from, start + to);
        }
        public ByteBuffer[] asReadOnlyBuffers() {
            return buffers(source, start, length).toArray(ByteBuffer[]::new);
        }
    }

    private static final class ByteJoin implements M3ByteArrayView {
        private final M3ByteArrayView[] segments;
        private final int[] ends;
        private final int length;

        ByteJoin(M3ByteArrayView[] segments) {
            this.segments = segments.clone();
            ends = new int[segments.length];
            int total = 0;
            for (int index = 0; index < segments.length; index++) {
                total = Math.addExact(total, segments[index].length());
                ends[index] = total;
            }
            length = total;
        }

        public int length() { return length; }
        public int segmentCount() {
            int result = 0;
            for (M3ByteArrayView segment : segments) result = Math.addExact(result, segment.segmentCount());
            return result;
        }
        public byte byteAt(int index) {
            int checked = Objects.checkIndex(index, length);
            int segment = locate(ends, checked);
            int previous = segment == 0 ? 0 : ends[segment - 1];
            return segments[segment].byteAt(checked - previous);
        }
        public M3ByteArrayView slice(int start, int end) {
            Objects.checkFromToIndex(start, end, length);
            return start == 0 && end == length ? this : new ByteSlice(this, start, end);
        }
        public ByteBuffer[] asReadOnlyBuffers() {
            ArrayList<ByteBuffer> result = new ArrayList<>();
            for (M3ByteArrayView segment : segments) {
                for (ByteBuffer buffer : segment.asReadOnlyBuffers()) {
                    if (buffer.hasRemaining()) result.add(buffer.asReadOnlyBuffer());
                }
            }
            return result.toArray(ByteBuffer[]::new);
        }
    }

    private static final class CharSnapshot implements M3Utf16ArrayView {
        private final char[] data;
        private final M3Utf16Facts facts;

        CharSnapshot(char[] data) {
            this.data = data;
            this.facts = M3Utf16Facts.of(CharBuffer.wrap(data));
        }

        public int length() { return data.length; }
        public char charAt(int index) { return data[Objects.checkIndex(index, data.length)]; }
        public int segmentCount() { return data.length == 0 ? 0 : 1; }
        public M3Utf16Facts facts() { return facts; }
        public M3Utf16ArrayView subSequence(int start, int end) {
            Objects.checkFromToIndex(start, end, data.length);
            return start == 0 && end == data.length ? this : new CharSlice(this, start, end);
        }
        public CharBuffer[] asReadOnlyCharBuffers() {
            return data.length == 0 ? new CharBuffer[0] : new CharBuffer[] {CharBuffer.wrap(data).asReadOnlyBuffer()};
        }
        public String toString() { return new String(data); }
    }

    private static final class CharSlice extends SliceBase implements M3Utf16ArrayView {
        private final M3Utf16ArrayView source;
        private final M3Utf16Facts facts;

        CharSlice(M3Utf16ArrayView source, int start, int end) {
            super(source.length(), start, end);
            this.source = source;
            this.facts = M3Utf16Facts.of(this);
        }
        public int length() { return length; }
        public char charAt(int index) { return source.charAt(start + Objects.checkIndex(index, length)); }
        public int segmentCount() { return charBuffers(source, start, length).size(); }
        public M3Utf16Facts facts() { return facts; }
        public M3Utf16ArrayView subSequence(int from, int to) {
            Objects.checkFromToIndex(from, to, length);
            return from == 0 && to == length ? this : new CharSlice(source, start + from, start + to);
        }
        public CharBuffer[] asReadOnlyCharBuffers() {
            return charBuffers(source, start, length).toArray(CharBuffer[]::new);
        }
        public String toString() { return new String(copy()); }
    }

    private static final class CharJoin implements M3Utf16ArrayView {
        private final M3Utf16ArrayView[] segments;
        private final int[] ends;
        private final int length;
        private final M3Utf16Facts facts;

        CharJoin(M3Utf16ArrayView[] segments) {
            this.segments = segments.clone();
            ends = new int[segments.length];
            int total = 0;
            M3Utf16Facts joined = M3Utf16Facts.of("");
            for (int index = 0; index < segments.length; index++) {
                total = Math.addExact(total, segments[index].length());
                ends[index] = total;
                joined = M3Utf16Facts.combine(joined, segments[index].facts());
            }
            length = total;
            facts = joined;
        }

        public int length() { return length; }
        public int segmentCount() {
            int result = 0;
            for (M3Utf16ArrayView segment : segments) result = Math.addExact(result, segment.segmentCount());
            return result;
        }
        public M3Utf16Facts facts() { return facts; }
        public char charAt(int index) {
            int checked = Objects.checkIndex(index, length);
            int segment = locate(ends, checked);
            int previous = segment == 0 ? 0 : ends[segment - 1];
            return segments[segment].charAt(checked - previous);
        }
        public M3Utf16ArrayView subSequence(int start, int end) {
            Objects.checkFromToIndex(start, end, length);
            return start == 0 && end == length ? this : new CharSlice(this, start, end);
        }
        public CharBuffer[] asReadOnlyCharBuffers() {
            ArrayList<CharBuffer> result = new ArrayList<>();
            for (M3Utf16ArrayView segment : segments) {
                for (CharBuffer buffer : segment.asReadOnlyCharBuffers()) {
                    if (buffer.hasRemaining()) result.add(buffer.asReadOnlyBuffer());
                }
            }
            return result.toArray(CharBuffer[]::new);
        }
        public String toString() { return new String(copy()); }
    }

    private static final class IntSnapshot implements M3IntArrayView {
        private final int[] data;
        IntSnapshot(int[] data) { this.data = data; }
        public int length() { return data.length; }
        public int intAt(int index) { return data[Objects.checkIndex(index, data.length)]; }
        public int segmentCount() { return data.length == 0 ? 0 : 1; }
        public M3IntArrayView slice(int start, int end) {
            Objects.checkFromToIndex(start, end, data.length);
            return start == 0 && end == data.length ? this : new IntSlice(this, start, end);
        }
    }

    private static final class IntSlice extends SliceBase implements M3IntArrayView {
        private final M3IntArrayView source;
        IntSlice(M3IntArrayView source, int start, int end) { super(source.length(), start, end); this.source = source; }
        public int length() { return length; }
        public int intAt(int index) { return source.intAt(start + Objects.checkIndex(index, length)); }
        public int segmentCount() { return length == 0 ? 0 : source.segmentCount(); }
        public M3IntArrayView slice(int from, int to) {
            Objects.checkFromToIndex(from, to, length);
            return from == 0 && to == length ? this : new IntSlice(source, start + from, start + to);
        }
    }

    private static final class IntJoin implements M3IntArrayView {
        private final M3IntArrayView[] segments;
        private final int[] ends;
        private final int length;
        IntJoin(M3IntArrayView[] segments) {
            this.segments = segments.clone();
            ends = new int[segments.length];
            int total = 0;
            for (int index = 0; index < segments.length; index++) {
                total = Math.addExact(total, segments[index].length());
                ends[index] = total;
            }
            length = total;
        }
        public int length() { return length; }
        public int segmentCount() { int n=0; for (M3IntArrayView s:segments) n=Math.addExact(n,s.segmentCount()); return n; }
        public int intAt(int index) { int c=Objects.checkIndex(index,length), s=locate(ends,c), p=s==0?0:ends[s-1]; return segments[s].intAt(c-p); }
        public M3IntArrayView slice(int start,int end) { Objects.checkFromToIndex(start,end,length); return start==0&&end==length?this:new IntSlice(this,start,end); }
    }

    private static final class LongSnapshot implements M3LongArrayView {
        private final long[] data;
        LongSnapshot(long[] data) { this.data = data; }
        public int length() { return data.length; }
        public long longAt(int index) { return data[Objects.checkIndex(index, data.length)]; }
        public int segmentCount() { return data.length == 0 ? 0 : 1; }
        public M3LongArrayView slice(int start, int end) {
            Objects.checkFromToIndex(start, end, data.length);
            return start == 0 && end == data.length ? this : new LongSlice(this, start, end);
        }
    }

    private static final class LongSlice extends SliceBase implements M3LongArrayView {
        private final M3LongArrayView source;
        LongSlice(M3LongArrayView source,int start,int end){super(source.length(),start,end);this.source=source;}
        public int length(){return length;}
        public long longAt(int index){return source.longAt(start+Objects.checkIndex(index,length));}
        public int segmentCount(){return length==0?0:source.segmentCount();}
        public M3LongArrayView slice(int from,int to){Objects.checkFromToIndex(from,to,length);return from==0&&to==length?this:new LongSlice(source,start+from,start+to);}
    }

    private static final class LongJoin implements M3LongArrayView {
        private final M3LongArrayView[] segments;
        private final int[] ends;
        private final int length;
        LongJoin(M3LongArrayView[] segments){
            this.segments=segments.clone();ends=new int[segments.length];int total=0;
            for(int i=0;i<segments.length;i++){total=Math.addExact(total,segments[i].length());ends[i]=total;}length=total;
        }
        public int length(){return length;}
        public int segmentCount(){int n=0;for(M3LongArrayView s:segments)n=Math.addExact(n,s.segmentCount());return n;}
        public long longAt(int index){int c=Objects.checkIndex(index,length),s=locate(ends,c),p=s==0?0:ends[s-1];return segments[s].longAt(c-p);}
        public M3LongArrayView slice(int start,int end){Objects.checkFromToIndex(start,end,length);return start==0&&end==length?this:new LongSlice(this,start,end);}
    }

    private static int locate(int[] ends, int index) {
        int found = java.util.Arrays.binarySearch(ends, index + 1);
        return found >= 0 ? found : -found - 1;
    }

    private static List<ByteBuffer> buffers(M3ByteArrayView source, int start, int length) {
        ArrayList<ByteBuffer> result = new ArrayList<>();
        int skip = start;
        int remaining = length;
        for (ByteBuffer original : source.asReadOnlyBuffers()) {
            ByteBuffer buffer = original.asReadOnlyBuffer();
            if (skip >= buffer.remaining()) { skip -= buffer.remaining(); continue; }
            int take = Math.min(remaining, buffer.remaining() - skip);
            ByteBuffer view = buffer.slice(buffer.position() + skip, take).asReadOnlyBuffer();
            if (view.hasRemaining()) result.add(view);
            remaining -= take;
            skip = 0;
            if (remaining == 0) break;
        }
        if (remaining != 0) throw new IllegalStateException("byte segments do not cover slice");
        return result;
    }

    private static List<CharBuffer> charBuffers(M3Utf16ArrayView source, int start, int length) {
        ArrayList<CharBuffer> result = new ArrayList<>();
        int skip = start;
        int remaining = length;
        for (CharBuffer original : source.asReadOnlyCharBuffers()) {
            CharBuffer buffer = original.asReadOnlyBuffer();
            if (skip >= buffer.remaining()) { skip -= buffer.remaining(); continue; }
            int take = Math.min(remaining, buffer.remaining() - skip);
            CharBuffer view = buffer.slice(buffer.position() + skip, take).asReadOnlyBuffer();
            if (view.hasRemaining()) result.add(view);
            remaining -= take;
            skip = 0;
            if (remaining == 0) break;
        }
        if (remaining != 0) throw new IllegalStateException("UTF-16 segments do not cover slice");
        return result;
    }
}
