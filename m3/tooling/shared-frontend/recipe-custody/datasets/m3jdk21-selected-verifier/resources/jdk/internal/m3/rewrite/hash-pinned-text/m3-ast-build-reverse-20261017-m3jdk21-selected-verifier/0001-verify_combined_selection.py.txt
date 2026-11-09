#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Execute independently selected Maven/OpenRewrite packs on a relocated M3 image."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
from xml.sax.saxutils import escape


def digest(path):
    with Path(path).open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ("repository", "jdk", "maven-lock", "rewrite-lock", "maven-repository", "out"):
        parser.add_argument("--" + name, type=Path, required=True)
    args = parser.parse_args()
    repository, jdk, maven_repository = (
        p.resolve(strict=True) for p in (args.repository, args.jdk, args.maven_repository))
    out = args.out.absolute()
    out.mkdir(parents=True, exist_ok=False)
    logs = out / "logs"
    logs.mkdir()
    commands = []
    source_image = {name: digest(jdk / name) for name in ("release", "lib/modules")}

    def run(command, label, cwd=None, expected=0, timeout=300):
        environment = os.environ.copy()
        for name in ("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS",
                     "MAVEN_OPTS", "MAVEN_ARGS", "CLASSPATH"):
            environment.pop(name, None)
        # Both dispatchers must choose their own image despite this different JAVA_HOME.
        environment["JAVA_HOME"] = str(jdk)
        result = subprocess.run([str(part) for part in command], cwd=cwd, env=environment,
                                stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                text=True, timeout=timeout)
        log = logs / (label + ".txt")
        log.write_text(result.stdout)
        commands.append({"label": label, "argv": [str(part) for part in command],
                         "cwd": str(cwd) if cwd else None, "returncode": result.returncode,
                         "log": str(log), "log_sha256": digest(log)})
        (out / "commands.json").write_text(json.dumps(commands, indent=2) + "\n")
        if result.returncode != expected:
            raise RuntimeError(f"{label}: expected exit {expected}, got {result.returncode}; see {log}")
        return result.stdout

    packs, lock_inputs = [], []
    for lock_path, selected_name in ((args.maven_lock, "maven"), (args.rewrite_lock, "openrewrite")):
        lock_path = lock_path.resolve(strict=True)
        original = lock_path.read_bytes()
        lock = json.loads(original)
        if lock.get("schema_version") != 1:
            raise ValueError("Unsupported supplied lock")
        candidates = [entry for entry in lock["packs"] if entry["name"] == selected_name]
        if len(candidates) != 1:
            raise ValueError("Expected one supplied pack: " + selected_name)
        pack = candidates[0]
        for field in ("assets", "modules"):
            for item in pack.get(field, []):
                item["file"] = str((lock_path.parent / item["file"]).resolve(strict=True))
        packs.append(pack)
        if lock_path.read_bytes() != original:
            raise ValueError("Input lock changed")
        lock_inputs.append({"name": selected_name, "path": str(lock_path),
                            "sha256": hashlib.sha256(original).hexdigest()})
    unavailable_plugin = out / "intentionally-absent-unselected-plugin.jar"
    packs.append({"name": "unselected-fixture-plugin", "version": "test-only",
                  "source": "Explicit nonselected-payload refusal fixture", "license": "Apache-2.0",
                  "roots": ["java.base"], "modules": [],
                  "assets": [{"file": str(unavailable_plugin), "sha256": "0" * 64,
                              "target": "lib/m3-tools/unselected-fixture-plugin/unused.jar",
                              "executable": False}]})
    combined = out / "combined-tool-lock.json"
    combined.write_text(json.dumps({"schema_version": 1, "packs": packs}, indent=2) + "\n")
    build_output = out / "linked"
    run([sys.executable, repository / "m3/tooling/module-packs/build_pack.py",
         "--jdk", jdk, "--out", build_output, "--tool-pack-lock", combined,
         "--tool-pack", "maven", "--tool-pack", "openrewrite"], "build-selected-image", timeout=1800)
    assembly = json.loads((build_output / "RECEIPT.json").read_text())
    if assembly["tool_packs"]["packs"] != ["maven", "openrewrite"]:
        raise ValueError("Unexpected tool selection")
    original_image = build_output / "image"
    run([original_image / "bin/m3-mvn", "--version"], "maven-before-relocation")
    run([original_image / "bin/m3-rewrite", "--help"], "rewrite-before-relocation")
    image = out / "relocated image with spaces"
    original_image.rename(image)
    assets = assembly["tool_packs"]["assets"]
    for asset in assets:
        installed = image / asset["target"]
        if digest(installed) != asset["sha256"]:
            raise ValueError("Relocated asset changed: " + asset["target"])
        if bool(installed.stat().st_mode & 0o111) != asset["executable"]:
            raise ValueError("Relocated asset mode changed: " + asset["target"])
    if unavailable_plugin.exists() or (image / "lib/m3-tools/unselected-fixture-plugin").exists():
        raise ValueError("Unselected plugin was materialized")
    for tool in ("gradle", "ant"):
        if (image / ("bin/m3-" + tool)).exists() or (image / ("lib/m3-tools/" + tool)).exists():
            raise ValueError("Unselected build tool was installed")
    version = run([image / "bin/m3-mvn", "--version"], "maven-after-relocation")
    if "Apache Maven 4.1.0-SNAPSHOT" not in version or str(image) not in version:
        raise ValueError("Dispatcher did not use the source-built Maven4 and selected image")
    run([image / "bin/m3-rewrite", "--help"], "rewrite-after-relocation")

    project = out / "project with spaces"
    source_root = project / "src/main/java"
    (source_root / "demo").mkdir(parents=True)
    source_texts = {
        "demo/Api.java": 'package demo;\npublic final class Api {\n'
                         '  public static final int VALUE = 42;\n'
                         '  public static String oldName(String value) { return value + "!"; }\n}\n',
        "demo/Use.java": 'package demo;\npublic final class Use {\n'
                         '  public static void main(String[] args) {\n'
                         '    System.out.println(Api.oldName("M3_TOOLS"));\n  }\n}\n',
    }
    for name, content in source_texts.items():
        (source_root / name).write_text(content)
    input_hashes = {name: digest(source_root / name) for name in source_texts}
    trace, primitive, slots = [project / name for name in ("maven-trace.tsv", "constants.m3prim", "slots.tsv")]
    # javac splits plugin options on whitespace; task-owned relative names preserve
    # an otherwise space-containing project directory without inventing quoting.
    plugin_arg = (f"-Xplugin:M3AST lossless trace={trace.name} image={primitive.name} "
                  f"slots={slots.name} fold-expressions")
    pom = ('<project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion>'
           '<groupId>org.m3.fixture</groupId><artifactId>selected-tools</artifactId><version>1</version>'
           '<properties><maven.compiler.release>21</maven.compiler.release></properties>'
           '<build><plugins><plugin><groupId>org.apache.maven.plugins</groupId>'
           '<artifactId>maven-compiler-plugin</artifactId><version>3.16.0</version>'
           '<configuration><proc>none</proc><compilerArgs><arg>' + escape(plugin_arg) + '</arg>'
           '</compilerArgs></configuration></plugin></plugins></build></project>\n')
    (project / "pom.xml").write_text(pom)
    settings = out / "offline-settings.xml"
    settings.write_text('<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0"><offline>true</offline></settings>\n')
    run([image / "bin/m3-mvn", "-o", "-B", "-ntp", "-s", settings,
         "-Dmaven.repo.local=" + str(maven_repository), "-Dm3.ast=true",
         "org.apache.maven.plugins:maven-compiler-plugin:3.16.0:compile"], "maven-compile", cwd=project)
    stats = dict(re.findall(r"(\w+)=(\S+)", trace.read_text().splitlines()[-1]))
    if stats.get("parsed") != "2" or stats.get("errors") != "0" or stats.get("completed") != "true" or stats.get("extraSourceReads") != "0":
        raise ValueError("Selected Maven did not capture the actual successful compiler task")
    data = primitive.read_bytes()
    if data[:8] != b"M3PRIM01" or not slots.read_text().startswith("# M3PRIM01 SHA-256 " + data[24:56].hex()):
        raise ValueError("Invalid selected-image primitive output")
    behavior = run([image / "bin/java", "-cp", project / "target/classes", "demo.Use"], "compiled-program")
    if behavior.strip() != "M3_TOOLS!":
        raise ValueError("Selected-image compiled behavior mismatch")

    recipe_text = '''type: specs.openrewrite.org/v1beta/recipe
name: demo.Rename
displayName: Rename a typed API
description: Exercise the full selected framework with shared compiler units.
recipeList:
  - org.openrewrite.java.ChangeMethodName:
      methodPattern: 'demo.Api oldName(java.lang.String)'
      newMethodName: renamed
      matchOverrides: false
      ignoreDefinition: false
'''
    recipe = project / "rewrite.yml"
    recipe.write_text(recipe_text)
    patch = project / "review.patch"
    rewrite = run([image / "bin/m3-rewrite", "--source-root", source_root,
                   "--source", "demo/Api.java", "--source", "demo/Use.java",
                   "--recipe", "demo.Rename", "--recipe-config", recipe, "--patch", patch,
                   "--", "--release", "21", "-proc:none"], "typed-java-recipe", cwd=project)
    compiler_lines = [line for line in rewrite.splitlines() if line.startswith("M3RewriteCompiler ")]
    if len(compiler_lines) != 1:
        raise ValueError("Expected exactly one real compiler summary")
    rewrite_stats = dict(re.findall(r"(\w+)=(\S+)", compiler_lines[0]))
    for key, expected in {"parsed": "2", "analyzed": "2", "inputLsts": "2", "conversions": "2",
                          "sourceChecks": "2", "errors": "0", "completed": "true"}.items():
        if rewrite_stats.get(key) != expected:
            raise ValueError("Unexpected actual shared compiler statistic: " + key)
    patch_lines = patch.read_text().splitlines()
    changed_lines = {line[1:].strip() for line in patch_lines if line.startswith("+") and not line.startswith("+++")}
    expected_additions = {'public static String renamed(String value) { return value + "!"; }',
                          'System.out.println(Api.renamed("M3_TOOLS"));'}
    if not expected_additions.issubset(changed_lines) or any("+++ b/" + name not in patch_lines for name in source_texts):
        raise ValueError("Typed recipe did not change both the declaration and its caller")
    if {name: digest(source_root / name) for name in source_texts} != input_hashes:
        raise ValueError("Recipe changed an original source file")
    if {name: digest(jdk / name) for name in source_image} != source_image:
        raise ValueError("Source-built input JDK changed")
    for item in lock_inputs:
        if digest(item["path"]) != item["sha256"]:
            raise ValueError("Supplied pack lock changed during execution")
    receipt = {"status": "SELECTED_MAVEN_OPENREWRITE_IMAGE_PASS", "image": str(image),
               "source_jdk": str(jdk), "source_jdk_sha256": source_image,
               "builder_sha256": digest(repository / "m3/tooling/module-packs/build_pack.py"),
               "verifier_sha256": digest(__file__), "selected_packs": ["maven", "openrewrite"],
               "input_locks": lock_inputs, "combined_lock_sha256": digest(combined),
               "relocated_with_spaces": True, "verified_asset_count": len(assets),
               "unselected_missing_plugin_not_opened": True, "unselected_gradle_ant_absent": True,
               "maven_compiler_stats": stats, "maven_version": version,
               "primitive_sha256": digest(primitive), "slots_sha256": digest(slots),
               "java_recipe_summary": rewrite, "java_recipe_stats": rewrite_stats,
               "review_patch": patch.read_text(),
               "input_sources": source_texts, "input_sources_sha256": input_hashes,
               "pom": pom, "recipe_yaml": recipe_text, "commands": commands,
               "framework_nonjava_execution": "SEPARATE_FRAMEWORK_JUNIT_GATE",
               "platform": "Linux x86_64; no native Windows execution claim"}
    (out / "RECEIPT.json").write_text(json.dumps(receipt, indent=2, sort_keys=True) + "\n")
    print("M3_SELECTED_TOOLS_IMAGE_PASS", out)


if __name__ == "__main__":
    main()
