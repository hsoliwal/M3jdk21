/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0 */
import java.lang.reflect.Field;
import java.util.Arrays;

public final class MIndexStringInvariant {
    private static final Field STRING_MINDEX = field(String.class, "mindex");
    private static final Field STRING_VALUE = field(String.class, "value");
    private static final Class<?> STORAGE = storageClass();
    private static final Field KIND = field(STORAGE, "storageKind");
    private static final Field LOCAL_VALUE = field(STORAGE, "localValue");
    private static final Field MAPPED_ADDRESS = field(STORAGE, "mappedAddress");
    private static final Field SEGMENTS = field(STORAGE, "segments");
    private static final Field MATERIALIZED = field(STORAGE, "materialized");
    private static final Field CANONICAL_ID = field(STORAGE, "canonicalId");
    private static final Field STRUCTURAL_HASH = field(STORAGE, "structuralHash64");

    private static final byte LOCAL = 1;
    private static final byte LEXICON = 2;
    private static final byte JOINED = 3;

    public static void main(String[] args) throws Exception {
        boolean expectLexicon = Boolean.getBoolean("m3.expect.lexicon");

        String alpha1 = new String(new char[] {'a','l','p','h','a'});
        String alpha2 = new String(new char[] {'a','l','p','h','a'});
        eq(5, alpha1.length(), "alpha length activates MIndex");
        eq(alpha1, alpha2, "alpha equality");

        Object alphaStorage1 = storage(alpha1);
        Object alphaStorage2 = storage(alpha2);
        same(alphaStorage1, alphaStorage2, "equal alpha values share scalar storage");
        eq(expectLexicon ? LEXICON : LOCAL, kind(alphaStorage1), "alpha storage kind");
        if (expectLexicon) {
            check(MAPPED_ADDRESS.getLong(alphaStorage1) != 0L, "lexicon atom address");
            check(LOCAL_VALUE.get(alphaStorage1) == null, "lexicon atom must not retain heap bytes");
            eq(0, ((byte[]) STRING_VALUE.get(alpha1)).length,
                    "lexicon-backed constructor discards temporary Compact-String bytes");
            eq(0, ((byte[]) STRING_VALUE.get(alpha2)).length,
                    "equal lexicon-backed constructor also keeps only compatibility sentinel");
        }

        String miss1 = new String(new char[] {'g','a','m','m','a'});
        String miss2 = new String(new char[] {'g','a','m','m','a'});
        miss1.hashCode();
        miss2.hashCode();
        Object missStorage1 = storage(miss1);
        Object missStorage2 = storage(miss2);
        same(missStorage1, missStorage2, "VM-local miss shares scalar storage");
        eq(LOCAL, kind(missStorage1), "miss storage kind");
        Object missBytes = LOCAL_VALUE.get(missStorage1);
        same(missBytes, LOCAL_VALUE.get(missStorage2),
                "VM-local miss shares exact canonical byte[]");
        same(missBytes, STRING_VALUE.get(miss1),
                "String.value is the canonical local atom byte[]");
        same(missBytes, STRING_VALUE.get(miss2),
                "equal String wrapper reuses the same canonical local atom byte[]");

        String joined1 = alpha1.concat(miss1);
        String joined2 = alpha2.concat(miss2);
        eq("alphagamma", joined1, "joined contents");
        Object joinedStorage1 = storage(joined1);
        Object joinedStorage2 = storage(joined2);
        eq(JOINED, kind(joinedStorage1), "joined storage kind");
        same(joinedStorage1, joinedStorage2, "equal joins share canonical tuple");
        eq(0, ((byte[])STRING_VALUE.get(joined1)).length,
                "joined String.value is compatibility sentinel");
        check(CANONICAL_ID.getLong(joinedStorage1) == CANONICAL_ID.getLong(joinedStorage2),
                "canonical tuple ID");
        check(STRUCTURAL_HASH.getLong(joinedStorage1) == STRUCTURAL_HASH.getLong(joinedStorage2),
                "structural hash");
        Object[] atoms = (Object[]) SEGMENTS.get(joinedStorage1);
        same(alphaStorage1, atoms[0], "join reuses lexicon/local alpha atom");
        same(missStorage1, atoms[1], "join reuses VM-local miss atom");

        String alphaSlice = joined1.substring(0, 5);
        same(alphaStorage1, storage(alphaSlice), "full atom slice returns canonical scalar atom");

        check(MATERIALIZED.get(joinedStorage1) == null, "join starts unmaterialized");
        eq(joined1.hashCode(), "alphagamma".hashCode(), "precomputed String hash");
        check(MATERIALIZED.get(joinedStorage1) == null, "hash does not materialize");
        check(Arrays.equals(joined1.toCharArray(), "alphagamma".toCharArray()), "char[] projection");
        check(MATERIALIZED.get(joinedStorage1) == null, "char[] projection traverses atoms");
        byte[] bytes = joined1.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        check(Arrays.equals(bytes, "alphagamma".getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                "UTF-8 projection");

        String repeated = joined1.repeat(2);
        eq("alphagammaalphagamma", repeated, "repeat contents");
        eq(JOINED, kind(storage(repeated)), "repeat remains MIndex tuple");

        System.out.println("M_INDEX_STRING_INVARIANT_PASS lexicon=" + expectLexicon
                + " alphaKind=" + kind(alphaStorage1)
                + " missKind=" + kind(missStorage1)
                + " joinKind=" + kind(joinedStorage1)
                + " jni=not-required");
    }

    private static Object storage(String value) throws IllegalAccessException {
        Object storage = STRING_MINDEX.get(value);
        if (storage == null) {
            value.length();
            storage = STRING_MINDEX.get(value);
        }
        check(storage != null, "String must have MIndex storage after activation");
        return storage;
    }

    private static byte kind(Object storage) throws IllegalAccessException {
        return KIND.getByte(storage);
    }

    private static Class<?> storageClass() {
        try {
            return Class.forName("java.lang.MIndexString");
        } catch (ClassNotFoundException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }

    private static Field field(Class<?> type, String name) {
        try {
            Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    private static void same(Object expected, Object actual, String label) {
        if (expected != actual) {
            throw new AssertionError(label + " expected same identity");
        }
    }

    private static void eq(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void eq(int expected, int actual, String label) {
        if (expected != actual) {
            throw new AssertionError(label + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
}
