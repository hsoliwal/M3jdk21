#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors

"""Build the owned Skia sources for libm3graphics; never download runtime inputs."""

import argparse
import hashlib
import json
from pathlib import Path
import re
import shlex
import subprocess


TARGETS = ["skia", "skottie", "sksg", "svg", "skparagraph", "skshaper",
           "skunicode_core", "skunicode_icu", "skresources", "harfbuzz"]
TARGET_LABELS = ["//:skia", "//modules/skottie:skottie", "//modules/sksg:sksg",
                 "//modules/svg:svg", "//modules/skparagraph:skparagraph",
                 "//modules/skshaper:skshaper", "//modules/skunicode:skunicode_core",
                 "//modules/skunicode:skunicode_icu", "//modules/skresources:skresources",
                 "//third_party/harfbuzz:harfbuzz"]


def run(command, cwd, capture=False):
    prefix = shlex.split(command[0])
    if not prefix:
        raise ValueError("empty configured tool command")
    return subprocess.run(prefix + command[1:], cwd=cwd, check=True, text=True,
                          stdout=subprocess.PIPE if capture else None).stdout


def write_changed(path, content):
    if not path.exists() or path.read_text() != content:
        temporary = path.with_suffix(path.suffix + ".tmp")
        temporary.write_text(content)
        temporary.replace(path)


def selected_msvc(args, parser):
    # GN otherwise searches independently for a Visual Studio installation and
    # selects its newest toolset. Use the compiler and SDK already chosen by JDK.
    compiler = re.compile(r"^(.*)/Tools/MSVC/([^/]+)/bin/(Host[^/]+)/([^/]+)/cl(?:\.exe)?$", re.I)
    selections = []
    for command in (args.cc, args.cxx):
        tokens = [token.strip('"').replace("\\", "/") for token in shlex.split(command)]
        matches = [compiler.fullmatch(token) for token in tokens]
        matches = [match for match in matches if match]
        if len(matches) != 1:
            parser.error("Skia must identify the configured cl.exe toolset; no independent MSVC discovery is allowed")
        selections.append(matches[0].groups())
    if selections[0] != selections[1]:
        parser.error("Skia C and C++ compilers must select the same MSVC toolset")
    root, version, host, cpu = selections[0]
    if host.lower() != "hostx64" or cpu.lower() != args.cpu:
        parser.error("the owned Skia MSVC graph requires the configured HostX64 toolset for " + args.cpu)
    sdk_pattern = re.compile(r"^(.*)/(?:Include|Lib)/(\d+\.\d+\.\d+\.\d+)/(?:ucrt|shared|um|winrt)(?:/.*)?$", re.I)
    sdks = set()
    for token in shlex.split(args.sysroot_cflags + " " + args.ldflags):
        path = re.sub(r"^(?:-I|/I|-libpath:|/LIBPATH:)", "", token, flags=re.I).replace("\\", "/")
        match = sdk_pattern.fullmatch(path)
        if match:
            sdks.add(match.groups())
    if len(sdks) != 1:
        parser.error("Skia requires one unambiguous Windows SDK in the configured JDK sysroot flags")
    sdk, sdk_version = sdks.pop()
    return {"win_vc": root, "win_toolchain_version": version,
            "win_sdk": sdk, "win_sdk_version": sdk_version}


def native_options(options, args, source):
    if not args.fixpath_base:
        return options

    def convert(value):
        # The original JDK path owner converts one native argument at a time.
        # Never send serialized GN options through FIXPATH as one compound arg.
        # Its short-option recognition does not include MSVC's /LIBPATH prefix.
        # Convert that option's path operand through the same owner separately.
        library_path = re.match(r"^(/LIBPATH:|-libpath:)(.*)$", value, re.I)
        if library_path:
            return library_path.group(1) + convert(library_path.group(2))
        return run([args.fixpath_base, "-m", "print", value], source, capture=True).strip()

    def command(value):
        tokens = shlex.split(value)
        # GN's own command lines contain native paths after conversion, so its
        # compiler subprocess does not need another path-conversion shell. The
        # configured compiler and any actual compiler-cache commands remain.
        cleaned = []
        index = 0
        wrapper = shlex.split(args.fixpath_base)
        while index < len(tokens):
            if tokens[index:index + len(wrapper) + 1] == wrapper + ["exec"]:
                index += len(wrapper) + 1
            else:
                cleaned.append(convert(tokens[index]))
                index += 1
        return subprocess.list2cmdline(cleaned)

    for key, value in list(options.items()):
        if key in ("cc", "cxx", "ar", "target_cc", "target_cxx", "target_ar"):
            options[key] = command(value)
        elif key in ("ndk", "win_vc", "win_sdk", "xcode_sysroot"):
            options[key] = convert(value)
        elif key.startswith("extra_") and isinstance(value, list):
            options[key] = [convert(item) for item in value]
    return options


def write_link_args(args, source, output):
    # The original GN graph owns both transitive static archives and inherited
    # system libraries. Release packages may merge archives; source builds keep
    # dependencies such as the JSON reader separate, so use the actual graph.
    graph = json.loads(run([args.gn, "desc", str(output), "*", "--format=json"], source, capture=True))
    values = {key: [] for key in ("lib_dirs", "framework_dirs", "libs", "frameworks", "weak_frameworks")}

    def link_dependencies(label):
        target = graph[label]
        if target.get("type") not in ("static_library", "source_set", "group"):
            return []
        # Build actions, host executables and data_deps do not contribute
        # archives to a native consumer's link, especially when cross compiling.
        return [dependency for key in ("deps", "public_deps")
                for dependency in target.get(key, [])
                if graph[dependency].get("type") in ("static_library", "source_set", "group")]

    pending = list(TARGET_LABELS)
    visited = set()
    while pending:
        label = pending.pop(0)
        if label in visited:
            continue
        visited.add(label)
        if label not in graph:
            raise RuntimeError("Skia build omitted required native target: " + label)
        for key, items in values.items():
            for item in graph[label].get(key, []):
                if item not in items:
                    items.append(item)
        pending.extend(link_dependencies(label))

    def native_path(value):
        return str(source / value[2:]) if value.startswith("//") else value

    archive_targets = []
    ordered = set()

    def visit(label):
        if label in ordered:
            return
        ordered.add(label)
        target = graph[label]
        for dependency in link_dependencies(label):
            visit(dependency)
        if target.get("type") == "static_library":
            archive_targets.append(label)

    for label in TARGET_LABELS:
        visit(label)
    archives = []
    for label in reversed(archive_targets):
        for value in graph[label].get("outputs", []):
            archive = Path(native_path(value)).resolve()
            archive.relative_to(output)  # Only archives built in this owned graph.
            if archive.suffix not in (".a", ".lib"):
                raise RuntimeError("Unexpected Skia static-library output: " + str(archive))
            if archive not in archives:
                archives.append(archive)
    if not archives:
        raise RuntimeError("Skia graph has no native archives")

    def response(flags):
        # One quoted argument per line also preserves the original JDK FIXPATH
        # response-file conversion for native Windows compiler commands.
        return "".join('"' + flag.replace("\\", "\\\\").replace('"', '\\"') + '"\n' for flag in flags)

    archive_flags = [str(archive) for archive in archives]
    if args.os in ("linux", "android"):
        archive_flags = ["-Wl,--start-group"] + archive_flags + ["-Wl,--end-group"]
    write_changed(output / "skia-native-libs.rsp", response(archive_flags))

    flags = []
    for directory in values["lib_dirs"]:
        flags.append(("/LIBPATH:" if args.os == "win" else "-L") + native_path(directory))
    for directory in values["framework_dirs"]:
        flags.append("-F" + native_path(directory))
    for library in values["libs"]:
        if args.os == "win" or "/" in library or "\\" in library:
            flags.append(native_path(library))
        else:
            flags.append("-l" + library)
    for framework in values["frameworks"]:
        flags.extend(["-framework", framework[:-10] if framework.endswith(".framework") else framework])
    for framework in values["weak_frameworks"]:
        flags.extend(["-weak_framework", framework[:-10] if framework.endswith(".framework") else framework])
    write_changed(output / "skia-system-libs.rsp", response(flags))
    return archives


def write_archive_state(output, archives):
    # Transitive archive changes must relink the JNI library even when the
    # top-level archives and the link argument list retain their timestamps.
    state = [{"path": str(path.relative_to(output)),
              "sha256": hashlib.sha256(path.read_bytes()).hexdigest()}
             for path in archives]
    write_changed(output / "skia-archives.stamp", json.dumps(state, indent=2) + "\n")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--gn", required=True)
    parser.add_argument("--ninja", required=True)
    parser.add_argument("--fixpath-base", default="")
    parser.add_argument("--cc", required=True)
    parser.add_argument("--cxx", required=True)
    parser.add_argument("--ar")
    parser.add_argument("--os", required=True, choices=("linux", "mac", "win", "android", "ios"))
    parser.add_argument("--cpu", required=True, choices=("x64", "arm64", "x86", "arm"))
    parser.add_argument("--toolchain", required=True, choices=("gcc", "clang", "microsoft"))
    parser.add_argument("--jobs", type=int, default=2)
    parser.add_argument("--ndk")
    parser.add_argument("--ndk-api", type=int)
    parser.add_argument("--sysroot", default="")
    parser.add_argument("--sysroot-cflags", default="")
    parser.add_argument("--cflags", default="")
    parser.add_argument("--cxxflags", default="")
    parser.add_argument("--ldflags", default="")
    parser.add_argument("--ios-simulator", choices=("0", "1"))
    parser.add_argument("--ios-min-target", type=int)
    parser.add_argument("--fontconfig-cflags", default="")
    parser.add_argument("--prepare-only", action="store_true")
    args = parser.parse_args()
    source = args.source.resolve()
    output = args.output.resolve()
    if args.jobs < 1:
        parser.error("--jobs must be positive")
    for required in (".gn", "BUILD.gn", "DEPS", "include/core/SkCanvas.h",
                     "third_party/externals/icu/source/common/unicode/utypes.h",
                     "third_party/externals/brotli/c/include/brotli/decode.h"):
        if not (source / required).is_file():
            parser.error("missing owned Skia build input: " + str(source / required))
    if args.os in ("linux", "android"):
        for required in ("third_party/externals/egl-registry/api/EGL/egl.h",
                         "third_party/externals/egl-registry/api/KHR/khrplatform.h",
                         "third_party/externals/opengl-registry/api/GLES/gl.h",
                         "third_party/externals/opengl-registry/api/GLES/glext.h",
                         "third_party/externals/opengl-registry/api/GLES2/gl2.h"):
            if not (source / required).is_file():
                parser.error("missing owned EGL/GLES build input: " + str(source / required))
    if args.os == "android" and (not args.ndk or not args.ndk_api or args.ndk_api < 1):
        parser.error("Android requires the configured --ndk and compiler-verified --ndk-api")
    if args.os == "ios" and (not args.sysroot or args.ios_simulator is None
                              or not args.ios_min_target or args.ios_min_target < 10000):
        parser.error("iOS requires the configured SDK, compiler-verified simulator selection and deployment minimum")
    version = run([args.gn, "--version"], source, capture=True).strip()
    if not version.startswith("2175 ") or "b2afae" not in version:
        parser.error("Skia requires GN 2175 from b2afae122eeb6ce09c52d63f67dc53fc517dbdc8; found " + version)

    options = {
        "is_official_build": True,
        "target_cpu": args.cpu,
        "cc": args.cc,
        "cxx": args.cxx,
        "skia_use_system_expat": False,
        "skia_use_system_libjpeg_turbo": False,
        "skia_use_system_libpng": False,
        "skia_use_system_libwebp": False,
        "skia_use_system_zlib": False,
        "skia_use_freetype": True,
        "skia_use_system_freetype2": False,
        # The original desktop runtime can load WOFF2 system and application
        # fonts. Keep that capability with the pinned, owned Brotli decoder;
        # GN otherwise enables it only for WebAssembly in bundled FreeType.
        "skia_use_freetype_woff2": True,
        "skia_use_system_harfbuzz": False,
        "skia_pdf_subset_harfbuzz": True,
        "skia_use_system_icu": False,
        "skia_enable_skottie": True,
        "skia_use_egl": args.os in ("linux", "android"),
        "skia_use_vulkan": args.os in ("linux", "android", "win", "mac"),
        "extra_cflags_cc": ["/GR"] if args.os == "win" else ["-frtti"],
    }
    if args.ar:
        options["ar"] = args.ar
        options["target_ar"] = args.ar
    options["extra_cflags_c"] = shlex.split(args.cflags)
    options["extra_cflags_cc"] += shlex.split(args.cxxflags)
    options["extra_ldflags"] = shlex.split(args.ldflags)
    if args.os != "linux":
        options["target_os"] = args.os
    if args.os != "win":
        options["extra_cflags"] = ["-fPIC"] + shlex.split(args.sysroot_cflags)
        if args.os == "linux":
            options["extra_cflags"] += shlex.split(args.fontconfig_cflags)
        if options["skia_use_egl"]:
            # The original EGL interface also includes GLES headers. GN leaves
            # these platform includes to its caller; use the pinned owned API
            # trees without exposing unrelated system ICU or FreeType headers.
            options["extra_cflags"] += [
                "-I" + str(source / "third_party/externals/egl-registry/api"),
                "-I" + str(source / "third_party/externals/opengl-registry/api"),
            ]
    if args.os == "android":
        options["ndk"] = str(Path(args.ndk).resolve())
        options["ndk_api"] = args.ndk_api
        options["target_cc"] = args.cc
        options["target_cxx"] = args.cxx
        options["skia_use_android_framework"] = False
        options["skia_enable_android_utils"] = True
        options["skia_enable_fontmgr_android"] = True
        options["skia_enable_pdf"] = True
    if args.os == "win":
        if args.toolchain != "microsoft":
            parser.error("the owned Windows Skia graph requires the configured Microsoft toolchain")
        options.update(selected_msvc(args, parser))
        options["skia_use_direct3d"] = True
        options["extra_cflags"] = ["-DSK_FONT_HOST_USE_SYSTEM_SETTINGS"] + shlex.split(args.sysroot_cflags)
    if args.os in ("mac", "ios"):
        options["skia_use_metal"] = True
        if args.sysroot:
            options["xcode_sysroot"] = str(Path(args.sysroot).resolve())
    if args.os == "ios":
        options["ios_use_simulator"] = args.ios_simulator == "1"
        version = args.ios_min_target
        options["ios_min_target"] = f"{version // 10000}.{version // 100 % 100}.{version % 100}"

    output.mkdir(parents=True, exist_ok=True)
    options = native_options(options, args, source)
    gn_args = "".join(key + " = " + json.dumps(value) + "\n" for key, value in options.items())
    write_changed(output / "args.gn", gn_args)
    run([args.gn, "gen", str(output)], source)
    # The same compiler definitions that built Skia also govern its consumer headers.
    # This is the definition extraction used by the pinned SkiaBuild distribution.
    commands = run([args.ninja, "-C", str(output), "-t", "commands", *TARGETS], source, capture=True)
    defines = {}
    for match in re.finditer(r"(?:^|\s)-D(\w+)(?:=(\S+))?", commands):
        name, value = match.groups()
        if name == "NDEBUG" or name.startswith(("SK_", "SKIA_", "SKSHAPER_", "SKUNICODE_", "GR_", "U_")):
            defines[name] = value or "1"
    if "SK_GANESH" not in defines or "SK_SHAPER_HARFBUZZ_AVAILABLE" not in defines:
        raise RuntimeError("Skia build omitted the required graphics or shaping capability")
    config = "// Generated from the owned Skia build's native compiler definitions.\n#pragma once\n"
    config += "".join("#ifndef " + key + "\n#define " + key + " " + value + "\n#endif\n"
                      for key, value in sorted(defines.items()))
    write_changed(output / "skia-build-config.h", config)
    archives = write_link_args(args, source, output)
    if not args.prepare_only:
        run([args.ninja, "-C", str(output), "-j", str(args.jobs), *TARGETS], source)
        write_archive_state(output, archives)
    elif all(path.is_file() for path in archives):
        write_archive_state(output, archives)


if __name__ == "__main__":
    main()
