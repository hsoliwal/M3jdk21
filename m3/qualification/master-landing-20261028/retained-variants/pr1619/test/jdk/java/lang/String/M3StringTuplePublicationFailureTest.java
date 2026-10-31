/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * GPLv2 with the Classpath exception.
 */

/*
 * @test
 * @summary Tuple publication failures return their cache count and preserve shared composition
 * @modules java.base/java.lang:open
 * @run main/othervm -Xmx128m -XX:+UnlockExperimentalVMOptions -XX:-UseM3StringStorage M3StringTuplePublicationFailureTest
 * @run main/othervm -Xmx128m -XX:+UnlockExperimentalVMOptions -XX:-UseM3StringStorage -XX:-CompactStrings M3StringTuplePublicationFailureTest
 * @run main/othervm -Xmx128m -XX:+UnlockExperimentalVMOptions -XX:-UseM3StringStorage -Dm3.string.pool.maxTuples=0 M3StringTuplePublicationFailureTest noRetention
 * @run main/othervm -Xmx128m -XX:+UnlockExperimentalVMOptions -XX:-UseM3StringStorage -XX:-CompactStrings -Dm3.string.pool.maxTuples=0 M3StringTuplePublicationFailureTest noRetention
 */

import java.lang.ref.Reference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Invokes the real tuple pool. Only a test-created empty bucket's add operation
 * throws. Native scalar children stay strongly reachable throughout the test.
 * String auto-admission is disabled to keep unrelated bookkeeping out of the
 * observed counters; installed String/JNI ownership has its own integration test.
 */
public final class M3StringTuplePublicationFailureTest {
    private static final OutOfMemoryError FAILURE =
            new OutOfMemoryError("deliberate test tuple publication failure");
    private static long checks;
    private static Method concat;
    private static Method owner;
    private static Method charAt;
    private static Method retainedTuples;
    private static Method nativeBytes;
    private static Field leftChild;
    private static Field rightChild;

    private static final class FailingValues extends ArrayList<Object> {
        private static final long serialVersionUID = 1L;
        transient Reference<?> captured;
        transient Object capturedOwner;

        @Override
        public boolean add(Object value) {
            captured = (Reference<?>) value;
            capturedOwner = captured.get();
            throw FAILURE;
        }
    }

    public static void main(String[] args) throws Exception {
        boolean noRetention = args.length == 1 && args[0].equals("noRetention");
        Class<?> pool = Class.forName("java.lang.M3StringPool");
        Class<?> value = Class.forName("java.lang.M3String");
        Class<?> tuple = Class.forName("java.lang.M3StringTuple");
        Class<?> bucketType = Class.forName("java.lang.M3StringPool$TupleBucket");
        Method internChars = method(pool, "internChars", char[].class, int.class, int.class);
        concat = method(pool, "concat", value, value);
        owner = method(value, "owner");
        charAt = method(value, "charAt", int.class);
        retainedTuples = method(pool, "retainedTuples");
        nativeBytes = method(pool, "localNativeBytes");
        leftChild = field(tuple, "left");
        rightChild = field(tuple, "right");
        long limit = field(pool, "MAX_RETAINED_TUPLES").getLong(null);
        check(noRetention ? limit == 0 : limit > 0, "requested tuple retention mode");

        char[] leftUnits = new char[19];
        char[] rightUnits = new char[23];
        for (int i = 0; i < leftUnits.length; i++) leftUnits[i] = (char) (0x201 + i * 29);
        for (int i = 0; i < rightUnits.length; i++) rightUnits[i] = (char) (0x401 + i * 31);
        leftUnits[leftUnits.length - 1] = '\ud83d';
        rightUnits[0] = '\ude42';
        rightUnits[7] = 0;
        rightUnits[8] = '\ud800';
        char[] expected = new char[leftUnits.length + rightUnits.length];
        System.arraycopy(leftUnits, 0, expected, 0, leftUnits.length);
        System.arraycopy(rightUnits, 0, expected, leftUnits.length, rightUnits.length);
        Object left = internChars.invoke(null, leftUnits, 0, leftUnits.length);
        Object right = internChars.invoke(null, rightUnits, 0, rightUnits.length);
        check(left != null && right != null, "real scalar owners admitted");
        long nativeBefore = (long) nativeBytes.invoke(null);
        long countBefore = (long) retainedTuples.invoke(null);

        int hash = 0;
        for (char unit : expected) hash = 31 * hash + unit;
        Method routeKey = method(pool, "tupleRouteKey", value, value,
                int.class, int.class, byte.class);
        Long route = (Long) routeKey.invoke(null, left, right, hash, expected.length, (byte) 1);
        Constructor<?> constructor = bucketType.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object bucket = constructor.newInstance();
        FailingValues failing = new FailingValues();
        field(bucketType, "values").set(bucket, failing);
        Object map = field(pool, "TUPLES").get(null);
        Method put = ConcurrentHashMap.class.getMethod("putIfAbsent", Object.class, Object.class);
        Method get = ConcurrentHashMap.class.getMethod("get", Object.class);
        Method remove = ConcurrentHashMap.class.getMethod("remove", Object.class, Object.class);
        check(put.invoke(map, route, bucket) == null, "isolated tuple route is initially absent");

        Throwable observed = null;
        Object result = null;
        try {
            result = concat.invoke(null, left, right);
        } catch (InvocationTargetException failure) {
            observed = failure.getCause();
        }
        long countAfter = (long) retainedTuples.invoke(null);
        try {
            check(get.invoke(map, route) == null, "empty failed or capped tuple bucket retired");
            if (noRetention) {
                check(observed == null && result != null, "zero-cap composition succeeds");
                check(failing.captured == null, "zero cap never publishes a cleanup record");
                check(countAfter == countBefore, "zero cap retains no tuple count");
                verifyShared(result, left, right, expected);
                Object repeated = concat.invoke(null, left, right);
                check(owner.invoke(repeated) != owner.invoke(result), "zero cap does not cache tuples");
                verifyShared(repeated, left, right, expected);
                check((long) retainedTuples.invoke(null) == countBefore, "zero-cap repeated count");
            } else {
                check(observed == FAILURE, "original tuple publication failure preserved");
                check(failing.captured != null && failing.capturedOwner != null,
                        "real tuple cleanup record reached publication");
                check(countAfter == countBefore, "tuple count returned on failed publication");
                check(failing.captured.enqueue(), "failed cleanup record can arrive later");
                check((long) retainedTuples.invoke(null) == countBefore,
                        "unpublished queued record cannot decrement a second time");
                check(!failing.captured.enqueue(), "cleanup record cannot be requeued");
                check((long) retainedTuples.invoke(null) == countBefore,
                        "repeated queue drain preserves count");
                Object retried = concat.invoke(null, left, right);
                Object repeated = concat.invoke(null, left, right);
                verifyShared(retried, left, right, expected);
                verifyShared(repeated, left, right, expected);
                check(owner.invoke(retried) == owner.invoke(repeated),
                        "successful retry reuses the canonical tuple");
                check((long) retainedTuples.invoke(null) == countBefore + 1,
                        "successful publication counts exactly one tuple");
            }
            check((long) nativeBytes.invoke(null) == nativeBefore,
                    "tuple construction does not allocate a second native payload");
        } finally {
            remove.invoke(map, route, bucket);
            Reference.reachabilityFence(left);
            Reference.reachabilityFence(right);
        }
        System.out.println("M3_STRING_TUPLE_PUBLICATION_FAILURE_PASS checks=" + checks
                + " noRetention=" + noRetention);
    }

    private static void verifyShared(Object value, Object left, Object right,
            char[] expected) throws Exception {
        Object tuple = owner.invoke(value);
        check(leftChild.get(tuple) == left && rightChild.get(tuple) == right,
                "tuple retains exactly its original child coordinates");
        check(owner.invoke(leftChild.get(tuple)) == owner.invoke(left)
                && owner.invoke(rightChild.get(tuple)) == owner.invoke(right),
                "composition shares original native payload owners");
        for (int i = 0; i < expected.length; i++) {
            check((char) charAt.invoke(value, i) == expected[i], "independent composed UTF-16 unit");
        }
    }

    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static Method method(Class<?> type, String name, Class<?>... parameters) throws Exception {
        Method method = type.getDeclaredMethod(name, parameters);
        method.setAccessible(true);
        return method;
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
