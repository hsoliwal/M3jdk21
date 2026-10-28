#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Product-specific Linux x64 proof for the two existing java.base mask-window JNI methods."""

import argparse
import concurrent.futures
import hashlib
import json
import os
import pathlib
import platform
import re
import shutil
import subprocess
import time

BASE_PIN = "2779aba3f98f31c324be43f731be7914e1a05935"
DONOR_PIN = "900a723619fb7021e41b6e2c48a773fd41407224"
DONOR_JAVA_SHA256 = "f42473470989a103062696f1a3f4aa348e524308391edcaffff3df311d35eacf"
JAVA_PATH = "src/java.base/share/classes/jdk/internal/m3/M3IndexMaskWindow.java"
C_PATH = "src/java.base/share/native/libjava/M3IndexMaskWindow.c"
CORE_PATH = "src/java.base/share/classes/java/util/CollectionViews.java"
HEADER = "jdk_internal_m3_M3IndexMaskWindow.h"
NATIVE_TESTS = ["M3IndexMaskWindowIntegrationTest", "M3IndexMaskWindowJniTest"]
CORE_TESTS = ["BootstrapViewsTest", "CollectionViewsCoreContract",
              "CollectionViewsCowBulkTest", "CollectionViewsIdentityTest"]

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--repo", required=True, type=pathlib.Path)
parser.add_argument("--jdk", required=True, type=pathlib.Path)
parser.add_argument("--work", required=True, type=pathlib.Path, help="new or empty output directory")
parser.add_argument("--target-root", type=pathlib.Path, help="review candidate tree; defaults to repo")
parser.add_argument("--base-pin", default=BASE_PIN)
parser.add_argument("--donor-java", type=pathlib.Path, help="optional exact #1557 Java source")
parser.add_argument("--compiler-image", action="store_true", help="also build an ALL-MODULE-PATH image")
args = parser.parse_args()
repo, jdk, work = (p.resolve() for p in [args.repo, args.jdk, args.work])
target = (args.target_root or repo).resolve()
if platform.system() != "Linux" or platform.machine() not in ("x86_64", "amd64"):
    raise SystemExit("This recorded qualification supports Linux x64; other platforms remain pending.")
if work.exists() and any(work.iterdir()):
    raise SystemExit("Refusing a nonempty work directory; preserve earlier evidence.")
work.mkdir(parents=True, exist_ok=True)
for name in ["logs", "receiver-src", "classes", "headers", "tests", "tools",
             "source", "header-owners", "build", "build/objects", "build/headers"]:
    (work / name).mkdir(parents=True, exist_ok=True)
for tool in ["git", "tar", "gcc", "nm"]:
    if shutil.which(tool) is None:
        raise SystemExit("Required tool not found: " + tool)
for tool in ["java", "javac", "jmod", "jlink"]:
    if not (jdk / "bin" / tool).is_file():
        raise SystemExit("A complete JDK 21 is required: " + tool)
commands, images, inputs, workloads = [], {}, {}, []


def sha(path):
    return hashlib.sha256(pathlib.Path(path).read_bytes()).hexdigest()


def save(name, value):
    (work / name).write_text(json.dumps(value, indent=2) + "\n")


def run(label, command, expected=0, contains=None, check_jni=False, cwd=None):
    command = [str(x) for x in command]
    log_path = work / "logs" / (label + ".log")
    invocation = command
    timer = pathlib.Path("/usr/bin/time")
    if timer.is_file():
        invocation = [str(timer), "-v", "-o", str(work / "logs" / (label + ".time")), "--", *command]
    start = time.perf_counter()
    with log_path.open("w") as log:
        log.write("COMMAND " + json.dumps(command) + "\n")
        log.flush()
        process = subprocess.Popen(invocation, stdout=log, stderr=subprocess.STDOUT, cwd=cwd or work)
        _, status, usage = os.wait4(process.pid, 0)
        process.returncode = os.waitstatus_to_exitcode(status)
        log.write("EXIT " + str(process.returncode) + "\n")
    output = log_path.read_text()
    receipt = {"label": label, "command": command, "exit": process.returncode,
               "elapsed_seconds": time.perf_counter() - start,
               "user_seconds": usage.ru_utime, "sys_seconds": usage.ru_stime,
               "max_rss_kib": usage.ru_maxrss, "log_sha256": sha(log_path),
               "result_lines": [line for line in output.splitlines()
                                if "PASS" in line or line.startswith("MASK_WINDOW_")]}
    commands.append(receipt)
    correct_exit = process.returncode == expected if expected is not None else process.returncode != 0
    if not correct_exit or (contains and contains not in output) or (
            check_jni and "WARNING in native method" in output):
        save("commands.json", commands)
        raise RuntimeError("Qualification failed: " + label + "; inspect " + str(log_path))
    return receipt


def git_text(*arguments):
    return subprocess.check_output(["git", *arguments], cwd=repo, text=True)


def archive(label, paths, destination):
    archive_path = work / (label + ".tar")
    with archive_path.open("wb") as output:
        subprocess.run(["git", "archive", args.base_pin, *paths], cwd=repo, stdout=output, check=True)
    run(label + "-extract", ["tar", "-xf", archive_path, "-C", destination])
    inputs[label] = {"revision": args.base_pin, "paths": paths, "sha256": sha(archive_path)}


def copy_input(relative):
    original = target / relative
    inputs[relative] = {"sha256": sha(original)}
    destination = work / "receiver-src" / relative.removeprefix("src/java.base/share/classes/")
    destination.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(original, destination)
    return destination


run("java-version", [jdk / "bin/java", "-version"], contains='version "21.')
run("compiler-version", ["gcc", "--version"])
inputs["supplied-jdk"] = {"root": str(jdk), "files": {path: sha(jdk / path) for path in [
    "release", "bin/java", "bin/javac", "bin/jmod", "bin/jlink", "lib/modules",
    "lib/libjava.so", "lib/jspawnhelper", "lib/server/libjvm.so", "jmods/java.base.jmod"]}}
resolved_pin = git_text("rev-parse", args.base_pin).strip()
if resolved_pin != args.base_pin:
    raise SystemExit("Use the full immutable base commit id.")
owner = copy_input(JAVA_PATH)
core = copy_input(CORE_PATH)
inputs[C_PATH] = {"sha256": sha(target / C_PATH)}
run("compile-receivers", [jdk / "bin/javac", "--patch-module", "java.base=" + str(work / "receiver-src"),
    "-d", work / "classes", "-h", work / "headers", owner, core])
test_sources = [target / ("test/jdk/jdk/internal/m3/" + name + ".java") for name in NATIVE_TESTS]
test_sources += [target / ("test/jdk/java/util/CollectionViews/" + name + ".java") for name in CORE_TESTS]
for source in test_sources:
    inputs[str(source.relative_to(target))] = {"sha256": sha(source)}
run("compile-oracles", [jdk / "bin/javac", "--patch-module", "java.base=" + str(work / "classes"),
    "--add-exports", "java.base/jdk.internal.m3=ALL-UNNAMED", "--add-modules", "jdk.management",
    "-d", work / "tests", *test_sources])

donor = work / "donor-src/jdk/internal/m3/M3IndexMaskWindow.java"
donor.parent.mkdir(parents=True)
if args.donor_java:
    shutil.copy2(args.donor_java, donor)
else:
    donor.write_text(git_text("show", DONOR_PIN + ":" + JAVA_PATH))
if sha(donor) != DONOR_JAVA_SHA256:
    raise SystemExit("Refusing unexpected #1557 Java donor bytes.")
inputs["donor-java"] = {"revision": DONOR_PIN, "sha256": sha(donor)}
for name in ["donor-classes", "donor-headers"]:
    (work / name).mkdir()
run("compile-original-donor", [jdk / "bin/javac", "--patch-module", "java.base=" + str(work / "donor-src"),
    "-d", work / "donor-classes", "-h", work / "donor-headers", donor])
export = ["--add-exports", "java.base/jdk.internal.m3=ALL-UNNAMED"]
run("donor-unavailable-red", [jdk / "bin/java", "-Xcheck:jni", "--patch-module",
    "java.base=" + str(work / "donor-classes"), *export, "-cp", work / "tests",
    NATIVE_TESTS[0], "unavailable"], expected=None,
    contains="nativeAvailable accepted libjava without mask-window symbols")
run("candidate-unavailable-green", [jdk / "bin/java", "-Xcheck:jni", "--patch-module",
    "java.base=" + str(work / "classes"), *export, "-cp", work / "tests",
    NATIVE_TESTS[0], "unavailable"], contains="PASS", check_jni=True)
if (work / "headers" / HEADER).read_bytes() != (work / "donor-headers" / HEADER).read_bytes():
    raise SystemExit("Existing JNI method declarations changed.")

archive("pinned-libjava", [
    "src/java.base/share/native/libjava", "src/java.base/unix/native/libjava",
    "src/java.base/linux/native/libjava", "src/java.base/share/native/include",
    "src/java.base/unix/native/include", "src/hotspot/share/include",
    "src/hotspot/os/posix/include", "make/modules/java.base/lib/CoreLibraries.gmk",
    "src/java.base/unix/native/jspawnhelper", "make/modules/java.base/Launcher.gmk"], work / "source")
source = work / "source"
all_names = {p.name for p in source.rglob("*") if p.is_file()}
headers = set()
for path in source.rglob("*"):
    if path.suffix in (".c", ".h"):
        headers.update(re.findall(r'^#include [<"]((?:java_|jdk_|sun_)[^>"]+\.h)[>"]',
                                  path.read_text(), re.M))
headers = sorted(h for h in headers if h not in all_names and "M3" not in h
                 and h != "java_props_macosx.h")
header_list = work / "installed-headers.list"
header_list.write_text("\n".join(headers) + "\n")
helper_sources = pathlib.Path(__file__).parent / "src/test/java"
helper = helper_sources / "HeaderFromInstalledClasses.java"
inputs["src/test/java/HeaderFromInstalledClasses.java"] = {"sha256": sha(helper)}
process_probe = helper_sources / "ProcessBuilderProbe.java"
inputs["src/test/java/ProcessBuilderProbe.java"] = {"sha256": sha(process_probe)}
exports = [arg for part in ["api", "code", "jvm"]
           for arg in ["--add-exports", "jdk.compiler/com.sun.tools.javac." + part + "=ALL-UNNAMED"]]
run("compile-header-tool", [jdk / "bin/javac", *exports, "-d", work / "tools", helper, process_probe])
run("generate-installed-headers", [jdk / "bin/java", *exports, "-cp", work / "tools",
    "HeaderFromInstalledClasses", work / "build/headers", header_list])

tree = git_text("ls-tree", "-r", "--name-only", args.base_pin,
                "src/java.base/share/classes/java/lang", "src/java.base/share/classes/jdk/internal/mindex")
closure = [p for p in tree.splitlines() if "/mindex/" in p or "/M3" in p]
closure.append("src/java.base/share/classes/java/lang/String.java")
archive("pinned-string-closure", closure, work / "header-owners")
class_sources = work / "header-owners/src/java.base/share/classes"
class_roots = ["jdk/internal/mindex/M3RX7.java", "jdk/internal/mindex/M3BitLane28.java",
    "jdk/internal/mindex/M3RegexDpPlan.java", "java/lang/M3StringRegexDp.java",
    "java/lang/M3StringPool.java", "java/lang/String.java"]
(work / "closure-classes").mkdir()
run("compile-pinned-string-closure", [jdk / "bin/javac", "--patch-module", "java.base=" + str(class_sources),
    "-d", work / "closure-classes", "-h", work / "build/headers",
    *[class_sources / path for path in class_roots]])

include_paths = [work / "build/headers", work / "headers",
    *[source / ("src/java.base/" + part + "/native/libjava") for part in ["share", "unix", "linux"]],
    source / "src/java.base/share/native/include", source / "src/java.base/unix/native/include",
    source / "src/hotspot/share/include", source / "src/hotspot/os/posix/include",
    jdk / "include", jdk / "include/linux"]
flags = ["-std=gnu17", "-O2", "-fPIC", "-fno-strict-aliasing", "-fstack-protector-strong",
    "-D_GNU_SOURCE", "-D_LARGEFILE64_SOURCE", "-DLINUX", "-D_LINUX", '-DARCHPROPNAME="amd64"',
    "-Werror=implicit-function-declaration",
    *[arg for path in include_paths for arg in ["-I", str(path)]]]
sources = sorted(path for part in ["share", "unix", "linux"]
    for path in (source / ("src/java.base/" + part + "/native/libjava")).glob("*.c")
    if path.name != "M3IndexMaskWindow.c")
sources.append(target / C_PATH)
if len({path.name for path in sources}) != len(sources):
    raise SystemExit("Refusing ambiguous platform source names.")


def compile_c(path):
    output = work / "build/objects" / (path.stem + ".o")
    strict = ["-Wall", "-Wextra", "-Werror", "-Wpedantic"] if path == target / C_PATH else []
    run("compile-" + path.stem, ["gcc", *flags, *strict, "-c", path, "-o", output])
    return {"source": str(path), "source_sha256": sha(path),
            "object": str(output), "object_sha256": sha(output)}


with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:
    objects = list(pool.map(compile_c, sources))
save("native-source-manifest.json", objects)


def link(label, extra=()):
    output = work / "build" / (label + ".so")
    run("link-" + label, ["gcc", "-shared", "-Wl,-z,defs", "-Wl,-z,relro", "-Wl,-z,now",
        "-Wl,-soname,libjava.so", "-Wl,-rpath,$ORIGIN", "-Wl,-rpath,$ORIGIN/server", *extra,
        "-o", output, *[item["object"] for item in objects], "-L", jdk / "lib/server",
        "-ljvm", "-ldl", "-lm", "-pthread"])
    return output


library = link("libjava")
process_helper = work / "build/jspawnhelper"
run("build-pinned-jspawnhelper", ["gcc", "-std=gnu17", "-O1", "-fPIE", "-fstack-protector-strong",
    "-D_GNU_SOURCE", "-D_LARGEFILE64_SOURCE", "-DLINUX", "-D_LINUX",
    "-Werror=implicit-function-declaration", "-I", source / "src/java.base/unix/native/libjava",
    source / "src/java.base/unix/native/jspawnhelper/jspawnhelper.c", work / "build/objects/childproc.o",
    "-Wl,-z,relro", "-Wl,-z,now", "-o", process_helper])
expected = sorted(set(re.findall(r"Java_jdk_internal_m3_M3IndexMaskWindow_\w+",
                                (work / "headers" / HEADER).read_text())))


def symbols(path):
    output = subprocess.check_output(["nm", "-D", "--defined-only", path], text=True)
    return sorted(set(re.findall(r"Java_jdk_internal_m3_M3IndexMaskWindow_\w+", output)))


if symbols(library) != expected or len(expected) != 2:
    raise SystemExit("Generated JNI header and libjava export set disagree.")
save("abi.json", {"generated_header_sha256": sha(work / "headers" / HEADER),
     "donor_header_identical": True, "exports": expected, "extra_receiver_exports": 0})

description = subprocess.check_output([jdk / "bin/jmod", "describe", jdk / "jmods/java.base.jmod"], text=True)
module_version = description.splitlines()[0].split("@", 1)[1]
target_platform = next(line.split(" ", 1)[1] for line in description.splitlines() if line.startswith("platform "))


def image(label, native_library, string_closure=True, modules="java.base,jdk.management", matched_helper=True):
    module = work / ("module-" + label)
    jmods = work / ("jmods-" + label)
    output = work / ("image-" + label)
    jmods.mkdir()
    run(label + "-jmod-extract", [jdk / "bin/jmod", "extract", "--dir", module, jdk / "jmods/java.base.jmod"])
    shutil.copy2(native_library, module / "lib/libjava.so")
    if matched_helper:
        shutil.copy2(process_helper, module / "lib/jspawnhelper")
    overlays = [work / "classes"]
    if string_closure:
        overlays.append(work / "closure-classes")
    for classes in overlays:
        shutil.copytree(classes, module / "classes", dirs_exist_ok=True)
    command = [jdk / "bin/jmod", "create", "--target-platform", target_platform,
        "--module-version", module_version, "--class-path", module / "classes"]
    for option, folder in [("--cmds", "bin"), ("--config", "conf"), ("--header-files", "include"),
            ("--legal-notices", "legal"), ("--libs", "lib"), ("--man-pages", "man")]:
        if (module / folder).exists():
            command.extend([option, module / folder])
    command.append(jmods / "java.base.jmod")
    run(label + "-jmod-create", command)
    run(label + "-jlink", [jdk / "bin/jlink", "--module-path", str(jmods) + ":" + str(jdk / "jmods"),
        "--add-modules", modules, "--output", output])
    images[label] = {"libjava_sha256": sha(output / "lib/libjava.so"),
        "modules_sha256": sha(output / "lib/modules"), "libjvm_sha256": sha(output / "lib/server/libjvm.so"),
        "modules": modules, "string_closure": string_closure,
        "matched_process_helper": matched_helper, "jspawnhelper_sha256": sha(output / "lib/jspawnhelper"),
        "overlay_classes": {str(path.relative_to(root)): sha(path)
                           for root in overlays for path in sorted(root.rglob("*.class"))}}
    return output / "bin/java"


negative_java = image("missing-string", library, string_closure=False)
run("missing-string-closure-red", [negative_java, "-Xcheck:jni", "-version"],
    expected=None, contains="java.lang.String.m3")
spawn_args = ["-Xcheck:jni", "-Djdk.lang.Process.launchMechanism=POSIX_SPAWN",
              "-cp", work / "tools", "ProcessBuilderProbe"]
missing_helper_java = image("missing-helper", library, matched_helper=False)
run("stock-process-helper-red", [missing_helper_java, *spawn_args], expected=None,
    contains="Failed to exec spawn helper")
actual_java = image("combined", library)
run("matching-process-helper-green", [actual_java, *spawn_args], contains="PROCESS_BUILDER_PASS", check_jni=True)
for execution in ["-Xint", "-Xmixed"]:
    common = [actual_java, "-Xcheck:jni", execution, "--add-modules", "jdk.management", *export,
        "--add-opens", "java.base/jdk.internal.m3=ALL-UNNAMED",
        "--add-opens", "java.base/java.util=ALL-UNNAMED", "-cp", work / "tests"]
    matrix = [(NATIVE_TESTS[0], ["image"]), (NATIVE_TESTS[1], []), (NATIVE_TESTS[1], ["native"])]
    matrix += [(name, []) for name in CORE_TESTS]
    for index, (name, parameters) in enumerate(matrix):
        label = "combined-" + execution[2:] + "-" + str(index) + "-" + name
        stack = ["-Xss256k"] if name == "CollectionViewsCoreContract" else []
        receipt = run(label, [*common, *stack, name, *parameters], check_jni=True)
        workloads.append(receipt)

# A real source-built compatibility fixture: hide only the bounded export.
# This adds no native symbol and leaves the full-remainder ABI callable.
hide = work / "hide-bounded.map"
hide.write_text("{ global: *; local: Java_jdk_internal_m3_M3IndexMaskWindow_scanBoundedNative; };\n")
legacy = link("libjava-legacy", ["-Wl,--version-script=" + str(hide)])
if symbols(legacy) != ["Java_jdk_internal_m3_M3IndexMaskWindow_scanNative"]:
    raise SystemExit("Legacy compatibility fixture did not isolate the intended missing ABI.")
legacy_java = image("legacy", legacy)
run("legacy-one-symbol-refusal-green", [legacy_java, "-Xcheck:jni", *export,
    "-cp", work / "tests", NATIVE_TESTS[0], "unavailable"], contains="PASS", check_jni=True)
if args.compiler_image:
    compiler_java = image("compiler", library, modules="ALL-MODULE-PATH")
    run("compiler-image-version", [compiler_java.with_name("javac"), "-version"], contains="javac 21.")
    run("compiler-process-helper-green", [compiler_java, *spawn_args], contains="PROCESS_BUILDER_PASS", check_jni=True)

save("inputs.json", inputs)
save("images.json", images)
save("commands.json", commands)
save("generated-headers.json", {str(path.relative_to(work)): sha(path)
    for root in [work / "headers", work / "build/headers"] for path in sorted(root.glob("*.h"))})
summary = {
    "status": "PASS", "base_revision": args.base_pin, "donor_revision": DONOR_PIN,
    "native_translation_units": len(sources), "jni_methods": expected,
    "combined_workloads": len(workloads), "jni_warnings": 0,
    "process_launch": "Source-built matching jspawnhelper: POSIX_SPAWN shell and child JVM passed",
    "workload_elapsed_seconds_sum": sum(r["elapsed_seconds"] for r in workloads),
    "workload_user_seconds_sum": sum(r["user_seconds"] for r in workloads),
    "workload_sys_seconds_sum": sum(r["sys_seconds"] for r in workloads),
    "maximum_workload_child_rss_kib": max(r["max_rss_kib"] for r in workloads),
    "measurement": "Sequential fresh JVMs. Linux wait4 CPU/maxRSS; GNU time -v logs when installed. No speedup comparison.",
    "scope": "Installed target Java/libjava source closure atop supplied JDK21 HotSpot and remaining module binaries.",
    "pending": ["Full source-built HotSpot and complete make images", "Windows/macOS ABI",
                "Sanitizers and broader GC/concurrency stress", "Performance and native-memory comparative budgets"]}
save("summary.json", summary)
print(json.dumps(summary, indent=2))
