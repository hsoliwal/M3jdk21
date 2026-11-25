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
PLAIN_TYPES_COMMIT = "06120d99868525ff7dac1e3785742f32f598150e"
PROVIDER_TRANSFER_COMMIT = "6477fd0fe952012d1456f7c94cc980dffb38fb25"
# Original M3 receiver source receipts predate these later, reviewed types.
OTHER_TYPE_RENAMES = (
    ("m3/collections/src/main/java/jdk/m3/collections/apache/M3ArrayStack.java",
     "m3/collections/src/main/java/jdk/m3/collections/apache/ArrayStack.java",
     "e8e3de6031e99bfa20583bdbccf29aa98b18523ecc3cdc08d3b263125c77f3d9",
     "54a360750253be7fb0cb9761f238f2a7d340fad2816fcbf05d8e4788cfa7ed71"),
    ("m3/collections/src/main/java/jdk/m3/collections/apache/M3NodeCachingLinkedList.java",
     "m3/collections/src/main/java/jdk/m3/collections/apache/NodeCachingLinkedList.java",
     "65e2963f8d44e109a7a1c687714e0326084f8cfb870ed4a2cc880d58ccd22498",
     "9e3d1ebd5c5be2eb46e097488fcd6127d5f785d23a185b246107cd8c3e39328b"),
    ("m3/collections/src/main/java/jdk/m3/collections/apache/M3TreeList.java",
     "m3/collections/src/main/java/jdk/m3/collections/apache/TreeList.java",
     "ad155c871d40d03dca1412443814e8670e8819c1ccd690f60f09f18e2ce9ca86",
     "a41baffa6bafd82eaeedee95bda5f851d714eacebed0a0f4e919ae1ad6006c85"),
    ("m3/collections/src/main/java/jdk/m3/collections/tooling/M3CountedCatalogue.java",
     "m3/collections/src/main/java/jdk/m3/collections/tooling/CountedCatalogue.java",
     "0128438ee625c58f355be774fc1f048ac1e9c9b5875d2e775067bbb176bd3dbc",
     "a17d4395486f2f0ed114c3c2f64d081144c233bf1d35adca85226d31a4ad9b4e"),
)
PROVIDER_TRANSFER = (
    "m3/collections/src/main/java/jdk/internal/m3/collections/StrategyHashMap.java",
    "src/jdk.m3.collections/providers/classes/jdk/internal/m3/collections/StrategyHashMap.java",
    "226909421841ccde6a6b89bfd5405d6c1bb65ad57be333b724f6ec49b76a85bc", 
    "2c7a5c1feee057bb05360bab68874e001246e0a41b806cd9932ec7f5f919f6dd",
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
    git("merge-base", "--is-ancestor", PLAIN_TYPES_COMMIT, current)
    git("merge-base", "--is-ancestor", PROVIDER_TRANSFER_COMMIT, current)
    return current


def status_paths(base=HIST_BASE, head=HIST_HEAD, roots=SOURCE_ROOTS):
    rows = git("diff", "--no-ext-diff", "--name-status", "-z",
               "--no-renames", "--diff-filter=AD", base, head,
               "--", *roots).split("\0")
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


def preview_source_receipt(predecessor, pairs):
    """Simulate source-key moves in memory only, without product receipt writes."""
    original = list(predecessor)
    current = dict(original)
    refuse_if(len(current) != len(original), "duplicate predecessor source path")
    extra = []
    targets = set()
    for pair in pairs:
        old, new = pair["old"], pair["new"]
        before, after = pair["historical_sha256"], pair["current_sha256"]
        refuse_if(current.get(old) != before,
                  "historical source predecessor does not match exact old SHA-256")
        refuse_if(new in current or new in targets,
                  "candidate new Java source already admitted")
        del current[old]
        targets.add(new)
        extra.append((new, after))
    return ([(name, current[name]) for name, _ in original if name in current]
            + extra)


def verify_other_history(expected, missing, unlisted, unlisted_sha256):
    """Require four plain-type source migrations and one exact provider transfer."""
    plain_added, plain_removed = status_paths(
        PLAIN_TYPES_COMMIT + "^", PLAIN_TYPES_COMMIT)
    provider_added, provider_removed = status_paths(
        PROVIDER_TRANSFER_COMMIT + "^", PROVIDER_TRANSFER_COMMIT,
        SOURCE_ROOTS + (
            "src/jdk.m3.collections/providers/classes/jdk/internal/m3/collections",))
    result = []
    seen_old, seen_new = set(), set()
    for old, new, historical_sha, source_sha in OTHER_TYPE_RENAMES:
        refuse_if(old in seen_old or new in seen_new,
                  "duplicate original plain-type migration")
        seen_old.add(old)
        seen_new.add(new)
        refuse_if(old not in missing or new not in unlisted or
                  expected.get(old) != historical_sha or
                  unlisted_sha256.get(new) != source_sha,
                  "historical plain-type source identity drift")
        old_physical = verifier.located(old).relative_to(verifier.REPO).as_posix()
        new_physical = verifier.located(new).relative_to(verifier.REPO).as_posix()
        refuse_if(old_physical not in plain_removed or
                  new_physical not in plain_added,
                  "plain-type rename not found in exact Git source commit")
        kind, current_sha = audit.guarded_source(new)
        refuse_if(kind != "FILE" or current_sha != source_sha,
                  "plain-type current file has unexpected SHA-256")
        result.append({
            "old": old, "new": new,
            "historical_sha256": historical_sha, "current_sha256": source_sha,
            "history": "GIT_RENAME", "commit": PLAIN_TYPES_COMMIT,
        })
    old, provider, historical_sha, source_sha = PROVIDER_TRANSFER
    refuse_if(old not in missing or expected.get(old) != historical_sha or
              old in seen_old or provider in unlisted,
              "StrategyHashMap must remain only in provider source root")
    old_physical = verifier.located(old).relative_to(verifier.REPO).as_posix()
    refuse_if(old_physical not in provider_removed or
              provider not in provider_added,
              "StrategyHashMap provider migration has no exact Git source proof")
    physical = verifier.REPO / provider
    directory = verifier.REPO
    for component in Path(provider).parts:
        directory = directory / component
        refuse_if(directory.is_symlink() or
                  getattr(directory, "is_junction", lambda: False)(),
                  "aliased StrategyHashMap provider source")
    refuse_if(not physical.is_file() or
              hashlib.sha256(physical.read_bytes()).hexdigest() != source_sha,
              "live provider StrategyHashMap source digest drift")
    result.append({
        "old": old, "new": provider,
        "historical_sha256": historical_sha, "current_sha256": source_sha,
        "history": "GIT_PROVIDER_TRANSFER",
        "commit": PROVIDER_TRANSFER_COMMIT,
    })
    refuse_if(len(result) != 5 or
              {x["old"] for x in result} != missing or
              len(unlisted - seen_new) != 251,
              "five extra source migrations do not close historic missing set")
    return result


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
    other_lineages = verify_other_history(
        expected, missing - assigned_old, unlisted - assigned_new, unlisted_sha256)
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
    preview = preview_source_receipt(rows, pairs)
    preview_java = {path for path, _ in preview if path.endswith(".java") and "/src/" in path}
    refuse_if(len(preview) != 1957 or len(preview_java) != 1951 or
              len(preview_java - physical) != 5 or len(physical - preview_java) != 255,
              "source-only MAT preview changes unexpected rows or residual membership")
    preview_text = "".join(path + "\t" + digest + "\n" for path, digest in preview)
    dry_run_text = "".join(
        p["old"] + "\t" + p["historical_sha256"] + "\tREMOVED\n" +
        p["new"] + "\tABSENT\t" + p["current_sha256"] + "\n"
        for p in pairs)
    preview_sha256 = hashlib.sha256(preview_text.encode("utf-8")).hexdigest()
    dry_run_sha256 = hashlib.sha256(dry_run_text.encode("utf-8")).hexdigest()
    extra = [{"old": x["old"], "new": x["new"],
              "historical_sha256": x["historical_sha256"],
              "current_sha256": x["current_sha256"]}
             for x in other_lineages if x["history"] == "GIT_RENAME"]
    extended = preview_source_receipt(preview, extra)
    extended_java = {path for path, _ in extended
                     if path.endswith(".java") and "/src/" in path}
    refuse_if(len(extended) != 1957 or len(extended_java) != 1951 or
              len(extended_java - physical) != 1 or
              len(physical - extended_java) != 251,
              "source-only full lineage preview changed historic/JDK membership")
    extended_text = "".join(path + "\t" + digest + "\n"
                            for path, digest in extended)
    extended_sha = hashlib.sha256(extended_text.encode("utf-8")).hexdigest()
    return {
        "sourceAuditHead": SOURCE_AUDIT_HEAD,
        "historicalMatPrMerge": HIST_MERGE,
        "originalPredecessorSha256": predecessor_sha256,
        "originalPredecessorRows": len(rows),
        "sourceOnlyExtendedPreview": {
            "status": "UNAPPROVED_DRY_RUN",
            "sourceRows": len(extended),
            "javaRows": len(extended_java),
            "sha256": extended_sha,
            "matOldNewPairs": 106,
            "plainNameOldNewPairs": 4,
            "providerTransferOutsideShareInventory": 1,
            "missingHistoricalKeyStillInShareReceipt": 1,
            "otherUnlistedJavaAfterPairs": 251,
            "existingSourceDriftStillOpen": len(changed),
        },
        "sourceOnlyProposedReceipt": {
            "status": "UNAPPROVED_DRY_RUN",
            "rowCount": len(preview),
            "javaCount": len(preview_java),
            "sha256": preview_sha256,
            "uninstalledTransitions": 2 * len(pairs),
            "uninstalledTransitionSha256": dry_run_sha256,
            "remainingHistoricalPaths": len(preview_java - physical),
            "remainingUnlistedJava": len(physical - preview_java),
            "remainingChangedSources": len(changed),
        },
        "pairs": pairs,
        "otherHistoricalLineages": other_lineages,
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
    old_row = [("original", "a" * 64), ("untouched", "b" * 64)]
    one = [{"old": "original", "new": "renamed",
            "historical_sha256": "a" * 64, "current_sha256": "c" * 64}]
    refuse_if(preview_source_receipt(old_row, one) !=
              [("untouched", "b" * 64), ("renamed", "c" * 64)],
              "source-only overlay must preserve row membership and ordering")
    try:
        preview_source_receipt(old_row, one + one)
        raise ValueError("duplicate receiver transition admitted")
    except ValueError as reason:
        refuse_if(str(reason) == "duplicate receiver transition admitted",
                  "duplicate receiving move was not refused")
    try:
        preview_source_receipt(old_row, [{"old": "original", "new": "new",
                                         "historical_sha256": "d" * 64,
                                         "current_sha256": "c" * 64}])
        raise ValueError("false predecessor admitted")
    except ValueError as reason:
        refuse_if(str(reason) == "false predecessor admitted",
                  "changed original source was admitted")
    refuse_if(len(OTHER_TYPE_RENAMES) != 4 or
              len({x[0] for x in OTHER_TYPE_RENAMES}) != 4 or
              len({x[1] for x in OTHER_TYPE_RENAMES}) != 4,
              "four original plain type names changed")
    refuse_if(not PROVIDER_TRANSFER[1].startswith(
                  "src/jdk.m3.collections/providers/classes/"),
              "StrategyHashMap must stay outside share-only module source")
    refuse_if(PROVIDER_TRANSFER[0].endswith("M3StrategyHashMap.java") or
              not PROVIDER_TRANSFER[0].endswith("StrategyHashMap.java"),
              "original StrategyHashMap name was not supposed to change")
    print("M3_PUBLIC_MAT_HISTORY_SELF_TEST_PASS cases=13")


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
              "installedAcceptance=HOLD allHistoricalExplained=111 "
              "plainRenames=4 providerTransfers=1 residualUnlisted=251")
        return 0
    except (OSError, ValueError, UnicodeError) as err:
        print("M3_PUBLIC_MAT_HISTORY_REFUSED: " + str(err), file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
