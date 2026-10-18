#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Seal runtime custody, compatibility witnesses, owner mapping and catalogue as a recipe."""
import hashlib
import importlib.util
import json
from pathlib import Path
import subprocess
import sys

sys.dont_write_bytecode = True
WORK = Path(__file__).resolve().parents[2]
HERE = WORK / "runtime-refinement"
BASE = "4b24824c3aa09d802121447f8970d442a855f064"
BASELINE = HERE / "pinned-baseline"
OWNER = HERE / "canonical/synexia-openrewrite-recipes"
CRATE = OWNER / "recipe-crates/m3-collections-contract-refinement-20261017"
CANDIDATE = HERE / "candidates/runtime-control"
DOCS_MANIFEST = "m3/compatibility/m3-progress-doclint-20261018.tsv"
DOCS = CRATE / "target/m3jdk21-progress-doclint/candidate"
FUNCTIONAL_MANIFEST = "m3/compatibility/m3-functional-progress-doclint-20261018.tsv"
FUNCTIONAL = CRATE / "target/m3jdk21-functional-doclint/candidate"
SCOPE_MANIFEST = "m3/compatibility/m3-progress-scope-lint-20261018.tsv"
SCOPES = CRATE / "target/m3jdk21-progress-scope-lint/candidate"
RECIPE = "com.synexia.m3jdk.CollectionsContractRefinement20261017"
PACKET = "m3/runtime-integration/runtime-repair-20261018/"
GITHUB_BASE = "https://github.com/hsoliwal/M3jdk21/blob/" + BASE + "/"
CATALOGUE = PACKET + "CATALOGUE.tsv"


def digest(data):
    return hashlib.sha256(data).hexdigest()


def put(relative, value):
    path = CANDIDATE / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(value.encode() if isinstance(value, str) else value)


def tsv(columns, rows):
    if any("\t" in str(cell) or "\n" in str(cell) or "\r" in str(cell) for row in rows for cell in row):
        raise ValueError("unsafe TSV cell")
    return "\t".join(columns) + "\n" + "".join("\t".join(map(str, row)) + "\n" for row in rows)


def append_mapping(source, additions):
    # Locate the existing JSON array with the decoder, then append entries
    # without reformatting or rewriting any original mapping or phase field.
    marker = source.index('"mappings"') + len('"mappings"')
    offset = source.index(":", marker) + 1
    while source[offset].isspace():
        offset += 1
    original, end = json.JSONDecoder().raw_decode(source, offset)
    if len(original) != 59 or not source[:end].endswith("\n  ]"):
        raise ValueError("mapping array shape changed")
    insertion = ",\n" + ",\n".join("\n".join("    " + line for line in
                              json.dumps(row, indent=2, ensure_ascii=False).splitlines()) for row in additions)
    result = source[:end - 4] + insertion + source[end - 4:]
    parsed = json.loads(result)
    expected = json.loads(source)
    expected["mappings"] += additions
    if parsed != expected:
        raise ValueError("mapping changed beyond exact append")
    return result


def main():
    from pinned_baseline import export
    if export() != BASELINE:
        raise ValueError("immutable baseline export mismatch")
    docs_manifest = (DOCS / DOCS_MANIFEST).read_bytes()
    docs_sha = digest(docs_manifest)
    if docs_sha != "7246c16a0e8ec742eba98dac97e24b7fc3947250ad5d2a22f72ac32dcddf0219":
        raise ValueError("qualified documentation successor drift")
    functional_manifest = (FUNCTIONAL / FUNCTIONAL_MANIFEST).read_bytes()
    functional_sha = digest(functional_manifest)
    if functional_sha != "3b62981be6a617b903c559cc31904eca2bffb2b200380f1be57e07a8f634d969":
        raise ValueError("qualified functional documentation successor drift")
    scope_manifest = (SCOPES / SCOPE_MANIFEST).read_bytes()
    scope_sha = digest(scope_manifest)
    if scope_sha != "1a45edfa747a03b3491063648a455083633e7de836e4c46241de104d689eebee":
        raise ValueError("qualified scope annotation successor drift")
    templates = HERE / "authoring/templates"
    targets = {
        "check_object_adapter_progress_receiver_20261017.py": "m3/compatibility/check_object_adapter_progress_receiver_20261017.py",
        "verify_m3_default_template_20261017.py": "m3/compatibility/verify_m3_default_template_20261017.py",
        "test_object_adapter_progress_receiver_20261018.py": "m3/compatibility/test_object_adapter_progress_receiver_20261018.py",
        "M3RuntimeCompatibilityHold.java": "test/jdk/java/lang/Object/M3RuntimeCompatibilityHold.java",
    }
    for name, target in targets.items():
        text = (templates / name).read_text().replace("@DOCS_MANIFEST_SHA256@", docs_sha).replace("@FUNCTIONAL_MANIFEST_SHA256@", functional_sha).replace("@SCOPE_MANIFEST_SHA256@", scope_sha)
        put(target, text)

    mapping = "m3/docs/name-mapping.json"
    mapping_before = (BASELINE / mapping).read_bytes()
    if digest(mapping_before) != "4247c5648cf6a5599f8840a7171f147f5c343bf9b97e7a386eb7f351f978c4f3":
        raise ValueError("current owner map drift")
    common = {"recipe": RECIPE, "recipe_datasets": ["m3jdk21-object-vtable", "m3jdk21-progress-doclint", "m3jdk21-functional-doclint", "m3jdk21-progress-scope-lint", "m3jdk21-runtime-control"],
              "canonical_recipe_repository": "hsoliwal/com.synexia", "catalogue": CATALOGUE,
              "receiving_baseline": BASE, "phase_rule": "STRING phase unchanged; maintenance of already-published owners only"}
    additions = [
        {**common, "source": "Published M3jdk21 #1082 Object self-adaptation; existing Synexia convergence pattern",
         "source_revision": "c8596a624ff7b15b3fe58cbf857eda0852b84402", "target": "java.lang.Object.toAdapter(java.lang.Class)",
         "target_path": "src/java.base/share/classes/java/lang/Object.java",
         "receiver_test": "test/jdk/java/lang/Object/M3RuntimeCompatibilityHold.java",
         "status": "HOLD: no universal source/binary/dispatch compatibility claim; explicit negative witnesses retained",
         "target_rule": "Keep the current published self-if-assignable result, null mismatch, and null-class rejection; this repair changes no Object declarations or bodies."},
        {**common, "source": "Published M3jdk21 #1082 progress SPI and #1091 weighted-child cancellation successor",
         "source_revision": "a5be05424262a3c7cd3ffb65e9de531be8921d98", "target": "java.util.concurrent.ProgressMonitors",
         "target_path": "src/java.base/share/classes/java/util/concurrent/ProgressMonitors.java",
         "public_spi": "java.util.concurrent.ProgressMonitor", "receiver_test": "test/jdk/java/lang/Object/M3MonitoredTemplateTest.java",
         "status": "Existing target-owned API; documentation successor preserves executable bytecode; current-source custody checked through all five manifests",
         "target_rule": "Borrowed monitor lifetime, thread-local binding, atomic supplier publication and local child cancellation stay unchanged. Eclipse bridge com.m3.collections.M3ProgressMonitor remains a separate published SPI."},
        {**common, "source": "Published M3jdk21 #1091 monitored default-template tasks, using existing JDK executors",
         "source_revision": "a5be05424262a3c7cd3ffb65e9de531be8921d98", "target": "java.util.concurrent.MonitoredRunnable and MonitoredCallable",
         "target_path": "src/java.base/share/classes/java/util/concurrent/MonitoredRunnable.java",
         "related_target_path": "src/java.base/share/classes/java/util/concurrent/MonitoredCallable.java",
         "receiver_test": "test/jdk/java/lang/Object/M3RuntimeCompatibilityHold.java",
         "status": "Existing public names retained; ordinary dual Runnable/Callable source and frozen binary behavior have dedicated witnesses",
         "target_rule": "Runnable.run remains void and Callable.call retains its result/checked-exception contract. No TaskStatus recast, duplicate Callable.progressMonitor default, new scheduler or mandatory client base class."},
        {**common, "source": "Published M3jdk21 #1082 monitored function overloads under their existing JDK names",
         "source_revision": "c8596a624ff7b15b3fe58cbf857eda0852b84402", "target": "java.util.function.Consumer, Function and Supplier",
         "target_path": "src/java.base/share/classes/java/util/function/Consumer.java",
         "related_target_paths": ["src/java.base/share/classes/java/util/function/Function.java", "src/java.base/share/classes/java/util/function/Supplier.java"],
         "receiver_test": "test/jdk/java/lang/Object/M3AdapterProgress.java",
         "status": "Existing progress overloads receive Javadoc only; their original single abstract methods and executable bytecode are preserved",
         "target_rule": "No client base class or SAM replacement; monitored overloads borrow their explicit monitor and restore the prior lexical binding."},
        {**common, "source": "Existing OpenJDK HotSpot bootstrap vtable count; matched current published Object source",
         "source_revision": BASE, "source_git_blob": "ab29ec3524d40ff58d6ff3e6fe2c1123b3abc1dd", "target": "ClassLoader::compute_Object_vtable",
         "target_path": "src/hotspot/share/classfile/classLoader.cpp", "source_license": "GPL-2.0-only",
         "status": "Exact count repaired from five to six with the existing vtable consistency guard retained; full fastdebug image is a separate gate",
         "target_rule": "No JNI symbol, object layout, native ownership or guard weakening. Bootstrap consistency does not establish Object API compatibility."},
    ]
    put(mapping, append_mapping(mapping_before.decode(), additions))

    atoms = [
        ("M3-RUNTIME-VTABLE-COUNT", "RUNTIME_BOOTSTRAP", "ClassLoader::compute_Object_vtable", "5 legacy entries versus 6 current non-final Object methods", "update matched constant; duplicate parser; weaken guard", "update matched constant to 6", "matches exact published Object bytes; existing klassVtable guard remains", "No JNI or Object API change; universal compatibility remains HOLD", "O(1)", "O(1)", "No performance claim", "Smallest target-owned correction; duplicating class parsing or relaxing guard would widen the change", GITHUB_BASE + "src/hotspot/share/classfile/classLoader.cpp", "GPL-2.0-only", "Not a challenge-array/search problem; no donor code copied", "Preserve OpenJDK source header and license", "Full fastdebug images plus actual VM witnesses", "Native text recipe qualified; VM behavior requires fresh image", "BOUNDED_REPAIR"),
        ("M3-PROGRESS-JAVADOC", "PUBLIC_API_DOCUMENTATION", "8 Runnable/concurrent plus 3 functional progress owners", "42 concurrent plus 7 functional original public/protected doclint warnings", "complete comments; disable Werror; alter anonymous implementation", "Javadoc-only exact snapshots", "14 -g:none class files in the full 11-source closure are byte-identical; original all/protected doclint has zero warnings", "Public signatures and executable tokens unchanged", "Unchanged", "Unchanged", "No runtime performance claim", "Restore original build gate without suppressions or representation changes", GITHUB_BASE + "make/modules/java.base/Java.gmk", "GPL-2.0-only WITH Classpath-exception-2.0", "Not a challenge algorithm; no external code copied", "Preserve every original source header", "verify_progress_doclint.py; original JDK doclint flags", "Strict all-visibility private/anonymous warnings remain separately retained", "DOCUMENTATION_ONLY"),
        ("M3-PROGRESS-SCOPE-LINT", "BORROWED_SCOPE_LIFETIME", "ProgressMonitors.run/call; Consumer.accept; Function.apply; Supplier.get", "Five original unused try-resource warnings in the 11-source closure", "method-local SOURCE annotation; change try-with-resources to finally; disable build warnings", "five method-local SuppressWarnings(try) annotations", "14 compiled classes remain byte-identical under the exact full JDK lint/doclint flags", "No changes to scope closing, exception suppression, task return values or monitor ownership", "Unchanged", "Unchanged", "No runtime performance claim", "Preserve the existing resource-closing semantics and global Werror gate; reuse established JDK annotation pattern", GITHUB_BASE + "src/java.base/share/classes/java/io/FileDescriptor.java", "GPL-2.0-only WITH Classpath-exception-2.0", "Scope lifetime and compiler warning policy are not challenge-array algorithms", "Existing target source headers preserved", "verify_progress_scope_lint.py; full fastdebug images", "Narrow source-only annotations; original failure retained", "SOURCE_ONLY_ANNOTATIONS"),
        ("M3-PROGRESS-MANIFEST-SUCCESSION", "SOURCE_IDENTITY_CUSTODY", "14 current original/template receiver owners", "Original #1082 checker rejected exact published #1091 Callable", "fixed successor chain; accept either hash; replace old manifest; general dependency graph", "five byte-pinned manifests with explicit overlap sets", "Each successor starts at preceding postimage; all 14 current sources and manifests are required", "No arbitrary old/new mixtures and no rewriting #1082/#1091 history", "O(total bytes hashed + path count)", "O(path count)", "No benchmark claim; bounded 11+5+8+3+4 manifest rows", "Reuse exact hash-pinned source custody and linear transition composition", "https://github.com/hsoliwal/com.synexia/tree/8f8200375d8d403b58e855ee5db02f2e8fac33a8/synexia-openrewrite-recipes/recipe-crates/m3-collections-contract-refinement-20261017", "Apache-2.0 recipe; target licenses retained", "Challenge-platform array/graph templates do not justify generic machinery for this fixed transition", "Original manifests retained byte-exact; new checker is first-party", "python3 m3/compatibility/test_object_adapter_progress_receiver_20261018.py", "SDK and Python regressions qualify current bytes only", "EXPLICIT_SUCCESSOR"),
        ("M3-OBJECT-LEGACY-DISPATCH", "API_COMPATIBILITY", "Object.toAdapter and existing interface defaults", "Published Object adds a public non-final instance method", "assert additive compatibility; frozen binary and source counterexamples", "frozen Java21 binaries plus candidate compiler witnesses", "Check invokeinterface dispatch separately from source collision and Optional-return binary compatibility", "A passing test records universal_no_break=FAIL_COUNTEREXAMPLES_OBSERVED", "Test only", "Test only", "No performance claim", "JLS source/default rules and observable frozen dispatch must both be tested", "https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.4.1.2", "Reference only", "Challenge algorithms cannot establish language or VM compatibility", "Original witness implementation; no external code copied", "M3RuntimeCompatibilityHold with candidate image and stock Java21", "Fresh current-image execution required; lost prior evidence not reused", "COMPATIBILITY_HOLD"),
    ]
    put(CATALOGUE, tsv(["atom_id", "problem_category", "target_owner", "observed_input", "candidates", "selection", "correctness", "contract_boundary", "time_complexity", "space_complexity", "performance_result", "selection_rationale", "primary_reference", "license", "challenge_discovery", "license_action", "validation", "evidence_scope", "status"], atoms))
    central = "m3/backports/ALGORITHM_CATALOGUE.tsv"
    central_before = (BASELINE / central).read_text()
    if not central_before.endswith("\n"):
        raise ValueError("catalogue termination drift")
    extra = []
    for atom in atoms:
        extra.append((atom[0], atom[1], atom[5], atom[-1], "FILE", atom[2], "NOT_APPLICABLE", "NOT_APPLICABLE", "NOT_APPLICABLE", atom[12], atom[13], "EXISTING_OWNER_OR_FIRST_PARTY", CATALOGUE))
    if any(row[0] in central_before for row in extra):
        raise ValueError("duplicate catalogue atom")
    put(central, central_before + "".join("\t".join(row) + "\n" for row in extra))

    review_rows = []
    native_receipt = json.loads((HERE / "authoring/native-candidate-receipt.json").read_text())
    review_rows.append((1, native_receipt["path"], native_receipt["before_sha256"], native_receipt["after_sha256"], "RUNTIME_BOOTSTRAP", "M3-RUNTIME-VTABLE-COUNT", "6 matched Object virtual methods; unchanged guard", "Exact native text dataset; full image separately gated"))
    for raw in (docs_manifest + functional_manifest).decode().splitlines():
        if raw and not raw.startswith("#"):
            path, before, after, _ = raw.split("\t")
            review_rows.append((len(review_rows) + 1, path, before, after, "PUBLIC_API_DOCUMENTATION", "M3-PROGRESS-JAVADOC", "Public lifecycle/parameters/results/errors documented; code preserved", "JavaLST recipe plus zero build-scope doclint and 14 identical classes in the full 11-source closure"))
    for raw in scope_manifest.decode().splitlines():
        if raw and not raw.startswith("#"):
            path, before, after, _ = raw.split("\t")
            review_rows.append((len(review_rows) + 1, path, before, after, "BORROWED_SCOPE_LIFETIME", "M3-PROGRESS-SCOPE-LINT", "Method-local source annotations; try-with-resources code unchanged", "Exact full-lint gate and 14 identical class files"))
    for path in sorted(targets.values()):
        before = digest((BASELINE / path).read_bytes()) if (BASELINE / path).exists() else "ABSENT"
        review_rows.append((len(review_rows) + 1, path, before, digest((CANDIDATE / path).read_bytes()), "SOURCE_IDENTITY_CUSTODY" if path.endswith(".py") else "API_COMPATIBILITY", "M3-PROGRESS-MANIFEST-SUCCESSION" if path.endswith(".py") else "M3-OBJECT-LEGACY-DISPATCH", "Explicit bounded successor or compatibility witness", "Fresh qualified recipe output; targeted regression/image evidence"))
    put(PACKET + "FILE_REVIEW.tsv", tsv(["pass", "path", "before_sha256", "after_sha256", "category", "catalogue_atom", "decision", "validation_scope"], review_rows))
    custody = {"schema": "M3_RUNTIME_CURRENT_OWNER_CUSTODY_V1", "receiving_baseline": BASE,
        "canonical_recipe_baseline": "8f8200375d8d403b58e855ee5db02f2e8fac33a8", "recipe": RECIPE,
        "datasets": common["recipe_datasets"], "recipe_runtime_dependency": False,
        "historical_manifests": {"m3/compatibility/m3-object-adapter-progress-20261017.tsv": "2f581a791adc7ecf7f217a6276f0da8af424c1e3e49385ca6ae44704d3319c82", "m3/compatibility/m3-default-template-source-seal.tsv": "c431bf732eb91a289b4bcd0b886db71d43da7d7bb17074cd6116db6b37e73ad6"},
        "documentation_successor": {DOCS_MANIFEST: docs_sha, FUNCTIONAL_MANIFEST: functional_sha}, "scope_annotation_successor": {SCOPE_MANIFEST: scope_sha}, "current_object_sha256": native_receipt["object_sha256"],
        "native_before_sha256": native_receipt["before_sha256"], "native_after_sha256": native_receipt["after_sha256"],
        "historical_evidence": "Lost unpublished runtime44/jtreg evidence is not recovered or applied to these bytes",
        "compatibility_policy": "HOLD: Object source and interface-default dispatch counterexamples must remain visible",
        "phase_policy": "STRING unchanged; no broad SDK/JNI admission",
        "baseline_binding": "Inputs exported from immutable Git objects; a prior working-tree catalogue mismatch was refused before receiving writes", "full_image_evidence": "A separate fresh gate recorded by the receiving PR"}
    put(PACKET + "CURRENT_OWNERS.json", json.dumps(custody, indent=2) + "\n")
    put(PACKET + "README.md", '''<!--
SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
SPDX-License-Identifier: Apache-2.0
-->
# Bounded runtime repair and compatibility hold

The current receiving baseline is `4b24824c3aa09d802121447f8970d442a855f064`.
Inputs are exported from immutable Git objects. A prior shared-working-tree
catalogue differed from this commit; the receiver refused that candidate before
writing anything. Its source-binding failure is retained separately.
This maintenance pass uses the existing Synexia Maven/OpenRewrite snapshot host
and four unmodified pinned engines. Native, documentation and control changes
are distinct datasets of `com.synexia.m3jdk.CollectionsContractRefinement20261017`.
There is no Synexia or OpenRewrite runtime dependency.

## Product changes and owner mapping

`ClassLoader::compute_Object_vtable` changes its matching count from five to six
for the already-published non-final `Object.toAdapter` method. The existing
`klassVtable` mismatch guard is retained. There is no new JNI symbol, object
layout or native ownership. `Object` itself is unchanged by this repair.

Eight published Runnable/progress API owners and three functional overload owners receive Javadoc only. Strict Java
21 compilation uses the repository's existing `-Xdoclint:all/protected -Werror`
visibility scope. The eleven emitted sources produce fourteen byte-identical class files
against the original sources when debug metadata is omitted. All-visibility
private/anonymous-constructor documentation failures are retained separately;
this pass does not claim that wider documentation scope is qualified.

The full compiler gate additionally detects five intentional unused scope
resources. Five method-local `@SuppressWarnings("try")` annotations in four
owners preserve the try-with-resources bodies and their exception-suppression
semantics. The exact `-Xlint:all -Xlint:-this-escape -Werror` build flags and
public/protected doclint then pass with fourteen unchanged class files. The
existing JDK `FileDescriptor` uses the same local annotation pattern; global
warning policy is unchanged.

The owner map appends Object adaptation, the progress SPI, monitored templates
the functional progress overloads, and the matched native vtable owner. The existing Eclipse forwarder uses the
separate `com.m3.collections.M3ProgressMonitor` SPI; this pass does not conflate
that SPI with `java.util.concurrent.ProgressMonitor`.

## Immutable source succession

The original #1082 eleven-row manifest and #1091 five-row manifest remain
byte-exact. The documentation successors are new eight-row and three-row manifests.
The receiver checker composes only those manifests and a four-row source-annotation successor: two original
owners are replaced and three template owners are added by #1091, followed by
eight concurrent and three functional documentation replacements, then four exact annotation-source replacements. It requires the resulting fourteen
current sources, rejects arbitrary drift or old/new mixtures, validates path
containment and symlinks, and performs no receiving writes. The default-template
checker shares that current-closure verification while retaining its five owners.

Run both receiving checks and the isolated negative regressions:

```sh
python3 m3/compatibility/check_object_adapter_progress_receiver_20261017.py
python3 m3/compatibility/verify_m3_default_template_20261017.py
python3 m3/compatibility/test_object_adapter_progress_receiver_20261018.py
```

## Object compatibility remains on hold

Adding this public Object method cannot be described as universally compatible.
The `M3RuntimeCompatibilityHold` jtreg driver deliberately distinguishes a
passing witness from a compatibility pass. It checks current self-adaptation,
then compiles legacy fixtures using an unmodified Java 21 compiler, records the
class hashes and `invokeinterface` bytecode, and runs those frozen classes on
both stock and candidate images. It separately records changed interface-default
dispatch, source collision diagnostics, Optional-return binary behavior, and
ordinary dual Runnable/Callable results and checked-exception identity.

Supply a genuine stock Java 21 toolchain using `-Dm3.stock.jdk`. Compile and run
the driver with the actual rebuilt candidate image. When invoking jtreg, pass
that system property through the test VM options. The driver uses a new evidence
directory, bounds each subprocess and keeps individual compiler/VM logs.
A successful witness writes `universal_no_break=FAIL_COUNTEREXAMPLES_OBSERVED`.
The candidate-source probes intentionally omit `--release`, so they see the
candidate Object API instead of historical `ct.sym` symbols.

The relevant language boundaries are documented in [JLS 9.4.1.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.4.1.2)
and [JLS 13.4.6](https://docs.oracle.com/javase/specs/jls/se21/html/jls-13.html#jls-13.4.6).
Neither source identity checks nor recipe passes replace VM execution, JNI,
JCK or full SDK qualification. Lost unpublished runtime proof is not reused.

## Catalogue and reusable verification

`CATALOGUE.tsv` records problem categories, evaluated candidates, complexity,
license and selection rationale. `FILE_REVIEW.tsv` binds the serial native and
Java source pass to exact before/after identities. The central algorithm
catalogue indexes this packet. Challenge-platform algorithms are marked
inapplicable for these runtime, documentation and source-custody problems;
no unrelated code or performance claims are imported.

Qualify each dataset with the canonical capped Maven host, its five actual SDK
tests and its input-sealing and receiver integrity profile. Apply only its
qualified retained SDK results using the unchanged `receive.py`, first with
`--check`, then apply, then repeat to prove a fixed point. Documentation requires
the additional exact-bytecode/doclint comparison; runtime witnesses and the
full fastdebug image build remain separate receiving gates. The receiving PR
must retain the fresh results and earlier failed attempts with their sources.
STRING phase and all wider compatibility/admission holds remain unchanged.
''')
    spec = importlib.util.spec_from_file_location("control_prepare", CRATE / "prepare.py")
    prepare = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(prepare)
    export([str(p.relative_to(CANDIDATE)) for p in CANDIDATE.rglob("*") if p.is_file()])
    metadata = prepare.seal(CRATE, "m3jdk21-runtime-control", "hsoliwal/M3jdk21", BASELINE,
                            [CANDIDATE], BASE, OWNER)
    receipt = {"schema": "M3_RUNTIME_CONTROL_CANDIDATE_V1", "baseline": BASE,
               "docs_manifest_sha256": docs_sha, "functional_manifest_sha256": functional_sha, "scope_manifest_sha256": scope_sha, "metadata": metadata,
               "map_previous_entries": 59, "map_appended_entries": len(additions),
               "receiving_source_written": False, "Object_universal_compatibility": "HOLD"}
    (HERE / "authoring/runtime-control-candidate-receipt.json").write_text(json.dumps(receipt, indent=2) + "\n")
    print(json.dumps(receipt, sort_keys=True))


if __name__ == "__main__":
    main()
