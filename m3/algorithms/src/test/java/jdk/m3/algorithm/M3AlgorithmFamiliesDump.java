// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.IntPredicate;

//FLLDCIM: DIFFERENTIAL - driver of recipe/families-20261106/differential.sh; reflective, so one class drives the donor and the relocated families
/**
 * Reflective dump driver of the differential gate (not a test): for every named class it replays seeded inputs,
 * generated from the parameter types of every public static method, and prints one line per case (arguments,
 * result or exception, mutated array arguments of void methods); long values are printed as length and sha256.
 * Classes without a progress-monitor parameter (the shape catalog) are dumped element by element instead.
 *
 * <p>usage: {@code M3AlgorithmFamiliesDump --cases N --seed S [--skip Label.method]... Label=fully.qualified.Name...}
 */
public final class M3AlgorithmFamiliesDump {
    private static final int[] EXTREMES = {
        Integer.MIN_VALUE, Integer.MIN_VALUE + 1, -1, 0, 1, Integer.MAX_VALUE - 1, Integer.MAX_VALUE
    };
    private static final long[] LONG_EXTREMES = {
        Long.MIN_VALUE, Long.MIN_VALUE + 1, -1L, 0L, 1L, Integer.MAX_VALUE, Long.MAX_VALUE - 1, Long.MAX_VALUE
    };
    private static final String[] RPN_TOKENS = {"1", "2", "3", "0", "-4", "7", "+", "-", "*", "/"};
    private static final Set<String> SMALL_ONLY = Set.of(
            "coinChange", "canPartition", "knapsack01", "combinationSumUnique", "subsets", "permutations",
            "wordBreak", "uniquePaths", "nQueens", "generateParentheses", "firstTrue", "longestCommonSubsequence",
            "editDistance");
    private static final Set<String> POSITIVE = Set.of("coinChange", "knapsack01", "combinationSumUnique", "canPartition");
    private static final Set<String> SORTED = Set.of("lowerBound", "upperBound", "sortedTwoSum", "mergeSorted");
    private static final long TIMEOUT_MILLIS = 3_000L;
    private static final int LONG_VALUE = 240;
    private static final int MONITOR_CANCEL_PERCENT = 30;

    private final PrintWriter out;
    private final Set<String> skip;
    private final long seed;
    private ExecutorService executor = newExecutor();
    private long cases;
    private long thrown;
    private long timeouts;

    private M3AlgorithmFamiliesDump(PrintWriter out, Set<String> skip, long seed) {
        this.out = out;
        this.skip = skip;
        this.seed = seed;
    }

    public static void main(String[] args) throws Exception {
        int cases = 100_000;
        long seed = 20261106L;
        Set<String> skip = new LinkedHashSet<>();
        List<String[]> targets = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--cases" -> cases = Integer.parseInt(args[++i]);
                case "--seed" -> seed = Long.parseLong(args[++i]);
                case "--skip" -> skip.add(args[++i]);
                default -> targets.add(args[i].split("=", 2));
            }
        }
        PrintWriter out = new PrintWriter(new BufferedWriter(
                new OutputStreamWriter(System.out, StandardCharsets.UTF_8), 1 << 20));
        M3AlgorithmFamiliesDump dump = new M3AlgorithmFamiliesDump(out, skip, seed);
        for (String[] target : targets) {
            dump.dump(target[0], Class.forName(target[1]), cases);
        }
        out.print("TOTAL families=" + targets.size() + " cases=" + dump.cases + " throws=" + dump.thrown
                + " timeouts=" + dump.timeouts + "\n");
        out.flush();
        System.exit(0);
    }

    private static ExecutorService newExecutor() {
        return Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "dump");
            thread.setDaemon(true);
            return thread;
        });
    }

    private void dump(String label, Class<?> type, int count) throws Exception {
        if (type.isInterface()) {
            out.print("SKIP interface " + label + "\n");
            return;
        }
        List<Method> methods = Arrays.stream(type.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()) && Modifier.isStatic(m.getModifiers()))
                .filter(m -> !m.isSynthetic() && !skip.contains(label + "." + m.getName()))
                .sorted(Comparator.comparing(M3AlgorithmFamiliesDump::signature))
                .toList();
        if (methods.isEmpty()) {
            out.print("SKIP no-public-static " + label + "\n");
            return;
        }
        if (methods.stream().noneMatch(m -> monitorType(m) != null)) {
            for (Method method : methods) {
                dumpData(label, method);
            }
            return;
        }
        for (int c = 0; c < count; c++) {
            Method method = methods.get(c % methods.size());
            SplittableRandom random = new SplittableRandom(seed * 1_000_003L + signature(method).hashCode() * 7919L + c);
            runCase(label, method, c, random);
        }
    }

    /** Catalog-style methods (no monitor): every element of a list result on its own line, every enum constant. */
    private void dumpData(String label, Method method) throws Exception {
        Class<?>[] params = method.getParameterTypes();
        List<Object[]> argLists = new ArrayList<>();
        if (params.length == 0) {
            argLists.add(new Object[0]);
        } else if (params.length == 1 && params[0].isEnum()) {
            for (Object constant : params[0].getEnumConstants()) {
                argLists.add(new Object[] {constant});
            }
        } else {
            out.print(label + "." + method.getName() + " SKIP unsupported data signature\n");
            return;
        }
        for (Object[] argv : argLists) {
            String head = label + "." + method.getName() + "(" + (argv.length == 0 ? "" : argv[0]) + ")";
            Object result = method.invoke(null, argv);
            cases++;
            if (result instanceof List<?> list) {
                if (list.isEmpty()) {
                    out.print(head + " -> empty\n");
                }
                for (Object element : list) {
                    out.print(head + " " + digest(String.valueOf(element)) + "\n");
                }
            } else {
                out.print(head + " -> " + digest(show(result)) + "\n");
            }
        }
    }

    private void runCase(String label, Method method, int index, SplittableRandom random) throws Exception {
        Class<?>[] params = method.getParameterTypes();
        Type[] generics = method.getGenericParameterTypes();
        Object[] argv = new Object[params.length];
        String[] shown = new String[params.length];
        Policy policy = policy(method.getName());
        Object tree = null;
        Object[] treeNodes = null;
        int[] firstArray = null;
        for (int i = 0; i < params.length; i++) {
            Class<?> p = params[i];
            if (p == monitorType(method)) {
                int k = random.nextInt(100) < MONITOR_CANCEL_PERCENT ? random.nextInt(25) : -1;
                argv[i] = k < 0 ? none(p) : cancelAfter(p, k);
                shown[i] = k < 0 ? "none" : "cancel@" + k;
            } else if (p == int.class) {
                int value = scalar(method.getName(), i, policy, random, argv, firstArray);
                argv[i] = value;
                shown[i] = Integer.toString(value);
            } else if (p == long.class) {
                long value = longScalar(policy, random);
                argv[i] = value;
                shown[i] = Long.toString(value);
            } else if (p == int[].class) {
                int[] values = ints(method.getName(), i, policy, random, firstArray);
                if (firstArray == null) {
                    firstArray = values;
                }
                argv[i] = values;
                shown[i] = Arrays.toString(values);
            } else if (p == long[].class) {
                long[] prefix = prefix(policy, random);
                argv[i] = prefix;
                shown[i] = Arrays.toString(prefix);
            } else if (p == int[][].class) {
                int[][] matrix = matrix(method.getName(), policy, random);
                argv[i] = matrix;
                shown[i] = Arrays.deepToString(matrix);
            } else if (p == char[][].class) {
                char[][] grid = grid(random);
                argv[i] = grid;
                shown[i] = Arrays.deepToString(grid);
            } else if (p == String[].class) {
                String[] tokens = new String[random.nextInt(8)];
                for (int t = 0; t < tokens.length; t++) {
                    tokens[t] = RPN_TOKENS[random.nextInt(RPN_TOKENS.length)];
                }
                argv[i] = tokens;
                shown[i] = Arrays.toString(tokens);
            } else if (p == String.class || p == CharSequence.class) {
                String text = text(method.getName(), i, random);
                argv[i] = text;
                shown[i] = "\"" + text + "\"";
            } else if (p == List.class) {
                Object list = list(method.getName(), generics[i], method.getDeclaringClass(), random);
                argv[i] = list;
                shown[i] = String.valueOf(list);
            } else if (p.isRecord()) {
                Object interval = interval(p, random, false);
                argv[i] = interval;
                shown[i] = String.valueOf(interval);
            } else if (p == Comparator.class) {
                argv[i] = Comparator.naturalOrder();
                shown[i] = "natural";
            } else if (p == IntPredicate.class) {
                int threshold = random.nextInt(-6, 24);
                IntPredicate predicate = value -> value >= threshold;
                argv[i] = predicate;
                shown[i] = ">=" + threshold;
            } else if (p.isEnum()) {
                Object[] constants = p.getEnumConstants();
                argv[i] = constants[index % constants.length];
                shown[i] = String.valueOf(argv[i]);
            } else if (isListNode(p)) {
                Object head = linkedList(p, method.getName(), random);
                argv[i] = head;
                shown[i] = chain(head);
            } else if (isTreeNode(p)) {
                if (tree == null) {
                    treeNodes = treeNodes(p, random);
                    tree = treeNodes.length == 0 ? null : treeNodes[0];
                    argv[i] = tree;
                    shown[i] = subtree(tree);
                } else {
                    Object node = treeNodes.length == 0 || random.nextInt(20) == 0
                            ? null : treeNodes[random.nextInt(treeNodes.length)];
                    argv[i] = node;
                    shown[i] = node == null ? "null" : "node:" + value(node);
                }
            } else {
                throw new IllegalStateException("no generator for " + p + " in " + signature(method));
            }
        }
        StringBuilder line = new StringBuilder(label).append('.').append(signature(method)).append(' ').append(index)
                .append(" | ").append(String.join("; ", shown)).append(" -> ");
        cases++;
        Future<Object> future = executor.submit(() -> method.invoke(null, argv));
        try {
            Object result = future.get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            if (method.getReturnType() == void.class) {
                line.append("void after=").append(digest(afterState(params, argv)));
            } else {
                line.append(digest(show(result)));
            }
        } catch (ExecutionException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof InvocationTargetException invocation) {
                cause = invocation.getCause();
            }
            thrown++;
            line.append("throws ").append(cause.getClass().getSimpleName()).append(": ").append(cause.getMessage());
        } catch (TimeoutException timeout) {
            timeouts++;
            future.cancel(true);
            executor.shutdownNow();
            executor = newExecutor();
            line.append("TIMEOUT");
        }
        out.print(line.append('\n'));
    }

    private static String afterState(Class<?>[] params, Object[] argv) {
        StringBuilder after = new StringBuilder();
        for (int i = 0; i < params.length; i++) {
            if (params[i].isArray()) {
                after.append(after.isEmpty() ? "" : "; ").append(show(argv[i]));
            }
        }
        return after.toString();
    }

    private record Policy(boolean extremes, int low, int high, int maxLength, boolean positive, boolean sorted) {
    }

    private static Policy policy(String method) {
        boolean small = SMALL_ONLY.contains(method);
        boolean positive = POSITIVE.contains(method);
        int maxLength = method.equals("permutations") || method.equals("subsets") ? 6 : small ? 8 : 10;
        return new Policy(!small, positive ? 1 : small ? 0 : -8, small ? 9 : 20, maxLength, positive,
                SORTED.contains(method));
    }

    private static int element(Policy policy, SplittableRandom random) {
        if (policy.extremes()) {
            int kind = random.nextInt(10);
            if (kind < 2) {
                return EXTREMES[random.nextInt(EXTREMES.length)];
            }
            if (kind == 2) {
                return random.nextInt();
            }
        }
        return random.nextInt(policy.low(), policy.high() + 1);
    }

    private static int[] ints(String method, int position, Policy policy, SplittableRandom random, int[] first) {
        int length = first != null && random.nextInt(10) < 9 ? first.length : random.nextInt(policy.maxLength() + 1);
        int[] values = new int[length];
        for (int i = 0; i < length; i++) {
            values[i] = element(policy, random);
        }
        if (policy.sorted() && random.nextInt(10) < 9) {
            Arrays.sort(values);
        }
        if (method.equals("searchRotated")) {
            values = Arrays.stream(values).distinct().sorted().toArray();
            if (values.length > 0) {
                int pivot = random.nextInt(values.length);
                int[] rotated = new int[values.length];
                for (int i = 0; i < values.length; i++) {
                    rotated[i] = values[(i + pivot) % values.length];
                }
                values = rotated;
            }
        }
        if (method.equals("sortColors") && random.nextInt(10) < 8) {
            for (int i = 0; i < length; i++) {
                values[i] = random.nextInt(3);
            }
        }
        return values;
    }

    private static int scalar(String method, int position, Policy policy, SplittableRandom random, Object[] argv,
            int[] first) {
        if (method.equals("hammingWeight") || method.equals("reverseBits")) {
            return random.nextInt(4) == 0 ? EXTREMES[random.nextInt(EXTREMES.length)] : random.nextInt();
        }
        if (method.equals("firstTrue")) {
            if (position == 0) {
                return random.nextInt(-5, 6);
            }
            return (Integer) argv[0] + random.nextInt(-1, 14);
        }
        if (first != null && (method.equals("twoSum") || method.equals("sortedTwoSum") || method.equals("lowerBound")
                || method.equals("upperBound") || method.equals("searchRotated") || method.equals("searchSorted"))
                && first.length > 0 && random.nextBoolean()) {
            return method.endsWith("TwoSum") && first.length > 1
                    ? first[random.nextInt(first.length)] + first[random.nextInt(first.length)]
                    : first[random.nextInt(first.length)];
        }
        if (first != null && (method.equals("twoSum") || method.equals("sortedTwoSum") || method.equals("lowerBound")
                || method.equals("upperBound") || method.equals("searchRotated"))) {
            return element(policy, random);
        }
        int bound = first != null ? first.length + 2 : policy.maxLength() + 1;
        return random.nextInt(bound);
    }

    private static long longScalar(Policy policy, SplittableRandom random) {
        int kind = random.nextInt(10);
        if (policy.extremes() && kind < 2) {
            return LONG_EXTREMES[random.nextInt(LONG_EXTREMES.length)];
        }
        if (kind < 4) {
            return random.nextLong(-40L, 41L);
        }
        return random.nextLong(0L, 31L);
    }

    private static long[] prefix(Policy policy, SplittableRandom random) {
        int length = random.nextInt(policy.maxLength() + 1);
        long[] prefix = new long[length + 1];
        for (int i = 1; i <= length; i++) {
            prefix[i] = prefix[i - 1] + element(policy, random);
        }
        return prefix;
    }

    private static int[][] matrix(String method, Policy policy, SplittableRandom random) {
        int rows = random.nextInt(6);
        int cols = random.nextInt(6);
        int[][] matrix = new int[rows][];
        boolean sorted = method.equals("searchSorted") && random.nextInt(10) < 7;
        boolean square = method.equals("rotateClockwise") && random.nextInt(10) < 8;
        int next = random.nextInt(-3, 3);
        for (int r = 0; r < rows; r++) {
            int width = random.nextInt(20) == 0 ? random.nextInt(6) : square ? rows : cols;
            matrix[r] = new int[width];
            for (int c = 0; c < width; c++) {
                if (sorted) {
                    next += random.nextInt(3);
                    matrix[r][c] = next;
                } else {
                    matrix[r][c] = random.nextInt(10) == 0 ? 0 : element(policy, random);
                }
            }
        }
        return matrix;
    }

    private static char[][] grid(SplittableRandom random) {
        int rows = random.nextInt(6);
        int cols = random.nextInt(6);
        char[][] grid = new char[rows][];
        for (int r = 0; r < rows; r++) {
            grid[r] = new char[random.nextInt(20) == 0 ? random.nextInt(6) : cols];
            for (int c = 0; c < grid[r].length; c++) {
                grid[r][c] = random.nextInt(20) == 0 ? 'x' : random.nextBoolean() ? '1' : '0';
            }
        }
        return grid;
    }

    private static String text(String method, int position, SplittableRandom random) {
        String alphabet = switch (method) {
            case "validBrackets" -> random.nextInt(10) == 0 ? "()[]{}a" : "()[]{}";
            case "wildcardMatch" -> position == 1 ? "ab*?" : "abc";
            case "longestDistinctSubstring" -> "abcd";
            case "wordBreak" -> "ab";
            default -> "abc";
        };
        int length = random.nextInt(11);
        StringBuilder text = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            text.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return text.toString();
    }

    private static Object list(String method, Type generic, Class<?> owner, SplittableRandom random) throws Exception {
        Type element = generic instanceof ParameterizedType parameterized ? parameterized.getActualTypeArguments()[0]
                : Object.class;
        if (element instanceof WildcardType wildcard) {
            element = wildcard.getUpperBounds()[0];
        }
        if (element instanceof Class<?> type && type.isRecord()) {
            int length = random.nextInt(9);
            List<Object> intervals = new ArrayList<>(length);
            boolean ordered = random.nextInt(10) < 8;
            int start = random.nextInt(-5, 6);
            for (int i = 0; i < length; i++) {
                if (ordered) {
                    start += random.nextInt(0, 7);
                    intervals.add(interval(type, start, start + random.nextInt(0, 7)));
                } else {
                    intervals.add(interval(type, random, true));
                }
            }
            return intervals;
        }
        if (element instanceof ParameterizedType) {
            int lists = random.nextInt(5);
            List<Object> outer = new ArrayList<>(lists);
            for (int i = 0; i < lists; i++) {
                outer.add(integers(random, true));
            }
            return outer;
        }
        if (element == String.class) {
            int words = random.nextInt(5);
            List<Object> dictionary = new ArrayList<>(words);
            for (int i = 0; i < words; i++) {
                int length = random.nextInt(1, 4);
                StringBuilder word = new StringBuilder(length);
                for (int c = 0; c < length; c++) {
                    word.append("ab".charAt(random.nextInt(2)));
                }
                dictionary.add(word.toString());
            }
            return dictionary;
        }
        return integers(random, SORTED.contains(method));
    }

    private static List<Object> integers(SplittableRandom random, boolean sorted) {
        int length = random.nextInt(6);
        int[] values = new int[length];
        for (int i = 0; i < length; i++) {
            values[i] = random.nextInt(-3, 12);
        }
        if (sorted) {
            Arrays.sort(values);
        }
        List<Object> list = new ArrayList<>(length);
        for (int value : values) {
            list.add(value);
        }
        return list;
    }

    private static Object interval(Class<?> type, SplittableRandom random, boolean anyOrder) throws Exception {
        int start = random.nextInt(20) == 0 ? EXTREMES[random.nextInt(EXTREMES.length)] : random.nextInt(-5, 21);
        int span = random.nextInt(0, 7);
        int end = start > Integer.MAX_VALUE - span ? start : start + span;
        if (anyOrder && random.nextInt(10) == 0) {
            int swap = start;
            start = end;
            end = swap;
        }
        return interval(type, start, end);
    }

    private static Object interval(Class<?> type, int start, int end) throws Exception {
        try {
            return type.getConstructor(int.class, int.class).newInstance(start, end);
        } catch (InvocationTargetException refused) {
            return type.getConstructor(int.class, int.class).newInstance(Math.min(start, end), Math.max(start, end));
        }
    }

    private static boolean isListNode(Class<?> type) {
        return type.getSimpleName().equals("Node") && hasMethod(type, "next");
    }

    private static boolean isTreeNode(Class<?> type) {
        return type.getSimpleName().equals("Node") && hasMethod(type, "left");
    }

    private static boolean hasMethod(Class<?> type, String name) {
        try {
            type.getMethod(name);
            return true;
        } catch (NoSuchMethodException absent) {
            return false;
        }
    }

    private static Object linkedList(Class<?> node, String method, SplittableRandom random) throws Exception {
        int length = random.nextInt(9);
        if (length == 0) {
            return null;
        }
        int[] values = new int[length];
        for (int i = 0; i < length; i++) {
            values[i] = random.nextInt(-3, 12);
        }
        if (method.equals("mergeSorted")) {
            Arrays.sort(values);
        }
        Object[] nodes = new Object[length];
        for (int i = 0; i < length; i++) {
            nodes[i] = node.getConstructor(Object.class).newInstance(values[i]);
            if (i > 0) {
                node.getMethod("next", node).invoke(nodes[i - 1], nodes[i]);
            }
        }
        if (method.equals("cycleStart") && random.nextBoolean()) {
            node.getMethod("next", node).invoke(nodes[length - 1], nodes[random.nextInt(length)]);
        }
        return nodes[0];
    }

    private static Object[] treeNodes(Class<?> node, SplittableRandom random) throws Exception {
        int size = random.nextInt(11);
        Object[] nodes = new Object[size];
        boolean bst = random.nextBoolean();
        Method left = node.getMethod("left", node);
        Method right = node.getMethod("right", node);
        Method leftOf = node.getMethod("left");
        Method rightOf = node.getMethod("right");
        for (int i = 0; i < size; i++) {
            int value = random.nextInt(-3, 30);
            nodes[i] = node.getConstructor(Object.class).newInstance(value);
            if (i == 0) {
                continue;
            }
            Object parent = nodes[0];
            while (true) {
                boolean goLeft = bst ? value < (Integer) value(parent) : random.nextBoolean();
                Object child = (goLeft ? leftOf : rightOf).invoke(parent);
                if (child == null) {
                    (goLeft ? left : right).invoke(parent, nodes[i]);
                    break;
                }
                parent = child;
            }
        }
        return nodes;
    }

    private static Object value(Object node) throws Exception {
        return node.getClass().getMethod("value").invoke(node);
    }

    private static String chain(Object head) {
        if (head == null) {
            return "null";
        }
        try {
            StringBuilder chain = new StringBuilder("[");
            Set<Object> seen = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
            Object node = head;
            Method next = head.getClass().getMethod("next");
            while (node != null) {
                if (!seen.add(node)) {
                    chain.append(" ~cycle->").append(value(node));
                    break;
                }
                chain.append(seen.size() == 1 ? "" : " ").append(value(node));
                node = next.invoke(node);
            }
            return chain.append(']').toString();
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static String subtree(Object node) {
        if (node == null) {
            return "()";
        }
        try {
            Class<?> type = node.getClass();
            return "(" + value(node) + " " + subtree(type.getMethod("left").invoke(node)) + " "
                    + subtree(type.getMethod("right").invoke(node)) + ")";
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static String show(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof int[] ints) {
            return Arrays.toString(ints);
        }
        if (value instanceof long[] longs) {
            return Arrays.toString(longs);
        }
        if (value instanceof char[] chars) {
            return Arrays.toString(chars);
        }
        if (value instanceof Object[] objects) {
            return Arrays.deepToString(objects);
        }
        if (isListNode(value.getClass())) {
            return chain(value);
        }
        if (isTreeNode(value.getClass())) {
            try {
                return "node:" + value(value);
            } catch (Exception failure) {
                throw new IllegalStateException(failure);
            }
        }
        return String.valueOf(value);
    }

    private static String digest(String text) {
        if (text.length() <= LONG_VALUE) {
            return text;
        }
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return "len=" + text.length() + " sha256=" + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException missing) {
            throw new IllegalStateException(missing);
        }
    }

    private static Class<?> monitorType(Method method) {
        for (Class<?> p : method.getParameterTypes()) {
            if (p.isInterface() && p.getSimpleName().equals("M3ProgressMonitor")) {
                return p;
            }
        }
        return null;
    }

    private static Object none(Class<?> monitor) throws Exception {
        return monitor.getMethod("none").invoke(null);
    }

    private static Object cancelAfter(Class<?> monitor, int k) {
        int[] ticks = {0};
        InvocationHandler handler = (proxy, method, a) -> switch (method.getName()) {
            case "isCanceled" -> ticks[0] >= k;
            case "worked" -> {
                ticks[0] += ((Number) a[0]).intValue();
                yield null;
            }
            case "checkCanceled" -> InvocationHandler.invokeDefault(proxy, method, a);
            case "toString" -> "cancel@" + k;
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == a[0];
            default -> method.getReturnType() == boolean.class ? Boolean.FALSE
                    : method.getReturnType() == int.class ? Integer.valueOf(0) : null;
        };
        return Proxy.newProxyInstance(monitor.getClassLoader(), new Class<?>[] {monitor}, handler);
    }

    private static String signature(Method method) {
        StringBuilder signature = new StringBuilder(method.getName()).append('(');
        Class<?>[] params = method.getParameterTypes();
        for (int i = 0; i < params.length; i++) {
            signature.append(i == 0 ? "" : ",").append(params[i].getSimpleName());
        }
        return signature.append(')').toString();
    }
}
