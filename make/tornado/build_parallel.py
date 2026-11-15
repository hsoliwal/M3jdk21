#!/usr/bin/env python3
# Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation. Oracle designates this
# particular file as subject to the "Classpath" exception as provided
# by Oracle in the LICENSE file that accompanied this code.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).

"""Build the optional TornadoVM parallel modules into an existing M3JDK21 image.

Inputs are the source-locked fork, this product tree, and an installed JDK 21
image that already contains the internal bulk API. No source is transformed,
downloaded, or patched into java.base. OpenCL uses the host loader and ICD.
"""

import argparse
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import re
import subprocess
import sys


PRODUCT_ROOT = Path(__file__).resolve().parents[2]
SOURCE_LOCK = Path(__file__).with_name("source-lock.json")
SCHEMA = "m3jdk21-tornado-parallel-source-lock/1"
FORK_MODULES = frozenset({
    "tornado.api", "tornado.runtime", "tornado.drivers.common",
    "tornado.drivers.opencl", "tornado.tc.check", "tornado.tc.codegen",
    "tornado.tc.debug", "tornado.tc.frontend", "tornado.tc.ir",
    "tornado.tc.lower", "tornado.tc.opt", "tornado.tc.runtime",
    "tornado.tc.selftest", "tornado.tc.tornado",
})
PRODUCT_SOURCE_ROOTS = {
    "m3.parallel": (
        "m3/ports/parallel/src/main/java",
    ),
    "jdk.m3.tornado": (
        "src/jdk.m3.tornado/share/classes",
        "m3/ports/tornadovm-bulk/src/main/java",
        "m3/ports/parallel-bulk/src/main/java",
    ),
}
LEGAL_FILES = ("LICENSE_APACHE2", "THIRD_PARTY_NOTICES")
JAVA_FLAGS = (
    "--enable-preview",
    "--add-modules=jdk.m3.tornado,tornado.runtime,tornado.drivers.opencl",
    "--enable-native-access=jdk.m3.tornado,tornado.api,tornado.runtime,"
    "tornado.drivers.common,tornado.drivers.opencl",
    "--add-exports=java.base/jdk.internal.vm.parallel=jdk.m3.tornado",
    "-Dtornado.recover.bailout=false",
    "-Dtornado.virtual.device=false",
)


class BuildError(Exception):
    """An input or build step cannot satisfy this image's requirements."""


def sha256(data):
    return hashlib.sha256(data).hexdigest()


def relative_path(value):
    if not isinstance(value, str) or not value or "\\" in value:
        raise BuildError("source-lock path must be a nonempty POSIX relative path")
    path = PurePosixPath(value)
    if (path.is_absolute() or str(path) != value
            or any(part in (".", "..") for part in path.parts)
            or any(ord(char) < 32 for char in value)):
        raise BuildError("noncanonical source-lock path: " + repr(value))
    return path


def checked_path(root, relative, directory=False):
    path = root
    for component in relative_path(relative).parts:
        path = path / component
        if path.is_symlink():
            raise BuildError("symlink source is not admitted: " + str(path))
    if directory:
        if not path.is_dir():
            raise BuildError("source directory is missing: " + str(path))
    elif not path.is_file():
        raise BuildError("source file is missing: " + str(path))
    return path


def regular_tree(root, java_only=False):
    """Enumerate without traversing or silently overlooking symbolic links."""
    found = []
    for current, directories, files in os.walk(root, followlinks=False):
        for name in directories + files:
            path = Path(current) / name
            if path.is_symlink():
                raise BuildError("symlink source is not admitted: " + str(path))
        for name in files:
            path = Path(current) / name
            if not path.is_file():
                raise BuildError("nonregular source is not admitted: " + str(path))
            if not java_only or path.suffix == ".java":
                found.append(path)
    return sorted(found)


def distinct_object(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise BuildError("duplicate source-lock key: " + key)
        result[key] = value
    return result


def verify_source_lock(source, lock_path=SOURCE_LOCK):
    """Return verified source bytes; later compilation uses these exact bytes."""
    if lock_path.is_symlink():
        raise BuildError("source lock must not be a symlink")
    try:
        lock = json.loads(lock_path.read_text(encoding="utf-8"),
                          object_pairs_hook=distinct_object)
    except (OSError, ValueError) as error:
        raise BuildError("cannot read source lock: " + str(error)) from error
    if not isinstance(lock, dict) or lock.get("schema") != SCHEMA:
        raise BuildError("unsupported TornadoVM source-lock schema")
    if lock.get("repository") != "hsoliwal/synexia-tornadovm":
        raise BuildError("unexpected TornadoVM source repository")
    if not re.fullmatch(r"[0-9a-f]{40}", lock.get("commit", "")):
        raise BuildError("source-lock commit must be an exact Git commit")
    modules = lock.get("modules")
    if not isinstance(modules, list):
        raise BuildError("source-lock modules must be a list")
    module_roots = {}
    expected = {}
    for module in modules:
        if not isinstance(module, dict):
            raise BuildError("source-lock module must be an object")
        name = module.get("name")
        if name not in FORK_MODULES or name in module_roots:
            raise BuildError("unexpected or duplicate source module: " + str(name))
        java_root = checked_path(source, module.get("java_root"), directory=True)
        module_roots[name] = java_root
        for path in regular_tree(java_root, java_only=True):
            expected[path.relative_to(source).as_posix()] = (
                "java", name, path.relative_to(java_root).as_posix())
        resource_relative = module.get("resources_root")
        resource_root = java_root.parent / "resources"
        if resource_relative is None:
            if resource_root.exists() or resource_root.is_symlink():
                raise BuildError("unlocked resource tree: " + str(resource_root))
        else:
            declared = checked_path(source, resource_relative, directory=True)
            if declared != resource_root:
                raise BuildError("resources must belong to their declared module")
            for path in regular_tree(resource_root):
                expected[path.relative_to(source).as_posix()] = (
                    "resource", name, path.relative_to(resource_root).as_posix())
    if set(module_roots) != FORK_MODULES:
        raise BuildError("source-lock module closure is incomplete")
    for name in LEGAL_FILES:
        checked_path(source, name)
        expected[name] = ("legal", None, name)

    entries = lock.get("files")
    if not isinstance(entries, list) or not entries:
        raise BuildError("source-lock file inventory is empty")
    verified = {}
    for entry in entries:
        if not isinstance(entry, dict):
            raise BuildError("source-lock file must be an object")
        path = entry.get("path")
        relative_path(path)
        if path in verified:
            raise BuildError("duplicate source-lock file: " + path)
        if path not in expected:
            raise BuildError("file lies outside the locked source closure: " + path)
        if (entry.get("kind"), entry.get("module"), entry.get("relative")) != expected[path]:
            raise BuildError("source-lock module/path identity mismatch: " + path)
        digest = entry.get("sha256", "")
        if not re.fullmatch(r"[0-9a-f]{64}", digest):
            raise BuildError("invalid SHA-256 for source: " + path)
        data = checked_path(source, path).read_bytes()
        if sha256(data) != digest:
            raise BuildError("source hash drift: " + path)
        verified[path] = (entry, data)
    if set(verified) != set(expected):
        omitted = sorted(set(expected) - set(verified))
        raise BuildError("unlocked source/resource files: " + ", ".join(omitted[:10]))
    for name, root in module_roots.items():
        descriptor = (root / "module-info.java").relative_to(source).as_posix()
        if descriptor not in verified:
            raise BuildError("module descriptor is missing: " + name)
    return lock, verified


def absolute_directory(path, description, reject_symlinks=False):
    absolute = path.absolute()
    if reject_symlinks:
        for item in (absolute, *absolute.parents):
            if item.is_symlink():
                raise BuildError(description + " contains a symlink: " + str(item))
    if not absolute.is_dir():
        raise BuildError(description + " is missing: " + str(absolute))
    return absolute.resolve()


def overlaps(first, second):
    return first == second or first in second.parents or second in first.parents


def output_path(value, source, jdk, product):
    path = value.absolute()
    for item in (path, *path.parents):
        if item.is_symlink():
            raise BuildError("output path contains a symlink: " + str(item))
    path = path.resolve()
    if path.exists():
        raise BuildError("output must be a fresh directory: " + str(path))
    protected = [source, jdk, product / "make/tornado"]
    protected.extend(product / relative for roots in PRODUCT_SOURCE_ROOTS.values()
                     for relative in roots)
    if product == path or path in product.parents:
        raise BuildError("output contains the product source tree")
    if any(overlaps(path, root.resolve()) for root in protected):
        raise BuildError("output overlaps an input source or JDK directory")
    return path


def clean_java_environment():
    environment = os.environ.copy()
    for name in ("JAVA_TOOL_OPTIONS", "_JAVA_OPTIONS", "JDK_JAVA_OPTIONS", "CLASSPATH"):
        environment.pop(name, None)
    return environment


def execute(command, environment, log=None):
    result = subprocess.run([str(value) for value in command], env=environment,
                            text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    if log is not None:
        log.write_text(result.stdout, encoding="utf-8")
    if result.returncode:
        if result.stdout:
            print(result.stdout, file=sys.stderr, end="")
        raise BuildError("build command failed (" + str(result.returncode)
                         + "): " + str(command[0]))
    return result.stdout


def jdk_tools(jdk, environment):
    suffix = ".exe" if os.name == "nt" else ""
    tools = {name: jdk / "bin" / (name + suffix)
             for name in ("java", "javac", "javap", "jmod", "jlink")}
    for path in tools.values():
        if not path.is_file():
            raise BuildError("required JDK tool is missing: " + str(path))
    if not (jdk / "jmods/java.base.jmod").is_file():
        raise BuildError("the supplied JDK must contain its source-built jmods")
    version = execute([tools["javac"], "-version"], environment)
    if not re.match(r"javac 21(?:[.\s]|$)", version.strip()):
        raise BuildError("this source closure requires a JDK 21 compiler: " + version.strip())
    try:
        bulk = execute([tools["javap"], "--module", "java.base",
                        "jdk.internal.vm.parallel.BulkTask"], environment)
    except BuildError as error:
        raise BuildError("the installed base JDK must already contain java.base/"
                         "jdk.internal.vm.parallel.BulkTask; stock-JDK patching is not admitted") from error
    if "jdk.internal.vm.parallel.BulkTask" not in bulk:
        raise BuildError("the installed base JDK bulk API cannot be verified")
    return tools


def product_sources(product):
    result = {}
    for module, roots in PRODUCT_SOURCE_ROOTS.items():
        files = {}
        for index, relative in enumerate(roots):
            directory = checked_path(product, relative, directory=True)
            for path in regular_tree(directory, java_only=True):
                relative_file = path.relative_to(directory).as_posix()
                # The canonical src descriptor supersedes the historical module descriptor.
                if index and relative_file == "module-info.java":
                    continue
                if relative_file in files:
                    raise BuildError("duplicate product source: " + module + "/" + relative_file)
                files[relative_file] = path.read_bytes()
        if "module-info.java" not in files:
            raise BuildError("product module descriptor is missing: " + module)
        result[module] = files
    return result


def product_notices(product):
    result = {}
    for module, relative in (
            ("m3.parallel", "m3/ports/parallel"),
            ("jdk.m3.tornado", "m3/ports/tornadovm-bulk")):
        directory = checked_path(product, relative, directory=True)
        notices = {}
        for name in ("LICENSE", "NOTICE"):
            notices[name] = checked_path(product, relative + "/" + name).read_bytes()
        result[module] = notices
    return result


def write_bytes(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)


def vm_images(jdk):
    result = {}
    for parent in (jdk / "lib", jdk / "bin"):
        for name in ("libjvm.so", "libjvm.dylib", "jvm.dll"):
            for path in parent.rglob(name):
                if path.is_file():
                    result[path.relative_to(jdk).as_posix()] = sha256(path.read_bytes())
    if not result:
        raise BuildError("the supplied JDK has no installed JVM library")
    return result


def create_launcher(image):
    if os.name == "nt":
        command = "@echo off\r\n\"%~dp0java.exe\" " + " ".join(JAVA_FLAGS) + " %*\r\n"
        (image / "bin/m3-parallel.cmd").write_text(command, encoding="utf-8", newline="")
    else:
        command = ("#!/bin/sh\nset -eu\n"
                   "M3_PARALLEL_BIN=$(CDPATH= cd -- \"$(dirname -- \"$0\")\" && pwd)\n"
                   "exec \"$M3_PARALLEL_BIN/java\" "
                   + " ".join(JAVA_FLAGS) + " \"$@\"\n")
        launcher = image / "bin/m3-parallel"
        launcher.write_text(command, encoding="utf-8")
        launcher.chmod(0o755)


def build(source, jdk, out, product=PRODUCT_ROOT, verify_only=False):
    source = absolute_directory(source, "TornadoVM source", reject_symlinks=True)
    jdk = absolute_directory(jdk, "base JDK")
    product = absolute_directory(product, "M3JDK21 product source", reject_symlinks=True)
    out = output_path(out, source, jdk, product)
    lock_path = checked_path(product, "make/tornado/source-lock.json")
    lock, fork_files = verify_source_lock(source, lock_path)
    own_sources = product_sources(product)
    own_notices = product_notices(product)
    product_license = checked_path(product, "LICENSE").read_bytes()
    environment = clean_java_environment()
    tools = jdk_tools(jdk, environment)
    original_vm = vm_images(jdk)
    if verify_only:
        print("Verified pinned TornadoVM source closure and installed M3JDK21 bulk API.")
        return

    out.mkdir(parents=True, exist_ok=False)
    sources, classes, jmods = out / "sources", out / "modules", out / "jmods"
    classes.mkdir()
    jmods.mkdir()
    for entry, data in fork_files.values():
        if entry["kind"] == "java":
            write_bytes(sources / entry["module"] / entry["relative"], data)
    for module, files in own_sources.items():
        for relative, data in files.items():
            write_bytes(sources / module / relative, data)
    modules = sorted(FORK_MODULES | set(own_sources))
    print("Compiling " + str(len(modules)) + " modules from pinned sources.", flush=True)
    execute([tools["javac"], "--enable-preview", "-source", "21", "-target", "21",
             "--add-exports", "java.base/jdk.internal.vm.parallel=jdk.m3.tornado",
             "--module-source-path", sources, "-d", classes,
             "--module", ",".join(modules)], environment, out / "javac.log")
    for entry, data in fork_files.values():
        if entry["kind"] == "resource":
            destination = classes / entry["module"] / entry["relative"]
            if destination.exists():
                raise BuildError("resource collides with a compiled class: " + str(destination))
            write_bytes(destination, data)

    legal = out / "legal"
    for name in LEGAL_FILES:
        write_bytes(legal / "tornado" / name, fork_files[name][1])
    for module, notices in own_notices.items():
        for name, data in notices.items():
            write_bytes(legal / module / name, data)
    write_bytes(legal / "jdk.m3.tornado/M3JDK21_LICENSE", product_license)
    for module in modules:
        notices = legal / ("tornado" if module in FORK_MODULES else module)
        execute([tools["jmod"], "create", "--class-path", classes / module,
                 "--legal-notices", notices, jmods / (module + ".jmod")],
                environment, out / (module + ".jmod.log"))

    image = out / "image"
    print("Linking the supplied M3JDK21 image and parallel modules.", flush=True)
    execute([tools["jlink"], "--module-path", os.pathsep.join((str(jmods), str(jdk / "jmods"))),
             "--add-modules", "ALL-MODULE-PATH", "--output", image],
            environment, out / "jlink.log")
    if vm_images(image) != original_vm:
        raise BuildError("linked image JVM library differs from the supplied M3JDK21 image")
    create_launcher(image)
    installed = execute([image / "bin" / tools["java"].name, "--list-modules"],
                        environment, out / "installed-modules.log")
    # java --list-modules appends "open" to open modules without a version.
    # The first whitespace-delimited token owns the name and optional version.
    installed_names = {line.split()[0].split("@", 1)[0]
                       for line in installed.splitlines() if line.strip()}
    if not set(modules).issubset(installed_names):
        raise BuildError("linked image omits required parallel modules")
    print("Built " + str(image) + " from TornadoVM " + lock["commit"])
    print("Preserved every supplied JVM library byte-for-byte; OpenCL uses the host loader and ICD.")


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--tornado-source", "--source", dest="source", type=Path, required=True)
    parser.add_argument("--jdk-home", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    parser.add_argument("--verify-only", action="store_true",
                        help="validate inputs and installed bulk API without producing output")
    options = parser.parse_args(argv)
    try:
        build(options.source, options.jdk_home, options.out, verify_only=options.verify_only)
    except (BuildError, OSError) as error:
        parser.exit(1, "TornadoVM parallel image: " + str(error) + "\n")


if __name__ == "__main__":
    main()
