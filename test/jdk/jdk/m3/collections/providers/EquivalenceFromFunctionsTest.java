/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary Paired functional equality and hashing preserve existing equivalence contracts
 * @modules jdk.m3.collections/jdk.m3.collections.guava.base
 *          jdk.m3.collections/jdk.m3.collections.eclipse.api.block
 * @run main EquivalenceFromFunctionsTest
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.ToIntFunction;
import jdk.m3.collections.eclipse.api.block.HashingStrategy;
import jdk.m3.collections.guava.base.Equivalence;
import jdk.m3.collections.guava.base.Function;

public class EquivalenceFromFunctionsTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        nullValidationAndCallbackOrder();
        callbackFailures();
        jdkValuePolicies();
        primitiveArrays();
        wrappersAndCollections();
        composition();
        serializationContracts();
        hashingStrategyInteroperability();
        invalidCallbacksRemainCallerErrors();
        System.out.println("EQUIVALENCE_FROM_FUNCTIONS_PASS checks=" + checks);
    }

    private static void nullValidationAndCallbackOrder() {
        BiPredicate<Object, Object> equality = Objects::equals;
        ToIntFunction<Object> hash = Objects::hashCode;
        check("equality".equals(expect(NullPointerException.class,
                () -> Equivalence.fromFunctions(null, hash)).getMessage()),
                "null equality parameter");
        check("hashFunction".equals(expect(NullPointerException.class,
                () -> Equivalence.fromFunctions(equality, null)).getMessage()),
                "null hash parameter");
        check("equality".equals(expect(NullPointerException.class,
                () -> Equivalence.fromFunctions(null, null)).getMessage()),
                "equality validation precedes hash validation");

        List<Object> calls = new ArrayList<>();
        Equivalence<String> policy = Equivalence.fromFunctions((left, right) -> {
            calls.add(left);
            calls.add(right);
            return left.length() == right.length();
        }, value -> {
            calls.add(value);
            return value.length();
        });
        check(calls.isEmpty(), "construction evaluates no callback");
        String left = new String("left");
        String right = new String("four");
        check(policy.equivalent(left, left), "same reference is equivalent");
        check(policy.equivalent(null, null), "two nulls are equivalent");
        check(!policy.equivalent(null, left), "left null is distinct");
        check(!policy.equivalent(left, null), "right null is distinct");
        check(policy.hash(null) == 0, "null hash is zero");
        check(calls.isEmpty(), "short circuits invoke no callback");
        check(policy.equivalent(left, right), "delegated comparison result");
        check(calls.size() == 2 && calls.get(0) == left && calls.get(1) == right,
                "one comparison preserves argument order and identity");
        calls.clear();
        check(policy.hash(left) == 4, "delegated primitive hash result");
        check(calls.size() == 1 && calls.get(0) == left,
                "hash calls only the hasher once");

        // Pretyped callbacks accepting Object must support a narrower strategy.
        Equivalence<String> contravariant = Equivalence.fromFunctions(equality, hash);
        check(contravariant.equivalent(new String("value"), new String("value")),
                "contravariant callback binding");
        check(contravariant.hash("value") == "value".hashCode(),
                "contravariant primitive hash binding");
        BiPredicate<String, String> predicateView = contravariant;
        check(predicateView.test("value", new String("value")),
                "existing BiPredicate bridge");
    }

    private static void callbackFailures() {
        RuntimeException comparisonFailure = new IllegalStateException("comparison marker");
        AssertionError hashFailure = new AssertionError("hash marker");
        Equivalence<Object> policy = Equivalence.fromFunctions((left, right) -> {
            throw comparisonFailure;
        }, value -> {
            throw hashFailure;
        });
        Object left = new Object();
        Object right = new Object();
        check(expect(RuntimeException.class, () -> policy.equivalent(left, right))
                == comparisonFailure, "comparison throwable identity");
        check(expect(AssertionError.class, () -> policy.hash(left)) == hashFailure,
                "hash throwable identity");
        check(policy.equivalent(left, left), "reference shortcut bypasses failing callback");
        check(policy.equivalent(null, null), "null shortcut bypasses failing callback");
        check(!policy.equivalent(left, null), "one-null shortcut bypasses failing callback");
        check(policy.hash(null) == 0, "null hash bypasses failing callback");
    }

    private static void jdkValuePolicies() {
        Equivalence<Object> policy = Equivalence.fromFunctions(Objects::equals, Objects::hashCode);
        Object[] values = {
                null, "", new String("same"), new String("same"), "different",
                Integer.valueOf(0), Integer.valueOf(1), Long.valueOf(1),
                Double.valueOf(Double.NaN),
                Double.valueOf(Double.longBitsToDouble(0x7ff8000000000001L)),
                Double.valueOf(+0.0d), Double.valueOf(-0.0d),
                Float.valueOf(Float.NaN),
                Float.valueOf(Float.intBitsToFloat(0x7fc00001)),
                Float.valueOf(+0.0f), Float.valueOf(-0.0f)
        };
        for (Object left : values) {
            check(policy.hash(left) == Objects.hashCode(left), "JDK object hash parity");
            for (Object right : values) {
                check(policy.equivalent(left, right) == Objects.equals(left, right),
                        "JDK object equality parity");
            }
        }
        assertLaws(policy, values);
        check(policy.equivalent(values[8], values[9]), "Double NaN payloads compare equally");
        check(!policy.equivalent(values[10], values[11]), "Double signed zeros remain distinct");
        check(policy.equivalent(values[12], values[13]), "Float NaN payloads compare equally");
        check(!policy.equivalent(values[14], values[15]), "Float signed zeros remain distinct");
    }

    private static void primitiveArrays() {
        Equivalence<int[]> integers = Equivalence.fromFunctions(Arrays::equals, Arrays::hashCode);
        int[][] samples = {null, {}, {1}, {1}, {1, 2}, {2, 1}};
        assertLaws(integers, samples);
        for (int[] left : samples) {
            check(integers.hash(left) == Arrays.hashCode(left), "primitive array hash parity");
            for (int[] right : samples) {
                check(integers.equivalent(left, right) == Arrays.equals(left, right),
                        "primitive array equality parity");
            }
        }
        Equivalence<double[]> doubles = Equivalence.fromFunctions(Arrays::equals, Arrays::hashCode);
        double[] nan = {Double.NaN};
        double[] otherNan = {Double.longBitsToDouble(0x7ff8000000000001L)};
        check(doubles.equivalent(nan, otherNan), "array NaN equality policy");
        check(doubles.hash(nan) == doubles.hash(otherNan), "array NaN hash policy");
        check(!doubles.equivalent(new double[] {+0.0}, new double[] {-0.0}),
                "array signed-zero equality policy");
        check(!Equivalence.equals().equivalent(new int[] {1}, new int[] {1}),
                "ordinary array reference equality remains unchanged");
    }

    private static void wrappersAndCollections() {
        BiPredicate<String, String> sameLength = (left, right) -> left.length() == right.length();
        ToIntFunction<String> length = String::length;
        Equivalence<String> policy = Equivalence.fromFunctions(sameLength, length);
        Equivalence<String> otherPolicy = Equivalence.fromFunctions(sameLength, length);
        String original = new String("aa");
        Equivalence.Wrapper<String> wrapped = policy.wrap(original);
        check(wrapped.get() == original, "wrapper preserves value reference");
        check(wrapped.equals(policy.wrap("bb")), "wrapper follows selected equivalence");
        check(wrapped.hashCode() == policy.wrap("bb").hashCode(), "wrapper paired hash");
        check(!wrapped.equals(original), "wrapper differs from raw value");
        check(!original.equals(wrapped), "raw value differs from wrapper");
        check(policy != otherPolicy && !policy.equals(otherPolicy), "fresh strategy identity");
        check(!wrapped.equals(otherPolicy.wrap(original)), "strategy identity isolates wrappers");
        check(!policy.wrap(null).equals(otherPolicy.wrap(null)), "null wrappers keep policy boundary");
        check(policy.wrap(null).equals(policy.wrap(null)), "same-policy null wrappers");
        check(policy.wrap(null).hashCode() == 0, "null wrapper hash");

        Map<Equivalence.Wrapper<String>, String> map = new HashMap<>();
        map.put(wrapped, "first");
        check("first".equals(map.put(policy.wrap("zz"), "replacement")),
                "equivalent key replaces mapping");
        map.put(policy.wrap(null), "null value");
        map.put(policy.wrap("long"), "long value");
        check(map.size() == 3, "map retains equivalence classes");
        check("replacement".equals(map.get(policy.wrap("xy"))), "equivalent key lookup");
        check("null value".equals(map.get(policy.wrap(null))), "null wrapper key lookup");
        check(!map.containsKey(otherPolicy.wrap("xy")), "foreign policy key rejected");

        Equivalence<String> collisions = Equivalence.fromFunctions(Objects::equals, value -> 7);
        Set<Equivalence.Wrapper<String>> set = new HashSet<>();
        set.add(collisions.wrap("alpha"));
        set.add(collisions.wrap("beta"));
        set.add(collisions.wrap(new String("alpha")));
        check(set.size() == 2, "hash collisions do not establish equality");
        check(set.remove(collisions.wrap(new String("alpha"))), "colliding key removal");
        check(set.contains(collisions.wrap("beta")), "colliding unequal key survives");
    }

    private static void composition() {
        Equivalence<String> length = Equivalence.fromFunctions(
                (left, right) -> left.length() == right.length(), String::length);
        Equivalence<Person> names = length.onResultOf(Person::name);
        Person first = new Person("aa", 1);
        Person second = new Person("bb", 2);
        check(names.equivalent(first, second), "existing projection composes with callback strategy");
        check(names.hash(first) == names.hash(second), "projection preserves hash relation");
        Person unnamed = new Person(null, 3);
        check(names.equivalent(unnamed, new Person(null, 4)), "projection accepts null results");
        check(names.hash(unnamed) == 0, "null projection result hash");
        check(!names.equivalent(null, unnamed), "null source is distinct from null projection");
        check(length.equivalentTo("xx").apply("yy"), "existing bound predicate");
        check(!length.equivalentTo("xx").apply(null), "bound predicate null semantics");

        Equivalence<Iterable<String>> sequences = length.pairwise();
        List<String> left = Arrays.asList("a", null, "bb");
        List<String> right = Arrays.asList("z", null, "cc");
        check(sequences.equivalent(left, right), "pairwise composition with null elements");
        check(sequences.hash(left) == sequences.hash(right), "pairwise hash relation");
        check(!sequences.equivalent(left, Arrays.asList("zz", null, "cc")),
                "pairwise element mismatch");
        check(!sequences.equivalent(left, Arrays.asList("a", null)), "pairwise length mismatch");
        check(!sequences.equivalent(List.of(), List.of("")), "empty differs from singleton");
        check(sequences.equivalent(null, null) && sequences.hash(null) == 0,
                "pairwise null container semantics");
    }

    private static void serializationContracts() throws Exception {
        check(roundTrip(Equivalence.equals()) == Equivalence.equals(), "legacy equals singleton");
        check(roundTrip(Equivalence.identity()) == Equivalence.identity(), "legacy identity singleton");
        Equivalence.Wrapper<String> ordinary = Equivalence.equals().wrap("payload");
        check(ordinary.equals(roundTrip(ordinary)), "legacy value wrapper round trip");
        check(ordinary.hashCode() == roundTrip(ordinary).hashCode(), "legacy wrapper hash round trip");
        Equivalence.Wrapper<Object> nullIdentity = Equivalence.identity().wrap(null);
        check(nullIdentity.equals(roundTrip(nullIdentity)), "legacy null identity wrapper round trip");
        Equivalence<Iterable<String>> pairwise = Equivalence.equals().pairwise();
        Equivalence<Iterable<String>> restoredPairwise = roundTrip(pairwise);
        check(restoredPairwise.equivalent(List.of("a"), List.of(new String("a"))),
                "legacy pairwise serialization");
        check(restoredPairwise.hash(List.of("a")) == pairwise.hash(List.of("a")),
                "legacy serialized pairwise hash");
        Equivalence<String> projection = Equivalence.equals().onResultOf(LengthFunction.INSTANCE);
        Equivalence<String> restoredProjection = roundTrip(projection);
        check(restoredProjection.equivalent("aa", "bb"), "legacy projection serialization");
        check(restoredProjection.hash("aa") == projection.hash("bb"),
                "legacy serialized projection hash");

        Equivalence<String> callbacks = Equivalence.fromFunctions(Objects::equals, String::hashCode);
        check(!(callbacks instanceof Serializable), "new strategy makes no serialization promise");
        expect(NotSerializableException.class, () -> roundTrip(callbacks));
    }

    private static void hashingStrategyInteroperability() throws Exception {
        List<Object> calls = new ArrayList<>();
        BiPredicate<String, String> sameLength = (left, right) -> {
            calls.add(left);
            calls.add(right);
            return left.length() == right.length();
        };
        ToIntFunction<String> length = value -> {
            calls.add(value);
            return value.length();
        };
        Equivalence<String> policy = Equivalence.fromFunctions(sameLength, length);
        HashingStrategy<String> strategy = policy.asHashingStrategy();
        check(calls.isEmpty(), "strategy adaptation evaluates no callback");
        String left = new String("left");
        String right = new String("four");
        String unequal = new String("longer");
        check(strategy.equals(left, left), "strategy preserves reference equality shortcut");
        check(strategy.equals(null, null), "strategy preserves two-null equality");
        check(!strategy.equals(null, left), "strategy preserves left-null distinction");
        check(!strategy.equals(left, null), "strategy preserves right-null distinction");
        check(strategy.computeHashCode(null) == 0, "strategy preserves zero null hash");
        check(calls.isEmpty(), "strategy shortcuts invoke no callback");
        check(strategy.equals(left, right), "strategy delegates true comparison result");
        check(calls.size() == 2 && calls.get(0) == left && calls.get(1) == right,
                "strategy comparison invokes callback once with original argument order");
        calls.clear();
        check(!strategy.equals(left, unequal), "strategy delegates false comparison result");
        check(calls.size() == 2 && calls.get(0) == left && calls.get(1) == unequal,
                "false strategy comparison invokes callback once with original references");
        calls.clear();
        check(strategy.computeHashCode(left) == 4, "strategy delegates primitive hash result");
        check(calls.size() == 1 && calls.get(0) == left,
                "strategy hash invokes only the hasher once");
        calls.clear();

        check(Equivalence.fromHashingStrategy(strategy) == policy,
                "strategy conversion returns original callback policy");
        HashingStrategy<String> samePolicy = policy.asHashingStrategy();
        check(strategy.equals(samePolicy) && samePolicy.equals(strategy),
                "adapters of the same callback policy are equal in both directions");
        check(strategy.hashCode() == samePolicy.hashCode(),
                "equal callback-policy adapters have equal hashes");
        Equivalence<String> otherPolicy = Equivalence.fromFunctions(sameLength, length);
        HashingStrategy<String> otherStrategy = otherPolicy.asHashingStrategy();
        check(!strategy.equals(otherStrategy) && !otherStrategy.equals(strategy),
                "distinct callback policies remain distinct through adapters");
        check(calls.isEmpty(), "conversion and adapter equality evaluate no callbacks");

        RuntimeException comparisonFailure = new IllegalStateException("adapter comparison marker");
        AssertionError hashFailure = new AssertionError("adapter hash marker");
        int[] failureCalls = {0, 0};
        Equivalence<Object> failingPolicy = Equivalence.fromFunctions((a, b) -> {
            failureCalls[0]++;
            throw comparisonFailure;
        }, value -> {
            failureCalls[1]++;
            throw hashFailure;
        });
        HashingStrategy<Object> failingStrategy = failingPolicy.asHashingStrategy();
        Object first = new Object();
        Object second = new Object();
        check(expect(RuntimeException.class, () -> failingStrategy.equals(first, second))
                == comparisonFailure, "strategy comparison throwable identity");
        check(failureCalls[0] == 1 && failureCalls[1] == 0,
                "throwing strategy comparison invokes only equality once");
        check(expect(AssertionError.class, () -> failingStrategy.computeHashCode(first))
                == hashFailure, "strategy hash throwable identity");
        check(failureCalls[0] == 1 && failureCalls[1] == 1,
                "throwing strategy hash invokes only hasher once");
        check(failingStrategy.equals(first, first), "strategy reference shortcut bypasses failure");
        check(failingStrategy.equals(null, null), "strategy null equality bypasses failure");
        check(!failingStrategy.equals(first, null) && !failingStrategy.equals(null, first),
                "strategy one-null comparisons bypass failure");
        check(failingStrategy.computeHashCode(null) == 0, "strategy null hash bypasses failure");
        check(failureCalls[0] == 1 && failureCalls[1] == 1,
                "failing strategy shortcuts invoke no callbacks");

        check(strategy instanceof Serializable, "hashing strategy retains Serializable marker");
        check(!(policy instanceof Serializable), "callback policy remains nonserializable");
        expect(NotSerializableException.class, () -> roundTrip(strategy));
    }

    private static void invalidCallbacksRemainCallerErrors() {
        Equivalence<String> invalid = Equivalence.fromFunctions(
                String::equalsIgnoreCase, String::hashCode);
        check(invalid.equivalent("A", "a"), "factory delegates caller equality policy");
        check(invalid.hash("A") != invalid.hash("a"), "fixture violates paired hashing law");
        // This is an independent test oracle detecting a deliberately bad policy;
        // it is not a claim that fromFunctions validates arbitrary callback laws.
        check(hashLawViolations(invalid, new String[] {null, "A", "a", "b"}) > 0,
                "finite oracle detects incompatible callbacks");
    }

    private static <T> void assertLaws(Equivalence<T> policy, T[] values) {
        for (T left : values) {
            check(policy.equivalent(left, left), "reflexivity");
            check(policy.hash(left) == policy.hash(left), "hash consistency");
            for (T right : values) {
                boolean equal = policy.equivalent(left, right);
                check(equal == policy.equivalent(right, left), "symmetry");
                check(equal == policy.equivalent(left, right), "equality consistency");
                if (equal) {
                    check(policy.hash(left) == policy.hash(right), "equal implies equal hash");
                }
                for (T third : values) {
                    if (equal && policy.equivalent(right, third)) {
                        check(policy.equivalent(left, third), "transitivity");
                    }
                }
            }
        }
        check(hashLawViolations(policy, values) == 0, "finite hash-law corpus");
    }

    private static <T> int hashLawViolations(Equivalence<T> policy, T[] values) {
        int violations = 0;
        for (T left : values) {
            for (T right : values) {
                if (policy.equivalent(left, right) && policy.hash(left) != policy.hash(right)) {
                    violations++;
                }
            }
        }
        return violations;
    }

    @SuppressWarnings("unchecked")
    private static <T> T roundTrip(T value) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(value);
        }
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return (T) input.readObject();
        }
    }

    private static <T extends Throwable> T expect(Class<T> type, ThrowingAction action) {
        try {
            action.run();
        } catch (Throwable thrown) {
            check(type.isInstance(thrown), "expected " + type.getName() + ", got " + thrown);
            return type.cast(thrown);
        }
        throw new AssertionError("Expected " + type.getName());
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
        checks++;
    }

    @FunctionalInterface
    private interface ThrowingAction {
        void run() throws Exception;
    }

    private record Person(String name, int tag) {}

    private enum LengthFunction implements Function<String, Integer> {
        INSTANCE;

        @Override
        public Integer apply(String value) {
            return value.length();
        }
    }
}
