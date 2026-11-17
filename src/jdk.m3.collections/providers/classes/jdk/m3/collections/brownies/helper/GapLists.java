/*
 * Copyright 2013 by Thomas Mauch
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * $Id$
 */
package jdk.m3.collections.brownies.helper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import jdk.m3.collections.brownies.BigList;
import jdk.m3.collections.brownies.GapList;
import jdk.m3.collections.brownies.GapListsImpl;
import jdk.m3.collections.brownies.IList;
import jdk.m3.collections.brownies.Key1Collection;
import jdk.m3.collections.brownies.Key1List;
import jdk.m3.collections.brownies.Key2Collection;
import jdk.m3.collections.brownies.Key2List;
import jdk.m3.collections.brownies.KeyCollectionImpl;
import jdk.m3.collections.brownies.KeyListImpl;
import jdk.m3.collections.brownies.exceptions.DuplicateKeyException;
import jdk.m3.collections.brownies.primitive.BooleanObjGapList;
import jdk.m3.collections.brownies.primitive.ByteGapList;
import jdk.m3.collections.brownies.primitive.ByteObjGapList;
import jdk.m3.collections.brownies.primitive.CharGapList;
import jdk.m3.collections.brownies.primitive.CharObjGapList;
import jdk.m3.collections.brownies.primitive.DoubleObjGapList;
import jdk.m3.collections.brownies.primitive.FloatObjGapList;
import jdk.m3.collections.brownies.primitive.GapListsPrimitiveImpl;
import jdk.m3.collections.brownies.primitive.IntObjGapList;
import jdk.m3.collections.brownies.primitive.LongObjGapList;
import jdk.m3.collections.brownies.primitive.ShortObjGapList;

/**
 * Helper class offering various functionality: <br>
 * - create wrapper list objects wrapping primitive GapLists <br>
 * - methods interacting with CharSequence, Reader, InputStream <br>
 * - add methods explaining reason if fails with DuplicateKeyException <br>
 *
 * @author Thomas Mauch
 */
@SuppressWarnings("missing-explicit-ctor")
public class GapLists {

    /** Class {@link Impl} makes protected members of {@link GapListsImpl} locally accessible */
    protected static class Impl extends GapListsImpl {

        protected static <E, EE extends E> boolean doAddAllExplained(KeyCollectionImpl<E> list, Collection<EE> adds) {
            return GapListsImpl.addAllExplained(list, adds);
        }

        protected static <E, EE extends E> boolean doAddAllExplained(KeyListImpl<E> list, Collection<EE> adds) {
            return GapListsImpl.addAllExplained(list, adds);
        }

        protected static <E, EE extends E> boolean doAddExplained(KeyCollectionImpl<E> list, EE add) {
            return GapListsImpl.addExplained(list, add);
        }

        protected static <E, EE extends E> boolean doAddExplained(KeyListImpl<E> list, EE add) {
            return GapListsImpl.addExplained(list, add);
        }
    }

    /** Class {@link PrimitiveImpl} makes protected members of {@link GapListsPrimitiveImpl} locally accessible */
    protected static class PrimitiveImpl extends GapListsPrimitiveImpl {

        protected static byte[] doPrepareAddBuffer(ByteGapList list, int index, int len) {
            return GapListsPrimitiveImpl.prepareAddBuffer(list, index, len);
        }

        protected static void doReleaseAddBuffer(ByteGapList list, int index, int len) {
            GapListsPrimitiveImpl.releaseAddBuffer(list, index, len);
        }

        protected static char[] doPrepareAddBuffer(CharGapList list, int index, int len) {
            return GapListsPrimitiveImpl.prepareAddBuffer(list, index, len);
        }

        protected static void doReleaseAddBuffer(CharGapList list, int index, int len) {
            GapListsPrimitiveImpl.releaseAddBuffer(list, index, len);
        }
    }

    // addAll

    /**
     * Implementation of {@link Key1Collection#addAll} which adds an explanatory exception message
     * why adding failed with a {@link DuplicateKeyException}.
     */
    public static <E, EE extends E, K> boolean addAllExplained(Key1Collection<E, K> list, Collection<EE> adds) {
        return Impl.doAddAllExplained(list, adds);
    }

    /**
     * Implementation of {@link Key2Collection#addAll} which adds an explanatory exception message
     * why adding failed with a {@link DuplicateKeyException}.
     */
    public static <E, EE extends E, K1, K2> boolean addAllExplained(Key2Collection<E, K1, K2> list, Collection<EE> adds) {
        return Impl.doAddAllExplained(list, adds);
    }

    /**
     * Implementation of {@link Key1List#addAll} which adds an explanatory exception message
     * why adding failed with a {@link DuplicateKeyException}.
     */
    public static <E, EE extends E, K> boolean addAllExplained(Key1List<E, K> list, Collection<EE> adds) {
        return Impl.doAddAllExplained(list, adds);
    }

    /**
     * Implementation of {@link Key2List#addAll} which adds an explanatory exception message
     * why adding failed with a {@link DuplicateKeyException}.
     */
    public static <E, EE extends E, K1, K2> boolean addAllExplained(Key2List<E, K1, K2> list, Collection<EE> adds) {
        return Impl.doAddAllExplained(list, adds);
    }

    // add

    /**
     * Implementation of {@link Key1Collection#add} which adds an explanatory exception message
     * why adding failed with a {@link DuplicateKeyException}.
     */
    public static <E, EE extends E, K> boolean addExplained(Key1Collection<E, K> list, EE add) {
        return Impl.doAddExplained(list, add);
    }

    /**
     * Implementation of {@link Key2Collection#add} which adds an explanatory exception message
     * why adding failed with a {@link DuplicateKeyException}.
     */
    public static <E, EE extends E, K1, K2> boolean addExplained(Key2Collection<E, K1, K2> list, EE add) {
        return Impl.doAddExplained(list, add);
    }

    /**
     * Implementation of {@link Key1List#add} which adds an explanatory exception message
     * why adding failed with a {@link DuplicateKeyException}.
     */
    public static <E, EE extends E, K> boolean addExplained(Key1List<E, K> list, EE add) {
        return Impl.doAddExplained(list, add);
    }

    /**
     * Implementation of {@link Key2List#add} which adds an explanatory exception message
     * why adding failed with a {@link DuplicateKeyException}.
     */
    public static <E, EE extends E, K1, K2> boolean addExplained(Key2List<E, K1, K2> list, EE add) {
        return Impl.doAddExplained(list, add);
    }

    //

    /**
     * Create a GapList wrapping a primitive GapList, e.g. an IntObjGapList wrapping an IntGapList.
     *
     * @param type  primitive type for GapList
     * @return      created wrapping GapList
     * @throws      IllegalArgumentException if no primitive type is specified
     */
    public static IList<?> createWrapperList(Class<?> type) {
        if (type == int.class) {
            return new IntObjGapList();
        } else if (type == long.class) {
            return new LongObjGapList();
        } else if (type == double.class) {
            return new DoubleObjGapList();
        } else if (type == float.class) {
            return new FloatObjGapList();
        } else if (type == boolean.class) {
            return new BooleanObjGapList();
        } else if (type == byte.class) {
            return new ByteObjGapList();
        } else if (type == char.class) {
            return new CharObjGapList();
        } else if (type == short.class) {
            return new ShortObjGapList();
        } else {
            throw new IllegalArgumentException("Primitive type expected: " + type);
        }
    }

    /**
     * Create a GapList wrapping a primitive GapList, e.g. an IntObjGapList wrapping an IntGapList.
     *
     * @param type      primitive type for GapList
     * @param capacity  initial capacity of created list
     * @return          created wrapping GapList
     * @throws          IllegalArgumentException if no primitive type is specified
     */
    public static IList<?> createWrapperList(Class<?> type, int capacity) {
        if (type == int.class) {
            return new IntObjGapList(capacity);
        } else if (type == long.class) {
            return new LongObjGapList(capacity);
        } else if (type == double.class) {
            return new DoubleObjGapList(capacity);
        } else if (type == float.class) {
            return new FloatObjGapList(capacity);
        } else if (type == boolean.class) {
            return new BooleanObjGapList(capacity);
        } else if (type == byte.class) {
            return new ByteObjGapList(capacity);
        } else if (type == char.class) {
            return new CharObjGapList(capacity);
        } else if (type == short.class) {
            return new ShortObjGapList(capacity);
        } else {
            throw new IllegalArgumentException("Primitive type expected: " + type);
        }
    }

    //

    /**
     * Return collector which collects the elements into a {@link GapList}.
     *
     * @return collector
     */
    public static <T> Collector<T, ?, GapList<T>> toGapList() {
        return new CollectorImpl<>((Supplier<List<T>>) GapList::new, List::add,
                (left, right) -> {
                    left.addAll(right);
                    return left;
                },
                CollectorImpl.CH_ID);
    }

    /**
     * Return collector which collects the elements into a {@link BigList}.
     *
     * @return collector
     */
    public static <T> Collector<T, ?, BigList<T>> toBigList() {
        return new CollectorImpl<>((Supplier<List<T>>) BigList::new, List::add,
                (left, right) -> {
                    left.addAll(right);
                    return left;
                },
                CollectorImpl.CH_ID);
    }

    private static class CollectorImpl<T, A, R> implements Collector<T, A, R> {
        private final Supplier<A> supplier;
        private final BiConsumer<A, T> accumulator;
        private final BinaryOperator<A> combiner;
        private final Function<A, R> finisher;
        private final Set<Characteristics> characteristics;

        CollectorImpl(Supplier<A> supplier,
                BiConsumer<A, T> accumulator,
                BinaryOperator<A> combiner,
                Function<A, R> finisher,
                Set<Characteristics> characteristics) {
            this.supplier = supplier;
            this.accumulator = accumulator;
            this.combiner = combiner;
            this.finisher = finisher;
            this.characteristics = characteristics;
        }

        CollectorImpl(Supplier<A> supplier,
                BiConsumer<A, T> accumulator,
                BinaryOperator<A> combiner,
                Set<Characteristics> characteristics) {
            this(supplier, accumulator, combiner, castingIdentity(), characteristics);
        }

        static final Set<Collector.Characteristics> CH_ID = Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.IDENTITY_FINISH));

        @Override
        public BiConsumer<A, T> accumulator() {
            return accumulator;
        }

        @Override
        public Supplier<A> supplier() {
            return supplier;
        }

        @Override
        public BinaryOperator<A> combiner() {
            return combiner;
        }

        @Override
        public Function<A, R> finisher() {
            return finisher;
        }

        @Override
        public Set<Characteristics> characteristics() {
            return characteristics;
        }

        @SuppressWarnings("unchecked")
        private static <I, R> Function<I, R> castingIdentity() {
            return i -> (R) i;
        }
    }

    //

    /**
     * Read specified number of bytes from InputStream into ByteGapList.
     *
     * @param istream   input stream (source)
     * @param list      list (target)
     * @param len       maximum number of bytes to read
     * @return          number of bytes read into the buffer, -1 if end of stream has been reached
     */
    public static int read(InputStream istream, ByteGapList list, int len) throws IOException {
        int index = list.size();
        byte[] buf = PrimitiveImpl.doPrepareAddBuffer(list, index, len);

        int read = istream.read(buf, index, len);
        if (read == -1) {
            read = 0;
        }
        PrimitiveImpl.doReleaseAddBuffer(list, index, (read >= -0) ? read : 0);
        return read;
    }

    /**
     * Write specified number of bytes from ByteGapList into OutputStream.
     *
     * @param ostream   output stream (target)
     * @param list      list (source)
     * @param off       offset of first byte to write
     * @param len       number of bytes to write
     */
    public static void write(OutputStream ostream, ByteGapList list, int off, int len) throws IOException {
        int index = list.size();
        byte[] buf = PrimitiveImpl.doPrepareAddBuffer(list, index, 0);
        ostream.write(buf, off, len);
    }

    /**
     * Read specified number of chars from Reader into CharGapList.
     *
     * @param reader    reader (source)
     * @param list      list (target)
     * @param len       maximum number of bytes to read
     * @return          number of bytes read into the buffer, -1 if end of stream has been reached
     */
    public static int read(Reader reader, CharGapList list, int len) throws IOException {
        int index = list.size();
        char[] buf = PrimitiveImpl.doPrepareAddBuffer(list, index, len);

        int read = reader.read(buf, index, len);
        if (read == -1) {
            read = 0;
        }
        PrimitiveImpl.doReleaseAddBuffer(list, index, (read >= -0) ? read : 0);
        return read;
    }

    /**
     * Write specified number of chars from CharGapList into Writer.
     *
     * @param writer    writer (target)
     * @param list      list (source)
     * @param off       offset of first char to write
     * @param len       number of chars to write
     */
    public static void write(Writer writer, CharGapList list, int off, int len) throws IOException {
        int index = list.size();
        char[] buf = PrimitiveImpl.doPrepareAddBuffer(list, index, 0);
        writer.write(buf, off, len);
    }

    /**
     * Add specified number of chars from CharSequence into CharGapList.
     *
     * @param str       CharSequence (source)
     * @param list      list (target)
     * @param start     start position of characters to add in CharSequence
     * @param end       end position of characters to add in CharSequence
     */
    public static void add(CharSequence str, CharGapList list, int start, int end) {
        int index = list.size();
        int len = end - start;
        char[] buf = PrimitiveImpl.doPrepareAddBuffer(list, index, len);
        for (int i = 0; i < len; i++) {
            buf[index + i] = str.charAt(start + i);
        }
    }

}
