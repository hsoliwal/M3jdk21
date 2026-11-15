#!/usr/bin/env python3
# Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
# SPDX-License-Identifier: GPL-2.0-only
"""Replay exact Apple locale functions with a CoreFoundation model and real JNI.

This compiles the complete locale function block, not the Objective-C OS/proxy
tail. Actual JDK property/JNI headers are required. CoreFoundation is an explicit
behavior model; this is not an Apple SDK, Mach-O or mobile-device qualification.
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

SOURCE = Path("src/java.base/macosx/native/libjava/java_props_macosx.c")
HEADERS = [
    Path("src/java.base/macosx/native/libjava/java_props_macosx.h"),
    Path("src/java.base/share/native/libjava/java_props.h"),
    Path("src/java.base/share/native/libjava/jni_util.h"),
    Path("src/java.base/share/native/libjava/jlong.h"),
    Path("src/java.base/unix/native/libjava/jlong_md.h"),
]
START = "char *getPosixLocale(int cat) {"
END = "// 10.9 SDK does not include the NSOperatingSystemVersion struct."

MODEL = r'''
#define _GNU_SOURCE 1
#include <assert.h>
#include <ctype.h>
#include <locale.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "java_props_macosx.h"
#include "AppleLocaleNativeContracts.h"
#define FIXTURE_CHECK(value) do { if (!(value)) abort(); } while (0)
typedef struct { const char *text; int kind; } CFObject;
typedef const CFObject *CFStringRef;
typedef const CFObject *CFArrayRef;
typedef const CFObject *CFLocaleRef;
typedef struct {
    CFObject language, locale, array;
    unsigned flags;
    int acquired, released, arrayLive, localeLive;
    char trace[128]; size_t length;
} Fixture;
static _Thread_local Fixture f;
static _Thread_local double kCFCoreFoundationVersionNumber;
static void trace(char c) {
    FIXTURE_CHECK(f.length + 1 < sizeof(f.trace));
    f.trace[f.length++] = c; f.trace[f.length] = '\0';
}
static void configure(const char *language, const char *locale, int newer, unsigned flags) {
    memset(&f, 0, sizeof(f));
    f.language = (CFObject){language, 1}; f.locale = (CFObject){locale, 2};
    f.array = (CFObject){NULL, 3}; f.flags = flags;
    kCFCoreFoundationVersionNumber = newer ? 1500 : 1200;
}
static CFArrayRef CFLocaleCopyPreferredLanguages(void) {
    trace('P'); if (f.flags & 1) return NULL;
    FIXTURE_CHECK(!f.arrayLive); f.arrayLive = 1; f.acquired++; return &f.array;
}
static int CFArrayGetCount(CFArrayRef value) {
    trace('N'); FIXTURE_CHECK(value == &f.array && f.arrayLive); return (f.flags & 2) ? 0 : 1;
}
static const void *CFArrayGetValueAtIndex(CFArrayRef value, int index) {
    trace('G'); FIXTURE_CHECK(value == &f.array && f.arrayLive && index == 0);
    return (f.flags & 4) || f.language.text == NULL ? NULL : &f.language;
}
static CFLocaleRef CFLocaleCopyCurrent(void) {
    trace('C'); if ((f.flags & 32) || f.locale.text == NULL) return NULL;
    FIXTURE_CHECK(!f.localeLive); f.localeLive = 1; f.acquired++; return &f.locale;
}
static CFStringRef CFLocaleGetIdentifier(CFLocaleRef value) {
    trace('I'); FIXTURE_CHECK(value == &f.locale && f.localeLive); return value;
}
static int CFStringGetSystemEncoding(void) { trace('E'); return 0; }
static bool CFStringGetCString(CFStringRef value, char *out, int capacity, int encoding) {
    (void)encoding; FIXTURE_CHECK(value != NULL && capacity > 0);
    trace(value->kind == 1 ? 'L' : 'S');
    unsigned failure = value->kind == 1 ? 8 : 16;
    if (f.flags & failure) {
        // A false return does not grant permission to use even plausible output.
        snprintf(out, (size_t)capacity, "zz_US"); return false;
    }
    size_t length = strlen(value->text);
    if (length >= (size_t)capacity) { out[0] = '\0'; return false; }
    memcpy(out, value->text, length + 1); return true;
}
static void CFRelease(const void *value) {
    trace('R');
    if (value == &f.array) { FIXTURE_CHECK(f.arrayLive); f.arrayLive = 0; }
    else { FIXTURE_CHECK(value == &f.locale && f.localeLive); f.localeLive = 0; }
    f.released++;
}
'''

BRIDGE = r'''
JNIEXPORT jstring JNICALL Java_AppleLocaleNativeContracts_run
  (JNIEnv *env, jclass cls, jstring language, jstring locale,
   jint category, jboolean newer, jint flags, jint mode) {
    (void)cls;
    const char *lang = language == NULL ? NULL : (*env)->GetStringUTFChars(env, language, NULL);
    if (language != NULL && lang == NULL) return NULL;
    const char *loc = locale == NULL ? NULL : (*env)->GetStringUTFChars(env, locale, NULL);
    if (locale != NULL && loc == NULL) {
        if (lang != NULL) (*env)->ReleaseStringUTFChars(env, language, lang);
        return NULL;
    }
    configure(lang, loc, newer, (unsigned)flags);
    int cat = category == 0 ? LC_MESSAGES : LC_TIME;
    char *owned = NULL;
    const char *result;
    if (mode == 2) {
        owned = strdup(lang == NULL ? "" : lang);
        result = owned == NULL ? NULL : convertToPOSIXLocale(owned);
    } else if (mode == 3) {
        owned = getPosixLocale(cat); result = owned;
    } else {
        owned = mode == 1 ? setupMacOSXLocale(cat) : getMacOSXLocale(cat);
        result = owned;
    }
    FIXTURE_CHECK(f.acquired == f.released && !f.arrayLive && !f.localeLive);
    char encoded[4096];
    int length = snprintf(encoded, sizeof(encoded), "%s\t%s\t%d\t%d",
                          result == NULL ? "<null>" : result,
                          f.trace, f.acquired, f.released);
    FIXTURE_CHECK(length >= 0 && (size_t)length < sizeof(encoded));
    jstring answer = (*env)->NewStringUTF(env, encoded);
    free(owned);
    if (loc != NULL) (*env)->ReleaseStringUTFChars(env, locale, loc);
    if (lang != NULL) (*env)->ReleaseStringUTFChars(env, language, lang);
    return answer;
}

int main(int argc, char **argv) {
    if (argc != 2) return 2;
    char language[256]; memset(language, 'a', sizeof(language)); language[125] = '\0';
    const char *lang = "en", *locale = "en_US"; unsigned flags = 0; int direct = 0;
    if (!strcmp(argv[1], "failed-current")) flags = 16;
    else if (!strcmp(argv[1], "overflow")) lang = language;
    else if (!strcmp(argv[1], "malformed")) { lang = "en-abcdefg"; direct = 1; }
    else if (!strcmp(argv[1], "invalid-script")) { lang = "en-Latin"; direct = 1; }
    else if (!strcmp(argv[1], "invalid-byte")) { lang = "en-\xE9" "abc"; direct = 1; }
    else if (!strcmp(argv[1], "empty-region")) locale = "en_";
    else return 2;
    configure(lang, locale, 1, flags);
    char *owned = direct ? strdup(lang) : getMacOSXLocale(LC_MESSAGES);
    const char *result = direct ? convertToPOSIXLocale(owned) : owned;
    if (result != NULL) {
        fprintf(stderr, "EXPECTED_NULL:%s\n", argv[1]); free(owned); return 42;
    }
    free(owned);
    FIXTURE_CHECK(f.acquired == f.released && !f.arrayLive && !f.localeLive);
    printf("PASS:%s\n", argv[1]); return 0;
}
'''

JAVA = r'''
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AppleLocaleNativeContracts {
    private static native String run(String language, String locale, int category,
                                     boolean newer, int flags, int mode);
    private static final Pattern TAG = Pattern.compile(
            "^([^-]+)(?:-([A-Za-z]{4})(?:[-_]([A-Za-z]{2}|[0-9]{3}))?|-([A-Za-z]{2}|[0-9]{3}))?$");
    private static long checks;
    private static String convert(String value) {
        Matcher m = TAG.matcher(value);
        if (!m.matches()) return null;
        String region = m.group(3) == null ? m.group(4) : m.group(3);
        return m.group(1) + (region == null ? "" : "_" + region)
                + (m.group(2) == null ? "" : "@" + m.group(2));
    }
    private static void check(boolean value, String message) {
        checks++; if (!value) throw new AssertionError(message);
    }
    private static String value(String encoded) {
        String[] parts = encoded.split("\\t", -1);
        if (parts.length != 4 || !parts[2].equals(parts[3])) {
            throw new AssertionError("unbalanced reference/invalid result: " + encoded);
        }
        return parts[0].equals("<null>") ? null : parts[0];
    }
    private static boolean equal(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }
    private record Case(String language, String locale, int category, boolean newer,
                        int mode, String expected) {}
    private static void add(List<Case> cases, String language, String locale,
                            int category, boolean newer, int mode, String expected) {
        cases.add(new Case(language, locale, category, newer, mode, expected));
    }
    private static String execute(Case c) {
        String result = run(c.language, c.locale, c.category, c.newer, 0, c.mode);
        if (!equal(c.expected, value(result))) {
            throw new AssertionError(c + " result=" + result);
        }
        return result;
    }
    private static void parity() throws Exception {
        List<String> regions = new ArrayList<>();
        for (char a = 'A'; a <= 'Z'; a++) for (char b = 'A'; b <= 'Z'; b++) {
            regions.add("" + a + b);
        }
        for (int n = 0; n < 1000; n++) regions.add(String.format(java.util.Locale.ROOT, "%03d", n));
        List<Case> cases = new ArrayList<>();
        for (String region : regions) {
            add(cases, "en-" + region, "fr_CA", 0, true, 2, "en_" + region);
            add(cases, "haw-" + region, "fr_CA", 0, false, 0, "haw_" + region);
            add(cases, "en", "fr_" + region, 1, true, 0, "en_" + region);
            for (String script : new String[]{"Latn", "Cyrl", "Hans", "Hant"}) {
                for (String separator : new String[]{"-", "_"}) {
                    String tag = "zh-" + script + separator + region;
                    add(cases, tag, "fr_CA", 0, true, 2, convert(tag));
                    add(cases, "en-US", tag, 1, false, 0, convert(tag));
                }
                add(cases, "sr-" + script, "fr_" + region + "@calendar=gregorian",
                        0, true, 0, "sr_" + region + "@" + script);
            }
        }
        for (String language : new String[]{"en", "haw", "zh-Hans", "sr-Cyrl"}) {
            add(cases, language, null, 0, true, 0, convert(language));
        }
        for (int length = 1; length <= 124; length++) {
            String language = "a".repeat(length);
            add(cases, language, "en_US", 0, true, 0, language + "_US");
        }
        add(cases, "de-DE@calendar=gregorian", "fr_CA", 0, true, 0, "de_DE");
        add(cases, "en-US", "fr_CA@calendar=gregorian", 1, false, 0, "fr_CA");
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (Case c : cases) {
            String result = execute(c); checks++;
            digest.update((result + "\n").getBytes(StandardCharsets.UTF_8));
        }
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread[] threads = new Thread[4];
        for (int t = 0; t < threads.length; t++) {
            final int offset = t;
            threads[t] = new Thread(() -> {
                try { for (int i = 0; i < 2000; i++) execute(cases.get((i * 31 + offset) % cases.size())); }
                catch (Throwable error) { failure.compareAndSet(null, error); }
            });
            threads[t].start();
        }
        for (Thread thread : threads) thread.join();
        if (failure.get() != null) throw new AssertionError(failure.get());
        checks += 8000;
        String retained = run("en-US", "fr_CA", 0, true, 0, 0);
        run("sr-Cyrl", "fr_CA", 0, true, 0, 0);
        check("en_US".equals(value(retained)), "returned result lost ownership");
        System.out.println("{\"checks\":" + checks + ",\"corpus\":" + cases.size()
                + ",\"thread_checks\":8000,\"digest\":\""
                + HexFormat.of().formatHex(digest.digest()) + "\"}");
    }
    private static void errors() {
        for (int flags : new int[]{1, 2, 4, 8, 16}) {
            check(value(run("en", "en_US", 0, true, flags, 0)) == null, "failure flag " + flags);
            String fallback = value(run("en", "en_US", 0, true, flags, 1));
            check(equal(fallback, value(run("en", "en_US", 0, true, flags, 3))), "POSIX fallback");
        }
        check("en".equals(value(run("en", "en_US", 0, true, 32, 0))), "absent region locale");
        check(value(run("en", null, 1, false, 0, 0)) == null, "absent format locale");
        check(value(run(null, "en_US", 0, true, 0, 0)) == null, "absent language");
        check(value(run("a".repeat(125), "en_US", 0, true, 0, 0)) == null, "one past capacity");
        check(value(run("a".repeat(128), "en_US", 0, true, 0, 0)) == null, "conversion capacity");
        check(value(run("en", "en_", 0, true, 0, 0)) == null, "empty region");
        for (String invalid : new String[]{"en-", "en-U", "en-1234", "en-Latin", "en-abcdefg",
                "en-Latn-", "en-Latn-USA", "en-Latn-1A2", "en-Latn-12", "en-La-n-US",
                "en-La1n-US", "en-Latn/US", "en-Latn--US", "en-US-extra", "en-1A"}) {
            check(value(run(invalid, null, 0, true, 0, 2)) == null, "invalid shape " + invalid);
        }
        System.out.println("{\"checks\":" + checks + ",\"invalid_metadata\":\"PASS\"}");
    }
    public static void main(String[] args) throws Exception {
        System.load(args[0]);
        if (args[1].equals("parity")) parity(); else errors();
    }
}
'''


def sha(data):
    return hashlib.sha256(data).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ("before", "after", "headers", "java-home", "output"):
        parser.add_argument("--" + name, required=True, type=Path)
    args = parser.parse_args()
    output = args.output.resolve()
    roots = [getattr(args, name).resolve() for name in ("before", "after", "headers")]
    if output.exists() or any(output.is_relative_to(root) for root in roots):
        raise ValueError("Output must be new and outside every source root")
    java_home = args.java_home.resolve()
    report = {"status": "RUNNING", "sources": {}, "commands": [], "artifacts": {},
              "leak_sanitizer": "UNAVAILABLE: executor ptrace; initial fatal diagnostic retained; ASan/UBSan remain active",
              "boundary": "Exact locale C block; modeled CoreFoundation; real host JVM JNI; no Apple SDK/Objective-C tail/device qualification"}
    env = dict(os.environ, LC_ALL="C", LANG="C", UBSAN_OPTIONS="halt_on_error=1:print_stacktrace=1",
               ASAN_OPTIONS="detect_leaks=0:abort_on_error=1")

    def run(command, label, expected=0, diagnostic=None):
        completed = subprocess.run([str(x) for x in command], env=env, text=True,
                                   stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=120)
        passed = completed.returncode == 0 if expected == 0 else completed.returncode != 0
        if diagnostic:
            passed = passed and diagnostic in completed.stdout + completed.stderr
        report["commands"].append({"label": label, "command": [str(x) for x in command],
                                   "exit": completed.returncode, "expected": expected,
                                   "diagnostic": diagnostic, "stdout": completed.stdout,
                                   "stderr": completed.stderr, "pass": passed})
        if not passed:
            raise AssertionError(f"{label}: exit {completed.returncode}\n{completed.stderr[-4000:]}")
        return completed.stdout

    try:
        if not shutil.which("gcc") or not shutil.which("g++"):
            raise RuntimeError("A real host GCC/G++ toolchain is required")
        with tempfile.TemporaryDirectory(prefix="apple-locale-contracts-") as directory:
            temp = Path(directory)
            headers = temp / "headers"; headers.mkdir()
            for path in HEADERS:
                data = (args.headers / path).read_bytes()
                report["sources"]["headers/" + str(path)] = sha(data)
                (headers / path.name).write_bytes(data)
            java = temp / "AppleLocaleNativeContracts.java"; java.write_text(JAVA)
            classes = temp / "classes"; classes.mkdir()
            run([java_home / "bin/javac", "-Xlint:all", "-Werror", "-h", headers, "-d", classes, java], "javac-generated-jni")
            include = ["-I" + str(headers), "-I" + str(java_home / "include"),
                       "-I" + str(java_home / "include/linux")]
            cpp = temp / "headers.cpp"
            cpp.write_text('#include <type_traits>\n#include "java_props_macosx.h"\n'
                           'static_assert(std::is_same_v<decltype(&setupMacOSXLocale), char *(*)(int)>);\n'
                           'static_assert(std::is_same_v<decltype(&convertToPOSIXLocale), const char *(*)(const char *)>);\n')
            run(["g++", "-std=c++20", "-Wall", "-Wextra", "-Werror", *include, "-c", cpp,
                 "-o", temp / "headers.o"], "actual-headers-cxx20")
            parity = {}; exports = {}; projections = {}
            for lane in ("before", "after"):
                source = (getattr(args, lane) / SOURCE).read_bytes()
                report["sources"][lane + "/" + str(SOURCE)] = sha(source)
                text = source.decode("utf-8")
                if text.count(START) != 1 or text.count(END) != 1:
                    raise ValueError("Source block boundaries are not unique")
                projections[lane] = text[text.index(START):text.index(END)]
                if lane == "before":
                    frozen_tail = text[text.index(END):]
                elif text[text.index(END):] != frozen_tail:
                    raise AssertionError("Unrelated Objective-C OS/proxy tail changed")
                native = temp / (lane + ".c")
                native.write_text(MODEL + projections[lane] + BRIDGE)
                library = temp / (lane + ".so")
                common = ["gcc", "-std=c17", "-Wall", "-Wextra", "-Werror", "-O1", "-g", *include]
                run([*common, "-fPIC", "-shared", "-fsanitize=undefined", "-fno-sanitize-recover=all",
                     native, "-o", library], lane + "-native-jni-ubsan")
                symbols = run(["nm", "-D", "--defined-only", library], lane + "-exports")
                exports[lane] = sorted(line.split()[-1] for line in symbols.splitlines())
                java_command = [java_home / "bin/java", "-Xcheck:jni", "-cp", classes,
                                "AppleLocaleNativeContracts", library]
                parity[lane] = json.loads(run([*java_command, "parity"], lane + "-host-jni-parity"))
                if lane == "after":
                    report["error_checks"] = json.loads(run([*java_command, "errors"], "after-host-jni-errors"))
                binary = temp / (lane + "-asan")
                run([*common, "-fno-pie", "-no-pie", "-DNDEBUG", "-fsanitize=address,undefined", "-fno-sanitize-recover=all",
                     native, "-o", binary], lane + "-native-asan")
                cases = {"failed-current": "EXPECTED_NULL", "overflow": "stack-buffer-overflow",
                         "malformed": "AddressSanitizer", "invalid-script": "EXPECTED_NULL",
                         "invalid-byte": "EXPECTED_NULL", "empty-region": "EXPECTED_NULL"}
                for case, diagnostic in cases.items():
                    run([binary, case], lane + "-" + case,
                        expected=1 if lane == "before" else 0,
                        diagnostic=diagnostic if lane == "before" else "PASS:" + case)
            if parity["before"] != parity["after"]:
                raise AssertionError("Supported result/call-trace parity changed")
            if exports["before"] != exports["after"]:
                raise AssertionError("Native export surface changed")
            report["parity"] = parity
            report["exports"] = exports["after"]
            report["projection_hashes"] = {key: sha(value.encode()) for key, value in projections.items()}
            for path in sorted(temp.rglob("*")):
                if path.is_file():
                    report["artifacts"][str(path.relative_to(temp))] = sha(path.read_bytes())
            report["status"] = "PASS"
    except Exception as failure:
        report["status"] = "FAIL"
        report["failure"] = repr(failure)
        raise
    finally:
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_text(json.dumps(report, indent=2) + "\n")
    print(json.dumps({"status": report["status"], "commands": len(report["commands"]),
                      "parity": report.get("parity"), "report": str(output)}))


if __name__ == "__main__":
    main()
