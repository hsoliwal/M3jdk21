// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
import java.nio.file.Files;
import java.nio.file.Path;
import jdk.internal.org.objectweb.asm.Attribute;
import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Opcodes;

/** Build-only step: discard stale compiler hashes before jmod recomputes the complete set. */
public final class RemoveRecordedModuleHashes {
    private RemoveRecordedModuleHashes() { }

    public static void main(String[] args) throws Exception {
        Path file = Path.of(args[0]);
        ClassReader reader = new ClassReader(Files.readAllBytes(file));
        ClassWriter writer = new ClassWriter(reader, 0);
        int[] removed = {0};
        reader.accept(new ClassVisitor(Opcodes.ASM8, writer) {
            @Override public void visitAttribute(Attribute attribute) {
                if (attribute.type.equals("ModuleHashes")) removed[0]++;
                else super.visitAttribute(attribute);
            }
        }, 0);
        if (removed[0] != 1) throw new IllegalStateException("Expected one recorded ModuleHashes attribute");
        Files.write(file, writer.toByteArray());
    }
}
