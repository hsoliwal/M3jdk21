#!/usr/bin/env python3
# Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
# SPDX-License-Identifier: GPL-2.0-only
"""Compile exact HotSpot loader functions against explicit Android API models.

The selected functions and unchanged unload caller are extracted byte-for-byte
from the requested source roots. Real host shared libraries, symbol versions,
dlopen/dlclose and C++20 sanitizers execute; this is not a full HotSpot/NDK build.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import tempfile

SOURCE = Path("src/hotspot/os/linux/os_linux.cpp")
CALLER = Path("src/hotspot/os/posix/os_posix.cpp")
LINUX_HEADER = Path("src/hotspot/os/linux/os_linux.hpp")
SIGNATURES = (
    "const char* os::Linux::dll_path(void* lib)",
    "void* os::Linux::libnuma_dlsym(void* handle, const char *name)",
    "void* os::Linux::libnuma_v2_dlsym(void* handle, const char* name)",
)

PRELUDE = r'''
// Availability fixture is independent of the production source branches.
// Android has no dlinfo/RTLD_DI_LINKMAP; dlvsym starts at API 24.
#if defined(__ANDROID__)
#define dlinfo hidden_unavailable_dlinfo
#define RTLD_DI_LINKMAP hidden_unavailable_RTLD_DI_LINKMAP
#endif
#if (defined(__ANDROID__) && __ANDROID_API__ < 24) || defined(MUSL_LIBC)
#define dlvsym hidden_unavailable_dlvsym
#endif
#include <dlfcn.h>
#include <link.h>
#undef dlinfo
#undef RTLD_DI_LINKMAP
#undef dlvsym
#include <cassert>
#include <cinttypes>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <string>

static int checks, copies, frees, closed;
static bool refuse_unload;
static std::string event_path, event_format, log_path;
static void check(bool value, const char *message) {
    ++checks;
    if (!value) { std::fprintf(stderr, "CONTRACT_FAILURE %s\n", message); std::abort(); }
}
#undef assert
#define assert(condition, message) check((condition), (message))
#define INTPTR_FORMAT "%" PRIxPTR
#define LINUX_ONLY(statement) statement
#define LINUX 1
static uintptr_t p2i(void *value) { return reinterpret_cast<uintptr_t>(value); }
struct Events {
    static void log_dll_message(void *, const char *format, const char *path, uintptr_t, ...) {
        event_path = path; event_format = format;
    }
};
static void log_capture(const char *, const char *path, uintptr_t, ...) { log_path = path; }
#define log_info(tag) log_capture

class os {
public:
    class Linux {
    public:
        static const char *dll_path(void *);
        static void *libnuma_dlsym(void *, const char *);
        static void *libnuma_v2_dlsym(void *, const char *);
    };
    static void dll_unload(void *);
    static char *strdup(const char *text) {
        ++copies;
        size_t length = std::strlen(text) + 1;
        char *result = static_cast<char *>(std::malloc(length));
        check(result != nullptr, "path copy allocation");
        std::memcpy(result, text, length);
        return result;
    }
    static void free(void *value) { if (value != nullptr) ++frees; std::free(value); }
    static bool pd_dll_unload(void *handle, char *error, int size) {
        if (refuse_unload) { std::snprintf(error, size, "contract unload refusal"); return false; }
        ++closed;
        return dlclose(handle) == 0;
    }
};
'''

MAIN = r'''
static int invoke(void *value) {
    check(value != nullptr, "required symbol exists");
    return reinterpret_cast<int (*)()>(value)();
}
int main(int argc, char **argv) {
    check(argc == 2, "fixture path argument");
    void *handle = dlopen(argv[1], RTLD_NOW | RTLD_LOCAL);
    check(handle != nullptr, "open versioned fixture");
    const char *path = os::Linux::dll_path(handle);
#ifdef __ANDROID__
    check(path == nullptr, "Android reports unavailable path");
#else
    check(path != nullptr && std::strstr(path, "versioned") != nullptr,
          "Linux loader path stays available");
#endif
#if (defined(__ANDROID__) && __ANDROID_API__ < 24) || defined(MUSL_LIBC)
    check(invoke(os::Linux::libnuma_dlsym(handle, "contract_symbol")) == 22,
          "v1 unversioned fallback uses default symbol");
#else
    check(invoke(os::Linux::libnuma_dlsym(handle, "contract_symbol")) == 11,
          "v1 preserves exact symbol version");
#endif
    check(invoke(os::Linux::libnuma_dlsym(handle, "contract_legacy")) == 7,
          "v1 legacy symbol");
    check(invoke(os::Linux::libnuma_dlsym(handle, "contract_modern")) == 33,
          "v1 missing version retains unversioned fallback");
    check(os::Linux::libnuma_dlsym(handle, "contract_missing") == nullptr,
          "missing v1 symbol stays missing");
#if defined(__ANDROID__) && __ANDROID_API__ < 24
    check(os::Linux::libnuma_v2_dlsym(handle, "contract_symbol") == nullptr,
          "pre24 cannot establish v2 version");
    check(os::Linux::libnuma_v2_dlsym(handle, "contract_legacy") == nullptr,
          "pre24 must not misbind legacy-only symbol to v2");
    check(os::Linux::libnuma_v2_dlsym(handle, "contract_modern") == nullptr,
          "pre24 unversioned symbol is not exact-version evidence");
#else
    check(invoke(os::Linux::libnuma_v2_dlsym(handle, "contract_symbol")) == 22,
          "v2 default version");
    check(invoke(os::Linux::libnuma_v2_dlsym(handle, "contract_modern")) == 33,
          "v2 exact modern symbol");
#ifdef MUSL_LIBC
    check(invoke(os::Linux::libnuma_v2_dlsym(handle, "contract_legacy")) == 7,
          "existing musl version-ignoring behavior is retained");
#else
    check(os::Linux::libnuma_v2_dlsym(handle, "contract_legacy") == nullptr,
          "legacy-only symbol is not v2");
#endif
#endif
    check(os::Linux::libnuma_v2_dlsym(handle, "contract_missing") == nullptr,
          "missing v2 symbol stays missing");

    refuse_unload = true;
    os::dll_unload(handle);
    check(closed == 0, "failed unload preserves handle");
    check(event_format.find("failed") != std::string::npos, "existing failure log path");
    check(event_path == log_path, "failure logging path ownership");
    check(invoke(os::Linux::libnuma_dlsym(handle, "contract_legacy")) == 7,
          "handle remains usable after refused unload");
    refuse_unload = false;
    os::dll_unload(handle);
    check(closed == 1, "unload closes library once");
    check(event_format.find("Unloaded") != std::string::npos, "existing successful unload log");
    check(event_path == log_path, "success logging path ownership");
#ifdef __ANDROID__
    check(event_path == "<not available>", "existing unavailable-path log sentinel");
    check(copies == 0 && frees == 0, "Android needs no fabricated path allocation");
#else
    check(event_path.find("versioned") != std::string::npos, "copied path survives dlclose");
    check(copies == 2 && frees == 2, "Linux frees both loader-path copies");
#endif
    check(dlopen(argv[1], RTLD_NOW | RTLD_NOLOAD) == nullptr, "fixture really unloaded");
    std::printf("PASS checks=%d copies=%d frees=%d closed=%d\n", checks, copies, frees, closed);
    return 0;
}
'''

LIBRARY = r'''
int symbol_v1(void) { return 11; }
int symbol_v2(void) { return 22; }
int contract_legacy(void) { return 7; }
int contract_modern(void) { return 33; }
__asm__(".symver symbol_v1,contract_symbol@libnuma_1.1");
__asm__(".symver symbol_v2,contract_symbol@@libnuma_1.2");
struct bitmask { unsigned long size; unsigned long *maskp; };
static unsigned long membind_bits = 3, interleave_bits = 0;
static struct bitmask membind_mask = {2, &membind_bits};
static struct bitmask interleave_mask = {2, &interleave_bits};
static int available_value, max_node_value = 1;
unsigned long numa_all_nodes = 3;
struct bitmask *numa_all_nodes_ptr = &membind_mask, *numa_nodes_ptr = &membind_mask;
void contract_numa_configure(int available, int maximum, unsigned long bits) {
    available_value = available; max_node_value = maximum; membind_bits = bits;
}
int numa_available(void) { return available_value; }
int numa_max_node(void) { return max_node_value; }
int numa_num_configured_nodes(void) { return max_node_value + 1; }
struct bitmask *numa_get_membind(void) { return &membind_mask; }
struct bitmask *numa_get_interleave_mask(void) { return &interleave_mask; }
int numa_bitmask_isbitset(struct bitmask *mask, unsigned int node) {
    return node < mask->size && ((*mask->maskp >> node) & 1);
}
int numa_node_to_cpus(int node, unsigned long *buffer, int length) {
    (void)node; (void)buffer; (void)length; return 0;
}
int numa_tonode_memory(void *start, __SIZE_TYPE__ size, int node) {
    (void)start; (void)size; (void)node; return 0;
}
void numa_interleave_memory(void *start, __SIZE_TYPE__ size, unsigned long *mask) {
    (void)start; (void)size; (void)mask;
}
void numa_set_bind_policy(int policy) { (void)policy; }
int numa_distance(int first, int second) { return first == second ? 10 : 20; }
long numa_move_pages(int pid, unsigned long count, void **pages, const int *nodes,
                     int *status, int flags) {
    (void)pid; (void)count; (void)pages; (void)nodes; (void)status; (void)flags; return 0;
}
void numa_set_preferred(int node) { (void)node; }
'''
VERSIONS = '''libnuma_1.1 {
  global: contract_symbol; contract_legacy; contract_numa_configure;
          numa_available; numa_max_node; numa_num_configured_nodes; numa_all_nodes;
          numa_all_nodes_ptr; numa_nodes_ptr; numa_bitmask_isbitset; numa_node_to_cpus;
          numa_tonode_memory; numa_interleave_memory; numa_set_bind_policy;
          numa_distance; numa_move_pages; numa_set_preferred;
};
libnuma_1.2 {
  global: contract_symbol; contract_modern; numa_get_membind; numa_get_interleave_mask;
} libnuma_1.1;
'''

NUMA_SUPPORT = r'''
#include <new>
#include <sys/syscall.h>
struct bitmask { unsigned long size; unsigned long *maskp; };
enum MemTag { mtInternal };
static int created_arrays, rebuilt_maps, warnings_seen, syscall_calls, open_calls, lookup_calls;
static int cpu_result;
static bool memory_policy_available = true, library_available = true, can_commit = true;
static const char *numa_library_path;
static void *numa_loaded_handle;
static bool UseNUMA = true, UseNUMAInterleaving, UseParallelGC, UseLargePages;
static bool UseAdaptiveSizePolicy = true, UseAdaptiveNUMAChunkSizing = true;
template<typename T> struct GrowableArray {
    GrowableArray(int, MemTag) { ++created_arrays; }
};
void *operator new(size_t bytes, MemTag) { return ::operator new(bytes); }
void operator delete(void *value, MemTag) noexcept { ::operator delete(value); }
#define FLAG_SET_ERGO(name, value) ((name) = (value))
#define FLAG_SET_ERGO_IF_DEFAULT(name, value) ((name) = (value))
#define CAST_TO_FN_PTR(type, value) reinterpret_cast<type>(value)
struct NumaLogTarget {};
#define LogTarget(level, tag) NumaLogTarget
struct LogStream {
    explicit LogStream(NumaLogTarget&) {}
    template<typename... Args> void print(const char *, Args...) {}
};
static void warning(const char *, ...) { ++warnings_seen; }
static int test_sched_getcpu() { return cpu_result; }
static long test_numa_syscall(long, ...) {
    ++syscall_calls;
    return memory_policy_available ? 0 : -1;
}
static void *test_numa_dlopen(const char *name, int flags) {
    ++open_calls;
    check(std::strcmp(name, "libnuma.so.1") == 0, "actual NUMA library name");
    if (!library_available) return nullptr;
    numa_loaded_handle = dlopen(numa_library_path, flags);
    return numa_loaded_handle;
}
static void *test_numa_dlsym(void *handle, const char *name) {
    ++lookup_calls;
    return dlsym(handle, name);
}
'''

NUMA_MAIN = r'''
static void cleanup() {
    delete os::Linux::_nindex_to_node; os::Linux::_nindex_to_node = nullptr;
    delete os::Linux::_cpu_to_node; os::Linux::_cpu_to_node = nullptr;
    if (numa_loaded_handle != nullptr) { dlclose(numa_loaded_handle); numa_loaded_handle = nullptr; }
}
static void reset() {
    cleanup();
    RESET_POINTERS
    os::Linux::set_sched_getcpu(test_sched_getcpu);
    created_arrays = rebuilt_maps = syscall_calls = open_calls = lookup_calls = warnings_seen = 0;
    cpu_result = 0; memory_policy_available = library_available = can_commit = true;
    UseNUMA = true; UseNUMAInterleaving = false; UseParallelGC = UseLargePages = false;
    UseAdaptiveSizePolicy = UseAdaptiveNUMAChunkSizing = true;
}
int main(int argc, char **argv) {
    check(argc >= 2, "NUMA fixture path"); numa_library_path = argv[1];
    void *control = dlopen(argv[1], RTLD_NOW | RTLD_LOCAL);
    check(control != nullptr, "NUMA fixture control handle");
    auto configure = reinterpret_cast<void (*)(int, int, unsigned long)>(
            dlsym(control, "contract_numa_configure"));
    check(configure != nullptr, "NUMA fixture control function");
    reset(); configure(0, 1, 3);
    if (argc == 3) {
        check(os::Linux::libnuma_v2_dlsym(control, "numa_get_membind") == nullptr,
              "rejected candidate really lacks v2 binding");
        check(os::Linux::libnuma_dlsym(control, "numa_available") != nullptr,
              "rejected candidate has usable v1 availability");
        std::fputs("REJECTED_NULL_V2_INIT_PROBE\n", stderr); std::fflush(stderr);
        os::Linux::libnuma_init();
        check(false, "rejected candidate should reproduce its null v2 call");
    }

    for (int scenario = 0; scenario < 8; ++scenario) {
        reset(); configure(0, 1, 3);
        if (scenario == 1) cpu_result = -1;
        if (scenario == 2) memory_policy_available = false;
        if (scenario == 3) library_available = false;
        if (scenario == 4) configure(-1, 1, 3);
        if (scenario == 5) configure(0, 0, 1);
        if (scenario == 6) configure(0, 1, 1);
        if (scenario == 7) { UseParallelGC = UseLargePages = true; can_commit = false; }
        os::Linux::numa_init();
#if defined(__ANDROID__) && __ANDROID_API__ < 24
        check(!UseNUMA && !UseNUMAInterleaving, "pre24 existing caller disables both NUMA modes");
        check(open_calls == 0 && lookup_calls == 0 && syscall_calls == 0,
              "pre24 refuses before loading, querying or publishing");
        check(os::Linux::_numa_available == nullptr && os::Linux::_numa_get_membind == nullptr
                && os::Linux::_numa_get_interleave_mask == nullptr, "pre24 pointers stay unpublished");
        check(created_arrays == 0 && rebuilt_maps == 0, "pre24 builds no NUMA maps");
        check(UseAdaptiveSizePolicy && UseAdaptiveNUMAChunkSizing && warnings_seen == 0,
              "pre24 rejection does not mutate unrelated GC policy");
#else
        bool initialized = scenario == 0 || scenario >= 5;
        bool enabled = scenario == 0 || scenario == 7;
        check(UseNUMA == enabled && UseNUMAInterleaving == enabled,
              "existing NUMA policy decisions remain");
        check(created_arrays == (initialized ? 2 : 0) && rebuilt_maps == (initialized ? 2 : 0),
              "existing map construction is preserved");
        check(open_calls == (scenario == 1 || scenario == 2 ? 0 : 1), "existing library-load gating");
        check(syscall_calls == (scenario == 1 ? 0 : 1), "existing syscall gate");
        check(warnings_seen == (scenario == 7 ? 1 : 0), "existing GC warning branch");
        check(UseAdaptiveSizePolicy == (scenario != 7) && UseAdaptiveNUMAChunkSizing == (scenario != 7),
              "existing adaptive sizing policy");
#endif
    }
    cleanup(); dlclose(control);
    std::printf("PASS numa-init checks=%d\n", checks);
    return 0;
}
'''


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def function(text: str, signature: str) -> str:
    if text.count(signature) != 1:
        raise ValueError("ambiguous exact source signature: " + signature)
    start = text.index(signature)
    opening = text.index("{", start)
    depth = 0
    for position in range(opening, len(text)):
        if text[position] == "{": depth += 1
        if text[position] == "}": depth -= 1
        if depth == 0:
            return text[start:position + 1]
    raise ValueError("unterminated source function")


def numa_projection(text: str, header: str, selected: dict) -> tuple[str, dict]:
    start = header.index("  typedef int (*sched_getcpu_func_t)")
    end = header.index("  static int sched_getcpu_syscall(void);", start)
    declarations = header[start:end]
    fields = re.findall(r"(?m)^  static (.+?) (_[A-Za-z0-9_]+);$", declarations)
    if len(fields) != 21:
        raise ValueError("unexpected actual NUMA field projection")
    declarations = re.sub(r"(?m)^  static (.+? _[A-Za-z0-9_]+);$",
                          r"  inline static \1;", declarations)
    inline_signatures = (
        "static int sched_getcpu()", "static int numa_max_node()",
        "static int numa_available()", "static bool is_running_in_interleave_mode()",
        "static void set_configured_numa_policy(NumaAllocationPolicy numa_policy)",
        "static NumaAllocationPolicy identify_numa_policy()", "static bool is_bound_to_single_node()",
    )
    inline_functions = "\n".join(function(header, name) for name in inline_signatures)
    initialization = function(text, "bool os::Linux::libnuma_init()")
    caller = function(text, "void os::Linux::numa_init()")
    syscall_gate = function(text, "static bool numa_syscall_check()")
    clazz = """
class os {
public:
    static bool can_commit_large_page_memory() { return can_commit; }
    class Linux {
    public:
        enum NumaAllocationPolicy { NotInitialized, Membind, Interleave };
        inline static NumaAllocationPolicy _current_numa_policy = NotInitialized;
        inline static GrowableArray<int> *_cpu_to_node, *_nindex_to_node;
        static void *libnuma_dlsym(void *, const char *);
        static void *libnuma_v2_dlsym(void *, const char *);
        static bool libnuma_init();
        static void numa_init();
        static void rebuild_nindex_to_node_map() { ++rebuilt_maps; }
        static void rebuild_cpu_to_node_map() { ++rebuilt_maps; }
""" + declarations + "\n" + inline_functions + "\n};\n};\n"
    reset = "\n".join("os::Linux::" + name + " = nullptr;" for _, name in fields)
    source = (PRELUDE[:PRELUDE.index("class os {")] + NUMA_SUPPORT + clazz
        + "\n#define dlsym test_numa_dlsym\n" + selected["shim"] + "\n" + selected["lookup"]
        + "\n#undef dlsym\n#define syscall test_numa_syscall\n" + syscall_gate
        + "\n#undef syscall\n#define dlopen test_numa_dlopen\n" + initialization
        + "\n#undef dlopen\n" + caller + "\n" + NUMA_MAIN.replace("RESET_POINTERS", reset))
    seals = {name: hashlib.sha256(value.encode()).hexdigest() for name, value in {
        "libnuma_init": initialization, "numa_init": caller, "numa_syscall_check": syscall_gate,
        "header_field_setters_projection": declarations, "header_inline_callers": inline_functions,
    }.items()}
    return source, seals


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ("before", "after", "caller-root", "output"):
        parser.add_argument("--" + name, type=Path, required=True)
    parser.add_argument("--cc", default="cc")
    parser.add_argument("--cxx", default="c++")
    parser.add_argument("--rejected-root", type=Path,
                        help="Optional retained unsafe candidate for its null-v2 regression")
    args = parser.parse_args()
    for name in ("before", "after", "caller_root", "output"):
        setattr(args, name, getattr(args, name).resolve())
    if args.rejected_root is not None: args.rejected_root = args.rejected_root.resolve()
    if any(args.output.is_relative_to(root) for root in (args.before, args.after, args.caller_root)):
        raise ValueError("receipt must be outside all source roots")
    sources = [args.before / SOURCE, args.after / SOURCE, args.caller_root / CALLER,
               args.caller_root / LINUX_HEADER]
    if args.rejected_root is not None:
        sources.append(args.rejected_root / SOURCE)
        if args.output.is_relative_to(args.rejected_root):
            raise ValueError("receipt must be outside the rejected source root")
        if digest(args.rejected_root / SOURCE) != "637cd99a2714dfa663f42916aa0988b5dcb33c5437b93cc9f0756eaf2b4338f5":
            raise ValueError("rejected candidate does not match its recorded identity")
    source_hashes = {str(path): digest(path) for path in sources}
    commands, artifacts, failures, blocks, projections = [], {}, [], {}, {}
    sanitizer_environment = {"ASAN_OPTIONS": "detect_leaks=0:halt_on_error=1",
                             "UBSAN_OPTIONS": "halt_on_error=1:print_stacktrace=1"}

    def run(label: str, command: list, expected_identifier: str | None = None,
            expected_null_call: bool = False) -> str:
        completed = subprocess.run([str(x) for x in command], text=True,
            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=60,
            env={**os.environ, **sanitizer_environment})
        good = completed.returncode == 0 if expected_identifier is None else (
            completed.returncode != 0 and expected_identifier in completed.stdout
            and re.search(r"not (?:been )?declared|undeclared", completed.stdout) is not None)
        if expected_null_call:
            good = (completed.returncode != 0 and "REJECTED_NULL_V2_INIT_PROBE" in completed.stdout
                    and "AddressSanitizer" in completed.stdout and "SEGV" in completed.stdout
                    and re.search(r"pc 0x0+\b", completed.stdout) is not None)
        commands.append({"label": label, "command": [str(x) for x in command],
                         "exit_code": completed.returncode, "passed": good,
                         "expected_missing_identifier": expected_identifier,
                         "expected_rejected_null_v2_call": expected_null_call,
                         "output": completed.stdout})
        if not good:
            raise RuntimeError(label + "\n" + completed.stdout)
        return completed.stdout

    try:
        run("c-compiler-identity", [args.cc, "--version"])
        run("cxx-compiler-identity", [args.cxx, "--version"])
        caller = function((args.caller_root / CALLER).read_text(), "void os::dll_unload(void *lib)")
        header = (args.caller_root / LINUX_HEADER).read_text()
        roots = [("before", args.before), ("after", args.after)]
        if args.rejected_root is not None: roots.append(("rejected", args.rejected_root))
        for version, root in roots:
            text = (root / SOURCE).read_text()
            marker = "#ifdef MUSL_LIBC\n// dlvsym is not a part of POSIX"
            if text.count(marker) != 1: raise ValueError("ambiguous existing musl adapter")
            start = text.index(marker)
            shim = text[start:text.index("#endif", start) + len("#endif")]
            selected = [function(text, signature) for signature in SIGNATURES]
            blocks[version] = {"shim": shim, "path": selected[0], "lookup": "\n".join(selected[1:])}
            unit, seals = numa_projection(text, header, blocks[version])
            projections[version] = {"source": unit, "seals": seals}
        if blocks["before"]["shim"] != blocks["after"]["shim"]:
            raise AssertionError("existing musl adapter changed")
        for name in ("numa_init", "numa_syscall_check", "header_field_setters_projection", "header_inline_callers"):
            if projections["before"]["seals"][name] != projections["after"]["seals"][name]:
                raise AssertionError("unreviewed affected caller change: " + name)
        with tempfile.TemporaryDirectory(prefix="android-loader-") as directory:
            work = Path(directory)
            library_source = work / "versioned.c"; library_source.write_text(LIBRARY)
            version_script = work / "versioned.map"; version_script.write_text(VERSIONS)
            library = work / "libversioned.so"
            run("build-real-versioned-library", [args.cc, "-std=c17", "-shared", "-fPIC",
                "-Wall", "-Wextra", "-Werror", library_source,
                "-Wl,--version-script=" + str(version_script), "-o", library])
            outputs = {}
            for profile, defines, android, api in (
                    ("host-linux", [], False, 0),
                    ("musl-symbol-model", ["-DMUSL_LIBC"], False, 0),
                    *[("android-api" + str(api) + "-model", ["-D__ANDROID__", "-D__ANDROID_API__=" + str(api)], True, api)
                      for api in (21, 23, 24, 35)]):
                common = [args.cxx, "-std=c++20", "-D_GNU_SOURCE", "-Wall", "-Wextra", "-Werror",
                          "-Wno-unused-parameter", "-Wno-unused-function", "-Wno-unused-variable", *defines]
                for version in ("before", "after"):
                    selected = blocks[version]
                    prefix = profile + "-" + version
                    if android:
                        for atom in ("path", "lookup"):
                            probe = work / (prefix + "-" + atom + "-declarations.cpp")
                            probe.write_text(PRELUDE + "\n" + selected["shim"] + "\n" + selected[atom])
                            missing = None
                            if version == "before":
                                if atom == "path": missing = "dlinfo"
                                elif api < 24: missing = "dlvsym"
                            run(prefix + "-" + atom + "-declarations", common + ["-fsyntax-only", probe], missing)
                        if version == "before": continue
                    unit = work / (prefix + ".cpp")
                    unit.write_text(PRELUDE + "\n" + selected["shim"] + "\n"
                        + selected["path"] + "\n" + selected["lookup"] + "\n" + caller + "\n" + MAIN)
                    executable = work / prefix
                    run(prefix + "-selected-source-compile", common + ["-g", "-O1",
                        "-fsanitize=address,undefined", "-fno-sanitize-recover=all",
                        unit, "-ldl", "-o", executable])
                    outputs[(profile, version)] = run(prefix + "-real-loader-runtime", [executable, library])
                    numa_unit = work / (prefix + "-numa-init.cpp")
                    numa_unit.write_text(projections[version]["source"])
                    numa_executable = work / (prefix + "-numa-init")
                    run(prefix + "-full-numa-callers-compile", common + ["-g", "-O1",
                        "-fsanitize=address,undefined", "-fno-sanitize-recover=all",
                        numa_unit, "-ldl", "-o", numa_executable])
                    outputs[(profile, version, "numa")] = run(prefix + "-full-numa-callers-runtime",
                        [numa_executable, library])
                if android and api < 24 and args.rejected_root is not None:
                    rejected_unit = work / (profile + "-rejected-numa-init.cpp")
                    rejected_unit.write_text(projections["rejected"]["source"])
                    rejected_executable = work / (profile + "-rejected-numa-init")
                    run(profile + "-rejected-numa-init-compile", common + ["-g", "-O1",
                        "-fsanitize=address,undefined", "-fno-sanitize-recover=all",
                        rejected_unit, "-ldl", "-o", rejected_executable])
                    run(profile + "-rejected-numa-null-call", [rejected_executable, library, "reject"],
                        expected_null_call=True)
            for profile in ("host-linux", "musl-symbol-model"):
                if outputs[(profile, "before")] != outputs[(profile, "after")]:
                    raise AssertionError(profile + " runtime control changed")
                if outputs[(profile, "before", "numa")] != outputs[(profile, "after", "numa")]:
                    raise AssertionError(profile + " NUMA initialization control changed")
            for path in sorted(work.iterdir()):
                if path.is_file(): artifacts[path.name] = digest(path)
    except Exception as failure:
        failures.append(str(failure))
    unchanged = all(digest(Path(path)) == value for path, value in source_hashes.items())
    block_hashes = {version: {name: hashlib.sha256(text.encode()).hexdigest()
                   for name, text in values.items()} for version, values in blocks.items()}
    report = {
        "status": "PASS" if not failures and unchanged else "FAIL",
        "scope": "Exact source-function extraction plus full libnuma_init/numa_init and unload caller projections; C++20 declarations; real host versioned DSOs; ASan and UBSan.",
        "limits": ["Not a complete HotSpot translation unit or source-built JDK",
                   "Android and musl availability are independent host-header omission models",
                   "No Android NDK/ARM/device or real libnuma runtime qualification",
                   "NUMA syscall/CPU/library availability, map rebuilding, and HotSpot flag/log infrastructure are explicit test adapters; caller bodies and relevant header accessors/setters are source-derived",
                   "LeakSanitizer is disabled because the executor runs under ptrace; ASan/UBSan memory and undefined-behavior checks remain enabled"],
        "sanitizer_environment": sanitizer_environment,
        "source_sha256": source_hashes, "source_block_sha256": block_hashes,
        "numa_caller_sha256": {version: projection["seals"] for version, projection in projections.items()},
        "sources_unchanged": unchanged, "artifact_sha256": artifacts,
        "commands": commands, "failures": failures,
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, indent=2) + "\n")
    print(json.dumps({"status": report["status"], "commands": len(commands),
                      "expected_baseline_compile_failures": sum(x["expected_missing_identifier"] is not None for x in commands),
                      "artifacts": len(artifacts), "output": str(args.output)}))
    return 0 if report["status"] == "PASS" else 1


if __name__ == "__main__":
    raise SystemExit(main())
