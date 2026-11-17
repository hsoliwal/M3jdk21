# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others
"""vm_layout_invariant.py <in-dir> <out-dir>: VM_STRING_LAYOUT_INVARIANT v1 (2026-11-17), owner decision 47.
<in-dir>/<out-dir> hold javaClasses.hpp, javaClasses.cpp and c2_globals.hpp (HotSpot sources of M3jdk21 master).
The VM sites of the String layout invariant:
  1. java_lang_String learns the offset of the private final char[] chValue (STRING_FIELDS_DO, so CDS serialises it).
  2. create_from_unicode, create_from_str and create_from_symbol (the three callers of basic_create; JNI NewString*,
     StringTable, symbols, platform strings and class names all come through them) fill chValue from the finished
     value bytes before they return.
  3. C2's OptimizeStringConcat defaults to false: PhaseStringOpts allocates a String and stores value and coder
     without running a constructor, so its Strings would have no chValue (shown by StringLayoutInvariantTest under
     -Xcomp). javac's invokedynamic concatenation and StringBuilder.toString build through String constructors.
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


hpp = read("javaClasses.hpp")
hpp = replace(hpp, """  static int _value_offset;
  static int _m3_offset;
""", """  static int _value_offset;
  static int _chValue_offset;
  static int _m3_offset;
""", "hpp offset")
hpp = replace(hpp, """  static Handle basic_create(int length, bool byte_arr, TRAPS);
""", """  static Handle basic_create(int length, bool byte_arr, TRAPS);
  // M3JDK layout invariant: fills the private final char[] chValue from the finished value bytes.
  static void fill_char_value(Handle java_string, TRAPS);
""", "hpp fill_char_value")
write("javaClasses.hpp", hpp)

cpp = read("javaClasses.cpp")
cpp = replace(cpp, """int java_lang_String::_value_offset;
int java_lang_String::_m3_offset;
""", """int java_lang_String::_value_offset;
int java_lang_String::_chValue_offset;
int java_lang_String::_m3_offset;
""", "cpp offset")
cpp = replace(cpp, """  macro(_value_offset, k, vmSymbols::value_name(), byte_array_signature, false); \\
""", """  macro(_value_offset, k, vmSymbols::value_name(), byte_array_signature, false); \\
  macro(_chValue_offset, k, "chValue",     char_array_signature,    false); \\
""", "STRING_FIELDS_DO chValue")
cpp = replace(cpp, """Handle java_lang_String::create_from_unicode(const jchar* unicode, int length, TRAPS) {
""", """// M3JDK String layout invariant (doc/m3/string-lane/STRING_LAYOUT_INVARIANT.md): every
// String holds its chars in the private final char[] chValue as well as its bytes in value.
// A String the VM makes gets its chars here, once its bytes are final; the array is never
// written again.
void java_lang_String::fill_char_value(Handle java_string, TRAPS) {
  int length = java_lang_String::length(java_string());
  typeArrayOop chars = oopFactory::new_charArray(length, CHECK);
  oop obj = java_string();
  typeArrayOop bytes = value(obj);
  if (is_latin1(obj)) {
    for (int index = 0; index < length; index++) {
      chars->char_at_put(index, ((jchar) bytes->byte_at(index)) & 0xff);
    }
  } else {
    for (int index = 0; index < length; index++) {
      chars->char_at_put(index, bytes->char_at(index));
    }
  }
  obj->obj_field_put(_chValue_offset, chars);
}

Handle java_lang_String::create_from_unicode(const jchar* unicode, int length, TRAPS) {
""", "fill_char_value definition")
cpp = replace(cpp, """    if (strcmp(expected, actual) != 0) {
      fatal("Unicode conversion failure: %s --> %s", expected, actual);
    }
  }
#endif

  return h_obj;
}
""", """    if (strcmp(expected, actual) != 0) {
      fatal("Unicode conversion failure: %s --> %s", expected, actual);
    }
  }
#endif

  fill_char_value(h_obj, CHECK_NH);
  return h_obj;
}
""", "create_from_unicode fills chValue")
cpp = replace(cpp, """    if (strcmp(expected, actual) != 0) {
      fatal("String conversion failure: %s --> %s", expected, actual);
    }
  }
#endif

  return h_obj;
}
""", """    if (strcmp(expected, actual) != 0) {
      fatal("String conversion failure: %s --> %s", expected, actual);
    }
  }
#endif

  fill_char_value(h_obj, CHECK_NH);
  return h_obj;
}
""", "create_from_str fills chValue")
cpp = replace(cpp, """    if (strncmp(expected, actual, utf8_len) != 0) {
      fatal("Symbol conversion failure: %s --> %s", expected, actual);
    }
  }
#endif

  return h_obj;
}
""", """    if (strncmp(expected, actual, utf8_len) != 0) {
      fatal("Symbol conversion failure: %s --> %s", expected, actual);
    }
  }
#endif

  fill_char_value(h_obj, CHECK_NH);
  return h_obj;
}
""", "create_from_symbol fills chValue")
write("javaClasses.cpp", cpp)

c2 = read("c2_globals.hpp")
c2 = replace(c2, """  product(bool, OptimizeStringConcat, true,                                 \\
          "Optimize the construction of Strings by StringBuilder")          \\
""", """  /* M3JDK String layout invariant: off by default, because the        */ \\
  /* optimized Strings would be built without their chValue chars.      */ \\
  product(bool, OptimizeStringConcat, false,                                \\
          "Optimize the construction of Strings by StringBuilder")          \\
""", "OptimizeStringConcat default")
write("c2_globals.hpp", c2)

man = read("java.1")
man = replace(man, """\\f[V]-XX:+OptimizeStringConcat\\f[R]
Enables the optimization of \\f[V]String\\f[R] concatenation operations.
This option is enabled by default.
To disable the optimization of \\f[V]String\\f[R] concatenation
operations, specify \\f[V]-XX:-OptimizeStringConcat\\f[R].
""", """\\f[V]-XX:+OptimizeStringConcat\\f[R]
Enables the optimization of \\f[V]String\\f[R] concatenation operations.
This option is disabled by default in M3JDK: the optimized code builds a
\\f[V]String\\f[R] without its \\f[V]char\\f[R] array, which the M3JDK
\\f[V]String\\f[R] layout invariant requires.
""", "java.1 OptimizeStringConcat")
write("java.1", man)
print("VM_STRING_LAYOUT_INVARIANT v1 applied")
