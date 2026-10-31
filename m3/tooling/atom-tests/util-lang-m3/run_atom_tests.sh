#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
#
# run_atom_tests.sh <worktree> <api_src_root> <out_dir> [include_regex]
#   Runs the generated per-atom JUnit tests on a stock JDK 21 without a JDK build:
#   1. stages this group's M3 sources (from <worktree>) plus the new M3JDK API files they need
#      (from <api_src_root>, read-only) and compiles them with -implicit:none, so modified core
#      classes (String, AbstractStringBuilder, Collections, JavaLangAccess) are API-only and are
#      never patched into the stock runtime;
#   2. compiles the tests against JUnit 5 with --patch-module java.base=<classes>;
#   3. runs the JUnit console launcher with -ea -esa (jtreg defaults) and the --add-opens the jtreg
#      @modules lines declare; writes JUnit XML under <out_dir>/reports and a one-line summary.
set -euo pipefail
WT=$1; API=$2; OUT=$3; INCLUDE=${4:-'.*AtomsTest'}
JDK=${JDK:-"C:/Program Files/Java/jdk-21"}
JUNIT=${JUNIT:-"$HOME/.m2/repository/org/junit/platform/junit-platform-console-standalone/1.11.4/junit-platform-console-standalone-1.11.4.jar"}
SRC=src/java.base/share/classes
GROUP_GLOBS="java/lang/M3*.java java/util/M3*.java java/util/regex/M3*.java jdk/internal/m3/*.java jdk/internal/m3/io/*.java jdk/internal/misc/M3*.java jdk/internal/util/M3*.java"
NEW_API="java/lang/ObjectEvent.java java/lang/ObjectSubscription.java java/lang/ObjectChangeListener.java java/util/Sorts.java java/util/SortAlgorithm.java java/util/concurrent/ProgressMonitor.java java/util/concurrent/ProgressMonitors.java"
API_ONLY="java/lang/String.java java/lang/AbstractStringBuilder.java java/util/Collections.java jdk/internal/access/JavaLangAccess.java"
TEST_DIRS="test/jdk/java/util/M3Atoms test/jdk/java/util/regex/M3Atoms test/jdk/java/lang/M3Atoms test/jdk/jdk/internal/m3/atoms test/jdk/jdk/internal/util/M3Atoms"
OPENS="java.lang java.util java.util.regex java.util.concurrent jdk.internal.m3 jdk.internal.m3.io jdk.internal.util jdk.internal.misc jdk.internal.mindex jdk.internal.access"

rm -rf "$OUT/stage" "$OUT/classes" "$OUT/testclasses" "$OUT/reports"
mkdir -p "$OUT/stage" "$OUT/classes" "$OUT/testclasses" "$OUT/reports" "$OUT/logs"
(cd "$WT/$SRC" && cp --parents $GROUP_GLOBS "$OUT/stage/")
(cd "$API/$SRC" && cp --parents $NEW_API $API_ONLY jdk/internal/mindex/*.java "$OUT/stage/")
(cd "$OUT/stage" && find . -name '*.java' | sort | grep -v -F -x -f <(printf './%s\n' $API_ONLY) > "$OUT/explicit.lst")
echo "stage: $(wc -l < "$OUT/explicit.lst") explicit sources"
(cd "$OUT/stage" && "$JDK/bin/javac" -J-Xmx2g -d "$OUT/classes" --patch-module java.base=. -implicit:none \
    -XDignore.symbol.file -Xlint:none -nowarn @"$OUT/explicit.lst") > "$OUT/logs/javac-prod.log" 2>&1 \
    || { cat "$OUT/logs/javac-prod.log"; exit 1; }
EXPORTS=(); for p in $OPENS; do EXPORTS+=(--add-exports "java.base/$p=ALL-UNNAMED"); done
OPENFLAGS=(); for p in $OPENS; do OPENFLAGS+=(--add-opens "java.base/$p=ALL-UNNAMED"); done
TESTS=$(cd "$WT" && for d in $TEST_DIRS; do if [ -d "$d" ]; then find "$d" -name '*.java'; fi; done | sort)
echo "tests: $(echo "$TESTS" | grep -c . || true) sources"
(cd "$WT" && "$JDK/bin/javac" -d "$OUT/testclasses" --patch-module "java.base=$OUT/classes" "${EXPORTS[@]}" \
    -cp "$JUNIT" -Xlint:none -nowarn $TESTS) > "$OUT/logs/javac-test.log" 2>&1 \
    || { cat "$OUT/logs/javac-test.log"; exit 1; }
set +e
"$JDK/bin/java" -ea -esa -Djava.awt.headless=true --patch-module "java.base=$OUT/classes" "${OPENFLAGS[@]}" \
    -jar "$JUNIT" execute --class-path "$OUT/testclasses" --scan-class-path --include-classname "$INCLUDE" \
    --reports-dir "$OUT/reports" --disable-banner --details=none --disable-ansi-colors \
    > "$OUT/logs/junit.log" 2>&1
rc=$?
set -e
python - "$OUT/reports" <<'PY'
import glob, sys, xml.etree.ElementTree as ET
t = f = e = s = 0
for path in glob.glob(sys.argv[1] + "/TEST-*.xml"):
    for suite in ET.parse(path).getroot().iter("testsuite"):
        t += int(suite.get("tests", 0)); f += int(suite.get("failures", 0))
        e += int(suite.get("errors", 0)); s += int(suite.get("skipped", 0))
print(f"junit tests={t} failures={f} errors={e} skipped={s}")
PY
exit $rc
