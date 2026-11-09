#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Stage qualified recipe custody inputs for the existing public Maven host.

This is an input builder. The original prepare/Maven/receive pipeline remains
the only transformation and receiving implementation. No product tree is written.
"""
from pathlib import Path, PurePosixPath
import argparse
import hashlib
import importlib.util
import json
import re
import shutil


CANONICAL_PREFIX = "synexia-openrewrite-recipes/recipe-crates/m3-collections-contract-refinement-20261017"
PUBLIC_HOST = "m3/arrays/qualification/empty-segments-20261017/recipe-custody"
ORIGINAL_POM = "77768901812250c397a0ec138c83c7117a86bd14d1c77fbd75cce1324812ebb5"
CAPPED_POM = "4c90faaa9e64aebeeba14fb2196731c6334293781d0fdcafbf190f2f5662eb7d"
CONTROLS = ("prepare.py", "receive.py", "ENGINE_SOURCES.tsv",
            "src/test/java/com/synexia/rewrite/M3TaskRecipeTest.java",
            "contract/test_prepare.py", "contract/verify_receiver.py")
ENGINES = ("M3HashPinnedJavaSnapshotRecipe.java", "M3HashPinnedTextSnapshotRecipe.java",
           "M3ScopeInference.java", "M3EditScope.java")


def sha(data):
    return hashlib.sha256(data).hexdigest()


def load_original(module_name, path):
    specification = importlib.util.spec_from_file_location(module_name, path)
    module = importlib.util.module_from_spec(specification)
    specification.loader.exec_module(module)
    return module


def stage(args):
    crate = args.source_crate.resolve()
    owner = args.recipe_owner.resolve()
    public = args.public_root.resolve()
    output = args.output.resolve()
    if output.exists():
        raise ValueError("candidate output already exists")
    if output.is_relative_to(public):
        raise ValueError("input staging cannot write the receiving repository")
    prepare = load_original("original_handoff_prepare", crate / "prepare.py")
    receiver = load_original("original_handoff_receive", crate / "receive.py")
    prepare.relative_path(args.public_host)
    if not re.fullmatch(r"[a-z0-9][a-z0-9-]{0,47}", args.label):
        raise ValueError("invalid handoff label")
    if not re.fullmatch(r"[0-9a-f]{40}", args.public_baseline_ref):
        raise ValueError("public baseline must be an exact commit")
    if not re.fullmatch(r"[0-9a-f]{40}", args.canonical_baseline_ref):
        raise ValueError("canonical baseline must be an exact commit")
    destination = public / args.public_host
    controls = []
    for relative in CONTROLS:
        source = prepare.source_path(crate, relative)
        target = prepare.source_path(destination, relative)
        if source.read_bytes() != target.read_bytes():
            raise ValueError("existing public host source differs: " + relative)
        controls.append({"path": relative, "sha256": sha(source.read_bytes())})
    engines = []
    for name in ENGINES:
        relative = "src/main/java/com/synexia/rewrite/" + name
        source = prepare.source_path(owner, relative)
        target = prepare.source_path(destination / "sdk-owner", relative)
        if source.read_bytes() != target.read_bytes():
            raise ValueError("existing public engine differs: " + name)
        engines.append({"path": "sdk-owner/" + relative, "sha256": sha(source.read_bytes())})
    current_pom = sha((destination / "pom.xml").read_bytes())
    if current_pom not in (ORIGINAL_POM, CAPPED_POM) or sha((crate / "pom.xml").read_bytes()) != CAPPED_POM:
        raise ValueError("unqualified public or canonical Maven host epoch")
    datasets = {name: receiver.qualification(crate, name, owner) for name in sorted(set(args.dataset))}
    selected = {}
    origins = {}

    def put(source, relative):
        prepare.relative_path(relative)
        if relative.startswith(("src/", "contract/", "sdk-owner/")):
            raise ValueError("existing public engines and tests must be reused, not copied")
        data = prepare.text_bytes(source)
        if relative in selected and selected[relative] != data:
            raise ValueError("different inputs claim the same custody path: " + relative)
        selected[relative] = data
        origins.setdefault(relative, []).append(str(source))
        return relative

    for inventory_path in args.inventory:
        inventory = json.loads(inventory_path.read_text())
        for entry in inventory["files"]:
            prefix = CANONICAL_PREFIX + "/"
            if not entry["path"].startswith(prefix):
                raise ValueError("inventory does not belong to the existing canonical crate")
            relative = entry["path"][len(prefix):]
            source = Path(entry["source"])
            if sha(source.read_bytes()) != entry["sha256"]:
                raise ValueError("custody inventory source drift: " + entry["path"])
            # Archive formats retain bytes with an admitted source-text suffix.
            if relative.startswith("evidence/") and (relative.endswith(".log") or relative.endswith(".MF")):
                relative += ".txt"
            if relative.startswith("datasets/") and relative.split("/", 2)[1] not in datasets:
                raise ValueError("inventory includes an unselected dataset")
            put(source, relative)
    put(crate / "pom.xml", "pom.xml")
    for name in datasets:
        for source in sorted((crate / "datasets" / name).rglob("*")):
            if source.is_file():
                put(source, source.relative_to(crate).as_posix())
    qualifications = []
    for name, qualified in datasets.items():
        candidate = qualified["candidate"]
        locations = {}
        for source, leaf in ((candidate / "SDK_INPUTS.tsv", "SDK_INPUTS.tsv"),
                             (candidate / "SDK_RESULTS.tsv", "SDK_RESULTS.tsv"),
                             (qualified["report"], "SDK_TESTS.xml")):
            matches = [relative for relative, paths in origins.items() if str(source) in paths]
            location = matches[0] if matches else put(source, "evidence/" + args.label + "/" + name + "/" + leaf)
            locations[leaf] = {"path": location, "sha256": sha(source.read_bytes())}
        qualifications.append({"dataset": name, "targets": qualified["metadata"]["targets"],
                               "actual_sdk_tests": 5, "sdk_bindings_checked": True, "evidence": locations})
    put(Path(__file__).resolve(), "authoring/build_public_recipe_handoff.py")
    metadata_name = "HANDOFF-" + args.label + ".json"
    metadata = {
        "schema": "M3_EXISTING_PUBLIC_RECIPE_HANDOFF_V1", "label": args.label,
        "canonical_repository": "hsoliwal/com.synexia", "canonical_crate": CANONICAL_PREFIX,
        "canonical_engine_baseline": args.canonical_baseline_ref,
        "canonical_successor_publication": "Recorded by the later pinned publication receipt; this staging record makes no publication claim.",
        "public_baseline": args.public_baseline_ref, "public_host": args.public_host,
        "public_recipe_owner_override": "sdk-owner", "runtime_dependency_added": False,
        "existing_host_controls": controls, "existing_engine_sources": engines,
        "pom_before_sha256": current_pom, "pom_after_sha256": CAPPED_POM,
        "qualified_datasets": qualifications,
        "historical_evidence": "Existing OWNER_CUSTODY.json, SOURCE.json and earlier reports describe their pinned predecessor. The capped POM has fresh qualification; predecessor reports are not new current-host runs.",
        "mirrored_files": [{"path": relative, "sha256": sha(data), "bytes": len(data), "source_paths": sorted(set(origins[relative]))}
                           for relative, data in sorted(selected.items())]
    }
    selected[metadata_name] = (json.dumps(metadata, indent=2) + "\n").encode()
    commands = []
    for name in datasets:
        commands.append("mvn -o -B -ntp -Dm3.dataset=" + name + " \\\n  -Drecipe.owner.root=\"$PWD/sdk-owner\" -Pcontract verify")
    readme = """<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Qualified successor in the existing public recipe host

This successor reuses the four existing public recipe engines and the original
SDK, sealing and receiving tests in this directory. Synexia remains the reusable
recipe owner. The new datasets and evidence are exact qualified source bytes;
the Maven POM exposes a bounded 768 MiB test heap. No recipe implementation or
runtime dependency is added to the JDK or array module.

The earlier `CUSTODY.md`, `OWNER_CUSTODY.json`, upstream `SOURCE.json` and their
reports remain evidence for their recorded predecessor. Their original POM
hash is retained in the successor metadata. Fresh qualification is required
for the capped host and each new dataset. A later publication receipt pins the
canonical successor commit; the handoff record itself does not assert that a
remote publication occurred.

## Replay from this directory

Use Java 21, Maven and Python 3. Source for all four engines, the tests and the
selected recipes is already present in this public checkout. Maven `-o` works
when the existing pinned OpenRewrite 8.17.1 and test dependencies are cached;
an empty dependency cache must first be populated. The explicit `sdk-owner`
override preserves the original POM's default owner-path contract.

```sh
mvn -o -B -ntp -Dm3.dataset=fixture \\
  -Drecipe.owner.root="$PWD/sdk-owner" -Pcontract verify

""" + "\n\n".join(commands) + """
```

Every `-Pcontract verify` runs the original five real SDK tests, eight sealing
tests and nine isolated receiving cases. Fresh SDK outputs remain under
`target/<dataset>/candidate`. They are checked against exact preimages,
postimages, engine bytes and complete source bindings. Before receiving a
dataset into an isolated target, use the original `receive.py --check`, then
the original receiver with the same `--recipe-owner "$PWD/sdk-owner"` argument.
An already received target must be a zero-write fixed point. Runtime behavior
and JNI are qualified by the original owning-module gates separately.

Evidence logs and the size-agent manifest have `.txt` appended where needed
by the existing text recipe's path contract; their bytes are unchanged. This
handoff does not contain its own receiving recipe recursively. A separate
qualified canonical custody dataset receives these source/evidence files.

See `""" + metadata_name + """` for exact source and evidence hashes, the
public baseline, inherited controls, and the immutable datasets in this pass.
"""
    selected["CUSTODY-" + args.label + ".md"] = readme.encode()
    output.mkdir(parents=True)
    for relative, data in sorted(selected.items()):
        target = output / args.public_host / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(data)
    return {"schema": "M3_PUBLIC_RECIPE_INPUTS_ONLY_V1", "targets": len(selected),
            "datasets": sorted(datasets), "product_sources_written": False,
            "unchanged_controls_verified": len(controls), "existing_engines_verified": len(engines),
            "output": str(output), "public_host": args.public_host}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--source-crate", type=Path, required=True)
    parser.add_argument("--recipe-owner", type=Path, required=True)
    parser.add_argument("--public-root", type=Path, required=True)
    parser.add_argument("--public-host", default=PUBLIC_HOST)
    parser.add_argument("--public-baseline-ref", required=True)
    parser.add_argument("--canonical-baseline-ref", required=True)
    parser.add_argument("--dataset", action="append", required=True)
    parser.add_argument("--inventory", action="append", type=Path, default=[])
    parser.add_argument("--label", required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    print(json.dumps(stage(args), sort_keys=True))


if __name__ == "__main__":
    main()
