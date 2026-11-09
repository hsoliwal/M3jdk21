/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: GPL-2.0-only
 */

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.ProtectionDomain;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;

/** Injects only an allocation-failure edge before each real copy operation. */
public final class M3IndexedArrayGrowthAgent {
    public static volatile int sites;
    public static volatile Throwable failure;
    private static Instrumentation instrumentation;

    private M3IndexedArrayGrowthAgent() { }

    public static void premain(String hookJar, Instrumentation supplied) throws Exception {
        instrumentation = supplied;
        if ("measure".equals(hookJar)) return;
        supplied.appendToBootstrapClassLoaderSearch(new JarFile(hookJar));
        Class<?> hook = Class.forName("M3IndexedArrayGrowthFault", true, null);
        supplied.redefineModule(Object.class.getModule(), Set.of(hook.getModule()),
                Map.of(), Map.of(), Set.of(), Map.of());
        supplied.addTransformer(new Transformer());
    }

    public static long size(Object object) {
        if (instrumentation == null) throw new IllegalStateException("agent not installed");
        return instrumentation.getObjectSize(object);
    }

    private static final class Transformer implements ClassFileTransformer {
        @Override
        public byte[] transform(ClassLoader loader, String name, Class<?> redefined,
                ProtectionDomain domain, byte[] original) {
            if (!name.equals("jdk/internal/m3/M3IndexedArray")) return null;
            try {
                if (loader != null || redefined != null) {
                    throw new AssertionError("expected the first bootstrap owner definition");
                }
                ClassReader reader = new ClassReader(original);
                ClassWriter writer = new ClassWriter(reader, 0);
                int[] copies = {0};
                reader.accept(new ClassVisitor(Opcodes.ASM8, writer) {
                    @Override
                    public MethodVisitor visitMethod(int access, String method,
                            String descriptor, String signature, String[] exceptions) {
                        MethodVisitor visitor = super.visitMethod(access, method,
                                descriptor, signature, exceptions);
                        if (!method.equals("ensureCapacity") || !descriptor.equals("(I)V")) {
                            return visitor;
                        }
                        return new MethodVisitor(Opcodes.ASM8, visitor) {
                            @Override
                            public void visitMethodInsn(int opcode, String owner,
                                    String called, String type, boolean isInterface) {
                                if (opcode == Opcodes.INVOKESTATIC
                                        && owner.equals("java/util/Arrays")
                                        && called.equals("copyOf")
                                        && (type.equals("([Ljava/lang/Object;I)[Ljava/lang/Object;")
                                            || type.equals("([II)[I"))) {
                                    super.visitMethodInsn(Opcodes.INVOKESTATIC,
                                            "M3IndexedArrayGrowthFault", "beforeCopy", "()V", false);
                                    copies[0]++;
                                }
                                super.visitMethodInsn(opcode, owner, called, type, isInterface);
                            }
                        };
                    }
                }, 0);
                if (copies[0] != 7) throw new AssertionError("unexpected copy-site count " + copies[0]);
                byte[] transformed = writer.toByteArray();
                Path output = Path.of(System.getProperty("m3.growth.evidence", "."));
                Files.createDirectories(output);
                Files.write(output.resolve("owner-original.class"), original);
                Files.write(output.resolve("owner-fault-instrumented.class"), transformed);
                System.out.println("GROWTH_AGENT original_sha256=" + digest(original)
                        + " transformed_sha256=" + digest(transformed) + " sites=" + copies[0]);
                sites = copies[0];
                return transformed;
            } catch (Throwable problem) {
                failure = problem;
                return null; // The fixture checks failure and refuses an uninstrumented pass.
            }
        }

        private static String digest(byte[] bytes) throws Exception {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        }
    }
}
