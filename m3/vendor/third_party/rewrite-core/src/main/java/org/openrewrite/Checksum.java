// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/Checksum.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
/*
 * Copyright 2022 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite;

import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.remote.Remote;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class Checksum {
    private static final byte[] HEX_ARRAY = "0123456789abcdef".getBytes(StandardCharsets.US_ASCII);
    private final String algorithm;
    private final byte[] value;

    public String getHexValue() {
        byte[] hexChars = new byte[value.length * 2];
        for (int j = 0; j < value.length; j++) {
            int v = value[j] & 255;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 15];
        }
        return new String(hexChars, StandardCharsets.UTF_8);
    }

    public static Checksum fromHex(String algorithm, String hex) {
        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Hex string must contain a set of hex pairs (length divisible by 2).");
        }
        byte[] value = new byte[hex.length() / 2];
        for (int i = 0; i < value.length; i++) {
            int index = i * 2;
            int n = Integer.parseInt(hex.substring(index, index + 2), 16);
            value[i] = (byte) n;
        }
        return new Checksum(algorithm, value);
    }

    /**
     * Interprets the URI as pointing to a UTF-8 encoded text file which contains only a base-16 number representing a
     * checksum. The ending of the URL is used to determine the algorithm that should be used.
     */
    public static Checksum fromUri(HttpSender httpSender, URI uri) {
        String uriStr = uri.toString();
        if (uriStr.endsWith(".sha256")) {
            return fromUri(httpSender, uri, "SHA-256");
        } else if (uriStr.endsWith(".md5")) {
            return fromUri(httpSender, uri, "MD5");
        }
        throw new IllegalArgumentException("Unable to automatically determine checksum type from URI: " + uriStr);
    }

    public static Checksum fromUri(HttpSender httpSender, URI uri, String algorithm) {
        HttpSender.Request request = HttpSender.Request.build(uri.toString(), httpSender).withMethod(HttpSender.Method.GET).build();
        try (HttpSender.Response response = httpSender.send(request)) {
            String hexString = new String(response.getBodyAsBytes(), StandardCharsets.UTF_8);
            return Checksum.fromHex(algorithm, hexString);
        }
    }

    public static SourceFile md5(SourceFile sourceFile, ExecutionContext ctx) {
        return checksum(sourceFile, "MD5", ctx);
    }

    public static SourceFile sha256(SourceFile sourceFile, ExecutionContext ctx) {
        return checksum(sourceFile, "SHA-256", ctx);
    }

    public static SourceFile checksum(SourceFile sourceFile, @Nullable String algorithm, ExecutionContext ctx) {
        if (algorithm == null) {
            return sourceFile;
        }
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            InputStream is;
            if (sourceFile instanceof Remote) {
                is = ((Remote) sourceFile).getInputStream(ctx);
            } else {
                is = Files.newInputStream(sourceFile.getSourcePath());
            }
            try (DigestInputStream dis = new DigestInputStream(is, md)) {
                //noinspection StatementWithEmptyBody
                while (dis.read() != -1) {
                    // read stream to EOF
                }
                return sourceFile.withChecksum(new Checksum(algorithm, md.digest()));
            }
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public Checksum(final String algorithm, final byte[] value) {
        this.algorithm = algorithm;
        this.value = value;
    }

    public String getAlgorithm() {
        return this.algorithm;
    }

    public byte[] getValue() {
        return this.value;
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof Checksum)) return false;
        final Checksum other = (Checksum) o;
        final Object this$algorithm = this.getAlgorithm();
        final Object other$algorithm = other.getAlgorithm();
        if (this$algorithm == null ? other$algorithm != null : !this$algorithm.equals(other$algorithm)) return false;
        if (!java.util.Arrays.equals(this.getValue(), other.getValue())) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $algorithm = this.getAlgorithm();
        result = result * PRIME + ($algorithm == null ? 43 : $algorithm.hashCode());
        result = result * PRIME + java.util.Arrays.hashCode(this.getValue());
        return result;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "Checksum(algorithm=" + this.getAlgorithm() + ", value=" + java.util.Arrays.toString(this.getValue()) + ")";
    }
}
