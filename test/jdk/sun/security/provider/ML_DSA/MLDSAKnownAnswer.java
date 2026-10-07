/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 */

/*
 * @test
 * @summary Reduced pinned FIPS 204 ACVP keyGen/sigGen/sigVer vectors for ML-DSA on Java 21
 * @library /test/lib
 * @run main MLDSAKnownAnswer
 */

import jdk.test.lib.json.JSONValue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.EncodedKeySpec;
import java.security.spec.NamedParameterSpec;
import java.util.Arrays;

import static jdk.test.lib.Utils.toByteArray;

public class MLDSAKnownAnswer {
    public static void main(String[] args) throws Exception {
        Provider sun = Security.getProvider("SUN");
        if (sun == null) {
            throw new AssertionError("SUN provider unavailable");
        }
        Path data = Path.of(System.getProperty("test.src"), "data");
        keyGen(JSONValue.parse(Files.readString(data.resolve("keyGen.json"), StandardCharsets.UTF_8)), sun);
        sigGen(JSONValue.parse(Files.readString(data.resolve("sigGen.json"), StandardCharsets.UTF_8)), sun);
        sigVer(JSONValue.parse(Files.readString(data.resolve("sigVer.json"), StandardCharsets.UTF_8)), sun);
    }

    private static void keyGen(JSONValue kat, Provider provider) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("ML-DSA", provider);
        KeyFactory factory = KeyFactory.getInstance("ML-DSA", provider);
        for (var group : kat.get("testGroups").asArray()) {
            String parameterSet = group.get("parameterSet").asString();
            NamedParameterSpec parameters = new NamedParameterSpec(parameterSet);
            for (var test : group.get("tests").asArray()) {
                byte[] seed = toByteArray(test.get("seed").asString());
                generator.initialize(parameters, new FixedSecureRandom(seed));
                var pair = generator.generateKeyPair();
                byte[] pk = factory.getKeySpec(pair.getPublic(), EncodedKeySpec.class).getEncoded();
                byte[] sk = factory.getKeySpec(pair.getPrivate(), EncodedKeySpec.class).getEncoded();
                requireEquals(pk, toByteArray(test.get("pk").asString()), "keyGen pk " + parameterSet);
                requireEquals(sk, toByteArray(test.get("sk").asString()), "keyGen sk " + parameterSet);
            }
        }
    }

    private static void sigGen(JSONValue kat, Provider provider) throws Exception {
        Signature signature = Signature.getInstance("ML-DSA", provider);
        for (var group : kat.get("testGroups").asArray()) {
            String parameterSet = group.get("parameterSet").asString();
            boolean deterministic = Boolean.parseBoolean(group.get("deterministic").asString());
            for (var test : group.get("tests").asArray()) {
                PrivateKey privateKey = new PrivateKey() {
                    public String getAlgorithm() { return parameterSet; }
                    public String getFormat() { return "RAW"; }
                    public byte[] getEncoded() { return toByteArray(test.get("sk").asString()); }
                };
                byte[] random = deterministic
                        ? new byte[32]
                        : toByteArray(test.get("rnd").asString());
                signature.initSign(privateKey, new FixedSecureRandom(random));
                signature.update(toByteArray(test.get("message").asString()));
                requireEquals(
                        signature.sign(),
                        toByteArray(test.get("signature").asString()),
                        "sigGen " + parameterSet);
            }
        }
    }

    private static void sigVer(JSONValue kat, Provider provider) throws Exception {
        Signature signature = Signature.getInstance("ML-DSA", provider);
        for (var group : kat.get("testGroups").asArray()) {
            String parameterSet = group.get("parameterSet").asString();
            PublicKey publicKey = new PublicKey() {
                public String getAlgorithm() { return parameterSet; }
                public String getFormat() { return "RAW"; }
                public byte[] getEncoded() { return toByteArray(group.get("pk").asString()); }
            };
            for (var test : group.get("tests").asArray()) {
                boolean expected = Boolean.parseBoolean(test.get("testPassed").asString());
                boolean actual = true;
                try {
                    signature.initVerify(publicKey);
                    signature.update(toByteArray(test.get("message").asString()));
                    actual = signature.verify(toByteArray(test.get("signature").asString()));
                } catch (InvalidKeyException | SignatureException failure) {
                    actual = false;
                }
                if (expected != actual) {
                    throw new AssertionError(
                            "sigVer " + parameterSet + " expected=" + expected + " actual=" + actual);
                }
            }
        }
    }

    private static void requireEquals(byte[] actual, byte[] expected, String label) {
        if (!Arrays.equals(actual, expected)) {
            throw new AssertionError(label + " mismatch");
        }
    }

    static final class FixedSecureRandom extends SecureRandom {
        private final byte[] bytes;
        private int offset;

        FixedSecureRandom(byte[] bytes) {
            this.bytes = bytes.clone();
        }

        @Override
        public void nextBytes(byte[] output) {
            if (offset + output.length > bytes.length) {
                throw new IllegalStateException("fixed random exhausted");
            }
            System.arraycopy(bytes, offset, output, 0, output.length);
            offset += output.length;
        }
    }
}
