#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors.
# SPDX-License-Identifier: GPL-2.0-only
"""Compile and run selected Android native contracts against explicit JDK sources.

This is a host compiler/JNI regression, not an Android NDK or device test.
The complete jni_util_md.c translation unit uses real JDK headers; Bionic's
strerror declarations and libc behavior are explicitly modeled. IPv6 tests
compile the exact source-derived IPv6_supported block with syscall models.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile

JNI_PATH = Path('src/java.base/unix/native/libjava/jni_util_md.c')
NET_PATH = Path('src/java.base/unix/native/libnet/net_util_md.c')
HEADERS = {
    'jvm.h': 'src/hotspot/share/include/jvm.h',
    'jvm_md.h': 'src/hotspot/os/posix/include/jvm_md.h',
    'jvm_constants.h': 'src/hotspot/share/include/jvm_constants.h',
    'jvm_io.h': 'src/hotspot/share/include/jvm_io.h',
    'jni_util.h': 'src/java.base/share/native/libjava/jni_util.h',
    'jlong.h': 'src/java.base/share/native/libjava/jlong.h',
    'jlong_md.h': 'src/java.base/unix/native/libjava/jlong_md.h',
}

BIONIC_HEADER = r'''
#ifndef PROBE_BIONIC_STRING_H
#define PROBE_BIONIC_STRING_H
/* Preserve host declarations except its incompatible strerror_r name. */
#define strerror_r probe_unused_host_strerror_r
#include_next <string.h>
#undef strerror_r
#ifdef __cplusplus
extern "C" {
#endif
/* From Bionic string.h declaration contract, API 23 GNU switch. */
#if defined(__USE_GNU) && __ANDROID_API__ >= 23
char *strerror_r(int, char *, size_t) __asm__("__gnu_strerror_r");
#else
int strerror_r(int, char *, size_t);
#endif
#ifdef __cplusplus
}
#endif
#endif
'''

MODEL = r'''
#include <errno.h>
#include <stddef.h>
#include <stdio.h>
#include <string.h>
/* Bionic contract model: POSIX returns status, GNU drops it and returns buf.
 * This independently written fixture is not the Bionic libc implementation. */
int probe_bionic_posix(int error, char *buf, size_t len) __asm__("strerror_r");
int probe_bionic_posix(int error, char *buf, size_t len) {
    int saved_errno = errno;
    const char *message = NULL;
    char unknown[64];
    if (error == EINVAL) message = "Invalid argument";
    if (error == ENOENT) message = "No such file or directory";
    if (error == ERANGE) message = "Numerical result out of range";
    if (!message) {
        snprintf(unknown, sizeof(unknown), "Unknown error %d", error);
        message = unknown;
    }
    size_t length = strlen(message);
    if (len) {
        size_t copy = length < len - 1 ? length : len - 1;
        memcpy(buf, message, copy);
        buf[copy] = 0;
    }
    errno = saved_errno;
    return length >= len ? ERANGE : 0;
}
char *__gnu_strerror_r(int error, char *buf, size_t len) {
    probe_bionic_posix(error, buf, len);
    return buf;
}
'''

JNI_PROBE = r'''
#include <errno.h>
#include <limits.h>
#include <stdio.h>
#include <string.h>
#include "jni_util.h"
#include "AndroidNativeContracts.h"

/* ASCII-only test seam; exercises real JNIEnv/NewStringUTF, not the full
 * platform charset conversion implementation owned by libjava. */
JNIEXPORT jstring JNICALL JNU_NewStringPlatform(JNIEnv *env, const char *str) {
    return (*env)->NewStringUTF(env, str);
}

JNIEXPORT jint JNICALL
Java_AndroidNativeContracts_check(JNIEnv *env, jclass type, jboolean model) {
    (void) env; (void) type;
    int (JNICALL *abi)(int, char *, size_t) = getErrorString;
    int assertions = 0;
    const int errors[] = {0, ENOENT, EINVAL, ERANGE, -1, INT_MAX};
    const size_t capacities[] = {0, 1, 2, 4, 16, 32, 256};
    for (size_t i = 0; i < sizeof(errors)/sizeof(errors[0]); ++i) {
        for (size_t j = 0; j < sizeof(capacities)/sizeof(capacities[0]); ++j) {
            struct { unsigned char left[16]; char text[256]; unsigned char right[16]; } b;
            memset(&b, 0xa5, sizeof(b));
            int error = errors[i];
            size_t cap = capacities[j];
            errno = EBUSY;
            int status = abi(error, b.text, cap);
            if (errno != EBUSY) return -1;
            for (size_t k = 0; k < 16; ++k)
                if (b.left[k] != 0xa5 || b.right[k] != 0xa5) return -2;
            for (size_t k = cap; k < sizeof(b.text); ++k)
                if ((unsigned char)b.text[k] != 0xa5) return -3;
            if (error == 0 || cap == 0) {
                if (status != 0) return -4;
                for (size_t k = 0; k < sizeof(b.text); ++k)
                    if ((unsigned char)b.text[k] != 0xa5) return -5;
            } else {
                const char *expected = NULL;
                char unknown[64];
                if (error == ENOENT) expected = "No such file or directory";
                if (error == EINVAL) expected = "Invalid argument";
                if (error == ERANGE) expected = "Numerical result out of range";
                if (!expected) {
                    snprintf(unknown, sizeof(unknown), "Unknown error %d", error);
                    expected = unknown;
                }
                size_t n = strlen(expected);
                size_t copy = n < cap - 1 ? n : cap - 1;
                if (memcmp(b.text, expected, copy) || b.text[copy] != 0) return -6;
                int expected_status = n >= cap ? ERANGE : 0;
                /* glibc POSIX uses EINVAL for an unknown error, including
                 * when the printable fallback is truncated. */
                if (!model && (error == -1 || error == INT_MAX)) expected_status = EINVAL;
                if (status != expected_status) return -7;
            }
            ++assertions;
        }
    }
    char symbol[128];
    buildJniFunctionName("Java_probe_check", NULL, symbol);
    if (strcmp(symbol, "Java_probe_check")) return -8;
    buildJniFunctionName("JNI_OnLoad", "probe", symbol);
    if (strcmp(symbol, "JNI_OnLoad_probe")) return -9;
    void *one = getProcessHandle();
    if (!one || one != getProcessHandle()) return -10;
    return assertions + 3;
}

JNIEXPORT jstring JNICALL
Java_AndroidNativeContracts_lastError(JNIEnv *env, jclass type, jint error) {
    (void) type;
    errno = error;
    return getLastErrorString(env);
}
'''

JAVA = r'''
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public final class AndroidNativeContracts {
    private static native int check(boolean model);
    private static native String lastError(int error);
    public static void main(String[] args) throws Exception {
        System.load(args[0]);
        boolean model = Boolean.parseBoolean(args[1]);
        if (lastError(0) != null) throw new AssertionError("zero errno");
        String message = lastError(2);
        if (!"No such file or directory".equals(message))
            throw new AssertionError("JNI message: " + message);
        // Initialize the unchanged process-handle cache before worker threads.
        if (check(model) != 45) throw new AssertionError("warmup contract");
        AtomicReference<Throwable> failure = new AtomicReference<>();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 4; ++i) {
            Thread thread = new Thread(() -> {
                try {
                    for (int j = 0; j < 40; ++j) {
                        int actual = check(model);
                        if (actual != 45) throw new AssertionError("native contract " + actual);
                        if (!"Invalid argument".equals(lastError(22)))
                            throw new AssertionError("thread-local errno/string");
                    }
                } catch (Throwable error) { failure.compareAndSet(null, error); }
            });
            threads.add(thread);
            thread.start();
        }
        for (Thread thread : threads) thread.join();
        if (failure.get() != null) throw new AssertionError(failure.get());
        System.out.println("PASS: 7245 native cases, 162 JNI string/null checks");
    }
}
'''

IPV6_PREFIX = r'''
#include <stdio.h>
#include <stdint.h>
#include <string.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <dlfcn.h>
#include "jni.h"
typedef union {struct sockaddr sa; struct sockaddr_in sa4; struct sockaddr_in6 sa6;} SOCKETADDRESS;
static int fake_fd, fake_proc, fake_symbol;
static int socket_calls, close_calls, fopen_calls, fgets_calls, fclose_calls, symbol_calls;
static int probe_socket(int family, int type, int protocol) {
    if (family != AF_INET6 || type != SOCK_STREAM || protocol != 0) return -2;
    ++socket_calls; return fake_fd;
}
static int probe_close(int fd) { if (fd != fake_fd) return -1; ++close_calls; return 0; }
static FILE *probe_fopen(const char *path, const char *mode) {
    ++fopen_calls;
    if (strcmp(path, "/proc/net/if_inet6") || strcmp(mode, "r")) return NULL;
    return fake_proc < 0 ? NULL : (FILE *)(uintptr_t)1;
}
static char *probe_fgets(char *buf, int len, FILE *file) {
    (void)len; (void)file; ++fgets_calls;
    if (!fake_proc) return NULL;
    strcpy(buf, "::1"); return buf;
}
static int probe_fclose(FILE *file) {(void)file; ++fclose_calls; return 0;}
static void *JVM_FindLibraryEntry(void *handle, const char *symbol) {
    (void)handle; ++symbol_calls;
    return fake_symbol && !strcmp(symbol, "inet_pton") ? (void *)(uintptr_t)1 : NULL;
}
#define socket probe_socket
#define close probe_close
#define fopen probe_fopen
#define fgets probe_fgets
#define fclose probe_fclose
'''

IPV6_SUFFIX = r'''
int main(void) {
    const int fds[] = {-1, 0, 7};
    int failures = 0, cases = 0;
    for (int i = 0; i < 3; ++i) for (int p = -1; p <= 1; ++p) for (int s = 0; s <= 1; ++s) {
        fake_fd=fds[i]; fake_proc=p; fake_symbol=s;
        socket_calls=close_calls=fopen_calls=fgets_calls=fclose_calls=symbol_calls=0;
        int actual=IPv6_supported();
        int enabled=1, android=0;
#ifdef DONT_ENABLE_IPV6
        enabled=0;
#endif
#ifdef __ANDROID__
        android=1;
#endif
        int open_ok=enabled && fake_fd >= 0;
        int use_proc=open_ok && !android;
        int proc_ok=android || p > 0;
        int expected=open_ok && proc_ok && s;
        int bad=actual != expected || socket_calls != enabled || close_calls != open_ok;
        bad |= fopen_calls != use_proc;
        bad |= fgets_calls != (use_proc && p >= 0);
        bad |= fclose_calls != (use_proc && p >= 0);
        bad |= symbol_calls != (open_ok && proc_ok);
        ++cases;
        if (bad) {
            ++failures;
            printf("FAIL fd=%d proc=%d symbol=%d actual=%d expected=%d calls=%d,%d,%d,%d,%d,%d\n",
                fake_fd,p,s,actual,expected,socket_calls,close_calls,fopen_calls,fgets_calls,fclose_calls,symbol_calls);
        }
    }
    printf("IPv6 cases=%d failures=%d\n",cases,failures);
    return failures ? 1 : 0;
}
'''

def sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--before', type=Path, required=True)
    parser.add_argument('--after', type=Path, required=True)
    parser.add_argument('--headers-root', type=Path, required=True,
                        help='Full JDK source root or directory of exact named JDK headers')
    parser.add_argument('--java-home', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--cc', default='gcc')
    parser.add_argument('--cxx', default='g++')
    args = parser.parse_args()
    if not sys.platform.startswith('linux'):
        parser.error('this host regression requires Linux, glibc and GNU-compatible linker wrapping')
    args.before=args.before.resolve(); args.after=args.after.resolve()
    args.headers_root=args.headers_root.resolve(); args.java_home=args.java_home.resolve()
    args.output=args.output.resolve()
    if args.output.exists(): parser.error('refusing to overwrite output')
    if any(args.output.is_relative_to(p) for p in (args.before,args.after)):
        parser.error('output must be outside source roots')
    results=[]
    identities={}
    for state, root in [('before',args.before),('after',args.after)]:
        identities[state]={str(p):sha256(root/p) for p in (JNI_PATH,NET_PATH)}
    env=dict(os.environ, LC_ALL='C')
    toolchain_header_seals={str(p):sha256(args.java_home/p) for p in (
        Path('include/jni.h'),Path('include/linux/jni_md.h'),
        Path('include/classfile_constants.h'))}
    with tempfile.TemporaryDirectory(prefix='android-native-contracts-') as tmp:
        work=Path(tmp); include=work/'include'; include.mkdir()
        header_seals={}
        for name, rel in HEADERS.items():
            src=args.headers_root/rel
            if not src.exists(): src=args.headers_root/name
            data=src.read_bytes(); (include/name).write_bytes(data)
            header_seals[name]=hashlib.sha256(data).hexdigest()
        (work/'AndroidNativeContracts.java').write_text(JAVA)
        (work/'jni_probe.c').write_text(JNI_PROBE)
        (work/'model.c').write_text(MODEL)
        model_include=work/'bionic'; model_include.mkdir()
        (model_include/'string.h').write_text(BIONIC_HEADER)
        def run(name, command, expected_success=True):
            process=subprocess.run([str(x) for x in command],cwd=work,env=env,
                                   capture_output=True,text=True,timeout=60)
            good=(process.returncode == 0) == expected_success
            row={'name':name,'command':[str(x) for x in command],
                 'expected_success':expected_success,'returncode':process.returncode,
                 'passed':good,'stdout':process.stdout,'stderr':process.stderr}
            results.append(row)
            return process,good
        versions={}
        for name,command in [('cc',[args.cc,'--version']),('cxx',[args.cxx,'--version']),
                             ('java',[args.java_home/'bin/java','-version']),
                             ('javac',[args.java_home/'bin/javac','-version'])]:
            process,good=run('version-'+name,command)
            versions[name]={'command':[str(x) for x in command],
                            'stdout':process.stdout,'stderr':process.stderr}
            if not good: raise RuntimeError('cannot identify '+name)
        common=['-fPIC','-O2','-Wall','-Wextra','-Werror','-I'+str(work),
                '-I'+str(include),'-I'+str(args.java_home/'include'),
                '-I'+str(args.java_home/'include/linux')]
        compiled,good=run('javac',[args.java_home/'bin/javac','--release','21','-h',work,work/'AndroidNativeContracts.java'])
        if not good: raise RuntimeError(compiled.stderr)
        profiles=[('host-gnu',[],True),('host-posix',[],False)]
        profiles += [(f'bionic-api{api}-'+('gnu' if gnu else 'posix'),
                      ['-D__BIONIC__=1','-D__ANDROID__=1',f'-D__ANDROID_API__={api}',
                       '-I'+str(model_include)],gnu)
                     for api in (21,23,35) for gnu in (False,True)]
        for name, extra, gnu in profiles:
            model=name.startswith('bionic')
            flags=['-D_GNU_SOURCE=1'] if gnu else ['-U_GNU_SOURCE','-D_POSIX_C_SOURCE=200809L']
            for state,root in [('before',args.before),('after',args.after)]:
                lib=work/f'lib-{state}-{name}.so'
                command=[args.cc,'-std=c17',*common,*extra,*flags,'-DLINUX=1',
                         '-shared','-Wl,-Bsymbolic-functions','-Wl,--no-undefined',
                         root/JNI_PATH,work/'jni_probe.c']
                if model: command += [work/'model.c','-Wl,--wrap=__xpg_strerror_r']
                command += ['-ldl','-o',lib]
                expected=not(model and state=='before' and gnu)
                process,good=run(f'jni-{state}-{name}-compile',command,expected)
                if not expected:
                    # Only the Bionic-unavailable glibc symbol is an accepted
                    # baseline failure; a syntax or unrelated link error is not.
                    results[-1]['passed'] &= (
                        "undefined reference to `__wrap___xpg_strerror_r'" in process.stderr)
                if process.returncode == 0:
                    run(f'jni-{state}-{name}-runtime',
                        [args.java_home/'bin/java','-Xcheck:jni','-cp',work,
                         'AndroidNativeContracts',lib,str(model).lower()])
                # Compile the complete production C source as C++ too to
                # check declaration linkage; no C++ runtime claim is made.
                if state=='after':
                    run(f'jni-{state}-{name}-cxx',
                        [args.cxx,'-x','c++','-std=c++20',*common,*extra,*flags,
                         '-DLINUX=1','-c',root/JNI_PATH,'-o',work/(name+'.o')])
        block_seals={}
        for state,root in [('before',args.before),('after',args.after)]:
            text=(root/NET_PATH).read_text()
            start=text.index('#if defined(DONT_ENABLE_IPV6)')
            end=text.index('#endif /* DONT_ENABLE_IPV6 */',start)+len('#endif /* DONT_ENABLE_IPV6 */')
            block=text[start:end]
            block_seals[state]=hashlib.sha256(block.encode()).hexdigest()
            for android in (False,True):
                for disabled in (False,True):
                    name=f'ipv6-{state}-'+('android' if android else 'linux')+('-disabled' if disabled else '')
                    src=work/(name+'.c'); exe=work/name
                    src.write_text(IPV6_PREFIX+block+IPV6_SUFFIX)
                    definitions=['-D_GNU_SOURCE=1']
                    if android: definitions += ['-D__ANDROID__=1']
                    if disabled: definitions += ['-DDONT_ENABLE_IPV6=1']
                    process,good=run(name+'-compile',[args.cc,'-std=c17',*common,
                        '-Wno-unused-variable','-Wno-unused-function',*definitions,src,'-o',exe])
                    if process.returncode==0:
                        expected=not(state=='before' and android and not disabled)
                        process,good=run(name+'-runtime',[exe],expected)
                        if not expected:
                            results[-1]['passed'] &= (
                                'IPv6 cases=18 failures=12' in process.stdout and
                                'fd=0 proc=-1 symbol=1 actual=0 expected=1' in process.stdout and
                                'fd=7 proc=0 symbol=1 actual=0 expected=1' in process.stdout)
        artifact_seals={str(path.relative_to(work)):sha256(path)
                        for path in sorted(work.rglob('*')) if path.is_file()}
    report={'schema':1,'scope':'host-native-and-JNI-model-regression',
            'identities':identities,'header_sha256':header_seals,
            'compiler_vm_versions':versions,'artifact_sha256':artifact_seals,
            'toolchain_header_sha256':toolchain_header_seals,
            'ipv6_source_block_sha256':block_seals,'results':results,
            'passed':all(x['passed'] for x in results),
            'limitations':['No Android NDK, Bionic libc binary, ARM ABI or device executed.',
                'Bionic strerror declarations/behavior are explicit host fixtures.',
                'JNI tests use a real host Java21 VM with an ASCII conversion seam.',
                'IPv6 tests use the exact source block, not the entire libnet translation unit.',
                'No full M3 JDK build, installed-image qualification or performance claim.']}
    args.output.parent.mkdir(parents=True,exist_ok=True)
    with args.output.open('x') as out: json.dump(report,out,indent=2); out.write('\n')
    print(json.dumps({'passed':report['passed'],'commands':len(results),
                      'failures':[x['name'] for x in results if not x['passed']]}))
    return 0 if report['passed'] else 1

if __name__=='__main__':
    raise SystemExit(main())
