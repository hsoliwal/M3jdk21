// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.pass;

import com.m3.indexdb.M3IndexDB;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Append-only serial ratification ledger for the canonical M3 multi-pass plan.
 *
 * <p>Parallel workers may produce candidates and proofs, but only this ordered receipt chain
 * advances canonical pass state. Resume starts from {@link #currentStateRoot()} and
 * {@link #nextPassOrdinal()}.
 */
public final class M3PassLedger {
    public static final String ARTIFACT_KIND = "m3.multipass-ledger";
    public static final int FORMAT_VERSION = 1;
    private static final int MAGIC = 0x4d33504c; // M3PL

    private final M3MultiPassPlan plan;
    private final List<M3PassReceipt> receipts;

    private M3PassLedger(M3MultiPassPlan plan, List<M3PassReceipt> receipts) {
        this.plan = Objects.requireNonNull(plan, "plan");
        this.receipts = List.copyOf(receipts);
    }

    public static M3PassLedger empty() {
        return new M3PassLedger(M3MultiPassPlan.canonical(), List.of());
    }

    public List<M3PassReceipt> receipts() {
        return receipts;
    }

    public String currentStateRoot() {
        return receipts.isEmpty()
                ? "0".repeat(64)
                : receipts.getLast().outputStateRoot();
    }

    public int nextPassOrdinal() {
        if (receipts.isEmpty()) return 0;
        M3PassReceipt last = receipts.getLast();
        return last.stopConditionSatisfied()
                ? Math.min(last.passOrdinal() + 1, plan.size())
                : last.passOrdinal();
    }

    public int nextIteration() {
        if (receipts.isEmpty()) return 0;
        M3PassReceipt last = receipts.getLast();
        return last.stopConditionSatisfied() ? 0 : last.iteration() + 1;
    }

    public boolean complete() {
        return nextPassOrdinal() >= plan.size()
                && !receipts.isEmpty()
                && receipts.getLast().status() == M3PassReceipt.Status.PASSED
                && receipts.getLast().stopConditionSatisfied();
    }

    public M3PassLedger append(M3PassReceipt receipt) {
        M3PassReceipt checked = Objects.requireNonNull(receipt, "receipt");
        if (complete()) throw new IllegalStateException("multi-pass ledger already complete");

        int expectedPass = nextPassOrdinal();
        int expectedIteration = nextIteration();
        if (checked.passOrdinal() != expectedPass) {
            throw new IllegalArgumentException(
                    "pass ordinal " + checked.passOrdinal() + " != " + expectedPass);
        }
        if (checked.iteration() != expectedIteration) {
            throw new IllegalArgumentException(
                    "iteration " + checked.iteration() + " != " + expectedIteration);
        }
        M3MultiPassPlan.Pass pass = plan.pass(expectedPass);
        if (!pass.passId().equals(checked.passId())) {
            throw new IllegalArgumentException("passId mismatch");
        }
        if (!currentStateRoot().equals(checked.inputStateRoot())) {
            throw new IllegalArgumentException("inputStateRoot is not current canon");
        }

        ArrayList<M3PassReceipt> next = new ArrayList<>(receipts.size() + 1);
        next.addAll(receipts);
        next.add(checked);
        return new M3PassLedger(plan, next);
    }

    public byte[] encode() {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream out = new DataOutputStream(bytes)) {
                out.writeInt(MAGIC);
                out.writeInt(FORMAT_VERSION);
                out.writeInt(receipts.size());
                for (M3PassReceipt receipt : receipts) {
                    out.writeInt(receipt.passOrdinal());
                    out.writeInt(receipt.iteration());
                    text(out, receipt.passId());
                    text(out, receipt.inputStateRoot());
                    text(out, receipt.outputStateRoot());
                    text(out, receipt.proofRoot());
                    text(out, receipt.exactCommit());
                    out.writeByte(receipt.status().ordinal());
                    out.writeBoolean(receipt.changed());
                    out.writeBoolean(receipt.stopConditionSatisfied());
                    text(out, receipt.receiptRoot());
                }
            }
            return bytes.toByteArray();
        } catch (IOException impossible) {
            throw new AssertionError(impossible);
        }
    }

    public static M3PassLedger decode(byte[] payload) {
        Objects.requireNonNull(payload, "payload");
        try (DataInputStream in =
                new DataInputStream(new ByteArrayInputStream(payload))) {
            if (in.readInt() != MAGIC) throw new IllegalArgumentException("pass ledger magic");
            if (in.readInt() != FORMAT_VERSION) {
                throw new IllegalArgumentException("pass ledger version");
            }
            int count = in.readInt();
            if (count < 0 || count > 10_000) {
                throw new IllegalArgumentException("pass ledger count");
            }

            M3PassLedger ledger = empty();
            M3PassReceipt.Status[] statuses = M3PassReceipt.Status.values();
            for (int index = 0; index < count; index++) {
                int passOrdinal = in.readInt();
                int iteration = in.readInt();
                String passId = text(in);
                String input = text(in);
                String output = text(in);
                String proof = text(in);
                String commit = text(in);
                int statusOrdinal = Byte.toUnsignedInt(in.readByte());
                if (statusOrdinal >= statuses.length) {
                    throw new IllegalArgumentException("pass receipt status");
                }
                boolean changed = in.readBoolean();
                boolean stop = in.readBoolean();
                String receiptRoot = text(in);
                ledger = ledger.append(
                        new M3PassReceipt(
                                passOrdinal,
                                iteration,
                                passId,
                                input,
                                output,
                                proof,
                                commit,
                                statuses[statusOrdinal],
                                changed,
                                stop,
                                receiptRoot));
            }
            if (in.read() != -1) {
                throw new IllegalArgumentException("trailing pass ledger bytes");
            }
            return ledger;
        } catch (EOFException truncated) {
            throw new IllegalArgumentException("truncated pass ledger", truncated);
        } catch (IOException impossible) {
            throw new IllegalArgumentException("cannot decode pass ledger", impossible);
        }
    }

    public void store(M3IndexDB database, String artifactName) throws IOException {
        Objects.requireNonNull(database, "database")
                .putArtifact(
                        artifactName,
                        ARTIFACT_KIND,
                        FORMAT_VERSION,
                        encode());
    }

    public static M3PassLedger load(M3IndexDB database, String artifactName)
            throws IOException {
        var artifact = Objects.requireNonNull(database, "database")
                .requireArtifact(artifactName);
        if (!ARTIFACT_KIND.equals(artifact.kind())
                || artifact.formatVersion() != FORMAT_VERSION) {
            throw new IOException("M3 pass ledger artifact contract mismatch");
        }
        try {
            return decode(artifact.payload());
        } catch (IllegalArgumentException invalid) {
            throw new IOException("M3 pass ledger artifact is invalid", invalid);
        }
    }

    private static void text(DataOutputStream out, String value) throws IOException {
        byte[] utf8 = Objects.requireNonNull(value, "value").getBytes(StandardCharsets.UTF_8);
        out.writeInt(utf8.length);
        out.write(utf8);
    }

    private static String text(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length < 0 || length > 1_000_000) {
            throw new IllegalArgumentException("pass ledger string length");
        }
        byte[] utf8 = in.readNBytes(length);
        if (utf8.length != length) throw new EOFException("truncated pass ledger string");
        return new String(utf8, StandardCharsets.UTF_8);
    }
}
