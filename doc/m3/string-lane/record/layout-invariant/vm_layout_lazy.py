# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others
"""vm_layout_lazy.py <in-dir> <out-dir>: VM_STRING_LAYOUT_LAZY v1 (2026-11-17), owner decision 51 (chValue lazily
filled, const). <in-dir>/<out-dir> hold javaClasses.cpp, c2_globals.hpp and java.1 of M3jdk21 master.
  - create_from_unicode, create_from_str and create_from_symbol no longer fill chValue at creation (String fills it on
    the first whole-string char demand); java_lang_String::fill_char_value and the chValue offset stay for the JNI
    accessors (layout stage J), with the comment saying so;
  - C2 OptimizeStringConcat returns to its stock default (true) and the man page to its stock text: a String built by
    PhaseStringOpts without chValue is a valid state now.
Every anchor must match exactly once; the recipe refuses otherwise."""
import io
import os
import sys

SRC, DST = sys.argv[1:3]


def read(name):
    with io.open(os.path.join(SRC, name), encoding="utf-8", newline="") as stream:
        return stream.read()


def write(name, text):
    with io.open(os.path.join(DST, name), "w", encoding="utf-8", newline="") as stream:
        stream.write(text)


def replace(text, old, new, label, count=1):
    if text.count(old) != count:
        sys.exit("REFUSE %s: anchor found %d times, expected %d" % (label, text.count(old), count))
    return text.replace(old, new)


cpp = read("javaClasses.cpp")
cpp = replace(cpp, "\n  fill_char_value(h_obj, CHECK_NH);\n  return h_obj;\n", "\n  return h_obj;\n",
              "create_* no longer fill chValue", 3)
cpp = replace(cpp, """// M3JDK String layout invariant (doc/m3/string-lane/STRING_LAYOUT_INVARIANT.md): every
// String holds its chars in the private final char[] chValue as well as its bytes in value.
// A String the VM makes gets its chars here, once its bytes are final; the array is never
// written again.
""", """// M3JDK String layout invariant (doc/m3/string-lane/STRING_LAYOUT_INVARIANT.md, decision 51):
// chValue is a const char[] filled lazily, once, on the first whole-string char demand.
// Java fills it in String.constChars(); this fills it for the VM's own demands (the JNI
// string accessors) when it is still null. The array is never written again.
""", "fill_char_value comment")
write("javaClasses.cpp", cpp)

c2 = read("c2_globals.hpp")
c2 = replace(c2, """  /* M3JDK String layout invariant: off by default, because the        */ \\
  /* optimized Strings would be built without their chValue chars.      */ \\
  product(bool, OptimizeStringConcat, false,                                \\
""", """  product(bool, OptimizeStringConcat, true,                                 \\
""", "OptimizeStringConcat stock default")
write("c2_globals.hpp", c2)

man = read("java.1")
man = replace(man, """This option is disabled by default in M3JDK: the optimized code builds a
\\f[V]String\\f[R] without its \\f[V]char\\f[R] array, which the M3JDK
\\f[V]String\\f[R] layout invariant requires.
""", """This option is enabled by default.
To disable the optimization of \\f[V]String\\f[R] concatenation
operations, specify \\f[V]-XX:-OptimizeStringConcat\\f[R].
""", "java.1 stock text")
write("java.1", man)
print("VM_STRING_LAYOUT_LAZY v1 applied")
