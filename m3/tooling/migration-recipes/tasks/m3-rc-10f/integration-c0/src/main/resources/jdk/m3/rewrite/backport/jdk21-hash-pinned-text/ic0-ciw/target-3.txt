#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Check ClassVer whitespace while preserving an exact, closed evidence snapshot.

This is an explicit immutable-evidence policy, not a directory exemption or proof
qualification. All other changed paths use Git's ordinary whitespace checker.
"""
import hashlib
import json
import os
from pathlib import Path
import stat
import subprocess
import sys
import types

LEDGER = "m3/migration/classver-evidence.json"
LEDGER_SHA256 = "da68bc2e110f1e6cfcadef240393c121c86a8bcc3341202a900e2c8e3466e1b5"
AUTHORITY = {
    "repository": "hsoliwal/M3jdk21",
    "commit": "8891dd43632cf6e5e7b174c5badd5c0d532f9541",
    "tree": "90ff0bbaea96b14dc65806ee10b587deee7eaffc",
}
HELPER = {
    "path": "m3/migration/recipe.py",
    "mode": "100644",
    "gitBlob": "02f88f9f8b1eb8930fff59684c81bb30819800ad",
    "sha256": "453d976a5f29526432be138b357d3f52f868fcc40a4ee697d969cc0836359628",
    "bytes": 9860,
}
ROLES = frozenset((
    "raw-log", "patch-snapshot", "before-image", "historical-source",
    "attribution-snapshot", "ci-excerpt", "coverage-snapshot",
))
COUNT = 60
MAX_LEDGER_BYTES = 262144
MAX_EVIDENCE_BYTES = 16777216


def _require(condition, message):
    if not condition:
        raise ValueError(message)


def _sha(data):
    return hashlib.sha256(data).hexdigest()


def _git(root, *arguments, data=None):
    result = subprocess.run(
        ["git", "-C", str(root), *arguments], input=data,
        stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=False)
    if result.returncode:
        raise ValueError("Git command refused: " + result.stderr.decode("utf-8", "replace").strip())
    return result.stdout


def _regular(root, relative):
    # Bootstrap the already-pinned helper without first executing unverified code.
    path = root
    for component in relative.split("/"):
        path /= component
        _require(not path.is_symlink(), "symlink refused: " + relative)
    _require(path.is_file() and stat.S_ISREG(path.stat().st_mode),
             "regular file required: " + relative)
    _require(not (path.stat().st_mode & 0o111), "executable evidence/helper refused: " + relative)
    return path


def _owner(root):
    path = _regular(root, HELPER["path"])
    _require(path.stat().st_size == HELPER["bytes"], "pinned recipe helper drift")
    data = path.read_bytes()
    _require(len(data) == HELPER["bytes"] and _sha(data) == HELPER["sha256"],
             "pinned recipe helper drift")
    owner = types.ModuleType("_classver_recipe_owner")
    owner.__file__ = str(path)
    sys.dont_write_bytecode = True
    # Execute the verified buffer, never a stale .pyc or a second pathname read.
    exec(compile(data, str(path), "exec"), owner.__dict__)
    return owner


def _ledger(root, owner):
    path = owner.checked_path(root, LEDGER)
    _require(_regular(root, LEDGER).stat().st_size <= MAX_LEDGER_BYTES,
             "evidence ledger byte budget exceeded")
    data = owner.read_optional(path)
    _require(data is not None and len(data) <= MAX_LEDGER_BYTES
             and owner.digest(data) == LEDGER_SHA256, "immutable evidence ledger drift")
    document = json.loads(data)
    _require(set(document) == {"schema", "authority", "observedFailure", "files"},
             "unsupported evidence ledger fields")
    _require(document["schema"] == "m3.classver-evidence/1"
             and document["authority"] == AUTHORITY, "wrong evidence authority")
    rows = document["files"]
    _require(isinstance(rows, list) and len(rows) == COUNT, "wrong evidence count")
    previous = ""
    total = 0
    for row in rows:
        _require(isinstance(row, dict) and set(row) == {
            "path", "mode", "gitBlob", "sha256", "bytes", "role", "provenance"},
            "invalid evidence row")
        name = row["path"]
        owner.checked_path(root, name)
        _require(name > previous and not any(ord(c) < 32 or ord(c) == 127 for c in name)
                 and ".git" not in (part.lower() for part in name.split("/")),
                 "duplicate, unordered or unsafe evidence path")
        previous = name
        _require(row["role"] in ROLES and row["mode"] == "100644",
                 "invalid immutable evidence role/mode")
        _require(type(row["bytes"]) is int and 0 <= row["bytes"] <= MAX_EVIDENCE_BYTES,
                 "invalid evidence byte count")
        for field, length in (("sha256", 64), ("gitBlob", 40)):
            value = row[field]
            _require(isinstance(value, str) and len(value) == length
                     and all(c in "0123456789abcdef" for c in value),
                     "invalid evidence identity")
        _require(isinstance(row["provenance"], list) and row["provenance"],
                 "missing immutable evidence provenance")
        total += row["bytes"]
    _require(total <= MAX_EVIDENCE_BYTES, "evidence byte budget exceeded")
    ledger_row = {
        "path": LEDGER, "mode": "100644", "sha256": LEDGER_SHA256,
        "gitBlob": hashlib.sha1(b"blob " + str(len(data)).encode("ascii")
                               + b"\0" + data).hexdigest(),
        "bytes": len(data),
    }
    return rows, ledger_row


def _endpoints(root):
    return tuple(_git(root, "rev-parse", "--verify", value).decode("ascii").strip()
                 for value in ("HEAD^", "HEAD"))


def _verify(root, head, rows, owner):
    names = [row["path"] for row in rows]
    tree = _git(root, "ls-tree", "-z", "--full-tree", head, "--", *names)
    entries = {}
    for record in tree.split(b"\0"):
        if not record:
            continue
        metadata, name = record.split(b"\t", 1)
        mode, kind, oid = metadata.decode("ascii").split(" ")
        name = name.decode("utf-8")
        _require(name not in entries, "duplicate Git tree path")
        entries[name] = (mode, kind, oid)
    _require(set(entries) == set(names), "immutable path missing, renamed or aliased")
    objects = _git(root, "cat-file", "--batch",
                   data="".join(row["gitBlob"] + "\n" for row in rows).encode("ascii"))
    position = 0
    for row in rows:
        name = row["path"]
        _require(entries[name] == (row["mode"], "blob", row["gitBlob"]),
                 "immutable Git identity/mode drift: " + name)
        end = objects.find(b"\n", position)
        _require(end >= position, "invalid Git object response")
        header = objects[position:end]
        expected = f'{row["gitBlob"]} blob {row["bytes"]}'.encode("ascii")
        _require(header == expected, "immutable Git object size/type drift: " + name)
        start = end + 1
        stop = start + row["bytes"]
        content = objects[start:stop]
        _require(len(content) == row["bytes"] and objects[stop:stop + 1] == b"\n"
                 and owner.digest(content) == row["sha256"],
                 "immutable Git object bytes drift: " + name)
        position = stop + 1
        path = owner.checked_path(root, name)
        _require(_regular(root, name).stat().st_size == row["bytes"],
                 "immutable checkout size drift: " + name)
        local = owner.read_optional(path)
        _require(local == content, "immutable checkout bytes drift: " + name)
    _require(position == len(objects), "unexpected Git object response tail")


def check(root):
    root = Path(root).absolute()
    _require(not root.is_symlink(), "symlink repository root refused")
    actual = Path(_git(root, "rev-parse", "--show-toplevel").decode("utf-8").strip())
    _require(root == actual, "run ClassVer admission from the repository root")
    owner = _owner(root)
    rows, ledger_row = _ledger(root, owner)
    endpoints = _endpoints(root)
    _verify(root, endpoints[1], [HELPER, ledger_row, *rows], owner)
    # Only authenticated literal files are omitted. Every unlisted path, including
    # source/test/build/control files below the same intake directory, stays checked.
    command = ["git", "-C", str(root), "diff", "--check", *endpoints, "--", ".",
               *(":(top,literal,exclude)" + row["path"] for row in rows)]
    result = subprocess.run(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=False)
    _require(_endpoints(root) == endpoints, "Git endpoints changed during admission")
    _verify(root, endpoints[1], [HELPER, ledger_row, *rows], owner)
    _require(_sha(_regular(root, LEDGER).read_bytes()) == LEDGER_SHA256,
             "evidence ledger changed during admission")
    if result.returncode:
        sys.stderr.buffer.write(result.stdout)
        sys.stderr.buffer.write(result.stderr)
        raise ValueError("native git diff --check failed with exit " + str(result.returncode))
    return {
        "schema": "m3.classver-evidence-diff/1",
        "status": "PASS_CLASSVER_EVIDENCE_DIFF",
        "base": endpoints[0], "head": endpoints[1],
        "authority": AUTHORITY,
        "ledgerSHA256": LEDGER_SHA256,
        "helperSHA256": HELPER["sha256"],
        "admittedEvidence": COUNT,
        "admittedEvidenceBytes": sum(row["bytes"] for row in rows),
        "checkedScope": "all changed paths except these exact authenticated evidence identities",
        "gitDiffCheckExit": result.returncode,
        "evidenceWrites": 0,
        "qualification": "evidence integrity and whitespace only",
    }


def main():
    _require(len(sys.argv) == 1, "ClassVer evidence policy takes no override arguments")
    print(json.dumps(check(Path.cwd()), sort_keys=True))


if __name__ == "__main__":
    try:
        main()
    except (ValueError, OSError, KeyError, TypeError, AttributeError) as failure:
        print("REFUSED: " + str(failure), file=sys.stderr)
        sys.exit(2)
