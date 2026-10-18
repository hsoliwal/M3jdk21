#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Catalogue immutable verification inputs for subsequent Maven recipe receiving.

This stages recipe inputs outside every receiving repository. It never changes
production files, tests, recipe engines, or existing verification receipts.
"""
from pathlib import Path
import argparse
import hashlib
import json
import xml.etree.ElementTree as ET

PREFIX = "synexia-openrewrite-recipes/recipe-crates/m3-collections-contract-refinement-20261017"


def digest(data):
    return hashlib.sha256(data).hexdigest()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--workspace", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    root, output = args.workspace.resolve(), args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)
    proof = root / "build-proof"
    selected = {}

    def add(source, relative):
        data = source.read_bytes()
        data.decode("utf-8")
        if source.suffix in (".log", ".jtr"):
            relative += ".txt"
        path = PREFIX + "/evidence/runtime-final-image-20261018/" + relative
        if path in selected:
            raise ValueError("duplicate evidence path: " + path)
        selected[path] = {"path": path, "source": str(source), "sha256": digest(data), "bytes": len(data)}

    stems = [
        "runtime-configure", "runtime-configure-with-xrandr", "runtime-images",
        "runtime-images-documentation", "runtime-images-scope-lint", "runtime-images-native-feature",
        "runtime-native-application", "runtime-documentation-application", "runtime-scope-lint-application",
        "runtime-control-application", "runtime-custody-application", "native-feature-guard-application",
        "arrays-m3-image", "arrays-m3-image-online", "collections-m3-image", "collections-m3-image-online",
    ]
    for stem in stems:
        for suffix in (".json", ".log", ".resources.txt"):
            source = proof / (stem + suffix)
            if source.is_file():
                add(source, "build/" + source.name)
    for group in ("public-array-host", "public-progress-host", "public-collection-oracle-host"):
        for source in sorted((proof / group).glob("*")):
            if source.is_file() and source.suffix in (".json", ".log", ".tsv", ".xml", ".txt"):
                add(source, group + "/" + source.name)
    jtreg = proof / "runtime-jtreg"
    for source in [jtreg / "receipt.json", jtreg / "jtreg.log", jtreg / "report/text/summary.txt", *sorted((jtreg / "work/java/lang/Object").glob("*.jtr"))]:
        add(source, "jtreg/" + source.name)
    witnesses = list((jtreg / "work/scratch/0").glob("m3-compatibility-hold-*"))
    if len(witnesses) != 1:
        raise ValueError("exactly one executed compatibility witness is required")
    for source in sorted(witnesses[0].rglob("*")):
        if source.is_file() and source.suffix in (".log", ".tsv"):
            add(source, "compatibility/" + source.relative_to(witnesses[0]).as_posix())
    native = proof / "native-image-execution"
    for source in sorted(native.glob("*")):
        if source.is_file() and source.suffix in (".json", ".log", ".tsv"):
            add(source, "jni/" + source.name)
    summary = {"schema": "M3_FRESH_IMAGE_QUALIFICATION_V1", "runtime_baseline": "4b24824c3aa09d802121447f8970d442a855f064", "collection_baseline": "0efc1719ca5766aead09f4f7c49f5067975bd946", "modules": {}, "universal_object_compatibility": "HOLD: the executed witness demonstrates old interface-default dispatch and source compatibility counterexamples", "jni": "131403 checks; two independent sequential JVM mappings and same-process collision refusal; not concurrent processes", "hosted_ci_claim": False, "phase_promotion": False}
    for module, count in (("arrays", 10), ("collections", 226)):
        receipt = json.loads((proof / (module + "-m3-image-online.json")).read_text())
        if receipt["exit"] != 0 or receipt["tests"] != {"tests": count, "failures": 0, "errors": 0, "skipped": 0} or not receipt["sources_recompiled_by_clean"] or receipt["pomBeforeSha256"] != receipt["pomAfterSha256"]:
            raise ValueError("clean module qualification did not pass: " + module)
        suites = []
        for report in sorted((proof / (module + "-m3-image-online-surefire")).glob("TEST-*.xml")):
            suite = ET.parse(report).getroot()
            suites.append({"name": suite.attrib["name"], "counts": {key: int(suite.get(key, 0)) for key in ("tests", "failures", "errors", "skipped")}, "xml_sha256": digest(report.read_bytes()), "testcases": [{"name": test.get("name"), "classname": test.get("classname")} for test in suite.findall("testcase")]})
        if sum(s["counts"]["tests"] for s in suites) != count:
            raise ValueError("test XML totals disagree")
        summary["modules"][module] = {"receipt": receipt, "suites": suites}
    build = json.loads((proof / "runtime-images-native-feature.json").read_text())
    if build["exit"] != 0:
        raise ValueError("image build did not pass")
    for relative, expected in build["inputs"].items():
        if digest((root / "M3jdk21-runtime" / relative).read_bytes()) != expected:
            raise ValueError("built source drift: " + relative)
    summary["image_inputs"] = build["inputs"]
    for relative in ("src/java.base/share/classes/java/lang/Object.java", "src/java.base/share/classes/java/lang/Runtime.java"):
        summary["image_inputs"][relative] = digest((root / "M3jdk21-runtime" / relative).read_bytes())
    generated = output / "QUALIFICATION.json"
    generated.write_text(json.dumps(summary, indent=2) + "\n")
    add(generated, generated.name)
    readme = output / "README.md"
    readme.write_text("""<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Fresh M3 JDK image qualification

These are executed local Linux x86_64 fastdebug results from the received source
inputs identified in QUALIFICATION.json. The full JDK images target passed with
the original warning-as-error and doclint gates. HotSpot, JNI libraries and CDS
archives were built. Earlier failure receipts remain distinct predecessor
observations; the missing offline Maven clean plugin was subsequently fetched
through the configured repository and both modules were rebuilt from clean.

The six Object jtreg test files passed using jtreg 7.3.1-dev+0, the new image,
`-Xcheck:jni`, and an explicitly supplied stock Java 21 compiler. The compatibility
witness is expected to detect a HOLD: adding Object.toAdapter preempts a legacy
interface default and rejects previously valid source. A passing witness does
not mean universal compatibility passed. Ordinary dual Runnable/Callable
dispatch and checked exception cause identity passed the tested cases.

The existing M3PrimitiveImageCompiler, M3PrimitiveIdentityTest and fixed-arena
probe were used without changing their implementation. The fixture comes from
the existing primitive-address-identity verifier; no third-party fixture is
copied here. JNI reports 131403 checks and two independent sequential JVM arena
mappings at 0x600000000000. Each existing probe also checks collision refusal.
The binaries, class files and generated primitive image are not source custody
inputs; their SHA-256 identities and executed commands are in the receipts.

The original array native-linux module passed 10 JUnit tests plus its existing
CTest target, and the collection module passed 226 JUnit tests, both compiled
from clean and executed with the new M3 JDK. No original POM was changed.
QUALIFICATION.json retains each executed suite/test case and original XML hash.

These results are local qualification, not hosted CI, JCK, whole-SDK completion,
or phase promotion. The String-first rollout and Object compatibility HOLD
remain in force. Reproduce commands using the pinned repository sources and
replace transient workspace/toolchain paths with the local equivalents.
""")
    add(readme, readme.name)
    source = Path(__file__).resolve()
    data = source.read_bytes()
    path = PREFIX + "/authoring/prepare_final_image_evidence.py"
    selected[path] = {"path": path, "source": str(source), "sha256": digest(data), "bytes": len(data)}
    result = {"schema": "M3_CANONICAL_ARTIFACT_INVENTORY_V1", "files": [selected[p] for p in sorted(selected)]}
    (output / "canonical-artifacts.json").write_text(json.dumps(result, indent=2) + "\n")
    print(json.dumps({"files": len(selected), "bytes": sum(f["bytes"] for f in selected.values()), "inventory": str(output / "canonical-artifacts.json")}))


if __name__ == "__main__":
    main()
