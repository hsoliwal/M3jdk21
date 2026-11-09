// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleReader;
import java.util.Set;
import java.util.stream.Collectors;

/** Actual named-module boundary probe; contains no provider or source-transform stub. */
public final class ModuleSmoke {
    private ModuleSmoke() { }

    public static void main(String[] args) throws Exception {
        Module module = ModuleLayer.boot().findModule("jdk.m3.llm").orElseThrow();
        ModuleDescriptor descriptor = module.getDescriptor();
        Set<String> expected = Set.of("jdk.m3.llm.chrome", "jdk.m3.llm.summary",
                "jdk.m3.llm.facets", "jdk.m3.llm.document");
        Set<String> actual = descriptor.exports().stream()
                .map(ModuleDescriptor.Exports::source).collect(Collectors.toSet());
        if (!actual.equals(expected) || descriptor.isOpen() || !descriptor.opens().isEmpty()) {
            throw new AssertionError("SDK exports/internality drift: " + descriptor);
        }
        if (descriptor.exports().stream().anyMatch(ModuleDescriptor.Exports::isQualified)) {
            throw new AssertionError("unexpected qualified export");
        }
        Set<String> requires = descriptor.requires().stream()
                .map(ModuleDescriptor.Requires::name).collect(Collectors.toSet());
        if (!requires.equals(Set.of("java.base"))) {
            throw new AssertionError("unexpected runtime dependency: " + requires);
        }
        for (String name : Set.of("jdk.m3.llm.chrome.ChromeProtocol",
                "jdk.m3.llm.chrome.ChromeBrowserPort", "jdk.m3.llm.summary.Summary",
                "jdk.m3.llm.summary.Summaries", "jdk.m3.llm.facets.FacetProjection",
                "jdk.m3.llm.document.Desm")) {
            Class<?> owner = Class.forName(name, false, ClassLoader.getSystemClassLoader());
            if (owner.getModule() != module) throw new AssertionError("wrong owner: " + name);
        }
        Class<?> helper = Class.forName("jdk.m3.llm.internal.Json", false,
                ClassLoader.getSystemClassLoader());
        if (helper.getModule() != module || module.isExported(helper.getPackageName())) {
            throw new AssertionError("internal helpers escaped module encapsulation");
        }
        try (ModuleReader reader = ModuleLayer.boot().configuration()
                .findModule("jdk.m3.llm").orElseThrow().reference().open()) {
            if (reader.find("jdk/m3/llm/summary/casefold.tsv").isEmpty()) {
                throw new AssertionError("Unicode case-fold resource missing");
            }
        }
        System.out.println("M3_LLM_MODULE_PASS exports=4 runtimeDependencies=java.base");
    }
}
