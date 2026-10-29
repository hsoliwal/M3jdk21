// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified for M3JDK: attributed package relocation and product module integration.
// SPDX-License-Identifier: Apache-2.0
package com.m3.llm.chrome;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Explicit production-only configuration; no runtime discovery or downloads are performed. */
public record ChromeMlProductionConfig(
        Path runtimeRoot,
        ChromeMlRuntimeLayout runtimeLayout,
        ChromeMlRuntimeLayout.ExpectedHashes expectedHashes,
        InetAddress bindAddress,
        int port,
        String bearerToken,
        Path mediaRoot,
        int maxConcurrentRequests,
        Duration requestTimeout,
        int maxBodyBytes,
        int maxCaptureBytes,
        long maxMediaBytes) {

    public ChromeMlProductionConfig {
        runtimeRoot = Objects.requireNonNull(runtimeRoot, "runtimeRoot").toAbsolutePath().normalize();
        runtimeLayout = Objects.requireNonNull(runtimeLayout, "runtimeLayout");
        expectedHashes = Objects.requireNonNull(expectedHashes, "expectedHashes");
        if (java.util.List.of(expectedHashes.runner(), expectedHashes.chromeMlDll(),
                expectedHashes.dawnDll(), expectedHashes.weights()).stream().anyMatch(String::isEmpty)) {
            throw new IllegalArgumentException("FOUR_APPROVED_HASHES_REQUIRED");
        }
        bindAddress = Objects.requireNonNull(bindAddress, "bindAddress");
        bearerToken = ChromeMlAdmissionPolicy.requireStrongToken(bearerToken);
        if (!bindAddress.isLoopbackAddress()) {
            throw new IllegalArgumentException("loopback bind required");
        }
        if (port < 1 || port > 65_535) {
            throw new IllegalArgumentException("port");
        }
        if (mediaRoot != null) {
            mediaRoot = mediaRoot.toAbsolutePath().normalize();
        }
        if (maxConcurrentRequests < 1 || maxConcurrentRequests > 1024) {
            throw new IllegalArgumentException("maxConcurrentRequests");
        }
        requestTimeout = Objects.requireNonNull(requestTimeout, "requestTimeout");
        if (requestTimeout.compareTo(Duration.ofMillis(1)) < 0
                || requestTimeout.compareTo(Duration.ofMinutes(30)) > 0) {
            throw new IllegalArgumentException("requestTimeout");
        }
        if (maxBodyBytes < 1024 || maxBodyBytes > 256 * 1024 * 1024) {
            throw new IllegalArgumentException("maxBodyBytes");
        }
        if (maxCaptureBytes < 1024 || maxCaptureBytes > 64 * 1024 * 1024) {
            throw new IllegalArgumentException("maxCaptureBytes");
        }
        if (maxMediaBytes < 1 || maxMediaBytes > 256L * 1024 * 1024) {
            throw new IllegalArgumentException("maxMediaBytes");
        }
    }

    public static ChromeMlProductionConfig fromEnvironment(Map<String, String> environment)
            throws IOException {
        Map<String, String> env = Objects.requireNonNull(environment, "environment");
        String rootText = required(env, "CHROMEML_ROOT");
        Path root = absolute(rootText, "CHROMEML_ROOT");
        ChromeMlPathPolicy.requireDirectoryNoLinks(root);

        ChromeMlRuntimeLayout defaults = ChromeMlRuntimeLayout.chrome2apiDefaults(root);
        ChromeMlRuntimeLayout layout = new ChromeMlRuntimeLayout(
                root,
                path(env, "CHROMEML_RUNNER", defaults.runner()),
                path(env, "CHROMEML_DLL", defaults.chromeMlDll()),
                path(env, "CHROMEML_DAWN_DLL", defaults.dawnDll()),
                path(env, "CHROMEML_WEIGHTS", defaults.weights()));

        ChromeMlRuntimeLayout.ExpectedHashes hashes =
                new ChromeMlRuntimeLayout.ExpectedHashes(
                        requiredSha(env, "CHROMEML_RUNNER_SHA256"),
                        requiredSha(env, "CHROMEML_DLL_SHA256"),
                        requiredSha(env, "CHROMEML_DAWN_DLL_SHA256"),
                        requiredSha(env, "CHROMEML_WEIGHTS_SHA256"));

        InetAddress address = numericLoopback(env.getOrDefault("CHROMEML_API_HOST", "127.0.0.1"));
        int port = integer(env, "CHROMEML_API_PORT", 11_435, 1, 65_535);
        String token = required(env, "CHROMEML_API_TOKEN");
        int maxConcurrent = integer(env, "CHROMEML_API_MAX_CONCURRENT", 2, 1, 1024);
        int timeoutMillis =
                integer(env, "CHROMEML_API_TIMEOUT_MS", 120_000, 1, 30 * 60 * 1000);
        int maxBody =
                integer(env, "CHROMEML_API_MAX_BODY_BYTES", 10 * 1024 * 1024, 1024, 256 * 1024 * 1024);
        int maxCapture =
                integer(env, "CHROMEML_API_MAX_CAPTURE_BYTES", 16 * 1024 * 1024, 1024, 64 * 1024 * 1024);
        long maxMedia =
                longInteger(env, "CHROMEML_API_MAX_MEDIA_BYTES", 64L * 1024 * 1024, 1, 256L * 1024 * 1024);

        Path mediaRoot = null;
        String media = env.get("CHROMEML_MEDIA_ROOT");
        if (media != null && !media.isBlank()) {
            mediaRoot = ChromeMlPathPolicy.requireDirectoryNoLinks(absolute(media, "CHROMEML_MEDIA_ROOT"));
        }

        return new ChromeMlProductionConfig(
                root,
                layout,
                hashes,
                address,
                port,
                token,
                mediaRoot,
                maxConcurrent,
                Duration.ofMillis(timeoutMillis),
                maxBody,
                maxCapture,
                maxMedia);
    }

    public ChromeMlRuntimeLayout.Identity doctor() throws IOException {
        if (mediaRoot != null) {
            ChromeMlPathPolicy.requireDirectoryNoLinks(mediaRoot);
        }
        return runtimeLayout.validate(expectedHashes);
    }

    @Override public String toString() {
        return "ChromeMlProductionConfig[port=" + port + ",bearerToken=REDACTED]";
    }

    private static Path absolute(String value, String name) {
        Path path = Path.of(value);
        if (!path.isAbsolute()) throw new IllegalArgumentException(name + " must be an absolute selected path");
        return path.normalize();
    }

    private static Path path(
            Map<String, String> env,
            String name,
            Path fallback) {
        String value = env.get(name);
        return value == null || value.isBlank()
                ? fallback
                : absolute(value, name);
    }

    private static String required(Map<String, String> env, String name) {
        String value = env.get(name);
        if (value == null || value.isBlank() || value.indexOf('\0') >= 0) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.strip();
    }

    private static String requiredSha(Map<String, String> env, String name) {
        String value = required(env, name).toLowerCase(Locale.ROOT);
        if (!value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(name + " must be SHA-256");
        }
        return value;
    }

    private static int integer(
            Map<String, String> env,
            String name,
            int fallback,
            int minimum,
            int maximum) {
        long value = longInteger(env, name, fallback, minimum, maximum);
        return Math.toIntExact(value);
    }

    private static long longInteger(
            Map<String, String> env,
            String name,
            long fallback,
            long minimum,
            long maximum) {
        String raw = env.get(name);
        long value;
        try {
            value = raw == null || raw.isBlank() ? fallback : Long.parseLong(raw.strip());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(name + " must be an integer", exception);
        }
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(name + " out of range");
        }
        return value;
    }

    private static InetAddress numericLoopback(String raw) {
        String value = Objects.requireNonNull(raw, "host").strip();
        if ("::1".equals(value)) {
            byte[] address = new byte[16];
            address[15] = 1;
            try {
                return InetAddress.getByAddress(address);
            } catch (UnknownHostException impossible) {
                throw new IllegalStateException(impossible);
            }
        }
        String[] parts = value.split("\\.", -1);
        if (parts.length != 4 || !"127".equals(parts[0])) {
            throw new IllegalArgumentException("CHROMEML_API_HOST must be a numeric loopback address");
        }
        byte[] address = new byte[4];
        for (int index = 0; index < parts.length; index++) {
            int octet;
            try {
                if (!parts[index].matches("[0-9]{1,3}")
                        || parts[index].length() > 3
                        || (parts[index].length() > 1 && parts[index].startsWith("0"))) {
                    throw new NumberFormatException();
                }
                octet = Integer.parseInt(parts[index]);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(
                        "CHROMEML_API_HOST must be a numeric loopback address",
                        exception);
            }
            if (octet < 0 || octet > 255) {
                throw new IllegalArgumentException(
                        "CHROMEML_API_HOST must be a numeric loopback address");
            }
            address[index] = (byte) octet;
        }
        try {
            return InetAddress.getByAddress(address);
        } catch (UnknownHostException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
