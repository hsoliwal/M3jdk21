// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.indexdb;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * The two helpers the relocated hash-family contracts call on the donor's command-line front end, which is not ported
 * (RELOCATION_MAP.tsv SKIP row MACHINERY): {@link #option} reads {@code --name=value} arguments, {@link #hex} prints a
 * 64-bit value as 16 lower-case hex digits. The member bodies are the donor's {@code option} and {@code hex}
 * verbatim; the scoped TYPE rows of the map point the contracts here. Test tree only.
 */
//FLLDCIM: RELOCATE - member bodies of donor M3HashFamily.option/hex (blob 4715c7ab8280, SKIP row MACHINERY) for the scoped TYPE rows of m3/indexdb/recipe/hash-family-20261106/RELOCATION_MAP.tsv
final class M3HashFamilyContractSupport {
    private M3HashFamilyContractSupport() { }

    static Optional<String> option(String[] args, String prefix) {
        return Arrays.stream(args).filter(arg -> arg.startsWith(prefix)).map(arg -> arg.substring(prefix.length())).findFirst();
    }

    static String hex(long value) {
        return String.format(Locale.ROOT, "%016x", value);
    }
}
