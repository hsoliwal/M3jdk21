// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** CLI for mechanical upstream/current FILE atom evidence generation. */
public final class M3JepFileAtomCandidateMain {
    private M3JepFileAtomCandidateMain() {}

    public static void main(String[] args) {
        if (args.length != 4) {
            throw new IllegalArgumentException(
                    "usage: M3JepFileAtomCandidateMain <current-root> <upstream-root> <paths.txt> <out-dir>");
        }
        Path paths = Path.of(args[2]).normalize();
        String text;
        try {
            text = Files.readString(paths, StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalArgumentException("cannot read path list " + paths, failure);
        }
        M3JepFileAtomCandidateGenerator.generate(
                Path.of(args[0]),
                Path.of(args[1]),
                M3JepFileAtomCandidateGenerator.parsePaths(text),
                Path.of(args[3]));
    }
}
