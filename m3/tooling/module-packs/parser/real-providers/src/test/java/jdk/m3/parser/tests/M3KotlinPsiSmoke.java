// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser.tests;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import jdk.m3.parser.M3ASTAuthoritativeModels;
import jdk.m3.parser.M3ASTIndex;
import jdk.m3.parser.M3ASTIngress;
import jdk.m3.parser.M3File;

/** Parse real Kotlin PSI with the exact embedded Kotlin compiler, never a mock KtFile. */
public final class M3KotlinPsiSmoke {
    private M3KotlinPsiSmoke() { }

    private static Object construct(String className) throws Exception {
        return Class.forName(className).getConstructor().newInstance();
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 0) throw new IllegalArgumentException("no arguments");
        String source = "plugins { kotlin(\"jvm\") version \"2.0.21\" }\n"
                + "val msg = \"\"\"{\"text\": \"{}\"}\"\"\"\n";
        // Gradle's Kotlin DSL carries the embeddable/relocated IntelliJ classes.
        Class<?> disposer = Class.forName("org.jetbrains.kotlin.com.intellij.openapi.util.Disposer");
        Object owner = disposer.getMethod("newDisposable").invoke(null);
        try {
            Object config = construct("org.jetbrains.kotlin.config.CompilerConfiguration");
            Class<?> configKey = Class.forName("org.jetbrains.kotlin.config.CompilerConfigurationKey");
            Class<?> messageKeys = Class.forName("org.jetbrains.kotlin.cli.common.CLIConfigurationKeys");
            Field messageKey = messageKeys.getField("MESSAGE_COLLECTOR_KEY");
            Object none = Class.forName("org.jetbrains.kotlin.cli.common.messages.MessageCollector")
                    .getField("NONE").get(null);
            config.getClass().getMethod("put", configKey, Object.class)
                    .invoke(config, messageKey.get(null), none);
            Class<?> environmentType = Class.forName(
                    "org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment");
            Object configFiles = Class.forName(
                    "org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles")
                    .getField("JVM_CONFIG_FILES").get(null);
            Class<?> disposableType = Class.forName(
                    "org.jetbrains.kotlin.com.intellij.openapi.Disposable");
            Object environment = environmentType.getMethod("createForProduction",
                            disposableType, config.getClass(),
                            Class.forName("org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles"))
                    .invoke(null, owner, config, configFiles);
            Object project = environmentType.getMethod("getProject").invoke(environment);
            Class<?> factoryType = Class.forName("org.jetbrains.kotlin.psi.KtPsiFactory");
            Object factory = null;
            for (Constructor<?> ctor : factoryType.getConstructors()) {
                Class<?>[] parameters = ctor.getParameterTypes();
                if (parameters.length == 1 && parameters[0].isInstance(project)) {
                    factory = ctor.newInstance(project);
                    break;
                }
                if (parameters.length == 2 && parameters[0].isInstance(project)
                        && parameters[1] == boolean.class) {
                    factory = ctor.newInstance(project, false);
                    break;
                }
            }
            if (factory == null) throw new IllegalStateException("Kotlin KtPsiFactory constructor unavailable");
            Method createFile = factoryType.getMethod("createFile", String.class, String.class);
            Object original = createFile.invoke(factory, "build.gradle.kts", source);
            M3ASTIndex index = M3ASTAuthoritativeModels.gradleKotlinPsi(
                    M3File.of(URI.create("memory:/build.gradle.kts"), source),
                    original, new M3ASTIngress.Limits(100_000, 200_000, 1000));
            int realNodes = index.nodeCount();
            if (realNodes < 5 || !index.hasExactRange(0)
                    || !index.sourceText(0).equals(source)) {
                throw new AssertionError("real Kotlin PSI source/offset parity");
            }
            System.out.println("M3_AST_REAL_KOTLIN_PSI_PASS nodes=" + realNodes);
        } finally {
            // Always release Gradle Kotlin compiler/IntelliJ PSI facilities.
            Class<?> disposableType = Class.forName(
                    "org.jetbrains.kotlin.com.intellij.openapi.Disposable");
            disposer.getMethod("dispose", disposableType).invoke(null, owner);
        }
    }
}
