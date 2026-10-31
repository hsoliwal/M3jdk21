/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

/**
 * Deterministic arguments and receivers of the jdk.m3.graphics atom tests. A case is first generated as a logical
 * value, independent of the side, then materialized once per side, so the M3 call and the oracle call receive equal
 * but distinct objects. Primitives, Strings and array lengths come from M3GraphicsAtomsPools; enums are constants by
 * ordinal; values of donor value types are static instances or are built by their recipe (M3GraphicsAtomsRecipes);
 * an {@code Object} parameter (equals) gets a value of the atom itself, equal to the receiver in every other case;
 * any other reference parameter is null.
 */
final class M3GraphicsAtomsValues {
    /** A side-independent argument or receiver. */
    sealed interface Logical permits Plain, Constant, Instance, Built, Elements, Absent {}

    /** A boxed primitive or a String. */
    record Plain(Object value) implements Logical {}

    /** An enum constant by ordinal; the type is a short donor name. */
    record Constant(String type, int ordinal) implements Logical {}

    /** A static instance: a public static final field of the type itself. */
    record Instance(String type, String field) implements Logical {}

    /** A value built by the type's recipe (M3GraphicsAtomsRecipes) from the arguments of a round. */
    record Built(String type, int round) implements Logical {}

    /** An array of logical elements; the component is a descriptor token. */
    record Elements(String component, List<Logical> items) implements Logical {}

    /** null: the parameter type has no value without natives, or is a JDK type outside the generated kinds. */
    record Absent() implements Logical {}

    /** What generation depends on: the atom under test, the member seed and, for enums, the constant names. */
    record Context(String atom, long seed, List<String> names) {}

    static final Absent ABSENT = new Absent();
    private static final int ELEMENT_STRIDE = 7;
    private static final int INSTANCE_PERIOD = 3;
    private static final int SELF_PARITY = 2;
    private static final int BUILD_ATTEMPTS = 8;
    private static final int RETRY_STRIDE = 7919;
    private static final String KEY_SEPARATOR = "#";
    private static final String RECIPE_SALT = "recipe";
    private static final String STRING = "java.lang.String";
    private static final String CHAR_SEQUENCE = "java.lang.CharSequence";
    private static final String OBJECT = "java.lang.Object";
    private static final Map<String, List<String>> INSTANCES = new ConcurrentHashMap<>();
    private static final Map<String, List<String>> NAMES = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> KINDS = new ConcurrentHashMap<>();
    private static final Map<String, Integer> BUILDABLE = new ConcurrentHashMap<>();

    private M3GraphicsAtomsValues() {}

    /** The seed of a member key (see M3GraphicsAtomsPools.seed). */
    static long seed(String key) {
        return M3GraphicsAtomsPools.seed(key);
    }

    /** The logical argument of a parameter token for one round and position. */
    static Logical arg(String token, Context c, int round, int position) {
        char k = token.charAt(0);
        if (k == M3GraphicsAtomsOracle.ARRAY) {
            return elements(token.substring(1), c, round, position);
        }
        if (k != M3GraphicsAtomsOracle.OBJECT) {
            return new Plain(M3GraphicsAtomsPools.primitive(k, c.seed(), round, position));
        }
        return reference(M3GraphicsAtomsOracle.binaryName(token), c, round, position);
    }

    /** The receiver of case {@code index}: an enum constant, a static instance or a built value. */
    static Logical receiver(String atom, int index) {
        if (isEnum(atom)) {
            return new Constant(atom, index % constants(atom));
        }
        List<String> inst = instances(atom);
        boolean recipe = M3GraphicsAtomsRecipes.recipe(atom).isPresent();
        if (!inst.isEmpty() && (index < inst.size() || !recipe)) {
            return new Instance(atom, inst.get(index % inst.size()));
        }
        return recipe ? buildable(atom, index) : ABSENT;
    }

    /** The value of a logical argument on one side. */
    static Object materialize(Logical l, M3GraphicsAtomsOracle.Side side) throws ReflectiveOperationException {
        return switch (l) {
            case Plain p -> p.value();
            case Constant c -> M3GraphicsAtomsOracle.atom(c.type(), side).getEnumConstants()[c.ordinal()];
            case Instance i -> M3GraphicsAtomsOracle.atom(i.type(), side).getField(i.field()).get(null);
            case Built b -> build(b, side);
            case Elements e -> array(e, side);
            case Absent a -> null;
        };
    }

    /** True when the atom is an enum (every Skija enum initializes without natives; the generator checks it). */
    static boolean isEnum(String atom) {
        return KINDS.computeIfAbsent(atom, a -> M3GraphicsAtomsOracle.atom(a, M3GraphicsAtomsOracle.Side.M3).isEnum());
    }

    /** The number of constants of an enum atom, 0 for other atoms (M3 side; values() compares both sides). */
    static int constants(String atom) {
        return names(atom).size();
    }

    /** The enum constant names of an atom (String arguments of its static methods); empty for other atoms. */
    static List<String> names(String atom) {
        return NAMES.computeIfAbsent(atom, M3GraphicsAtomsValues::enumNames);
    }

    private static List<String> enumNames(String atom) {
        if (!isEnum(atom)) {
            return List.of();
        }
        Object[] all = M3GraphicsAtomsOracle.atom(atom, M3GraphicsAtomsOracle.Side.M3).getEnumConstants();
        return Arrays.stream(all).map(e -> ((Enum<?>) e).name()).toList();
    }

    private static List<String> instances(String type) {
        return INSTANCES.computeIfAbsent(type,
                t -> M3GraphicsAtomsOracle.instances(M3GraphicsAtomsOracle.atom(t, M3GraphicsAtomsOracle.Side.M3)));
    }

    /**
     * A built value whose construction the oracle accepts: the round itself, else the first of a few alternative
     * rounds (round + k * RETRY_STRIDE) that builds, else the round itself. Receivers and value arguments then
     * exercise the members instead of the constructor's argument checks; the constructors' own cases still use every
     * round. The choice depends on the oracle only, so it is the same for both sides and every run.
     */
    private static Built buildable(String type, int round) {
        String key = type + KEY_SEPARATOR + round;
        Integer chosen = BUILDABLE.get(key);
        if (chosen == null) {
            chosen = IntStream.range(0, BUILD_ATTEMPTS).map(k -> round + k * RETRY_STRIDE)
                    .filter(r -> builds(type, r)).findFirst().orElse(round);
            BUILDABLE.put(key, chosen);
        }
        return new Built(type, chosen);
    }

    private static boolean builds(String type, int round) {
        try {
            build(new Built(type, round), M3GraphicsAtomsOracle.Side.ORACLE);
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return false;
        }
    }

    private static Logical reference(String binary, Context c, int round, int position) {
        if (STRING.equals(binary) || CHAR_SEQUENCE.equals(binary)) {
            return new Plain(M3GraphicsAtomsPools.string(c.names(), c.seed(), round, position));
        }
        if (OBJECT.equals(binary)) {
            Logical self = receiver(c.atom(), round % SELF_PARITY == 0 ? round : round + 1);
            return self instanceof Absent ? new Plain(c.atom()) : self;
        }
        if (!M3GraphicsAtomsOracle.isDonor(binary)) {
            return ABSENT;
        }
        String type = M3GraphicsAtomsOracle.shortName(binary);
        return isEnum(type) ? new Constant(type, (round + position) % constants(type)) : value(type, round, position);
    }

    private static Logical value(String type, int round, int position) {
        boolean recipe = M3GraphicsAtomsRecipes.recipe(type).isPresent();
        if (!recipe && !M3GraphicsAtomsRecipes.INSTANCE_ONLY.contains(type)) {
            return ABSENT;
        }
        List<String> inst = instances(type);
        int pick = round + position;
        if (!inst.isEmpty() && (!recipe || pick % INSTANCE_PERIOD == 0)) {
            return new Instance(type, inst.get(pick % inst.size()));
        }
        return buildable(type, round * M3GraphicsAtomsPools.POSITION_STRIDE + position);
    }

    private static Logical elements(String component, Context c, int round, int position) {
        boolean values = component.charAt(0) == M3GraphicsAtomsOracle.OBJECT && !isText(component);
        int n = M3GraphicsAtomsPools.arrayLength(values, c.seed(), round, position);
        List<Logical> items = IntStream.range(0, n)
                .mapToObj(k -> arg(component, c, round, position * ELEMENT_STRIDE + k + 1)).toList();
        return new Elements(component, items);
    }

    private static boolean isText(String token) {
        String binary = M3GraphicsAtomsOracle.binaryName(token);
        return STRING.equals(binary) || CHAR_SEQUENCE.equals(binary);
    }

    private static Object build(Built b, M3GraphicsAtomsOracle.Side side) throws ReflectiveOperationException {
        M3GraphicsAtomsRecipes.Recipe r = M3GraphicsAtomsRecipes.recipe(b.type()).orElseThrow();
        Context c = new Context(b.type(), seed(RECIPE_SALT + b.type()), List.of());
        List<String> params = M3GraphicsAtomsOracle.params(r.descriptor());
        Object[] args = new Object[params.size()];
        for (int p = 0; p < args.length; p++) {
            args[p] = materialize(arg(params.get(p), c, b.round(), p), side);
        }
        Class<?> owner = M3GraphicsAtomsOracle.atom(b.type(), side);
        Class<?>[] types = M3GraphicsAtomsOracle.types(r.descriptor(), side);
        return M3GraphicsAtomsOracle.CONSTRUCTOR.equals(r.name()) ? owner.getConstructor(types).newInstance(args)
                : owner.getMethod(r.name(), types).invoke(null, args);
    }

    private static Object array(Elements e, M3GraphicsAtomsOracle.Side side) throws ReflectiveOperationException {
        Object a = Array.newInstance(M3GraphicsAtomsOracle.type(e.component(), side), e.items().size());
        for (int k = 0; k < e.items().size(); k++) {
            Array.set(a, k, materialize(e.items().get(k), side));
        }
        return a;
    }
}
