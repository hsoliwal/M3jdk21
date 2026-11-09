/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project */
/* SPDX-License-Identifier: Apache-2.0 */
/* Synthetic inspector output for the product import-gate regression. Not a binary inspector. */
#include <stdio.h>
#include <string.h>

static void elf(const char *name) {
    fputs("Dynamic section at offset 0x1000 contains 2 entries:\n", stdout);
    printf(" 0x0000000000000001 (NEEDED) Shared library: [%s]\n", name);
}

static void coff(const char *name) {
    fputs("File: fixture.dll\nFormat: COFF-x86-64\nImport {\n", stdout);
    printf("  Name: %s\n}\n", name);
}

static void macho(const char *name, const char *command) {
    /* LC_ID_DYLIB is an identifier, not a dependency; the checker must ignore it. */
    fputs("fixture.dylib:\nLoad command 0\n cmd LC_ID_DYLIB\n cmdsize 64\n"
          " name @rpath/not-a-dependency.dylib (offset 24)\n"
          "Load command 1\n", stdout);
    printf(" cmd %s\n cmdsize 64\n name %s (offset 24)\n", command, name);
}

int main(int argc, char **argv) {
    if (argc != 3) return 2;
    const char *input = argv[2];
    if (strstr(input, "tool-error") != NULL) {
        fputs("fixture inspector failure\n", stderr);
        return 42;
    }
    if (strstr(input, "empty") != NULL) return 0;
    if (strstr(input, "malformed") != NULL) {
        fputs("not an object inspection\n", stdout);
        return 0;
    }
    int vendor = strstr(input, "vendor") != NULL;
    int unknown = strstr(input, "unknown") != NULL;
    if (strcmp(argv[1], "-d") == 0) {
        elf(vendor ? "libOpenCL.so.1" : unknown ? "libc.so.vendor" : "libc.so.6");
        if (strstr(input, "partial") != NULL) {
            fputs(" 0x0000000000000001 (NEEDED) missing library name\n", stdout);
        }
        return 0;
    }
    if (strcmp(argv[1], "--coff-imports") == 0) {
        coff(vendor && strstr(input, "delay") == NULL ? "OpenCL.dll"
                : unknown ? "unexpected.dll" : "KERNEL32.dll");
        if (strstr(input, "partial") != NULL) {
            fputs("Import {\n Symbol: missing-dll-name\n}\n", stdout);
        }
        if (strstr(input, "delay") != NULL) {
            printf("DelayImport {\n Name: %s\n}\n", vendor ? "OpenCL.dll" : "KERNEL32.dll");
        }
        return 0;
    }
    if (strcmp(argv[1], "-l") == 0) {
        const char *name = vendor ? "/System/Library/Frameworks/OpenCL.framework/Versions/A/OpenCL"
                : unknown ? "/usr/local/lib/unexpected.dylib" : "/usr/lib/libSystem.B.dylib";
        const char *command = strstr(input, "weak") != NULL ? "LC_LOAD_WEAK_DYLIB"
                : strstr(input, "reexport") != NULL ? "LC_REEXPORT_DYLIB"
                : strstr(input, "upward") != NULL ? "LC_LOAD_UPWARD_DYLIB"
                : strstr(input, "lazy") != NULL ? "LC_LAZY_LOAD_DYLIB" : "LC_LOAD_DYLIB";
        if (strstr(input, "missing-name") != NULL) {
            fputs("fixture.dylib:\nLoad command 0\n cmd LC_LOAD_DYLIB\n cmdsize 64\n", stdout);
        } else {
            macho(name, command);
            if (strstr(input, "partial") != NULL) {
                fputs("Load command 2\n cmd LC_LOAD_DYLIB\n cmdsize 64\n", stdout);
            }
        }
        return 0;
    }
    return 3;
}
