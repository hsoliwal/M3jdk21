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
 */

/*
 * @test
 * @bug 8298390
 * @summary M3JDK21 ML-KEM provider registration and opt-in NamedParameterSpec smoke
 * @run main/othervm MLKEMProviderSmoke
 */

import javax.crypto.KEM;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.Provider;
import java.security.Security;
import java.security.spec.NamedParameterSpec;

public class MLKEMProviderSmoke {

    public static void main(String[] args) throws Exception {
        Provider provider = Security.getProvider("SunJCE");
        if (provider == null) {
            throw new AssertionError("SunJCE provider is missing");
        }

        assertName(NamedParameterSpec.ML_KEM_512, "ML-KEM-512");
        assertName(NamedParameterSpec.ML_KEM_768, "ML-KEM-768");
        assertName(NamedParameterSpec.ML_KEM_1024, "ML-KEM-1024");

        require(provider, "KEM", "ML-KEM");
        require(provider, "KEM", "ML-KEM-512");
        require(provider, "KEM", "ML-KEM-768");
        require(provider, "KEM", "ML-KEM-1024");
        require(provider, "KeyPairGenerator", "ML-KEM");
        require(provider, "KeyPairGenerator", "ML-KEM-512");
        require(provider, "KeyPairGenerator", "ML-KEM-768");
        require(provider, "KeyPairGenerator", "ML-KEM-1024");
        require(provider, "KeyFactory", "ML-KEM");
        require(provider, "KeyFactory", "ML-KEM-512");
        require(provider, "KeyFactory", "ML-KEM-768");
        require(provider, "KeyFactory", "ML-KEM-1024");

        require(provider, "KEM", "2.16.840.1.101.3.4.4.1");
        require(provider, "KEM", "2.16.840.1.101.3.4.4.2");
        require(provider, "KEM", "2.16.840.1.101.3.4.4.3");

        KEM.getInstance("ML-KEM", provider);
        KEM.getInstance("ML-KEM-512", provider);
        KEM.getInstance("ML-KEM-768", provider);
        KEM.getInstance("ML-KEM-1024", provider);

        KeyPairGenerator.getInstance("ML-KEM", provider);
        KeyPairGenerator.getInstance("ML-KEM-512", provider);
        KeyPairGenerator.getInstance("ML-KEM-768", provider);
        KeyPairGenerator.getInstance("ML-KEM-1024", provider);

        KeyFactory.getInstance("ML-KEM", provider);
        KeyFactory.getInstance("ML-KEM-512", provider);
        KeyFactory.getInstance("ML-KEM-768", provider);
        KeyFactory.getInstance("ML-KEM-1024", provider);
    }

    private static void assertName(NamedParameterSpec spec, String expected) {
        if (!expected.equals(spec.getName())) {
            throw new AssertionError(
                    "NamedParameterSpec mismatch: expected "
                            + expected
                            + ", got "
                            + spec.getName());
        }
    }

    private static void require(Provider provider, String type, String algorithm) {
        Provider.Service service = provider.getService(type, algorithm);
        if (service == null) {
            throw new AssertionError(type + "." + algorithm + " is not registered");
        }
    }
}
