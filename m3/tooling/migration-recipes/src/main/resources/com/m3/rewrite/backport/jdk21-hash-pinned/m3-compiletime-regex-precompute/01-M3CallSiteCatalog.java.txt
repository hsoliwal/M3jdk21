// SPDX-License-Identifier: Apache-2.0
package com.m3.compiletime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Closed catalogue of JDK/library call sites whose String argument is a compile-time-known
 * precompute candidate. One row = one exact {@code owner.name desc}; {@code arg} is the
 * zero-based argument index of the String operand, {@code -1} for the receiver.
 *
 * <p>Inventory categories follow {@code m3/docs/m3-compiletime-precompute.md}:
 * {@code b} regex, {@code c} format, {@code f} keys, {@code g} other large lanes.</p>
 */
public final class M3CallSiteCatalog {
    /** One catalogued String operand of a call site. */
    public record Site(String category, String kind, int arg) {}

    private static final String S = "Ljava/lang/String;";
    private static final String O = "[Ljava/lang/Object;";
    private static final String CS = "Ljava/lang/CharSequence;";
    private static final String LOC = "Ljava/util/Locale;";
    private static final String P = "java/util/regex/Pattern";
    private static final String STR = "java/lang/String";

    private static final Map<String, Site> EXACT = new HashMap<>();
    private static final Map<String, List<Map.Entry<String, Site>>> PREFIX = new HashMap<>();

    static {
        regex();
        format();
        keys();
        needles();
        reflection();
        logging();
    }

    private M3CallSiteCatalog() {}

    private static void regex() {
        put("b", "pattern_compile", 0, P, "compile", "(" + S + ")Ljava/util/regex/Pattern;");
        put("b", "pattern_compile_flags", 0, P, "compile", "(" + S + "I)Ljava/util/regex/Pattern;");
        put("b", "pattern_matches", 0, P, "matches", "(" + S + CS + ")Z");
        put("b", "string_matches", 0, STR, "matches", "(" + S + ")Z");
        put("b", "string_replace_all", 0, STR, "replaceAll", "(" + S + S + ")" + S);
        put("b", "string_replace_first", 0, STR, "replaceFirst", "(" + S + S + ")" + S);
        put("b", "string_split", 0, STR, "split", "(" + S + ")[" + S);
        put("b", "string_split_limit", 0, STR, "split", "(" + S + "I)[" + S);
        put("b", "string_split_with_delimiters", 0, STR, "splitWithDelimiters", "(" + S + "I)[" + S);
        String scanner = "java/util/Scanner";
        put("b", "scanner_use_delimiter", 0, scanner, "useDelimiter", "(" + S + ")Ljava/util/Scanner;");
        put("b", "scanner_find_in_line", 0, scanner, "findInLine", "(" + S + ")" + S);
        put("b", "scanner_has_next", 0, scanner, "hasNext", "(" + S + ")Z");
        put("b", "scanner_next", 0, scanner, "next", "(" + S + ")" + S);
    }

    private static void format() {
        put("c", "string_format", 0, STR, "format", "(" + S + O + ")" + S);
        put("c", "string_format_locale", 1, STR, "format", "(" + LOC + S + O + ")" + S);
        put("c", "string_formatted", -1, STR, "formatted", "(" + O + ")" + S);
        for (String owner : List.of("java/io/PrintStream", "java/io/PrintWriter")) {
            String self = "L" + owner + ";";
            for (String name : List.of("printf", "format")) {
                put("c", "stream_" + name, 0, owner, name, "(" + S + O + ")" + self);
                put("c", "stream_" + name + "_locale", 1, owner, name, "(" + LOC + S + O + ")" + self);
            }
        }
        String fmt = "java/util/Formatter";
        put("c", "formatter_format", 0, fmt, "format", "(" + S + O + ")Ljava/util/Formatter;");
        put("c", "formatter_format_locale", 1, fmt, "format", "(" + LOC + S + O + ")Ljava/util/Formatter;");
        String mf = "java/text/MessageFormat";
        put("c", "message_format_static", 0, mf, "format", "(" + S + O + ")" + S);
        put("c", "message_format_new", 0, mf, "<init>", "(" + S + ")V");
        put("c", "message_format_new_locale", 0, mf, "<init>", "(" + S + LOC + ")V");
    }

    private static void keys() {
        String rb = "java/util/ResourceBundle";
        put("f", "resource_bundle_key", 0, rb, "getString", "(" + S + ")" + S);
        put("f", "resource_bundle_key", 0, rb, "getObject", "(" + S + ")Ljava/lang/Object;");
        put("f", "resource_bundle_key", 0, rb, "getStringArray", "(" + S + ")[" + S);
        put("f", "resource_bundle_key", 0, rb, "containsKey", "(" + S + ")Z");
        String sys = "java/lang/System";
        put("f", "property_key", 0, sys, "getProperty", "(" + S + ")" + S);
        put("f", "property_key", 0, sys, "getProperty", "(" + S + S + ")" + S);
        put("f", "property_key_set", 0, sys, "setProperty", "(" + S + S + ")" + S);
        put("f", "env_key", 0, sys, "getenv", "(" + S + ")" + S);
        put("f", "property_key", 0, "java/lang/Integer", "getInteger", "(" + S + ")Ljava/lang/Integer;");
        put("f", "property_key", 0, "java/lang/Integer", "getInteger", "(" + S + "I)Ljava/lang/Integer;");
        put("f", "property_key", 0, "java/lang/Long", "getLong", "(" + S + ")Ljava/lang/Long;");
        put("f", "property_key", 0, "java/lang/Long", "getLong", "(" + S + "J)Ljava/lang/Long;");
        put("f", "property_key", 0, "java/lang/Boolean", "getBoolean", "(" + S + ")Z");
        put("f", "property_key", 0, "java/util/Properties", "getProperty", "(" + S + ")" + S);
        put("f", "property_key", 0, "java/util/Properties", "getProperty", "(" + S + S + ")" + S);
        put("f", "property_key_set", 0, "java/util/Properties", "setProperty", "(" + S + S + ")Ljava/lang/Object;");
        String gpa = "sun/security/action/GetPropertyAction";
        put("f", "property_key", 0, gpa, "privilegedGetProperty", "(" + S + ")" + S);
        put("f", "property_key", 0, gpa, "privilegedGetProperty", "(" + S + S + ")" + S);
        put("f", "property_key", 0, "sun/security/action/GetIntegerAction", "privilegedGetProperty", "(" + S + "I)I");
        put("f", "property_key", 0, "sun/security/action/GetBooleanAction", "privilegedGetProperty", "(" + S + ")Z");
        put("f", "property_key", 0, gpa, "<init>", "(" + S + ")V");
        put("f", "property_key", 0, gpa, "<init>", "(" + S + S + ")V");
        put("f", "property_key", 0, "jdk/internal/misc/VM", "getSavedProperty", "(" + S + ")" + S);
    }

    private static void needles() {
        put("g", "search_index_of", 0, STR, "indexOf", "(" + S + ")I");
        put("g", "search_index_of", 0, STR, "indexOf", "(" + S + "I)I");
        put("g", "search_last_index_of", 0, STR, "lastIndexOf", "(" + S + ")I");
        put("g", "search_last_index_of", 0, STR, "lastIndexOf", "(" + S + "I)I");
        put("g", "search_contains", 0, STR, "contains", "(" + CS + ")Z");
        put("g", "search_starts_with", 0, STR, "startsWith", "(" + S + ")Z");
        put("g", "search_starts_with", 0, STR, "startsWith", "(" + S + "I)Z");
        put("g", "search_ends_with", 0, STR, "endsWith", "(" + S + ")Z");
        put("g", "search_replace_literal", 0, STR, "replace", "(" + CS + CS + ")" + S);
        put("g", "equals_ignore_case_arg", 0, STR, "equalsIgnoreCase", "(" + S + ")Z");
        put("g", "string_builder_append", 0, "java/lang/StringBuilder", "append", "(" + S + ")Ljava/lang/StringBuilder;");
        put("g", "string_builder_append", 0, "java/lang/StringBuffer", "append", "(" + S + ")Ljava/lang/StringBuffer;");
        put("g", "string_builder_new", 0, "java/lang/StringBuilder", "<init>", "(" + S + ")V");
    }

    private static void reflection() {
        put("g", "class_for_name", 0, "java/lang/Class", "forName", "(" + S + ")Ljava/lang/Class;");
        put("g", "class_for_name", 0, "java/lang/Class", "forName", "(" + S + "ZLjava/lang/ClassLoader;)Ljava/lang/Class;");
        prefix("g", "reflection_member_name", 0, "java/lang/Class", "getMethod", "(" + S);
        prefix("g", "reflection_member_name", 0, "java/lang/Class", "getDeclaredMethod", "(" + S);
        prefix("g", "reflection_member_name", 0, "java/lang/Class", "getField", "(" + S);
        prefix("g", "reflection_member_name", 0, "java/lang/Class", "getDeclaredField", "(" + S);
        String lookup = "java/lang/invoke/MethodHandles$Lookup";
        for (String name : List.of("findVirtual", "findStatic", "findSpecial", "findGetter",
                "findSetter", "findStaticGetter", "findStaticSetter", "findVarHandle",
                "findStaticVarHandle")) {
            prefix("g", "method_handle_member_name", 1, lookup, name, "(Ljava/lang/Class;" + S);
        }
        put("g", "unsafe_field_name", 1, "jdk/internal/misc/Unsafe", "objectFieldOffset", "(Ljava/lang/Class;" + S + ")J");
        put("g", "charset_name", 0, "java/nio/charset/Charset", "forName", "(" + S + ")Ljava/nio/charset/Charset;");
        put("g", "string_getbytes_charset_name", 0, STR, "getBytes", "(" + S + ")[B");
    }

    private static void logging() {
        for (String level : List.of("trace", "debug", "info", "warn", "error")) {
            prefix("g", "log_template_slf4j", 0, "org/slf4j/Logger", level, "(" + S);
            prefix("g", "log_template_slf4j", 1, "org/slf4j/Logger", level, "(Lorg/slf4j/Marker;" + S);
        }
        String jul = "java/util/logging/Logger";
        String lvl = "Ljava/util/logging/Level;";
        prefix("g", "log_template_jul", 1, jul, "log", "(" + lvl + S);
        for (String level : List.of("severe", "warning", "info", "config", "fine", "finer", "finest")) {
            put("g", "log_message_jul", 0, jul, level, "(" + S + ")V");
            prefix("g", "log_message_platform", 0, "sun/util/logging/PlatformLogger", level, "(" + S);
        }
        String sl = "java/lang/System$Logger";
        String sll = "Ljava/lang/System$Logger$Level;";
        prefix("g", "log_template_system_logger", 1, sl, "log", "(" + sll + S);
        prefix("f", "resource_bundle_key", 2, sl, "log", "(" + sll + "Ljava/util/ResourceBundle;" + S);
    }

    private static void put(String category, String kind, int arg, String owner, String name, String desc) {
        EXACT.put(owner + '.' + name + desc, new Site(category, kind, arg));
    }

    private static void prefix(String category, String kind, int arg, String owner, String name, String descPrefix) {
        PREFIX.computeIfAbsent(owner + '.' + name, key -> new ArrayList<>())
                .add(Map.entry(descPrefix, new Site(category, kind, arg)));
    }

    /** Returns the catalogued operand for an exact call, if any. */
    public static Optional<Site> lookup(String owner, String name, String desc) {
        String member = owner + '.' + name;
        Site exact = EXACT.get(member + desc);
        if (exact != null) {
            return Optional.of(exact);
        }
        for (Map.Entry<String, Site> entry : PREFIX.getOrDefault(member, List.of())) {
            if (desc.startsWith(entry.getKey())) {
                return Optional.of(entry.getValue());
            }
        }
        if ("<init>".equals(name) && throwableName(owner)
                && (desc.equals("(" + S + ")V") || desc.equals("(" + S + "Ljava/lang/Throwable;)V"))) {
            return Optional.of(new Site("g", "exception_message", 0));
        }
        return Optional.empty();
    }

    private static boolean throwableName(String owner) {
        return owner.endsWith("Exception") || owner.endsWith("Error");
    }
}
