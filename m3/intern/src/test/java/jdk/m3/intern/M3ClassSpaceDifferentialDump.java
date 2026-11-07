// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.intern;

import java.io.BufferedOutputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

/**
 * Differential driver of the class-space relocation (recipe class-space-20261107, gate 3; M3JDK-authored, test
 * scope). One seeded operation stream drives M3ClassSpace (intern, internRegion, internUtf8Batch, copy, copyInto,
 * digestHex) against a reference model (a HashMap from the exact bytes to the first-admission ordinal plus the
 * owner's byte budget: a duplicate is found before the budget check, a distinct over-budget image is refused and
 * changes nothing, an all-or-nothing batch either publishes every new symbol in constant-pool order or nothing), a
 * forced-collision owner (constant digest: full bytes decide equality), the M3ClassSpaceArchive round trip (freeze,
 * openCopy / openCanonicalCopy, restore and re-freeze, a file written, mapped read-only, opened zero-copy, pinned,
 * verified, snapshotted, and reopened from the file bytes: every frame byte-identical), single-byte corruptions of
 * the frame (each refused), and M3ClassFileAtoms over real JDK class files from jrt (serial and atomic interning
 * agree, every symbol reads back exactly; truncated images refused before any store mutation).
 *
 * <p>The dump prints ordinals, lengths, SHA-256 digests and exception messages, never a class or package name, so
 * differential.sh compiles it a second time for the donor with only its package line replaced and both runs (also
 * under -Xint) must print byte-identical dumps. Every model disagreement throws. Lines end with a single LF.</p>
 *
 * <p>usage: java M3ClassSpaceDifferentialDump [operations, default 100000]</p>
 */
public final class M3ClassSpaceDifferentialDump {
    private static final long SEED = 0x20261107434c5350L;
    private static final String[] JDK_CLASSES = {
        "/java/lang/Object.class", "/java/lang/String.class", "/java/util/HashMap.class",
        "/java/util/concurrent/ConcurrentHashMap.class", "/java/lang/invoke/MethodHandle.class",
        "/java/util/regex/Pattern.class", "/java/lang/Record.class", "/java/lang/Math.class"
    };

    private final PrintStream out;
    private final SplittableRandom random = new SplittableRandom(SEED);
    private long checks;

    private M3ClassSpaceDifferentialDump(PrintStream out) {
        this.out = out;
    }

    public static void main(String[] args) throws Exception {
        int operations = args.length > 0 ? Integer.parseInt(args[0]) : 100_000;
        PrintStream stdout = new PrintStream(new BufferedOutputStream(new FileOutputStream(FileDescriptor.out), 1 << 16),
                false, StandardCharsets.UTF_8);
        M3ClassSpaceDifferentialDump dump = new M3ClassSpaceDifferentialDump(stdout);
        dump.serialStream("serial", 1 << 20, operations);
        dump.serialStream("budget", 4096, operations / 4);
        dump.collisions();
        M3ClassSpace owner = dump.regionAndBatches(operations / 10);
        dump.archive(owner);
        dump.classFiles();
        dump.line("CHECKS " + dump.checks);
        dump.line("RESULT PASS");
        stdout.flush();
    }

    private void line(String text) {
        out.print(text);
        out.print('\n');
    }

    private void check(boolean truth, String what) {
        checks++;
        if (!truth) {
            out.flush();
            throw new AssertionError("model disagreement: " + what);
        }
    }

    private byte[] atom(List<byte[]> seen) {
        int kind = random.nextInt(10);
        if (kind < 4 && !seen.isEmpty()) {
            return seen.get(random.nextInt(seen.size())).clone();
        }
        if (kind == 4 && !seen.isEmpty()) {
            byte[] mutated = seen.get(random.nextInt(seen.size())).clone();
            if (mutated.length > 0) {
                mutated[random.nextInt(mutated.length)] ^= (byte) (1 + random.nextInt(255));
            }
            return mutated;
        }
        byte[] fresh = new byte[kind == 5 ? 0 : random.nextInt(kind == 9 ? 300 : 24)];
        for (int i = 0; i < fresh.length; i++) {
            fresh[i] = (byte) (kind == 6 ? random.nextInt(3) : random.nextInt(256));
        }
        return fresh;
    }

    /** Serial intern stream against the exact-bytes model with an owner byte budget. */
    private void serialStream(String label, int maxBytes, int operations) {
        M3ClassSpace owner = new M3ClassSpace(maxBytes);
        Map<ByteBuffer, Integer> model = new HashMap<>();
        List<byte[]> seen = new ArrayList<>();
        int modelBytes = 0;
        int refused = 0;
        MessageDigest trace = sha256();
        for (int op = 0; op < operations; op++) {
            byte[] image = atom(seen);
            byte[] before = image.clone();
            Integer known = model.get(ByteBuffer.wrap(before));
            int id;
            try {
                id = owner.intern(image);
            } catch (IllegalStateException refusal) {
                check(known == null && image.length > maxBytes - modelBytes, label + " refusal at " + op);
                check(owner.imageCount() == model.size() && owner.retainedByteCount() == modelBytes,
                        label + " refusal changed the owner at " + op);
                refused++;
                trace.update((byte) 'R');
                continue;
            }
            check(Arrays.equals(image, before), label + " input changed at " + op);
            if (known == null) {
                check(id == model.size(), label + " new ordinal at " + op);
                model.put(ByteBuffer.wrap(before), id);
                modelBytes += before.length;
                seen.add(before);
            } else {
                check(id == known, label + " duplicate ordinal at " + op);
            }
            check(owner.imageCount() == model.size() && owner.retainedByteCount() == modelBytes,
                    label + " counters at " + op);
            trace.update(intBytes(id));
            if (op % 997 == 0) {
                line(label + " op=" + op + " id=" + id + " length=" + before.length + " images="
                        + owner.imageCount() + " bytes=" + owner.retainedByteCount());
            }
        }
        MessageDigest contents = sha256();
        for (int id = 0; id < owner.imageCount(); id++) {
            byte[] copy = owner.copy(id);
            check(model.get(ByteBuffer.wrap(copy)) == id, label + " copy " + id);
            check(owner.digestHex(id).equals(HexFormat.of().formatHex(sha256().digest(copy))), label + " digest " + id);
            contents.update(owner.digestHex(id).getBytes(StandardCharsets.US_ASCII));
        }
        line(label + " operations=" + operations + " images=" + owner.imageCount() + " bytes="
                + owner.retainedByteCount() + " max=" + owner.maxByteCount() + " refused=" + refused
                + " trace=" + hex(trace) + " digests=" + hex(contents));
    }

    /** Constant digest: every candidate collides, full bytes decide equality, bucket rebuilds keep ordinals. */
    private void collisions() {
        byte[] constant = new byte[32];
        Arrays.fill(constant, (byte) 0x5a);
        M3ClassSpace owner = new M3ClassSpace(1 << 16, bytes -> constant.clone());
        for (int round = 0; round < 3; round++) {
            for (int i = 0; i < 300; i++) {
                byte[] value = {(byte) i, (byte) (i >>> 8), (byte) (i % 7)};
                check(owner.intern(value) == i, "collision ordinal " + i + " round " + round);
            }
        }
        check(owner.imageCount() == 300 && owner.retainedByteCount() == 900, "collision counters");
        byte[] frame = M3ClassSpaceArchive.freeze(owner);
        line("collisions images=" + owner.imageCount() + " bytes=" + owner.retainedByteCount() + " digest0="
                + owner.digestHex(0) + " archive=" + frame.length + " sha256=" + sha256hex(frame));
    }

    /** internRegion and all-or-nothing internUtf8Batch against the same model (constant-pool order). */
    private M3ClassSpace regionAndBatches(int batches) {
        int maxBytes = 1 << 20;
        M3ClassSpace owner = new M3ClassSpace(maxBytes);
        Map<ByteBuffer, Integer> model = new HashMap<>();
        int modelBytes = 0;
        int refusedRegions = 0;
        int refusedBatches = 0;
        int invalid = 0;
        MessageDigest trace = sha256();
        for (int b = 0; b < batches; b++) {
            byte[] source = new byte[16 + random.nextInt(512)];
            for (int i = 0; i < source.length; i++) {
                source[i] = (byte) random.nextInt(random.nextBoolean() ? 4 : 256);
            }
            int count = 1 + random.nextInt(40);
            byte[] tags = new byte[count];
            int[] offsets = new int[count];
            int[] lengths = new int[count];
            for (int cp = 1; cp < count; cp++) {
                tags[cp] = (byte) (random.nextInt(4) == 0 ? 7 : 1);
                offsets[cp] = random.nextInt(source.length);
                lengths[cp] = random.nextInt(Math.min(24, source.length - offsets[cp]) + 1);
            }
            if (random.nextInt(8) == 0) {
                int cp = 1 + random.nextInt(Math.max(1, count - 1));
                if (cp < count) {
                    tags[cp] = 1;
                    lengths[cp] = source.length + 1;
                }
            }
            boolean region = random.nextBoolean();
            int[] expected = new int[count];
            Arrays.fill(expected, -1);
            Map<ByteBuffer, Integer> staged = new HashMap<>();
            int stagedBytes = 0;
            boolean bad = false;
            for (int cp = 1; cp < count; cp++) {
                if (tags[cp] != 1) continue;
                if (offsets[cp] + lengths[cp] > source.length) {
                    bad = true;
                    break;
                }
                ByteBuffer key = ByteBuffer.wrap(Arrays.copyOfRange(source, offsets[cp], offsets[cp] + lengths[cp]));
                Integer known = model.get(key);
                if (known == null) known = staged.get(key);
                if (known == null) {
                    known = model.size() + staged.size();
                    staged.put(key, known);
                    stagedBytes += lengths[cp];
                }
                expected[cp] = known;
            }
            boolean overBudget = !bad && stagedBytes > maxBytes - modelBytes;
            if (region) {
                for (int cp = 1; cp < count; cp++) {
                    if (tags[cp] != 1) continue;
                    boolean inRange = offsets[cp] + lengths[cp] <= source.length;
                    ByteBuffer key = inRange
                            ? ByteBuffer.wrap(Arrays.copyOfRange(source, offsets[cp], offsets[cp] + lengths[cp])) : null;
                    Integer known = inRange ? model.get(key) : null;
                    try {
                        int id = owner.internRegion(source, offsets[cp], lengths[cp]);
                        check(inRange, "region accepted out of range batch " + b);
                        if (known == null) {
                            check(id == model.size(), "region new ordinal batch " + b);
                            model.put(key, id);
                            modelBytes += lengths[cp];
                        } else {
                            check(id == known, "region duplicate batch " + b);
                        }
                        trace.update(intBytes(id));
                    } catch (IndexOutOfBoundsException range) {
                        check(!inRange, "region range batch " + b);
                        invalid++;
                        trace.update((byte) 'X');
                    } catch (IllegalStateException budget) {
                        check(inRange && known == null && lengths[cp] > maxBytes - modelBytes, "region budget batch " + b);
                        refusedRegions++;
                        trace.update((byte) 'R');
                    }
                    check(owner.imageCount() == model.size() && owner.retainedByteCount() == modelBytes,
                            "region counters batch " + b);
                }
                continue;
            }
            try {
                int[] ids = owner.internUtf8Batch(source, tags, offsets, lengths);
                check(!bad && !overBudget, "batch accepted batch " + b);
                check(Arrays.equals(ids, expected), "batch ids batch " + b);
                for (Map.Entry<ByteBuffer, Integer> entry : staged.entrySet()) {
                    model.put(entry.getKey(), entry.getValue());
                }
                modelBytes += stagedBytes;
                for (int id : ids) trace.update(intBytes(id));
            } catch (IndexOutOfBoundsException range) {
                check(bad, "batch range batch " + b);
                invalid++;
                trace.update((byte) 'X');
            } catch (IllegalStateException budget) {
                check(overBudget, "batch budget batch " + b);
                refusedBatches++;
                trace.update((byte) 'R');
            }
            check(owner.imageCount() == model.size() && owner.retainedByteCount() == modelBytes,
                    "batch counters batch " + b);
        }
        byte[] destination = new byte[64];
        int copied = 0;
        for (int id = 0; id < owner.imageCount(); id++) {
            byte[] copy = owner.copy(id);
            check(model.get(ByteBuffer.wrap(copy)) == id, "region copy " + id);
            if (copy.length <= destination.length - 8) {
                Arrays.fill(destination, (byte) -1);
                check(owner.copyInto(id, destination, 8, copy.length) == copy.length
                        && Arrays.equals(destination, 8, 8 + copy.length, copy, 0, copy.length), "copyInto " + id);
                copied++;
            }
        }
        try {
            owner.copyInto(0, destination, 60, 64);
            check(owner.copy(0).length <= 4, "copyInto destination bound");
        } catch (IndexOutOfBoundsException expected) {
            check(owner.copy(0).length > 4, "copyInto destination refusal");
        }
        line("regions batches=" + batches + " images=" + owner.imageCount() + " bytes=" + owner.retainedByteCount()
                + " refusedRegions=" + refusedRegions + " refusedBatches=" + refusedBatches + " invalid=" + invalid
                + " copyInto=" + copied + " trace=" + hex(trace));
        M3ClassSpace.Binding binding = owner.bind(String.class, 0);
        line("binding id=" + binding.imageId() + " runtimeClassIsString=" + (binding.runtimeClass() == String.class));
        return owner;
    }

    /** Archive round trip in memory and through a mapped file; every frame byte-identical; corruptions refused. */
    private void archive(M3ClassSpace owner) throws IOException {
        byte[] frame = M3ClassSpaceArchive.freeze(owner);
        line("archive images=" + owner.imageCount() + " payload=" + owner.retainedByteCount() + " frame=" + frame.length
                + " sha256=" + sha256hex(frame));
        M3ClassSpaceArchive.View copy = M3ClassSpaceArchive.openCopy(frame);
        M3ClassSpaceArchive.View canonical = M3ClassSpaceArchive.openCanonicalCopy(frame);
        check(copy.count() == owner.imageCount() && canonical.payloadBytes() == owner.retainedByteCount(), "view counters");
        for (int id = 0; id < owner.imageCount(); id++) {
            check(Arrays.equals(copy.copyAt(id), owner.copy(id)), "view entry " + id);
            check(canonical.viewAt(id).remaining() == owner.copy(id).length, "view slice " + id);
        }
        byte[] restored = M3ClassSpaceArchive.freeze(canonical.restore());
        check(Arrays.equals(restored, frame), "restore round trip");
        Path file = Files.createTempFile("m3-classspace-dump-", ".img");
        try {
            Files.write(file, frame);
            try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
                MappedByteBuffer mapped = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
                M3ClassSpaceArchive.View zeroCopy = M3ClassSpaceArchive.openCanonicalTrustedMapped(mapped.asReadOnlyBuffer());
                for (int id = 0; id < owner.imageCount(); id++) {
                    check(Arrays.equals(zeroCopy.copyAt(id), owner.copy(id)), "mapped entry " + id);
                }
                M3ClassSpaceArchive.PinnedView pinned =
                        M3ClassSpaceArchive.openPinnedCanonicalTrustedMapped(mapped.asReadOnlyBuffer());
                pinned.verifyRoot();
                for (int id = 0; id < pinned.count(); id++) {
                    check(Arrays.equals(pinned.copyVerifiedAt(id), owner.copy(id)), "pinned entry " + id);
                }
                ByteBuffer snapshot = pinned.snapshotCanonicalCopy().readOnlyImage();
                byte[] snapshotBytes = new byte[snapshot.remaining()];
                snapshot.get(snapshotBytes);
                check(Arrays.equals(snapshotBytes, frame), "pinned snapshot frame");
            }
            byte[] reread = Files.readAllBytes(file);
            byte[] reopened = M3ClassSpaceArchive.freeze(M3ClassSpaceArchive.openCanonicalCopy(reread).restore());
            check(Arrays.equals(reopened, frame), "file reopen round trip");
        } finally {
            try {
                Files.deleteIfExists(file);
            } catch (IOException mappedOnWindows) {
                file.toFile().deleteOnExit();
            }
        }
        line("archive roundtrip restore=IDENTICAL mapped=IDENTICAL pinned=IDENTICAL reopen=IDENTICAL");
        int[] positions = {0, 7, 8, 11, 12, 15, 16, 19, 20, 23, 24, 31, 32, 63, frame.length / 2,
            frame.length - 33, frame.length - 32, frame.length - 1};
        MessageDigest messages = sha256();
        for (int position : positions) {
            byte[] corrupt = frame.clone();
            corrupt[position] ^= 0x01;
            String outcome;
            try {
                M3ClassSpaceArchive.openCopy(corrupt);
                outcome = "ACCEPTED";
            } catch (IllegalArgumentException refused) {
                outcome = refused.getMessage();
            }
            check(!outcome.equals("ACCEPTED"), "corruption at " + position + " accepted");
            messages.update(outcome.getBytes(StandardCharsets.UTF_8));
            line("corrupt position=" + position + " refused=" + outcome);
        }
        line("corruptions=" + positions.length + " messages=" + hex(messages));
    }

    /** Real JDK class files: scan, serial and atomic interning agree, symbols read back, truncations refused. */
    private void classFiles() throws IOException {
        M3ClassSpace serial = new M3ClassSpace(1 << 22);
        M3ClassSpace atomic = new M3ClassSpace(1 << 22);
        for (String resource : JDK_CLASSES) {
            byte[] image;
            try (InputStream in = Object.class.getResourceAsStream(resource)) {
                image = in.readAllBytes();
            }
            M3ClassFileAtoms.Plan plan = M3ClassFileAtoms.scan(image);
            M3ClassFileAtoms.Interned first = plan.internInto(serial);
            M3ClassFileAtoms.Interned second = plan.internAtomicallyInto(atomic);
            MessageDigest ids = sha256();
            for (int cp = 1; cp < plan.constantPoolCount(); cp++) {
                check(first.symbolIdAt(cp) == second.symbolIdAt(cp), resource + " serial/atomic id at " + cp);
                if (plan.tagAt(cp) == 1) {
                    check(Arrays.equals(serial.copy(first.symbolIdAt(cp)), plan.copyUtf8At(cp)), resource + " symbol " + cp);
                }
                ids.update(intBytes(first.symbolIdAt(cp)));
            }
            int refusedTruncations = 0;
            int images = serial.imageCount();
            for (int cut : new int[] {0, 4, 9, image.length / 3, image.length / 2, image.length - 1}) {
                try {
                    M3ClassFileAtoms.scan(Arrays.copyOf(image, cut));
                } catch (IllegalArgumentException refused) {
                    refusedTruncations++;
                }
            }
            check(refusedTruncations == 6 && serial.imageCount() == images, resource + " truncations");
            line("classfile " + resource + " bytes=" + image.length + " major=" + plan.majorVersion() + " minor="
                    + plan.minorVersion() + " cp=" + plan.constantPoolCount() + " utf8=" + plan.utf8Count()
                    + " candidate=" + plan.candidateHashHex() + " ids=" + hex(ids) + " truncationsRefused="
                    + refusedTruncations);
        }
        check(serial.imageCount() == atomic.imageCount() && serial.retainedByteCount() == atomic.retainedByteCount(),
                "serial vs atomic owners");
        check(Arrays.equals(M3ClassSpaceArchive.freeze(serial), M3ClassSpaceArchive.freeze(atomic)), "serial vs atomic frames");
        line("classfiles symbols=" + serial.imageCount() + " bytes=" + serial.retainedByteCount() + " frame="
                + sha256hex(M3ClassSpaceArchive.freeze(serial)));
    }

    private static byte[] intBytes(int value) {
        return new byte[] {(byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8), (byte) value};
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException missing) {
            throw new IllegalStateException(missing);
        }
    }

    private static String hex(MessageDigest digest) {
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String sha256hex(byte[] bytes) {
        return HexFormat.of().formatHex(sha256().digest(bytes));
    }
}
