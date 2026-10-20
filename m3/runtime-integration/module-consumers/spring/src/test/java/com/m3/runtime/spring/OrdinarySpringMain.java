/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.runtime.spring;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/** Fork entry: fixture is only on the boot module path, never the class path. */
public final class OrdinarySpringMain {
    private OrdinarySpringMain() { }
    public static void main(String[] args) throws Exception {
        Path origin = Path.of(PathMatchingResourcePatternResolver.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI());
        if (!Files.isSameFile(origin, Path.of(args[0]))) {
            throw new AssertionError("Stock Spring binary loaded instead of compiled receiving source: " + origin);
        }
        var module = ModuleLayer.boot().findModule("m3.spring.fixture").orElseThrow();
        var resolver = new PathMatchingResourcePatternResolver();
        var moduleRows = new ModuleProbe().moduleRows("modulefixture/*.class");
        if (!(moduleRows instanceof com.m3.collections.M3LinkedHashSet<?>)) {
            throw new AssertionError("Original public Spring method did not use M3 ordered storage");
        }
        var resources = resolver.getResources("classpath*:modulefixture/*.class");
        if (Arrays.stream(resources).noneMatch(r -> "Hello.class".equals(r.getFilename()))) {
            throw new AssertionError("Ordinary public resolver missed the module-only class");
        }
        try (var context = new AnnotationConfigApplicationContext()) {
            context.scan("modulefixture");
            context.refresh();
            Object bean = context.getBean("moduleHello");
            if (bean.getClass().getModule() != module) throw new AssertionError("Wrong defining module");
            if (!"ordinary bean".equals(bean.getClass().getMethod("value").invoke(bean))) {
                throw new AssertionError("Wrong bean value");
            }
        }
        System.out.println("ORDINARY_SPRING_SOURCE_AND_MODULE_BEAN_OK " + origin);
    }
    private static final class ModuleProbe extends PathMatchingResourcePatternResolver {
        java.util.Set<org.springframework.core.io.Resource> moduleRows(String pattern) throws java.io.IOException {
            return findAllModulePathResources(pattern);
        }
    }
}
