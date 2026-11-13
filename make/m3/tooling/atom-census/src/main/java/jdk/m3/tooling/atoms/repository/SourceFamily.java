/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.atoms.repository;

import java.util.Locale;
import java.util.Objects;

/** Closed source-family classification used to dispatch later language-specific atomizers. */
public enum SourceFamily {
    /** Java source. */
    JAVA,
    /** C source. */
    C,
    /** C++ source. */
    CPP,
    /** Native header or inline include source. */
    HEADER,
    /** HotSpot architecture-description source. */
    HOTSPOT_AD,
    /** Assembly source. */
    ASSEMBLY,
    /** Build-system source. */
    BUILD,
    /** Executable repository script. */
    SCRIPT,
    /** Declarative, documentation, generated-data or binary resource. */
    RESOURCE,
    /** Tracked file not yet assigned to a more specific family. */
    OTHER;

    /**
     * Classifies a normalized repository-relative path without reading working-tree bytes.
     *
     * @param path normalized Git tree path
     * @return closed source family
     */
    public static SourceFamily classify(String path) {
        Objects.requireNonNull(path, "path");
        String lower = path.toLowerCase(Locale.ROOT);
        String name = lower.substring(lower.lastIndexOf('/') + 1);

        if (lower.endsWith(".java")) {
            return JAVA;
        }
        if (lower.endsWith(".ad")) {
            return HOTSPOT_AD;
        }
        if (lower.endsWith(".c") || lower.endsWith(".m") || lower.endsWith(".c.in")) {
            return C;
        }
        if (lower.endsWith(".cc") || lower.endsWith(".cpp") || lower.endsWith(".cxx")
                || lower.endsWith(".mm") || lower.endsWith(".cc.in") || lower.endsWith(".cpp.in")
                || lower.endsWith(".cxx.in")) {
            return CPP;
        }
        if (lower.endsWith(".h") || lower.endsWith(".hh") || lower.endsWith(".hpp")
                || lower.endsWith(".hxx") || lower.endsWith(".inc") || lower.endsWith(".inl")
                || lower.endsWith(".h.in") || lower.endsWith(".hh.in") || lower.endsWith(".hpp.in")
                || lower.endsWith(".hxx.in")) {
            return HEADER;
        }
        if (lower.endsWith(".s") || lower.endsWith(".asm")) {
            return ASSEMBLY;
        }
        if (name.equals("makefile") || name.equals("gnumakefile") || name.equals("configure")
                || name.equals("configure.ac") || name.equals("pom.xml") || name.equals("gradlew")
                || name.equals("mvnw") || name.equals("cmakelists.txt") || name.equals("dockerfile")
                || name.equals("build") || name.equals("build.bazel") || name.equals("workspace")
                || name.equals("workspace.bazel") || name.equals("meson.build") || lower.endsWith(".gmk")
                || lower.endsWith(".mk") || lower.endsWith(".m4") || lower.endsWith(".gradle")
                || lower.endsWith(".gradle.kts") || lower.endsWith(".bzl")
                || lower.startsWith(".github/workflows/")) {
            return BUILD;
        }
        if (lower.endsWith(".sh") || lower.endsWith(".bash") || lower.endsWith(".ksh")
                || lower.endsWith(".zsh") || lower.endsWith(".py") || lower.endsWith(".pl")
                || lower.endsWith(".rb") || lower.endsWith(".awk") || lower.endsWith(".tcl")
                || lower.endsWith(".lua") || lower.endsWith(".groovy") || lower.endsWith(".js")
                || lower.endsWith(".mjs") || lower.endsWith(".ps1") || lower.endsWith(".cmd")
                || lower.endsWith(".bat")) {
            return SCRIPT;
        }
        if (lower.endsWith(".md") || lower.endsWith(".txt") || lower.endsWith(".tsv")
                || lower.endsWith(".csv") || lower.endsWith(".properties")
                || lower.endsWith(".xml") || lower.endsWith(".json") || lower.endsWith(".yaml")
                || lower.endsWith(".yml") || lower.endsWith(".html") || lower.endsWith(".css")
                || lower.endsWith(".xhtml") || lower.endsWith(".xsl") || lower.endsWith(".xslt")
                || lower.endsWith(".xsd") || lower.endsWith(".dtd") || lower.endsWith(".rng")
                || lower.endsWith(".conf") || lower.endsWith(".cfg") || lower.endsWith(".ini")
                || lower.endsWith(".jfc") || lower.endsWith(".png") || lower.endsWith(".jpg")
                || lower.endsWith(".jpeg") || lower.endsWith(".gif") || lower.endsWith(".svg")
                || lower.endsWith(".classlist") || lower.endsWith(".sym") || lower.endsWith(".dat")
                || lower.endsWith(".bin") || lower.endsWith(".jar") || lower.endsWith(".zip")
                || lower.endsWith(".gz") || lower.endsWith(".ico") || lower.endsWith(".ttf")
                || lower.endsWith(".otf") || lower.endsWith(".wav")) {
            return RESOURCE;
        }
        return OTHER;
    }

    /**
     * Returns the closed file-container pattern identity for this family.
     *
     * @return non-unknown pattern identity
     */
    public String patternId() {
        return "M3-FILE-CONTAINER/" + name() + "/1";
    }
}
