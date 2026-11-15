#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors.
# SPDX-License-Identifier: GPL-2.0-only
"""Qualify the Apple launcher's source-derived static JNI linkage on Linux.

This checks the actual LoadJavaVM body and InvocationFunctions declaration with
the selected JDK's real JNI headers. An archive fixture demonstrates why string
lookups do not retain archive members. Three host JVM runs exercise invocation,
Java/native callbacks, exception handling, thread attachment and shutdown. This
is not an Apple SDK build, an iOS simulator/device run, or a source-built M3 VM.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile

SOURCE = Path('src/java.base/macosx/native/libjli/java_md_macosx.m')
HEADER = Path('src/java.base/share/native/libjli/java.h')
ENTRY_POINTS = ('JNI_CreateJavaVM', 'JNI_GetDefaultJavaVMInitArgs', 'JNI_GetCreatedJavaVMs')


def function(text, name):
    matches = list(re.finditer(r'(?m)^(?:static )?jboolean\n' + re.escape(name) + r'\([^;]+?\)\s*\{', text))
    if len(matches) != 1:
        raise ValueError('expected one function definition: ' + name)
    start = matches[0].start()
    at = matches[0].end() - 1
    depth, state = 0, 'code'
    while at < len(text):
        pair = text[at:at + 2]
        ch = text[at]
        if state == 'code':
            if pair == '/*':
                state = 'comment'; at += 2; continue
            if pair == '//':
                state = 'line'; at += 2; continue
            if ch in ('"', "'"):
                state = ch
            elif ch == '{':
                depth += 1
            elif ch == '}':
                depth -= 1
                if depth == 0:
                    return text[start:at + 1]
        elif state == 'comment' and pair == '*/':
            state = 'code'; at += 2; continue
        elif state == 'line' and ch == '\n':
            state = 'code'
        elif state in ('"', "'"):
            if ch == '\\':
                at += 2; continue
            if ch == state:
                state = 'code'
        at += 1
    raise ValueError('unterminated function: ' + name)


STUB = r'''
#include <jni.h>
#ifndef OMIT_CREATE
JNIEXPORT jint JNICALL JNI_CreateJavaVM(JavaVM **vm, void **env, void *args) {
    return vm == NULL && env == NULL && args == NULL ? 11 : -1;
}
#endif
#ifndef OMIT_DEFAULT
JNIEXPORT jint JNICALL JNI_GetDefaultJavaVMInitArgs(void *args) {
    return args == NULL ? 22 : -1;
}
#endif
#ifndef OMIT_CREATED
JNIEXPORT jint JNICALL JNI_GetCreatedJavaVMs(JavaVM **vms, jsize length, jsize *count) {
    if (vms != NULL || length != 0 || count == NULL) return -1;
    *count = 33;
    return JNI_OK;
}
#endif
'''

STUB_MAIN = r'''
#include <stdio.h>
int main(int argc, char **argv) {
    if (argc != 3) return 91;
    InvocationFunctions ifn = {0};
    jboolean loaded = LoadJavaVM(argv[1], &ifn);
    if (argv[2][0] == 'f') return loaded == JNI_FALSE ? 0 : 92;
    if (loaded != JNI_TRUE) return 93;
    jsize count = -1;
    if (ifn.CreateJavaVM(NULL, NULL, NULL) != 11) return 94;
    if (ifn.GetDefaultJavaVMInitArgs(NULL) != 22) return 95;
    if (ifn.GetCreatedJavaVMs(NULL, 0, &count) != JNI_OK || count != 33) return 96;
    puts("ARCHIVE_OR_DYNAMIC_SIGNATURES_PASS");
    return 0;
}
'''

REAL_MAIN = r'''
#include <pthread.h>
#include <stdio.h>
#include "StaticJniPayload.h"
static JavaVM *test_vm;
static jclass payload;
static jmethodID run_id;
JNIEXPORT jint JNICALL Java_StaticJniPayload_nativeRoundTrip(JNIEnv *env, jclass type, jint value) {
    JavaVM *found = NULL;
    (void)type;
    if ((*env)->GetJavaVM(env, &found) != JNI_OK || found != test_vm) return -1;
    return value + 1;
}
static void *worker(void *unused) {
    (void)unused;
    JNIEnv *env = NULL;
    if ((*test_vm)->GetEnv(test_vm, (void **)&env, JNI_VERSION_1_8) != JNI_EDETACHED) return (void *)1;
    if ((*test_vm)->AttachCurrentThread(test_vm, (void **)&env, NULL) != JNI_OK) return (void *)2;
    JNIEnv *confirmed = NULL;
    if ((*test_vm)->GetEnv(test_vm, (void **)&confirmed, JNI_VERSION_1_8) != JNI_OK || confirmed != env) return (void *)3;
    jint result = (*env)->CallStaticIntMethod(env, payload, run_id);
    if ((*env)->ExceptionCheck(env) || result != 42) return (void *)4;
    if ((*test_vm)->DetachCurrentThread(test_vm) != JNI_OK) return (void *)5;
    if ((*test_vm)->GetEnv(test_vm, (void **)&env, JNI_VERSION_1_8) != JNI_EDETACHED) return (void *)6;
    return NULL;
}
int main(int argc, char **argv) {
    if (argc != 3) return 70;
    InvocationFunctions ifn = {0};
    if (!LoadJavaVM(argv[1], &ifn)) return 71;
    JavaVMInitArgs args = {0};
    args.version = JNI_VERSION_1_8;
    if (ifn.GetDefaultJavaVMInitArgs(&args) != JNI_OK) return 72;
    JavaVMOption options[1] = {{argv[2], NULL}};
    args.nOptions = 1; args.options = options; args.ignoreUnrecognized = JNI_FALSE;
    jsize count = -1;
    if (ifn.GetCreatedJavaVMs(NULL, 0, &count) != JNI_OK || count != 0) return 73;
    JNIEnv *env = NULL;
    if (ifn.CreateJavaVM(&test_vm, (void **)&env, &args) != JNI_OK) return 74;
    JavaVM *found = NULL;
    if (ifn.GetCreatedJavaVMs(&found, 1, &count) != JNI_OK || count != 1 || found != test_vm) return 75;
    jclass local = (*env)->FindClass(env, "StaticJniPayload");
    if (local == NULL || (*env)->ExceptionCheck(env)) return 76;
    payload = (*env)->NewGlobalRef(env, local);
    if (payload == NULL) return 77;
    JNINativeMethod methods[1] = {{"nativeRoundTrip", "(I)I", (void *)Java_StaticJniPayload_nativeRoundTrip}};
    if ((*env)->RegisterNatives(env, payload, methods, 1) != JNI_OK) return 78;
    run_id = (*env)->GetStaticMethodID(env, payload, "run", "()I");
    if (run_id == NULL || (*env)->ExceptionCheck(env)) return 79;
    if ((*env)->CallStaticIntMethod(env, payload, run_id) != 42 || (*env)->ExceptionCheck(env)) return 80;
    if ((*env)->FindClass(env, "missing/LauncherRegressionClass") != NULL || !(*env)->ExceptionCheck(env)) return 81;
    (*env)->ExceptionClear(env);
    pthread_t thread;
    void *outcome = NULL;
    if (pthread_create(&thread, NULL, worker, NULL) != 0) return 82;
    if (pthread_join(thread, &outcome) != 0 || outcome != NULL) return 83;
    (*env)->DeleteGlobalRef(env, payload);
    (*env)->DeleteLocalRef(env, local);
    if ((*test_vm)->DestroyJavaVM(test_vm) != JNI_OK) return 84;
    if (ifn.GetCreatedJavaVMs(NULL, 0, &count) != JNI_OK || count != 0) return 85;
    puts("HOST_JNI_INVOCATION_PASS callback=42 attached-thread=42 destroyed-vms=0");
    return 0;
}
'''

PATH_PREFIX = r'''
#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#ifndef MAXPATHLEN
#define MAXPATHLEN 64
#endif
#define JAVA_DLL "libjava.dylib"
#define JRE_ERROR8 "missing runtime: "
#define JLI_Snprintf snprintf
#define JLI_StrLen strlen
#define JLI_StrCat strcat
static void JLI_TraceLauncher(const char *fmt, ...) { (void)fmt; }
static void JLI_ReportErrorMessage(const char *fmt, ...) { (void)fmt; }
typedef struct { const char *dli_fname; } Dl_info;
static const char *fixture_filename;
static const char *fixture_home;
static const char *fixture_resolved;
static const char *fixture_existing;
static int fixture_dladdr_success = 1;
static int fixture_access_count;
static int fixture_access_any;
static int fixture_access_second;
static int fixture_realpath_allocations;
static jboolean GetApplicationHome(char *out, jint capacity) {
    if (fixture_home == NULL) return JNI_FALSE;
    size_t length = strlen(fixture_home);
    if (capacity <= 0 || length >= (size_t)capacity) return JNI_FALSE;
    memcpy(out, fixture_home, length + 1);
    return JNI_TRUE;
}
static int fixture_dladdr(const void *address, Dl_info *info) {
    (void)address;
    info->dli_fname = fixture_filename;
    return fixture_dladdr_success;
}
static int fixture_access(const char *path, int mode) {
    (void)mode;
    fixture_access_count++;
    return fixture_access_any || (fixture_access_second && fixture_access_count == 2) ||
        (fixture_existing != NULL && strcmp(path, fixture_existing) == 0) ? 0 : -1;
}
static char *fixture_realpath(const char *source, char *destination) {
    (void)source;
    if (fixture_resolved == NULL) return NULL;
    size_t length = strlen(fixture_resolved) + 1;
    if (destination == NULL) {
        destination = malloc(length);
        fixture_realpath_allocations++;
    }
    if (destination != NULL) memcpy(destination, fixture_resolved, length);
    return destination;
}
static void fixture_free(void *memory) {
    if (memory != NULL) fixture_realpath_allocations--;
    free(memory);
}
#define dladdr fixture_dladdr
#define access fixture_access
#define realpath fixture_realpath
#define free fixture_free
'''

PATH_MAIN = r'''
#undef free
static void make_home(char *home, size_t length) {
    memset(home, 'a', length); home[0] = '/'; home[length] = '\0';
}
int main(int argc, char **argv) {
    if (argc != 2) return 30;
    int scenario = atoi(argv[1]);
    char home[MAXPATHLEN * 2], executable[MAXPATHLEN * 2];
    char expected_cfg[MAXPATHLEN * 2];
    char short_resolved[sizeof("/MacOS/libjli.dylib")];
    unsigned char buffer[MAXPATHLEN * 2 + 32];
    memset(buffer, 0x5a, sizeof(buffer));
    char *out = (char *)buffer + 8;
    jint capacity = MAXPATHLEN;
    jboolean expected = JNI_FALSE;
    const char *expected_path = NULL;
    fixture_filename = "/app/bin/java";
    fixture_existing = "/app/lib/jvm.cfg";
    switch (scenario) {
    case 0: expected = JNI_TRUE; expected_path = "/app"; break;
    case 1: fixture_filename = "/app/bin/./java"; expected = JNI_TRUE; expected_path = "/app"; break;
    case 2:
    case 3: {
        size_t length = MAXPATHLEN - sizeof("/lib/jvm.cfg") + (scenario == 3);
        make_home(home, length);
        snprintf(executable, sizeof(executable), "%s/bin/java", home);
        snprintf(expected_cfg, sizeof(expected_cfg), "%s/lib/jvm.cfg", home);
        fixture_filename = executable; fixture_existing = expected_cfg;
        if (scenario == 2) { expected = JNI_TRUE; expected_path = home; }
        break;
    }
    case 4: capacity = 4; break;
    case 5: fixture_dladdr_success = 0; fixture_filename = NULL; break;
    case 6: fixture_filename = NULL; break;
    case 7: fixture_filename = "java"; break;
    case 8: fixture_filename = "/java"; break;
    case 9: fixture_filename = "/./java"; break;
    case 10:
        memset(executable, 'a', MAXPATHLEN); executable[MAXPATHLEN] = '\0';
        fixture_filename = executable; break;
    case 11: capacity = 0; break;
    case 12: capacity = -1; break;
    case 13: out = NULL; break;
    case 14:
        fixture_existing = NULL; fixture_resolved = "/alternative/lib/libjli.dylib";
        expected = JNI_TRUE; expected_path = "/alternative/"; break;
    case 15:
        fixture_existing = NULL; fixture_resolved = "/alternative/lib/libjli.dylib";
        capacity = 8; break;
    case 16: fixture_home = "/app"; expected = JNI_TRUE; expected_path = "/app"; break;
    case 17:
        make_home(home, MAXPATHLEN - sizeof("/lib/jvm.cfg") + 1);
        fixture_home = home; fixture_access_any = 1; capacity = MAXPATHLEN * 2; break;
    case 18:
        make_home(home, MAXPATHLEN - 18);
        fixture_home = home; fixture_existing = NULL; fixture_access_second = 1;
        capacity = MAXPATHLEN * 2; break;
    case 19: capacity = 5; expected = JNI_TRUE; expected_path = "/app"; break;
    case 20: fixture_existing = NULL; break;
    case 21:
        fixture_existing = NULL; fixture_resolved = "/alternative/lib/libjli.dylib";
        capacity = (jint)strlen(fixture_resolved) + 1;
        expected = JNI_TRUE; expected_path = "/alternative/"; break;
    case 22:
        fixture_existing = NULL; fixture_resolved = "/alternative/lib/libjli.dylib";
        capacity = (jint)strlen(fixture_resolved); break;
    case 23:
    case 24: {
        size_t length = sizeof("/lib/libjli.dylib") - 1 + (scenario == 24);
        memset(short_resolved, 'a', length); short_resolved[length] = '\0';
        fixture_existing = NULL; fixture_resolved = short_resolved; break;
    }
    case 25:
        fixture_existing = "/alternative/Home";
        fixture_resolved = "/alternative/MacOS/libjli.dylib";
        expected = JNI_TRUE; expected_path = "/alternative/Home"; break;
    default: return 31;
    }
    /* Keep short resolved paths at an ASan allocation boundary so suffix
     * underflow cannot hide inside the ordinary destination canary padding. */
    char *short_allocation = NULL;
    if (scenario == 23 || scenario == 24) {
        short_allocation = malloc((size_t)capacity);
        if (short_allocation == NULL) return 38;
        memset(short_allocation, 0x5a, (size_t)capacity);
        out = short_allocation;
    }
    jboolean result = GetJREPath(out, capacity, JNI_TRUE);
    if (fixture_realpath_allocations != 0) return 36;
    if (result != expected) return 32;
    if (expected == JNI_TRUE && strcmp(out, expected_path) != 0) return 33;
    for (size_t i = 0; i < 8; i++) if (buffer[i] != 0x5a) return 34;
    if (capacity >= 0) {
        for (size_t i = 8 + (size_t)capacity; i < sizeof(buffer); i++) if (buffer[i] != 0x5a) return 35;
    }
    free(short_allocation);
    printf("STATIC_PATH_PASS case=%d capacity=%d limit=%d\n", scenario, capacity, MAXPATHLEN);
    return 0;
}
'''


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--before', type=Path, required=True)
    p.add_argument('--after', type=Path, required=True)
    p.add_argument('--headers', type=Path, help='pinned unchanged JDK source headers; defaults to before')
    p.add_argument('--java-home', type=Path, required=True)
    p.add_argument('--output', type=Path, required=True)
    a = p.parse_args()
    a.before, a.after, a.java_home, a.output = (v.resolve() for v in (a.before, a.after, a.java_home, a.output))
    a.headers = (a.headers or a.before).resolve()
    if a.output.exists():
        p.error('output already exists')
    if a.output.is_relative_to(a.before) or a.output.is_relative_to(a.after):
        p.error('output must be outside both source trees')
    cc, cxx, ar, nm = (shutil.which(n) for n in ('cc', 'c++', 'ar', 'nm'))
    if not all((cc, cxx, ar, nm)):
        p.error('cc, c++, ar and nm are required')
    header = (a.headers / HEADER).read_text()
    start = header.index('typedef jint (JNICALL *CreateJavaVM_t)')
    end = header.index('} InvocationFunctions;', start) + len('} InvocationFunctions;')
    declarations = header[start:end]
    report = {'schema': 1, 'scope': 'source-derived Linux compiler/archive/JNI host qualification',
              'not_qualified': ['Apple SDK compilation/linking', 'iOS device/simulator', 'M3 source-built runtime',
                                'Mach-O dead stripping', 'complete mobile build configuration'],
              'sanitizers': {'address': True, 'undefined': True, 'leak': False,
                  'reason': 'LeakSanitizer is unavailable under executor ptrace; fallback allocations are counted explicitly'},
              'sources': {}, 'commands': [], 'checks': []}
    for label, path in [('before', a.before / SOURCE), ('after', a.after / SOURCE), ('declarations', a.headers / HEADER)]:
        report['sources'][label] = {'path': str(path), 'sha256': hashlib.sha256(path.read_bytes()).hexdigest()}

    def run(label, cmd, expected=0):
        child_env = dict(os.environ)
        child_env['ASAN_OPTIONS'] = 'detect_leaks=0:halt_on_error=1:exitcode=99'
        child_env['UBSAN_OPTIONS'] = 'halt_on_error=1:exitcode=98'
        completed = subprocess.run([str(c) for c in cmd], capture_output=True, text=True, timeout=90, env=child_env)
        report['commands'].append({'label': label, 'argv': [str(c) for c in cmd], 'returncode': completed.returncode,
                                   'stdout': completed.stdout, 'stderr': completed.stderr, 'expected': expected})
        if (completed.returncode == 0 if expected == 'nonzero' else completed.returncode != expected):
            raise RuntimeError(label + ': unexpected exit ' + str(completed.returncode) + '\n' + completed.stderr)
        return completed.stdout

    prefix = '#include <jni.h>\n#include <dlfcn.h>\n#include <stddef.h>\n' + declarations + r'''
/* RTLD_FIRST is Darwin-specific. The host archive fixture uses RTLD_NOW solely
 * to exercise the symbol-retention failure in the old static branch. */
#ifndef RTLD_FIRST
#define RTLD_FIRST RTLD_NOW
#endif
#define DLL_ERROR1 "loader error at %d"
#define DLL_ERROR2 "%s: %s"
static void JLI_TraceLauncher(const char *fmt, ...) { (void)fmt; }
static void JLI_ReportErrorMessage(const char *fmt, ...) { (void)fmt; }
'''
    includes = ['-I' + str(a.java_home / 'include'), '-I' + str(a.java_home / 'include/linux')]
    compile_flags = ['-std=c17', '-O2', '-Wall', '-Wextra', '-Werror=incompatible-pointer-types',
                     '-Werror=implicit-function-declaration', '-Werror=return-type', '-ffunction-sections', '-fdata-sections']
    try:
        for label, command in [('cc', [cc, '--version']), ('cxx', [cxx, '--version']), ('ar', [ar, '--version']), ('nm', [nm, '--version']),
                               ('java', [a.java_home / 'bin/java', '-version']), ('javac', [a.java_home / 'bin/javac', '-version'])]:
            run('tool-identity-' + label, command)
        report['jdk_release_sha256'] = hashlib.sha256((a.java_home / 'release').read_bytes()).hexdigest()
        with tempfile.TemporaryDirectory(prefix='static-jni-launcher-') as scratch:
            tmp = Path(scratch)
            stub = tmp / 'invocation-stub.c'; stub.write_text(STUB)
            run('archive-compile', [cc, *compile_flags, *includes, '-c', stub, '-o', tmp / 'stub.o'])
            run('archive-create', [ar, 'rcs', tmp / 'libinvocation.a', tmp / 'stub.o'])
            shared = {}
            for omission in ('NONE', 'CREATE', 'DEFAULT', 'CREATED'):
                dest = tmp / ('libinvocation-' + omission + '.so')
                flags = [] if omission == 'NONE' else ['-DOMIT_' + omission]
                run('dynamic-fixture-' + omission, [cc, *compile_flags, *includes, *flags, '-fPIC', '-shared', stub, '-o', dest])
                shared[omission] = dest
            java = tmp / 'StaticJniPayload.java'
            java.write_text('public class StaticJniPayload { private static native int nativeRoundTrip(int value); public static int run() { return nativeRoundTrip(41); } }\n')
            run('javac-payload', [a.java_home / 'bin/javac', '--release', '21', '-h', tmp, '-d', tmp, java])
            for label, root in [('before', a.before), ('after', a.after)]:
                body = function((root / SOURCE).read_text(), 'LoadJavaVM')
                report['sources'][label]['function_sha256'] = hashlib.sha256(body.encode()).hexdigest()
                unit = tmp / (label + '-loader.c'); unit.write_text(prefix + body + '\n' + STUB_MAIN)
                obj = tmp / (label + '-static.o')
                run(label + '-static-compile', [cc, *compile_flags, *includes, '-DSTATIC_BUILD', '-c', unit, '-o', obj])
                symbols = run(label + '-undefined-symbols', [nm, '-u', obj])
                has_refs = all(re.search(r'\b' + name + r'\s*$', symbols, re.M) for name in ENTRY_POINTS)
                if has_refs != (label == 'after'):
                    raise RuntimeError(label + ': unexpected invocation references')
                report['checks'].append({'name': label + '-direct-invocation-references', 'pass': True, 'present': has_refs})
                cpp_obj = tmp / (label + '-static-cpp.o')
                run(label + '-static-cpp-compile', [cxx, '-x', 'c++', '-std=c++20', '-O2', '-Wall', '-Wextra',
                    '-Werror=return-type', *includes, '-DSTATIC_BUILD', '-c', unit, '-o', cpp_obj])
                cpp_symbols = run(label + '-cpp-undefined-symbols', [nm, '-u', cpp_obj])
                cpp_refs = all(re.search(r'\b' + name + r'\s*$', cpp_symbols, re.M) for name in ENTRY_POINTS)
                if cpp_refs != (label == 'after'):
                    raise RuntimeError(label + ': unexpected C++ invocation references')
                report['checks'].append({'name': label + '-cpp-invocation-types', 'pass': True})
                exe = tmp / (label + '-archive')
                run(label + '-archive-link', [cc, obj, tmp / 'libinvocation.a', '-Wl,--gc-sections', '-ldl', '-o', exe])
                linked_symbols = run(label + '-archive-symbols', [nm, exe])
                linked = all(re.search(r'\b' + name + r'\s*$', linked_symbols, re.M) for name in ENTRY_POINTS)
                if linked != (label == 'after'):
                    raise RuntimeError(label + ': unexpected linked archive entry points')
                expected = 93 if label == 'before' else 0
                run(label + '-archive-run', [exe, 'unused-static-library-path', 'success'], expected)
                report['checks'].append({'name': label + '-archive-retention', 'pass': True, 'expected_failure': label == 'before'})
                dyn = tmp / (label + '-dynamic')
                run(label + '-dynamic-compile', [cc, *compile_flags, *includes, unit, '-ldl', '-o', dyn])
                for omission, lib in shared.items():
                    run(label + '-dynamic-' + omission, [dyn, lib, 'success' if omission == 'NONE' else 'failure'])
                run(label + '-dynamic-missing-library', [dyn, tmp / 'does-not-exist.so', 'failure'])
                report['checks'].append({'name': label + '-dynamic-resolution', 'pass': True, 'cases': 5})
                modes = ('dynamic',) if label == 'before' else ('static', 'dynamic')
                real_unit = tmp / (label + '-real.c'); real_unit.write_text(prefix + body + '\n' + REAL_MAIN)
                for mode in modes:
                    exe = tmp / (label + '-real-' + mode)
                    flags = [] if mode == 'dynamic' else ['-DSTATIC_BUILD', '-L' + str(a.java_home / 'lib/server'),
                                                          '-Wl,-rpath,' + str(a.java_home / 'lib/server')]
                    libs = ['-ldl', '-pthread'] + (['-ljvm'] if mode == 'static' else [])
                    run(label + '-real-' + mode + '-compile', [cc, *compile_flags, *includes, *flags, real_unit, *libs, '-o', exe])
                    run(label + '-real-' + mode + '-run', [exe, a.java_home / 'lib/server/libjvm.so', '-Djava.class.path=' + str(tmp)])
                    report['checks'].append({'name': label + '-host-jvm-' + mode, 'pass': True})
                path_body = function((root / SOURCE).read_text(), 'GetJREPath')
                path_unit = tmp / (label + '-path.c'); path_unit.write_text(PATH_PREFIX + path_body + '\n' + PATH_MAIN)
                for limit in (64, 4096):
                    path_exe = tmp / (label + '-path-' + str(limit))
                    run(label + '-path-compile-' + str(limit), [cc, '-std=gnu17', '-D_GNU_SOURCE', '-g', '-O1',
                        '-DSTATIC_BUILD', '-DMAXPATHLEN=' + str(limit), '-fsanitize=address,undefined',
                        '-fno-omit-frame-pointer', *includes, path_unit, '-o', path_exe])
                    scenarios = range(26) if label == 'after' else (0, 1, 2, 19)
                    for scenario in scenarios:
                        run(label + '-path-' + str(limit) + '-' + str(scenario), [path_exe, scenario])
                    if label == 'before':
                        for scenario in (3, 5, 10):
                            run(label + '-path-baseline-defect-' + str(limit) + '-' + str(scenario), [path_exe, scenario], 'nonzero')
                            failure = report['commands'][-1]['stderr']
                            diagnostic = 'null pointer passed as argument' if scenario == 5 else 'AddressSanitizer: stack-buffer-overflow'
                            if diagnostic not in failure:
                                raise RuntimeError('baseline failure lacks the expected sanitizer diagnostic: ' + diagnostic)
                    report['checks'].append({'name': label + '-static-path-' + str(limit), 'pass': True,
                                            'cases': len(scenarios), 'expected_sanitizer_failures': 3 if label == 'before' else 0})
                    dynamic_path_exe = tmp / (label + '-dynamic-path-' + str(limit))
                    run(label + '-dynamic-path-compile-' + str(limit), [cc, '-std=gnu17', '-D_GNU_SOURCE', '-g', '-O1',
                        '-DMAXPATHLEN=' + str(limit), '-fsanitize=address,undefined', '-fno-omit-frame-pointer',
                        *includes, path_unit, '-o', dynamic_path_exe])
                    for scenario in (14, 23, 24, 25):
                        expected = 'nonzero' if label == 'before' and scenario in (23, 24) else 0
                        run(label + '-dynamic-path-' + str(limit) + '-' + str(scenario), [dynamic_path_exe, scenario], expected)
                        if expected == 'nonzero' and 'AddressSanitizer: heap-buffer-overflow' not in report['commands'][-1]['stderr']:
                            raise RuntimeError('dynamic baseline failure lacks the expected heap-underread diagnostic')
                    report['checks'].append({'name': label + '-dynamic-path-' + str(limit), 'pass': True,
                                            'cases': 4, 'expected_sanitizer_failures': 2 if label == 'before' else 0})
            # Compile the complete iOS-preprocessed source with pinned JDK source
            # declarations and Linux platform headers; this is not an Apple SDK TU.
            header_dirs = ['src/java.base/share/native/libjli', 'src/java.base/unix/native/libjli',
                           'src/hotspot/share/include', 'src/hotspot/os/posix/include']
            source_includes = ['-I' + str(a.headers / d) for d in header_dirs]
            run('after-ios-host-source-compile', [cc, '-x', 'c', '-std=gnu17', '-O2', '-D_GNU_SOURCE', '-D__IOS__',
                '-DSTATIC_BUILD', '-Werror=incompatible-pointer-types', '-Werror=implicit-function-declaration',
                '-Werror=return-type', *includes, *source_includes, '-c', a.after / SOURCE, '-o', tmp / 'ios-source.o'])
            report['checks'].append({'name': 'after-ios-host-source-compile', 'pass': True})
            report['artifacts'] = {str(path.relative_to(tmp)): {'bytes': path.stat().st_size,
                    'sha256': hashlib.sha256(path.read_bytes()).hexdigest()}
                for path in sorted(tmp.rglob('*')) if path.is_file()}
        report['status'] = 'PASS'
    except Exception as failure:
        report['status'] = 'FAIL'
        report['failure'] = str(failure)
    a.output.parent.mkdir(parents=True, exist_ok=True)
    a.output.write_text(json.dumps(report, indent=2) + '\n')
    print(json.dumps({'status': report['status'], 'checks': len(report['checks']), 'commands': len(report['commands']),
                      'output': str(a.output), 'failure': report.get('failure')}))
    raise SystemExit(0 if report['status'] == 'PASS' else 1)


if __name__ == '__main__':
    main()
