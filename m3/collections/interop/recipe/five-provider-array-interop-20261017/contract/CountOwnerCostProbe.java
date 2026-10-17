/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */

import com.google.common.collect.LinkedHashMultiset;
import com.m3.collections.M3IndexPathMasks;
import com.m3.collections.M3ObjectCounts;
import com.sun.management.ThreadMXBean;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.commons.collections4.multiset.HashMultiSet;
import org.eclipse.collections.impl.bag.mutable.HashBag;

/**
 * Bounded diagnostic for the pinned count-owner packet; not a performance gate or JMH suite.
 *
 * <p>All keys and flag words are created before measurement. Fresh-owner admission uses
 * each provider's default constructor after code warmup. Updates preserve the original
 * positive count at every key; no rehash, key creation, overflow or complete removal is
 * part of that phase. Every measured result is fully checked outside its timed region.
 * The full size corpus is warmed before any samples are recorded, and provider order
 * reverses on alternating rounds. CPU and cumulative allocated bytes
 * cover only the calling Java thread; allocation is not retained-heap size. Meter-call
 * overhead is not subtracted. There is no forced GC or process fork in this harness.
 *
 * <p>The flags phases exercise only M3: these donor collections have no equivalent
 * application flag columns. Selection includes construction of the primitive selection
 * metadata and detached result list on every call. It explicitly uses the Java kernel.
 * This source is a contract asset, not a runtime source or a production dependency.
 */
public final class CountOwnerCostProbe {
    private static final int WARMUP_ROUNDS = 6;
    private static final int SAMPLE_ROUNDS = 8;
    private static final int UPDATE_ROUNDS = 4;
    private static final int FLAG_WRITE_ROUNDS = 4;
    private static final int[] SIZES = {4096, 65536};
    private static volatile Object escaped;
    private static volatile long checksumSink;

    private CountOwnerCostProbe() { }

    private record Key(int id) { }

    private record Corpus(Key[] keys, int[] flags) { }

    private enum Provider {
        M3 {
            @Override Object create() { return new M3ObjectCounts<Key>(); }
            @Override void fill(Object value, Key[] keys) {
                M3ObjectCounts<Key> counts = cast(value);
                for (Key key : keys) counts.add(key, baseCount(key));
            }
            @Override void update(Object value, Key[] keys) {
                M3ObjectCounts<Key> counts = cast(value);
                for (int round = 0; round < UPDATE_ROUNDS; round++) {
                    for (int position = 0; position < keys.length; position++) {
                        Key key = permuted(keys, position);
                        counts.add(key, 2);
                        counts.remove(key, 2);
                    }
                }
            }
            @Override int count(Object value, Key key) {
                return ((M3ObjectCounts<?>) value).count(key);
            }
            @Override int distinct(Object value) {
                return ((M3ObjectCounts<?>) value).distinctSize();
            }
            @Override long total(Object value) {
                return ((M3ObjectCounts<?>) value).totalCount();
            }
        },
        GUAVA_LINKED {
            @Override Object create() { return LinkedHashMultiset.<Key>create(); }
            @Override void fill(Object value, Key[] keys) {
                LinkedHashMultiset<Key> counts = cast(value);
                for (Key key : keys) counts.add(key, baseCount(key));
            }
            @Override void update(Object value, Key[] keys) {
                LinkedHashMultiset<Key> counts = cast(value);
                for (int round = 0; round < UPDATE_ROUNDS; round++) {
                    for (int position = 0; position < keys.length; position++) {
                        Key key = permuted(keys, position);
                        counts.add(key, 2);
                        counts.remove(key, 2);
                    }
                }
            }
            @Override int count(Object value, Key key) {
                return ((LinkedHashMultiset<?>) value).count(key);
            }
            @Override int distinct(Object value) {
                return ((LinkedHashMultiset<?>) value).elementSet().size();
            }
            @Override long total(Object value) {
                return ((LinkedHashMultiset<?>) value).size();
            }
        },
        APACHE_HASH {
            @Override Object create() { return new HashMultiSet<Key>(); }
            @Override void fill(Object value, Key[] keys) {
                HashMultiSet<Key> counts = cast(value);
                for (Key key : keys) counts.add(key, baseCount(key));
            }
            @Override void update(Object value, Key[] keys) {
                HashMultiSet<Key> counts = cast(value);
                for (int round = 0; round < UPDATE_ROUNDS; round++) {
                    for (int position = 0; position < keys.length; position++) {
                        Key key = permuted(keys, position);
                        counts.add(key, 2);
                        counts.remove(key, 2);
                    }
                }
            }
            @Override int count(Object value, Key key) {
                return ((HashMultiSet<?>) value).getCount(key);
            }
            @Override int distinct(Object value) {
                return ((HashMultiSet<?>) value).uniqueSet().size();
            }
            @Override long total(Object value) {
                return ((HashMultiSet<?>) value).size();
            }
        },
        ECLIPSE_HASH {
            @Override Object create() { return new HashBag<Key>(); }
            @Override void fill(Object value, Key[] keys) {
                HashBag<Key> counts = cast(value);
                for (Key key : keys) counts.addOccurrences(key, baseCount(key));
            }
            @Override void update(Object value, Key[] keys) {
                HashBag<Key> counts = cast(value);
                for (int round = 0; round < UPDATE_ROUNDS; round++) {
                    for (int position = 0; position < keys.length; position++) {
                        Key key = permuted(keys, position);
                        counts.addOccurrences(key, 2);
                        counts.removeOccurrences(key, 2);
                    }
                }
            }
            @Override int count(Object value, Key key) {
                return ((HashBag<?>) value).occurrencesOf(key);
            }
            @Override int distinct(Object value) {
                return ((HashBag<?>) value).sizeDistinct();
            }
            @Override long total(Object value) {
                return ((HashBag<?>) value).size();
            }
        };

        abstract Object create();
        abstract void fill(Object value, Key[] keys);
        abstract void update(Object value, Key[] keys);
        abstract int count(Object value, Key key);
        abstract int distinct(Object value);
        abstract long total(Object value);
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object value) { return (T) value; }

    private record Observation(int distinct, long total, long checksum) { }

    private record Sample(int keys, int round, String order, String phase, Provider provider,
            long operations, long wallNanos, long cpuNanos, long allocatedBytes,
            Observation observation, int selected, long selectionChecksum) {
        String csv() {
            return keys + "," + round + "," + order + "," + phase + "," + provider
                    + "," + operations + "," + wallNanos + "," + cpuNanos + "," + allocatedBytes
                    + "," + observation.distinct + "," + observation.total + "," + observation.checksum
                    + "," + selected + "," + selectionChecksum;
        }
    }

    private static final class Meter {
        private final ThreadMXBean bean = ManagementFactory.getPlatformMXBean(ThreadMXBean.class);
        private final long threadId = Thread.currentThread().threadId();
        private long startAllocation;
        private long startCpu;
        private long startWall;
        private long wallNanos;
        private long cpuNanos;
        private long allocatedBytes;

        Meter() {
            if (bean == null || !bean.isThreadAllocatedMemorySupported()
                    || !bean.isCurrentThreadCpuTimeSupported()) {
                throw new IllegalStateException("This VM lacks required CPU/allocation meters");
            }
            if (!bean.isThreadAllocatedMemoryEnabled()) bean.setThreadAllocatedMemoryEnabled(true);
            if (!bean.isThreadCpuTimeEnabled()) bean.setThreadCpuTimeEnabled(true);
            start();
            stop();
        }

        void start() {
            startAllocation = bean.getThreadAllocatedBytes(threadId);
            startCpu = bean.getCurrentThreadCpuTime();
            startWall = System.nanoTime();
        }

        void stop() {
            wallNanos = System.nanoTime() - startWall;
            cpuNanos = bean.getCurrentThreadCpuTime() - startCpu;
            allocatedBytes = bean.getThreadAllocatedBytes(threadId) - startAllocation;
            if (wallNanos < 0 || cpuNanos < 0 || allocatedBytes < 0) {
                throw new IllegalStateException("A required meter returned an invalid sample");
            }
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 0) throw new IllegalArgumentException("The frozen probe takes no arguments");
        requireVersion("com.google.guava", "guava", "33.4.8-jre");
        requireVersion("org.apache.commons", "commons-collections4", "4.6.0");
        requireVersion("org.eclipse.collections", "eclipse-collections", "11.1.0");
        requireVersion("org.eclipse.collections", "eclipse-collections-api", "11.1.0");

        System.out.println("# probe=CountOwnerCostProbe-20261017; diagnostic=true; forks=1");
        System.out.println("# java=" + System.getProperty("java.runtime.version")
                + "; vm=" + System.getProperty("java.vm.name")
                + "; arch=" + System.getProperty("os.arch")
                + "; processors=" + Runtime.getRuntime().availableProcessors());
        System.out.println("# warmups=" + WARMUP_ROUNDS + "; samples=" + SAMPLE_ROUNDS
                + "; updateRounds=" + UPDATE_ROUNDS + "; flagWriteRounds=" + FLAG_WRITE_ROUNDS
                + "; warmupPlan=all-sizes-before-measurement");
        System.out.println("# initialCount=1+(keyId&3); keyHash=keyId; construction=default-capacity"
                + "; updateOrder=(position*2053+17)&(keys-1)");
        System.out.println("# fresh_admit=warm-code-new-owner; current-thread-only=true"
                + "; allocation=total-allocated-not-retained; no-forced-GC=true");
        System.out.println("# m3_flags_select=required:1,forbidden:2,backend:JAVA"
                + "; selection-includes-metadata-and-result=true; donor-flag-equivalence=none");
        System.out.println("keys,round,order,phase,provider,operations,wall_ns,cpu_ns,allocated_bytes,"
                + "distinct,total,count_checksum,selected,selection_checksum");

        Meter meter = new Meter();
        List<Sample> samples = new ArrayList<>();
        List<Corpus> corpora = new ArrayList<>();
        for (int size : SIZES) {
            Key[] keys = new Key[size];
            int[] flags = new int[size];
            for (int i = 0; i < size; i++) {
                keys[i] = new Key(i);
                flags[i] = ((i & 1) == 0 ? 1 : 0) | (i % 3 == 0 ? 2 : 0)
                        | (i % 5 == 0 ? Integer.MIN_VALUE : 0);
            }
            corpora.add(new Corpus(keys, flags));
        }
        for (int round = -WARMUP_ROUNDS; round < 0; round++) {
            for (Corpus corpus : corpora) {
                runCountRound(corpus.keys, round, meter, samples);
                runFlagRound(corpus.keys, corpus.flags, round, meter, samples);
            }
        }
        for (Corpus corpus : corpora) {
            for (int round = 0; round < SAMPLE_ROUNDS; round++) {
                runCountRound(corpus.keys, round, meter, samples);
                runFlagRound(corpus.keys, corpus.flags, round, meter, samples);
            }
        }
        for (Sample sample : samples) System.out.println(sample.csv());
        System.out.println("# verified_samples=" + samples.size() + "; sink=" + checksumSink
                + "; last_result_class=" + escaped.getClass().getName());
    }

    private static void runCountRound(Key[] keys, int round, Meter meter, List<Sample> samples) {
        Provider[] providers = Provider.values();
        boolean reverse = (round & 1) != 0;
        String order = reverse ? "reverse" : "forward";
        for (int position = 0; position < providers.length; position++) {
            Provider provider = providers[reverse ? providers.length - 1 - position : position];
            meter.start();
            Object value = provider.create();
            provider.fill(value, keys);
            meter.stop();
            escaped = value;
            Observation observation = verify(provider, value, keys);
            if (round >= 0) samples.add(sample(keys, round, order, "fresh_admit", provider,
                    keys.length, meter, observation, 0, 0));

            // The same fully admitted owner is used; verification is outside both regions.
            meter.start();
            provider.update(value, keys);
            meter.stop();
            escaped = value;
            observation = verify(provider, value, keys);
            if (round >= 0) samples.add(sample(keys, round, order, "update_existing", provider,
                    (long) keys.length * UPDATE_ROUNDS * 2, meter, observation, 0, 0));
        }
    }

    private static void runFlagRound(Key[] keys, int[] flags, int round, Meter meter,
            List<Sample> samples) {
        M3ObjectCounts<Key> counts = new M3ObjectCounts<>();
        Provider.M3.fill(counts, keys);
        long countRevision = counts.countRevision();
        meter.start();
        for (int repeat = 0; repeat < FLAG_WRITE_ROUNDS; repeat++) {
            int toggled = (repeat & 1) == 0 ? 0x100 : 0;
            for (int i = 0; i < keys.length; i++) counts.setFlags(keys[i], flags[i] ^ toggled);
        }
        meter.stop();
        escaped = counts;
        Observation observation = verify(Provider.M3, counts, keys);
        if (counts.countRevision() != countRevision) {
            throw new AssertionError("Flag writes changed the multiplicity revision");
        }
        int expectedSelected = 0;
        long expectedSelectionChecksum = 0;
        for (int i = 0; i < keys.length; i++) {
            if (counts.flags(keys[i]) != flags[i]) throw new AssertionError("Flag mismatch at " + i);
            if ((flags[i] & 1) == 1 && (flags[i] & 2) == 0) {
                expectedSelected++;
                expectedSelectionChecksum += i + 1L;
            }
        }
        if (round >= 0) samples.add(sample(keys, round, "m3_only", "flags_write", Provider.M3,
                (long) keys.length * FLAG_WRITE_ROUNDS, meter, observation, 0, 0));

        meter.start();
        List<Key> selected = counts.flaggedElements(1, 2, M3IndexPathMasks.Backend.JAVA);
        meter.stop();
        escaped = selected;
        long selectionChecksum = 0;
        int previous = -1;
        for (Key key : selected) {
            if (key.id <= previous || (flags[key.id] & 1) != 1 || (flags[key.id] & 2) != 0) {
                throw new AssertionError("Selection content/order mismatch");
            }
            previous = key.id;
            selectionChecksum += key.id + 1L;
        }
        if (selected.size() != expectedSelected || selectionChecksum != expectedSelectionChecksum) {
            throw new AssertionError("Selection checksum mismatch");
        }
        if (round >= 0) samples.add(sample(keys, round, "m3_only", "flags_select", Provider.M3,
                keys.length, meter, observation, selected.size(), selectionChecksum));
        checksumSink += observation.checksum ^ selectionChecksum;
    }

    private static Sample sample(Key[] keys, int round, String order, String phase,
            Provider provider, long operations, Meter meter, Observation observation,
            int selected, long selectionChecksum) {
        return new Sample(keys.length, round, order, phase, provider, operations,
                meter.wallNanos, meter.cpuNanos, meter.allocatedBytes,
                observation, selected, selectionChecksum);
    }

    private static Observation verify(Provider provider, Object value, Key[] keys) {
        long total = 0;
        long checksum = 0;
        for (Key key : keys) {
            int expected = baseCount(key);
            int actual = provider.count(value, key);
            if (actual != expected) throw new AssertionError(provider + " count at " + key.id);
            total += actual;
            checksum += (key.id + 1L) * actual;
        }
        if (provider.distinct(value) != keys.length || provider.total(value) != total) {
            throw new AssertionError(provider + " aggregate mismatch");
        }
        checksumSink += checksum;
        return new Observation(keys.length, total, checksum);
    }

    private static int baseCount(Key key) { return 1 + (key.id & 3); }

    private static Key permuted(Key[] keys, int position) {
        return keys[(position * 2053 + 17) & (keys.length - 1)];
    }

    private static void requireVersion(String group, String artifact, String expected) throws IOException {
        String resource = "/META-INF/maven/" + group + "/" + artifact + "/pom.properties";
        Properties properties = new Properties();
        try (InputStream input = CountOwnerCostProbe.class.getResourceAsStream(resource)) {
            if (input == null) throw new IllegalStateException("Missing identity " + resource);
            properties.load(input);
        }
        if (!expected.equals(properties.getProperty("version"))) {
            throw new IllegalStateException("Wrong artifact version for " + artifact + ": " + properties);
        }
    }
}
