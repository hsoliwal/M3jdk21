#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
"""land_edits.py <repo> : the one-off edits that come with JACKSON_NUMBERS_TO_JDK v1 once Jackson no longer uses
FastDoubleParser. The recipe (m3/tooling/jdk-superset/number-parse/tools/jackson_numbers_to_jdk.py) owns the
donor overlays and the pack lock; this script owns the rest. Each edit asserts its anchor once, or that it is
already applied, so a second run changes nothing. Removing the four FastDoubleParser trees and the pack's
legal/fastdoubleparser-1.0.90 copy, and moving m3/tooling/fdp-delegate to m3/evidence/java-base/fdp-delegate,
are tree operations recorded in NUMBER_PARSE_JACKSON.md."""
import json
from pathlib import Path
import sys

repo = Path(sys.argv[1])
PACK = "m3/tooling/module-packs/jackson/"


def edit(rel, old, new):
    path = repo / rel
    text = path.read_bytes().decode("utf-8")
    if text.count(old) == 1:
        text = text.replace(old, new)
    elif new not in text or (old and old in text and old not in new):
        raise SystemExit(f"{rel}: anchor found {text.count(old)} times")
    path.write_bytes(text.encode("utf-8"))
    print("EDIT", rel)


for version in ("0.9.0", "1.0.0", "1.0.90"):
    edit("m3/vendor/third_party/pom.xml", f"    <module>fastdoubleparser-{version}</module>\n", "")
edit(".github/workflows/m3sdk-jmod-receiver.yml", "      - 'm3/vendor/third_party/fastdoubleparser-1.0.90/**'\n", "")
edit(PACK + "source-build/pom.xml", "    <module>sources/fastdoubleparser-1.0.90</module>\n", "")
edit(PACK + "build.py",
     '    ("fastdoubleparser-1.0.90", "ch.randelshofer", "fastdoubleparser", "1.0.90-m3-source-1", None),\n', "")
edit(PACK + "build.py",
     '            required = {\n'
     '                "META-INF/versions/9/module-info.class",\n'
     '                "com/fasterxml/jackson/core/internal/shaded/fdp/v2_19_2/JavaDoubleParser.class",\n'
     '                "META-INF/LICENSE", "META-INF/NOTICE",\n'
     '            }\n'
     '            if any(entry.startswith("ch/randelshofer/fastdoubleparser/") for entry in entries):\n'
     '                raise ValueError("Unshaded FastDoubleParser classes in Jackson core")\n',
     '            required = {"META-INF/versions/9/module-info.class", "META-INF/LICENSE", "META-INF/NOTICE"}\n'
     '            # JACKSON_NUMBERS_TO_JDK v1: Jackson core parses numbers with java.base, so no parser copy ships.\n'
     '            if any("fastdoubleparser/" in entry or "/shaded/fdp/" in entry for entry in entries):\n'
     '                raise ValueError("FastDoubleParser classes in Jackson core; java.base parses its numbers")\n')
edit(PACK + "build.py",
     '        else:\n'
     '            required = {"META-INF/versions/11/module-info.class",\n'
     '                        "META-INF/versions/17/module-info.class", "META-INF/versions/21/module-info.class",\n'
     '                        "META-INF/LICENSE", "META-INF/NOTICE", "META-INF/thirdparty-LICENSE"}\n',
     '        else:\n'
     '            raise ValueError("No built-JAR contract for donor " + name)\n')
edit(PACK + "legal/NOTICE.txt",
     "Jackson core's existing Maven build shades the existing source inclusion of\n"
     "Werner Randelshofer's FastDoubleParser 1.0.90. Its MIT license, original notices,\n"
     "and third-party license texts are preserved independently. The build uses its\n"
     "existing JDK 11, 17 and 21 overlays; JDK 22-only overlays are outside this JDK 21\n"
     "package.\n",
     "Jackson core parses numbers with java.base (Double.parseDouble, Float.parseFloat,\n"
     "new BigDecimal, new BigInteger) through one recorded M3 source overlay,\n"
     "JACKSON_NUMBERS_TO_JDK v1; it no longer bundles Werner Randelshofer's\n"
     "FastDoubleParser. M3JDK's java.base carries that parser's algorithm under its MIT\n"
     "license, with the notice in java.base's legal/fastdoubleparser.md. The notices that\n"
     "the published Jackson core JAR carries stay unchanged among its resources.\n")
edit(PACK + "legal/NOTICE.txt", "existing, unchanged M3JDK21 source inclusions at version 2.19.2",
     "existing M3JDK21 source inclusions at version 2.19.2")
edit(PACK + "legal/M3-JACKSON-NOTICE.txt",
     "Jackson annotations/core/databind and FastDoubleParser retain their original\n",
     "Jackson annotations/core/databind retain their original\n")
edit(PACK + "README.md",
     "annotations/core/databind 2.19.2 and FastDoubleParser 1.0.90. `SOURCE_LOCK.json`\n"
     "pins their complete source/build/legal trees. Their exact Maven POMs perform the\n"
     "source builds, FastDoubleParser shading and multi-release compilation in fresh\n"
     "staging directories. No upstream source or POM is patched by this build. The\n"
     "FastDoubleParser 1.0.90 tree carries two recorded M3 overlays: its\n"
     "`JavaDoubleParser` and `JavaFloatParser` facades delegate to java.base\n"
     "(`m3/tooling/fdp-delegate`), so Jackson's `USE_FAST_DOUBLE_PARSER` path and the\n"
     "JDK use one parser; its BigDecimal/BigInteger parsers are unchanged.\n",
     "annotations/core/databind 2.19.2. `SOURCE_LOCK.json` pins their complete\n"
     "source/build/legal trees. Their exact Maven POMs perform the source builds and\n"
     "multi-release compilation in fresh staging directories. No upstream source or\n"
     "POM is patched by this build. Jackson core carries one recorded M3 overlay,\n"
     "`JACKSON_NUMBERS_TO_JDK v1` (`m3/tooling/jdk-superset/number-parse`): its\n"
     "`NumberInput`, `BigDecimalParser` and `BigIntegerParser` call `Double.parseDouble`,\n"
     "`Float.parseFloat`, `new BigDecimal` and `new BigInteger`, so Jackson and the JDK\n"
     "use one number parser, the one in java.base.\n")
edit(PACK + "README.md",
     "FastDoubleParser is built from source and shaded by Jackson core's original\n"
     "build configuration; it is not installed as a competing runtime module.\n",
     "No FastDoubleParser copy is built, shaded or installed: java.base parses the\n"
     "numbers.\n")
edit(PACK + "INTEGRATION.md",
     "| Existing streaming parser/generator and shaded FastDoubleParser |",
     "| Existing streaming parser/generator; numbers parse in java.base (JACKSON_NUMBERS_TO_JDK v1) |")
edit(PACK + "FOSS_REUSE_DECISION.tsv",
     "Numeric parsing\tFastDoubleParser source already in M3JDK21\t1.0.90 at M3JDK21 "
     "f29b0afe7ad253cc0562bc8dfdd48fc1acec710e\tREUSE_WITH_M3_OVERLAY\tBuild the existing source POM and original "
     "Jackson shading/multi-release closure; the JavaDoubleParser/JavaFloatParser facades are recorded M3 overlays "
     "delegating to java.base (m3/tooling/fdp-delegate)",
     "Numeric parsing\tFastDoubleParser 1.0.90 absorbed in M3JDK java.base (jdk.internal.math.M3*)\t"
     "NUMBER_PARSE_ABSORB v1 and v2\tREUSE_JDK\tJackson core calls Double.parseDouble, Float.parseFloat, "
     "new BigDecimal and new BigInteger (recorded M3 overlay JACKSON_NUMBERS_TO_JDK v1, "
     "m3/tooling/jdk-superset/number-parse); no parser copy is built or shaded")

provenance_path = repo / PACK / "PROVENANCE.json"
provenance = json.loads(provenance_path.read_bytes())
provenance["legal_copies"] = [row for row in provenance["legal_copies"]
                              if not row["target"].startswith("legal/fastdoubleparser-1.0.90/")]
provenance_path.write_bytes((json.dumps(provenance, indent=2) + "\n").encode("utf-8"))
print("EDIT", PACK + "PROVENANCE.json")
