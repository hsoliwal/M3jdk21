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
 * @summary Smoke ML-DSA provider names, OIDs, encodings and signatures on the Java 21 receiver
 * @run main MLDSAProviderSmoke
 */

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.spec.NamedParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

public class MLDSAProviderSmoke {
    private static final String[] ALGORITHMS = {
            "ML-DSA", "ML-DSA-44", "ML-DSA-65", "ML-DSA-87"
    };
    private static final NamedParameterSpec[] PARAMETERS = {
            NamedParameterSpec.ML_DSA_65,
            NamedParameterSpec.ML_DSA_44,
            NamedParameterSpec.ML_DSA_65,
            NamedParameterSpec.ML_DSA_87
    };
    private static final String[] OIDS = {
            null,
            "2.16.840.1.101.3.4.3.17",
            "2.16.840.1.101.3.4.3.18",
            "2.16.840.1.101.3.4.3.19"
    };
    private static final byte[] MESSAGE =
            "M3JDK21 ML-DSA provider smoke".getBytes(java.nio.charset.StandardCharsets.UTF_8);

    public static void main(String[] args) throws Exception {
        Provider sun = Security.getProvider("SUN");
        if (sun == null) {
            throw new AssertionError("SUN provider unavailable");
        }

        assertName(NamedParameterSpec.ML_DSA_44, "ML-DSA-44");
        assertName(NamedParameterSpec.ML_DSA_65, "ML-DSA-65");
        assertName(NamedParameterSpec.ML_DSA_87, "ML-DSA-87");

        for (int index = 0; index < ALGORITHMS.length; index++) {
            String algorithm = ALGORITHMS[index];
            NamedParameterSpec parameters = PARAMETERS[index];

            KeyPairGenerator generator = KeyPairGenerator.getInstance(algorithm, sun);
            generator.initialize(parameters, new FixedSecureRandom(0x21 + index));
            KeyPair pair = generator.generateKeyPair();

            KeyFactory factory = KeyFactory.getInstance(algorithm, sun);
            var publicKey = factory.generatePublic(
                    new X509EncodedKeySpec(pair.getPublic().getEncoded()));
            var privateKey = factory.generatePrivate(
                    new PKCS8EncodedKeySpec(pair.getPrivate().getEncoded()));

            if (!Arrays.equals(pair.getPublic().getEncoded(), publicKey.getEncoded())
                    || !Arrays.equals(pair.getPrivate().getEncoded(), privateKey.getEncoded())) {
                throw new AssertionError("key encoding round trip failed: " + algorithm);
            }

            Signature signature = Signature.getInstance(algorithm, sun);
            signature.initSign(privateKey, new FixedSecureRandom(0x51 + index));
            signature.update(MESSAGE);
            byte[] signed = signature.sign();

            signature.initVerify(publicKey);
            signature.update(MESSAGE);
            if (!signature.verify(signed)) {
                throw new AssertionError("signature verification failed: " + algorithm);
            }

            String oid = OIDS[index];
            if (oid != null) {
                Signature.getInstance(oid, sun);
                KeyPairGenerator.getInstance(oid, sun);
                KeyFactory.getInstance(oid, sun);
            }
        }
    }

    private static void assertName(NamedParameterSpec parameters, String expected) {
        if (!expected.equals(parameters.getName())) {
            throw new AssertionError(expected + " != " + parameters.getName());
        }
    }

    static final class FixedSecureRandom extends SecureRandom {
        private final int seed;
        private int round;

        FixedSecureRandom(int seed) {
            this.seed = seed;
        }

        @Override
        public void nextBytes(byte[] bytes) {
            int salt = round++ * 17;
            for (int index = 0; index < bytes.length; index++) {
                bytes[index] = (byte) (seed + salt + index * 29);
            }
        }
    }
}
