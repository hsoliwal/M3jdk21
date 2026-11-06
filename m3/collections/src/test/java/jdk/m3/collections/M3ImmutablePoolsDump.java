// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.io.BufferedOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Reflective dump driver of the differential gate of recipe immutable-pools-20261106 (not a test, no Test suffix).
 *
 * <p>Replays the same seeded random pool, canonicalizer, packed-id and UUID operations through the classes named
 * by a package and a type prefix (the donor classes compiled from the donor tree, or the relocated jdk.m3.collections
 * classes) and prints every observable result as text, so the two dumps can be compared byte for byte. No line
 * prints a type name or an identity hash; records are printed through their accessors.</p>
 *
 * <p>usage: M3ImmutablePoolsDump &lt;package&gt; &lt;typePrefix&gt; &lt;progressMonitorSimpleName&gt; [ops] [seed]</p>
 */
public final class M3ImmutablePoolsDump {
    private static final HexFormat HEX = HexFormat.of();
    private static final String[] POOL_NAMES = {"A", "B", "C"};
    private static final int[] POOL_TYPE_IDS = {7, 9, 1};
    private static final int[] POOL_CAPACITIES = {512, 64, 4096};
    private static final int[] POOL_KEY_BOUNDS = {700, 100, 5000};
    private static final String[] POOL_POLICIES = {"RETURN_INPUT", "REJECT", null};

    private final Ref pool;
    private final Ref id;
    private final Ref policy;
    private final Ref stats;
    private final Ref weak;
    private final Ref uuid;
    private final Ref vector;
    private final Ref builder;
    private final Ref index;
    private final Ref footprint;
    private final Ref monitor;
    private final Random random;
    private final PrintStream out;
    private final Object[] pools = new Object[3];
    private final Object weakCanonicalizer;
    private final List<Object> retained = new ArrayList<>();
    private Object vectorBuilder;
    private final List<long[]> staged = new ArrayList<>();
    private long lines;

    private M3ImmutablePoolsDump(String pkg, String prefix, String monitorName, long seed, PrintStream out)
            throws ReflectiveOperationException {
        pool = new Ref(pkg + "." + prefix + "StrongValuePool");
        id = new Ref(pkg + "." + prefix + "ImmutableId");
        policy = new Ref(pkg + "." + prefix + "StrongPoolOverflowPolicy");
        stats = new Ref(pkg + "." + prefix + "StrongPoolStats");
        weak = new Ref(pkg + "." + prefix + "WeakValueCanonicalizer");
        uuid = new Ref(pkg + "." + prefix + "Uuid128");
        vector = new Ref(pkg + "." + prefix + "Uuid128Vector");
        builder = new Ref(pkg + "." + prefix + "Uuid128Vector$Builder");
        index = new Ref(pkg + "." + prefix + "Uuid128HashIndex");
        footprint = new Ref(pkg + "." + prefix + "UuidStorageFootprint");
        monitor = new Ref(pkg + "." + monitorName);
        random = new Random(seed);
        this.out = out;
        for (int i = 0; i < pools.length; i++) {
            pools[i] = POOL_POLICIES[i] == null
                ? pool.make(new Class<?>[] {int.class, int.class}, new Object[] {POOL_TYPE_IDS[i], POOL_CAPACITIES[i]})
                : pool.make(new Class<?>[] {int.class, int.class, policy.type},
                    new Object[] {POOL_TYPE_IDS[i], POOL_CAPACITIES[i], constant(POOL_POLICIES[i])});
        }
        weakCanonicalizer = weak.make(new Class<?>[0], new Object[0]);
        vectorBuilder = vector.call(null, "builder", new Class<?>[] {int.class}, new Object[] {4});
    }

    public static void main(String[] args) throws Exception {
        String pkg = args[0];
        String prefix = args[1];
        String monitorName = args[2];
        int ops = args.length > 3 ? Integer.parseInt(args[3]) : 100_000;
        long seed = args.length > 4 ? Long.parseLong(args[4]) : 20261106L;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        PrintStream sink = new PrintStream(new DigestOutputStream(new BufferedOutputStream(System.out, 1 << 16),
            digest), false, StandardCharsets.UTF_8);
        M3ImmutablePoolsDump dump = new M3ImmutablePoolsDump(pkg, prefix, monitorName, seed, sink);
        dump.constants();
        dump.refusals();
        for (int op = 0; op < ops; op++) {
            dump.step(op);
        }
        dump.flushVector(ops);
        dump.poolStats(ops);
        sink.flush();
        System.out.println("TOTAL ops=" + ops + " seed=" + seed + " lines=" + dump.lines + " sha256="
            + HEX.formatHex(digest.digest()));
        System.out.flush();
    }

    private void step(int op) {
        int kind = random.nextInt(100);
        if (kind < 30) poolOp(op);
        else if (kind < 40) weakOp(op);
        else if (kind < 50) idOp(op);
        else if (kind < 80) uuidOp(op);
        else if (kind < 95) vectorOp(op);
        else footprintOp(op);
        if (op % 2000 == 1999) {
            flushVector(op);
            poolStats(op);
        }
    }

    // ---- constants and refusals -------------------------------------------------------------------------------

    private void constants() {
        line(0, "const MAX_TYPE_ID=" + id.constant("MAX_TYPE_ID") + " MAX_LOCAL_ID=" + id.constant("MAX_LOCAL_ID")
            + " NONE.packed=" + show(id.constant("NONE")) + " NONE.present=" + show(id.call(id.constant("NONE"),
                "present", new Class<?>[0], new Object[0])));
        line(0, "const BINARY_BYTES=" + uuid.constant("BINARY_BYTES") + " TEXT_CHARS=" + uuid.constant("TEXT_CHARS")
            + " UNKNOWN=" + monitor.constant("UNKNOWN"));
        Object[] policies = policy.type.getEnumConstants();
        StringBuilder names = new StringBuilder("const policies=");
        for (Object value : policies) names.append(((Enum<?>) value).name()).append(',');
        line(0, names.toString());
        for (int i = 0; i < pools.length; i++) {
            line(0, "pool " + POOL_NAMES[i] + " typeId=" + show(call(pool, pools[i], "typeId")) + " capacity="
                + show(call(pool, pools[i], "capacity")) + " policy=" + show(call(pool, pools[i], "overflowPolicy"))
                + " size=" + show(call(pool, pools[i], "size")) + " full=" + show(call(pool, pools[i], "full")));
        }
    }

    private void refusals() {
        Class<?>[] two = {int.class, int.class};
        Class<?>[] three = {int.class, int.class, policy.type};
        line(0, "refuse pool typeId=0 -> " + show(pool.make(two, new Object[] {0, 8})));
        line(0, "refuse pool typeId=65536 -> " + show(pool.make(two, new Object[] {65536, 8})));
        line(0, "refuse pool capacity=0 -> " + show(pool.make(two, new Object[] {3, 0})));
        line(0, "refuse pool policy=null -> " + show(pool.make(three, new Object[] {3, 8, null})));
        line(0, "refuse internId(null) -> " + show(pool.call(pools[0], "internId", new Class<?>[] {Object.class},
            new Object[] {null})));
        line(0, "refuse findId(null) -> " + show(pool.call(pools[0], "findId", new Class<?>[] {Object.class},
            new Object[] {null})));
        line(0, "refuse resolve(null) -> " + show(pool.call(pools[0], "resolve", new Class<?>[] {id.type},
            new Object[] {null})));
        line(0, "refuse resolve(NONE) -> " + show(pool.call(pools[0], "resolve", new Class<?>[] {id.type},
            new Object[] {id.constant("NONE")})));
        line(0, "refuse weak.canonicalize(null) -> " + show(weak.call(weakCanonicalizer, "canonicalize",
            new Class<?>[] {Object.class}, new Object[] {null})));
        line(0, "refuse parse(null) -> " + show(uuid.call(null, "parse", new Class<?>[] {CharSequence.class},
            new Object[] {null})));
        line(0, "refuse fromUuid(null) -> " + show(uuid.call(null, "fromUuid", new Class<?>[] {UUID.class},
            new Object[] {null})));
        line(0, "refuse fromBigEndian((byte[]) null) -> " + show(uuid.call(null, "fromBigEndian",
            new Class<?>[] {byte[].class}, new Object[] {null})));
        line(0, "refuse fromBigEndian(byte[15]) -> " + show(uuid.call(null, "fromBigEndian",
            new Class<?>[] {byte[].class}, new Object[] {new byte[15]})));
        line(0, "refuse fromBigEndian(byte[20], 5) -> " + show(uuid.call(null, "fromBigEndian",
            new Class<?>[] {byte[].class, int.class}, new Object[] {new byte[20], 5})));
        line(0, "refuse fromBigEndian(byte[20], -1) -> " + show(uuid.call(null, "fromBigEndian",
            new Class<?>[] {byte[].class, int.class}, new Object[] {new byte[20], -1})));
        line(0, "refuse fromBigEndian(ByteBuffer[15]) -> " + show(uuid.call(null, "fromBigEndian",
            new Class<?>[] {ByteBuffer.class}, new Object[] {ByteBuffer.allocate(15)})));
        line(0, "refuse builder(-1) -> " + show(vector.call(null, "builder", new Class<?>[] {int.class},
            new Object[] {-1})));
        line(0, "refuse copyOf(long[2], long[3]) -> " + show(vector.call(null, "copyOf",
            new Class<?>[] {long[].class, long[].class}, new Object[] {new long[2], new long[3]})));
        line(0, "refuse forOccurrences(-1) -> " + show(footprint.call(null, "forOccurrences",
            new Class<?>[] {long.class}, new Object[] {-1L})));
        line(0, "refuse forOccurrences(MAX/15) -> " + show(footprint.call(null, "forOccurrences",
            new Class<?>[] {long.class}, new Object[] {Long.MAX_VALUE / 15})));
        line(0, "refuse index.build(null, none) -> " + show(index.call(null, "build",
            new Class<?>[] {vector.type, monitor.type}, new Object[] {null, none()})));
        Object empty = vector.call(null, "copyOf", new Class<?>[] {long[].class, long[].class},
            new Object[] {new long[0], new long[0]});
        Object emptyIndex = index.call(null, "build", new Class<?>[] {vector.type, monitor.type},
            new Object[] {empty, none()});
        line(0, "empty index size=" + show(call(index, emptyIndex, "size")) + " capacity="
            + show(call(index, emptyIndex, "capacity")) + " payload=" + show(call(index, emptyIndex,
                "primitivePayloadBytes")) + " firstPosition(0,0)=" + show(index.call(emptyIndex, "firstPosition",
                    new Class<?>[] {long.class, long.class}, new Object[] {0L, 0L})));
        Object one = vector.call(null, "copyOf", new Class<?>[] {long[].class, long[].class},
            new Object[] {new long[] {1L}, new long[] {2L}});
        line(0, "cancelled build -> " + show(index.call(null, "build", new Class<?>[] {vector.type, monitor.type},
            new Object[] {one, countingMonitor(new long[4], true)})) + " emptyCancelled="
            + show(index.call(null, "build", new Class<?>[] {vector.type, monitor.type},
                new Object[] {empty, countingMonitor(new long[4], true)})));
    }

    // ---- pools ------------------------------------------------------------------------------------------------

    private void poolOp(int op) {
        int which = random.nextInt(pools.length);
        Object target = pools[which];
        String name = POOL_NAMES[which];
        String key = "k" + random.nextInt(POOL_KEY_BOUNDS[which]);
        switch (random.nextInt(6)) {
            case 0, 1 -> line(op, "internId " + name + " " + key + " -> " + idText(pool.call(target, "internId",
                new Class<?>[] {Object.class}, new Object[] {key})));
            case 2 -> {
                Object result = pool.call(target, "canonicalize", new Class<?>[] {Object.class}, new Object[] {key});
                Object found = pool.call(target, "findId", new Class<?>[] {Object.class}, new Object[] {key});
                line(op, "canonicalize " + name + " " + key + " -> " + show(result) + " sameObjectAsInput="
                    + (result == key) + " findId=" + idText(found));
            }
            case 3 -> line(op, "findId " + name + " " + key + " -> " + idText(pool.call(target, "findId",
                new Class<?>[] {Object.class}, new Object[] {key})));
            case 4 -> {
                int size = (Integer) call(pool, target, "size");
                long local = random.nextInt(size + 3) - 1;
                Object probe = id.call(null, "of", new Class<?>[] {int.class, long.class},
                    new Object[] {POOL_TYPE_IDS[which], Math.max(0L, local)});
                line(op, "resolve " + name + " local=" + Math.max(0L, local) + " -> " + show(pool.call(target,
                    "resolve", new Class<?>[] {id.type}, new Object[] {probe})));
            }
            default -> {
                int other = (which + 1) % pools.length;
                Object foreign = id.call(null, "of", new Class<?>[] {int.class, long.class},
                    new Object[] {POOL_TYPE_IDS[other], 0L});
                line(op, "resolve " + name + " foreign(" + POOL_NAMES[other] + ") -> " + show(pool.call(target,
                    "resolve", new Class<?>[] {id.type}, new Object[] {foreign})));
            }
        }
    }

    private void poolStats(int op) {
        for (int i = 0; i < pools.length; i++) {
            Object snapshot = call(pool, pools[i], "stats");
            line(op, "stats " + POOL_NAMES[i] + " hits=" + show(call(stats, snapshot, "hits")) + " inserts="
                + show(call(stats, snapshot, "inserts")) + " overflows=" + show(call(stats, snapshot, "overflows"))
                + " entries=" + show(call(stats, snapshot, "entries")) + " capacity="
                + show(call(stats, snapshot, "capacity")) + " requests=" + show(call(stats, snapshot, "requests"))
                + " reuseRatio=" + show(call(stats, snapshot, "reuseRatio")) + " size="
                + show(call(pool, pools[i], "size")) + " full=" + show(call(pool, pools[i], "full")));
        }
        line(op, "weak hits=" + show(call(weak, weakCanonicalizer, "hits")) + " inserts="
            + show(call(weak, weakCanonicalizer, "inserts")) + " approximateSize="
            + show(call(weak, weakCanonicalizer, "approximateSize")) + " retained=" + retained.size());
    }

    private void weakOp(int op) {
        if (random.nextInt(400) == 0) {
            call(weak, weakCanonicalizer, "clear");
            retained.clear();
            line(op, "weak clear approximateSize=" + show(call(weak, weakCanonicalizer, "approximateSize")));
            return;
        }
        String value = "w" + random.nextInt(200);
        Object result = weak.call(weakCanonicalizer, "canonicalize", new Class<?>[] {Object.class},
            new Object[] {value});
        retained.add(value);
        line(op, "weak " + value + " -> " + show(result) + " sameObjectAsInput=" + (result == value) + " hits="
            + show(call(weak, weakCanonicalizer, "hits")) + " inserts=" + show(call(weak, weakCanonicalizer,
                "inserts")) + " approximateSize=" + show(call(weak, weakCanonicalizer, "approximateSize")));
    }

    // ---- packed ids -------------------------------------------------------------------------------------------

    private void idOp(int op) {
        int[] typeIds = {-1, 0, 1, 7, 65535, 65536, random.nextInt(70000) - 1000};
        long[] locals = {-1L, 0L, random.nextLong() & 0x0000_ffff_ffff_ffffL, 0x0000_ffff_ffff_ffffL,
            0x0001_0000_0000_0000L, random.nextLong()};
        int typeId = typeIds[random.nextInt(typeIds.length)];
        long local = locals[random.nextInt(locals.length)];
        Object made = id.call(null, "of", new Class<?>[] {int.class, long.class}, new Object[] {typeId, local});
        if (made instanceof String) {
            line(op, "id.of " + typeId + " " + local + " -> " + made);
            return;
        }
        Object other = id.call(null, "of", new Class<?>[] {int.class, long.class},
            new Object[] {1 + random.nextInt(65535), random.nextLong() & 0x0000_ffff_ffff_ffffL});
        line(op, "id.of " + typeId + " " + local + " -> packed=" + show(made) + " typeId=" + show(call(id, made,
            "typeId")) + " localId=" + show(call(id, made, "localId")) + " present=" + show(call(id, made,
                "present")) + " compareTo(" + show(other) + ")=" + Integer.signum((Integer) id.call(made,
                    "compareTo", new Class<?>[] {id.type}, new Object[] {other})) + " same="
            + show(id.call(null, "same", new Class<?>[] {long.class, long.class},
                new Object[] {packed(made), packed(other)}))
            + " equalsSelf=" + made.equals(id.call(null, "of", new Class<?>[] {int.class, long.class},
                new Object[] {typeId, local})) + " hash=" + made.hashCode());
    }

    // ---- UUID -------------------------------------------------------------------------------------------------

    private void uuidOp(int op) {
        long[] specials = {0L, -1L, Long.MIN_VALUE, Long.MAX_VALUE, 0x8000_0000_0000_0000L, 0x4000_0000_0000_0000L,
            0xc000_0000_0000_0000L, 0xe000_0000_0000_0000L, 0x0000_0000_0000_f000L};
        long msb = random.nextInt(4) == 0 ? specials[random.nextInt(specials.length)] : random.nextLong();
        long lsb = random.nextInt(4) == 0 ? specials[random.nextInt(specials.length)] : random.nextLong();
        Object value = uuid.make(new Class<?>[] {long.class, long.class}, new Object[] {msb, lsb});
        String canonical = (String) call(uuid, value, "toCanonicalString");
        byte[] bytes = (byte[]) call(uuid, value, "toBigEndianBytes");
        switch (random.nextInt(5)) {
            case 0 -> line(op, "uuid " + canonical + " toString=" + value + " version=" + show(call(uuid, value,
                "version")) + " variant=" + show(call(uuid, value, "variant")) + " bytes=" + HEX.formatHex(bytes)
                + " msb=" + show(call(uuid, value, "mostSignificantBits")) + " lsb=" + show(call(uuid, value,
                    "leastSignificantBits")) + " hash=" + value.hashCode());
            case 1 -> {
                int offset = random.nextInt(17);
                byte[] target = new byte[32];
                Object written = uuid.call(value, "writeBigEndian", new Class<?>[] {byte[].class, int.class},
                    new Object[] {target, offset});
                Object back = uuid.call(null, "fromBigEndian", new Class<?>[] {byte[].class, int.class},
                    new Object[] {target, offset});
                line(op, "uuid bytes " + canonical + " offset=" + offset + " written=" + show(written) + " target="
                    + HEX.formatHex(target) + " back=" + show(back) + " equals=" + value.equals(back)
                    + " fromBytes=" + show(uuid.call(null, "fromBigEndian", new Class<?>[] {byte[].class},
                        new Object[] {bytes})) + " badOffset=" + show(uuid.call(value, "writeBigEndian",
                            new Class<?>[] {byte[].class, int.class}, new Object[] {target, 17})));
            }
            case 2 -> {
                ByteBuffer little = ByteBuffer.allocate(20).order(ByteOrder.LITTLE_ENDIAN);
                little.position(random.nextInt(5));
                int start = little.position();
                Object written = uuid.call(value, "writeBigEndian", new Class<?>[] {ByteBuffer.class},
                    new Object[] {little});
                little.position(start);
                Object back = uuid.call(null, "fromBigEndian", new Class<?>[] {ByteBuffer.class},
                    new Object[] {little});
                ByteBuffer small = ByteBuffer.allocate(20);
                small.position(5);
                line(op, "uuid buffer " + canonical + " start=" + start + " written=" + show(written) + " array="
                    + HEX.formatHex(little.array()) + " position=" + little.position() + " back=" + show(back)
                    + " equals=" + value.equals(back) + " short=" + show(uuid.call(value, "writeBigEndian",
                        new Class<?>[] {ByteBuffer.class}, new Object[] {small})));
            }
            case 3 -> {
                Object parsed = uuid.call(null, "parse", new Class<?>[] {CharSequence.class},
                    new Object[] {canonical});
                Object upper = uuid.call(null, "parse", new Class<?>[] {CharSequence.class},
                    new Object[] {canonical.toUpperCase(java.util.Locale.ROOT)});
                Object builderParse = uuid.call(null, "parse", new Class<?>[] {CharSequence.class},
                    new Object[] {new StringBuilder(canonical)});
                UUID jdk = (UUID) call(uuid, value, "toUuid");
                Object fromJdk = uuid.call(null, "fromUuid", new Class<?>[] {UUID.class}, new Object[] {jdk});
                Object other = uuid.make(new Class<?>[] {long.class, long.class},
                    new Object[] {random.nextLong(), random.nextLong()});
                line(op, "uuid parse " + canonical + " parsed=" + value.equals(parsed) + " upper="
                    + value.equals(upper) + " charSequence=" + value.equals(builderParse) + " jdk=" + jdk
                    + " jdkText=" + canonical.equals(jdk.toString()) + " fromUuid=" + value.equals(fromJdk)
                    + " compareTo(" + other + ")=" + Integer.signum((Integer) uuid.call(value, "compareTo",
                        new Class<?>[] {uuid.type}, new Object[] {other})) + " jdkCompare="
                    + Integer.signum(jdk.compareTo((UUID) call(uuid, other, "toUuid"))) + " textCompare="
                    + Integer.signum(canonical.compareTo(other.toString())));
            }
            default -> {
                String[] bad = {canonical.substring(1), canonical + "0", canonical.replace('-', ':'),
                    canonical.substring(0, 8) + canonical.substring(9) + "-", "g" + canonical.substring(1),
                    canonical.substring(0, 35) + "G", canonical.replace(canonical.charAt(0), '\u00e9'), ""};
                String text = bad[random.nextInt(bad.length)];
                line(op, "uuid parse-bad " + show(text) + " -> " + show(uuid.call(null, "parse",
                    new Class<?>[] {CharSequence.class}, new Object[] {text})));
            }
        }
    }

    // ---- vector and hash index --------------------------------------------------------------------------------

    private void vectorOp(int op) {
        long msb = random.nextInt(3) == 0 && !staged.isEmpty() ? staged.get(random.nextInt(staged.size()))[0]
            : random.nextLong();
        long lsb = random.nextInt(3) == 0 && !staged.isEmpty() ? staged.get(random.nextInt(staged.size()))[1]
            : random.nextLong();
        Object value = uuid.make(new Class<?>[] {long.class, long.class}, new Object[] {msb, lsb});
        Object result = switch (random.nextInt(3)) {
            case 0 -> builder.call(vectorBuilder, "add", new Class<?>[] {uuid.type}, new Object[] {value});
            case 1 -> builder.call(vectorBuilder, "add", new Class<?>[] {UUID.class},
                new Object[] {new UUID(msb, lsb)});
            default -> builder.call(vectorBuilder, "add", new Class<?>[] {long.class, long.class},
                new Object[] {msb, lsb});
        };
        staged.add(new long[] {msb, lsb});
        line(op, "vector add " + value + " returnsBuilder=" + (result == vectorBuilder) + " staged=" + staged.size());
    }

    private void flushVector(int op) {
        Object built = call(builder, vectorBuilder, "build");
        int size = (Integer) call(vector, built, "size");
        line(op, "vector build size=" + size + " staged=" + staged.size() + " payloadBytes=" + show(call(vector, built,
            "payloadBytes")));
        for (int i = 0; i < size; i += Math.max(1, size / 16)) {
            Object at = vector.call(built, "get", new Class<?>[] {int.class}, new Object[] {i});
            Object other = uuid.make(new Class<?>[] {long.class, long.class},
                new Object[] {staged.get(i)[0] ^ 1L, staged.get(i)[1]});
            byte[] target = new byte[24];
            vector.call(built, "writeBigEndian", new Class<?>[] {int.class, byte[].class, int.class},
                new Object[] {i, target, 3});
            ByteBuffer buffer = ByteBuffer.allocate(16);
            vector.call(built, "writeBigEndian", new Class<?>[] {int.class, ByteBuffer.class},
                new Object[] {i, buffer});
            line(op, "vector at " + i + " get=" + show(at) + " msb=" + show(vector.call(built, "mostSignificantBitsAt",
                new Class<?>[] {int.class}, new Object[] {i})) + " lsb=" + show(vector.call(built,
                    "leastSignificantBitsAt", new Class<?>[] {int.class}, new Object[] {i})) + " toUuid="
                + show(vector.call(built, "toUuid", new Class<?>[] {int.class}, new Object[] {i})) + " equalsAt="
                + show(vector.call(built, "equalsAt", new Class<?>[] {int.class, uuid.type}, new Object[] {i, at}))
                + " equalsAtOther=" + show(vector.call(built, "equalsAt", new Class<?>[] {int.class, uuid.type},
                    new Object[] {i, other})) + " indexOf=" + show(vector.call(built, "indexOf",
                        new Class<?>[] {uuid.type}, new Object[] {at})) + " indexOfOther=" + show(vector.call(built,
                            "indexOf", new Class<?>[] {uuid.type}, new Object[] {other})) + " bytes="
                + HEX.formatHex(target) + " buffer=" + HEX.formatHex(buffer.array()));
        }
        line(op, "vector get(size) -> " + show(vector.call(built, "get", new Class<?>[] {int.class},
            new Object[] {size})) + " equalsAt(-1) -> " + show(vector.call(built, "equalsAt",
                new Class<?>[] {int.class, uuid.type}, new Object[] {-1, uuid.make(new Class<?>[] {long.class,
                    long.class}, new Object[] {0L, 0L})})));
        hashIndex(op, built, size);
        vectorBuilder = vector.call(null, "builder", new Class<?>[] {int.class}, new Object[] {random.nextInt(64)});
        staged.clear();
    }

    private void hashIndex(int op, Object built, int size) {
        long[] counts = new long[4];
        Object monitorProxy = countingMonitor(counts, false);
        Object idx = index.call(null, "build", new Class<?>[] {vector.type, monitor.type},
            new Object[] {built, monitorProxy});
        line(op, "index build size=" + show(call(index, idx, "size")) + " capacity=" + show(call(index, idx,
            "capacity")) + " payload=" + show(call(index, idx, "primitivePayloadBytes")) + " monitor begins="
            + counts[0] + " total=" + counts[1] + " worked=" + counts[2] + " done=" + counts[3]);
        for (int i = 0; i < size; i += Math.max(1, size / 32)) {
            Object at = vector.call(built, "get", new Class<?>[] {int.class}, new Object[] {i});
            line(op, "index at " + i + " " + show(at) + " firstPosition=" + show(index.call(idx, "firstPosition",
                new Class<?>[] {uuid.type}, new Object[] {at})) + " firstPositionBits=" + show(index.call(idx,
                    "firstPosition", new Class<?>[] {long.class, long.class}, new Object[] {staged.get(i)[0],
                        staged.get(i)[1]})) + " contains=" + show(index.call(idx, "contains",
                            new Class<?>[] {uuid.type}, new Object[] {at})) + " vectorIndexOf="
                + show(vector.call(built, "indexOf", new Class<?>[] {uuid.type}, new Object[] {at})));
        }
        for (int i = 0; i < 4; i++) {
            Object absent = uuid.make(new Class<?>[] {long.class, long.class},
                new Object[] {random.nextLong(), random.nextLong()});
            line(op, "index absent " + show(absent) + " firstPosition=" + show(index.call(idx, "firstPosition",
                new Class<?>[] {uuid.type}, new Object[] {absent})) + " contains=" + show(index.call(idx, "contains",
                    new Class<?>[] {uuid.type}, new Object[] {absent})));
        }
        line(op, "index firstPosition(null) -> " + show(index.call(idx, "firstPosition", new Class<?>[] {uuid.type},
            new Object[] {null})));
    }

    private void footprintOp(int op) {
        long[] occurrences = {0L, 1L, 16L, 10_000_000L, random.nextInt(1 << 20), Long.MAX_VALUE / 36,
            Long.MAX_VALUE / 16, Long.MAX_VALUE};
        long count = occurrences[random.nextInt(occurrences.length)];
        Object result = footprint.call(null, "forOccurrences", new Class<?>[] {long.class}, new Object[] {count});
        if (result instanceof String) {
            line(op, "footprint " + count + " -> " + result);
            return;
        }
        line(op, "footprint " + count + " occurrences=" + show(call(footprint, result, "occurrences"))
            + " binaryBytes=" + show(call(footprint, result, "binaryBytes")) + " canonicalAsciiBytes="
            + show(call(footprint, result, "canonicalAsciiBytes")) + " savedBytes=" + show(call(footprint, result,
                "savedBytes")) + " textToBinaryRatio=" + show(call(footprint, result, "textToBinaryRatio")));
    }

    // ---- helpers ----------------------------------------------------------------------------------------------

    private Object none() {
        return monitor.call(null, "none", new Class<?>[0], new Object[0]);
    }

    /** A proxy of the donor's progress contract: counts begin/total/worked/done, optionally cancelled. */
    private Object countingMonitor(long[] counts, boolean cancelled) {
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "begin" -> { counts[0]++; counts[1] += (Long) args[1]; yield null; }
            case "worked" -> { counts[2] += (Long) args[0]; yield null; }
            case "done" -> { counts[3]++; yield null; }
            case "cancelled" -> cancelled;
            case "hashCode" -> 0;
            case "equals" -> proxy == args[0];
            case "toString" -> "countingMonitor";
            default -> InvocationHandler.invokeDefault(proxy, method, args);
        };
        return Proxy.newProxyInstance(monitor.type.getClassLoader(), new Class<?>[] {monitor.type}, handler);
    }

    private Object constant(String name) {
        for (Object value : policy.type.getEnumConstants()) {
            if (((Enum<?>) value).name().equals(name)) return value;
        }
        throw new IllegalArgumentException(name);
    }

    private static Object call(Ref ref, Object target, String name) {
        return ref.call(target, name, new Class<?>[0], new Object[0]);
    }

    private long packed(Object identity) {
        return (Long) call(id, identity, "packed");
    }

    private String idText(Object result) {
        if (result instanceof String) return (String) result;
        return "packed=" + packed(result) + " present=" + show(call(id, result, "present")) + " typeId="
            + show(call(id, result, "typeId")) + " localId=" + show(call(id, result, "localId"));
    }

    /** Text of a result that never names a type: records through accessors, enums by name, bytes as hex. */
    private String show(Object result) {
        if (result == null) return "null";
        if (id.type.isInstance(result)) return Long.toString(packed(result));
        if (uuid.type.isInstance(result)) return result.toString();
        if (result instanceof Enum<?> value) return value.name();
        if (result instanceof byte[] bytes) return HEX.formatHex(bytes);
        if (result instanceof String text && !text.startsWith("!")) return "\"" + text + "\"";
        if (vector.type.isInstance(result) || index.type.isInstance(result) || builder.type.isInstance(result)) {
            return "<object>";
        }
        return String.valueOf(result);
    }

    private void line(int op, String text) {
        lines++;
        out.print(op);
        out.print(' ');
        out.print(text);
        out.print('\n');
    }

    /** One reflected donor or relocated class; a thrown exception becomes the text "!<SimpleName>: <message>". */
    private static final class Ref {
        final Class<?> type;

        Ref(String name) throws ClassNotFoundException {
            type = Class.forName(name);
        }

        Object call(Object target, String name, Class<?>[] types, Object[] args) {
            try {
                Method method = type.getMethod(name, types);
                return method.invoke(target, args);
            } catch (InvocationTargetException failure) {
                return thrown(failure.getCause());
            } catch (ReflectiveOperationException | IllegalArgumentException failure) {
                throw new IllegalStateException(type.getName() + "." + name, failure);
            }
        }

        Object make(Class<?>[] types, Object[] args) {
            try {
                return type.getConstructor(types).newInstance(args);
            } catch (InvocationTargetException failure) {
                return thrown(failure.getCause());
            } catch (ReflectiveOperationException failure) {
                throw new IllegalStateException(type.getName() + ".<init>", failure);
            }
        }

        Object constant(String name) {
            try {
                return type.getField(name).get(null);
            } catch (ReflectiveOperationException failure) {
                throw new IllegalStateException(type.getName() + "." + name, failure);
            }
        }

        static String thrown(Throwable cause) {
            return "!" + cause.getClass().getSimpleName() + ": " + cause.getMessage();
        }
    }
}
