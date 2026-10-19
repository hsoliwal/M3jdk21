/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.IOUtils;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.PrimitiveIterator;
import java.util.Random;
import java.util.Spliterator;
import java.util.Streams;
import java.util.TreeSet;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/** Behavioral tests against the actual patched public utility classes. */
public final class StreamsIOUtilitiesContract {
    private static long checks;

    private StreamsIOUtilitiesContract() { }

    public static void main(String[] args) throws Exception {
        streamsFromOwners();
        streamsFromOptionalAndPrimitiveIterators();
        streamsValidationAndFailure();
        ioAuthorityDelegation();
        ioBoundedCopies();
        ioExactReads();
        ioExactSkips();
        ioContentComparison();
        ioTextAndValidation();
        ioFailureIdentity();
        randomizedIO();
        System.out.println("PASS StreamsIOUtilitiesContract checks=" + checks);
    }

    private static void streamsFromOwners() {
        Object first = new Object();
        Object second = new Object();
        Object third = new Object();
        ArrayList<Object> values = new ArrayList<>(Arrays.asList(first, second, first, null));
        Stream<Object> collectionStream = Streams.stream(values);
        check(!collectionStream.isParallel(), "collection is sequential");
        values.add(third);
        sameElements(collectionStream.toArray(), new Object[] { first, second, first, null, third },
                "collection late binding and identity");

        int[] spliteratorCalls = { 0 };
        Collection<Object> owner = new AbstractCollection<>() {
            @Override public Iterator<Object> iterator() { return values.iterator(); }
            @Override public int size() { return values.size(); }
            @Override public Spliterator<Object> spliterator() {
                spliteratorCalls[0]++;
                return values.spliterator();
            }
            @Override public Stream<Object> stream() {
                throw new AssertionError("must use this owner's spliterator");
            }
        };
        Spliterator<Object> actual = Streams.stream(owner).spliterator();
        check(spliteratorCalls[0] == 1, "one collection spliterator");
        check(actual.hasCharacteristics(Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED),
                "collection characteristics retained");
        check(actual.getExactSizeIfKnown() == values.size(), "collection exact size retained");

        Comparator<Integer> descending = Comparator.reverseOrder();
        TreeSet<Integer> sorted = new TreeSet<>(descending);
        sorted.addAll(List.of(1, 3, 2));
        Spliterator<Integer> sortedSource = Streams.stream(sorted).spliterator();
        check(sortedSource.hasCharacteristics(Spliterator.SORTED | Spliterator.DISTINCT | Spliterator.ORDERED),
                "sorted owner characteristics retained");
        check(sortedSource.getComparator() == descending, "sorted owner comparator retained");
        check(Arrays.equals(Streams.stream(sorted).toArray(Integer[]::new), new Integer[] { 3, 2, 1 }),
                "collection encounter order");

        BorrowedIterator iterator = new BorrowedIterator(new Object[] { first, null, second, third });
        try (Stream<Object> stream = Streams.stream(iterator)) {
            check(!stream.isParallel(), "iterator is sequential");
            check(iterator.nextCalls == 0 && iterator.hasNextCalls == 0, "iterator is lazy");
            sameElements(stream.limit(2).toArray(), new Object[] { first, null }, "iterator prefix");
            check(iterator.nextCalls == 2, "iterator short circuit consumes only prefix");
        }
        check(iterator.closeCalls == 0, "iterator remains borrowed");
        check(iterator.next() == second, "iterator leaves next value unread");

        BorrowedIterator iterableIterator = new BorrowedIterator(new Object[] { second, first });
        Iterable<Object> iterable = () -> iterableIterator;
        try (Stream<Object> stream = Streams.stream(iterable)) {
            check(!stream.isParallel(), "iterable is sequential");
            check(iterableIterator.nextCalls == 0, "iterable traversal is lazy");
            sameElements(stream.toArray(), new Object[] { second, first }, "iterable encounter order");
        }
        check(iterableIterator.closeCalls == 0, "iterable iterator remains borrowed");

        BorrowedEnumeration enumeration = new BorrowedEnumeration(new Object[] { third, first, second });
        try (Stream<Object> stream = Streams.stream(enumeration)) {
            check(!stream.isParallel(), "enumeration is sequential");
            check(enumeration.nextCalls == 0 && enumeration.hasNextCalls == 0, "enumeration is lazy");
            check(stream.findFirst().orElseThrow() == third, "enumeration first identity");
        }
        check(enumeration.nextCalls == 1, "enumeration short circuit");
        check(enumeration.closeCalls == 0, "enumeration remains borrowed");
        check(enumeration.nextElement() == first, "enumeration remaining order");
    }

    private static void streamsFromOptionalAndPrimitiveIterators() {
        Object value = new Object();
        check(Streams.stream(Optional.empty()).count() == 0, "empty optional");
        try (Stream<Object> stream = Streams.stream(Optional.of(value))) {
            check(!stream.isParallel(), "optional is sequential");
            check(stream.findFirst().orElseThrow() == value, "optional identity");
        }
        check(Streams.stream(OptionalInt.empty()).count() == 0, "empty optional int");
        check(Streams.stream(OptionalLong.empty()).count() == 0, "empty optional long");
        check(Streams.stream(OptionalDouble.empty()).count() == 0, "empty optional double");
        try (IntStream stream = Streams.stream(OptionalInt.of(Integer.MIN_VALUE))) {
            check(!stream.isParallel(), "optional int is sequential");
            check(Arrays.equals(stream.toArray(), new int[] { Integer.MIN_VALUE }), "optional int value");
        }
        try (LongStream stream = Streams.stream(OptionalLong.of(Long.MAX_VALUE))) {
            check(!stream.isParallel(), "optional long is sequential");
            check(Arrays.equals(stream.toArray(), new long[] { Long.MAX_VALUE }), "optional long value");
        }
        double taggedNaN = Double.longBitsToDouble(0x7ff8000000000042L);
        try (DoubleStream stream = Streams.stream(OptionalDouble.of(taggedNaN))) {
            check(!stream.isParallel(), "optional double is sequential");
            sameDoubleBits(stream.toArray(), new double[] { taggedNaN }, "optional double bits");
        }

        IntIterator ints = new IntIterator(new int[] { Integer.MIN_VALUE, 0, Integer.MAX_VALUE });
        try (IntStream stream = Streams.stream(ints)) {
            check(!stream.isParallel() && ints.nextCalls == 0 && ints.hasNextCalls == 0,
                    "primitive int lazy sequential");
            check(Arrays.equals(stream.limit(2).toArray(), new int[] { Integer.MIN_VALUE, 0 }),
                    "primitive int prefix without boxing");
        }
        check(ints.nextCalls == 2 && ints.nextInt() == Integer.MAX_VALUE, "primitive int remainder");
        check(ints.closeCalls == 0, "primitive int borrowed");

        LongIterator longs = new LongIterator(new long[] { Long.MIN_VALUE, 0, Long.MAX_VALUE });
        try (LongStream stream = Streams.stream(longs)) {
            check(!stream.isParallel() && longs.nextCalls == 0 && longs.hasNextCalls == 0,
                    "primitive long lazy sequential");
            check(Arrays.equals(stream.toArray(), new long[] { Long.MIN_VALUE, 0, Long.MAX_VALUE }),
                    "primitive long traversal without boxing");
        }
        check(longs.closeCalls == 0, "primitive long borrowed");

        double[] doubles = { -0.0, 0.0, Double.NEGATIVE_INFINITY, taggedNaN, Double.POSITIVE_INFINITY };
        DoubleIterator doubleIterator = new DoubleIterator(doubles);
        try (DoubleStream stream = Streams.stream(doubleIterator)) {
            check(!stream.isParallel() && doubleIterator.nextCalls == 0 && doubleIterator.hasNextCalls == 0,
                    "primitive double lazy sequential");
            sameDoubleBits(stream.toArray(), doubles, "primitive double traversal without boxing");
        }
        check(doubleIterator.closeCalls == 0, "primitive double borrowed");
    }

    private static void streamsValidationAndFailure() throws Exception {
        expect(NullPointerException.class, () -> Streams.stream((Collection<Object>) null));
        expect(NullPointerException.class, () -> Streams.stream((Iterable<Object>) null));
        expect(NullPointerException.class, () -> Streams.stream((Iterator<Object>) null));
        expect(NullPointerException.class, () -> Streams.stream((Enumeration<Object>) null));
        expect(NullPointerException.class, () -> Streams.stream((Optional<Object>) null));
        expect(NullPointerException.class, () -> Streams.stream((OptionalInt) null));
        expect(NullPointerException.class, () -> Streams.stream((OptionalLong) null));
        expect(NullPointerException.class, () -> Streams.stream((OptionalDouble) null));
        expect(NullPointerException.class, () -> Streams.stream((PrimitiveIterator.OfInt) null));
        expect(NullPointerException.class, () -> Streams.stream((PrimitiveIterator.OfLong) null));
        expect(NullPointerException.class, () -> Streams.stream((PrimitiveIterator.OfDouble) null));
        RuntimeException failure = new IllegalStateException("source failure identity");
        Iterator<Object> source = new Iterator<>() {
            @Override public boolean hasNext() { return true; }
            @Override public Object next() { throw failure; }
        };
        Stream<Object> stream = Streams.stream(source);
        check(expect(IllegalStateException.class, stream::toArray) == failure, "source exception identity");
    }

    private static void ioAuthorityDelegation() throws Exception {
        ByteSink bytes = new ByteSink();
        int[] byteTransfers = { 0 };
        InputStream byteAuthority = new InputStream() {
            @Override public int read() { throw new AssertionError("transfer authority was bypassed"); }
            @Override public long transferTo(OutputStream target) throws IOException {
                byteTransfers[0]++;
                check(target == bytes, "byte transfer target identity");
                target.write(0x9a);
                return 1L << 34;
            }
            @Override public void close() { throw new AssertionError("closed byte authority"); }
        };
        check(IOUtils.copy(byteAuthority, bytes) == 1L << 34, "whole byte copy preserves long result");
        check(byteTransfers[0] == 1 && Arrays.equals(bytes.bytes(), new byte[] { (byte) 0x9a }),
                "whole byte copy delegates exactly once");
        bytes.checkBorrowed();

        CharSink chars = new CharSink();
        int[] charTransfers = { 0 };
        Reader charAuthority = new Reader() {
            @Override public int read(char[] b, int o, int n) { throw new AssertionError("transfer authority bypassed"); }
            @Override public long transferTo(Writer target) throws IOException {
                charTransfers[0]++;
                check(target == chars, "char transfer target identity");
                target.write('\u03bb');
                return 1L << 35;
            }
            @Override public void close() { throw new AssertionError("closed char authority"); }
        };
        check(IOUtils.copy(charAuthority, chars) == 1L << 35, "whole char copy preserves long result");
        check(charTransfers[0] == 1 && chars.text().equals("\u03bb"), "whole char copy delegates exactly once");
        chars.checkBorrowed();

        byte[] allBytes = { 4, 0, (byte) 255 };
        int[] allCalls = { 0 };
        InputStream allAuthority = new InputStream() {
            @Override public int read() { throw new AssertionError("readAllBytes authority bypassed"); }
            @Override public byte[] readAllBytes() { allCalls[0]++; return allBytes; }
        };
        check(IOUtils.readAllBytes(allAuthority) == allBytes && allCalls[0] == 1,
                "readAllBytes authority and returned array retained");

        long[] skipArgument = { -1 };
        InputStream skipAuthority = new InputStream() {
            @Override public int read() { throw new AssertionError("skipNBytes authority bypassed"); }
            @Override public void skipNBytes(long n) { skipArgument[0] = n; }
        };
        IOUtils.skipFully(skipAuthority, Long.MAX_VALUE);
        check(skipArgument[0] == Long.MAX_VALUE, "skipNBytes authority retains long count");
        expect(IllegalArgumentException.class, () -> IOUtils.skipFully(skipAuthority, -1));
        check(skipArgument[0] == Long.MAX_VALUE, "negative count refused before skip authority");
    }

    private static void ioBoundedCopies() throws Exception {
        byte[] data = bytes(20003);
        char[] text = chars(20003);
        for (long limit : new long[] { 0, 1, 2, 7, 8191, 8192, 8193, 20003, 20004, Long.MAX_VALUE }) {
            int expected = (int) Math.min(data.length, limit);
            ByteSource input = new ByteSource(data, 317, 3);
            ByteSink output = new ByteSink();
            check(IOUtils.copy(input, output, limit) == expected, "bounded byte count " + limit);
            check(Arrays.equals(output.bytes(), Arrays.copyOf(data, expected)), "bounded byte contents " + limit);
            check(input.position == expected, "bounded byte no overread " + limit);
            check(input.maxRequest <= Math.min(8192L, limit), "bounded byte request budget");
            if (limit == 0) check(input.calls == 0 && input.singleCalls == 0 && output.writes == 0,
                    "zero byte limit has no I/O");
            input.checkBorrowed();
            output.checkBorrowed();

            CharSource reader = new CharSource(text, 193, 2);
            CharSink writer = new CharSink();
            check(IOUtils.copy(reader, writer, limit) == expected, "bounded char count " + limit);
            check(writer.text().equals(new String(text, 0, expected)), "bounded char contents " + limit);
            check(reader.position == expected, "bounded char no overread " + limit);
            check(reader.maxRequest <= Math.min(8192L, limit), "bounded char request budget");
            if (limit == 0) check(reader.calls == 0 && reader.singleCalls == 0 && writer.writes == 0,
                    "zero char limit has no I/O");
            reader.checkBorrowed();
            writer.checkBorrowed();
        }
        for (long invalid : new long[] { -1, Long.MIN_VALUE }) {
            ByteSource source = new ByteSource(data, 1, 1);
            ByteSink sink = new ByteSink();
            expect(IllegalArgumentException.class, () -> IOUtils.copy(source, sink, invalid));
            check(source.calls == 0 && source.singleCalls == 0 && sink.writes == 0, "invalid byte limit admission");
            CharSource reader = new CharSource(text, 1, 1);
            CharSink writer = new CharSink();
            expect(IllegalArgumentException.class, () -> IOUtils.copy(reader, writer, invalid));
            check(reader.calls == 0 && reader.singleCalls == 0 && writer.writes == 0, "invalid char limit admission");
        }
        ByteSource zeroBulk = new ByteSource(new byte[] { 0, (byte) 255, 1 }, 1, 1);
        ByteSink zeroOutput = new ByteSink();
        check(IOUtils.copy(zeroBulk, zeroOutput, 3) == 3, "all-zero bulk byte progress");
        check(Arrays.equals(zeroOutput.bytes(), zeroBulk.data) && zeroBulk.singleCalls == 3,
                "single-byte fallback preserves NUL and unsigned bytes");
        CharSource zeroReader = new CharSource(new char[] { 0, '\uffff', 'x' }, 1, 1);
        CharSink zeroWriter = new CharSink();
        check(IOUtils.copy(zeroReader, zeroWriter, 3) == 3, "all-zero bulk char progress");
        check(zeroWriter.text().equals(new String(zeroReader.data)) && zeroReader.singleCalls == 3,
                "single-char fallback preserves NUL and max char");
    }

    private static void ioExactReads() throws Exception {
        byte[] data = bytes(9001);
        char[] text = chars(9001);
        byte[] bytes = new byte[data.length + 10];
        char[] chars = new char[text.length + 10];
        Arrays.fill(bytes, (byte) 83);
        Arrays.fill(chars, '\u5728');
        ByteSource input = new ByteSource(data, 29, 2);
        CharSource reader = new CharSource(text, 31, 3);
        IOUtils.readFully(input, bytes, 5, data.length);
        IOUtils.readFully(reader, chars, 5, text.length);
        check(Arrays.equals(Arrays.copyOfRange(bytes, 5, 5 + data.length), data), "exact byte offset contents");
        check(Arrays.equals(Arrays.copyOfRange(chars, 5, 5 + text.length), text), "exact char offset contents");
        for (int i = 0; i < 5; i++) {
            check(bytes[i] == 83 && bytes[bytes.length - 1 - i] == 83, "exact byte outside guards");
            check(chars[i] == '\u5728' && chars[chars.length - 1 - i] == '\u5728', "exact char outside guards");
        }
        input.checkBorrowed();
        reader.checkBorrowed();

        byte[] partial = new byte[9];
        char[] partialChars = new char[9];
        Arrays.fill(partial, (byte) 67);
        Arrays.fill(partialChars, 'Q');
        ByteSource shortInput = new ByteSource(new byte[] { 0, 1, 2 }, 1, 2);
        CharSource shortReader = new CharSource(new char[] { 0, 'a', '\ud800' }, 1, 2);
        expect(EOFException.class, () -> IOUtils.readFully(shortInput, partial, 2, 5));
        expect(EOFException.class, () -> IOUtils.readFully(shortReader, partialChars, 2, 5));
        check(Arrays.equals(partial, new byte[] { 67, 67, 0, 1, 2, 67, 67, 67, 67 }), "EOF retains exact byte prefix");
        check(Arrays.equals(partialChars, new char[] { 'Q', 'Q', 0, 'a', '\ud800', 'Q', 'Q', 'Q', 'Q' }),
                "EOF retains exact char prefix");

        ByteSource empty = new ByteSource(new byte[0], 1, 1);
        CharSource emptyReader = new CharSource(new char[0], 1, 1);
        IOUtils.readFully(empty, partial, partial.length, 0);
        IOUtils.readFully(emptyReader, partialChars, partialChars.length, 0);
        IOUtils.readFully(empty, new byte[0]);
        IOUtils.readFully(emptyReader, new char[0]);
        check(empty.calls == 0 && emptyReader.calls == 0, "empty exact reads perform no I/O");
        int[][] invalid = { { -1, 0 }, { 0, -1 }, { 9, 1 }, { 10, 0 },
                { Integer.MAX_VALUE, 2 }, { 1, Integer.MAX_VALUE }, { Integer.MIN_VALUE, Integer.MAX_VALUE } };
        for (int[] range : invalid) {
            expect(IndexOutOfBoundsException.class, () -> IOUtils.readFully(empty, partial, range[0], range[1]));
            expect(IndexOutOfBoundsException.class, () -> IOUtils.readFully(emptyReader, partialChars, range[0], range[1]));
        }
        check(empty.calls == 0 && emptyReader.calls == 0, "invalid overflow ranges refused before input");
        byte[] whole = new byte[3];
        char[] wholeChars = new char[3];
        IOUtils.readFully(new ByteSource(new byte[] { 1, 2, 3 }, 1, 1), whole);
        IOUtils.readFully(new CharSource(new char[] { 'a', 'b', 'c' }, 1, 1), wholeChars);
        check(Arrays.equals(whole, new byte[] { 1, 2, 3 }), "whole byte readFully");
        check(Arrays.equals(wholeChars, new char[] { 'a', 'b', 'c' }), "whole char readFully");
    }

    private static void ioExactSkips() throws Exception {
        for (int n : new int[] { 0, 1, 2, 7, 19 }) {
            ByteSource input = new ByteSource(bytes(20), 3, 0);
            CharSource reader = new CharSource(chars(20), 3, 0);
            input.zeroSkips = true;
            reader.zeroSkips = true;
            IOUtils.skipFully(input, n);
            IOUtils.skipFully(reader, n);
            check(input.position == n && reader.position == n, "zero-skip fallback exact position");
            check(input.singleCalls == n && reader.singleCalls == n, "zero-skip fallback finite progress");
            input.checkBorrowed();
            reader.checkBorrowed();
        }
        ByteSource input = new ByteSource(bytes(20), 3, 0);
        CharSource reader = new CharSource(chars(20), 4, 0);
        IOUtils.skipFully(input, 17);
        IOUtils.skipFully(reader, 17);
        check(input.position == 17 && reader.position == 17, "partial skip exact position");
        check(input.singleCalls == 0 && reader.singleCalls == 0, "positive skip reused");
        expect(EOFException.class, () -> IOUtils.skipFully(input, Long.MAX_VALUE));
        expect(EOFException.class, () -> IOUtils.skipFully(reader, Long.MAX_VALUE));
        check(input.position == 20 && reader.position == 20, "huge skip reaches finite EOF without overflow");
        for (long count : new long[] { -1, Long.MIN_VALUE }) {
            ByteSource invalid = new ByteSource(bytes(2), 1, 0);
            CharSource invalidReader = new CharSource(chars(2), 1, 0);
            expect(IllegalArgumentException.class, () -> IOUtils.skipFully(invalid, count));
            expect(IllegalArgumentException.class, () -> IOUtils.skipFully(invalidReader, count));
            check(invalid.skipCalls == 0 && invalid.singleCalls == 0, "negative byte skip admission");
            check(invalidReader.skipCalls == 0 && invalidReader.singleCalls == 0, "negative char skip admission");
        }
        for (long badResult : new long[] { -1, 2 }) {
            Reader invalid = new Reader() {
                @Override public long skip(long n) { return badResult; }
                @Override public int read(char[] b, int o, int n) { throw new AssertionError("invalid skip accepted"); }
                @Override public void close() { throw new AssertionError("invalid skipper closed"); }
            };
            expect(IOException.class, () -> IOUtils.skipFully(invalid, 1));
        }
    }

    private static void ioContentComparison() throws Exception {
        for (int length : new int[] { 0, 1, 2, 3, 8191, 8192, 8193, 16384, 20003 }) {
            byte[] data = bytes(length);
            char[] text = chars(length);
            ByteSource left = new ByteSource(data, 19, 2);
            ByteSource right = new ByteSource(data.clone(), 8192, 3);
            CharSource first = new CharSource(text, 37, 3);
            CharSource second = new CharSource(text.clone(), 8192, 2);
            check(IOUtils.contentEquals(left, right), "equal bytes under unequal fragmentation " + length);
            check(IOUtils.contentEquals(first, second), "equal chars under unequal fragmentation " + length);
            left.checkBorrowed();
            right.checkBorrowed();
            first.checkBorrowed();
            second.checkBorrowed();
            check(!IOUtils.contentEquals(new ByteSource(data, 7, 2),
                    new ByteSource(Arrays.copyOf(data, length + 1), 23, 3)), "byte length mismatch");
            check(!IOUtils.contentEquals(new CharSource(text, 11, 2),
                    new CharSource(Arrays.copyOf(text, length + 1), 17, 3)), "char length mismatch");
            if (length > 0) {
                for (int changed : new int[] { 0, length / 2, length - 1 }) {
                    byte[] different = data.clone();
                    char[] differentChars = text.clone();
                    different[changed] ^= 1;
                    differentChars[changed] ^= 1;
                    check(!IOUtils.contentEquals(new ByteSource(data, 8192, 2),
                            new ByteSource(different, 3, 3)), "byte mismatch position " + changed);
                    check(!IOUtils.contentEquals(new CharSource(text, 8192, 3),
                            new CharSource(differentChars, 5, 2)), "char mismatch position " + changed);
                }
            }
        }
        ByteSource self = new ByteSource(bytes(2), 1, 1);
        CharSource charSelf = new CharSource(chars(2), 1, 1);
        check(IOUtils.contentEquals(self, self) && self.calls == 0 && self.singleCalls == 0,
                "same byte source needs no input");
        check(IOUtils.contentEquals(charSelf, charSelf) && charSelf.calls == 0 && charSelf.singleCalls == 0,
                "same char source needs no input");
    }

    private static void ioTextAndValidation() throws Exception {
        String text = "A\u0000\u03bb\u6c34\ud83d\ude80\r\n";
        for (Charset charset : new Charset[] { StandardCharsets.UTF_8, StandardCharsets.UTF_16LE,
                StandardCharsets.UTF_16BE }) {
            ByteSource input = new ByteSource(text.getBytes(charset), 2, 0);
            check(IOUtils.toString(input, charset).equals(text), "explicit text charset " + charset);
            input.checkBorrowed();
        }
        byte[] malformed = { (byte) 0xc0, (byte) 0xaf, 0x41, (byte) 0xff };
        check(IOUtils.toString(new ByteArrayInputStream(malformed), StandardCharsets.UTF_8)
                .equals(new String(malformed, StandardCharsets.UTF_8)), "documented replacement decoding");
        CharSource reader = new CharSource(text.toCharArray(), 2, 0);
        check(IOUtils.toString(reader).equals(text), "already decoded reader text");
        reader.checkBorrowed();
        ByteSource all = new ByteSource(bytes(9001), 173, 0);
        check(Arrays.equals(IOUtils.readAllBytes(all), all.data), "remaining bytes delegated contents");
        all.checkBorrowed();
        ByteSource unchanged = new ByteSource(bytes(2), 1, 0);
        expect(NullPointerException.class, () -> IOUtils.toString(unchanged, null));
        check(unchanged.calls == 0 && unchanged.singleCalls == 0, "null charset rejected before input");

        ByteSink byteSink = new ByteSink();
        CharSink charSink = new CharSink();
        ByteSource byteSource = new ByteSource(new byte[0], 1, 0);
        CharSource charSource = new CharSource(new char[0], 1, 0);
        expect(NullPointerException.class, () -> IOUtils.copy((InputStream) null, byteSink));
        expect(NullPointerException.class, () -> IOUtils.copy(byteSource, (OutputStream) null));
        expect(NullPointerException.class, () -> IOUtils.copy((Reader) null, charSink));
        expect(NullPointerException.class, () -> IOUtils.copy(charSource, (Writer) null));
        expect(NullPointerException.class, () -> IOUtils.copy((InputStream) null, byteSink, 0));
        expect(NullPointerException.class, () -> IOUtils.copy(byteSource, (OutputStream) null, 0));
        expect(NullPointerException.class, () -> IOUtils.copy((Reader) null, charSink, 0));
        expect(NullPointerException.class, () -> IOUtils.copy(charSource, (Writer) null, 0));
        expect(NullPointerException.class, () -> IOUtils.readFully((InputStream) null, new byte[0]));
        expect(NullPointerException.class, () -> IOUtils.readFully(byteSource, (byte[]) null));
        expect(NullPointerException.class, () -> IOUtils.readFully((Reader) null, new char[0]));
        expect(NullPointerException.class, () -> IOUtils.readFully(charSource, (char[]) null));
        expect(NullPointerException.class, () -> IOUtils.readFully((InputStream) null, new byte[0], 0, 0));
        expect(NullPointerException.class, () -> IOUtils.readFully(byteSource, null, 0, 0));
        expect(NullPointerException.class, () -> IOUtils.readFully((Reader) null, new char[0], 0, 0));
        expect(NullPointerException.class, () -> IOUtils.readFully(charSource, null, 0, 0));
        expect(NullPointerException.class, () -> IOUtils.skipFully((InputStream) null, 0));
        expect(NullPointerException.class, () -> IOUtils.skipFully((Reader) null, 0));
        expect(NullPointerException.class, () -> IOUtils.contentEquals((InputStream) null, byteSource));
        expect(NullPointerException.class, () -> IOUtils.contentEquals(byteSource, (InputStream) null));
        expect(NullPointerException.class, () -> IOUtils.contentEquals((Reader) null, charSource));
        expect(NullPointerException.class, () -> IOUtils.contentEquals(charSource, (Reader) null));
        expect(NullPointerException.class, () -> IOUtils.readAllBytes(null));
        expect(NullPointerException.class, () -> IOUtils.toString(null, StandardCharsets.UTF_8));
        expect(NullPointerException.class, () -> IOUtils.toString((Reader) null));
        check(byteSource.calls == 0 && charSource.calls == 0 && byteSink.writes == 0 && charSink.writes == 0,
                "null admission leaves sources and sinks untouched");
    }

    private static void ioFailureIdentity() throws Exception {
        IOException failure = new IOException("deliberate source failure");
        ByteSource input = new ByteSource(new byte[] { 1, 2, 3, 4 }, 1, 0);
        input.failAt = 2;
        input.failure = failure;
        byte[] target = { 9, 9, 9, 9, 9, 9 };
        check(expect(IOException.class, () -> IOUtils.readFully(input, target, 1, 4)) == failure,
                "exact byte read exception identity");
        check(Arrays.equals(target, new byte[] { 9, 1, 2, 9, 9, 9 }), "read exception retains byte prefix");
        CharSource reader = new CharSource(new char[] { 'a', 'b', 'c', 'd' }, 1, 0);
        reader.failAt = 2;
        reader.failure = failure;
        char[] chars = { 'Q', 'Q', 'Q', 'Q', 'Q', 'Q' };
        check(expect(IOException.class, () -> IOUtils.readFully(reader, chars, 1, 4)) == failure,
                "exact char read exception identity");
        check(Arrays.equals(chars, new char[] { 'Q', 'a', 'b', 'Q', 'Q', 'Q' }),
                "read exception retains char prefix");
        ByteSource copySource = new ByteSource(bytes(4), 1, 0);
        copySource.failAt = 2;
        copySource.failure = failure;
        ByteSink copySink = new ByteSink();
        check(expect(IOException.class, () -> IOUtils.copy(copySource, copySink, 4)) == failure,
                "bounded copy read exception identity");
        check(Arrays.equals(copySink.bytes(), Arrays.copyOf(copySource.data, 2)), "copy failure retains prefix");
        OutputStream failingOutput = new OutputStream() {
            @Override public void write(int b) throws IOException { throw failure; }
            @Override public void close() { throw new AssertionError("failure output closed"); }
            @Override public void flush() { throw new AssertionError("failure output flushed"); }
        };
        check(expect(IOException.class, () -> IOUtils.copy(new ByteArrayInputStream(new byte[] { 1 }),
                failingOutput, 1)) == failure, "bounded copy output exception identity");
        Writer failingWriter = new Writer() {
            @Override public void write(char[] b, int o, int n) throws IOException { throw failure; }
            @Override public void close() { throw new AssertionError("failure writer closed"); }
            @Override public void flush() { throw new AssertionError("failure writer flushed"); }
        };
        check(expect(IOException.class, () -> IOUtils.copy(new StringReader("x"), failingWriter, 1)) == failure,
                "bounded char output exception identity");
        ByteSource comparison = new ByteSource(bytes(4), 1, 0);
        comparison.failAt = 0;
        comparison.failure = failure;
        check(expect(IOException.class, () -> IOUtils.contentEquals(comparison,
                new ByteArrayInputStream(bytes(4)))) == failure, "comparison exception identity");
        input.checkBorrowed();
        reader.checkBorrowed();
        copySource.checkBorrowed();
        copySink.checkBorrowed();
        comparison.checkBorrowed();
    }

    private static void randomizedIO() throws Exception {
        Random random = new Random(0x3a21c0ffeeL);
        for (int sample = 0; sample < 240; sample++) {
            int length = random.nextInt(10001);
            byte[] data = new byte[length];
            char[] text = new char[length];
            random.nextBytes(data);
            for (int i = 0; i < length; i++) text[i] = (char) random.nextInt(65536);
            int limit = random.nextInt(length + 33);
            int expected = Math.min(length, limit);
            int fragment = 1 + random.nextInt(521);
            ByteSource input = new ByteSource(data, fragment, 2 + random.nextInt(4));
            ByteSink output = new ByteSink();
            CharSource reader = new CharSource(text, fragment, 2 + random.nextInt(4));
            CharSink writer = new CharSink();
            check(IOUtils.copy(input, output, limit) == expected, "random byte copy count");
            check(IOUtils.copy(reader, writer, limit) == expected, "random char copy count");
            check(Arrays.equals(output.bytes(), Arrays.copyOf(data, expected)), "random byte copy contents");
            check(writer.text().equals(new String(text, 0, expected)), "random char copy contents");
            check(input.position == expected && reader.position == expected, "random copy no overread");

            byte[] other = data.clone();
            char[] otherChars = text.clone();
            if (length != 0 && random.nextBoolean()) {
                other[random.nextInt(length)] ^= 7;
                otherChars[random.nextInt(length)] ^= 31;
            }
            check(IOUtils.contentEquals(new ByteSource(data, fragment, 3),
                    new ByteSource(other, 1 + random.nextInt(619), 2)) == Arrays.equals(data, other),
                    "random byte equality independent array oracle");
            check(IOUtils.contentEquals(new CharSource(text, fragment, 2),
                    new CharSource(otherChars, 1 + random.nextInt(619), 3)) == Arrays.equals(text, otherChars),
                    "random char equality independent array oracle");
            byte[] exact = new byte[length];
            char[] exactChars = new char[length];
            IOUtils.readFully(new ByteSource(data, fragment, 2), exact);
            IOUtils.readFully(new CharSource(text, fragment, 3), exactChars);
            check(Arrays.equals(exact, data), "random exact byte read");
            check(Arrays.equals(exactChars, text), "random exact char read");

            ByteSink whole = new ByteSink();
            CharSink wholeChars = new CharSink();
            ByteSource wholeInput = new ByteSource(data, fragment, 0);
            CharSource wholeReader = new CharSource(text, fragment, 0);
            check(IOUtils.copy(wholeInput, whole) == length && Arrays.equals(whole.bytes(), data),
                    "random whole byte transfer");
            check(IOUtils.copy(wholeReader, wholeChars) == length && wholeChars.text().equals(new String(text)),
                    "random whole char transfer");
            whole.checkBorrowed();
            wholeChars.checkBorrowed();
            wholeInput.checkBorrowed();
            wholeReader.checkBorrowed();
        }
    }

    private static byte[] bytes(int length) {
        byte[] result = new byte[length];
        for (int i = 0; i < length; i++) result[i] = (byte) (i * 131 + (i >>> 8));
        return result;
    }

    private static char[] chars(int length) {
        char[] result = new char[length];
        for (int i = 0; i < length; i++) result[i] = (char) (i * 1031 + (i >>> 8));
        return result;
    }

    private static void sameElements(Object[] actual, Object[] expected, String message) {
        check(actual.length == expected.length, message + " length");
        for (int i = 0; i < expected.length; i++) check(actual[i] == expected[i], message + " identity " + i);
    }

    private static void sameDoubleBits(double[] actual, double[] expected, String message) {
        check(actual.length == expected.length, message + " length");
        for (int i = 0; i < expected.length; i++) {
            check(Double.doubleToRawLongBits(actual[i]) == Double.doubleToRawLongBits(expected[i]),
                    message + " index " + i);
        }
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    @FunctionalInterface
    private interface Action { void run() throws Exception; }

    private static <T extends Throwable> T expect(Class<T> type, Action action) throws Exception {
        checks++;
        try {
            action.run();
        } catch (Throwable failure) {
            if (type.isInstance(failure)) return type.cast(failure);
            throw new AssertionError("Expected " + type.getName() + ", got " + failure, failure);
        }
        throw new AssertionError("Expected " + type.getName());
    }

    private static final class BorrowedIterator implements Iterator<Object>, AutoCloseable {
        final Object[] values;
        int position, nextCalls, hasNextCalls, closeCalls;
        BorrowedIterator(Object[] values) { this.values = values; }
        @Override public boolean hasNext() { hasNextCalls++; return position < values.length; }
        @Override public Object next() {
            nextCalls++;
            if (position == values.length) throw new NoSuchElementException();
            return values[position++];
        }
        @Override public void close() { closeCalls++; }
    }

    private static final class BorrowedEnumeration implements Enumeration<Object>, AutoCloseable {
        final Object[] values;
        int position, nextCalls, hasNextCalls, closeCalls;
        BorrowedEnumeration(Object[] values) { this.values = values; }
        @Override public boolean hasMoreElements() { hasNextCalls++; return position < values.length; }
        @Override public Object nextElement() {
            nextCalls++;
            if (position == values.length) throw new NoSuchElementException();
            return values[position++];
        }
        @Override public void close() { closeCalls++; }
    }

    private static final class IntIterator implements PrimitiveIterator.OfInt, AutoCloseable {
        final int[] values;
        int position, nextCalls, hasNextCalls, closeCalls;
        IntIterator(int[] values) { this.values = values; }
        @Override public boolean hasNext() { hasNextCalls++; return position < values.length; }
        @Override public int nextInt() {
            nextCalls++;
            if (position == values.length) throw new NoSuchElementException();
            return values[position++];
        }
        @Override public Integer next() { throw new AssertionError("boxed int element traversal"); }
        @Override public void close() { closeCalls++; }
    }

    private static final class LongIterator implements PrimitiveIterator.OfLong, AutoCloseable {
        final long[] values;
        int position, nextCalls, hasNextCalls, closeCalls;
        LongIterator(long[] values) { this.values = values; }
        @Override public boolean hasNext() { hasNextCalls++; return position < values.length; }
        @Override public long nextLong() {
            nextCalls++;
            if (position == values.length) throw new NoSuchElementException();
            return values[position++];
        }
        @Override public Long next() { throw new AssertionError("boxed long element traversal"); }
        @Override public void close() { closeCalls++; }
    }

    private static final class DoubleIterator implements PrimitiveIterator.OfDouble, AutoCloseable {
        final double[] values;
        int position, nextCalls, hasNextCalls, closeCalls;
        DoubleIterator(double[] values) { this.values = values; }
        @Override public boolean hasNext() { hasNextCalls++; return position < values.length; }
        @Override public double nextDouble() {
            nextCalls++;
            if (position == values.length) throw new NoSuchElementException();
            return values[position++];
        }
        @Override public Double next() { throw new AssertionError("boxed double element traversal"); }
        @Override public void close() { closeCalls++; }
    }

    private static final class ByteSource extends InputStream {
        final byte[] data;
        final int fragment, zeroEvery;
        int position, calls, singleCalls, skipCalls, closeCalls, maxRequest;
        int failAt = -1;
        boolean zeroSkips;
        IOException failure;
        ByteSource(byte[] data, int fragment, int zeroEvery) {
            this.data = data;
            this.fragment = fragment;
            this.zeroEvery = zeroEvery;
        }
        private void beforeRead() throws IOException {
            if (calls + singleCalls > 8 * data.length + 100) throw new AssertionError("byte progress budget exhausted");
            if (failAt >= 0 && position >= failAt) throw Objects.requireNonNull(failure);
        }
        @Override public int read(byte[] target, int offset, int length) throws IOException {
            Objects.checkFromIndexSize(offset, length, target.length);
            if (length == 0) return 0;
            calls++;
            beforeRead();
            maxRequest = Math.max(maxRequest, length);
            if (zeroEvery > 0 && calls % zeroEvery == 0) return 0;
            if (position == data.length) return -1;
            int count = Math.min(length, Math.min(fragment, data.length - position));
            if (failAt >= 0) count = Math.min(count, failAt - position);
            System.arraycopy(data, position, target, offset, count);
            position += count;
            return count;
        }
        @Override public int read() throws IOException {
            singleCalls++;
            beforeRead();
            return position == data.length ? -1 : data[position++] & 255;
        }
        @Override public long skip(long count) {
            skipCalls++;
            if (zeroSkips) return 0;
            int moved = (int) Math.min(Math.max(0, count), Math.min(fragment, data.length - position));
            position += moved;
            return moved;
        }
        @Override public void close() { closeCalls++; }
        void checkBorrowed() { check(closeCalls == 0, "byte input not closed"); }
    }

    private static final class CharSource extends Reader {
        final char[] data;
        final int fragment, zeroEvery;
        int position, calls, singleCalls, skipCalls, closeCalls, maxRequest;
        int failAt = -1;
        boolean zeroSkips;
        IOException failure;
        CharSource(char[] data, int fragment, int zeroEvery) {
            this.data = data;
            this.fragment = fragment;
            this.zeroEvery = zeroEvery;
        }
        private void beforeRead() throws IOException {
            if (calls + singleCalls > 8 * data.length + 100) throw new AssertionError("char progress budget exhausted");
            if (failAt >= 0 && position >= failAt) throw Objects.requireNonNull(failure);
        }
        @Override public int read(char[] target, int offset, int length) throws IOException {
            Objects.checkFromIndexSize(offset, length, target.length);
            if (length == 0) return 0;
            calls++;
            beforeRead();
            maxRequest = Math.max(maxRequest, length);
            if (zeroEvery > 0 && calls % zeroEvery == 0) return 0;
            if (position == data.length) return -1;
            int count = Math.min(length, Math.min(fragment, data.length - position));
            if (failAt >= 0) count = Math.min(count, failAt - position);
            System.arraycopy(data, position, target, offset, count);
            position += count;
            return count;
        }
        @Override public int read() throws IOException {
            singleCalls++;
            beforeRead();
            return position == data.length ? -1 : data[position++];
        }
        @Override public long skip(long count) {
            skipCalls++;
            if (zeroSkips) return 0;
            int moved = (int) Math.min(Math.max(0, count), Math.min(fragment, data.length - position));
            position += moved;
            return moved;
        }
        @Override public void close() { closeCalls++; }
        void checkBorrowed() { check(closeCalls == 0, "reader not closed"); }
    }

    private static final class ByteSink extends OutputStream {
        final ByteArrayOutputStream data = new ByteArrayOutputStream();
        int writes, closeCalls, flushCalls;
        @Override public void write(int value) { writes++; data.write(value); }
        @Override public void write(byte[] source, int offset, int length) { writes++; data.write(source, offset, length); }
        @Override public void close() { closeCalls++; }
        @Override public void flush() { flushCalls++; }
        byte[] bytes() { return data.toByteArray(); }
        void checkBorrowed() { check(closeCalls == 0 && flushCalls == 0, "byte output not closed or flushed"); }
    }

    private static final class CharSink extends Writer {
        final StringBuilder data = new StringBuilder();
        int writes, closeCalls, flushCalls;
        @Override public void write(char[] source, int offset, int length) { writes++; data.append(source, offset, length); }
        @Override public void close() { closeCalls++; }
        @Override public void flush() { flushCalls++; }
        String text() { return data.toString(); }
        void checkBorrowed() { check(closeCalls == 0 && flushCalls == 0, "writer not closed or flushed"); }
    }
}
