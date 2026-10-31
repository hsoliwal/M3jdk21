/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * @test
 * @bug 8298390 8342442
 * @summary Java-21-compatible FIPS 203 ML-KEM known-answer proof
 * @library /test/lib
 * @run main/othervm MLKEMKnownAnswer
 */

import jdk.test.lib.json.JSONValue;

import javax.crypto.KEM;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.EncodedKeySpec;
import java.security.spec.NamedParameterSpec;
import java.util.Arrays;
import java.util.HexFormat;

/**
 * Focused Java-21 adaptation of the later static ACVP ML-KEM harness.
 *
 * <p>The vector files are copied verbatim from the JEP 496 upstream commit. The harness keeps only
 * the ML-KEM behavior required by this packet and carries a tiny deterministic SecureRandom so it
 * does not depend on later test-lib additions.</p>
 */
public class MLKEMKnownAnswer {

    private static final HexFormat HEX = HexFormat.of();

    public static void main(String[] args) throws Exception {
        Path data = Path.of(System.getProperty("test.src"), "data");
        run(data.resolve("ML-KEM-keyGen-FIPS203/internalProjection.json"));
        run(data.resolve("ML-KEM-encapDecap-FIPS203/internalProjection.json"));
    }

    private static void run(Path path) throws Exception {
        JSONValue kat = JSONValue.parse(Files.readString(path));
        String algorithm = kat.get("algorithm").asString();
        if (!"ML-KEM".equals(algorithm)) {
            throw new AssertionError("Unexpected algorithm in " + path + ": " + algorithm);
        }
        switch (kat.get("mode").asString()) {
            case "keyGen" -> keyGen(kat);
            case "encapDecap" -> encapDecap(kat);
            default -> throw new AssertionError("Unexpected mode in " + path);
        }
    }

    private static void keyGen(JSONValue kat) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("ML-KEM", "SunJCE");
        KeyFactory factory = KeyFactory.getInstance("ML-KEM", "SunJCE");

        for (JSONValue group : kat.get("testGroups").asArray()) {
            String parameterSet = group.get("parameterSet").asString();
            NamedParameterSpec spec = new NamedParameterSpec(parameterSet);
            for (JSONValue test : group.get("tests").asArray()) {
                FixedSecureRandom random = new FixedSecureRandom(
                        bytes(test.get("d").asString()),
                        bytes(test.get("z").asString()));
                generator.initialize(spec, random);
                var pair = generator.generateKeyPair();

                byte[] publicKey =
                        factory.getKeySpec(pair.getPublic(), EncodedKeySpec.class).getEncoded();
                byte[] privateKey =
                        factory.getKeySpec(pair.getPrivate(), EncodedKeySpec.class).getEncoded();

                assertBytes("keyGen public " + parameterSet, bytes(test.get("ek").asString()), publicKey);
                assertBytes("keyGen private " + parameterSet, bytes(test.get("dk").asString()), privateKey);
                random.requireExhausted("keyGen " + parameterSet);
            }
        }
    }

    private static void encapDecap(JSONValue kat) throws Exception {
        KEM kem = KEM.getInstance("ML-KEM", "SunJCE");

        for (JSONValue group : kat.get("testGroups").asArray()) {
            String parameterSet = group.get("parameterSet").asString();
            String function = group.get("function").asString();

            if ("encapsulation".equals(function)) {
                for (JSONValue test : group.get("tests").asArray()) {
                    PublicKey publicKey = new RawPublicKey(
                            parameterSet, bytes(test.get("ek").asString()));
                    FixedSecureRandom random =
                            new FixedSecureRandom(bytes(test.get("m").asString()));

                    var encapsulator = kem.newEncapsulator(publicKey, random);
                    var encapsulated = encapsulator.encapsulate();

                    assertBytes(
                            "encapsulation " + parameterSet,
                            bytes(test.get("c").asString()),
                            encapsulated.encapsulation());
                    assertBytes(
                            "shared secret " + parameterSet,
                            bytes(test.get("k").asString()),
                            encapsulated.key().getEncoded());
                    random.requireExhausted("encapsulation " + parameterSet);
                }
            } else if ("decapsulation".equals(function)) {
                PrivateKey privateKey = new RawPrivateKey(
                        parameterSet, bytes(group.get("dk").asString()));
                var decapsulator = kem.newDecapsulator(privateKey);

                for (JSONValue test : group.get("tests").asArray()) {
                    byte[] secret =
                            decapsulator.decapsulate(bytes(test.get("c").asString())).getEncoded();
                    assertBytes(
                            "decapsulation " + parameterSet,
                            bytes(test.get("k").asString()),
                            secret);
                }
            } else {
                throw new AssertionError("Unexpected ML-KEM function: " + function);
            }
        }
    }

    private static byte[] bytes(String hex) {
        return HEX.parseHex(hex);
    }

    private static void assertBytes(String label, byte[] expected, byte[] actual) {
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError(
                    label
                            + " mismatch\nexpected="
                            + HEX.formatHex(expected)
                            + "\nactual="
                            + HEX.formatHex(actual));
        }
    }

    private record RawPublicKey(String algorithm, byte[] encoded) implements PublicKey {
        RawPublicKey {
            encoded = encoded.clone();
        }

        @Override
        public String getAlgorithm() {
            return algorithm;
        }

        @Override
        public String getFormat() {
            return "RAW";
        }

        @Override
        public byte[] getEncoded() {
            return encoded.clone();
        }
    }

    private record RawPrivateKey(String algorithm, byte[] encoded) implements PrivateKey {
        RawPrivateKey {
            encoded = encoded.clone();
        }

        @Override
        public String getAlgorithm() {
            return algorithm;
        }

        @Override
        public String getFormat() {
            return "RAW";
        }

        @Override
        public byte[] getEncoded() {
            return encoded.clone();
        }
    }

    private static final class FixedSecureRandom extends SecureRandom {
        private final byte[] bytes;
        private int offset;

        FixedSecureRandom(byte[]... segments) {
            int length = 0;
            for (byte[] segment : segments) {
                length = Math.addExact(length, segment.length);
            }
            bytes = new byte[length];
            int cursor = 0;
            for (byte[] segment : segments) {
                System.arraycopy(segment, 0, bytes, cursor, segment.length);
                cursor += segment.length;
            }
        }

        @Override
        public void nextBytes(byte[] out) {
            if (out.length > bytes.length - offset) {
                throw new IllegalStateException("Not enough deterministic random bytes");
            }
            System.arraycopy(bytes, offset, out, 0, out.length);
            offset += out.length;
        }

        void requireExhausted(String operation) {
            if (offset != bytes.length) {
                throw new AssertionError(
                        operation
                                + " did not consume all deterministic random bytes: "
                                + (bytes.length - offset)
                                + " remain");
            }
        }
    }
}
