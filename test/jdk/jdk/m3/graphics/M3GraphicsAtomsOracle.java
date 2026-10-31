/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

import java.io.File;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Both sides of the jdk.m3.graphics atom tests (support of m3/tooling/skija-absorb/tools/gen_atom_tests.py).
 * The M3 side is the absorbed module: jdk.m3.graphics in the image, or its classes on the class path. The oracle side
 * is the donor, HumbleUI Skija 0.143.18 and Types 0.2.0, loaded from the jars named by
 * {@code -Dm3.graphics.oracle=<skija-shared jar><path separator><types jar>} in a class loader of its own whose parent
 * is the platform loader, so no donor class is visible to the M3 side and no M3 class to the oracle.
 *
 * <p>Atoms and descriptors use short donor names: {@code skija.ColorType}, {@code types.Rect} and descriptor tokens
 * {@code Lskija/paragraph/TextStyle;} stand for {@code io.github.humbleui.skija.ColorType} and so on. The M3 name of
 * a donor type follows the leaf-1 rename (m3/tooling/skija-absorb/leaf1/TYPE_RENAME_L1.tsv):
 * {@code io.github.humbleui.skija[.x].T} and {@code io.github.humbleui.types.T} become {@code jdk.m3.graphics[.x].M3T}.
 * Classes are loaded without initialization; a class initializes when a test first uses it.
 */
final class M3GraphicsAtomsOracle {
    /** The two implementations under comparison. */
    enum Side { M3, ORACLE }

    static final String ORACLE_PROPERTY = "m3.graphics.oracle";
    static final String CONSTRUCTOR = "<init>";
    static final char ARRAY = '[';
    static final char OBJECT = 'L';
    private static final String DONOR_ROOT = "io.github.humbleui.";
    private static final String DONOR_ROOT_PATH = "io/github/humbleui/";
    private static final String SHORT_SKIJA = "skija";
    private static final String SHORT_TYPES = "types";
    private static final String M3_PACKAGE = "jdk.m3.graphics";
    private static final String M3_PREFIX = "M3";
    private static final String BLANK = "";
    private static final char DOT = '.';
    private static final char SLASH = '/';
    private static final char OBJECT_END = ';';
    private static final char PARAMS_END = ')';
    private static final int FIRST_PARAM = 1;
    private static final String MISSING_TYPE = "type not found: ";
    private static final String BAD_JAR = "oracle jar path not usable: ";
    private static final String NO_ORACLE = "oracle not configured: -D" + ORACLE_PROPERTY;
    private static final Map<Character, Class<?>> PRIMITIVES = Map.of('Z', boolean.class, 'B', byte.class,
            'C', char.class, 'S', short.class, 'I', int.class, 'J', long.class, 'F', float.class, 'D', double.class);
    private static final Optional<ClassLoader> LOADER = loader();

    private M3GraphicsAtomsOracle() {}

    /** True when the oracle jars are configured; the tests abort (JUnit assumption) otherwise. */
    static boolean present() {
        return LOADER.isPresent();
    }

    /** The full donor binary name of a short donor name ({@code types.Rect}). */
    static String donorName(String shortName) {
        return DONOR_ROOT + shortName;
    }

    /** The short donor name of a donor binary name ({@code io.github.humbleui.types.Rect} is {@code types.Rect}). */
    static String shortName(String donorBinaryName) {
        return donorBinaryName.substring(DONOR_ROOT.length());
    }

    /** True when a binary name belongs to the donor (Skija or Types). */
    static boolean isDonor(String binaryName) {
        return binaryName.startsWith(DONOR_ROOT);
    }

    /** The M3 binary name of a donor binary name, by the leaf-1 rename rule (nested names follow their owner). */
    static String m3Name(String donor) {
        int dot = donor.lastIndexOf(DOT);
        String pkg = donor.substring(DONOR_ROOT.length(), dot);
        String sub = pkg.startsWith(SHORT_SKIJA) ? pkg.substring(SHORT_SKIJA.length()) : BLANK;
        return M3_PACKAGE + sub + DOT + M3_PREFIX + donor.substring(dot + 1);
    }

    /** The class of a short donor name on one side, not initialized. */
    static Class<?> atom(String shortName, Side side) {
        return named(donorName(shortName), side);
    }

    /** The class of a donor binary name (or of a JDK name) on one side, not initialized. */
    static Class<?> named(String binaryName, Side side) {
        boolean donor = isDonor(binaryName);
        String name = donor && side == Side.M3 ? m3Name(binaryName) : binaryName;
        ClassLoader loader = donor && side == Side.ORACLE ? oracleLoader()
                : M3GraphicsAtomsOracle.class.getClassLoader();
        try {
            return Class.forName(name, false, loader);
        } catch (ClassNotFoundException e) {
            throw new AssertionError(MISSING_TYPE + name, e);
        }
    }

    /** The parameter tokens of a descriptor such as {@code (FLjava/lang/String;[ILtypes/Point;)}. */
    static List<String> params(String descriptor) {
        List<String> tokens = new ArrayList<>();
        int i = FIRST_PARAM;
        while (descriptor.charAt(i) != PARAMS_END) {
            int end = tokenEnd(descriptor, i);
            tokens.add(descriptor.substring(i, end));
            i = end;
        }
        return tokens;
    }

    /** The binary name of an object token ({@code Ltypes/Point;} is {@code io.github.humbleui.types.Point}). */
    static String binaryName(String objectToken) {
        String internal = objectToken.substring(FIRST_PARAM, objectToken.length() - 1);
        boolean donor = internal.startsWith(SHORT_SKIJA + SLASH) || internal.startsWith(SHORT_TYPES + SLASH);
        return (donor ? DONOR_ROOT_PATH + internal : internal).replace(SLASH, DOT);
    }

    /** The class of a parameter token on one side. */
    static Class<?> type(String token, Side side) {
        char c = token.charAt(0);
        if (c == ARRAY) {
            return type(token.substring(1), side).arrayType();
        }
        return c == OBJECT ? named(binaryName(token), side) : PRIMITIVES.get(c);
    }

    /** The parameter classes of a descriptor on one side. */
    static Class<?>[] types(String descriptor, Side side) {
        return params(descriptor).stream().map(t -> type(t, side)).toArray(Class<?>[]::new);
    }

    /**
     * The names of the public static final fields declared by a type whose type is the type itself (FontStyle.NORMAL,
     * SamplingMode.DEFAULT), sorted: the static instances that serve as receivers and values. Empty for an enum.
     */
    static List<String> instances(Class<?> type) {
        return type.isEnum() ? List.of() : Arrays.stream(type.getDeclaredFields())
                .filter(f -> f.getType() == type && isPublicStaticFinal(f))
                .map(Field::getName).sorted().toList();
    }

    /** A public member of one side: constructor, method or field by kind, name and parameter classes. */
    static AccessibleObject member(Class<?> owner, M3GraphicsAtoms.Kind kind, String name, Class<?>[] params)
            throws ReflectiveOperationException {
        return switch (kind) {
            case CTOR -> owner.getConstructor(params);
            case METHOD, STATIC_METHOD -> owner.getMethod(name, params);
            case FIELD, STATIC_FIELD -> owner.getField(name);
        };
    }

    /** Calls a member found by {@link #member} (a field is read); exceptions of the callee stay wrapped. */
    static Object call(AccessibleObject target, M3GraphicsAtoms.Kind kind, Object receiver, Object[] args)
            throws ReflectiveOperationException {
        return switch (kind) {
            case CTOR -> ((Constructor<?>) target).newInstance(args);
            case METHOD, STATIC_METHOD -> ((Method) target).invoke(receiver, args);
            case FIELD, STATIC_FIELD -> ((Field) target).get(receiver);
        };
    }

    private static boolean isPublicStaticFinal(Field f) {
        int m = f.getModifiers();
        return Modifier.isPublic(m) && Modifier.isStatic(m) && Modifier.isFinal(m);
    }

    private static int tokenEnd(String d, int start) {
        int i = start;
        while (d.charAt(i) == ARRAY) {
            i++;
        }
        return d.charAt(i) == OBJECT ? d.indexOf(OBJECT_END, i) + 1 : i + 1;
    }

    private static ClassLoader oracleLoader() {
        return LOADER.orElseThrow(() -> new IllegalStateException(NO_ORACLE));
    }

    private static Optional<ClassLoader> loader() {
        String jars = System.getProperty(ORACLE_PROPERTY, BLANK);
        if (jars.isBlank()) {
            return Optional.empty();
        }
        URL[] urls = Arrays.stream(jars.split(File.pathSeparator)).map(M3GraphicsAtomsOracle::url).toArray(URL[]::new);
        return Optional.of(new URLClassLoader(urls, ClassLoader.getPlatformClassLoader()));
    }

    private static URL url(String jar) {
        try {
            return Path.of(jar).toUri().toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(BAD_JAR + jar, e);
        }
    }
}
