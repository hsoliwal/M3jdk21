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
 * @summary Verify deterministic-random ML-DSA key generation/signing and negative verification
 * @run main MLDSADeterministic
 */

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.spec.NamedParameterSpec;
import java.util.Arrays;

public class MLDSADeterministic {
    private static final String[] ALGORITHMS = {
            "ML-DSA", "ML-DSA-44", "ML-DSA-65", "ML-DSA-87"
    };
    private static final NamedParameterSpec[] PARAMETERS = {
            NamedParameterSpec.ML_DSA_65,
            NamedParameterSpec.ML_DSA_44,
            NamedParameterSpec.ML_DSA_65,
            NamedParameterSpec.ML_DSA_87
    };
    private static final byte[] MESSAGE =
            "FIPS 204 deterministic-random receiver check"
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8);

    public static void main(String[] args) throws Exception {
        Provider sun = Security.getProvider("SUN");
        if (sun == null) {
            throw new AssertionError("SUN provider unavailable");
        }

        for (int index = 0; index < ALGORITHMS.length; index++) {
            String algorithm = ALGORITHMS[index];
            NamedParameterSpec parameters = PARAMETERS[index];

            KeyPair first = generate(sun, algorithm, parameters, 0x31 + index);
            KeyPair second = generate(sun, algorithm, parameters, 0x31 + index);
            if (!Arrays.equals(first.getPublic().getEncoded(), second.getPublic().getEncoded())
                    || !Arrays.equals(first.getPrivate().getEncoded(), second.getPrivate().getEncoded())) {
                throw new AssertionError("key generation is not random-input deterministic: " + algorithm);
            }

            byte[] firstSignature = sign(sun, algorithm, first, 0x61 + index, MESSAGE);
            byte[] secondSignature = sign(sun, algorithm, first, 0x61 + index, MESSAGE);
            if (!Arrays.equals(firstSignature, secondSignature)) {
                throw new AssertionError("signature is not random-input deterministic: " + algorithm);
            }

            Signature verifier = Signature.getInstance(algorithm, sun);
            verifier.initVerify(first.getPublic());
            verifier.update(MESSAGE);
            if (!verifier.verify(firstSignature)) {
                throw new AssertionError("positive verify failed: " + algorithm);
            }

            byte[] changed = MESSAGE.clone();
            changed[0] ^= 1;
            verifier.initVerify(first.getPublic());
            verifier.update(changed);
            if (verifier.verify(firstSignature)) {
                throw new AssertionError("changed message verified: " + algorithm);
            }
        }
    }

    private static KeyPair generate(
            Provider provider,
            String algorithm,
            NamedParameterSpec parameters,
            int seed) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance(algorithm, provider);
        generator.initialize(parameters, new FixedSecureRandom(seed));
        return generator.generateKeyPair();
    }

    private static byte[] sign(
            Provider provider,
            String algorithm,
            KeyPair pair,
            int seed,
            byte[] message) throws Exception {
        Signature signature = Signature.getInstance(algorithm, provider);
        signature.initSign(pair.getPrivate(), new FixedSecureRandom(seed));
        signature.update(message);
        return signature.sign();
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
