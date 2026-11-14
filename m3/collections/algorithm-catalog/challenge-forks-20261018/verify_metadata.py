# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Read-only replay of this finite catalogue's metadata; no algorithm execution."""

import csv
import gzip
import hashlib
import io
import json
import re
from collections import Counter, defaultdict
from pathlib import Path, PurePosixPath
from urllib.parse import unquote, urlsplit


ROOT = Path(__file__).resolve().parent
PINS = {
    "javadev/LeetCode-in-Java": (
        "bed5d330e2b145e24e6fc1048ca28bf156b3d639",
        "c9710969e21afcfb3b52ebe6f8aeac3e01590a8f"),
    "RodneyShag/HackerRank_solutions": (
        "a98773d3a0c4f11b9cd90d6a1f37b994c170996a",
        "f7611734f13a7b74e75254eb489230f02f89e966"),
}
INVENTORY_SHA = "d90d6081a863e088f48bf5ad2e8664681556b689670473fbfcc1d67a8ce51d8c"
TAXONOMY_SHA = "5be0d1de9d3ad3fc9e3177ac54dfe61a18d419b21e6c44161091c170343a5510"
FIELDS = ("input_id repository revision path git_blob git_mode git_size_bytes "
          "file_kind path_role filename_kind source_top_level native_parent_directory "
          "java_files_in_same_directory readme_files_in_same_directory rights rights_path "
          "rights_git_blob rights_scope decision observation").split()
TAXONOMY_FIELDS = ("repository revision native_top_level_directory blobs java readme "
                   "license other taxonomy_origin decision").split()
NUMBERED = re.compile(r"src/main/java/g\d+_\d+/s\d+_[^/]+/[^/]+\.java\Z")
HEX40 = re.compile(r"[0-9a-f]{40}\Z")


def require(condition, message):
    if not condition:
        raise ValueError(message)


def sha256(data):
    return hashlib.sha256(data).hexdigest()


def git_hash(kind, data):
    return hashlib.sha1(kind.encode() + b" " + str(len(data)).encode() + b"\0" + data).hexdigest()


def read_json(name):
    return json.loads((ROOT / name).read_bytes())


def read_tsv(name):
    with (ROOT / name).open(encoding="utf-8", newline="") as stream:
        return list(csv.DictReader(stream, delimiter="\t"))


def tsv_bytes(rows, fields):
    stream = io.StringIO(newline="")
    writer = csv.DictWriter(stream, fieldnames=fields, delimiter="\t", lineterminator="\n")
    writer.writeheader()
    writer.writerows(rows)
    return stream.getvalue().encode("utf-8")


def verify_manifest():
    seals = {}
    for line in (ROOT / "SHA256SUMS").read_text().splitlines():
        digest, name = line.split("  ", 1)
        parts = PurePosixPath(name).parts
        require(not name.startswith("/") and ".." not in parts and name not in seals,
                "unsafe or duplicate manifest path")
        path = ROOT / name
        require(path.is_file() and not path.is_symlink(), "missing or linked manifest file: " + name)
        require(sha256(path.read_bytes()) == digest, "file SHA-256 mismatch: " + name)
        seals[name] = digest
    actual = {str(path.relative_to(ROOT)) for path in ROOT.rglob("*") if path.is_file()}
    require(actual == set(seals) | {"SHA256SUMS"}, "unsealed or missing package file")
    return len(seals)


def verify_tree(tree, root_sha):
    require(tree["sha"] == root_sha and tree["truncated"] is False, "tree pin/completeness mismatch")
    entries, children, directories = {}, defaultdict(list), {"": root_sha}
    for entry in tree["tree"]:
        path = entry["path"]
        parts = path.split("/")
        require(path and all(part not in {"", ".", ".."} for part in parts)
                and "\0" not in path and path not in entries, "unsafe or duplicate tree path")
        require(HEX40.fullmatch(entry["sha"]) is not None, "invalid Git object identity")
        require(entry["type"] in {"tree", "blob", "commit"}, "unknown Git object type")
        entries[path] = entry
        children[path.rpartition("/")[0]].append(entry)
        if entry["type"] == "tree":
            directories[path] = entry["sha"]
    require(set(children) <= set(directories), "unlisted parent tree")
    for directory, expected in directories.items():
        def order(entry):
            return entry["path"].rsplit("/", 1)[-1].encode() + (b"/" if entry["type"] == "tree" else b"")
        payload = b"".join(
            entry["mode"].lstrip("0").encode() + b" "
            + entry["path"].rsplit("/", 1)[-1].encode() + b"\0" + bytes.fromhex(entry["sha"])
            for entry in sorted(children[directory], key=order))
        require(git_hash("tree", payload) == expected, "Git tree hash mismatch: " + directory)
    return entries, len(directories)


def project(source, entries):
    blobs = sorted((entry for entry in entries.values() if entry["type"] == "blob"),
                   key=lambda entry: entry["path"].encode("utf-8"))
    java_dirs = Counter(entry["path"].rpartition("/")[0] for entry in blobs if entry["path"].endswith(".java"))
    readme_dirs = Counter(entry["path"].rpartition("/")[0] for entry in blobs
                          if entry["path"].rsplit("/", 1)[-1].lower().startswith("readme"))
    rows = []
    for ordinal, entry in enumerate(blobs, 1):
        path = entry["path"]
        parent, _, name = path.rpartition("/")
        if path == source["license_path"]:
            kind, role, filename = "ROOT_LICENSE", "ROOT_LICENSE_BLOB_VERIFIED", "ROOT_LICENSE_FILENAME"
        elif name.endswith(".java"):
            kind = "JAVA"
            if path.startswith("src/main/java/"):
                role = "MAIN_ROOT_JAVA_IN_NUMBERED_CHALLENGE_DIRECTORY" if NUMBERED.fullmatch(path) else "MAIN_ROOT_JAVA_OTHER_DIRECTORY"
            else:
                role = "TEST_ROOT_JAVA" if path.startswith("src/test/java/") else "JAVA_OUTSIDE_DECLARED_MAIN_TEST_ROOTS"
            filename = "SOLUTION_JAVA_FILENAME" if name == "Solution.java" else (
                "TEST_JAVA_FILENAME_SUFFIX" if name.endswith("Test.java") else "OTHER_JAVA_FILENAME")
        elif name.lower().startswith("readme"):
            kind, filename = "README", "README_FILENAME"
            role = "README_COLOCATED_WITH_JAVA" if java_dirs[parent] else "README_WITHOUT_SAME_DIRECTORY_JAVA"
        else:
            kind, role, filename = "OTHER_BLOB", "OTHER_BLOB_NOT_CLASSIFIED", "OTHER_FILENAME"
        values = [f"{source['donor_id']}:{ordinal:05d}", source["repository"], source["revision"],
                  path, entry["sha"], entry["mode"], entry["size"], kind, role, filename,
                  path.split("/", 1)[0] if parent else "<ROOT>", parent or "<ROOT>",
                  java_dirs[parent], readme_dirs[parent], "MIT", source["license_path"],
                  source["license_git_blob"], "REPOSITORY_ROOT_DECLARATION_VERIFIED;PER_FILE_BODY_RIGHTS_NOT_AUDITED",
                  "NOT_EVALUATED", "COMPLETE_PINNED_GIT_TREE_BLOB_IDENTITY;PATH_ROLE_IS_STRUCTURAL_ONLY;NO_FITNESS_ADMISSION"]
        rows.append(dict(zip(FIELDS, values)))
    return rows


def verify_counts(source, entries, rows, directory_count, expected):
    java = [row for row in rows if row["file_kind"] == "JAVA"]
    main = [row for row in java if row["path"].startswith("src/main/java/")]
    tests = [row for row in java if row["path"].startswith("src/test/java/")]
    numbered = [row for row in main if NUMBERED.fullmatch(row["path"])]
    readmes = [row for row in rows if row["file_kind"] == "README"]
    main_dirs = {row["native_parent_directory"] for row in main}
    actual = {
        "repository": source["repository"], "revision": source["revision"], "donor_id": source["donor_id"],
        "root_tree": source["root_tree"], "recursive": True, "truncated": False,
        "root_license": "MIT", "root_license_path": source["license_path"],
        "root_license_git_blob": source["license_git_blob"], "all_blobs_disposition": "NOT_EVALUATED",
        "target_implementation_admissions": 0, "all_tree_entries": len(entries), "blob_entries": len(rows),
        "tree_entries_excluding_root": directory_count - 1, "git_tree_hashes_verified_including_root": directory_count,
        "commit_entries": sum(entry["type"] == "commit" for entry in entries.values()),
        "file_kind_counts": dict(Counter(row["file_kind"] for row in rows)),
        "filename_kind_counts": dict(Counter(row["filename_kind"] for row in rows)),
        "path_role_counts": dict(Counter(row["path_role"] for row in rows)), "java_all_paths": len(java),
        "java_main_root_paths": len(main), "java_test_root_paths": len(tests),
        "java_outside_declared_roots": len(java) - len(main) - len(tests),
        "java_solution_filename_paths": sum(row["filename_kind"] == "SOLUTION_JAVA_FILENAME" for row in java),
        "java_named_non_solution_non_test_suffix_paths": sum(row["filename_kind"] == "OTHER_JAVA_FILENAME" for row in java),
        "java_main_numbered_challenge_directory_paths": len(numbered),
        "java_main_numbered_challenge_directories": len({row["native_parent_directory"] for row in numbered}),
        "java_main_other_directory_paths": [row["path"] for row in main if not NUMBERED.fullmatch(row["path"])],
        "readme_all_paths": len(readmes),
        "readme_under_main_java_root": sum(row["path"].startswith("src/main/java/") for row in readmes),
        "readme_colocated_with_any_java": sum(row["java_files_in_same_directory"] > 0 for row in readmes),
        "readme_colocated_with_main_java": sum(row["native_parent_directory"] in main_dirs for row in readmes),
    }
    require(actual == expected, "source counts differ: " + source["repository"])


def verify_forks():
    evidence = read_json("PUBLIC_FORK_EVIDENCE.json")
    rows = read_tsv("FORK_WORKSPACE_INVENTORY.tsv")
    expected, public = {}, set(PINS)
    require(len(evidence["repositories"]) == 5 and len(rows) == 9, "public fork scope drift")
    for repository in evidence["repositories"]:
        require(repository["private"] is False and repository["visibility"] == "public"
                and repository["parent_private"] is False and repository["source_private"] is False
                and repository["is_fork"] is True, "nonpublic or nonfork candidate")
        public.update(repository[key] for key in ("repository", "fork_parent", "fork_source"))
        for ref in repository["selected_refs"]:
            key = (repository["repository"], ref["branch"])
            require(key not in expected and HEX40.fullmatch(ref["head_sha"]), "duplicate/invalid branch identity")
            expected[key] = (repository, ref)
    require(len(expected) == len(rows), "public ref count mismatch")
    seen = set()
    for row in rows:
        key = (row["repository"], row["branch"])
        require(key in expected and key not in seen, "duplicate/unlisted public fork row")
        seen.add(key)
        repository, ref = expected[key]
        require(row["visibility"] == "public" and row["is_fork"] == "true"
                and row["usage"] == "READ_ONLY_CANDIDATE"
                and row["license_admission"] == "NOT_REVIEWED_FOR_SOURCE_REUSE", "fork admission state changed")
        for field in ("fork_parent", "fork_source", "metadata_license_only"):
            require(row[field] == repository[field], "fork metadata mismatch")
        for field in ("head_sha", "head_commit_utc"):
            require(row[field] == ref[field], "fork head mismatch")
        require(row["evidence_files"] == "PUBLIC_FORK_EVIDENCE.json", "unprojected fork evidence")
    return {repository.lower() for repository in public}


def main():
    sealed_files = verify_manifest()
    for item in read_json("PACKET_SOURCE_RECEIPTS.json")["copies_and_projections"]:
        data = (ROOT / item["destination"]).read_bytes()
        require(len(data) == item["destination_bytes"] and sha256(data) == item["destination_sha256"],
                "packet projection/copy seal differs")
        if item["transformation"] == "EXACT_BYTE_COPY":
            require(len(data) == item["source_bytes"] and sha256(data) == item["source_sha256"],
                    "original copied source differs")
        elif item["transformation"] == "GZIP_LEVEL9_MTIME0_NO_FILENAME":
            raw = gzip.decompress(data)
            require(len(raw) == item["source_bytes"] and sha256(raw) == item["source_sha256"],
                    "compressed source differs")
    receipts = read_json("source-receipts/API_SOURCE_RECEIPTS.json")
    counts = read_json("INVENTORY_COUNTS.json")
    source_counts = {source["repository"]: source for source in counts["sources"]}
    require({source["repository"] for source in receipts["sources"]} == set(PINS), "donor set drift")
    rows, taxonomy, identities, raw_documents = [], [], {}, []
    directory_total = tree_entry_total = 0
    for source in sorted(receipts["sources"], key=lambda item: item["repository"].encode("utf-8")):
        require((source["revision"], source["root_tree"]) == PINS[source["repository"]], "donor pin drift")
        documents = []
        for index, kind in enumerate(("commit", "tree", "LICENSE")):
            relative = source["donor_id"] + "_" + kind
            data = (ROOT / ("licenses/" + relative + ".txt" if kind == "LICENSE"
                            else "metadata/" + relative + ".json.gz")).read_bytes()
            if kind != "LICENSE":
                require(data[:4] == b"\x1f\x8b\x08\x00" and data[4:8] == bytes(4), "gzip timestamp/flags differ")
                data = gzip.decompress(data)
            request = source["requests"][index]
            require(len(data) == request["response_bytes"] and sha256(data) == request["response_sha256"],
                    "raw API/license byte identity mismatch")
            raw_documents.append(data.decode("utf-8"))
            documents.append(data if kind == "LICENSE" else json.loads(data))
        commit, tree, license_body = documents
        require(commit["sha"] == source["revision"] and commit["tree"]["sha"] == source["root_tree"], "commit/root mismatch")
        entries, directory_count = verify_tree(tree, source["root_tree"])
        require(entries[source["license_path"]]["sha"] == source["license_git_blob"]
                == git_hash("blob", license_body), "root license Git blob mismatch")
        projected = project(source, entries)
        verify_counts(source, entries, projected, directory_count, source_counts[source["repository"]])
        for row in projected:
            key = (row["repository"], row["path"])
            require(key not in identities, "duplicate repository/path occurrence")
            identities[key] = row
        groups = defaultdict(list)
        for row in projected:
            groups[row["source_top_level"]].append(row)
        for group, members in sorted(groups.items(), key=lambda item: item[0].encode("utf-8")):
            kinds = Counter(row["file_kind"] for row in members)
            values = [source["repository"], source["revision"], group, len(members), kinds["JAVA"],
                      kinds["README"], kinds["ROOT_LICENSE"], kinds["OTHER_BLOB"], "EXACT_DONOR_DIRECTORY_NAME", "NOT_EVALUATED"]
            taxonomy.append(dict(zip(TAXONOMY_FIELDS, values)))
        rows.extend(projected)
        directory_total += directory_count
        tree_entry_total += len(entries)
    for name, projected, fields, expected_sha in (
            ("DONOR_SOURCE_INVENTORY.tsv", rows, FIELDS, INVENTORY_SHA),
            ("DONOR_DIRECTORY_TAXONOMY.tsv", taxonomy, TAXONOMY_FIELDS, TAXONOMY_SHA)):
        data = tsv_bytes(projected, fields)
        require(data == (ROOT / name).read_bytes() and sha256(data) == expected_sha, "deterministic projection differs: " + name)
    candidates = read_tsv("REVIEWED_CANDIDATES.tsv")
    require(len(candidates) == 5 and len({row["donor_link_id"] for row in candidates}) == 5, "reviewed candidate count differs")
    for candidate in candidates:
        parts = unquote(urlsplit(candidate["source"]).path).split("/")
        require(parts[3] == "blob" and candidate["decision"] == "REFERENCE_ONLY", "candidate admission changed")
        row = identities[("/".join(parts[1:3]), "/".join(parts[5:]))]
        require(row["revision"] == parts[4] and "blob " + row["git_blob"] in candidate["revision"], "candidate blob binding differs")
    combined = {
        "donors": 2, "blob_entries": len(rows), "java_paths": sum(row["file_kind"] == "JAVA" for row in rows),
        "main_root_java_paths": sum(row["path"].startswith("src/main/java/") and row["file_kind"] == "JAVA" for row in rows),
        "test_root_java_paths": sum(row["path_role"] == "TEST_ROOT_JAVA" for row in rows),
        "java_outside_declared_roots": sum(row["path_role"] == "JAVA_OUTSIDE_DECLARED_MAIN_TEST_ROOTS" for row in rows),
        "literal_solution_java_paths": sum(row["filename_kind"] == "SOLUTION_JAVA_FILENAME" for row in rows),
        "readme_paths": sum(row["file_kind"] == "README" for row in rows), "tree_entries": tree_entry_total,
        "not_evaluated_blob_rows": len(rows), "separate_reference_only_candidate_rows": len(candidates),
        "target_implementation_admissions": 0,
    }
    require(combined == counts["combined"] and len(rows) == 9996 and directory_total == 6760, "combined count drift")
    public = verify_forks()
    for alias in read_json("PUBLIC_DONOR_ALIASES.json")["aliases"]:
        repository = json.loads(alias["canonical_response_body"])
        redirect = json.loads(alias["historical_response_body"])
        require(repository["full_name"] == alias["canonical_repository"] in PINS
                and repository["id"] == alias["repository_id"] and repository["private"] is False
                and repository["visibility"] == "public", "unconfirmed public donor alias")
        require(redirect["message"] == "Moved Permanently"
                and redirect["url"] == "https://api.github.com/repositories/" + str(repository["id"]),
                "historical repository redirect mismatch")
        public.add(alias["historical_repository"].lower())
    for path in ROOT.rglob("*"):
        if path.is_file() and path.suffix != ".gz":
            raw_documents.append(path.read_text(encoding="utf-8"))
    pattern = re.compile(r"https?://(?:api\.github\.com/repos/|github\.com/|raw\.githubusercontent\.com/)([^/\\\s\"<>()]+/[^/\\\s\"<>()]+)")
    for document in raw_documents:
        require(all(repository.lower().removesuffix(".git") in public for repository in pattern.findall(document)),
                "GitHub URL outside retained public repository scope")
    print(json.dumps({"status": "PASS_METADATA_ONLY", "sealed_files": sealed_files,
                      "blob_occurrences": len(rows), "java_paths": combined["java_paths"],
                      "readme_paths": combined["readme_paths"], "verified_git_trees": directory_total,
                      "reference_only_candidates": len(candidates), "public_fork_rows": 9,
                      "public_historical_donor_aliases": 1,
                      "target_implementation_admissions": 0, "java_jni_execution": "NOT_RUN"}, sort_keys=True))


if __name__ == "__main__":
    main()
