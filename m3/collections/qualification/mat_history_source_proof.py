#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Independent PUBLIC Git/source evidence for historically merged MAT renames.

No private Synexia source, credentials, Java code, historical receipt or
acceptance policy is copied into this receiver. Source-only review, not a
source transition or a green original installed Java/JNI verification.
"""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import sys

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import installed_source_drift_audit as audit  # noqa: E402
verifier = audit.verifier

HIST_BASE = "961bfaadb6cc3fbcb836a51bbb593ebcf9860cb3"
HIST_HEAD = "00e8af9e4ed5e2d00623c98dcda5c52cb73a9261"
HIST_MERGE = "a7c2c2d8378d4ce55395537abce07be7df483d56"
SOURCE_AUDIT_HEAD = "4d3f7fecd57102880bba0086f58a142fb691c8c2"
SPECIAL = {
    "MatAdaptiveIntIntMap.java": "AdaptiveIntToIntMap.java",
    "MatAdaptiveLongLongMap.java": "AdaptiveLongToLongMap.java",
    "MatIdIdMap.java": "IdToIdMap.java",
}
SOURCE_ROOTS = (
    "src/jdk.m3.collections/share/classes/jdk/m3/collections",
    "src/jdk.m3.collections/share/classes/jdk/internal/m3/collections",
    "test/jdk/jdk/m3/collections",
    "test/jdk/jdk/internal/m3/collections",
)
MEMO_TEST_RENAME = (
    "m3/collections/src/test/java/jdk/internal/m3/collections/MatMemoTableTest.java",
    "m3/collections/src/test/java/jdk/internal/m3/collections/MemoTableTest.java",
)


def refuse_if(condition, message):
    if condition:
        raise ValueError("MAT source history refused: " + message)


def git(*args):
    cp = subprocess.run(["git", "-C", str(verifier.REPO), *args],
                        check=False, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    refuse_if(cp.returncode != 0,
              "Git identity/history request failed: " +
              cp.stderr.decode("utf-8", "replace")[-500:])
    return cp.stdout.decode("utf-8", "strict")


def checked_header():
    current = git("rev-parse", "HEAD").strip()
    refuse_if(len(current) != 40, "not checked out at an exact Git commit")
    # The original source audit was executed at a frozen commit; verify no
    # intervening Java-source changes rather than assuming a living branch
    # still has the same source identities.
    source_delta = git("diff", "--no-ext-diff", "--name-only",
                       SOURCE_AUDIT_HEAD, current, "--", *SOURCE_ROOTS)
    refuse_if(bool(source_delta.strip()),
              "current original installed source roots differ from frozen audit")
    git("merge-base", "--is-ancestor", HIST_MERGE, current)
    return current


def status_paths():
    rows = git("diff", "--no-ext-diff", "--name-status", "-z",
               "--no-renames", "--diff-filter=AD", HIST_BASE, HIST_HEAD,
               "--", *SOURCE_ROOTS).split("\0")
    refuse_if(rows[-1] != "", "incomplete NUL-delimited historical diff")
    added, removed = set(), set()
    for i in range(0, len(rows) - 1, 2):
        refuse_if(i + 1 >= len(rows), "truncated Git name/status diff")
        kind, path = rows[i:i + 2]
        refuse_if(kind not in ("A", "D") or not path or ".." in Path(path).parts,
                  "unexpected Git historical source operation")
        (added if kind == "A" else removed).add(path)
    return added, removed


def infer_candidate(recorded_path):
    # The original PR #3268 also renamed this one test outside the internal
    # .mat package. It is an explicit historical exception, not a wildcard.
    if recorded_path == MEMO_TEST_RENAME[0]:
        return MEMO_TEST_RENAME[1]
    parent, separator, name = recorded_path.rpartition("/")
    if (not separator or "/mat/" not in recorded_path or
            not name.startswith("Mat") or not name.endswith(".java")):
        return None
    other = SPECIAL.get(name, name[3:])
    return parent + "/" + other


def tracked_java_roots():
    source_set = {verifier.recorded(p) for p in (verifier.HERE / "src").rglob("*.java")}
    for root in verifier.MOVED_JAVA_ROOTS:
        source_set.update(verifier.recorded(p) for p in (verifier.REPO / root).rglob("*.java"))
    return source_set


def audit_lineage():
    rows = audit.exact_rows()
    expected = dict(rows)
    physical = tracked_java_roots()
    declared_java = {p for p in expected if p.endswith(".java") and "/src/" in p}
    missing = {p for p in declared_java if p not in physical}
    unlisted = physical - declared_java
    # Capture the full independent preimage receipt so the canonical private
    # successor can later pin the EXACT 1,957-row predecessor, not a guess.
    predecessor = "".join(path + "\t" + digest + "\n" for path, digest in rows)
    predecessor_sha256 = hashlib.sha256(predecessor.encode("utf-8")).hexdigest()
    unlisted_sha256 = {}
    for path in sorted(unlisted):
        category, digest = audit.guarded_source(path)
        refuse_if(category != "FILE" or digest is None or len(digest) != 64,
                  "unlisted Java file is missing/aliased/unreadable: " + path)
        unlisted_sha256[path] = digest
    seen_a, seen_d = status_paths()
    pairs = []
    for old in sorted(missing):
        new = infer_candidate(old)
        if new is None or new not in unlisted:
            continue
        prior = verifier.located(old).relative_to(verifier.REPO).as_posix()
        current = verifier.located(new).relative_to(verifier.REPO).as_posix()
        # Both sides must appear as an old D and new A in the exact original
        # merged PR's pinned Git history, irrespective of rename thresholds.
        refuse_if(prior not in seen_d or current not in seen_a,
                  "claimed MAT rename absent from pinned original Git history: " + old)
        label, old_sha = audit.guarded_source(old)
        current_label, new_sha = audit.guarded_source(new)
        refuse_if(label != "MISSING" or current_label != "FILE"
                  or old_sha is not None or new_sha is None
                  or new_sha != unlisted_sha256[new],
                  "current missing/live MAT source mismatch")
        pairs.append({
            "old": old, "new": new, "historical_sha256": expected[old],
            "current_sha256": new_sha,
            "history": "EXACT_OLD_DELETE_NEW_ADD",
            "spelling": "SPECIAL" if old.rsplit("/", 1)[1] in SPECIAL else "MAT_PREFIX",
        })
    unique_new = len({p["new"] for p in pairs})
    special_count = sum(p["spelling"] == "SPECIAL" for p in pairs)
    if len(pairs) != 106 or unique_new != 106 or special_count != 3:
        assigned = {p["old"] for p in pairs}
        unresolved = sorted(p for p in missing if p not in assigned)
        print("M3_PUBLIC_MAT_HISTORY_DIAGNOSTIC " +
              json.dumps({"pairs": len(pairs), "uniqueCurrent": unique_new,
                          "special": special_count, "missingHistorical": len(missing),
                          "unlistedCurrent": len(unlisted),
                          "unresolved": unresolved}, sort_keys=True),
              file=sys.stderr)
        raise ValueError("original MAT Git lineage count or mapping altered")
    assigned_old = {p["old"] for p in pairs}
    assigned_new = {p["new"] for p in pairs}
    refuse_if(len(missing - assigned_old) != 5 or
              len(unlisted - assigned_new) != 255,
              "unattributed source inventory changed")
    changed = []
    observed_missing = set()
    for old, expected_sha in rows:
        category, actual_sha = audit.guarded_source(old)
        refuse_if(category == "ALIEN_ALIAS", "source alias refused: " + old)
        if category == "MISSING":
            observed_missing.add(old)
        elif actual_sha != expected_sha:
            changed.append({"path": old, "expected": expected_sha,
                            "actual": actual_sha})
    refuse_if(len(changed) != 120 or observed_missing != missing,
              "current source SHA-256/absence census changed")
    return {
        "sourceAuditHead": SOURCE_AUDIT_HEAD,
        "historicalMatPrMerge": HIST_MERGE,
        "originalPredecessorSha256": predecessor_sha256,
        "originalPredecessorRows": len(rows),
        "pairs": pairs,
        "unresolvedMissing": [
            {"path": path, "sha256": expected[path]}
            for path in sorted(missing - assigned_old)
        ],
        "otherUnlisted": [
            {"path": path, "sha256": unlisted_sha256[path]}
            for path in sorted(unlisted - assigned_new)
        ],
        "existingSourceDrift": sorted(changed, key=lambda x: x["path"]),
        "sourceOnly": True,
        "installedAcceptance": "HOLD",
    }


def self_test():
    old = "m3/collections/src/main/java/jdk/internal/m3/collections/mat/MatFoo.java"
    refuse_if(infer_candidate(old) != old[:-11] + "Foo.java",
              "simple MAT filename inference")
    refuse_if(infer_candidate(old.replace("/mat/", "/other/")) is not None,
              "outside-MAT rename was admitted")
    other = old.replace("MatFoo.java", "MatAdaptiveIntIntMap.java")
    refuse_if(not infer_candidate(other).endswith("/AdaptiveIntToIntMap.java"),
              "three specialized names require explicit mapping")
    refuse_if(infer_candidate(old.replace("MatFoo.java", "Foo.java")) is not None,
              "unprefixed source was admitted as a rename")
    refuse_if(len(SPECIAL) != 3 or len(SOURCE_ROOTS) != 4,
              "unreviewed history scope change")
    refuse_if(infer_candidate(MEMO_TEST_RENAME[0]) != MEMO_TEST_RENAME[1],
              "one original out-of-package MemoTable test rename is required")
    refuse_if(infer_candidate(
        MEMO_TEST_RENAME[0].replace("MatMemoTableTest.java", "MatUnreviewedTest.java"))
        is not None, "non-historical test rename admitted")
    print("M3_PUBLIC_MAT_HISTORY_SELF_TEST_PASS cases=7")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    try:
        if args.self_test:
            self_test()
            return 0
        head = checked_header()
        report = audit_lineage()
        print(json.dumps({"head": head, **report}, sort_keys=True, indent=2))
        print("M3_PUBLIC_MAT_HISTORY_PASS pairs=106 special=3 "
              "unresolvedOld=5 otherUnlisted=255 unchangedOriginalVerifier=YES "
              "installedAcceptance=HOLD")
        return 0
    except (OSError, ValueError, UnicodeError) as err:
        print("M3_PUBLIC_MAT_HISTORY_REFUSED: " + str(err), file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
