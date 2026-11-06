# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Real-Git fixtures for reseal.py: after a tree-mode rename, sha256 and blob pins and path pins in manifests follow
the renamed files, a receipt pinning a manifest is resealed after the manifest (dependency order, any depth), a pin
cycle terminates and is reported, an old digest that still names bytes in the tree is reported as ambiguous instead
of guessed, and the resealed commit reaches a fixed point."""
from __future__ import annotations

import hashlib
import importlib.util
import subprocess
import tempfile
import unittest
from pathlib import Path

SPEC = importlib.util.spec_from_file_location("reseal", Path(__file__).with_name("reseal.py"))
rs = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(rs)
jn = rs.jn

JAVA = "package jdk.internal.m3.x;\nclass M3Foo {}\n"
JAVA_RENAMED = "package jdk.internal.m3.x;\nclass M3Foo {}\n"


def sha256(text: str) -> str:
    return hashlib.sha256(text.encode()).hexdigest()


def blob(text: str) -> str:
    data = text.encode()
    return hashlib.sha1(b"blob %d\0" % len(data) + data).hexdigest()


class ResealTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        subprocess.run(["git", "init", "-q", str(self.root)], check=True)
        for key, value in (("core.autocrlf", "false"), ("user.name", "t"), ("user.email", "t@example.com")):
            subprocess.run(["git", "-C", str(self.root), "config", key, value], check=True)

    def tearDown(self):
        self.tmp.cleanup()

    def put(self, rel, text):
        path = self.root / rel
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding="utf-8", newline="\n")
        subprocess.run(["git", "-C", str(self.root), "add", rel], check=True)

    def commit(self):
        subprocess.run(["git", "-C", str(self.root), "commit", "-q", "-m", "fixture"], check=True)
        return jn.git(self.root, "rev-parse", "HEAD")

    def show(self, rev, path):
        return subprocess.run(["git", "-C", str(self.root), "show", f"{rev}:{path}"], check=True,
                              capture_output=True).stdout.decode()

    def renamed(self):
        base = self.commit()
        return base, jn.apply_tree(self.root, base, "refs/heads/renamed", "rename")

    def test_pins_follow_the_renamed_file_through_a_chain_in_dependency_order(self):
        self.put("m3/x/src/jdk/internal/m3/x/M3Foo.java", JAVA)
        manifest = "path\tbefore\tafter\nm3/x/src/jdk/internal/m3/x/M3Foo.java\tABSENT\t%s\n" % sha256(JAVA)
        self.put("m3/x/manifest.tsv", manifest)
        receipt = '{"manifest_sha256": "%s", "path": "./m3/x/manifest.tsv"}\n' % sha256(manifest)
        self.put("m3/x/receipt.json", receipt)
        self.put("m3/x/seal.txt", "receipt blob %s\n" % blob(receipt).upper())
        base, renamed = self.renamed()
        result = rs.reseal(self.root, base, renamed)
        files = result["files"]
        self.assertEqual(result["unmapped"], [])
        self.assertEqual(result["cycles"], [])
        new_manifest = files["m3/x/manifest.tsv"].decode()
        self.assertIn("m3/x/src/jdk/internal/m3/x/M3Foo.java\tABSENT\t" + sha256(JAVA_RENAMED), new_manifest)
        new_receipt = files["m3/x/receipt.json"].decode()
        self.assertIn(sha256(new_manifest), new_receipt)
        self.assertIn('"./m3/x/manifest.tsv"', new_receipt)
        self.assertEqual(files["m3/x/seal.txt"].decode(), "receipt blob %s\n" % blob(new_receipt).upper())
        self.assertGreaterEqual(result["layers"], 3)
        commit = rs.commit(self.root, renamed, "refs/heads/resealed", "reseal", dict(files, **{rs.LEDGER: rs.ledger(result)}))
        self.assertEqual(rs.reseal(self.root, renamed, commit)["files"], {})
        self.assertIn("SHA256\t%s\t%s" % (sha256(JAVA), sha256(JAVA_RENAMED)), self.show(commit, rs.LEDGER))
        self.assertIn("PATH\tm3/x/src/jdk/internal/m3/x/M3Foo.java\tm3/x/src/jdk/internal/m3/x/M3Foo.java",
                      self.show(commit, rs.LEDGER))

    def test_a_long_hash_chain_is_resealed_in_one_pass(self):
        self.put("m3/x/src/jdk/internal/m3/x/M3Foo.java", JAVA)
        previous = sha256(JAVA)
        for entry in range(1, 13):  # deeper than the 8 rounds of the first version
            text = "history %02d previous %s\n" % (entry, previous)
            self.put("m3/x/history/history-%02d.txt" % entry, text)
            previous = sha256(text)
        base, renamed = self.renamed()
        result = rs.reseal(self.root, base, renamed)
        self.assertEqual(len(result["files"]), 12)
        previous = sha256(JAVA_RENAMED)
        for entry in range(1, 13):
            text = result["files"]["m3/x/history/history-%02d.txt" % entry].decode()
            self.assertEqual(text, "history %02d previous %s\n" % (entry, previous))
            previous = sha256(text)
        self.assertEqual(result["cycles"], [])

    def test_a_corrupted_record_claiming_a_cycle_terminates_and_is_reported(self):
        x_text, y_text = "x pins %s\n" % ("a" * 64), "y pins %s\n" % ("b" * 64)
        x = subprocess.run(["git", "-C", str(self.root), "hash-object", "-w", "--stdin"], input=x_text.encode(),
                           capture_output=True, check=True).stdout.decode().strip()
        y = subprocess.run(["git", "-C", str(self.root), "hash-object", "-w", "--stdin"], input=y_text.encode(),
                           capture_output=True, check=True).stdout.decode().strip()
        refs = {x: {"a" * 64: y}, y: {"b" * 64: x}}
        sha = {x: sha256(x_text), y: sha256(y_text)}
        nodes, dependents = rs.must_change(refs, {x})
        done = rs.resolve(self.root, refs, nodes, dependents, sha, rs.Pins())
        self.assertEqual(done["cycles"], {x, y})
        self.assertEqual(set(done["new_bytes"]), {x, y})

    def test_an_old_digest_that_still_names_bytes_in_the_tree_is_ambiguous_not_guessed(self):
        kept = "// Synexia-NOTICE applies\nclass M3A {}\n"
        self.put("m3/a/M3A.java", kept)
        self.put("m3/a/copy/M3A.java.md", kept)  # lineage copy: not rewritten, keeps the old bytes
        self.put("m3/pins.txt", sha256(kept) + "\n")
        base, renamed = self.renamed()
        result = rs.reseal(self.root, base, renamed)
        self.assertNotIn("m3/pins.txt", result["files"])
        self.assertIn(sha256(kept), result["pins"].ambiguous)
        self.assertIn("AMBIGUOUS\t" + sha256(kept), rs.ledger(result).decode())
        pins = rs.Pins()
        pins.add_bytes(b"same\n", b"one\n")
        pins.add_bytes(b"same\n", b"two\n")
        self.assertIn(sha256("same\n"), pins.ambiguous)

    def test_an_ambiguity_that_the_reseal_resolves_is_carried_in_a_later_pass(self):
        """A postimage (.txt, renamed) and its lineage copy (.tsv, resealed) hold the same bytes; after the reseal
        carries the moved path in the copy, both hold the renamed bytes, so a pin of the old bytes is no longer
        ambiguous and must follow (M3jdk21: after-028.txt and the manifest.tsv copies)."""
        self.put("m3/x/src/jdk/internal/m3/x/M3Foo.java", JAVA)
        listing = "m3/x/src/jdk/internal/m3/x/M3Foo.java\n"
        self.put("m3/x/after-028.txt", listing)
        self.put("m3/x/copy/manifest.tsv", listing)
        self.put("m3/x/pins.json", '{"after_sha256": "%s"}\n' % sha256(listing))
        base, renamed = self.renamed()
        first = rs.reseal(self.root, base, renamed)
        self.assertNotIn("m3/x/pins.json", first["files"])  # ambiguous in the renamed tree
        commit = rs.reseal_to_fixed_point(self.root, base, renamed, "refs/heads/resealed", "reseal")
        renamed_listing = "m3/x/src/jdk/internal/m3/x/M3Foo.java\n"
        self.assertEqual(self.show(commit, "m3/x/after-028.txt"), renamed_listing)
        self.assertEqual(self.show(commit, "m3/x/copy/manifest.tsv"), renamed_listing)
        self.assertEqual(self.show(commit, "m3/x/pins.json"), '{"after_sha256": "%s"}\n' % sha256(renamed_listing))
        self.assertEqual(jn.git(self.root, "rev-parse", commit + "^"), jn.git(self.root, "rev-parse", renamed))
        self.assertEqual(rs.reseal(self.root, base, commit)["files"], {})
        self.assertIn(sha256(listing), self.show(commit, rs.LEDGER))

    def test_a_path_token_with_a_prefix_is_carried_and_an_unrelated_one_is_not(self):
        pins = rs.Pins()
        pins.add("PATH", "m3/x/M3Foo.java", "m3/x/M3Foo.java")
        pins.clean()
        self.assertEqual(rs.carried("see repo/m3/x/M3Foo.java and m3/x/M3Foo.java.bak", pins),
                         "see repo/m3/x/M3Foo.java and m3/x/M3Foo.java.bak")


if __name__ == "__main__":
    unittest.main()
