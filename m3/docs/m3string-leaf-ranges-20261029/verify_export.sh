#!/usr/bin/env bash
# Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
# GPLv2 with the Classpath exception.
#
# Export-mechanism probe for leaf 1b on a stock JDK 21: a generated module named jdk.m3.text
# reads jdk.internal.access.SharedSecrets. Shows the export is required, that a command-line
# --add-exports grants it to that module only, and that jlink --add-options bakes the same
# export into a linked image (the module-pack image route). The generated probe module is
# scratch output only; it is not the module-pack jdk.m3.text.
#
# usage: verify_export.sh <jdk-21-home> <fresh output dir>
set -u
export MSYS_NO_PATHCONV=1
JDK=$(cygpath -m "$1" 2>/dev/null || echo "$1")
OUT=$(cygpath -m "$2" 2>/dev/null || echo "$2")
EXPORT=java.base/jdk.internal.access=jdk.m3.text
mkdir "$OUT" || { echo "output dir must be fresh: $OUT"; exit 2; }
GATES="$OUT/GATES.tsv"
printf 'gate\texpect\tpattern\trc\tverdict\tlog\n' > "$GATES"
# gate <name> <0|nonzero> <ERE that the log must contain, or -> <cmd...>
gate() {
    local name=$1 expect=$2 pattern=$3; shift 3
    printf '%q ' "$@" > "$OUT/$name.cmd"; echo >> "$OUT/$name.cmd"
    "$@" > "$OUT/$name.log" 2>&1; local rc=$? verdict=FAIL ok=0
    if [ "$expect" = 0 ] && [ $rc -eq 0 ]; then ok=1; fi
    if [ "$expect" = nonzero ] && [ $rc -ne 0 ]; then ok=1; fi
    if [ $ok = 1 ] && { [ "$pattern" = - ] || grep -q -E "$pattern" "$OUT/$name.log"; }; then
        verdict=PASS
    fi
    printf '%s\t%s\t%s\t%s\t%s\t%s\n' "$name" "$expect" "$pattern" "$rc" "$verdict" "$name.log" >> "$GATES"
    echo "HEARTBEAT $(date -u +%FT%TZ) $name rc=$rc expect=$expect verdict=$verdict"
}
mkdir -p "$OUT/src/jdk.m3.text/jdk/m3/text" "$OUT/cp/probe"
printf 'module jdk.m3.text { exports jdk.m3.text; }\n' > "$OUT/src/jdk.m3.text/module-info.java"
cat > "$OUT/src/jdk.m3.text/jdk/m3/text/Probe.java" <<'EOF'
package jdk.m3.text;

public final class Probe {
    private Probe() {}

    public static void main(String[] args) {
        int positives = jdk.internal.access.SharedSecrets.getJavaLangAccess()
                .countPositives(new byte[] {1, 2, -1}, 0, 3);
        System.out.println("PROBE_OK countPositives=" + positives);
    }
}
EOF
cat > "$OUT/cp/probe/Unnamed.java" <<'EOF'
package probe;

public final class Unnamed {
    private Unnamed() {}

    public static void main(String[] args) throws Exception {
        Class<?> secrets = Class.forName("jdk.internal.access.SharedSecrets");
        System.out.println("UNNAMED_REACHED " + secrets.getMethod("getJavaLangAccess").invoke(null));
    }
}
EOF
J="$JDK/bin"
# Module lint must be off for the name itself ("module name component m3 should avoid terminal
# digits"); text-tools/build.py compiles jdk.m3.text with the same -Xlint:all,-module -Werror.
PACK_LINT=-Xlint:all,-module
gate E0_module_name_lint nonzero 'avoid terminal digits' "$J/javac" -Xlint:all -Werror \
    --add-exports "$EXPORT" --module-source-path "$OUT/src" -d "$OUT/mods-lint" --module jdk.m3.text
gate E1_javac_without_export nonzero 'package jdk\.internal\.access is not visible' "$J/javac" \
    "$PACK_LINT" -Werror --module-source-path "$OUT/src" -d "$OUT/mods-x" --module jdk.m3.text
gate E2_javac_with_export 0 - "$J/javac" "$PACK_LINT" -Werror --add-exports "$EXPORT" \
    --module-source-path "$OUT/src" -d "$OUT/mods" --module jdk.m3.text
gate E3_run_without_export nonzero 'does not export jdk\.internal\.access to module jdk\.m3\.text' \
    "$J/java" --module-path "$OUT/mods" -m jdk.m3.text/jdk.m3.text.Probe
gate E4_run_with_export 0 'PROBE_OK countPositives=2' "$J/java" --add-exports "$EXPORT" \
    --module-path "$OUT/mods" -m jdk.m3.text/jdk.m3.text.Probe
# jlink rejects a plugin value that starts with "--" ("no value given for --add-options"); the
# leading space keeps the same VM option string.
gate E5_jlink_add_options 0 - "$J/jlink" --module-path "$JDK/jmods;$OUT/mods" \
    --add-modules jdk.m3.text --add-options " --add-exports=$EXPORT" --output "$OUT/image" \
    --no-header-files --no-man-pages --strip-debug
gate E6_image_run_baked_export 0 'PROBE_OK countPositives=2' "$OUT/image/bin/java" \
    -m jdk.m3.text/jdk.m3.text.Probe
gate E7_image_validate_modules 0 - "$OUT/image/bin/java" --validate-modules
gate E8_unnamed_javac 0 - "$J/javac" -Xlint:all -Werror -d "$OUT/cp-classes" "$OUT/cp/probe/Unnamed.java"
gate E9_image_unnamed_denied nonzero 'does not export jdk\.internal\.access to unnamed module' \
    "$OUT/image/bin/java" -cp "$OUT/cp-classes" probe.Unnamed
rm -rf "$OUT/image"
# Option 1 run-time shape: a qualified export to a module absent from the image is inert.
mkdir -p "$OUT/q/probe.base/probe/base/internal"
printf 'module probe.base { exports probe.base.internal to jdk.m3.text; }\n' \
    > "$OUT/q/probe.base/module-info.java"
printf 'package probe.base.internal;\n\npublic final class Hidden {\n    private Hidden() {}\n}\n' \
    > "$OUT/q/probe.base/probe/base/internal/Hidden.java"
gate E10_qualified_export_javac 0 - "$J/javac" "$PACK_LINT" -Werror --module-source-path "$OUT/q" \
    -d "$OUT/qmods" --module probe.base
gate E11_jlink_target_absent 0 - "$J/jlink" --module-path "$JDK/jmods;$OUT/qmods" \
    --add-modules probe.base --output "$OUT/qimage" --no-header-files --no-man-pages --strip-debug
gate E12_validate_target_absent 0 - "$OUT/qimage/bin/java" --validate-modules
gate E13_describe_target_absent 0 'qualified exports probe\.base\.internal to jdk\.m3\.text' \
    "$OUT/qimage/bin/java" --describe-module probe.base
rm -rf "$OUT/qimage"
grep -h -E 'PROBE_OK|IllegalAccess|not visible|does not export|UNNAMED_REACHED|qualified exports' \
    "$OUT"/E*.log | sort -u
cat "$GATES"
