#!/usr/bin/env python3
# Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
# SPDX-License-Identifier: GPL-2.0-only
"""Compile the complete UnixNativeDispatcher and exercise its actual JNI entry points.

Android symbol availability is an explicit model. JNI, libc file operations and
the JVM are real host executions, not an Android SDK/device qualification.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile

SOURCE = Path("src/java.base/unix/native/libnio/fs/UnixNativeDispatcher.c")
JAVA_SOURCE = Path("src/java.base/unix/classes/sun/nio/fs/UnixNativeDispatcher.java")
HEADERS = [
    Path("src/java.base/share/native/libjava/jni_util.h"),
    Path("src/java.base/share/native/libjava/jlong.h"),
    Path("src/java.base/unix/native/libjava/jlong_md.h"),
]

JAVA = r'''
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

public class AndroidFileTimestampContracts {
    private static native int configure(int symbols, int behavior, int error, int interrupts);
    private static native void setTimes(int fd, long access, long modification);
    private static native int openFile(String path);
    private static native void closeFile(int fd);
    private static native long[] statTimes(int fd);
    private static native long[] trace();
    private static native int errnoOf(Throwable failure);
    private static native boolean rawStatFallback();
    private static long checks;
    private static final int ALL = 127, FUTIMESAT = 1, FUTIMENS = 2;
    private static final int OPENAT = 2, FUTIMES = 4, FUTIMENS_CAP = 8;

    private static synchronized void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    private static void same(long[] expected, long[] actual, String context) {
        check(Arrays.equals(expected, actual), context + " expected="
                + Arrays.toString(expected) + " actual=" + Arrays.toString(actual));
    }
    private static void internalError(int fd) {
        try {
            setTimes(fd, 1, 2);
            throw new AssertionError("missing InternalError");
        } catch (InternalError expected) {
            check("my_futimesat_func is NULL".equals(expected.getMessage()),
                    "existing InternalError message changed");
        }
    }
    private static void unixError(int fd, int expected) {
        try {
            setTimes(fd, 1, 2);
            throw new AssertionError("missing UnixException");
        } catch (Throwable failure) {
            check(failure.getClass().getName().equals("sun.nio.fs.UnixException"),
                    "wrong exception: " + failure);
            check(errnoOf(failure) == expected, "wrong errno");
        }
    }
    private static void matrix(boolean androidFallback) {
        Integer otherCapabilities = null;
        for (int symbols = 0; symbols <= ALL; symbols++) {
            int actual = configure(symbols, 1, 0, 0);
            boolean time = (symbols & FUTIMESAT) != 0
                    || (androidFallback && (symbols & FUTIMENS) != 0);
            boolean directories = (symbols & (4 | 16 | 32 | 64)) == (4 | 16 | 32 | 64)
                    && ((symbols & 8) != 0 || rawStatFallback());
            int expected = (time ? FUTIMES : 0)
                    | ((symbols & FUTIMENS) != 0 ? FUTIMENS_CAP : 0)
                    | (time && directories ? OPENAT : 0);
            check((actual & (OPENAT | FUTIMES | FUTIMENS_CAP)) == expected,
                    "capability matrix symbols=" + symbols + " actual=" + actual
                            + " expected=" + expected);
            int others = actual & ~(OPENAT | FUTIMES | FUTIMENS_CAP);
            if (otherCapabilities == null) otherCapabilities = others;
            check(otherCapabilities == others, "unrelated capability drift");
        }
    }
    private static void conversion(int symbols, boolean nanos, int fd) {
        // Literal boundary witnesses distinguish seconds, microseconds and nanoseconds.
        long[][] cases = {
                {0, 0, 0}, {1, 0, 1}, {999999, 0, 999999}, {1000000, 1, 0},
                {1000001, 1, 1}, {-1, 0, -1}, {-999999, 0, -999999},
                {-1000000, -1, 0}, {-1000001, -1, -1},
                {Long.MAX_VALUE, 9223372036854L, 775807},
                {Long.MIN_VALUE, -9223372036854L, -775808}
        };
        for (int i = 0; i < cases.length; i++) {
            long[] a = cases[i], m = cases[cases.length - 1 - i];
            configure(symbols, 1, 0, 0);
            setTimes(fd, a[0], m[0]);
            long scale = nanos ? 1000 : 1;
            same(new long[]{nanos ? 0 : 1, nanos ? 1 : 0,
                            a[1], a[2] * scale, m[1], m[2] * scale},
                    trace(), "conversion " + i);
        }
    }
    private static void realFile(int symbols, boolean nanos, Path path) throws Exception {
        int fd = openFile(path.toString());
        check(fd >= 0, "open temporary file");
        try {
            long[][] values = {{0, 0}, {1, 999999}, {1000000, 1000001},
                    {1700000000123456L, 1700000010987654L}};
            for (long[] times : values) {
                configure(symbols, 0, 0, 0);
                setTimes(fd, times[0], times[1]);
                same(times, statTimes(fd), "real descriptor timestamps");
                long[] observed = trace();
                check(observed[nanos ? 1 : 0] == 1, "expected backend");
                check(observed[nanos ? 0 : 1] == 0, "no unintended backend");
            }
            configure(symbols, 0, 0, 3);
            setTimes(fd, 1234567890L, 2345678901L);
            same(new long[]{1234567890L, 2345678901L}, statTimes(fd), "EINTR then success");
            check(trace()[nanos ? 1 : 0] == 4, "all EINTR attempts retried");

            for (int error : new int[]{5, 9, 13, 22}) {
                configure(symbols, 2, error, 2);
                unixError(fd, error);
                check(trace()[nanos ? 1 : 0] == 3, "retry stops at non-EINTR error");
            }
            configure(symbols, 0, 0, 0);
            unixError(-1, 9);
            configure(symbols, 0, 0, 0);
            try {
                setTimes(fd, -1, 1000000);
                throw new AssertionError("fractional negative timestamps must retain EINVAL");
            } catch (Throwable failure) {
                check(failure.getClass().getName().equals("sun.nio.fs.UnixException"),
                        "negative fraction exception");
                check(errnoOf(failure) == 22, "negative fraction errno");
            }
            conversion(symbols, nanos, fd);
        } finally {
            closeFile(fd);
        }
    }
    private static void threads(Path directory, boolean fallback) throws Exception {
        int symbols = fallback ? ALL & ~FUTIMESAT : ALL;
        configure(symbols, 0, 0, 0);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread[] workers = new Thread[4];
        for (int n = 0; n < workers.length; n++) {
            final int id = n;
            workers[n] = new Thread(() -> {
                int fd = -1;
                try {
                    Path path = Files.createTempFile(directory, "worker", ".tmp");
                    fd = openFile(path.toString());
                    check(fd >= 0, "thread descriptor");
                    for (int i = 0; i < 64; i++) {
                        long a = 1700000000000000L + id * 1000L + i;
                        long m = a + 1111111L;
                        setTimes(fd, a, m);
                        same(new long[]{a, m}, statTimes(fd), "thread descriptor isolation");
                    }
                } catch (Throwable problem) {
                    failure.compareAndSet(null, problem);
                } finally {
                    if (fd >= 0) closeFile(fd);
                }
            }, "native-timestamp-" + n);
            workers[n].start();
        }
        for (Thread worker : workers) worker.join();
        if (failure.get() != null) throw new AssertionError("threaded JNI", failure.get());
        same(new long[]{fallback ? 0 : 256, fallback ? 256 : 0},
                Arrays.copyOf(trace(), 2), "concurrent call counts");
    }
    public static void main(String[] args) throws Exception {
        System.load(args[0]);
        String mode = args[1];
        boolean fallback = Boolean.parseBoolean(args[2]);
        Path directory = Path.of(args[3]);
        if (mode.equals("capability-probe")) {
            int capabilities = configure(ALL & ~FUTIMESAT, 0, 0, 0);
            check((capabilities & (OPENAT | FUTIMES)) == (OPENAT | FUTIMES),
                    "ANDROID_FUTIMENS_CAPABILITY_MISSING");
        } else if (mode.equals("timestamp-probe")) {
            configure(ALL & ~FUTIMESAT, 0, 0, 0);
            int fd = openFile(Files.createTempFile(directory, "probe", ".tmp").toString());
            try {
                try {
                    setTimes(fd, 1700000000123456L, 1700000001654321L);
                } catch (InternalError failure) {
                    throw new AssertionError("ANDROID_FUTIMENS_TIMESTAMP_UNAVAILABLE", failure);
                }
                same(new long[]{1700000000123456L, 1700000001654321L}, statTimes(fd), "probe");
            } finally { closeFile(fd); }
        } else {
            matrix(fallback);
            realFile(ALL, false, Files.createTempFile(directory, "modern", ".tmp"));
            realFile(ALL & ~FUTIMENS, false, Files.createTempFile(directory, "legacy", ".tmp"));
            if (fallback) {
                realFile(ALL & ~FUTIMESAT, true,
                        Files.createTempFile(directory, "android", ".tmp"));
            } else {
                configure(ALL & ~FUTIMESAT, 0, 0, 0);
                internalError(-1);
            }
            configure(ALL & ~(FUTIMESAT | FUTIMENS), 0, 0, 0);
            internalError(-1);
            threads(directory, fallback);
        }
        System.out.println("PASS " + mode + " checks=" + checks);
    }
}
'''

NATIVE = r'''
#include <jni.h>
#include <stdatomic.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <dlfcn.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/stat.h>
#include <sys/time.h>
#include <sys/syscall.h>
#include "AndroidFileTimestampContracts.h"

/* Model a platform with no raw fstatat syscall substitute when requested. */
#if defined(CONTRACT_NO_RAW_STAT)
#undef __NR_newfstatat
#undef __NR_fstatat64
#endif
static void *contract_dlsym(void *, const char *);
#define dlsym contract_dlsym
#include SOURCE_PATH
#undef dlsym

static int available_symbols, operation_behavior, injected_errno;
static atomic_int interruptions, timeval_calls, timespec_calls;
static _Thread_local jlong last_times[4];

static int intercept(void) {
    int remaining = atomic_load(&interruptions);
    while (remaining > 0) {
        if (atomic_compare_exchange_weak(&interruptions, &remaining, remaining - 1)) {
            errno = EINTR;
            return -1;
        }
    }
    if (operation_behavior == 2) {
        errno = injected_errno;
        return -1;
    }
    return 0;
}
static int contract_futimesat(int fd, const char *path, const struct timeval *times) {
    atomic_fetch_add(&timeval_calls, 1);
    last_times[0] = times[0].tv_sec; last_times[1] = times[0].tv_usec;
    last_times[2] = times[1].tv_sec; last_times[3] = times[1].tv_usec;
    if (path != NULL) { errno = EINVAL; return -1; }
    if (intercept() != 0) return -1;
    return operation_behavior == 1 ? 0 : futimesat(fd, path, times);
}
static int contract_futimens(int fd, const struct timespec *times) {
    atomic_fetch_add(&timespec_calls, 1);
    last_times[0] = times[0].tv_sec; last_times[1] = times[0].tv_nsec;
    last_times[2] = times[1].tv_sec; last_times[3] = times[1].tv_nsec;
    if (intercept() != 0) return -1;
    return operation_behavior == 1 ? 0 : futimens(fd, times);
}
static void *contract_dlsym(void *handle, const char *name) {
    if (strcmp(name, "futimesat") == 0)
        return (available_symbols & 1) ? (void *)contract_futimesat : NULL;
    if (strcmp(name, "futimens") == 0)
        return (available_symbols & 2) ? (void *)contract_futimens : NULL;
    int dependency = strcmp(name, "openat64") == 0 ? 4
            : strcmp(name, "fstatat64") == 0 ? 8
            : strcmp(name, "unlinkat") == 0 ? 16
            : strcmp(name, "renameat") == 0 ? 32
            : strcmp(name, "fdopendir") == 0 ? 64 : 0;
    if (dependency != 0 && !(available_symbols & dependency)) return NULL;
    return dlsym(handle, name);
}
JNIEXPORT jint JNICALL Java_AndroidFileTimestampContracts_configure(
        JNIEnv *env, jclass cls, jint symbols, jint behavior, jint error, jint interrupts) {
    available_symbols = symbols; operation_behavior = behavior; injected_errno = error;
    atomic_store(&interruptions, interrupts);
    atomic_store(&timeval_calls, 0); atomic_store(&timespec_calls, 0);
    memset(last_times, 0, sizeof(last_times));
    return Java_sun_nio_fs_UnixNativeDispatcher_init(env, cls);
}
JNIEXPORT void JNICALL Java_AndroidFileTimestampContracts_setTimes(
        JNIEnv *env, jclass cls, jint fd, jlong atime, jlong mtime) {
    Java_sun_nio_fs_UnixNativeDispatcher_futimes0(env, cls, fd, atime, mtime);
}
JNIEXPORT jint JNICALL Java_AndroidFileTimestampContracts_openFile(
        JNIEnv *env, jclass cls, jstring path) {
    const char *name = (*env)->GetStringUTFChars(env, path, NULL);
    if (name == NULL) return -1;
    int fd = open(name, O_RDWR);
    (*env)->ReleaseStringUTFChars(env, path, name);
    return fd;
}
JNIEXPORT void JNICALL Java_AndroidFileTimestampContracts_closeFile(
        JNIEnv *env, jclass cls, jint fd) { close(fd); }
JNIEXPORT jlongArray JNICALL Java_AndroidFileTimestampContracts_statTimes(
        JNIEnv *env, jclass cls, jint fd) {
    struct stat status;
    if (fstat(fd, &status) != 0) { throwUnixException(env, errno); return NULL; }
    jlong values[2] = {(jlong)status.st_atim.tv_sec * 1000000 + status.st_atim.tv_nsec / 1000,
                       (jlong)status.st_mtim.tv_sec * 1000000 + status.st_mtim.tv_nsec / 1000};
    jlongArray result = (*env)->NewLongArray(env, 2);
    if (result != NULL) (*env)->SetLongArrayRegion(env, result, 0, 2, values);
    return result;
}
JNIEXPORT jlongArray JNICALL Java_AndroidFileTimestampContracts_trace(JNIEnv *env, jclass cls) {
    jlong values[6] = {atomic_load(&timeval_calls), atomic_load(&timespec_calls),
            last_times[0], last_times[1], last_times[2], last_times[3]};
    jlongArray result = (*env)->NewLongArray(env, 6);
    if (result != NULL) (*env)->SetLongArrayRegion(env, result, 0, 6, values);
    return result;
}
JNIEXPORT jint JNICALL Java_AndroidFileTimestampContracts_errnoOf(
        JNIEnv *env, jclass cls, jthrowable error) {
    jclass type = (*env)->GetObjectClass(env, error);
    if (type == NULL) return -1;
    jfieldID field = (*env)->GetFieldID(env, type, "errno", "I");
    if (field == NULL) return -1;
    return (*env)->GetIntField(env, error, field);
}
JNIEXPORT jboolean JNICALL Java_AndroidFileTimestampContracts_rawStatFallback(
        JNIEnv *env, jclass cls) {
#ifdef FSTATAT64_SYSCALL_AVAILABLE
    return JNI_TRUE;
#else
    return JNI_FALSE;
#endif
}
'''


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ("before", "after", "headers-root", "java-home", "output"):
        parser.add_argument("--" + name, required=True, type=Path)
    parser.add_argument("--java-source-root", type=Path)
    parser.add_argument("--cc", default="cc")
    args = parser.parse_args()
    for name in ("before", "after", "headers_root", "java_home", "output"):
        setattr(args, name, getattr(args, name).resolve())
    java_source_root = (args.java_source_root or args.before).resolve()
    for root in (args.before, args.after, args.headers_root, java_source_root):
        if args.output.is_relative_to(root):
            raise ValueError("The receipt must be outside all source roots")
    source_inputs = [args.before / SOURCE, args.after / SOURCE,
                     java_source_root / JAVA_SOURCE, *[args.headers_root / p for p in HEADERS]]
    toolchain_inputs = [args.java_home / p for p in (
        "include/jni.h", "include/linux/jni_md.h", "lib/libjava.so", "lib/server/libjvm.so")]
    source_hashes = {str(p): digest(p) for p in source_inputs}
    toolchain_hashes = {str(p): digest(p) for p in toolchain_inputs}
    commands, artifacts, failures = [], {}, []

    def run(label: str, command: list, expected: str | None = None) -> str:
        completed = subprocess.run([str(x) for x in command], text=True,
                                   stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                   timeout=60)
        good = completed.returncode == 0 if expected is None else (
            completed.returncode != 0 and expected in completed.stdout)
        commands.append({"label": label, "command": [str(x) for x in command],
                         "exit_code": completed.returncode, "expected_failure": expected,
                         "passed": good, "output": completed.stdout})
        if not good:
            failures.append(label)
            raise RuntimeError(label + "\n" + completed.stdout)
        return completed.stdout

    try:
        run("compiler-identity", [args.cc, "--version"])
        run("java-identity", [args.java_home / "bin/java", "-version"])
        run("javac-identity", [args.java_home / "bin/javac", "-version"])
        with tempfile.TemporaryDirectory(prefix="android-fd-times-") as temporary:
            work = Path(temporary)
            generated = work / "headers"; generated.mkdir()
            classes = work / "classes"; classes.mkdir()
            # Use the actual unchanged Java declaration, not a manually written native header.
            run("generate-dispatcher-header", [args.java_home / "bin/javac",
                "--patch-module", "java.base=" + str(java_source_root / "src/java.base/unix/classes"),
                "-implicit:none", "-d", classes, "-h", generated, java_source_root / JAVA_SOURCE])
            java = work / "AndroidFileTimestampContracts.java"; java.write_text(JAVA)
            run("generate-test-header", [args.java_home / "bin/javac", "--release", "21",
                                         "-d", classes, "-h", generated, java])
            # Compare actual capability source blocks on non-Android/BSD branches.
            # This is a source-block compilation check, not a complete BSD SDK build.
            for profile, profile_flags in (("non-android", []), ("bsd", ["-D_ALLBSD_SOURCE"])):
                observed = []
                bodies = []
                for version, root in (("before", args.before), ("after", args.after)):
                    text = (root / SOURCE).read_text()
                    start = "    /* supports futimes or futimesat, futimens, and/or lutimes */"
                    stop = "    /* supports file birthtime */"
                    if text.count(start) != 1 or text.count(stop) != 1:
                        raise ValueError("capability source region is ambiguous")
                    block = text[text.index(start):text.index(stop)]
                    unit = work / (profile + "-" + version + "-capabilities.c")
                    unit.write_text("#include <stdio.h>\n#include <stdint.h>\n"
                        "#include <jni.h>\n#include \"sun_nio_fs_UnixNativeDispatcher.h\"\n"
                        "static int capabilities_for(unsigned mask) {\nint capabilities=0;\n"
                        + "\n".join("void *" + name + "=(void *)(uintptr_t)((mask >> "
                            + str(index) + ") & 1);" for index, name in enumerate((
                                "my_futimesat_func", "my_futimens_func", "my_openat64_func",
                                "my_fstatat64_func", "my_unlinkat_func", "my_renameat_func",
                                "my_fdopendir_func", "my_lutimes_func"))) + "\n" + block
                        + "return capabilities; }\nint main(void) {\n"
                        "for(unsigned mask=0; mask<256; mask++) printf(\"%d\\n\",capabilities_for(mask));\n"
                        "return 0; }\n")
                    executable = work / (profile + "-" + version + "-capabilities")
                    flags = [args.cc, "-std=c17", "-Wall", "-Wextra", "-Werror",
                             "-Wno-unused-variable", "-I" + str(generated),
                             "-I" + str(args.java_home / "include"),
                             "-I" + str(args.java_home / "include/linux")]
                    run(profile + "-" + version + "-capability-block-compile",
                        flags + profile_flags + [unit, "-o", executable])
                    observed.append(run(profile + "-" + version + "-capability-block-run", [executable]))
                    artifacts[unit.name] = digest(unit); artifacts[executable.name] = digest(executable)
                    if profile == "bsd":
                        start = "JNIEXPORT void JNICALL\nJava_sun_nio_fs_UnixNativeDispatcher_futimes0("
                        stop = "JNIEXPORT void JNICALL\nJava_sun_nio_fs_UnixNativeDispatcher_futimens0("
                        if text.count(start) != 1 or text.count(stop) != 1:
                            raise ValueError("timestamp source region is ambiguous")
                        body = work / (version + "-bsd-futimes-body.c")
                        body.write_text(text[text.index(start):text.index(stop)])
                        bodies.append(run(version + "-bsd-timestamp-preprocess",
                            [args.cc, "-std=c17", "-E", "-P", "-D_ALLBSD_SOURCE", body]))
                        artifacts[body.name] = digest(body)
                if observed[0] != observed[1]:
                    raise AssertionError(profile + " capability decisions changed")
                if bodies and bodies[0] != bodies[1]:
                    raise AssertionError("BSD timestamp body changed")
            common = [args.cc, "-std=c17", "-D_GNU_SOURCE", "-D_LARGEFILE64_SOURCE", "-fPIC",
                      "-Wall", "-Wextra", "-Werror", "-Wno-unused-parameter",
                      "-Wno-deprecated-declarations", "-fsanitize=undefined",
                      "-fno-sanitize-recover=all", "-g", "-O1",
                      "-I" + str(args.java_home / "include"),
                      "-I" + str(args.java_home / "include/linux"), "-I" + str(generated),
                      "-I" + str(args.headers_root / "src/java.base/share/native/libjava"),
                      "-I" + str(args.headers_root / "src/java.base/unix/native/libjava")]
            # Android profiles describe symbol sets only; all three execute on the host ABI.
            for name, flags, android in (
                    ("host-linux", [], False),
                    ("android-api21-model", ["-D__ANDROID__", "-D__ANDROID_API__=21", "-DCONTRACT_NO_RAW_STAT"], True),
                    ("android-api26-model", ["-D__ANDROID__", "-D__ANDROID_API__=26", "-DCONTRACT_NO_RAW_STAT"], True)):
                for version, root in (("before", args.before), ("after", args.after)):
                    prefix = name + "-" + version
                    unit = work / (prefix + ".c")
                    unit.write_text(NATIVE.replace("SOURCE_PATH", json.dumps(str(root / SOURCE))))
                    library = work / ("lib" + prefix + ".so")
                    run(prefix + "-whole-source-compile", common + flags + ["-shared", unit,
                        "-Wl,-Bsymbolic", "-Wl,-z,defs", "-L" + str(args.java_home / "lib"),
                        "-Wl,-rpath," + str(args.java_home / "lib"), "-ljava", "-ldl", "-o", library])
                    destination = work / (prefix + "-files"); destination.mkdir()
                    base = [args.java_home / "bin/java", "-Xcheck:jni", "-cp", classes,
                            "AndroidFileTimestampContracts", library]
                    enabled = android and version == "after"
                    run(prefix + "-contracts", base + ["suite", str(enabled).lower(), destination])
                    if android:
                        for mode, marker in (("capability-probe", "ANDROID_FUTIMENS_CAPABILITY_MISSING"),
                                             ("timestamp-probe", "ANDROID_FUTIMENS_TIMESTAMP_UNAVAILABLE")):
                            run(prefix + "-" + mode, base + [mode, "true", destination],
                                marker if version == "before" else None)
                    artifacts[library.name] = digest(library)
                    artifacts[unit.name] = digest(unit)
            for folder in (generated, classes):
                for path in sorted(folder.rglob("*")):
                    if path.is_file():
                        artifacts[str(path.relative_to(work))] = digest(path)
            artifacts[java.name] = digest(java)
    except Exception as failure:
        failures.append(str(failure))
    source_unchanged = all(digest(Path(path)) == value for path, value in source_hashes.items())
    report = {
        "status": "PASS" if not failures and source_unchanged else "FAIL",
        "scope": "Complete production C translation unit; real host JVM JNI and descriptor file operations; Android symbol availability models.",
        "limits": ["No Android NDK/ARM/device or full source-built JDK execution",
                   "Host time_t is 64-bit; no ILP32 time_t qualification",
                   "BSD capability source blocks compile/compare and its timestamp branch preprocesses identically; no complete Apple/BSD SDK build"],
        "source_sha256": source_hashes, "sources_unchanged": source_unchanged,
        "toolchain_sha256": toolchain_hashes, "artifact_sha256": artifacts,
        "commands": commands, "failures": failures,
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, indent=2) + "\n")
    print(json.dumps({"status": report["status"], "commands": len(commands),
                      "expected_baseline_failures": sum(x["expected_failure"] is not None for x in commands),
                      "artifacts": len(artifacts), "output": str(args.output)}))
    return 0 if report["status"] == "PASS" else 1


if __name__ == "__main__":
    raise SystemExit(main())
