/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.image;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/** Backend-neutral build options. */
public record M3BuildOptions(String imageName, Path cacheDirectory, Path nativeImageExecutable,
                             Duration timeout, List<String> backendArguments) {
    /** Normalizes options. */
    public M3BuildOptions {
        if (imageName == null || !imageName.matches("[A-Za-z0-9._-]+")) {
            throw new IllegalArgumentException("Invalid image name: " + imageName);
        }
        cacheDirectory = cacheDirectory == null ? null : cacheDirectory.toAbsolutePath().normalize();
        nativeImageExecutable = nativeImageExecutable == null ? null : nativeImageExecutable.toAbsolutePath().normalize();
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("Build timeout must be positive");
        }
        backendArguments = List.copyOf(backendArguments);
    }
}
