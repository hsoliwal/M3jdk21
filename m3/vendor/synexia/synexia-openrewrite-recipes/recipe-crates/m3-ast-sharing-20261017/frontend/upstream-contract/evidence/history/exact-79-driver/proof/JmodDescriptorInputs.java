// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
import java.nio.file.Files;
import java.nio.file.Path;
import jdk.internal.classfile.Classfile;
import jdk.internal.classfile.attribute.ModuleHashesAttribute;
import jdk.internal.classfile.attribute.ModulePackagesAttribute;

/**
 * Prepare an already decorated stock descriptor for standard jmod recreation.
 * JDK21 ModuleInfoExtender appends these generated attributes; retaining their
 * old copies would produce an invalid duplicate-attribute descriptor. No other
 * module contract or class entry is modified, and jmod recomputes both fields.
 */
public final class JmodDescriptorInputs {
    private JmodDescriptorInputs() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("one scratch descriptor path required");
        Path path = Path.of(args[0]);
        var model = Classfile.parse(Files.readAllBytes(path));
        int[] dropped = new int[2];
        byte[] prepared = model.transform((builder, element) -> {
            if (element instanceof ModulePackagesAttribute) dropped[0]++;
            else if (element instanceof ModuleHashesAttribute) dropped[1]++;
            else builder.with(element);
        });
        if (dropped[0] != 1 || dropped[1] != 1)
            throw new IllegalStateException("expected exactly one original package and hash attribute");
        Files.write(path, prepared);
        System.out.println("M3_JMOD_RECOMPUTED_INPUT_ATTRIBUTES ModulePackages=1 ModuleHashes=1");
    }
}
