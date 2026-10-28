# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Verify the exact HotSpot owner and shipped Object ABI; no product writes."""
import argparse
import csv
import hashlib
import json
from pathlib import Path
import re
import shlex
import subprocess

parser = argparse.ArgumentParser()
parser.add_argument("--target", required=True, type=Path)
parser.add_argument("--jdk", required=True, type=Path)
parser.add_argument("--command-file", type=Path)
args = parser.parse_args()
target = args.target.resolve()
jdk = args.jdk.resolve()
crate = Path(__file__).resolve().parent.parent
relative = Path("src/hotspot/share/classfile/classLoader.cpp")
resources = crate / "src/main/resources/com/synexia/rewrite/hash-pinned-text/m3-object-base-vtable-20261018"
manifest = (resources / "manifest.tsv").read_text().strip().split("\t")
assert len(manifest) == 4 and manifest[0] == relative.as_posix()
digest = lambda value: hashlib.sha256(value).hexdigest()
before = (resources / ("before-" + manifest[3])).read_bytes()
candidate = crate / "target/candidate" / relative
after = candidate.read_bytes()
assert digest(before) == manifest[1] and digest(after) == manifest[2]
assert digest((target / relative).read_bytes()) in manifest[1:3], "receiver owner drift"

old = b"""int ClassLoader::compute_Object_vtable() {
  // hardwired for JDK1.2 -- would need to duplicate class file parsing
  // code to determine actual value from file
  // Would be value '11' if finals were in vtable
  int JDK_1_2_Object_vtable_size = 5;
  return JDK_1_2_Object_vtable_size * vtableEntry::size();
}"""
new = b"""int ClassLoader::compute_Object_vtable() {
  // M3JDK21 Object has six non-final instance methods: hashCode, equals,
  // clone, toString, finalize, and toAdapter. Reserve their slots before
  // Object is loaded so early array klass metadata has the same vtable size.
  const int M3_Object_vtable_size = 6;
  return M3_Object_vtable_size * vtableEntry::size();
}"""
assert before.count(old) == 1 and after == before.replace(old, new)
assert old.count(b"\n") == new.count(b"\n"), "preserve remaining source line identities"

# Ten independently sealed Java/VM context owners retain the public contract,
# parser checks, native wait registration and early array metadata consumers.
context = list(csv.DictReader((crate / "contract/context/manifest.tsv").open(), delimiter="\t"))
assert len(context) == 10 and len({row["path"] for row in context}) == 10
for row in context:
    assert digest((target / row["path"]).read_bytes()) == row["sha256"], "context drift: " + row["path"]
symbols_source = (target / "src/hotspot/share/classfile/vmSymbols.hpp").read_text()
assert re.search(r'template\(wait_name,\s+"wait0"\)', symbols_source)
vtable_source = (target / "src/hotspot/share/oops/klassVtable.cpp").read_text()
assert 'super == nullptr && vtable_length != Universe::base_vtable_size()' in vtable_source
assert 'vm_exit_during_initialization("Incompatible definition of java.lang.Object")' in vtable_source

out = crate / "target/hotspot-owner-proof"
out.mkdir(parents=True, exist_ok=True)
object_class = target / "build/m3-p0/jdk/modules/java.base/java/lang/Object.class"
javap = subprocess.run([str(jdk / "bin/javap"), "-p", "-s", str(object_class)],
                       capture_output=True, check=True, text=True, timeout=60)
assert not javap.stderr
(out / "Object.javap.txt").write_text(javap.stdout)
virtuals = {}
for declaration, descriptor in re.findall(r"^  ([^\n;]+);\n    descriptor: ([^\n]+)$", javap.stdout, re.MULTILINE):
    if declaration.startswith("public java.lang.Object("):
        continue
    if any(word in declaration.split() for word in ["static", "private", "final"]):
        continue
    name = declaration.split("(", 1)[0].split()[-1]
    assert name not in virtuals
    virtuals[name] = descriptor
expected = {
    "toAdapter": "(Ljava/lang/Class;)Ljava/lang/Object;",
    "hashCode": "()I", "equals": "(Ljava/lang/Object;)Z",
    "clone": "()Ljava/lang/Object;", "toString": "()Ljava/lang/String;",
    "finalize": "()V",
}
assert virtuals == expected, virtuals
assert "private final native void wait0(long)" in javap.stdout
assert "public final void wait(long)" in javap.stdout

command_file = args.command_file or target / "build/m3-p0/hotspot/variant-server/libjvm/objs/classLoader.o.cmdline"
command_bytes = command_file.read_bytes()
command = shlex.split(command_bytes.decode())
assert all(flag in command for flag in ["-Werror", "-Wall", "-Wextra", "-std=c++14", "-O3", "-fPIC"])
source_argument = str(target / relative)
assert command.count(source_argument) == 1 and command.count("-o") == 1
(out / "actual-build-command.txt").write_bytes(command_bytes)

# A single isolated source path keeps __FILE__ and compiler file symbols equal.
# The replacement has the same line count, preserving later __LINE__ expansions.
isolated = out / "owner/classLoader.cpp"
isolated.parent.mkdir(exist_ok=True)
observations = []
symbol_outputs = []
objects = []
for label, data, expected_count in [("before", before, 5), ("after", after, 6)]:
    isolated.write_bytes(data)
    cmd = command.copy()
    cmd[cmd.index(source_argument)] = str(isolated)
    object_path = out / (label + ".o")
    cmd[cmd.index("-o") + 1] = str(object_path)
    if "-MF" in cmd:
        cmd[cmd.index("-MF") + 1] = str(out / (label + ".d"))
    completed = subprocess.run(cmd, cwd=target, capture_output=True, text=True, timeout=180)
    (out / (label + "-compiler.log")).write_text(completed.stdout + completed.stderr)
    (out / (label + "-command.json")).write_text(json.dumps(cmd, indent=2) + "\n")
    assert completed.returncode == 0, label + "\n" + completed.stderr
    assert not completed.stderr, label + " emitted compiler diagnostics"
    object_bytes = object_path.read_bytes()
    objects.append(object_bytes)
    symbols = subprocess.run(["nm", "--defined-only", str(object_path)], capture_output=True,
                             check=True, text=True, timeout=60).stdout
    (out / (label + "-symbols.txt")).write_text(symbols)
    symbol_outputs.append(symbols)
    function = "_ZN11ClassLoader21compute_Object_vtableEv"
    disassembly = subprocess.run(["objdump", "-d", "-C", "--disassemble=" + function, str(object_path)],
                                capture_output=True, check=True, text=True, timeout=60).stdout
    # With demangling, objdump expects the demangled selector on this binutils.
    if "mov" not in disassembly:
        disassembly = subprocess.run(["objdump", "-d", "-C",
                                     "--disassemble=ClassLoader::compute_Object_vtable()", str(object_path)],
                                    capture_output=True, check=True, text=True, timeout=60).stdout
    (out / (label + "-base-vtable-disassembly.txt")).write_text(disassembly)
    assert re.search(r"mov\s+\$0x" + str(expected_count) + r",%eax", disassembly), disassembly
    observations.append({"case": label, "compiler_exit": completed.returncode,
                         "object_sha256": digest(object_bytes), "base_vtable_entries": expected_count})

assert symbol_outputs[0] == symbol_outputs[1], "defined symbols or addresses changed"
assert len(objects[0]) == len(objects[1]), "whole-object size changed"
differences = [index for index, (left, right) in enumerate(zip(*objects)) if left != right]
assert len(differences) == 1, "unexpected whole-object byte differences: " + str(len(differences))
offset = differences[0]
assert objects[0][offset] == 5 and objects[1][offset] == 6
fixture = crate / "contract/M3ObjectBaseVtableContract.java"
fixture_classes = crate / "target/runtime-classes"
fixture_classes.mkdir(parents=True, exist_ok=True)
fixture_command = [str(jdk / "bin/javac"), "--release", "21", "--patch-module",
                   "java.base=" + str(target / "build/m3-p0/jdk/modules/java.base"),
                   "-Xlint:all", "-Werror", "-d", str(fixture_classes), str(fixture)]
fixture_compile = subprocess.run(fixture_command, capture_output=True, text=True, timeout=60)
(out / "runtime-fixture-compiler.log").write_text(fixture_compile.stdout + fixture_compile.stderr)
(out / "runtime-fixture-compiler-command.json").write_text(json.dumps(fixture_command, indent=2) + "\n")
assert fixture_compile.returncode == 0 and not fixture_compile.stderr, fixture_compile.stderr
report = {
    "target": relative.as_posix(), "before_sha256": manifest[1], "after_sha256": manifest[2],
    "production_output_origin": "actual named OpenRewrite SDK Results",
    "context_sources_sealed_unchanged": len(context),
    "object_class_sha256": digest(object_class.read_bytes()),
    "object_virtual_methods": virtuals, "object_virtual_method_count": len(virtuals),
    "wait_native_binding": "existing wait_name literal wait0 matches private final native wait0(J)V; unchanged",
    "actual_build_command_sha256": digest(command_bytes),
    "compiler": subprocess.run([command[0], "--version"], capture_output=True, check=True, text=True).stdout.splitlines()[0],
    "strict_whole_owner_compiles": observations,
    "defined_symbols_and_addresses_equal": True,
    "whole_object_byte_differences": [{"offset": offset, "before": 5, "after": 6}],
    "public_java_api_and_bodies_changed": False, "vtable_guard_changed": False,
    "runtime_fixture_sha256": digest(fixture.read_bytes()),
    "runtime_fixture_strict_compile": "PASS; --release21 --patch-module java.base -Xlint:all -Werror",
    "runtime_fixture_classes": str(fixture_classes),
    "full_image_and_runtime_fixture": "PENDING parent rebuild after exact source admission",
}
(out / "RESULT.json").write_text(json.dumps(report, indent=2) + "\n")
print(json.dumps(report, indent=2))
