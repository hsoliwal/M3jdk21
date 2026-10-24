/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package com.synexia.m3;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.ChangePackage;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

/** Contract: only isolated source forks get renamed; public provider JARs remain unchanged. */
final class FiveProviderChangePackageTest implements RewriteTest {
    @Override public void defaults(RecipeSpec spec) {
        spec.parser(org.openrewrite.java.JavaParser.fromJavaVersion());
    }

    @Test void guava() {
        rewrite("com.google.common", "com.m3.sdk.guava",
            "com.google.common.collect", "com.m3.sdk.guava.collect", true);
    }

    @Test void eclipse() {
        rewrite("org.eclipse.collections", "com.m3.sdk.eclipse.collections",
            "org.eclipse.collections.api.list", "com.m3.sdk.eclipse.collections.api.list", true);
    }

    @Test void apache() {
        rewrite("org.apache.commons.collections4", "com.m3.sdk.apache.commons.collections4",
            "org.apache.commons.collections4.bag", "com.m3.sdk.apache.commons.collections4.bag", true);
    }

    @Test void spring() {
        rewrite("org.springframework.util", "com.m3.sdk.spring.util",
            "org.springframework.util", "com.m3.sdk.spring.util", true);
    }

    @Test void restrictedJdk() {
        rewrite("java.util", "com.m3.sdk.jdk.util",
            "java.util", "com.m3.sdk.jdk.util", false);
    }

    private void rewrite(String oldRoot, String newRoot, String before, String after,
            boolean recursive) {
        rewriteRun(
            spec -> spec.recipe(new ChangePackage(oldRoot, newRoot, recursive)),
            java("package " + before + ";\n\nclass PackageFixture { }\n",
                 "package " + after + ";\n\nclass PackageFixture { }\n"));
    }
}
