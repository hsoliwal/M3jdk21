#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Hydrate verified Git objects and run the unchanged committed provenance gate.

This publication adapter creates a fresh partial object checkout. It does not
transform product source, fetch anything, publish refs, or establish a full build.
"""

from __future__ import annotations

import argparse
import csv
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import re
import shutil
import subprocess
import sys
import traceback


CHECKER = "scripts/legal/check_m3_provenance.py"
POLICY = "docs/M3-SCALE/provenance/m3-provenance-policy.json"
LEDGER = "docs/M3-SCALE/provenance/M3_FILE_PROVENANCE.tsv"
AUTHORITIES = {
    CHECKER: "55e8183c58e4bf3608355e650e3be18609feab70f7baa75c935716b396d24900",
    POLICY: "4a3bbc60b2960774058c8a822999e8668239c9192571633480463d47d31381dc",
    "LICENSE": "1eb85fc97224598dad1852b5d6483bbcf0aa8608790dcc657a5a2a761ae9c8c6",
}
PLAN = "design/provenance/EXACT_COMMIT_GATE_PLAN.md"
PLAN_SHA256 = "cdc5e26afb095a084c08615725de28f938706272c6f160c33f281c69f5cb3049"
PLAN_ADDENDUM = "design/provenance/EXACT_COMMIT_GATE_PLAN_ADDENDUM.md"
PLAN_ADDENDUM_SHA256 = "7089b5277e6f7019d577067a806542f165d8d175320e7c7ab02f920d60745086"
UNSIGNED_OFFSETS = ("+0000", "+0530")


def require(ok, message):
    if not ok:
        raise ValueError(message)


def sha256(data):
    return hashlib.sha256(data).hexdigest()


def git_id(kind, data):
    return hashlib.sha1(kind.encode("ascii") + b" " + str(len(data)).encode("ascii")
                        + b"\0" + data).hexdigest()


def digest(value, size=40):
    require(isinstance(value, str) and re.fullmatch("[0-9a-f]{" + str(size) + "}", value)
            and value != "0" * size, f"invalid {size}-digit digest: {value!r}")
    return value


def fields(obj, required, optional=()):
    require(isinstance(obj, dict), "expected JSON object")
    require(set(required) <= obj.keys() and obj.keys() <= set(required) | set(optional),
            f"unknown or missing fields: {sorted(obj)}; required={sorted(required)}")
    for key in ("url", "html_url", "node_id"):
        if key in obj:
            require(isinstance(obj[key], str) and obj[key], f"invalid API field {key}")


def unique_object(pairs):
    result = {}
    for key, value in pairs:
        require(key not in result, f"duplicate JSON key: {key}")
        result[key] = value
    return result


def read_json(path):
    raw = path.read_bytes()
    return json.loads(raw.decode("utf-8"), object_pairs_hook=unique_object,
                      parse_constant=lambda value: (_ for _ in ()).throw(
                          ValueError(f"nonfinite JSON value: {value}"))), raw


def repository_path(value):
    require(isinstance(value, str) and value, "missing repository path")
    pure = PurePosixPath(value)
    require(not pure.is_absolute() and pure.as_posix() == value and value != "."
            and ".." not in pure.parts and "\\" not in value
            and not any(part.lower() == ".git" for part in pure.parts)
            and not any(ord(char) < 32 or ord(char) == 127 for char in value),
            f"unsafe repository path: {value!r}")
    return value


def local_path(root, argument, existing=True):
    require(isinstance(argument, str) and argument, "missing local input path")
    given = Path(argument)
    given = given if given.is_absolute() else root / given
    require(".." not in given.parts, f"noncanonical local path: {given}")
    resolved = given.resolve(strict=existing)
    require(resolved != root and root in resolved.parents, f"path outside task root: {given}")
    for part in (given, *given.parents):
        require(not part.is_symlink(), f"symlink input/output path: {given}")
        if part == root:
            break
    if existing:
        require(resolved.is_file(), f"not a regular input file: {resolved}")
    return resolved


def seal_file(path):
    raw = path.read_bytes()
    return {"bytes": len(raw), "sha256": sha256(raw), "git_blob": git_id("blob", raw)}


def identity(obj):
    fields(obj, ("name", "email", "date"))
    require(all(isinstance(obj[key], str) for key in obj), "invalid commit identity")
    require(not any(c in obj["name"] + obj["email"] for c in "\r\n\0<>"),
            "unsupported identity delimiter")
    timestamp = datetime.fromisoformat(obj["date"].replace("Z", "+00:00"))
    require(timestamp.tzinfo is not None and timestamp.microsecond == 0,
            "commit date requires exact timezone-aware seconds")
    epoch = int(timestamp.astimezone(timezone.utc).timestamp())
    return f'{obj["name"]} <{obj["email"]}>', epoch


def messages(message):
    require(isinstance(message, str) and "\0" not in message, "invalid commit message")
    return (message, message[:-1] if message.endswith("\n") else message + "\n")


def encode_commit(obj):
    fields(obj, ("sha", "tree", "parents", "author", "committer", "message", "verification"),
           ("node_id", "url", "html_url"))
    oid = digest(obj["sha"])
    fields(obj["tree"], ("sha",), ("url",))
    tree = digest(obj["tree"]["sha"])
    require(isinstance(obj["parents"], list), "parents must be an ordered list")
    parents = []
    for parent in obj["parents"]:
        fields(parent, ("sha",), ("url", "html_url"))
        parents.append(digest(parent["sha"]))
    require(len(set(parents)) == len(parents), "duplicate commit parent")
    author, author_epoch = identity(obj["author"])
    committer, committer_epoch = identity(obj["committer"])
    alternatives = messages(obj["message"])
    verification = obj["verification"]
    fields(verification, ("verified", "reason", "signature", "payload"), ("verified_at",))
    require(type(verification["verified"]) is bool and isinstance(verification["reason"], str),
            "invalid verification metadata")
    require(verification.get("verified_at") is None or isinstance(verification["verified_at"], str),
            "invalid verification timestamp")
    prefix = [f"tree {tree}"] + [f"parent {parent}" for parent in parents]
    if verification["signature"] is None:
        require(verification["payload"] is None and not verification["verified"],
                "unsigned metadata has a payload or verified signature")
        candidates = []
        for author_offset in UNSIGNED_OFFSETS:
            for committer_offset in UNSIGNED_OFFSETS:
                headers = prefix + [f"author {author} {author_epoch} {author_offset}",
                                    f"committer {committer} {committer_epoch} {committer_offset}"]
                candidates.extend(("\n".join(headers) + "\n\n" + message).encode("utf-8")
                                  for message in alternatives)
        matches = [raw for raw in candidates if git_id("commit", raw) == oid]
        require(len(matches) == 1, f"unsigned commit cannot be reconstructed exactly: {oid}")
        raw = matches[0]
        signed = False
    else:
        signature, payload = verification["signature"], verification["payload"]
        require(isinstance(signature, str) and signature and isinstance(payload, str)
                and "\0" not in signature and "\0" not in payload,
                "signed commit requires exact signature and payload")
        headers, separator, message = payload.partition("\n\n")
        lines = headers.split("\n")
        require(separator and len(lines) == len(prefix) + 2 and lines[:len(prefix)] == prefix
                and message in alternatives, f"unsupported signed payload headers/message: {oid}")
        for line, label, name, epoch in ((lines[-2], "author", author, author_epoch),
                                         (lines[-1], "committer", committer, committer_epoch)):
            start = f"{label} {name} "
            require(line.startswith(start), "signed payload identity differs from API metadata")
            match = re.fullmatch(r"(-?(?:0|[1-9][0-9]*)) ([+-])([0-9]{2})([0-9]{2})", line[len(start):])
            require(match is not None and int(match[1]) == epoch and int(match[3]) < 24
                    and int(match[4]) < 60, "signed payload timestamp differs from API metadata")
        signature_lines = signature.split("\n")
        require(signature_lines and signature_lines[0], "empty signature header")
        signature_header = "gpgsig " + signature_lines[0] + "".join(
            "\n " + line for line in signature_lines[1:])
        raw = (headers + "\n" + signature_header + "\n\n" + message).encode("utf-8")
        require(git_id("commit", raw) == oid, f"signed commit hash mismatch: {oid}")
        signed = True
    return oid, {"raw": raw, "tree": tree, "parents": parents, "signed": signed}


def encode_tree(obj):
    fields(obj, ("sha", "tree", "truncated"), ("url",))
    oid = digest(obj["sha"])
    require(obj["truncated"] is False and isinstance(obj["tree"], list), "truncated/invalid Git tree")
    entries = {}
    for entry in obj["tree"]:
        fields(entry, ("path", "mode", "type", "sha"), ("size", "url"))
        name = repository_path(entry["path"])
        require("/" not in name and name not in entries, "recursive or duplicate tree entry")
        mode = "40000" if entry["mode"] in ("040000", "40000") else entry["mode"]
        expected_type = {"40000": "tree", "100644": "blob", "100755": "blob",
                         "120000": "blob", "160000": "commit"}.get(mode)
        require(expected_type is not None and entry["type"] == expected_type, "unsupported tree mode/type")
        if "size" in entry:
            require(type(entry["size"]) is int and entry["size"] >= 0 and entry["type"] == "blob",
                    "invalid tree entry size")
        entries[name] = {"mode": mode, "type": entry["type"], "sha": digest(entry["sha"])}
    ordered = sorted(entries, key=lambda name: name.encode("utf-8") +
                     (b"/" if entries[name]["type"] == "tree" else b"\0"))
    raw = b"".join(entries[name]["mode"].encode("ascii") + b" " + name.encode("utf-8")
                   + b"\0" + bytes.fromhex(entries[name]["sha"]) for name in ordered)
    require(git_id("tree", raw) == oid, f"tree hash mismatch: {oid}")
    return oid, {"raw": raw, "entries": entries}


def tree_lookup(trees, tree, path):
    parts = PurePosixPath(path).parts
    for index, part in enumerate(parts):
        require(tree in trees, f"missing required ancestor tree: {tree} for {path}")
        entry = trees[tree]["entries"].get(part)
        if entry is None:
            return None
        if index == len(parts) - 1:
            return entry
        require(entry["type"] == "tree", f"non-directory ancestor of {path}")
        tree = entry["sha"]
    raise ValueError("empty lookup path")


def tree_changes(trees, before, after, prefix, result, opaque):
    if before == after:
        opaque.append({"path": prefix or ".", "tree": before})
        return
    require(after in trees and (before is None or before in trees),
            f"missing changed tree at {prefix}: {before} -> {after}")
    old = {} if before is None else trees[before]["entries"]
    new = trees[after]["entries"]
    for name in sorted(set(old) | set(new)):
        a, b = old.get(name), new.get(name)
        path = f"{prefix}/{name}" if prefix else name
        if a == b:
            if b["type"] == "tree":
                opaque.append({"path": path, "tree": b["sha"]})
            continue
        require(b is not None, f"deletion outside publication scope: {path}")
        if b["type"] == "tree":
            require(a is None or a["type"] == "tree", f"type change: {path}")
            tree_changes(trees, None if a is None else a["sha"], b["sha"], path, result, opaque)
        else:
            require(b["type"] == "blob" and b["mode"] in ("100644", "100755"),
                    f"unsupported changed file mode: {path}")
            require(a is None or (a["type"] == "blob" and a["mode"] == b["mode"]
                                 and a["sha"] != b["sha"]), f"type/mode-only change: {path}")
            result[path] = {"status": "A" if a is None else "M", "before": a, "after": b}


class Commands:
    def __init__(self, output, repo, git):
        self.output, self.repo, self.git = output, repo, git
        self.count = 0
        self.env = {key: value for key, value in os.environ.items()
                    if not key.startswith("GIT_") and key not in ("PYTHONPATH", "PYTHONHOME")}
        self.env.update(GIT_CONFIG_NOSYSTEM="1", GIT_CONFIG_GLOBAL=os.devnull,
                        GIT_CONFIG_SYSTEM=os.devnull, GIT_TERMINAL_PROMPT="0",
                        GIT_NO_REPLACE_OBJECTS="1", PYTHONDONTWRITEBYTECODE="1")

    def run(self, label, argv, data=None, allow_failure=False):
        self.count += 1
        completed = subprocess.run([str(arg) for arg in argv], cwd=self.output,
                                   env=self.env, input=data, stdout=subprocess.PIPE,
                                   stderr=subprocess.PIPE, check=False)
        stem = f"{self.count:04d}-{label}"
        for suffix, content in (("stdout", completed.stdout), ("stderr", completed.stderr)):
            with (self.output / f"{stem}.{suffix}.log").open("xb") as stream:
                stream.write(content)
        record = {"label": label, "argv": [str(arg) for arg in argv], "cwd": str(self.output),
                  "exit": completed.returncode, "stdout": f"{stem}.stdout.log",
                  "stdout_sha256": sha256(completed.stdout), "stderr": f"{stem}.stderr.log",
                  "stderr_sha256": sha256(completed.stderr)}
        if data is not None:
            record.update(stdin_bytes=len(data), stdin_sha256=sha256(data))
        with (self.output / "COMMANDS.jsonl").open("a", encoding="utf-8") as stream:
            stream.write(json.dumps(record, sort_keys=True) + "\n")
        require(allow_failure or completed.returncode == 0,
                f"command failed ({completed.returncode}): {label}; retained {stem}.stderr.log")
        return completed

    def git_run(self, label, *args, data=None):
        return self.run(label, [self.git, "-C", self.repo, *args], data=data).stdout

    def object(self, kind, oid, data):
        require(git_id(kind, data) == oid, f"unverified object before write: {oid}")
        actual = self.git_run("write-" + kind, "hash-object", "-w", "-t", kind, "--stdin", data=data)
        require(actual == (oid + "\n").encode("ascii"), f"Git wrote unexpected {kind} identity")


def write_json(path, value):
    with path.open("x", encoding="utf-8") as stream:
        stream.write(json.dumps(value, indent=2, sort_keys=True) + "\n")


def execute(args, state):
    root = Path(args.task_root).resolve(strict=True)
    require(root.is_dir(), "invalid task root")
    objects_path = local_path(root, args.objects_json)
    manifest_path = local_path(root, args.manifest)
    output = local_path(root, args.output_dir, existing=False)
    publication = root / "publication-v2"
    require(publication in output.parents and not output.exists() and not output.is_symlink()
            and output.parent.is_dir(), "output must be a fresh child of publication-v2 with an existing parent")
    base, head = digest(args.base_sha), digest(args.head_sha)
    require(base != head, "base and head must differ")
    require(sha256(local_path(root, PLAN).read_bytes()) == PLAN_SHA256, "frozen adapter plan drift")
    require(sha256(local_path(root, PLAN_ADDENDUM).read_bytes()) == PLAN_ADDENDUM_SHA256,
            "frozen adapter plan addendum drift")
    supplied, objects_raw = read_json(objects_path)
    fields(supplied, ("commits", "trees"))
    require(isinstance(supplied["commits"], list) and isinstance(supplied["trees"], list), "invalid object lists")
    commits, trees = {}, {}
    for kind, encoder, collection in (("commits", encode_commit, commits), ("trees", encode_tree, trees)):
        for item in supplied[kind]:
            oid, value = encoder(item)
            require(oid not in collection, f"duplicate supplied object: {oid}")
            collection[oid] = value
    require(base in commits and head in commits, "missing explicit base/head commit")
    seen, visiting = set(), set()

    def ancestry(oid):
        require(oid in commits and oid not in visiting, f"missing/cyclic commit ancestry: {oid}")
        if oid in seen:
            return
        visiting.add(oid)
        if oid != base:
            require(commits[oid]["parents"], "history terminates before explicit base")
            for parent in commits[oid]["parents"]:
                ancestry(parent)
        visiting.remove(oid)
        seen.add(oid)

    ancestry(head)
    require(base in seen and seen == set(commits), "extraneous commits or base is not the history boundary")
    require(all(commit["tree"] in trees for commit in commits.values()), "missing supplied commit root tree")
    before_tree, after_tree = commits[base]["tree"], commits[head]["tree"]
    changes, opaque = {}, []
    tree_changes(trees, before_tree, after_tree, "", changes, opaque)
    manifest, manifest_raw = read_json(manifest_path)
    require(isinstance(manifest, list) and manifest, "manifest must be the complete nonempty Syn delta list")
    files = {}
    for item in manifest:
        fields(item, ("repo", "path", "source", "sha256", "before_sha256"),
               ("status", "bytes", "git_blob", "phase"))
        require(item["repo"] == "syn", "foreign repository in final Syn manifest")
        path = repository_path(item["path"])
        require(path not in files and path not in AUTHORITIES, f"duplicate or changed authority path: {path}")
        status = "A" if item["before_sha256"] is None else "M"
        require(item.get("status", status) == status, f"status/preimage mismatch: {path}")
        require(path in changes and changes[path]["status"] == status, f"manifest differs from actual Git trees: {path}")
        source = local_path(root, item["source"])
        current = seal_file(source)
        require(current["sha256"] == digest(item["sha256"], 64)
                and current["git_blob"] == changes[path]["after"]["sha"], f"postimage mismatch: {path}")
        for optional in ("bytes", "git_blob"):
            if optional in item:
                require(optional != "bytes" or type(item[optional]) is int,
                        f"manifest byte count must be an integer: {path}")
                require(item[optional] == current[optional], f"manifest {optional} mismatch: {path}")
        if "phase" in item:
            require(isinstance(item["phase"], str), "invalid manifest phase")
        before_source = None
        if status == "M":
            before_source = local_path(root, "input/syn/" + path)
            original = seal_file(before_source)
            require(original["sha256"] == digest(item["before_sha256"], 64)
                    and original["git_blob"] == changes[path]["before"]["sha"], f"preimage mismatch: {path}")
        files[path] = {**current, "status": status, "source": str(source),
                       "mode": changes[path]["after"]["mode"],
                       "before_source": str(before_source) if before_source else None}
    require(set(files) == set(changes), "manifest omits actual committed changes")
    require(LEDGER in files and files[LEDGER]["status"] == "M", "final modified ledger must be in the delta")
    authorities = {}
    for path, expected in AUTHORITIES.items():
        source = local_path(root, "input/syn/" + path)
        entry = tree_lookup(trees, before_tree, path)
        require(entry is not None and entry == tree_lookup(trees, after_tree, path)
                and entry["type"] == "blob" and entry["mode"] in ("100644", "100755"),
                f"authority differs or is missing from base/head: {path}")
        current = seal_file(source)
        require(current["sha256"] == expected and current["git_blob"] == entry["sha"],
                f"unchanged authority bytes do not match actual commit: {path}")
        authorities[path] = {**current, "source": str(source), "mode": entry["mode"]}

    git = shutil.which("git")
    require(git is not None, "git executable unavailable")
    output.mkdir()
    state["output"] = output
    repo = output / "repo"
    empty_template = output / "empty-git-template"
    empty_template.mkdir()
    commands = Commands(output, repo, git)
    commands.run("git-init", [git, "init", "--quiet", "--object-format=sha1",
                               "--template=" + str(empty_template), repo])
    for collection, kind in ((trees, "tree"), (commits, "commit")):
        for oid, value in sorted(collection.items()):
            commands.object(kind, oid, value["raw"])
    written_blobs = set()
    for path, record in {**files, **authorities}.items():
        source = local_path(root, record["source"])
        raw = source.read_bytes()
        require(sha256(raw) == record["sha256"] and git_id("blob", raw) == record["git_blob"],
                f"source changed before materialization: {path}")
        target = repo / path
        target.parent.mkdir(parents=True, exist_ok=True)
        with target.open("xb") as stream:
            stream.write(raw)
        target.chmod(0o755 if record["mode"] == "100755" else 0o644)
        if record["git_blob"] not in written_blobs:
            commands.object("blob", record["git_blob"], raw)
            written_blobs.add(record["git_blob"])
        if record.get("before_source"):
            original = local_path(root, record["before_source"]).read_bytes()
            oid = changes[path]["before"]["sha"]
            require(git_id("blob", original) == oid, f"preimage changed before object write: {path}")
            if oid not in written_blobs:
                commands.object("blob", oid, original)
                written_blobs.add(oid)
    (repo / ".git/shallow").write_bytes((base + "\n").encode("ascii"))
    commands.git_run("base-ref", "update-ref", "refs/heads/publication-base", base)
    commands.git_run("head-ref", "update-ref", "refs/heads/publication-head", head)
    commands.git_run("head-symbolic-ref", "symbolic-ref", "HEAD", "refs/heads/publication-head")
    for label, oid in (("base", base), ("head", head)):
        require(commands.git_run("resolve-" + label, "rev-parse", "--verify", oid + "^{commit}")
                == (oid + "\n").encode("ascii"), f"explicit {label} cannot resolve exactly")
    merge_base = commands.git_run("merge-base", "merge-base", "--all", base, head)
    require(merge_base == (base + "\n").encode("ascii"), "unique merge base differs from explicit base")
    diff = commands.git_run("actual-committed-diff", "diff", "--name-status", "-z",
                            "--find-renames", base + "..." + head)
    parts = diff.split(b"\0")
    require(parts[-1] == b"" and len(parts[:-1]) % 2 == 0, "invalid Git name-status output")
    actual = {}
    for index in range(0, len(parts) - 1, 2):
        status, path = parts[index].decode("utf-8"), parts[index + 1].decode("utf-8")
        require(status in ("A", "M") and path not in actual, "unsupported or duplicate Git diff action")
        actual[path] = status
    require(actual == {path: record["status"] for path, record in files.items()},
            "actual base...head diff differs from complete final manifest")
    gate_output = output / "provenance"
    gate = commands.run("unchanged-provenance-checker", [sys.executable, "-I", "-B", repo / CHECKER,
        "--repo-root", repo, "--base-ref", base, "--head-ref", head,
        "--policy", POLICY, "--ledger", LEDGER, "--output-dir", gate_output], allow_failure=True)
    require(gate.returncode == 0, "unchanged provenance checker failed; all reports retained")
    with (gate_output / "PROVENANCE_FILES.tsv").open(encoding="utf-8", newline="") as stream:
        reader = csv.DictReader(stream, delimiter="\t")
        require(reader.fieldnames == ["status", "path", "oldPath", "classification", "sha256"],
                "unexpected provenance files schema")
        reported = {}
        for row in reader:
            require(row["path"] not in reported and row["oldPath"] == "-", "duplicate/renamed reported path")
            reported[row["path"]] = (row["status"], row["sha256"])
    require(reported == {path: (record["status"], record["sha256"]) for path, record in files.items()},
            "PROVENANCE_FILES.tsv differs from exact final manifest including ledger")
    with (gate_output / "PROVENANCE_GATE.tsv").open(encoding="utf-8", newline="") as stream:
        gate_rows = list(csv.reader(stream, delimiter="\t"))
    require(gate_rows[0] == ["key", "value"] and all(len(row) == 2 for row in gate_rows[1:]),
            "unexpected provenance gate schema")
    gate_values = dict(gate_rows[1:])
    require(len(gate_values) == len(gate_rows) - 1 and gate_values["baseRef"] == base
            and gate_values["headRef"] == head and gate_values["status"] == "PASS"
            and gate_values["violationCount"] == "0"
            and gate_values["changedPathCount"] == str(len(files)), "provenance gate metadata mismatch")
    with (gate_output / "PROVENANCE_VIOLATIONS.tsv").open(encoding="utf-8", newline="") as stream:
        require(list(csv.reader(stream, delimiter="\t")) == [["path", "code", "message"]],
                "nonempty provenance violations")
    for path, record in {**files, **authorities}.items():
        require(seal_file(repo / path) == {key: record[key] for key in ("bytes", "sha256", "git_blob")}
                and seal_file(local_path(root, record["source"])) ==
                {key: record[key] for key in ("bytes", "sha256", "git_blob")}, f"materialized/source drift: {path}")
    require(objects_path.read_bytes() == objects_raw and manifest_path.read_bytes() == manifest_raw,
            "input JSON changed during gate")
    receipt = {
        "schema": "m3.integrated.collections.exact-commit-provenance.v1", "status": "PASS",
        "base_sha": base, "head_sha": head, "merge_base_sha": base, "shallow_boundary": base,
        "scope": "Exact committed changed-file provenance in a partial object checkout; not a complete clone, signature-trust check, remote publication, full build or product acceptance.",
        "objects_json": {"path": str(objects_path), "sha256": sha256(objects_raw)},
        "manifest": {"path": str(manifest_path), "sha256": sha256(manifest_raw), "files": len(files)},
        "plan_sha256": PLAN_SHA256, "plan_addendum_sha256": PLAN_ADDENDUM_SHA256,
        "adapter_sha256": sha256(Path(__file__).read_bytes()),
        "git_executable": git, "python_executable": sys.executable,
        "hydrated_commits": [{"sha": oid, "sha256": sha256(value["raw"]),
                               "signed_payload_reconstructed": value["signed"]}
                              for oid, value in sorted(commits.items())],
        "hydrated_trees": [{"sha": oid, "sha256": sha256(value["raw"]),
                             "entries": len(value["entries"])} for oid, value in sorted(trees.items())],
        "hydrated_blob_count": len(written_blobs), "unchanged_opaque_subtrees": opaque,
        "files": files, "unchanged_authorities": authorities, "command_count": commands.count,
        "commands_sha256": sha256((output / "COMMANDS.jsonl").read_bytes()),
        "checker_reports": {name: seal_file(gate_output / name) for name in
                            ("PROVENANCE_GATE.tsv", "PROVENANCE_FILES.tsv", "PROVENANCE_VIOLATIONS.tsv")},
        "exact_report_status_path_hash_match": True,
    }
    write_json(output / "RECEIPT.json", receipt)
    return receipt


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ("task-root", "objects-json", "manifest", "base-sha", "head-sha", "output-dir"):
        parser.add_argument("--" + name, required=True)
    state = {}
    try:
        receipt = execute(parser.parse_args(), state)
    except Exception as error:
        if "output" in state:
            write_json(state["output"] / "FAILURE.json", {"status": "FAIL", "error": str(error),
                "traceback": traceback.format_exc(), "scope": "Partial publication gate; preserve all created evidence."})
        print(f"Exact committed provenance gate: FAIL: {error}", file=sys.stderr)
        return 1
    print(f'Exact committed provenance gate: PASS ({receipt["manifest"]["files"]} changed paths); '
          f'base={receipt["base_sha"]} head={receipt["head_sha"]}')
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
