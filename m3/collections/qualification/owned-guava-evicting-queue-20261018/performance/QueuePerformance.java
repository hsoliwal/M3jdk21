/* Copyright 2026 Synexia. SPDX-License-Identifier: Apache-2.0 */
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/** Local indicative batches; setup and SHA256 are outside timed regions. */
public final class QueuePerformance {
    private static final int SEED = 1592594996;
    private static final int[] CAPACITIES = {0, 17, 1024};
    private static final String[] WORKLOADS = {"eviction-offer", "mixed-offer-poll",
        "interior-iterator-remove", "removeAll-sparse", "removeAll-dense",
        "oversized-addAll", "wrapped-typed-toArray"};
    private static final int[] CYCLES = {1000000, 500000, 1500, 1500, 1500, 2500, 20000};
    private static volatile long blackhole;
    private static volatile Object arrayBlackhole;

    private QueuePerformance() { }

    private record Inputs(Integer[] values, Integer[] fill, List<Integer> oversized,
                          Set<Integer> sparse, Set<Integer> dense) { }
    private record Result(long nanos, long checksum, String digest) { }

    private static Inputs inputs(int capacity) {
        Integer[] values = new Integer[65536];
        int state = SEED;
        for (int index = 0; index < values.length; index++) {
            state ^= state << 13;
            state ^= state >>> 17;
            state ^= state << 5;
            values[index] = state & 65535;
        }
        List<Integer> oversized = new ArrayList<>(2 * capacity + 7);
        for (int index = 0; index < 2 * capacity + 7; index++) oversized.add(values[index]);
        Set<Integer> sparse = new HashSet<>();
        Set<Integer> dense = new HashSet<>();
        for (int index = 0; index < capacity + capacity / 3; index++) {
            if ((index & 15) == 0) sparse.add(index);
            if ((index & 3) != 0) dense.add(index);
        }
        Integer[] fill = new Integer[capacity + capacity / 3];
        for (int index = 0; index < fill.length; index++) fill[index] = index;
        return new Inputs(values, fill, oversized, sparse, dense);
    }

    private static long consume(long accumulated, Integer value) {
        return accumulated * 31 + (value == null ? -1 : value.intValue());
    }

    private static void fillWrapped(Queue<Integer> queue, Inputs inputs) {
        queue.clear();
        for (Integer value : inputs.fill()) queue.offer(value);
    }

    @SuppressWarnings("unchecked")
    private static Queue<Integer> create(Method factory, int capacity) throws ReflectiveOperationException {
        Object result = factory.invoke(null, capacity);
        if (!(result instanceof Queue<?>)) throw new IllegalStateException("factory is not a Queue");
        return (Queue<Integer>) result;
    }

    private static Result batch(Method factory, Inputs inputs, int capacity, int workload) throws Exception {
        Queue<Integer> queue = create(factory, capacity);
        fillWrapped(queue, inputs);
        Integer[] oversizedArray = new Integer[capacity + 2];
        Integer[] lastArray = null;
        long consumed = 0;
        int cycles = CYCLES[workload];
        Integer[] values = inputs.values();
        long started = System.nanoTime();
        switch (workload) {
            case 0 -> {
                for (int index = 0; index < cycles; index++) {
                    consumed += queue.offer(values[index & 65535]) ? 1 : 0;
                }
            }
            case 1 -> {
                for (int index = 0; index < cycles; index++) {
                    consumed += queue.offer(values[index & 65535]) ? 1 : 0;
                    if (index % 3 == 0) consumed = consume(consumed, queue.poll());
                }
            }
            case 2 -> {
                for (int index = 0; index < cycles; index++) {
                    fillWrapped(queue, inputs);
                    Iterator<Integer> cursor = queue.iterator();
                    if (capacity == 0) {
                        consumed += cursor.hasNext() ? 1 : 0;
                    } else {
                        for (int step = 0; step <= capacity / 2; step++) {
                            consumed = consume(consumed, cursor.next());
                        }
                        cursor.remove();
                        consumed += queue.size();
                    }
                    consumed += queue.offer(values[index & 65535]) ? 1 : 0;
                }
            }
            case 3, 4 -> {
                Set<Integer> removed = workload == 3 ? inputs.sparse() : inputs.dense();
                for (int index = 0; index < cycles; index++) {
                    fillWrapped(queue, inputs);
                    consumed += queue.removeAll(removed) ? 1 : 0;
                    consumed = consume(consumed, queue.peek());
                    consumed += queue.size();
                }
            }
            case 5 -> {
                for (int index = 0; index < cycles; index++) {
                    consumed += queue.offer(values[index & 65535]) ? 1 : 0;
                    consumed += queue.addAll(inputs.oversized()) ? 1 : 0;
                    consumed = consume(consumed, queue.peek());
                }
            }
            case 6 -> {
                for (int index = 0; index < cycles; index++) {
                    consumed += queue.offer(values[index & 65535]) ? 1 : 0;
                    Integer[] caller = (index & 1) == 0 ? new Integer[0] : oversizedArray;
                    lastArray = queue.toArray(caller);
                    int size = queue.size();
                    consumed += lastArray.length;
                    if (size != 0) {
                        consumed = consume(consumed, lastArray[0]);
                        consumed = consume(consumed, lastArray[size - 1]);
                    }
                    if (lastArray.length > size) consumed += lastArray[size] == null ? 1 : 0;
                }
            }
            default -> throw new IllegalArgumentException("unknown workload");
        }
        long elapsed = System.nanoTime() - started;
        blackhole = consumed;
        arrayBlackhole = lastArray;
        // Every final element and the return checksum are bound outside timing.
        StringBuilder state = new StringBuilder(WORKLOADS[workload]).append('\t')
            .append(capacity).append('\t').append(consumed).append('\t').append(queue.size());
        for (Integer value : queue) state.append('\t').append(value);
        String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
            .digest(state.toString().getBytes(StandardCharsets.UTF_8)));
        return new Result(elapsed, consumed, digest);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("exact owner class required");
        Method factory = Class.forName(args[0]).getMethod("create", int.class);
        System.out.println("META\t" + System.getProperty("java.runtime.version") + "\t"
            + System.getProperty("java.vm.name") + "\t" + Runtime.getRuntime().availableProcessors()
            + "\t" + Runtime.getRuntime().maxMemory() + "\t" + args[0]);
        for (int capacity : CAPACITIES) {
            Inputs prepared = inputs(capacity);
            for (int workload = 0; workload < WORKLOADS.length; workload++) {
                Result expected = batch(factory, prepared, capacity, workload);
                System.out.println("DIGEST\t" + capacity + "\t" + WORKLOADS[workload] + "\t" + expected.digest());
                for (int warmup = 0; warmup < 3; warmup++) {
                    Result warmed = batch(factory, prepared, capacity, workload);
                    if (!warmed.digest().equals(expected.digest())) throw new IllegalStateException("warmup state drift");
                }
                for (int measurement = 0; measurement < 3; measurement++) {
                    Result measured = batch(factory, prepared, capacity, workload);
                    if (!measured.digest().equals(expected.digest())) throw new IllegalStateException("timed state drift");
                    System.out.println("ROW\t" + capacity + "\t" + WORKLOADS[workload] + "\t"
                        + measurement + "\t" + CYCLES[workload] + "\t" + measured.nanos()
                        + "\t" + measured.checksum() + "\t" + measured.digest());
                }
            }
        }
        System.out.println("BLACKHOLE\t" + blackhole + "\t" + (arrayBlackhole == null ? 0 : 1));
    }
}
