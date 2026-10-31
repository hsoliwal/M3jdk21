#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors.
# SPDX-License-Identifier: Apache-2.0
"""Executable proof: two JVMs, one read-only image inode, exactly the same virtual address."""
import hashlib
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parent
JDK = Path(os.environ.get("M3_JDK", "/usr/lib/jvm/default-java"))
if not (JDK / "bin/javac").exists():
    JDK = Path(shutil.which("javac")).resolve().parent.parent
CC = os.environ.get("CC", "cc")
BASE = 0x600000000000


def command(*args):
    subprocess.run(args, check=True)


def assert_mapping(process, image):
    entries = []
    for line in Path(f"/proc/{process.pid}/maps").read_text().splitlines():
        if str(image) in line:
            parts = line.split()
            first = int(parts[0].split("-")[0], 16)
            entries.append((first, parts[1], int(parts[2], 16), int(parts[4]), line))
    st = image.stat()
    matching = [item for item in entries if item[0] == BASE and item[1] == "r--s"
                and item[2] == 0 and item[3] == st.st_ino]
    assert len(matching) == 1, (process.pid, entries)
    return matching[0][4]


def main():
    if os.name != "posix" or not Path("/proc/self/maps").exists():
        raise RuntimeError("Linux /proc/maps required for executable exact-address proof")
    with tempfile.TemporaryDirectory(prefix="m3-gvas-") as directory:
        work = Path(directory)
        image = work / "canonical-image.bin"
        payload = bytearray(4096)
        payload[128:130] = b"Z6"
        image.write_bytes(payload)
        image.chmod(0o444)
        digest = hashlib.sha256(payload).hexdigest()
        javac = str(JDK / "bin/javac")
        java = str(JDK / "bin/java")
        command(javac, "--release", "21", "-Xlint:all", "-Werror", "-h", str(work),
                "-d", str(work),
                str(ROOT / "core/src/com/m3/text/M3FixedAddressMapping.java"),
                str(ROOT / "core/test/com/m3/text/M3FixedAddressMappingContractTest.java"))
        library = work / "libm3gvas.so"
        command(CC, "-std=c11", "-Wall", "-Wextra", "-Werror", "-fPIC", "-shared",
                "-I" + str(JDK / "include"), "-I" + str(JDK / "include/linux"),
                str(ROOT / "lexicon/native/m3_fixed_address_mapping_jni.c"),
                "-o", str(library))
        base_cmd = [java, "-Xcheck:jni", "-ea", "-cp", str(work),
                    "com.m3.text.M3FixedAddressMappingContractTest"]
        for mode in ("collision", "digest"):
            output = subprocess.run([*base_cmd, mode, str(library), str(image), digest],
                                    text=True, capture_output=True, check=True)
            assert f"M3_GVAS_{mode.upper()}_REFUSED" in output.stdout, output
        children = []
        maps = []
        try:
            for vm_flag in ("-Xint", "-Xbatch"):
                argv = [java, "-Xcheck:jni", vm_flag, "-ea", "-cp", str(work),
                        "com.m3.text.M3FixedAddressMappingContractTest",
                        "child", str(library), str(image), digest]
                child = subprocess.Popen(argv, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                                         stderr=subprocess.PIPE, text=True, bufsize=1)
                children.append(child)
                ready = child.stdout.readline().strip()
                assert "M3_GVAS_READY" in ready, (ready, child.stderr.read())
                got = int(re.search(r"base=0x([0-9a-f]+)", ready).group(1), 16)
                assert got == BASE, (vm_flag, ready)
                maps.append(assert_mapping(child, image))
            for child in children:
                stdout, stderr = child.communicate("\n", timeout=30)
                assert child.returncode == 0, (stdout, stderr)
                assert "M3_GVAS_JAVA_PASS" in stdout, (stdout, stderr)
        finally:
            for child in children:
                if child.poll() is None:
                    child.kill()
                    child.wait(timeout=10)
        assert len(maps) == 2 and image.stat().st_ino > 0
        print(f"M3_GVAS_STRICT_PASS address=0x{BASE:x} processes=2 "
              f"inode={image.stat().st_ino} sha256={digest} "
              "collision=refused digest=refused read_only=true")
        for line in maps:
            print("  " + line)


if __name__ == "__main__":
    main()
