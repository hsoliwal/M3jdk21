#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: GPL-2.0-only
"""Compile bounded Android-header regressions without claiming an Android build.

Usage:
  python3 verify_android_headers.py --before BEFORE_TREE --after AFTER_TREE \
      --output results.json --java-home /absolute/path/to/jdk21 \
      [--bionic-basis /directory/containing/libc/include/...]

The Bionic cases are explicitly GCC x86 ILP32/LP64 *models*, not NDK builds.
The struct block is extracted unchanged from each actual product source. Its
original OpenJDK copyright/license header is retained in each temporary C file.
Temporary translation units are deleted after compilation. The JSON retains
source hashes, commands, diagnostics and the exact evidence boundary.

Model basis: aosp-mirror/platform_bionic at
731631f300090436d7f5df80d50b6275c8c60a93. Its stdint.h uses unsigned long long
for __uint64_t on ILP32 and unsigned long on LP64. Its netdb.h imports generic
sys/socket.h, whereas netinet/in.h imports the IPv4/IPv6 sockaddr definitions.
The models below reproduce these two declaration boundaries; they are not copied
replacement Bionic headers, a complete include closure, or a target sysroot.

The inspected donor fixes are Termux patches 0001 and 0036 at
termux/termux-packages@8eb3169dbd10d71ef26e2847191f9ae2a452ab2e:
https://github.com/termux/termux-packages/blob/8eb3169dbd10d71ef26e2847191f9ae2a452ab2e/packages/openjdk-21/0001-libinet-include-netinet-in.h-for-android-builds.patch
https://github.com/termux/termux-packages/blob/8eb3169dbd10d71ef26e2847191f9ae2a452ab2e/packages/openjdk-21/0036-OUTTREE-bionic-type-redef-fix.patch
LICENSE.md at that pin assigns package patches their underlying package license;
this original product verifier's GPL-2.0-only header does not relicense extracted JDK
source or donor patches. Product files retain their own original notices.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import platform
import re
import shutil
import subprocess
import sys
import tempfile


DISPATCHER = Path("src/java.base/unix/native/libnio/fs/UnixNativeDispatcher.c")
NET_HEADER = Path("src/java.base/unix/native/libnet/net_util_md.h")
BIONIC_REVISION = "731631f300090436d7f5df80d50b6275c8c60a93"
BIONIC_BASIS = {
    "libc/include/stdint.h": {
        "git_blob": "772fe8b6033767cd03bdc96e8682f9a85e4f2bcd",
        "sha256": "2368fab63d602a8d2f9d1baabacd82f063eccdde95201aa2320fc3c70f06d0fa",
    },
    "libc/include/netdb.h": {
        "git_blob": "04aaf5cfd3b0e0561d42960856b9b7b92991817d",
        "sha256": "d0ef196b65897ee0970a419a09cd7f7ac9632afe66775dc23babf51f08a4dfde",
    },
    "libc/include/sys/socket.h": {
        "git_blob": "47ddce088ab3ffcfec3a31f5b40f7b95640a7f4e",
        "sha256": "30eb5a88eec2c22cf0b1d47a77124acbc5b0e69b6f0f714953deb5b35018e1fe",
    },
    "libc/include/netinet/in.h": {
        "git_blob": "d4ce30271734cc600e29e6a85332e0092829350a",
        "sha256": "79c2542f82cd962e4179417e4a8648ad97bb36c0430338bb70145881baf8cc03",
    },
}


def identity(path: Path) -> dict:
    raw = path.read_bytes()
    return {
        "path": str(path.resolve()),
        "bytes": len(raw),
        "sha256": hashlib.sha256(raw).hexdigest(),
        "git_blob": hashlib.sha1(b"blob " + str(len(raw)).encode() + b"\0" + raw).hexdigest(),
    }


def only_index(text: str, marker: str) -> int:
    if text.count(marker) != 1:
        raise ValueError(f"Expected exactly one extraction marker: {marker!r}")
    return text.index(marker)


def statx_block(path: Path) -> str:
    """Extract the actual guarded declarations, with original file rights."""
    text = path.read_text(encoding="utf-8")
    if not text.startswith("/*"):
        raise ValueError(f"Missing original license header: {path}")
    header_end = text.index("*/") + 2
    original_header = text[:header_end]
    if "GNU General Public License" not in original_header:
        raise ValueError(f"Unexpected original license header: {path}")
    start = only_index(text, "#if defined(__linux__)\n// Account for the case where we compile on a system without statx")
    end = only_index(text, "// statx masks, flags, constants")
    if not start < end or end - start > 8000:
        raise ValueError("Unbounded or reordered statx extraction")
    block = text[start:end]
    # Close only the outer Linux conditional. The original inner guard remains.
    return original_header + "\n\n" + block + "\n#endif\n"


STATX_OFFSETS = {
    "stx_mask": 0, "stx_blksize": 4, "stx_attributes": 8,
    "stx_nlink": 16, "stx_uid": 20, "stx_gid": 24, "stx_mode": 28,
    "stx_ino": 32, "stx_size": 40, "stx_blocks": 48,
    "stx_attributes_mask": 56, "stx_atime": 64, "stx_btime": 80,
    "stx_ctime": 96, "stx_mtime": 112, "stx_rdev_major": 128,
    "stx_rdev_minor": 132, "stx_dev_major": 136, "stx_dev_minor": 140,
}


def statx_assertions(host: bool) -> str:
    lines = [
        '_Static_assert(sizeof(struct my_statx) == 256, "statx ABI size");',
        '_Static_assert(sizeof(struct my_statx_timestamp) == 16, "statx timestamp ABI size");',
        '_Static_assert(sizeof(((struct my_statx *)0)->stx_attributes) == 8, "statx attributes width");',
        '_Static_assert(sizeof(((struct my_statx *)0)->stx_ino) == 8, "statx inode width");',
        '_Static_assert(sizeof(((struct my_statx *)0)->stx_mode) == 2, "statx mode width");',
        '_Static_assert(__builtin_offsetof(struct my_statx_timestamp, tv_nsec) == 8, "timestamp nanoseconds offset");',
        '_Static_assert(__builtin_offsetof(struct my_statx_timestamp, __reserved) == 12, "timestamp reserved offset");',
    ]
    for field, offset in STATX_OFFSETS.items():
        lines.append(f'_Static_assert(__builtin_offsetof(struct my_statx, {field}) == {offset}, "{field} ABI offset");')
    if host:
        lines += [
            '_Static_assert(sizeof(struct my_statx) == sizeof(struct statx), "host kernel statx size");',
            '_Static_assert(_Alignof(struct my_statx) == _Alignof(struct statx), "host kernel statx alignment");',
        ]
        for field in STATX_OFFSETS:
            lines.append(f'_Static_assert(__builtin_offsetof(struct my_statx, {field}) == __builtin_offsetof(struct statx, {field}), "host kernel {field} offset");')
    return "\n".join(lines) + "\n"


def statx_translation(path: Path, bits: int, host: bool) -> str:
    if host:
        prefix = "#include <stdint.h>\n#include <sys/types.h>\n#include <linux/stat.h>\n"
    else:
        # Independently authored, deliberately minimal declaration model. GCC's
        # actual -m32/-m64 data layout drives the 32/64-bit proof, not a mock size.
        prefix = f"""/* BIONIC_DECLARATION_MODEL: not Android SDK headers. */
#define __ANDROID__ 1
#define __BIONIC__ 1
typedef __INT64_TYPE__ int64_t;
typedef __INT32_TYPE__ int32_t;
typedef unsigned short __uint16_t;
typedef unsigned int __uint32_t;
#ifdef __LP64__
typedef unsigned long __uint64_t;
#else
typedef unsigned long long __uint64_t;
#endif
_Static_assert(sizeof(void *) * 8 == {bits}, "actual compiler pointer model");
_Static_assert(sizeof(__uint64_t) == 8, "modeled Bionic uint64 width");
"""
    # Preserve original rights at the beginning of the extracted translation unit.
    block = statx_block(path)
    header_end = block.index("*/") + 2
    return block[:header_end] + "\n" + prefix + block[header_end:] + statx_assertions(host)


def modeled_network_headers(directory: Path) -> list[dict]:
    """Make independent minimal headers matching the observed include boundary."""
    license_line = "/* SPDX-License-Identifier: GPL-2.0-only; first-party declaration model, not an Android header. */\n"
    contents = {
        "sys/socket.h": """#ifndef MODEL_SYS_SOCKET_H
#define MODEL_SYS_SOCKET_H
struct sockaddr { unsigned short family; char address[14]; };
#endif
""",
        "netdb.h": "#include <sys/socket.h>\n",
        "poll.h": "/* No declarations are needed by this product header. */\n",
        "netinet/in.h": """#ifndef MODEL_NETINET_IN_H
#define MODEL_NETINET_IN_H
#include <sys/socket.h>
struct sockaddr_in {
    unsigned short family;
    unsigned short port;
    unsigned int address;
    unsigned char padding[8];
};
struct sockaddr_in6 {
    unsigned short family;
    unsigned short port;
    unsigned int flow_info;
    unsigned char address[16];
    unsigned int scope_id;
};
#endif
""",
    }
    rows = []
    for name, content in contents.items():
        path = directory / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(license_line + content, encoding="utf-8")
        rows.append({"model_path": name, "sha256": identity(path)["sha256"]})
    return rows


def c_path(path: Path) -> str:
    value = str(path.resolve())
    if any(c in value for c in ['"', "\\", "\n", "\r"]):
        raise ValueError(f"Unsupported C include path: {value!r}")
    return '"' + value + '"'


def net_translation(header: Path, bits: int, host: bool) -> str:
    prefix = "#include <jni.h>\n" if host else """/* BIONIC_INCLUDE_MODEL: opaque JNI declaration; not VM execution. */
#define __ANDROID__ 1
#define __BIONIC__ 1
typedef struct ModelJNIEnv JNIEnv;
"""
    return prefix + f"#include {c_path(header)}\n" + f"""
_Static_assert(sizeof(void *) * 8 == {bits}, "actual compiler pointer model");
_Static_assert(sizeof(SOCKETADDRESS) == 28, "socket address union layout");
_Static_assert(sizeof(((SOCKETADDRESS *)0)->sa) == 16, "generic socket address size");
_Static_assert(sizeof(((SOCKETADDRESS *)0)->sa4) == 16, "IPv4 socket address size");
_Static_assert(sizeof(((SOCKETADDRESS *)0)->sa6) == 28, "IPv6 socket address size");
_Static_assert(__builtin_offsetof(SOCKETADDRESS, sa4) == 0, "IPv4 union offset");
_Static_assert(__builtin_offsetof(SOCKETADDRESS, sa6) == 0, "IPv6 union offset");
typedef void (*UnknownHostSignature)(JNIEnv *, const char *, int);
typedef void (*LastErrorSignature)(JNIEnv *, const char *, const char *);
_Static_assert(__builtin_types_compatible_p(__typeof__(&NET_ThrowUnknownHostExceptionWithGaiError), UnknownHostSignature), "unknown host declaration contract");
_Static_assert(__builtin_types_compatible_p(__typeof__(&NET_ThrowByNameWithLastError), LastErrorSignature), "last error declaration contract");
"""


def run_command(command: list[str]) -> dict:
    env = os.environ.copy()
    env["LC_ALL"] = "C"
    result = subprocess.run(command, capture_output=True, text=True, timeout=45, env=env, check=False)
    return {"command": command, "exit_code": result.returncode,
            "stdout": result.stdout, "stderr": result.stderr}


def compile_case(cc: str, work: Path, name: str, source: str,
                 flags: list[str], expected_success: bool,
                 failure_terms: tuple[str, ...] = ()) -> dict:
    path = work / (name + ".c")
    path.write_text(source, encoding="utf-8")
    result = run_command([cc, "-std=c11", "-Wall", "-Wextra", "-Werror", "-fsyntax-only", *flags, str(path)])
    diagnostics = result["stdout"] + result["stderr"]
    success = result["exit_code"] == 0
    expected_failure_observed = not success and all(term in diagnostics for term in failure_terms)
    result.update({
        "name": name,
        "translation_unit_sha256": identity(path)["sha256"],
        "expected": "COMPILE_SUCCESS" if expected_success else "SPECIFIED_BASELINE_COMPILE_FAILURE",
        "required_failure_diagnostic_terms": list(failure_terms),
        "expectation_met": success if expected_success else expected_failure_observed,
        "evidence_type": "HOST_LINUX_HEADERS_SYNTAX" if "host_linux" in name else "GCC_X86_BIONIC_DECLARATION_MODEL_SYNTAX",
    })
    return result


def validate_basis(directory: Path | None) -> dict:
    result = {"repository": "aosp-mirror/platform_bionic", "revision": BIONIC_REVISION,
              "status": "PINNED_REFERENCE_NOT_READ_IN_THIS_RUN", "files": []}
    for relative, expected in BIONIC_BASIS.items():
        row = {"path": relative, **expected,
               "url": f"https://github.com/aosp-mirror/platform_bionic/blob/{BIONIC_REVISION}/{relative}"}
        if directory is not None:
            path = directory / relative
            observed = identity(path)
            if any(observed[k] != expected[k] for k in ("sha256", "git_blob")):
                raise ValueError(f"Bionic basis identity mismatch: {relative}")
            row["observed"] = observed
        result["files"].append(row)
    if directory is not None:
        stdint = (directory / "libc/include/stdint.h").read_text(encoding="utf-8")
        conditional = re.compile(r"#if defined\(__LP64__\)\s+typedef long __int64_t;\s+typedef unsigned long __uint64_t;\s+#else\s+typedef long long __int64_t;\s+typedef unsigned long long __uint64_t;")
        if not conditional.search(stdint):
            raise ValueError("Pinned Bionic stdint.h does not substantiate the model")
        netdb = (directory / "libc/include/netdb.h").read_text(encoding="utf-8")
        socket = (directory / "libc/include/sys/socket.h").read_text(encoding="utf-8")
        inet = (directory / "libc/include/netinet/in.h").read_text(encoding="utf-8")
        if "#include <sys/socket.h>" not in netdb or "#include <netinet/in.h>" in netdb:
            raise ValueError("Unexpected pinned Bionic netdb header boundary")
        if "#include <netinet/in.h>" in socket or re.search(r"struct\s+sockaddr_in6?\s*\{", socket):
            raise ValueError("Unexpected complete IP sockaddr definition in sys/socket.h")
        if not all(item in inet for item in ("#include <linux/in.h>", "#include <linux/in6.h>")):
            raise ValueError("Unexpected pinned Bionic netinet/in.h boundary")
        result["status"] = "EXACT_SOURCE_BYTES_AND_MODEL_BASIS_CHECKED"
    return result


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--before", required=True, type=Path)
    parser.add_argument("--after", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--cc", default="gcc")
    parser.add_argument("--java-home", type=Path, default=os.environ.get("JAVA_HOME"))
    parser.add_argument("--bionic-basis", type=Path)
    args = parser.parse_args()
    output = args.output.resolve()
    if output.exists():
        parser.error("--output must be a new file; preserve previous evidence")
    cc = shutil.which(args.cc)
    if cc is None:
        parser.error(f"C compiler not found: {args.cc}")
    if sys.platform != "linux" or platform.machine() not in ("x86_64", "amd64"):
        parser.error("This bounded harness requires Linux x86_64 GCC -m32/-m64 syntax support")
    if args.java_home is None:
        parser.error("Provide --java-home (or JAVA_HOME) for the real Linux JNI-header control")
    java_home = Path(args.java_home).resolve()
    jni = java_home / "include/jni.h"
    jni_md = java_home / "include/linux/jni_md.h"
    if not jni.is_file() or not jni_md.is_file():
        parser.error("--java-home must contain include/jni.h and include/linux/jni_md.h")
    before, after = args.before.resolve(), args.after.resolve()
    for root in (before, after):
        for relative in (DISPATCHER, NET_HEADER):
            if not (root / relative).is_file():
                parser.error(f"Missing product source: {root / relative}")
        if output == root or root in output.parents:
            parser.error("Write evidence outside both product source trees")

    receipt = {
        "schema": "m3.android-header-regression.v1",
        "purpose": "Verify two bounded native-header portability fixes using compile models and actual Linux headers",
        "status": "FAILED",
        "host": {"system": platform.system(), "machine": platform.machine(), "python": platform.python_version()},
        "compiler": run_command([cc, "--version"]),
        "verifier": identity(Path(__file__)),
        "inputs": {name: [identity(root / p) for p in (DISPATCHER, NET_HEADER)]
                   for name, root in (("before", before), ("after", after))},
        "jni_headers": [identity(jni), identity(jni_md)],
        "cases": [],
        "limitations": [
            "No Android NDK, Android sysroot, ARM compiler, Android device, emulator or VM was used.",
            "The Android cases model only Bionic integer typedefs and IPv4/IPv6 header declaration boundaries on GCC x86 ILP32/LP64.",
            "The Linux statx test compiles the exact extracted private layout and compares it with host Linux kernel headers, not the complete libnio translation unit.",
            "The network-header tests compile the complete actual product header; modeled cases use an opaque JNI declaration, Linux controls use the identified host JNI headers.",
            "All checks are syntax/type/layout compilation; they do not qualify link-time symbols, runtime behavior, full JDK images, packaging, iOS or mobile release readiness.",
        ],
    }
    try:
        receipt["bionic_basis"] = validate_basis(args.bionic_basis)
        with tempfile.TemporaryDirectory(prefix="m3-android-header-regression-") as temp:
            work = Path(temp)
            model_headers = work / "modeled-headers"
            receipt["modeled_network_header_hashes"] = modeled_network_headers(model_headers)
            for name, root in (("before", before), ("after", after)):
                for bits in (32, 64):
                    expected = name == "after" or bits == 64
                    receipt["cases"].append(compile_case(
                        cc, work, f"statx_{name}_modeled_bionic_ilp32" if bits == 32 else f"statx_{name}_modeled_bionic_lp64",
                        statx_translation(root / DISPATCHER, bits, False),
                        [f"-m{bits}", "-nostdinc"], expected,
                        ("conflicting types", "__uint64_t") if not expected else (),
                    ))
                receipt["cases"].append(compile_case(
                    cc, work, f"statx_{name}_host_linux_lp64",
                    statx_translation(root / DISPATCHER, 64, True), ["-m64"], True,
                ))
                for bits in (32, 64):
                    receipt["cases"].append(compile_case(
                        cc, work, f"network_{name}_modeled_bionic_{bits}",
                        net_translation(root / NET_HEADER, bits, False),
                        [f"-m{bits}", "-nostdinc", "-I", str(model_headers)], name == "after",
                        ("sa4", "sa6", "incomplete type") if name == "before" else (),
                    ))
                receipt["cases"].append(compile_case(
                    cc, work, f"network_{name}_host_linux_lp64",
                    net_translation(root / NET_HEADER, 64, True),
                    ["-m64", "-I", str(jni.parent), "-I", str(jni_md.parent)], True,
                ))
        receipt["input_bytes_unchanged"] = all(
            identity(Path(row["path"])) == row
            for rows in receipt["inputs"].values() for row in rows
        )
        receipt["summary"] = {
            "compiler_invocations": len(receipt["cases"]),
            "expectations_met": sum(row["expectation_met"] for row in receipt["cases"]),
            "expected_baseline_failures_reproduced": sum(
                row["expected"] == "SPECIFIED_BASELINE_COMPILE_FAILURE" and row["expectation_met"]
                for row in receipt["cases"]),
            "unexpected_results": [row["name"] for row in receipt["cases"] if not row["expectation_met"]],
        }
        if (len(receipt["cases"]) == 12 and all(row["expectation_met"] for row in receipt["cases"])
                and receipt["input_bytes_unchanged"]):
            receipt["status"] = "PASS_BOUNDED_COMPILER_REGRESSIONS_ONLY"
    except (ValueError, OSError, subprocess.SubprocessError) as error:
        receipt["error"] = f"{type(error).__name__}: {error}"
    output.parent.mkdir(parents=True, exist_ok=True)
    with output.open("x", encoding="utf-8") as stream:
        json.dump(receipt, stream, indent=2, sort_keys=True)
        stream.write("\n")
    print(json.dumps({"status": receipt["status"], "summary": receipt.get("summary"), "output": str(output)}))
    return 0 if receipt["status"] == "PASS_BOUNDED_COMPILER_REGRESSIONS_ONLY" else 1


if __name__ == "__main__":
    raise SystemExit(main())
