// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays.packed;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Verifies packed payloads and records explicitly scoped JVM object-size estimates. */
public final class MemoryProbe {
    private static final int DEFAULT_LENGTH = 10_000_000;
    private static final long CHECKSUM_SEED = 0x6a09e667f3bcc909L;
    private static final long CHECKSUM_INCREMENT = 0x9e3779b97f4a7c15L;

    private MemoryProbe() {
    }

    /**
     * Verifies all widths against a long-array baseline and emits one JSON report.
     *
     * @param arguments optional element count followed by an optional JSON output path
     * @throws IllegalArgumentException if arguments or the element count are invalid
     * @throws IllegalStateException if instrumentation or a verification invariant fails
     * @throws ReflectiveOperationException if the declared private backing array is unavailable
     * @throws IOException if the requested JSON output cannot be written
     */
    public static void main(String[] arguments)
            throws ReflectiveOperationException, IOException {
        if (arguments.length > 2) {
            throw new IllegalArgumentException("Usage: MemoryProbe [element-count [output.json]]");
        }
        int length = arguments.length == 0 ? DEFAULT_LENGTH : Integer.parseInt(arguments[0]);
        if (length < 0 || length > Integer.MAX_VALUE / Int56Array.BYTES) {
            throw new IllegalArgumentException("Element count exceeds the common packed capacity");
        }

        long[] baseline = new long[length];
        long baselinePayload = (long) length * Long.BYTES;
        long baselineEstimate = MemoryAgent.getObjectSize(baseline);
        Field dataField = PackedIntArray.class.getDeclaredField("data");
        dataField.setAccessible(true);

        StringBuilder json = new StringBuilder(4096);
        json.append("{\n  \"schema_version\": 1,\n  \"verification\": \"passed\",\n");
        appendStringField(json, "jvm_version", System.getProperty("java.version"));
        appendStringField(json, "jvm_name", System.getProperty("java.vm.name"));
        appendStringField(json, "jvm_build", System.getProperty("java.vm.version"));
        appendStringField(json, "jvm_vendor", System.getProperty("java.vendor"));
        appendStringField(json, "architecture", System.getProperty("os.arch"));
        json.append("  \"element_count\": ").append(length).append(",\n");
        json.append("  \"baseline_payload_bytes\": ").append(baselinePayload).append(",\n");
        json.append("  \"baseline_estimated_instance_bytes\": ")
                .append(baselineEstimate).append(",\n");
        json.append("  \"estimate_method\": \"Instrumentation.getObjectSize\",\n");
        json.append("  \"estimate_scope\": ")
                .append("\"packed wrapper plus owned byte array; baseline long array only\",\n");
        json.append("  \"byte_order\": \"little-endian\",\n");
        json.append("  \"signed\": true,\n");
        json.append("  \"dataset\": ")
                .append("\"boundary sentinels followed by a deterministic modular sequence\",\n");
        json.append("  \"checksum_algorithm\": ")
                .append("\"seed 6a09e667f3bcc909; rotate-left 7, xor value, ")
                .append("add 9e3779b97f4a7c15 modulo 2^64\",\n");
        json.append("  \"widths\": [\n");

        int[] widths = {Int40Array.BITS, Int48Array.BITS, Int56Array.BITS};
        for (int index = 0; index < widths.length; index++) {
            Measurement result = measure(widths[index], baseline, dataField);
            appendMeasurement(json, result, baselinePayload, baselineEstimate);
            json.append(index + 1 == widths.length ? "\n" : ",\n");
        }
        json.append("  ],\n  \"total_verified_values\": ")
                .append((long) length * widths.length).append("\n}");
        String report = json.toString();
        if (arguments.length == 2) {
            Files.writeString(Path.of(arguments[1]), report + "\n", StandardCharsets.UTF_8);
        }
        System.out.println(report);
    }

    private static Measurement measure(int bits, long[] baseline, Field dataField)
            throws IllegalAccessException {
        PackedIntArray packed = switch (bits) {
            case Int40Array.BITS -> new Int40Array(baseline.length);
            case Int48Array.BITS -> new Int48Array(baseline.length);
            case Int56Array.BITS -> new Int56Array(baseline.length);
            default -> throw new IllegalArgumentException("Unsupported width: " + bits);
        };
        int bytes = bits / Byte.SIZE;
        byte[] data = (byte[]) dataField.get(packed);
        long payload = (long) baseline.length * bytes;
        require(data.length == payload, "Backing payload mismatch for int" + bits);
        require(packed.payloadBytes() == payload, "Reported payload mismatch for int" + bits);
        require(packed.length() == baseline.length, "Element count mismatch for int" + bits);
        require(packed.bitsPerValue() == bits, "Reported bit width mismatch for int" + bits);
        require(packed.bytesPerValue() == bytes, "Reported byte width mismatch for int" + bits);

        long expectedChecksum = CHECKSUM_SEED;
        for (int index = 0; index < baseline.length; index++) {
            long value = sequenceValue(index, bits);
            baseline[index] = value;
            packed.set(index, value);
            expectedChecksum = checksum(expectedChecksum, value);
        }
        long actualChecksum = CHECKSUM_SEED;
        for (int index = 0; index < baseline.length; index++) {
            long actual = packed.get(index);
            if (actual != baseline[index]) {
                throw new IllegalStateException("int" + bits + " mismatch at index " + index
                        + ": expected " + baseline[index] + ", found " + actual);
            }
            actualChecksum = checksum(actualChecksum, actual);
        }
        require(actualChecksum == expectedChecksum, "Checksum mismatch for int" + bits);
        return new Measurement(bits, bytes, payload, MemoryAgent.getObjectSize(packed),
                MemoryAgent.getObjectSize(data), actualChecksum, baseline.length);
    }

    private static long sequenceValue(int index, int bits) {
        return switch (index) {
            case 0 -> -(1L << (bits - 1));
            case 1 -> (1L << (bits - 1)) - 1;
            case 2 -> -(1L << 32) - 1;
            case 3 -> (1L << 32) + 1;
            case 4 -> -1;
            case 5 -> 0;
            case 6 -> 1;
            default -> {
                long raw = ((long) index * 0x1_0000_01b3L + 0x1_0000_0001L)
                        & ((1L << bits) - 1);
                yield (raw << (Long.SIZE - bits)) >> (Long.SIZE - bits);
            }
        };
    }

    private static long checksum(long previous, long value) {
        return (Long.rotateLeft(previous, 7) ^ value) + CHECKSUM_INCREMENT;
    }

    private static void appendMeasurement(StringBuilder json, Measurement result,
            long baselinePayload, long baselineEstimate) {
        long totalEstimate = result.wrapperEstimate() + result.backingEstimate();
        double savingPercent = (Long.SIZE - result.bits()) * 100.0 / Long.SIZE;
        json.append("    {\n      \"representation\": \"int")
                .append(result.bits()).append("\",\n");
        json.append("      \"bits_per_value\": ").append(result.bits()).append(",\n");
        json.append("      \"bytes_per_value\": ").append(result.bytes()).append(",\n");
        json.append("      \"payload_bytes\": ").append(result.payload()).append(",\n");
        json.append("      \"saved_payload_bytes\": ")
                .append(baselinePayload - result.payload()).append(",\n");
        json.append("      \"payload_saving_percent\": ").append(savingPercent).append(",\n");
        json.append("      \"estimated_wrapper_bytes\": ")
                .append(result.wrapperEstimate()).append(",\n");
        json.append("      \"estimated_backing_array_bytes\": ")
                .append(result.backingEstimate()).append(",\n");
        json.append("      \"estimated_instance_bytes\": ").append(totalEstimate).append(",\n");
        json.append("      \"estimated_instance_saving_bytes\": ")
                .append(baselineEstimate - totalEstimate).append(",\n");
        json.append("      \"checksum_unsigned_decimal\": \"")
                .append(Long.toUnsignedString(result.checksum())).append("\",\n");
        json.append("      \"verified_values\": ").append(result.verified()).append(",\n");
        json.append("      \"all_values_verified\": true\n    }");
    }

    private static void appendStringField(StringBuilder json, String name, String value) {
        json.append("  \"").append(name).append("\": ");
        appendQuoted(json, value == null ? "unavailable" : value);
        json.append(",\n");
    }

    private static void appendQuoted(StringBuilder json, String value) {
        json.append('"');
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> json.append("\\\"");
                case '\\' -> json.append("\\\\");
                case '\b' -> json.append("\\b");
                case '\f' -> json.append("\\f");
                case '\n' -> json.append("\\n");
                case '\r' -> json.append("\\r");
                case '\t' -> json.append("\\t");
                default -> {
                    if (character < 0x20) {
                        json.append("\\u00");
                        json.append(Character.forDigit(character >>> 4, 16));
                        json.append(Character.forDigit(character & 0xf, 16));
                    } else {
                        json.append(character);
                    }
                }
            }
        }
        json.append('"');
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private record Measurement(int bits, int bytes, long payload, long wrapperEstimate,
            long backingEstimate, long checksum, int verified) {
    }
}
