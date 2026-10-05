#!/usr/bin/env python3
"""Independent stdlib readback of one native evidence bundle; execute no gates."""
import argparse
import gzip
import hashlib
import io
import json
from pathlib import Path, PurePosixPath
import tarfile

COMMIT = "9963cc08ff13922b92a0e3db7c30f56fddacba7d"
ROOT_TREE = "6c055c584ebb5fa177420d1668db73bcb7e7fef4"
RESULT_SHA = "1e82bec642c8ea8ece15e8caeeed283416c1b612e22c357b20178524b6ab1605"
BUNDLE_SHA = "d3a09e407eeb5e352e00ff7167ddb73737a2c488c16e4d382491defa4d706b42"
PREFIX = "evidence/current-v1/"
STAGES = ("lint", "compile", "test", "runtime")


def sha(data):
    return hashlib.sha256(data).hexdigest()


def git_sha(kind, data):
    return hashlib.sha1(kind.encode() + b" " + str(len(data)).encode() + b"\0" + data).hexdigest()


def check(condition, reason):
    if not condition:
        raise ValueError(reason)


def safe(path):
    p = PurePosixPath(path)
    check(bool(path) and not p.is_absolute() and all(x not in ("", ".", "..") for x in p.parts)
          and "\\" not in path and "\0" not in path and "\n" not in path, "unsafe archive path")
    return path


def deterministic_archive(members):
    raw = io.BytesIO()
    with tarfile.open(fileobj=raw, mode="w", format=tarfile.USTAR_FORMAT) as archive:
        for name, data in members:
            item = tarfile.TarInfo(name)
            item.size, item.mode, item.mtime = len(data), 0o644, 0
            item.uid = item.gid = 0
            item.uname = item.gname = ""
            archive.addfile(item, io.BytesIO(data))
    return gzip.compress(raw.getvalue(), compresslevel=9, mtime=0)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--bundle", type=Path, required=True)
    parser.add_argument("--original", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    check(not args.output.exists(), "readback output must be fresh")
    bundle = args.bundle.resolve()
    encoded = (bundle / "BUNDLE.json").read_bytes()
    check(sha(encoded) == BUNDLE_SHA, "unexpected bundle identity")
    index = json.loads(encoded)
    check(index["schema"] == "synexia.evidence.bundle/1", "unsupported bundle schema")
    check(index["partByteLimit"] == 2097152 and index["proofGatesExecuted"] is False, "profile drift")
    for row in index["topFiles"]:
        data = (bundle / safe(row["path"])).read_bytes()
        check(sha(data) == row["sha256"] and len(data) == row["bytes"], "top-file drift")
    members, member_order = {}, []
    for row in index["parts"]:
        payload = (bundle / safe(row["path"])).read_bytes()
        check(sha(payload) == row["sha256"] and len(payload) == row["bytes"] <= 2097152, "part drift")
        current = []
        with tarfile.open(fileobj=io.BytesIO(payload), mode="r:gz") as archive:
            for item in archive:
                name = safe(item.name)
                check(item.isfile() and name not in members and item.size <= 524288, "invalid member")
                check((item.mode, item.mtime, item.uid, item.gid, item.uname, item.gname)
                      == (0o644, 0, 0, 0, "", ""), "noncanonical member metadata")
                stream = archive.extractfile(item)
                check(stream is not None, "unreadable member")
                data = stream.read()
                check(len(data) == item.size, "member truncation")
                members[name] = data
                current.append((name, data))
                member_order.append((name, data))
        check(len(current) == row["members"], "part member-count drift")
        check(deterministic_archive(current) == payload, "part is not the declared deterministic encoding")
    full_archive_bytes = len(deterministic_archive(member_order))
    check(len(index["parts"]) == 1 or full_archive_bytes > 2097152, "unnecessary archive split")
    groups = {}
    expected_metadata = set()
    for row in index["metadataShards"]:
        name = safe(row["member"])
        data = members[name]
        check(sha(data) == row["sha256"] and len(data) == row["bytes"], "metadata shard drift")
        records = [json.loads(line) for line in data.splitlines()]
        check(len(records) == row["records"], "metadata record-count drift")
        groups.setdefault(row["kind"], []).extend(records)
        expected_metadata.add(name)
    check(expected_metadata == {name for name in members if name.startswith("manifests/")}, "metadata set drift")
    files, used_objects = {}, set()
    for row in groups["files"]:
        name = safe(row["path"])
        check(name not in files, "duplicate logical file")
        pieces = []
        for chunk in row["chunks"]:
            object_name = "objects/" + chunk["sha256"][:2] + "/" + chunk["sha256"]
            data = members[object_name]
            check(sha(data) == chunk["sha256"] and len(data) == chunk["bytes"], "chunk drift")
            used_objects.add(object_name)
            pieces.append(data)
        data = b"".join(pieces)
        check(sha(data) == row["sha256"] and len(data) == row["bytes"], "logical file drift")
        files[name] = data
        if args.original:
            check((args.original / name).read_bytes() == data, "original bytes differ: " + name)
    check(used_objects | expected_metadata == set(members), "undeclared or unused members")
    check(len(files) == 250 == index["summary"]["retainedPaths"], "logical file-count drift")
    check(sum(map(len, files.values())) == 10912451 == index["summary"]["retainedPathBytes"], "logical byte-count drift")
    check(len(used_objects) == 202 == index["summary"]["uniqueChunks"], "chunk-count drift")

    def load(path):
        return json.loads(files[PREFIX + path])

    result, audit, patch, plan = (load(path) for path in ("RESULT.json", "FINAL-AUDIT.json", "proof/PATCH.json", "proof/commands.json"))
    check(sha(files[PREFIX + "RESULT.json"]) == RESULT_SHA and result["result"] == "PASS", "result drift")
    check(result["commit"] == COMMIT and result["root_tree"] == ROOT_TREE, "executed source revision drift")
    check(audit["result"] == "PASS" and audit["input_drift"] == [], "final audit did not pass")
    check(result["final_audit_sha256"] == sha(files[PREFIX + "FINAL-AUDIT.json"]), "audit binding drift")
    check(result["patch_sha256"] == sha(files[PREFIX + "proof/PATCH.json"]), "patch binding drift")
    check(patch["source_manifest_sha256"] == sha(files[PREFIX + "proof/SOURCE-MANIFEST.json"])
          and patch["commands_sha256"] == sha(files[PREFIX + "proof/commands.json"]), "source or command seal drift")
    references = groups["references"]
    check(len(references) == 8147 and sum(x["retention"] == "exact-path-and-content" for x in references) == 94
          and sum(x["retention"] == "external-input-manifest-only" for x in references) == 8053, "sealed reference coverage drift")
    for row in references:
        if row["retention"] == "exact-path-and-content":
            check(sha(files[row["path"]]) == row["expectedSHA256"], "local input reference drift")
    tools = load("proof/TOOLCHAIN.json")["tools"]
    check(len(tools) == 8053 and sum(x["bytes"] for x in tools) == 466293744, "tool-manifest count drift")
    sources = load("source-receipts.json")
    check(len(sources) == 65 and sum(x["status"] == "UNCHANGED_SOURCE" for x in sources) == 64, "repository scope drift")
    tree_bytes = files[PREFIX + "proof/current-tree-proof.json.gz"]
    check(sha(tree_bytes) == patch["current_tree_proof_sha256"], "tree proof binding drift")
    tree_doc = json.loads(gzip.decompress(tree_bytes))
    trees = tree_doc["trees"]
    check(tree_doc["rootTree"] == ROOT_TREE and len(trees) == 45, "tree proof scope drift")
    for identity, entries in trees.items():
        raw = b""
        for entry in sorted(entries, key=lambda x: (x["path"] + ("/" if x["type"] == "tree" else "")).encode()):
            raw += (entry["mode"].lstrip("0") + " " + entry["path"]).encode() + b"\0" + bytes.fromhex(entry["sha"])
        check(git_sha("tree", raw) == identity, "tree object drift")
    for row in sources:
        data = files[PREFIX + "repo/" + row["path"]]
        check(len(data) == row["bytes"] and sha(data) == row["sha256"]
              and git_sha("blob", data) == row["git_blob_sha1"], "source blob drift")
        identity, chain = ROOT_TREE, []
        for part in row["path"].split("/"):
            chain.append(identity)
            hits = [entry for entry in trees[identity] if entry["path"] == part]
            check(len(hits) == 1, "source path missing from tree")
            identity = hits[0]["sha"]
        check(chain == row["tree_chain"] and identity == row["git_blob_sha1"]
              and hits[0]["mode"] == row["mode"], "source path or mode drift")
    prior, command_count = [], 0
    for index_number, stage in enumerate(STAGES, 1):
        receipt_name = f"{index_number:02d}-{stage}.json"
        relative = "chain/current-v1/" + receipt_name
        receipt = load(relative)
        check(receipt["exit_code"] == 0 and receipt["source_drift_after"] == []
              and receipt["prior_receipts"] == prior, "stage predecessor drift")
        check(receipt["source_manifest_sha256"] == patch["source_manifest_sha256"]
              and receipt["patch_seal_sha256"] == result["patch_sha256"], "stage input binding drift")
        for stream in ("stdout", "stderr"):
            data = files[PREFIX + relative.removesuffix(".json") + "." + stream + ".log"]
            check(sha(data) == receipt[stream + "_sha256"], "stage log drift")
        stage_result = result["stages"][index_number - 1]
        check(stage_result["receipt_sha256"] == sha(files[PREFIX + relative]), "result stage binding drift")
        prior.append({"path": receipt_name, "sha256": sha(files[PREFIX + relative])})
        command_path = "command-logs/" + stage + "/commands.json"
        commands = load(command_path)
        check(len(commands) == len(plan["stages"][stage]) == stage_result["accepted_commands"], "command-count drift")
        check(stage_result["command_receipts_sha256"] == sha(files[PREFIX + command_path]), "command binding drift")
        for expected, actual in zip(plan["stages"][stage], commands, strict=True):
            check(actual["accepted"] and actual["argv"] == expected["argv"]
                  and actual["name"] == expected["name"]
                  and actual["exit_code"] == expected.get("expected_exit_code", 0), "command outcome drift")
            output = b""
            for stream in ("stdout", "stderr"):
                data = files[PREFIX + actual[stream + "_path"]]
                check(sha(data) == actual[stream + "_sha256"], "command log drift")
                output += data
            if expected.get("required_output"):
                check(expected["required_output"].encode() in output, "required result marker missing")
            command_count += 1
    check(command_count == 32, "executed command scope drift")
    artifact_rows = load("proof/build-artifacts.json")
    omissions = {row["path"]: row for row in groups["omissions"]}
    check(len(artifact_rows) == 61 and len(omissions) == 67, "artifact identity count drift")
    for row in artifact_rows:
        name = PREFIX + row["path"]
        check(name not in files and omissions[name]["sha256"] == row["sha256"]
              and omissions[name]["bytes"] == row["bytes"], "compiled artifact retention drift")
    if args.original:
        for row in omissions.values():
            data = (args.original / row["path"]).read_bytes()
            check(sha(data) == row["sha256"] and len(data) == row["bytes"], "omitted original artifact drift")
    publish = bundle.parent
    for name in ("STATUS.tsv", "FINAL_REPORT.md", "RUN_CONTEXT.tsv", "PROVENANCE.tsv", "VERIFY_CONTRACT.tsv", "OUTPUT_CONTRACT.tsv"):
        check((publish / name).read_bytes() == files["publication-metadata/" + name], "canonical packet copy drift")
    for name, archived in (("RESULT.json", PREFIX + "RESULT.json"), ("FINAL-AUDIT.json", PREFIX + "FINAL-AUDIT.json"),
                           ("PREPARED.json", "PREPARED.json"), ("package-plan.json", "package-plan.json"),
                           ("evidence-tools/package_evidence.py", "acquisition/package_evidence.py"),
                           ("evidence-tools/native_bundle.py", "acquisition/native_bundle.py")):
        check((publish / name).read_bytes() == files[archived], "loose evidence copy drift")
    report = {"schema": "synexia.native-bundle-readback/1", "status": "PASS_INDEPENDENT_IMPLEMENTATION_READBACK",
              "review_kind": "Separate stdlib implementation; not an independent agent review",
              "proof_gates_executed": False, "compiled_binaries_executed": False,
              "bundle_sha256": BUNDLE_SHA, "result_sha256": RESULT_SHA,
              "program_sha256": sha(Path(__file__).read_bytes()), "source_commit": COMMIT, "source_tree": ROOT_TREE,
              "parts": index["parts"], "single_archive_bytes": full_archive_bytes, "part_limit_bytes": 2097152,
              "unnecessary_split": False, "deterministic_part_bytes_verified": True,
              "archive_members": len(members), "logical_files": len(files), "logical_bytes": sum(map(len, files.values())),
              "unique_chunks": len(used_objects), "local_sealed_inputs_retained": 94,
              "external_tool_inputs_manifest_only": 8053, "external_tool_installation_bytes_archived": 0,
              "external_tool_bodies_read_by_readback": False, "repository_blobs_verified": 65,
              "root_linked_trees_recomputed": 45, "stage_chains_verified": 4, "command_receipts_and_logs_verified": command_count,
              "sealed_class_library_identities_retained": 61, "compiled_artifact_omission_records": 67,
              "original_logical_files_compared": len(files) if args.original else 0,
              "omitted_original_artifacts_rehashed": len(omissions) if args.original else 0,
              "canonical_packet_copies_verified": 6, "input_or_log_drift": [],
              "observations": result["observations"], "semantic_negative_controls": result["semantic_negative_controls"],
              "strict_repository_admission": False, "performance_measurement": False,
              "boundary": "Readback validates exact retained evidence and declared identities; it does not rerun proof gates, install tools, supply omitted compiled binaries or establish hermetic execution."}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open("x") as stream:
        stream.write(json.dumps(report, indent=2, sort_keys=True) + "\n")
    print(json.dumps({"status": report["status"], "output": str(args.output), "sha256": sha(args.output.read_bytes()),
                      "logical_files": len(files), "command_receipts": command_count, "single_archive_bytes": full_archive_bytes}))


if __name__ == "__main__":
    main()
