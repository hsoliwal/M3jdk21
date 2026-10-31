#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Admit exact lost runtime states and seal a current-baseline restoration recipe.

This prepares recipe inputs only. The unchanged Maven/OpenRewrite owners emit
receiving files after qualification. Earlier source datasets and evidence are
read-only inputs; a merged commit is never substituted for file identity.
"""

import argparse
from collections import defaultdict
import hashlib
import importlib.util
import json
from pathlib import Path
import re
import subprocess


TASK = "m3-collections-contract-refinement-20261017"
DATASET = "m3jdk21-runtime-restore-07ba"
CURRENT = "07ba6e383343d32c83cfdaf641387bbe72bf325d"
DELIVERED = "86528a90079b89a50b51ef13da08e7e70d468cb4"
CATALOGUE = "m3/backports/ALGORITHM_CATALOGUE.tsv"
CURRENT_CATALOGUE = "50e6381eed8e06fe9c073298b8a4cf20ad853df096887c6c8510a40bedce6569"
OBJECT = "src/java.base/share/classes/java/lang/Object.java"
OBJECT_SHA = "829fb37093cf3e8d11b3c1e3d42f898f0c86f3b697e7a66cefe09934be3bb259"
METADATA = "m3/runtime-integration/merged-repair-recovery-20261018"
ORIGINAL_DATASETS = (
    "m3jdk21-object-vtable", "m3jdk21-progress-doclint",
    "m3jdk21-functional-doclint", "m3jdk21-strings-doclint",
    "m3jdk21-progress-scope-lint", "m3jdk21-native-feature-guard",
    "m3jdk21-runtime-control",
)


def sha(data):
    return hashlib.sha256(data).hexdigest()


def json_bytes(value):
    return (json.dumps(value, indent=2) + "\n").encode("utf-8")


def git(repo, *args):
    result = subprocess.run(["git", "-C", str(repo), *args], check=True,
                            stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    return result.stdout


def read_tree(repo, revision, paths):
    result = {}
    for record in git(repo, "ls-tree", "-rz", "--full-tree", revision, "--", *paths).split(b"\0"):
        if not record:
            continue
        metadata, raw_path = record.split(b"\t", 1)
        mode, kind, blob = metadata.decode("ascii").split()
        path = raw_path.decode("utf-8")
        if kind != "blob" or mode not in ("100644", "100755") or path not in paths:
            raise ValueError("nonregular or unexpected Git input: " + path)
        data = git(repo, "cat-file", "blob", blob)
        if hashlib.sha1(b"blob " + str(len(data)).encode("ascii") + b"\0" + data).hexdigest() != blob:
            raise ValueError("Git blob identity mismatch")
        result[path] = {"bytes": data, "git_blob": blob, "mode": int(mode, 8) & 0o777}
    return result


def verify_chain(chain):
    if not chain:
        raise ValueError("empty original recipe chain")
    for index, entry in enumerate(chain):
        if sha(entry["postimage"]) != entry["after"]:
            raise ValueError("original postimage drift")
        expected = "ABSENT" if entry["preimage"] is None else sha(entry["preimage"])
        if expected != entry["before"]:
            raise ValueError("original preimage drift")
        if index and chain[index - 1]["after"] != entry["before"]:
            raise ValueError("noncontiguous original recipe chain")
    return chain[-1]["postimage"]


def restore_exact(current, chain):
    postimage = verify_chain(chain)
    current_hash = "ABSENT" if current is None else sha(current)
    if current_hash != chain[0]["before"]:
        raise ValueError("current source is not the admitted original preimage")
    return postimage


def append_missing_rows(current, original_before, original_after):
    """Keep current bytes and append only exact missing historical catalogue rows."""
    if not original_after.startswith(original_before) or not current.startswith(original_before):
        raise ValueError("catalogue prefix diverged from the reviewed original")
    if not current.endswith(b"\n") or not original_before.endswith(b"\n"):
        raise ValueError("catalogue does not end at a complete row")
    current_rows = {}
    for row in current.splitlines(keepends=True):
        if not row.strip() or row.startswith(b"#"):
            continue
        key = row.split(b"\t", 1)[0]
        if key in current_rows:
            raise ValueError("duplicate current catalogue key")
        current_rows[key] = row
    appended = []
    for row in original_after[len(original_before):].splitlines(keepends=True):
        if not row.strip() or row.startswith(b"#"):
            raise ValueError("unexpected original catalogue delta")
        key = row.split(b"\t", 1)[0]
        if key in current_rows:
            if current_rows[key] != row:
                raise ValueError("catalogue owner collision")
            continue
        current_rows[key] = row
        appended.append(row)
    return current + b"".join(appended), len(appended)


def load_originals(crate):
    chains = defaultdict(list)
    bindings = []
    for dataset in ORIGINAL_DATASETS:
        root = crate / "datasets" / dataset
        metadata = json.loads((root / "DATASET.json").read_text())
        if metadata["dataset"] != dataset or metadata["engine_changes_from_origin"]:
            raise ValueError("original dataset owner changed")
        modes = dict(line.split("\t") for line in (root / "MODES.tsv").read_text().splitlines() if line)
        for manifest in sorted((root / "resources").rglob("manifest.tsv")):
            manifest_data = manifest.read_bytes()
            bindings.append({"path": manifest.relative_to(crate).as_posix(), "sha256": sha(manifest_data)})
            for line in manifest_data.decode("utf-8").splitlines():
                if not line or line.startswith("#"):
                    continue
                target, before, after, template = line.split("\t")
                if not re.fullmatch(r"[a-f0-9]{64}|ABSENT", before) or not re.fullmatch(r"[a-f0-9]{64}", after):
                    raise ValueError("invalid original manifest digest")
                post_path = manifest.parent / template
                pre_path = manifest.parent / "pre" / template
                if post_path.is_symlink() or (before != "ABSENT" and pre_path.is_symlink()):
                    raise ValueError("symlink original recipe payload")
                chains[target].append({"dataset": dataset, "before": before, "after": after,
                                       "preimage": None if before == "ABSENT" else pre_path.read_bytes(),
                                       "postimage": post_path.read_bytes(), "mode": int(modes[target], 8),
                                       "manifest": manifest.relative_to(crate).as_posix(),
                                       "template": post_path.relative_to(crate).as_posix()})
    if len(chains) != 27:
        raise ValueError("original bounded target scope changed")
    for chain in chains.values():
        verify_chain(chain)
    return chains, bindings


def write(path, data, mode=0o644):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)
    path.chmod(mode)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ("baseline-root", "recipe-owner", "work-dir"):
        parser.add_argument("--" + name, type=Path, required=True)
    args = parser.parse_args()
    repo, owner, work = args.baseline_root.resolve(), args.recipe_owner.resolve(), args.work_dir.resolve()
    if git(repo, "rev-parse", "HEAD").decode().strip() != CURRENT:
        raise ValueError("current receiving checkout is not the reviewed commit")
    crate = owner / "recipe-crates" / TASK
    chains, bindings = load_originals(crate)
    paths = sorted(chains)
    current = read_tree(repo, CURRENT, paths + [OBJECT])
    delivered = read_tree(repo, DELIVERED, paths + [OBJECT])
    if sha(current[OBJECT]["bytes"]) != OBJECT_SHA or current[OBJECT]["bytes"] != delivered[OBJECT]["bytes"]:
        raise ValueError("Object owner changed; the existing no-break HOLD must be reevaluated")
    work.mkdir(parents=True, exist_ok=False)
    baseline, candidate = work / "baseline", work / "candidate-input"
    baseline.mkdir()
    candidate.mkdir()
    admissions = []
    for target in paths:
        chain = chains[target]
        final = verify_chain(chain)
        if target not in delivered or delivered[target]["bytes"] != final:
            raise ValueError("original source recipe does not match its published delivery")
        entry = current.get(target)
        before = None if entry is None else entry["bytes"]
        live = repo / target
        if live.is_symlink() or (live.read_bytes() if live.is_file() else None) != before:
            raise ValueError("current worktree differs from the pinned Git input: " + target)
        if target == CATALOGUE:
            if sha(before) != CURRENT_CATALOGUE:
                raise ValueError("unreviewed current catalogue")
            after, appended = append_missing_rows(before, chain[0]["preimage"], final)
            if appended != 5 or b"ALG-NUMERIC-COPY-RUNS\t" not in before:
                raise ValueError("reviewed current catalogue union changed")
            disposition = "PRESERVE_CURRENT_PREFIX_APPEND_FIVE_EXACT_REPAIR_ROWS"
        else:
            after = restore_exact(before, chain)
            disposition = "RESTORE_EXACT_ORIGINAL_PREIMAGE_TO_DELIVERED_POSTIMAGE"
        if before is not None:
            write(baseline / target, before, entry["mode"])
        write(candidate / target, after, chain[-1]["mode"])
        admissions.append({"path": target, "current_sha256": "ABSENT" if before is None else sha(before),
                           "current_git_blob": None if entry is None else entry["git_blob"],
                           "original_preimage_sha256": chain[0]["before"],
                           "delivered_sha256": sha(final), "delivered_git_blob": delivered[target]["git_blob"],
                           "candidate_sha256": sha(after), "disposition": disposition,
                           "recipe_chain": [{key: part[key] for key in ("dataset", "before", "after", "manifest", "template")}
                                            for part in chain]})
    ancestry = subprocess.run(["git", "-C", str(repo), "merge-base", "--is-ancestor", DELIVERED, CURRENT],
                              stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if ancestry.returncode not in (0, 1):
        raise ValueError("could not inspect published ancestry")
    diff_paths = git(repo, "diff", "--name-only", "--no-renames", DELIVERED, CURRENT,
                     "--", "src", "make", "configure").decode().splitlines()
    expected_sources = sorted(target for target in paths if target.startswith("src/"))
    if sorted(diff_paths) != expected_sources or len(expected_sources) != 14:
        raise ValueError("OpenJDK build input changes exceed the admitted fourteen reversions")
    admission = {"schema": "M3_CURRENT_RUNTIME_DELIVERY_ADMISSION_V1", "status": "SOURCE_ADMISSION_ONLY",
                 "repository": "hsoliwal/M3jdk21", "baseline": CURRENT, "earlier_delivery": DELIVERED,
                 "earlier_delivery_is_ancestor": ancestry.returncode == 0,
                 "ancestry_is_not_source_delivery": True, "original_target_count": 27,
                 "exact_original_preimages": 26, "preserved_current_catalogue_rows": ["ALG-NUMERIC-COPY-RUNS"],
                 "original_recipe_manifests": bindings, "targets": admissions,
                 "OpenJDK_build_input_diff": diff_paths,
                 "Object": {"path": OBJECT, "sha256": OBJECT_SHA, "changed": False,
                            "universal_no_break": "HOLD_UNCHANGED"},
                 "phase": "STRING_UNCHANGED", "public_interfaces": "NO_NEW_INTERFACE_CHANGE_IN_THIS_RESTORATION",
                 "execution": "NEW_MAVEN_RECEIVING_AND_BUILD_GATES_REQUIRED; EARLIER_EVIDENCE_REMAINS_BOUND_TO_EARLIER_BYTES"}
    write(candidate / METADATA / "ADMISSION.json", json_bytes(admission))
    mapping = {"schema": "M3_CURRENT_RUNTIME_DELIVERY_MAP_V1", "dataset": DATASET,
               "recipe": "com.synexia.m3jdk.CollectionsContractRefinement20261017",
               "baseline": CURRENT, "restored_delivery": DELIVERED,
               "scope": "27 original runtime repair targets; four separate current-delivery metadata files",
               "owner": "Existing JDK/HotSpot/JNI owners and seven original canonical datasets",
               "admission": METADATA + "/ADMISSION.json", "catalogue": METADATA + "/CATALOGUE.tsv",
               "current_catalogue_prefix_preserved": True, "names_and_public_signatures": "UNCHANGED_BY_RESTORATION",
               "Object_no_break": "HOLD_UNCHANGED", "phase": "STRING_UNCHANGED",
               "status": "PENDING_CURRENT_RECEIVING_AND_BUILD_SOURCE_IDENTITY"}
    write(candidate / METADATA / "RECEIVING-MAP.json", json_bytes(mapping))
    catalogue = ("# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project\n"
                 "# SPDX-License-Identifier: Apache-2.0\n"
                 "problem_category\tcandidate\tdecision\tcorrectness_and_compatibility\tcomplexity\tdonor\tqualification\n"
                 "SOURCE_DELIVERY_IDENTITY\tGit ancestry as delivery\tREJECT\tA merged ancestor does not establish current file bytes\tO(1) assertion is insufficient\tCurrent and earlier pinned Git trees\t27 actual target comparisons required\n"
                 "SOURCE_DELIVERY_IDENTITY\texact original recipe postimages\tSELECT\t26 exact old preimages restore the same delivered bytes; four per-file chains are composed without changing owners\tO(total admitted bytes) hashing and comparison\tSeven existing canonical datasets; preserved per-file licenses\tOriginal Maven/OpenRewrite SDK and receiver gates\n"
                 "CATALOGUE_UNION\twhole-file historical replacement\tREJECT\tWould remove the current ALG-NUMERIC-COPY-RUNS row\tO(file bytes)\tCurrent catalogue\tExplicit prefix-preservation witness\n"
                 "CATALOGUE_UNION\tappend five missing exact rows\tSELECT\tPreserve every current byte, compare row identities, refuse conflicting owners\tO(current and original row bytes)\tExisting original runtime catalogue delta\tPrefix, collision, and exact-row checks\n"
                 "DISCOVERY_APPLICABILITY\tLeetCode HackerRank GeeksforGeeks\tNOT_APPLICABLE\tThis pass restores reviewed source identity and metadata; it introduces no new search or collection algorithm\tNOT_APPLICABLE\tPrior algorithm choices and donor reviews remain retained\tNo new performance or algorithm superiority claim\n")
    write(candidate / METADATA / "CATALOGUE.tsv", catalogue.encode())
    readme = """<!--
SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0
-->
# Restore bounded runtime repairs on the current receiving tree

Current commit `07ba6e383343d32c83cfdaf641387bbe72bf325d` has lost the
delivered bytes from `86528a90079b89a50b51ef13da08e7e70d468cb4`. The admission
record compares actual Git blobs and original recipe manifests. A merge or an
ancestor relationship is not evidence that a source change remains delivered.

Of the 27 targets controlled by the seven original runtime recipes, 26 are
exact original preimages, including absent additions. This pass reuses their
exact qualified postimage atoms. The current global algorithm catalogue has
one later numeric-copy row; every current byte is retained and the five missing
runtime rows are appended. The old name map is itself an exact original
preimage, so its restored postimage preserves all current entries.

The fourteen production source files after restoration are byte-identical to
the earlier delivered files: HotSpot's matching Object vtable count, the JNI
feature-macro guard, and the progress/function/Strings strict-build repairs.
There are no changes to allocator error handling, public signatures, or global
build gates. Object itself remains unchanged and its universal compatibility
claim remains HOLD. The ordered STRING phase is unchanged. Runtime facade and
ProgressJob API additions are separate feature datasets and reviews.

The source authorer prepares inputs and invokes the original Maven sealer.
The transformation is `com.synexia.m3jdk.CollectionsContractRefinement20261017`
with dataset `m3jdk21-runtime-restore-07ba`. The original Java and text owners,
five SDK tests, eight sealing cases, nine receiver cases, and capped POM are
reused. No target is copied into a receiving tree before actual SDK
qualification. Current receiving and fixed-point receipts must bind these
inputs; arbitrary source drift is refused.

Earlier build/test receipts retain their earlier source identity. They are not
relabeled as a build of this current commit. A later verification may establish
that the complete current OpenJDK build input set equals the exact earlier
feature-build input set; such a receipt must identify both source sets and the
actual image build workspace. Otherwise a new image build is required. This
preparation record alone claims neither outcome.
"""
    write(candidate / METADATA / "README.md", readme.encode())
    spec = importlib.util.spec_from_file_location("m3_original_prepare", crate / "prepare.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    sealed = module.seal(crate, DATASET, "hsoliwal/M3jdk21", baseline, [candidate], CURRENT, owner)
    if sealed["targets"] != 31 or sealed["java_targets"] != 13 or sealed["text_targets"] != 18:
        raise ValueError("unexpected bounded restoration target count")
    receipt = {"schema": "M3_CURRENT_RUNTIME_RECIPE_PREPARATION_V1", "result": "SEALED_INPUTS_ONLY",
               "dataset": DATASET, "baseline": CURRENT, "earlier_delivery": DELIVERED,
               "source_admission_sha256": sha(json_bytes(admission)), "authorer_sha256": sha(Path(__file__).read_bytes()),
               "recipe_owner": str(owner), "crate": str(crate), "candidate": str(candidate),
               "target_count": sealed["targets"], "production_sources": 14,
               "receiving_files_written": False, "new_gate_results": "NOT_RUN"}
    write(work / "PREPARATION.json", json_bytes(receipt))
    print(json.dumps(receipt, sort_keys=True))


if __name__ == "__main__":
    main()
