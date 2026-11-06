// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compression;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;

/** Writes deterministic raw codec bytes for independent native decompression. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/6fdee5330b8ab005005785aeff0d1105ba40d12c/synexia-algo/src/test/java/com/synexia/algorithms/compression/M3CompressionEncodedCorpusCli.java blob 7a7630955039; recipe m3/compression/recipe/compression-20261106/relocate.py v3
public final class M3CompressionEncodedCorpusCli {
    private static final int MAGIC = 0x4d334543; // M3EC
    private static final int CASES_PER_CODEC = 250;

    private M3CompressionEncodedCorpusCli() {}

    public static void main(String[] args) throws IOException {
        if (args.length != 1) throw new IllegalArgumentException("expected output file");
        Path output = Path.of(args[0]).toAbsolutePath().normalize();
        Path parent = output.getParent();
        if (parent != null) Files.createDirectories(parent);
        M3SnappyRawEncoder snappy = new M3SnappyRawEncoder(13);
        M3Lz4RawBlockEncoder lz4 = new M3Lz4RawBlockEncoder(13);
        Random random = new Random(0x4d33454e434f4445L);
        try (DataOutputStream writer = new DataOutputStream(
                new BufferedOutputStream(Files.newOutputStream(output)))) {
            writer.writeInt(MAGIC);
            writer.writeInt(CASES_PER_CODEC * 2);
            for (int i = 0; i < CASES_PER_CODEC; i++) {
                int length = switch (i % 20) {
                    case 0 -> 0;
                    case 1 -> 1;
                    case 2 -> 3;
                    case 3 -> 4;
                    case 4 -> 5;
                    case 5 -> 12;
                    case 6 -> 15;
                    case 7 -> 16;
                    case 8 -> 60;
                    case 9 -> 61;
                    case 10 -> 255;
                    case 11 -> 256;
                    case 12 -> 1024;
                    case 13 -> 4096;
                    case 14 -> 65535;
                    case 15 -> 65536;
                    case 16 -> 70000;
                    default -> random.nextInt(30000);
                };
                byte[] plain = new byte[length];
                random.nextBytes(plain);
                if (i % 3 == 0) Arrays.fill(plain, (byte) 'A');
                if (i % 3 == 1) {
                    for (int j = 0; j < length; j++) plain[j] = (byte) (j & 15);
                }
                writeRecord(writer, 0, plain, snappy.encode(plain, length * 2 + 64));
                writeRecord(writer, 1, plain, lz4.encode(plain, length * 2 + 64));
            }
        }
        System.out.println("M3 independently decodable encoder corpus records="
                + CASES_PER_CODEC * 2);
    }

    private static void writeRecord(DataOutputStream writer, int codec, byte[] plain, byte[] packed)
            throws IOException {
        writer.writeByte(codec);
        writer.writeInt(plain.length);
        writer.writeInt(packed.length);
        writer.write(plain);
        writer.write(packed);
    }
}
