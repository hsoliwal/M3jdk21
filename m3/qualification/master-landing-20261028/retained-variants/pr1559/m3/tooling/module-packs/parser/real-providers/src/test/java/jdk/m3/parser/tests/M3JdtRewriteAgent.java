// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser.tests;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.ProtectionDomain;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Qualification-only instrumentation. Load the two source-built JDT owners in
 * their official provider's normal class loader and count actual ASTParser
 * entry calls. The official signed JAR remains unchanged. No production code
 * depends on this test agent or on ASM.
 */
public final class M3JdtRewriteAgent {
    public static final AtomicInteger PARSE_ENTRIES = new AtomicInteger();
    public static final AtomicInteger SOURCE_BUILT_OWNERS = new AtomicInteger();
    public static volatile boolean active;
    private M3JdtRewriteAgent() { }

    public static void recordParseEntry() {
        int entries = PARSE_ENTRIES.incrementAndGet();
        if (entries > 1 && Boolean.getBoolean("m3.jdt.traceParses")) {
            new Exception("Observed ASTParser entry " + entries).printStackTrace(System.err);
        }
    }

    public static void premain(String sourceBuiltClasses, Instrumentation instrumentation) throws Exception {
        if (sourceBuiltClasses == null) throw new IllegalArgumentException("source-built class directory required");
        Path root = Path.of(sourceBuiltClasses).toAbsolutePath().normalize();
        String rewrite = "org/eclipse/jdt/core/dom/rewrite/ASTRewrite";
        String store = "org/eclipse/jdt/internal/core/dom/rewrite/NodeInfoStore";
        Map<String, byte[]> owners = "COUNT_ONLY".equals(sourceBuiltClasses) ? Map.of() : Map.of(
                rewrite, Files.readAllBytes(root.resolve(rewrite + ".class")),
                store, Files.readAllBytes(root.resolve(store + ".class")));
        instrumentation.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String name, Class<?> redefined,
                    ProtectionDomain domain, byte[] original) {
                byte[] sourceBuilt = owners.get(name);
                if (sourceBuilt != null) {
                    SOURCE_BUILT_OWNERS.incrementAndGet();
                    return sourceBuilt;
                }
                if (!name.equals("org/eclipse/jdt/core/dom/ASTParser")) return null;
                ClassReader reader = new ClassReader(original);
                ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
                reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
                    @Override
                    public MethodVisitor visitMethod(int access, String method, String descriptor,
                            String signature, String[] exceptions) {
                        MethodVisitor next = super.visitMethod(access, method, descriptor, signature, exceptions);
                        if (!method.equals("createAST") && !method.equals("createASTs")) return next;
                        return new MethodVisitor(Opcodes.ASM9, next) {
                            @Override
                            public void visitCode() {
                                super.visitCode();
                                super.visitMethodInsn(Opcodes.INVOKESTATIC,
                                        "jdk/m3/parser/tests/M3JdtRewriteAgent", "recordParseEntry", "()V", false);
                            }
                        };
                    }
                }, 0);
                return writer.toByteArray();
            }
        });
        active = true;
    }
}
