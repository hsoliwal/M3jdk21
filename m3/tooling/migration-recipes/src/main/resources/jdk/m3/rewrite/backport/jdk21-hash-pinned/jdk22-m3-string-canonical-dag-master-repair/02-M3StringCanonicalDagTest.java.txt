/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 *
 * @test
 * @summary Verify M3String tuple identity is parenthesization-independent and composition stays balanced
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringCanonicalDagTest
 */

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class M3StringCanonicalDagTest {
    private static final Field STRING_M3;
    private static final Field M3_OWNER;
    private static final Method M3_FACTS;
    private static long checks;

    static {
        try {
            STRING_M3 = String.class.getDeclaredField("m3");
            STRING_M3.setAccessible(true);
            Class<?> m3 = Class.forName("java.lang.M3String");
            M3_OWNER = m3.getDeclaredField("owner");
            M3_OWNER.setAccessible(true);
            M3_FACTS = m3.getDeclaredMethod("facts");
            M3_FACTS.setAccessible(true);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    public static void main(String[] args) throws Exception {
        String a = fresh("a");
        String b = fresh("b");
        String c = fresh("c");

        String leftAssociated = a.concat(b).concat(c);
        String rightAssociated = a.concat(b.concat(c));
        Object leftBody = body(leftAssociated);
        Object rightBody = body(rightAssociated);
        same(owner(leftBody), owner(rightBody),
                "concat parenthesization must not define canonical tuple identity");
        same(M3_FACTS.invoke(leftBody), M3_FACTS.invoke(rightBody),
                "equivalent tuple coordinates must share internal canonical facts");
        equal("abc", leftAssociated, "left-associated content");
        equal("abc", rightAssociated, "right-associated content");

        String atom = fresh("abcd");
        String prefix = atom.substring(0, 2);
        String suffix = atom.substring(2, 4);
        String splitBoundary = a.concat(prefix).concat(suffix);
        String collapsedBoundary = a.concat(prefix.concat(suffix));
        same(owner(body(splitBoundary)), owner(body(collapsedBoundary)),
                "adjacent ranges of one canonical atom normalize across tuple boundaries");

        String flat = fresh("aabcd");
        check(owner(body(flat)) != owner(body(splitBoundary)),
                "equal UTF-16 spelling from another canonical coordinate structure stays distinct");

        String hashCollisionA = fresh("A").concat(fresh("a"));
        String hashCollisionB = fresh("B").concat(fresh("B"));
        check(hashCollisionA.hashCode() == hashCollisionB.hashCode(),
                "candidate hash collision fixture");
        check(owner(body(hashCollisionA)) != owner(body(hashCollisionB)),
                "candidate hash collisions require exact coordinate verification");
        equal("Aa", hashCollisionA, "hash collision left content");
        equal("BB", hashCollisionB, "hash collision right content");

        String high = fresh("\uD83D");
        String lowAndSpace = fresh("\uDE00\u2003");
        String tail = fresh("Abc  ");
        String factsLeft = high.concat(lowAndSpace).concat(tail);
        String factsRight = high.concat(lowAndSpace.concat(tail));
        String factsFlat = fresh("\uD83D\uDE00\u2003Abc  ");
        same(owner(body(factsLeft)), owner(body(factsRight)),
                "shape-independent composed facts share canonical owner");
        same(M3_FACTS.invoke(body(factsLeft)), M3_FACTS.invoke(body(factsRight)),
                "shape-independent composed facts share one fact bundle");
        equal(factsFlat, factsLeft, "split-surrogate composed content");
        check(factsLeft.hashCode() == factsFlat.hashCode(), "shape-independent Java hash");
        check(factsLeft.codePointCount(0, factsLeft.length())
                        == factsFlat.codePointCount(0, factsFlat.length()),
                "shape-independent code-point count");
        equal(factsFlat.strip(), factsLeft.strip(), "shape-independent strip");
        equal(factsFlat.trim(), factsLeft.trim(), "shape-independent trim");
        check(factsLeft.startsWith(fresh("\uD83D\uDE00")), "shape-independent prefix facts");
        check(factsLeft.endsWith(fresh("c  ")), "shape-independent suffix facts");
        check(factsLeft.indexOf("Abc") == factsFlat.indexOf("Abc"),
                "shape-independent prepared search");
        check(java.util.Arrays.equals(
                        factsLeft.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                        factsFlat.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                "shape-independent UTF-8 projection");

        String chain = fresh("x");
        for (int index = 1; index < 4096; index++) {
            chain = chain.concat(fresh("x"));
        }
        Object chainOwner = owner(body(chain));
        check(chainOwner.getClass().getName().equals("java.lang.M3StringTuple"),
                "long concat chain must remain tuple-backed");
        Field heightField = chainOwner.getClass().getDeclaredField("height");
        heightField.setAccessible(true);
        int height = heightField.getInt(chainOwner);
        check(height <= 20, "4096-piece chain must stay balanced, height=" + height);
        check(chain.length() == 4096, "balanced chain length");
        check(chain.charAt(0) == 'x' && chain.charAt(4095) == 'x',
                "balanced chain endpoint content");

        System.out.println("M3_STRING_CANONICAL_DAG_PASS checks=" + checks + " height=" + height);
    }

    private static String fresh(String value) {
        return new String(value.toCharArray());
    }

    private static Object body(String value) throws Exception {
        value.length();
        Object body = STRING_M3.get(value);
        check(body != null, "String must be admitted to M3");
        return body;
    }

    private static Object owner(Object body) throws Exception {
        return M3_OWNER.get(body);
    }

    private static void same(Object expected, Object actual, String label) {
        check(expected == actual, label);
    }

    private static void equal(String expected, String actual, String label) {
        check(expected.equals(actual), label + " expected=" + expected + " actual=" + actual);
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
}
