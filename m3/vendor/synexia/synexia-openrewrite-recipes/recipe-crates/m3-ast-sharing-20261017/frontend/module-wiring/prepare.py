#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Seal reviewed Maven/service snapshots and the optional target package mapping.

This prepares recipe inputs; only actual OpenRewrite Results materialize owner files.
Existing preimages are retained once captured so re-preparation cannot admit drift.
"""
from pathlib import Path
from hashlib import sha1, sha256
import argparse
import json

HERE = Path(__file__).resolve().parent
FRONTEND = HERE.parent
RECIPES = HERE.parents[3]
SYNEXIA = RECIPES.parent
ENGINE = "synexia-openrewrite-recipes/src/main/java/com/synexia/rewrite/M3HashPinnedTextSnapshotRecipe.java"
CANONICAL_POM = "synexia-mindex/compiler/pom.xml"
TARGET_POM = "m3/ports/compiletime/pom.xml"
CANONICAL_SERVICE = "synexia-mindex/compiler/src/main/resources/META-INF/services/com.sun.source.util.Plugin"
TARGET_SERVICE = "m3/ports/compiletime/src/main/resources/META-INF/services/com.sun.source.util.Plugin"
OPTIONAL_SOURCE = "m3/ports/compiletime-openrewrite/src/main/java/com/m3/compiletime/ast/M3OpenRewriteSharedAST.java"


def digest(data):
    return sha256(data).hexdigest()


def preimage(label, path, root):
    saved = HERE / "preimages" / label / path
    if not saved.exists():
        saved.parent.mkdir(parents=True, exist_ok=True)
        saved.write_bytes((root / path).read_bytes())
    return saved.read_text()


def seal(kind, label, outputs, roots):
    resource = HERE / "resources/com/synexia/rewrite" / ("hash-pinned-" + kind) / ("m3-ast-wiring-" + label + "-20261017")
    resource.mkdir(parents=True, exist_ok=True)
    lines, pins = [], []
    for number, (path, text, before) in enumerate(sorted(outputs)):
        name = f"{number:03d}-" + Path(path).name + ".txt"
        data = text.encode()
        resource.joinpath(name).write_bytes(data)
        template = HERE / "templates" / label / name
        template.parent.mkdir(parents=True, exist_ok=True)
        template.write_bytes(data)
        prior = "ABSENT" if before is None else digest(before.encode())
        lines.append(f"{path}\t{prior}\t{digest(data)}\t{name}\n")
        pins.append({"path": path, "before_sha256": prior, "after_sha256": digest(data), "template": str(template.relative_to(HERE))})
    resource.joinpath("manifest.tsv").write_text("".join(lines))
    return {"representation": kind, "crate": resource.name, "files": pins}


NATIVE_EXCLUDES = """          <excludes>
            <exclude>**/*$*</exclude>
            <exclude>**/M3ASTWorkspaceCacheNativeTest.java</exclude>
          </excludes>"""

NATIVE_PROFILE = """    <!-- Explicit opt-in: the selected tests fail if -Dm3.cache.native is absent or invalid. -->
    <profile>
      <id>m3-ast-native</id>
      <build><plugins><plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <configuration>
          <excludes combine.self="override"><exclude>**/*$*</exclude></excludes>
        </configuration>
      </plugin></plugins></build>
    </profile>
"""

DEFERRED_SERVICE_RESOURCES = """    <!-- The provider must compile before javac can discover its service descriptor. -->
    <resources><resource>
      <directory>src/main/resources</directory>
      <excludes><exclude>META-INF/services/com.sun.source.util.Plugin</exclude></excludes>
    </resource></resources>
"""

DEFERRED_SERVICE_PLUGIN = """      <plugin>
        <groupId>org.apache.maven.plugins</groupId><artifactId>maven-resources-plugin</artifactId><version>3.3.1</version>
        <executions><execution>
          <id>m3-ast-plugin-service</id><phase>process-classes</phase><goals><goal>copy-resources</goal></goals>
          <configuration>
            <outputDirectory>${project.build.outputDirectory}</outputDirectory>
            <resources><resource>
              <directory>${project.basedir}/src/main/resources</directory><filtering>false</filtering>
              <includes><include>META-INF/services/com.sun.source.util.Plugin</include></includes>
            </resource></resources>
          </configuration>
        </execution></executions>
      </plugin>
"""


def optional_pom(target):
    group, artifact, version, core = ("com.m3", "m3-compiletime-openrewrite", "0.1.0-SNAPSHOT", "m3-compiletime-precompute") if target else ("com.synexia", "synexia-mindex-compiler-openrewrite", "1.0.0-SNAPSHOT", "synexia-mindex-compiler")
    exports = "\n".join("            <arg>--add-exports</arg><arg>jdk.compiler/com.sun.tools.javac." + package + "=ALL-UNNAMED</arg>" for package in ("api", "code", "comp", "tree", "util"))
    text = f'''<?xml version="1.0" encoding="UTF-8"?>
<!-- SPDX-License-Identifier: Apache-2.0 -->
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>{group}</groupId><artifactId>{artifact}</artifactId><version>{version}</version>
  <name>Optional Java 21 shared-AST OpenRewrite provider</name>
  <description>Explicitly selected projection of an existing compiler invocation; the core compiler module does not depend on this artifact.</description>
  <properties>
    <maven.compiler.source>21</maven.compiler.source><maven.compiler.target>21</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <rewrite.version>8.17.1</rewrite.version>
  </properties>
  <dependencies>
    <dependency><groupId>{group}</groupId><artifactId>{core}</artifactId><version>{version}</version></dependency>
    <dependency><groupId>org.openrewrite</groupId><artifactId>rewrite-java</artifactId><version>${{rewrite.version}}</version></dependency>
    <dependency><groupId>org.openrewrite</groupId><artifactId>rewrite-java-21</artifactId><version>${{rewrite.version}}</version></dependency>
  </dependencies>
  <build><plugins>
    <plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-compiler-plugin</artifactId><version>3.13.0</version>
      <configuration><proc>none</proc><compilerArgs>
            <arg>-Xlint:all</arg><arg>-Werror</arg>
{exports}
      </compilerArgs></configuration>
    </plugin>
  </plugins></build>
</project>
'''
    if target:
        text = text.replace("    <rewrite.version>8.17.1</rewrite.version>",
                "    <rewrite.version>8.17.1</rewrite.version>\n"
                "    <!-- Keep the core default; the documented CI override also controls transitive ASM here. -->\n"
                "    <asm.version>9.6-m3-source-1</asm.version>")
        management = "  <dependencyManagement><dependencies>\n" + "".join(
                "    <dependency><groupId>org.ow2.asm</groupId><artifactId>" + artifact
                + "</artifactId><version>${asm.version}</version></dependency>\n"
                for artifact in ("asm", "asm-tree", "asm-analysis")) + "  </dependencies></dependencyManagement>\n"
        text = text.replace("  <dependencies>\n", management + "  <dependencies>\n", 1)
    return text


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--target", type=Path, required=True)
    args = parser.parse_args()
    engine_before = preimage("engine", ENGINE, SYNEXIA)
    marker = "    String lower = value.toLowerCase(java.util.Locale.ROOT);"
    assert engine_before.count(marker) == 1
    engine_after = engine_before.replace(marker, """    // Exact compiler-plugin service owners; near-miss and unrelated service paths remain denied.
    boolean m3AstPluginService = value.equals(
        "synexia-mindex/compiler/src/main/resources/META-INF/services/com.sun.source.util.Plugin")
        || value.equals("m3/ports/compiletime/src/main/resources/META-INF/services/com.sun.source.util.Plugin");
""" + marker)
    engine_after = engine_after.replace("&& !jdkJavadocBootstrap && !invalidJavaSyntaxBootstrap && !structuredText) return false;", "&& !jdkJavadocBootstrap && !invalidJavaSyntaxBootstrap && !m3AstPluginService && !structuredText) return false;")
    engine_after = engine_after.replace("|| invalidJavaSyntaxBootstrap || structuredText\n", "|| invalidJavaSyntaxBootstrap || m3AstPluginService || structuredText\n")
    assert engine_after.count("m3AstPluginService") == 3

    original = json.loads((FRONTEND / "SOURCE_LINEAGE.json").read_text())["canonical_dependencies"]
    cache = json.loads((FRONTEND.parent / "cache/SOURCE_LINEAGE.json").read_text())["files"]
    paths = {entry["path"] for entry in original}
    assert len(paths) == 77
    paths.update(entry["source"] for entry in cache if entry["mode"] == "EXACT_VENDOR_CUSTODY" and entry["source"].endswith(".java"))
    assert len(paths) == 84
    ast = "synexia-mindex/compiler/src/main/java/com/synexia/mindex/compiler/ast/"
    paths.update(ast + name + ".java" for name in ("M3File", "M3CompilationContext", "M3AST", "M3JavacASTIndex", "M3ASTCodec", "M3ASTPlugin", "M3FileSourceFacts", "M3ASTWorkspaceCache"))
    assert len(paths) == 92
    hierarchy = json.loads((FRONTEND / "hierarchy/SOURCE_LINEAGE.json").read_text())
    assert len(hierarchy["partial_owner_preimages"]) == 9
    paths.update(entry["path"] for entry in hierarchy["partial_owner_preimages"])
    paths.update(ast + name + ".java" for name in ("MIndexASTHierarchy", "MIndexASTHierarchyFacts"))
    assert len(paths) == 103
    roots = sorted({path.split("/src/main/java/")[0] + "/src/main/java" for path in paths})
    package_paths = sorted(path.split("/src/main/java/")[1] for path in paths)
    assert len(set(package_paths)) == 103
    helper = '''      <!-- Sealed AST dependency closure: ordinary src/main/java remains a source root. -->
      <plugin>
        <groupId>org.codehaus.mojo</groupId><artifactId>build-helper-maven-plugin</artifactId><version>3.6.2</version>
        <executions><execution><id>m3-ast-vendor-closure</id><phase>generate-sources</phase><goals><goal>add-source</goal></goals>
          <configuration><sources>
''' + "".join("            <source>${project.basedir}/../../vendor/synexia/" + root + "</source>\n" for root in roots) + '''          </sources></configuration>
        </execution></executions>
      </plugin>
'''
    includes = """          <!-- 77 original dependencies, 7 cache dependencies, 8 facade/cache sources, and 11 hierarchy sources. -->
          <includes>
            <include>com/m3/**/*.java</include>
""" + "".join("            <include>" + path + "</include>\n" for path in package_paths) + "          </includes>\n"
    target_before = preimage("receiver", TARGET_POM, args.target)
    target_after = target_before.replace("  <build>\n", "  <build>\n" + DEFERRED_SERVICE_RESOURCES, 1)
    target_after = target_after.replace("    <plugins>\n", "    <plugins>\n" + helper + DEFERRED_SERVICE_PLUGIN, 1)
    target_after = target_after.replace("          <proc>none</proc>\n", "          <proc>none</proc>\n" + includes, 1)
    target_after = target_after.replace("<arg>-Xlint:all</arg><arg>-Werror</arg>", "<arg>-Xlint:all</arg><arg>-Werror</arg><arg>-implicit:none</arg>", 1)
    target_after = target_after.replace("          <argLine>-Xmx1g ${m3.opens}</argLine>", "          <argLine>-Xmx1g ${m3.opens}</argLine>\n" + NATIVE_EXCLUDES, 1)
    assert target_after != target_before and target_after.count("<include>") == 105
    target_after = target_after.replace("</project>", "  <profiles>\n" + NATIVE_PROFILE + "  </profiles>\n</project>")

    canonical_before = preimage("canonical", CANONICAL_POM, SYNEXIA)
    needle = "        <artifactId>maven-surefire-plugin</artifactId>\n"
    assert canonical_before.count(needle) == 1
    canonical_after = canonical_before.replace(needle, needle + "        <configuration>\n" + NATIVE_EXCLUDES + "\n        </configuration>\n")
    canonical_after = canonical_after.replace("  <build>\n", "  <build>\n" + DEFERRED_SERVICE_RESOURCES, 1)
    canonical_after = canonical_after.replace("    <plugins>\n", "    <plugins>\n" + DEFERRED_SERVICE_PLUGIN, 1)
    canonical_after = canonical_after.replace("  <profiles>\n", "  <profiles>\n" + NATIVE_PROFILE, 1)
    canonical_service_before = preimage("canonical", CANONICAL_SERVICE, SYNEXIA)
    service_bytes = canonical_service_before.encode()
    service_blob = sha1(b"blob " + str(len(service_bytes)).encode() + b"\0" + service_bytes).hexdigest()
    assert service_blob == "d81d5a8fce1e80a30e75a68e3b8232189fc8bf99"
    assert canonical_service_before.endswith("\n")
    canonical_service_after = canonical_service_before + "com.synexia.mindex.compiler.ast.M3ASTPlugin\n"

    optional = (FRONTEND / "openrewrite/target/candidate/synexia-mindex/compiler-openrewrite/src/main/java/com/synexia/mindex/compiler/ast/M3OpenRewriteSharedAST.java").read_text()
    assert optional.count("package com.synexia.mindex.compiler.ast;") == 1
    mapped_optional = optional.replace("package com.synexia.mindex.compiler.ast;", "package com.m3.compiletime.ast;")
    pins = [
        seal("java", "engine", [(ENGINE, engine_after, engine_before)], roots),
        seal("java", "receiver", [(OPTIONAL_SOURCE, mapped_optional, None)], roots),
        seal("text", "canonical", [(CANONICAL_POM, canonical_after, canonical_before), (CANONICAL_SERVICE, canonical_service_after, canonical_service_before), ("synexia-mindex/compiler-openrewrite/pom.xml", optional_pom(False), None)], roots),
        seal("text", "receiver", [(TARGET_POM, target_after, target_before), (TARGET_SERVICE, "com.m3.compiletime.ast.M3ASTPlugin\n", None), ("m3/ports/compiletime-openrewrite/pom.xml", optional_pom(True), None)], roots),
    ]
    # Preserve the exact first-attempt outputs that exposed service bootstrap ordering.
    # This local successor admits only those already-sealed intermediate POMs.
    for label, path, text in (("canonical", CANONICAL_POM, canonical_after), ("receiver", TARGET_POM, target_after)):
        prior = HERE / "preimages" / ("service-order-" + label) / path
        if prior.exists():
            pins.append(seal("text", "service-order-" + label, [(path, text, prior.read_text())], roots))
    # Exact local successors preserve the original receiver/canonical admission pins.
    for label, path, text in (("service-preservation-canonical", CANONICAL_SERVICE, canonical_service_after),
                              ("hierarchy-closure-receiver", TARGET_POM, target_after),
                              ("optional-transitives-receiver", "m3/ports/compiletime-openrewrite/pom.xml", optional_pom(True))):
        prior = HERE / "preimages" / label / path
        if prior.exists():
            pins.append(seal("text", label, [(path, text, prior.read_text())], roots))
    providers = json.loads(HERE.joinpath("CURRENT_PROVIDERS.json").read_text())
    HERE.joinpath("SOURCE_PINS.json").write_text(json.dumps({"schema": "M3_AST_MODULE_WIRING_SOURCE_PINS_V1", "synexia_base": "e244d68fdee88d4a406be48e4ff1d31b60a17988", "canonical_compiler_pom_git_blob": "c8883a706a6affac7333cd5b4c81aee32e0381f4", "canonical_compiler_pom_verified_unchanged_at": providers["revision"], "canonical_plugin_service_git_blob": service_blob, "canonical_plugin_service_verified_unchanged_at": providers["revision"], "canonical_service_policy": "append M3ASTPlugin after both existing providers in their original order", "hierarchy_lineage_sha256": digest((FRONTEND / "hierarchy/SOURCE_LINEAGE.json").read_bytes()), "recipes": pins, "historical_recipe_only": "m3-ast-wiring-engine-20261017", "active_providers": providers, "optional_canonical_source_sha256": digest(optional.encode()), "optional_mapping": "package declaration only"}, indent=2) + "\n")
    HERE.joinpath("VENDOR_CLOSURE.json").write_text(json.dumps({"schema": "M3_AST_MODULE_VENDOR_CLOSURE_V1", "count": len(paths), "ordinary_source_root": "src/main/java", "vendor_roots": roots, "vendor_sources": sorted(paths)}, indent=2) + "\n")
    print(json.dumps({"vendor_sources": len(paths), "engine_before_sha256": digest(engine_before.encode()), "engine_after_sha256": digest(engine_after.encode()), "recipes": len(pins)}))


if __name__ == "__main__":
    main()
