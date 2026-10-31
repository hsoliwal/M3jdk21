#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Verify the bounded 090 acquisition and immutable reuse proof; no network/tests."""
import base64
import hashlib
import json
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
AUDIT = ROOT / "receiving-merge-audit"
OLD_COMMIT = "c0a14387009aefc7d62bd3268055d526e04f9074"
OLD_ROOT = "491bbd80a51483d0710fc12c1bad3a037b385ba0"
NEW_COMMIT = "09075810b55229662329a158923287e8a7118f86"
NEW_ROOT = "bba8647ea80d1cb4101394b4c0fc5a97f0b8eb52"
DONOR_INPUT_COMMIT = "d1b9162cd568108f4c8d82f6b6a03cccfdb91bd2"
DONOR_INPUT_ROOT = "0812573f02908d8078d13f5f49346865cdf1c0e0"
API = "https://api.github.com/repos/hsoliwal/M3jdk21"

def require(ok, message):
    if not ok:
        raise ValueError(message)

def sha(body):
    return hashlib.sha256(body).hexdigest()

def git(kind, body):
    return hashlib.sha1(kind.encode() + b" " + str(len(body)).encode() + b"\0" + body).hexdigest()

def relative(path):
    return path.relative_to(ROOT).as_posix()

def envelope(path):
    data = json.loads(path.read_bytes())
    require(data["response"].get("isError") is False, "failed capture: " + str(path))
    return data

def captured_json(path):
    return json.loads(envelope(path)["response"]["structuredContent"]["content"])

def direct_tree(entries):
    require(len(entries) == len({row["path"] for row in entries}), "duplicate tree entry")
    body = bytearray()
    for row in sorted(entries, key=lambda r: r["path"].encode() + (b"/" if r["type"] == "tree" else b"")):
        name = row["path"]
        require(name and name not in (".", "..", ".git") and "/" not in name and "\\" not in name
                and not any(ord(c) < 32 for c in name), "non-direct or unsafe tree entry")
        require((row["mode"], row["type"]) in {
            ("040000", "tree"), ("100644", "blob"), ("100755", "blob"),
            ("120000", "blob"), ("160000", "commit")}, "unsupported tree mode")
        body.extend(row["mode"].lstrip("0").encode() + b" " + name.encode() + b"\0" + bytes.fromhex(row["sha"]))
    return git("tree", bytes(body))

def direct_capture(path, expected=None, allow_legacy_request_absence=False):
    data = envelope(path)
    tree = json.loads(data["response"]["structuredContent"]["content"])
    require(tree["truncated"] is False, "truncated direct tree")
    expected_url = API + "/git/trees/" + tree["sha"]
    require(data.get("request") == {"url": expected_url}
            or (allow_legacy_request_absence and "request" not in data), "non-direct tree request")
    require(tree["url"] == expected_url, "tree response URL mismatch")
    require(expected is None or tree["sha"] == expected, "unexpected tree identity")
    require(direct_tree(tree["tree"]) == tree["sha"], "direct tree reconstruction mismatch")
    for row in tree["tree"]:
        kind = "trees" if row["type"] == "tree" else "blobs" if row["type"] == "blob" else "commits"
        require(row["url"] == API + "/git/" + kind + "/" + row["sha"], "entry URL identity mismatch")
    return tree

def file_capture(path, expected_path, expected_commit):
    data = envelope(path)
    require(data["request"] == {"repository_full_name": "hsoliwal/M3jdk21", "path": expected_path,
            "ref": expected_commit, "encoding": "base64"}, "wrong immutable file request")
    content = data["response"]["structuredContent"]
    require(content["encoding"] == "base64", "wrong body encoding")
    body = base64.b64decode("".join(content["content"].split()), validate=True)
    require(git("blob", body) == content["sha"], "body Git identity mismatch")
    require(content["display_url"] == "https://github.com/hsoliwal/M3jdk21/blob/" + expected_commit + "/" + expected_path,
            "immutable returned path/ref mismatch")
    return body, content["sha"]

def walk(start, parts, trees):
    chain = []
    current = start
    for component in parts:
        rows = {row["path"]: row for row in current["tree"]}
        require(component in rows, "missing ancestry component: " + component)
        child = rows[component]
        require(child["type"] == "tree" and child["mode"] == "040000", "ancestry component is not a tree")
        chain.append({"component": component, "parent_tree": current["sha"], "tree": child["sha"]})
        current = trees[child["sha"]]
    return current, chain

def main():
    tree_rows, trees = [], {}
    for path in sorted((HERE / "captures").glob("*.json")):
        if path.name in ("new-matcher.json", "new-mr-manifest.json"):
            continue
        tree = direct_capture(path)
        trees[tree["sha"]] = tree
        tree_rows.append({"capture": relative(path), "capture_sha256": sha(path.read_bytes()),
                          "tree_sha1": tree["sha"], "entries": len(tree["tree"]), "reconstructed": True})
    roots = [
        ("old", OLD_COMMIT, OLD_ROOT, AUDIT / "current-map-validation-v03/root-evidence/M3_FINAL_CURRENT_COMMIT.json",
         AUDIT / "current-map-validation-v03/root-evidence/M3_FINAL_CURRENT_ROOT.json"),
        ("current", NEW_COMMIT, NEW_ROOT, ROOT / "publication/M3_LATEST_COMMIT.json", ROOT / "publication/M3_LATEST_ROOT.json"),
        ("original_body_epoch", DONOR_INPUT_COMMIT, DONOR_INPUT_ROOT,
         AUDIT / "current-map-validation/root-evidence/M3_FOLLOWUP_CURRENT_COMMIT.json",
         AUDIT / "current-map-validation/root-evidence/M3_FOLLOWUP_CURRENT_ROOT.json"),
    ]
    root_rows = []
    for role, commit_id, root_id, commit_path, root_path in roots:
        commit_envelope = envelope(commit_path)
        require(commit_envelope.get("request") == {"url": API + "/git/commits/" + commit_id}
                or (role == "original_body_epoch" and "request" not in commit_envelope), "wrong commit request")
        commit = json.loads(commit_envelope["response"]["structuredContent"]["content"])
        require(commit["sha"] == commit_id and commit["tree"]["sha"] == root_id, "commit/root mismatch")
        require(commit["url"] == API + "/git/commits/" + commit_id, "commit response URL mismatch")
        root = direct_capture(root_path, root_id, allow_legacy_request_absence=role == "original_body_epoch")
        trees[root_id] = root
        root_rows.append({"role": role, "commit": commit_id, "root_tree": root_id,
            "commit_capture": relative(commit_path), "commit_capture_sha256": sha(commit_path.read_bytes()),
            "root_capture": relative(root_path), "root_capture_sha256": sha(root_path.read_bytes()),
            "root_reconstructed": True, "commit_request_recorded": "request" in commit_envelope,
            "root_request_recorded": "request" in envelope(root_path)})
    old_src = {r["path"]: r for r in trees[OLD_ROOT]["tree"]}["src"]["sha"]
    require(old_src == {r["path"]: r for r in trees[DONOR_INPUT_ROOT]["tree"]}["src"]["sha"]
            == "43d78cd610b41303691a87f58f06d5e8dafa1c9d", "original body epoch src differs from C0")
    require({r["path"]: r for r in trees[NEW_ROOT]["tree"]}["src"]["sha"]
            == "56a7cf0f39e57e668e1407e055449235f4d9774c", "unexpected current src")
    reuse_endpoints = []
    for suffix in ["src/java.base/share/classes/jdk/internal/mindex", "src/java.base/share/native/libjava"]:
        old_tree, old_chain = walk(trees[OLD_ROOT], suffix.split("/"), trees)
        new_tree, new_chain = walk(trees[NEW_ROOT], suffix.split("/"), trees)
        require(old_tree["sha"] == new_tree["sha"], "reuse endpoint changed: " + suffix)
        reuse_endpoints.append({"path": suffix, "old_tree": old_tree["sha"], "current_tree": new_tree["sha"],
                               "identical": True, "old_chain": old_chain, "current_chain": new_chain})
    mindex = trees[reuse_endpoints[0]["current_tree"]]
    libjava = trees[reuse_endpoints[1]["current_tree"]]
    old_acq_path = AUDIT / "current-map-validation-v03/ACQUISITION.json"
    old_acq = json.loads(old_acq_path.read_bytes())
    old_rows = [row for row in old_acq["readbacks"] if row["path"].startswith("src/")]
    require(len(old_rows) == 23, "expected 23 reused src validator bodies")
    reused = []
    for row in old_rows:
        p = row["path"]
        capture = AUDIT / "current-map-validation-v03" / row["capture"]
        require(sha(capture.read_bytes()) == row["capture_sha256"], "historical capture drift")
        body, blob = file_capture(capture, p, DONOR_INPUT_COMMIT)
        require(len(body) == row["bytes"] and sha(body) == row["sha256"] and blob == row["git_blob_sha1"],
                "historical body receipt drift")
        endpoint = mindex if p.startswith(reuse_endpoints[0]["path"] + "/") else libjava
        require(p == (reuse_endpoints[0]["path"] if endpoint is mindex else reuse_endpoints[1]["path"]) + "/" + Path(p).name,
                "unexpected historical source body")
        entry = next((e for e in endpoint["tree"] if e["path"] == Path(p).name), None)
        require(entry and entry["type"] == "blob" and entry["mode"] == "100644"
                and entry["sha"] == blob and entry["size"] == len(body), "current reused leaf identity mismatch")
        require((AUDIT / "current-map-validation-v03/fixture" / p).read_bytes() == body, "historical fixture differs")
        reused.append({"path": p, "bytes": len(body), "sha256": sha(body), "git_blob_sha1": blob,
            "mode": entry["mode"], "capture": relative(capture), "capture_sha256": sha(capture.read_bytes()),
            "origin_commit": DONOR_INPUT_COMMIT, "intermediate_commit": OLD_COMMIT, "current_commit": NEW_COMMIT,
            "unchanged_subtree": reuse_endpoints[0]["path"] if endpoint is mindex else reuse_endpoints[1]["path"]})
    require(sum(row["path"].endswith(".java") for row in reused) == 22
            and sum(row["path"].endswith("M3BitLane28.c") for row in reused) == 1, "reuse scope changed")
    m3_path = ROOT / "publication/M3_LATEST_M3.json"
    m3_envelope = envelope(m3_path)
    m3 = json.loads(m3_envelope["response"]["structuredContent"]["content"])
    require(m3["truncated"] is False, "truncated current m3 tree")
    require(m3["sha"] == {r["path"]: r for r in trees[NEW_ROOT]["tree"]}["m3"]["sha"], "m3 root disconnected")
    require(m3_envelope["request"] == {"url": API + "/git/trees/" + m3["sha"] + "?recursive=1"}, "wrong existing m3 request")
    require(len(m3["tree"]) == len({r["path"] for r in m3["tree"]}), "duplicate m3 path")
    children, expected = {}, {"": m3["sha"]}
    for row in m3["tree"]:
        parent, _, name = row["path"].rpartition("/")
        children.setdefault(parent, []).append({**row, "path": name})
        if row["type"] == "tree":
            expected[row["path"]] = row["sha"]
    for directory, oid in expected.items():
        require(direct_tree(children.get(directory, [])) == oid, "m3 direct tree reconstruction failed: " + directory)
    matcher_tree, matcher_chain = walk(trees[NEW_ROOT], "src/java.base/share/classes/java/util/regex".split("/"), trees)
    fresh = []
    requests = [
        ("new-matcher.json", "src/java.base/share/classes/java/util/regex/Matcher.java",
         next(row for row in matcher_tree["tree"] if row["path"] == "Matcher.java")),
        ("new-mr-manifest.json", "m3/tooling/mr/src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned/jdk22-m3-mr/manifest.tsv",
         next(row for row in m3["tree"] if row["path"] == "tooling/mr/src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned/jdk22-m3-mr/manifest.tsv")),
    ]
    for filename, p, entry in requests:
        capture = HERE / "captures" / filename
        body, blob = file_capture(capture, p, NEW_COMMIT)
        require(entry["type"] == "blob" and entry["mode"] == "100644" and entry["sha"] == blob
                and entry["size"] == len(body), "fresh body differs from connected immutable tree")
        target = HERE / "bodies" / p
        target.parent.mkdir(parents=True, exist_ok=True)
        if target.exists():
            require(target.read_bytes() == body, "existing decoded body drift")
        else:
            with target.open("xb") as stream:
                stream.write(body)
        fresh.append({"path": p, "bytes": len(body), "sha256": sha(body), "git_blob_sha1": blob,
            "mode": entry["mode"], "capture": relative(capture), "capture_sha256": sha(capture.read_bytes()),
            "body": relative(target), "commit": NEW_COMMIT, "membership_verified": True})
    docs = []
    m3_entries = {row["path"]: row for row in m3["tree"]}
    for local, p, entry in [
        ("M3_LATEST_AGENTS.md", "AGENTS.md", next(r for r in trees[NEW_ROOT]["tree"] if r["path"] == "AGENTS.md")),
        ("M3_LATEST_PORTING_INVARIANT.md", "m3/docs/m3jdk21-porting-invariant.md", m3_entries["docs/m3jdk21-porting-invariant.md"]),
    ]:
        target = ROOT / "publication" / local
        body = target.read_bytes()
        require(git("blob", body) == entry["sha"] and len(body) == entry["size"], "applicable instruction body drift")
        docs.append({"path": p, "local": relative(target), "bytes": len(body), "sha256": sha(body),
                     "git_blob_sha1": entry["sha"], "read": True})
    result = {"schema": "m3-current-map-bounded-acquisition/4", "status": "PASS",
        "repository": "hsoliwal/M3jdk21", "commit": NEW_COMMIT, "root_tree": NEW_ROOT,
        "previous_commit": OLD_COMMIT, "previous_root_tree": OLD_ROOT, "roots": root_rows,
        "direct_tree_captures": tree_rows, "new_nonrecursive_tree_reads": len(tree_rows),
        "new_body_reads": len(fresh), "fresh_artifacts": fresh, "reused_src_artifacts": reused,
        "reuse_endpoints": reuse_endpoints, "matcher_tree_chain": matcher_chain,
        "current_m3_evidence": {"capture": relative(m3_path), "capture_sha256": sha(m3_path.read_bytes()),
            "tree": m3["sha"], "reconstructed_direct_trees": len(expected)},
        "prior_acquisition": {"path": relative(old_acq_path), "sha256": sha(old_acq_path.read_bytes())},
        "applicable_instructions": docs, "verification_script_sha256": sha(Path(__file__).read_bytes()),
        "source_publication_performed": False, "tests_executed": False,
        "scope": "Two exact latest validator bodies only; immutable endpoint proof reuses 22 mindex Java bodies and one native body from original d1 captures through C0 to090. No source publication, source edits, runtime acceptance, or whole-repository acquisition."}
    target = HERE / "PROOF_MANIFEST.json"
    with target.open("x") as stream:
        json.dump(result, stream, indent=2, sort_keys=True)
        stream.write("\n")
    print(json.dumps({"status": "PASS", "proof_manifest": str(target), "sha256": sha(target.read_bytes()),
        "direct_tree_captures": len(tree_rows), "m3_direct_trees_reconstructed": len(expected),
        "fresh_bodies": fresh, "reused_src_bodies": len(reused), "reuse_endpoints": [{k:r[k] for k in ("path","current_tree")} for r in reuse_endpoints]}))

if __name__ == "__main__":
    main()
