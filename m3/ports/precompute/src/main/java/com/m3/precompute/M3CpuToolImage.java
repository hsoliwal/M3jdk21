// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.precompute;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Versioned byte image for M3-prepared CPU DAGs; SHA-256 authenticates bytes, not a signer. */
public final class M3CpuToolImage {
    private static final byte[] MAGIC = "M3CPU001".getBytes(StandardCharsets.US_ASCII);
    private static final int HASH_BYTES = 32;
    private static final int MAX_IMAGE_BYTES = 1024 * 1024;
    private static final int MAX_STRING_BYTES = 4096;

    private M3CpuToolImage() {}

    /** Build-time deterministic serialization of the stable plan; never serializes callbacks. */
    public static byte[] encode(M3CpuToolPlan.Compiled plan) {
        Objects.requireNonNull(plan, "plan");
        try {
            ByteArrayOutputStream storage = new ByteArrayOutputStream();
            DataOutputStream data = new DataOutputStream(storage);
            data.write(MAGIC);
            writeString(data, plan.outputId());
            data.writeInt(plan.definitions().size());
            for (M3CpuToolPlan.Step node : plan.definitions()) {
                writeString(data, node.id());
                writeString(data, node.operation());
                writeString(data, node.constant());
                data.writeInt(node.dependencies().size());
                for (String dependency : node.dependencies()) writeString(data, dependency);
            }
            data.flush();
            byte[] payload = storage.toByteArray();
            if (payload.length + HASH_BYTES > MAX_IMAGE_BYTES) {
                throw new IllegalArgumentException("precompute image too large");
            }
            byte[] result = Arrays.copyOf(payload, payload.length + HASH_BYTES);
            byte[] checksum = M3CpuToolPlan.sha256().digest(payload);
            System.arraycopy(checksum, 0, result, payload.length, checksum.length);
            return result;
        } catch (IOException unexpected) {
            throw new IllegalStateException("byte image output failed", unexpected);
        }
    }

    /** Rejects truncation, mutation, unsupported versions and inconsistent graphs before use. */
    public static M3CpuToolPlan.Compiled decode(byte[] image) {
        Objects.requireNonNull(image, "image");
        if (image.length < MAGIC.length + HASH_BYTES + 8 || image.length > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("invalid image size");
        }
        int payloadLength = image.length - HASH_BYTES;
        byte[] actualHash = M3CpuToolPlan.sha256().digest(
                Arrays.copyOfRange(image, 0, payloadLength));
        byte[] storedHash = Arrays.copyOfRange(image, payloadLength, image.length);
        if (!MessageDigest.isEqual(actualHash, storedHash)) {
            throw new IllegalArgumentException("image checksum mismatch");
        }
        try (DataInputStream in = new DataInputStream(
                new ByteArrayInputStream(image, 0, payloadLength))) {
            byte[] magic = in.readNBytes(MAGIC.length);
            if (!Arrays.equals(magic, MAGIC)) throw new IllegalArgumentException("image version");
            String output = readString(in);
            int count = in.readInt();
            if (count <= 0 || count > 128) throw new IllegalArgumentException("image node count");
            List<M3CpuToolPlan.Step> steps = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                String id = readString(in);
                String operator = readString(in);
                String constant = readString(in);
                int edgeCount = in.readInt();
                if (edgeCount < 0 || edgeCount > 16) {
                    throw new IllegalArgumentException("image fan-in budget");
                }
                List<String> inputs = new ArrayList<>(edgeCount);
                for (int j = 0; j < edgeCount; j++) inputs.add(readString(in));
                steps.add(new M3CpuToolPlan.Step(id, operator, inputs, constant));
            }
            if (in.read() != -1) throw new IllegalArgumentException("image trailing data");
            M3CpuToolPlan.Compiled restored = M3CpuToolPlan.compile(steps, output);
            if (!Arrays.equals(image, encode(restored))) {
                throw new IllegalArgumentException("noncanonical precompute image");
            }
            return restored;
        } catch (IOException invalid) {
            throw new IllegalArgumentException("truncated precompute image", invalid);
        }
    }

    private static void writeString(DataOutputStream output, String text) throws IOException {
        byte[] bytes = M3CpuToolPlan.utf8Exact(text);
        output.writeInt(bytes.length);
        output.write(bytes);
    }

    private static String readString(DataInputStream input) throws IOException {
        int size = input.readInt();
        if (size < 0 || size > MAX_STRING_BYTES) {
            throw new IllegalArgumentException("unbounded image string");
        }
        byte[] bytes = input.readNBytes(size);
        if (bytes.length != size) throw new IllegalArgumentException("short image string");
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException malformed) {
            throw new IllegalArgumentException("invalid image UTF-8", malformed);
        }
    }
}
