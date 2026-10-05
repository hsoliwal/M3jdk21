"""Read-only delivery census; writes research reports only, invokes no gates."""
from collections import Counter, defaultdict
import hashlib
import io
import json
from pathlib import Path
import tarfile

ROOT = Path(__file__).resolve().parents[3]
HERE = Path(__file__).resolve().parent
TASK = "m3/migration/intake-20261005/current-752-ci/"
inputs = {}


def digest(data):
    return hashlib.sha256(data).hexdigest()


def read_json(path):
    data = path.read_bytes()
    inputs[str(path.relative_to(ROOT))] = {"sha256": digest(data), "bytes": len(data)}
    return json.loads(data)


def verify_bytes(path, row):
    data = path.read_bytes()
    assert len(data) == row["bytes"], (path, "size")
    assert digest(data) == row["sha256"], (path, "sha256")
    blob = row.get("gitBlob", row.get("sha"))
    if blob:
        assert hashlib.sha1(b"blob " + str(len(data)).encode() + b"\0" + data).hexdigest() == blob, path
    return data


def identity(row):
    return row["sha256"], row["bytes"]


transport = read_json(ROOT / "publication/source-producer-increment-v2/transport.json")
assert inputs["publication/source-producer-increment-v2/transport.json"]["sha256"] == "0a6622cac3e852b4e3db9e2441143810c08671b671a6017b6e1aef64e5de6cbb"
source = []
for group in transport["groups"]:
    for row in group["files"]:
        verify_bytes(Path(transport["localRoot"]) / row["path"], row)
        source.append({**row, "universe": "published-source-205", "group": group["name"]})
assert len(source) == len({row["path"] for row in source}) == 205
assert sum(row["bytes"] for row in source) == 5228304
remote_publication = read_json(ROOT / "publication/source-producer-increment-v2/REMOTE-PUBLICATION.json")
assert inputs["publication/source-producer-increment-v2/REMOTE-PUBLICATION.json"]["sha256"] == "bbee9a38f74068b39b576923d1f1760cd17198fac964c5aefc567d2eed2be490"
assert remote_publication["state"] == "PASS_REMOTE_SOURCE205_READBACK"
assert remote_publication["commit"] == "e1cf684627a66ee784cfca0b42553531b2d8e7d4"
assert {row["path"]: row["sha"] for row in remote_publication["files"]} == {row["path"]: row["sha"] for row in source}

cia = read_json(ROOT / "ci-repairs/CIA-PUBLICATION-WHITELIST.json")
assert len(cia["files"]) == 26
for row in cia["files"]:
    verify_bytes(Path(row["localPath"]), row)
    source.append({**row, "universe": "cia-source-26-increment"})
assert len({row["path"] for row in source}) == 231
provenance = read_json(ROOT / "publication/SOURCE205-PUBLICATION-PROVENANCE-WHITELIST.json")
assert inputs["publication/SOURCE205-PUBLICATION-PROVENANCE-WHITELIST.json"]["sha256"] == "637d0923b3f07bd18d7c9b584e481a4fb1e3906ef61a4498b691704cf296ce32"
assert len(provenance["files"]) == 3 and sum(row["bytes"] for row in provenance["files"]) == 116236
for row in provenance["files"]:
    verify_bytes(Path(row["localPath"]), row)
    source.append({**row, "universe": "source-publication-provenance-3"})
assert len({row["path"] for row in source}) == 234

source_cohorts = []
source_by_path = {row["path"]: row for row in source}
for filename in ["ci-repairs/PUBLICATION-WHITELIST.json", "ci-repairs/PORTABLE-PRODUCER-WHITELIST.json", "ci-repairs/CIR-PUBLICATION-WHITELIST.json", "ci-repairs/PORTABLE-CIR-WHITELIST-v2.json", "native/publication-manifest.json", "publication/ROOT-WHITELIST.json", "ci-repairs/PORTABLE-CIA-WHITELIST.json"]:
    cohort = read_json(ROOT / filename)
    for row in cohort["files"]:
        if "PORTABLE-CIA" not in filename:
            assert identity(source_by_path[row["path"]]) == identity(row), row["path"]
        verify_bytes(Path(row.get("localPath", row.get("file"))), row)
    source_cohorts.append({"whitelist": filename, "sha256": inputs[filename]["sha256"], "files": len(cohort["files"]), "bytes": sum(row["bytes"] for row in cohort["files"]), "producerResultSHA256": cohort.get("producerResultSHA256", cohort.get("resultSHA256"))})

candidate = ROOT / "m3/candidate-v3"
receiver = read_json(ROOT / "m3/consumer-manifest-v3.json")
candidate_index = defaultdict(list)
for row in receiver["files"]:
    verify_bytes(candidate / row["path"], row)
    candidate_index[identity(row)].append(row["path"])

explicit = defaultdict(list)
portable_index = defaultdict(list)
mapping_rows = {}
for name in ("producer", "cir-producer", "cia-producer"):
    path = ROOT / f"m3/publication-extra/{name}-portable-v1/MAPPING.json"
    mapping = read_json(path)
    mapping_rows[name] = mapping["files"]
    for row in mapping["files"]:
        verify_bytes(Path(row["file"]), row)
        explicit[row["sourcePath"]].append({"receivingPath": row["path"], "sha256": row["sha256"], "bytes": row["bytes"], "delivery": "append-only-publication-copy", "mapping": str(path.relative_to(ROOT))})
        portable_index[identity(row)].append({"receivingPath": row["path"], "sourcePath": row["sourcePath"], "mapping": str(path.relative_to(ROOT))})
native_mapping_path = candidate / TASK / "native-delivery-mapping.json"
native_mapping = read_json(native_mapping_path)
for row in native_mapping["files"]:
    verify_bytes(candidate / row["receivingPath"], row)
    explicit[row["sourcePath"]].append({**row, "delivery": "frozen-candidate-evidence"})

# Validate every logical file, including its chunk bodies. Merely appearing in a
# references/omissions shard is not content delivery and cannot cover a source.
logical_index = defaultdict(list)
bundle_reports = []
for name, directory, delivery_prefix in [
    ("producer", ROOT / "ci-repairs/portable-producer/evidence-bundle", TASK + "producer-portable/evidence-bundle"),
    ("cir", ROOT / "ci-repairs/portable-cir/evidence-bundle", TASK + "cir-producer-portable/evidence-bundle"),
    ("cia", ROOT / "ci-repairs/portable-cia/evidence-bundle", TASK + "cia-producer-portable/evidence-bundle"),
    ("native", ROOT / "synexia/synexia-openrewrite-recipes/verification/ci-current-20261005/native-current-v1/evidence-bundle", TASK + "native-current-v1/evidence-bundle"),
]:
    bundle = read_json(directory / "BUNDLE.json")
    members = {}
    for part in bundle["parts"]:
        data = verify_bytes(directory / part["path"], part)
        with tarfile.open(fileobj=io.BytesIO(data), mode="r:gz") as archive:
            assert len(archive.getmembers()) == part["members"]
            for member in archive.getmembers():
                assert member.isfile() and member.name not in members, member.name
                members[member.name] = archive.extractfile(member).read()
    for row in bundle["topFiles"]:
        verify_bytes(directory / row["path"], row)
    logical = []
    for shard in bundle["metadataShards"]:
        data = members[shard["member"]]
        assert digest(data) == shard["sha256"] and len(data) == shard["bytes"]
        rows = [json.loads(line) for line in data.splitlines()]
        assert len(rows) == shard["records"]
        if shard["kind"] != "files":
            continue
        for row in rows:
            chunks = []
            for chunk in row["chunks"]:
                data = members[f"objects/{chunk['sha256'][:2]}/{chunk['sha256']}"]
                assert len(data) == chunk["bytes"] and digest(data) == chunk["sha256"]
                chunks.append(data)
            data = b"".join(chunks)
            assert len(data) == row["bytes"] and digest(data) == row["sha256"]
            logical.append(row)
            logical_index[identity(row)].append({"bundle": name, "receivingBundle": delivery_prefix + "/BUNDLE.json", "logicalPath": row["path"]})
    assert len(logical) == len({row["path"] for row in logical})
    bundle_reports.append({"id": name, "bundle": str((directory / "BUNDLE.json").relative_to(ROOT)), "sha256": digest((directory / "BUNDLE.json").read_bytes()), "logicalFileCount": len(logical), "logicalBytes": sum(row["bytes"] for row in logical), "parts": bundle["parts"], "allLogicalFileBytesReconstructedAndVerified": True})

rows = []
for item in source:
    matches = candidate_index[identity(item)]
    direct = explicit[item["path"]]
    for mapping in direct:
        assert identity(mapping) == identity(item), item["path"]
    active = [path for path in matches if not path.startswith(TASK) and not path.startswith("m3/migration/intake-20261005/")]
    evidence = [path for path in matches if path not in active]
    logical = logical_index[identity(item)] if item["bytes"] else []
    logical.sort(key=lambda match: (0 if match["logicalPath"].startswith(("producer-authoring-v2/", "cir-authoring-v2/", "cia-authoring-v1/")) else 1 if match["logicalPath"].startswith(("evidence/producer-v2/project/", "evidence/cir-producer-v2/project/", "evidence/cia-producer-v1/project/")) else 2, match["logicalPath"]))
    portable = portable_index[identity(item)] if item["bytes"] else []
    # Zero-byte equality is not meaningful path or role evidence.
    if not item["bytes"]:
        active, evidence = [], []
    state = "ACTIVE_RECEIVING_BYTES" if active else "FROZEN_RECEIVING_EVIDENCE" if evidence else "EXPLICIT_PORTABLE_FILE" if direct else "PORTABLE_FILE_BYTES" if portable else "PORTABLE_LOGICAL_BYTES" if logical else "MISSING_BYTES"
    rows.append({"universe": item["universe"], "sourcePath": item["path"], "sha256": item["sha256"], "bytes": item["bytes"], "gitBlob": item.get("gitBlob", item.get("sha")), "group": item["group"], "classification": state, "sameCanonicalPathInCandidate": item["path"] in matches, "activeReceivingByteMatches": active, "receivingEvidenceByteMatches": evidence, "explicitSourcePathMappings": direct, "portableFileByteMatches": portable, "portableLogicalByteMatches": logical, "selectedLogicalArchiveEntry": logical[0] if logical else None})

publication_additions = []
for row in rows:
    if row["classification"] != "MISSING_BYTES":
        continue
    original = next(item for item in source if item["path"] == row["sourcePath"])
    assert original["universe"] in {"published-source-205", "source-publication-provenance-3"}
    original_file = Path(transport["localRoot"]) / original["path"] if original["universe"] == "published-source-205" else Path(original["localPath"])
    suffix = original["path"].removeprefix("synexia-openrewrite-recipes/verification/ci-current-20261005/")
    assert suffix != original["path"]
    receiving_path = TASK + "source-admission-evidence/" + suffix
    assert not (candidate / receiving_path).exists()
    publication_additions.append({"path": receiving_path, "file": str(original_file), "sourcePath": row["sourcePath"], "sha": row["gitBlob"], "gitBlob": row["gitBlob"], "sha256": row["sha256"], "bytes": row["bytes"], "mode": "100644", "type": "blob", "group": "provenance-only-publication-addition"})

result = {
    "schema": "synexia.source-delivery-coverage/1",
    "state": "READ_ONLY_CENSUS",
    "sourcePublishedCommit": "e1cf684627a66ee784cfca0b42553531b2d8e7d4",
    "sourcePublicationReceipt": {"path": "publication/source-producer-increment-v2/REMOTE-PUBLICATION.json", "sha256": inputs["publication/source-producer-increment-v2/REMOTE-PUBLICATION.json"]["sha256"], "state": remote_publication["state"], "pr": remote_publication["pr"], "tree": remote_publication["tree"], "verifiedPathAndGitBlobCount": len(remote_publication["files"])},
    "candidate": str(candidate),
    "inputs": inputs,
    "universes": {universe: {"files": sum(row["universe"] == universe for row in rows), "bytes": sum(row["bytes"] for row in rows if row["universe"] == universe), "classifications": dict(Counter(row["classification"] for row in rows if row["universe"] == universe))} for universe in sorted({row["universe"] for row in rows})},
    "verifiedCandidateFiles": len(receiver["files"]),
    "appendOnlyPortableMappingCounts": {key: len(value) for key, value in mapping_rows.items()},
    "bundles": bundle_reports,
    "sourceCohorts": source_cohorts,
    "rows": rows,
    "publicationAdditions": publication_additions,
    "publicationAdditionBytes": sum(row["bytes"] for row in publication_additions),
    "coverageAfterSelectingPublicationAdditions": {"missingSourceFiles": 0, "qualifiedSourceContextChanged": False, "additionalProofGatesExecuted": False},
    "limitations": ["Byte equality establishes content presence, not execution or semantic activation of every matching path.", "Source Synexia canonical paths and M3 receiving paths are distinct; exact source path mappings are reported separately from byte matches.", "Portable append-only copies are outside the frozen candidate and have not been treated as consumer gate inputs.", "The current consumer-v3 stopped; this census makes no consumer PASS claim. Final consumer bundle and remote receiving publication readback are not yet part of this census.", "Reference-only and omission records do not count as delivered bytes.", "The universe is exactly published source205 plus separate Cia26 and source-publication provenance3; this is not repository-wide coverage.", "No imports of candidate code, gates, proof edits, candidate edits, or external writes were performed."],
}
(HERE / "coverage.json").write_text(json.dumps(result, indent=2, sort_keys=True) + "\n")
lines = ["universe\tsource_path\tsha256\tbytes\tclassification\tactive_receiving_paths\treceiving_evidence_paths\texplicit_portable_paths\tportable_file_byte_matches\tportable_logical_entries"]
for row in rows:
    cells = [row["universe"], row["sourcePath"], row["sha256"], str(row["bytes"]), row["classification"], " | ".join(row["activeReceivingByteMatches"]), " | ".join(row["receivingEvidenceByteMatches"]), " | ".join(x["receivingPath"] for x in row["explicitSourcePathMappings"]), " | ".join(x["receivingPath"] for x in row["portableFileByteMatches"]), " | ".join(x["bundle"] + ":" + x["logicalPath"] for x in row["portableLogicalByteMatches"])]
    assert all("\t" not in cell and "\n" not in cell for cell in cells)
    lines.append("\t".join(cells))
(HERE / "coverage.tsv").write_text("\n".join(lines) + "\n")
print(json.dumps({"universes": result["universes"], "bundles": [{"id": x["id"], "logicalFileCount": x["logicalFileCount"]} for x in bundle_reports], "missing": [{"path": row["sourcePath"], "sha256": row["sha256"], "bytes": row["bytes"]} for row in rows if row["classification"] == "MISSING_BYTES"]}, indent=2))
