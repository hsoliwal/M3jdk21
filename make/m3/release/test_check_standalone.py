# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Real-Git fixtures for the standalone check (check_standalone.py): planted cross-repository dependencies are caught,
an in-repository com.synexia module is a name and not a dependency, documentation is not counted, the baseline is a
ratchet, and release mode refuses any dependency and any unscanned scoped file."""
from __future__ import annotations

import importlib.util
import json
import subprocess
import tempfile
import unittest
from pathlib import Path

SPEC = importlib.util.spec_from_file_location("check_standalone", Path(__file__).with_name("check_standalone.py"))
cs = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(cs)

WORKFLOW = """name: planted
jobs:
  build:
    steps:
      - uses: actions/checkout@v4
        with:
          repository: hsoliwal/com.synexia
          token: ${{ secrets.SYNEXIA_READ_TOKEN }}
"""
POM = """<project xmlns="http://maven.apache.org/POM/4.0.0">
  <groupId>{group}</groupId><artifactId>{artifact}</artifactId>
  <dependencies>{deps}</dependencies>
</project>
"""
DEP = "<dependency><groupId>{group}</groupId><artifactId>{artifact}</artifactId></dependency>"


def receipt_contract():
    return {
        "schema": "M3_RELEASE_CONTRACT_V1",
        "execution_evidence": {
            "schema": "M3_RELEASE_EVIDENCE_V1",
            "gate_owners": {"M3_MAVEN_CONTROL_REACTOR": "hsoliwal/com.synexia"},
        },
    }


class StandaloneCheckTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        subprocess.run(["git", "init", "-q", str(self.root)], check=True)

    def tearDown(self):
        self.tmp.cleanup()

    def put(self, rel, text):
        path = self.root / rel
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding="utf-8", newline="\n")
        subprocess.run(["git", "-C", str(self.root), "add", rel], check=True)

    def counts(self):
        return cs.scan(self.root)["counts"]

    def test_clean_tree_passes_both_modes(self):
        self.put(".github/workflows/ok.yml", "name: ok\njobs: {}\n")
        self.put("m3/x/pom.xml", POM.format(group="com.m3", artifact="x", deps=""))
        result = cs.scan(self.root)
        cs.write_baseline(self.root, result)
        self.assertEqual(sum(result["counts"].values()), 0)
        self.assertEqual(cs.verdict(result, cs.read_baseline(self.root), False)[0], "PASS")
        self.assertEqual(cs.verdict(result, cs.read_baseline(self.root), True)[0], "PASS")

    def test_planted_checkout_and_secret_are_dependencies(self):
        self.put(".github/workflows/planted.yml", WORKFLOW)
        counts = self.counts()
        self.assertEqual(counts["D_CHECKOUT"], 1)
        self.assertEqual(counts["D_SECRET"], 1)

    def test_maven_resolution_separates_dependency_from_name(self):
        self.put("m3/a/pom.xml", POM.format(group="com.synexia.m3", artifact="a", deps=""))
        self.put("m3/b/pom.xml", POM.format(group="com.m3", artifact="b",
                 deps=DEP.format(group="com.synexia.m3", artifact="a") + DEP.format(group="com.synexia.m3", artifact="ext")))
        found = {(kind, text) for kind, _path, _line, text in cs.scan(self.root)["findings"]}
        self.assertIn(("D_MAVEN", "dependency com.synexia.m3:ext"), found)
        self.assertIn(("N_NAME", "dependency com.synexia.m3:a"), found)
        self.assertIn(("N_NAME", "module groupId com.synexia.m3:a"), found)
        self.assertNotIn(("D_MAVEN", "dependency com.synexia.m3:a"), found)

    def test_documentation_and_lineage_are_not_counted(self):
        self.put("README.md", "Lineage: https://github.com/hsoliwal/com.synexia\n")
        self.put("m3/x/provenance.json", '{"source": "hsoliwal/com.synexia"}\n')
        self.put("doc/m3/notes.yml", "repository: hsoliwal/com.synexia\n")
        self.assertEqual(sum(self.counts().values()), 0)

    def test_canonical_maven_receipt_owner_is_passive_metadata(self):
        self.put("make/m3/release/release-contract.json", json.dumps(receipt_contract(), indent=2))
        result = cs.scan(self.root)
        cs.write_baseline(self.root, result)
        self.assertEqual(sum(result["counts"].values()), 0)
        self.assertEqual(cs.verdict(result, cs.read_baseline(self.root), True)[0], "PASS")

    def test_compact_contract_keeps_other_references_on_same_line(self):
        contract = receipt_contract()
        contract["build_command"] = ["git", "clone", "https://github.com/hsoliwal/com.synexia"]
        text = json.dumps(contract)
        findings = cs.scan_text("make/m3/release/release-contract.json", text)
        self.assertEqual(1, len(findings))
        self.assertEqual(("D_REPO_REF", "make/m3/release/release-contract.json", 1), findings[0][:3])

    def test_pretty_contract_keeps_dependency_line_numbers(self):
        contract = receipt_contract()
        contract["build_command"] = "git clone https://github.com/hsoliwal/com.synexia"
        text = json.dumps(contract, indent=2)
        number = next(i for i, line in enumerate(text.splitlines(), 1) if "build_command" in line)
        findings = cs.scan_text("make/m3/release/release-contract.json", text)
        self.assertEqual(1, len(findings))
        self.assertEqual(number, findings[0][2])
        self.assertIn("build_command", findings[0][3])

    def test_passive_owner_does_not_hide_real_workflow_checkout(self):
        self.put("make/m3/release/release-contract.json", json.dumps(receipt_contract()))
        self.put(".github/workflows/planted.yml", WORKFLOW)
        result = cs.scan(self.root)
        cs.write_baseline(self.root, result)
        self.assertEqual(1, result["counts"]["D_CHECKOUT"])
        self.assertEqual("BLOCK", cs.verdict(result, cs.read_baseline(self.root), True)[0])

    def test_only_exact_structured_receipt_location_is_exempt(self):
        for document in (
                {"M3_MAVEN_CONTROL_REACTOR": "hsoliwal/com.synexia"},
                {"schema": "M3_RELEASE_CONTRACT_V1", "build": {
                    "M3_MAVEN_CONTROL_REACTOR": "hsoliwal/com.synexia"}},
                {"schema": "wrong", "execution_evidence": receipt_contract()["execution_evidence"]}):
            with self.subTest(document=document):
                self.assertTrue(cs.scan_text("make/m3/release/release-contract.json", json.dumps(document)))

    def test_other_gate_owner_is_still_a_dependency(self):
        contract = receipt_contract()
        contract["execution_evidence"]["gate_owners"]["OPENJDK_SANITY_MATRIX"] = "hsoliwal/com.synexia"
        findings = cs.scan_text("make/m3/release/release-contract.json", json.dumps(contract, indent=2))
        self.assertEqual(1, len(findings))
        self.assertEqual("D_REPO_REF", findings[0][0])

    def test_other_source_or_configuration_file_is_not_exempt(self):
        for path in (".github/config.json", "make/m3/release/run.py", "make/m3/build.sh"):
            with self.subTest(path=path):
                self.assertTrue(cs.scan_text(path, json.dumps(receipt_contract())))

    def test_malformed_duplicate_or_ambiguous_json_gets_no_exemption(self):
        text = json.dumps(receipt_contract())
        for value in (
                text + " trailing",
                text.replace('"gate_owners":', '"gate_owners": {}, "gate_owners":'),
                text[:-1] + ', "number": NaN}',
                text.replace('M3_MAVEN_CONTROL_REACTOR', 'M3_MAVEN_CONTROL_REACTO\\u0052')[:-1]
                + ', "other": {"M3_MAVEN_CONTROL_REACTOR": "hsoliwal/com.synexia"}}',
                text[:-1] + ', "other": {"M3_MAVEN_CONTROL_REACTOR": "hsoliwal/com.synexia"}}'):
            with self.subTest(value=value):
                self.assertEqual(value, cs.dependency_scan_text("make/m3/release/release-contract.json", value))
                self.assertTrue(cs.scan_text("make/m3/release/release-contract.json", value))

    def test_metadata_value_is_not_an_arbitrary_repository_exemption(self):
        contract = receipt_contract()
        contract["execution_evidence"]["gate_owners"]["M3_MAVEN_CONTROL_REACTOR"] = "hsoliwal/com.synexia-extra"
        self.assertTrue(cs.scan_text("make/m3/release/release-contract.json", json.dumps(contract)))

    def test_baseline_is_a_ratchet_and_release_refuses_dependencies(self):
        self.put(".github/workflows/planted.yml", WORKFLOW)
        result = cs.scan(self.root)
        cs.write_baseline(self.root, result)
        self.assertEqual(cs.verdict(result, cs.read_baseline(self.root), False)[0], "PASS")
        self.assertEqual(cs.verdict(result, cs.read_baseline(self.root), True)[0], "BLOCK")
        self.put(".github/workflows/second.yml", WORKFLOW)
        status, detail = cs.verdict(cs.scan(self.root), cs.read_baseline(self.root), False)
        self.assertEqual(status, "BLOCK")
        self.assertIn("grew past the baseline", detail)

    def test_missing_baseline_blocks(self):
        self.put(".github/workflows/ok.yml", "name: ok\n")
        self.assertEqual(cs.verdict(cs.scan(self.root), None, False)[0], "BLOCK")

    def test_tree_mode_scans_files_that_are_not_checked_out(self):
        self.put(".github/workflows/planted.yml", WORKFLOW)
        self.put("m3/x/pom.xml", POM.format(group="com.m3", artifact="x",
                 deps=DEP.format(group="com.synexia.m3", artifact="ext")))
        subprocess.run(["git", "-C", str(self.root), "-c", "user.name=t", "-c", "user.email=t@example.com",
                        "commit", "-q", "-m", "fixture"], check=True)
        (self.root / ".github/workflows/planted.yml").unlink()
        (self.root / "m3/x/pom.xml").unlink()
        worktree = cs.scan(self.root)
        self.assertEqual((worktree["unscanned"], worktree["counts"]["D_CHECKOUT"]), (2, 0))
        tree = cs.scan(self.root, "HEAD")
        self.assertEqual(tree["unscanned"], 0)
        self.assertEqual((tree["counts"]["D_CHECKOUT"], tree["counts"]["D_SECRET"], tree["counts"]["D_MAVEN"]), (1, 1, 1))
        self.assertIsNone(cs.read_baseline(self.root, "HEAD"))
        cs.write_baseline(self.root, tree)  # in the working tree only: the revision's own ratchet is what counts
        self.assertIsNotNone(cs.read_baseline(self.root))
        self.assertIsNone(cs.read_baseline(self.root, "HEAD"))
        self.assertEqual(cs.verdict(tree, cs.read_baseline(self.root, "HEAD"), False)[0], "BLOCK")

    def test_the_naming_recipe_crate_is_not_counted(self):
        self.put("make/m3/tooling/jdk-naming/reseal.py", "PATTERN = 'com.synexia.m3 SynexiaFoo SYNEXIA_X'\n")
        self.put("make/m3/tooling/jdk-naming/conformance/pom.xml", POM.format(group="com.synexia.m3", artifact="m3-x", deps=""))
        self.assertEqual(sum(self.counts().values()), 0)

    # Lineage rules (2026-11-10): L_ARCHIVE and L_PROVENANCE are data; the same strings in live use still count.
    def lineage(self):
        result = cs.scan(self.root)
        return result["counts"], result["lineage"]

    def kinds_by_path(self):
        found = {}
        for kind, path, _line, _text in cs.scan(self.root)["findings"]:
            found.setdefault(path, []).append(kind)
        return found

    def test_retained_variants_archive_is_lineage_and_a_live_copy_still_counts(self):
        archive = "m3/qualification/master-landing-20261028/retained-variants/pr1/.github/workflows/w.yml"
        self.put(archive, WORKFLOW)
        self.put("make/m3/qualification/run/sub/retained-variants/pr2/build.sh", "git clone https://github.com/hsoliwal/com.synexia\n")
        counts, lineage = self.lineage()
        self.assertEqual(0, sum(counts.values()))
        self.assertEqual(3, lineage["L_ARCHIVE"])
        self.put(".github/workflows/w.yml", WORKFLOW)
        for live in ("make/m3/tools/retained-variants/build.sh", "make/m3/qualification/run/retained-variants-old/build.sh",
                     "make/m3/qualification/retained-variants/build.sh", "make/m3/collections/qualification/r/retained-variants/b.sh"):
            self.put(live, "git clone https://github.com/hsoliwal/com.synexia\n")
        counts, lineage = self.lineage()
        self.assertEqual((1, 1, 4), (counts["D_CHECKOUT"], counts["D_SECRET"], counts["D_REPO_REF"]))
        self.assertEqual(3, lineage["L_ARCHIVE"])
        status, detail = cs.verdict(cs.scan(self.root), {k: 9 for k in cs.KINDS}, True)
        self.assertEqual("BLOCK", status)
        self.assertIn("L_ARCHIVE=3", detail)

    def test_an_archived_pom_produces_nothing_for_a_live_pom(self):
        self.put("make/m3/qualification/r/retained-variants/pr1/pom.xml", POM.format(group="com.synexia.m3", artifact="a", deps=""))
        self.put("m3/b/pom.xml", POM.format(group="com.m3", artifact="b", deps=DEP.format(group="com.synexia.m3", artifact="a")))
        counts, lineage = self.lineage()
        self.assertEqual(1, counts["D_MAVEN"])
        self.assertEqual(1, lineage["L_ARCHIVE"])

    def test_provenance_constant_and_receipt_fields_are_lineage(self):
        self.put("m3/x/recipe/relocate.py",
                 '"""Relocated from the donor hsoliwal/com.synexia."""\nimport subprocess\n'
                 'DONOR_REPO = "hsoliwal/com.synexia"  # lineage: hsoliwal/com.synexia\n'
                 'def git(donor, *a):\n    return subprocess.run(["git", "-C", donor, *a], check=True).stdout\n'
                 'def check(record, row):\n'
                 '    if record["repository"] != "hsoliwal/com.synexia" or row.get("source_repo") == "hsoliwal/com.synexia#12":\n'
                 '        raise ValueError(record)\n'
                 '    return {"source_repository": "hsoliwal/com.synexia", "url": f"https://github.com/hsoliwal/com.synexia/tree/{row}"}\n')
        self.put("m3/x/test_receipt.py",
                 'import unittest\nclass T(unittest.TestCase):\n    def test(self):\n'
                 '        self.assertEqual("hsoliwal/com.synexia", {}.get("canonical_repository"))\n'
                 '        self.assertNotEqual({}.get("pr"), "hsoliwal/com.synexia#9")\n')
        counts, lineage = self.lineage()
        self.assertEqual(0, sum(counts.values()))
        self.assertEqual(6, lineage["L_PROVENANCE"])

    def test_the_same_string_in_a_clone_fetch_or_build_reference_still_counts(self):
        fetchers = {
            "m3/a/gh.py": 'import subprocess\nREPO = "hsoliwal/com.synexia"\n'
                          'def gh(p):\n    return subprocess.run(["gh", "api", p]).stdout\ngh(f"repos/{REPO}/contents")\n',
            "m3/a/clone.py": 'import subprocess\nDONOR_REPO = "hsoliwal/com.synexia"\n'
                             'subprocess.run(["git", "clone", "https://github.com/" + DONOR_REPO])\n',
            "m3/a/fetch.py": 'import urllib.request\nSOURCE_REPO = "hsoliwal/com.synexia"\n'
                             'urllib.request.urlopen("https://example.invalid/" + SOURCE_REPO)\n',
            "m3/a/run.py": 'import subprocess\nsubprocess.run(["python", "x.py", "--repo", "hsoliwal/com.synexia"])\n',
            "m3/a/roots.py": 'import sys\nroots = {"hsoliwal/com.synexia": sys.argv[1]}\n',
            "m3/a/load.py": 'from pathlib import Path\nsource = Path("hsoliwal/com.synexia")\n',
            "m3/a/owner.py": 'POLICY = {"recipe_owner": "hsoliwal/com.synexia"}\nM3_RECIPE_OWNER = "hsoliwal/com.synexia"\n',
            "m3/a/blob.py": 'BLOB = "https://github.com/hsoliwal/com.synexia/blob/" + "abc"\n',
            "m3/a/rows.py": 'ROWS = ["a\\thsoliwal/com.synexia#9\\tb", \'{"repository": "hsoliwal/com.synexia"}\']\n',
            "m3/a/call.py": 'from m3.a import export\nexport.run("https://github.com/hsoliwal/com.synexia", "abc")\n',
            "m3/a/broken.py": 'DONOR_REPO = "hsoliwal/com.synexia"\nif (:\n',
            "m3/a/build.sh": 'git clone https://github.com/hsoliwal/com.synexia "$W"\n',
            "m3/a/check.sh": 'test "$(jq .repository r.json)" = hsoliwal/com.synexia\n',
            "m3/a/plan.yaml": 'source:\n  clone: https://github.com/hsoliwal/com.synexia\n',
            "m3/a/fetch.yaml": 'source:\n  repo: hsoliwal/com.synexia\nrun: git fetch origin\n',
            "m3/a/copy/.github/workflows/w.yml": 'with:\n  repo: hsoliwal/com.synexia\n',
            ".github/workflows/fetch.yml": 'run: curl -L https://github.com/hsoliwal/com.synexia/archive/x.zip '
                                          '# if r["repository"]=="hsoliwal/com.synexia"\n',
        }
        for path, text in fetchers.items():
            self.put(path, text)
        found = self.kinds_by_path()
        for path in fetchers:
            with self.subTest(path=path):
                self.assertIn("D_REPO_REF", found.get(path, []))
                self.assertNotIn("L_PROVENANCE", found.get(path, []))
        self.assertEqual(18, self.lineage()[0]["D_REPO_REF"])

    def test_a_planted_checkout_is_never_lineage(self):
        self.put("m3/x/gen.py", 'W = "        with:\\n          repository: hsoliwal/com.synexia\\n"\n')
        self.put("m3/x/pom.xml", POM.format(group="com.m3", artifact="x", deps=DEP.format(group="com.synexia.m3", artifact="e"))
                 .replace("  <dependencies>", "  <description>donor hsoliwal/com.synexia</description>\n  <dependencies>"))
        counts, lineage = self.lineage()
        self.assertEqual((1, 1, 0), (counts["D_CHECKOUT"], counts["D_MAVEN"], counts["D_REPO_REF"]))
        self.assertEqual(1, lineage["L_PROVENANCE"])

    def test_embedded_comparison_and_pom_description_are_lineage_only_when_exact(self):
        self.put(".github/workflows/bound.yml",
                 'run: |\n  python3 - <<EOF\n  if b["repository"]!="hsoliwal/com.synexia": raise SystemExit(1)\n'
                 '  print("built from hsoliwal/com.synexia")\n  EOF\n')
        self.put("m3/y/pom.xml", '<project xmlns="http://maven.apache.org/POM/4.0.0">\n'
                 '  <groupId>com.m3</groupId><artifactId>y</artifactId>\n'
                 '  <description>relocated from\n    the donor hsoliwal/com.synexia develop</description>\n'
                 '  <!-- donor hsoliwal/com.synexia -->\n'
                 '  <url>https://github.com/hsoliwal/com.synexia</url>\n</project>\n')
        findings = [(k, p, n) for k, p, n, _t in cs.scan(self.root)["findings"]]
        self.assertIn(("L_PROVENANCE", ".github/workflows/bound.yml", 3), findings)
        self.assertIn(("D_REPO_REF", ".github/workflows/bound.yml", 4), findings)
        self.assertIn(("L_PROVENANCE", "m3/y/pom.xml", 4), findings)
        self.assertIn(("L_PROVENANCE", "m3/y/pom.xml", 5), findings)
        self.assertIn(("D_REPO_REF", "m3/y/pom.xml", 6), findings)

    def test_yaml_provenance_record_and_recipe_prose_are_lineage(self):
        self.put("make/m3/migration/plan.yaml", "schema: X\nsource:\n  repo: hsoliwal/com.synexia\n  pr: 'hsoliwal/com.synexia#7'\n"
                 "  note: imported from hsoliwal/com.synexia\n  url: https://github.com/hsoliwal/com.synexia/archive/x.zip\n")
        self.put("m3/r/META-INF/rewrite/r.yml",
                 "type: specs.openrewrite.org/v1beta/recipe\nname: jdk.internal.m3.rewrite.R\n"
                 "displayName: Re-seal of hsoliwal/com.synexia R\ndescription: >-\n  Re-seal of the donor recipe\n"
                 "  (hsoliwal/com.synexia 4a2e6452a7a) onto jdk.m3.\nrecipeList:\n  - jdk.internal.m3.rewrite.Fix:\n"
                 "      source: hsoliwal/com.synexia\n")
        findings = [(k, p, n) for k, p, n, _t in cs.scan(self.root)["findings"] if k in ("L_PROVENANCE", "D_REPO_REF")]
        self.assertEqual([("L_PROVENANCE", "make/m3/migration/plan.yaml", 3), ("L_PROVENANCE", "make/m3/migration/plan.yaml", 4),
                          ("D_REPO_REF", "make/m3/migration/plan.yaml", 5), ("D_REPO_REF", "make/m3/migration/plan.yaml", 6)],
                         [f for f in findings if f[1] == "make/m3/migration/plan.yaml"])
        self.assertEqual([("L_PROVENANCE", 3), ("L_PROVENANCE", 6), ("D_REPO_REF", 9)],
                         [(k, n) for k, p, n in findings if p == "m3/r/META-INF/rewrite/r.yml"])

    # Sealed lineage (2026-11-10): a listed file at its pinned blob is data; edited, unlisted or networked it counts.
    SEALED = ('EXPECTED = ("M3JDK21_HANDOFF", "hsoliwal/com.synexia#12951")\n'
              'ROWS = ["a\\thsoliwal/com.synexia\\tb"]\n'
              "assert call['url'] == 'https://api.github.com/repos/hsoliwal/com.synexia/git/trees/' + OID\n")

    def seal(self, *paths, blob=None):
        rows = ["# notes", cs.LINEAGE_FILES_HEADER]
        for rel in paths:
            pinned = blob or subprocess.run(["git", "-C", str(self.root), "hash-object", rel], check=True,
                                            capture_output=True, text=True).stdout.strip()
            rows.append(f"{rel}\t{pinned}\tfixture")
        self.put(cs.LINEAGE_FILES, "\n".join(rows) + "\n")

    def commit(self):
        subprocess.run(["git", "-C", str(self.root), "-c", "user.name=t", "-c", "user.email=t@example.com",
                        "commit", "-q", "-m", "fixture"], check=True)

    def test_a_listed_file_at_its_pinned_blob_is_sealed_lineage_until_edited(self):
        self.put("make/m3/history/verify_handoff.py", self.SEALED)
        self.assertEqual(3, self.lineage()[0]["D_REPO_REF"])
        self.seal("make/m3/history/verify_handoff.py")
        counts, lineage = self.lineage()
        self.assertEqual((0, 3), (counts["D_REPO_REF"], lineage["L_SEALED"]))
        self.commit()
        tree = cs.scan(self.root, "HEAD")
        self.assertEqual((0, 3, []), (tree["counts"]["D_REPO_REF"], tree["lineage"]["L_SEALED"], tree["lineage_not_sealed"]))
        (self.root / "make/m3/history/verify_handoff.py").write_bytes(self.SEALED.replace("\n", "\r\n").encode())
        self.assertEqual(3, cs.scan(self.root)["lineage"]["L_SEALED"])  # a CRLF checkout is the same blob
        self.put("make/m3/history/verify_handoff.py", self.SEALED + "# edited\n")
        result = cs.scan(self.root)
        self.assertEqual((3, 0), (result["counts"]["D_REPO_REF"], result["lineage"]["L_SEALED"]))
        self.assertEqual(["make/m3/history/verify_handoff.py"], result["lineage_not_sealed"])
        self.assertEqual(3, cs.scan(self.root, "HEAD")["lineage"]["L_SEALED"])  # the revision keeps its own pin

    def test_a_listed_file_that_can_reach_the_network_is_never_sealed(self):
        networked = {
            "m3/a/bootstrap.py": "import urllib.request\nREPOS = ('hsoliwal/M3jdk21', 'hsoliwal/com.synexia')\n"
                                 "def blob(repo, oid):\n    assert repo in REPOS\n"
                                 "    return urllib.request.urlopen('https://api.github.com/repos/' + repo + '/git/blobs/' + oid)\n",
            "m3/a/from_import.py": "from urllib import request\nSRC = ('x', 'hsoliwal/com.synexia')\n",
            "m3/a/clone.py": "import subprocess\nsubprocess.run(['git', 'clone', 'https://github.com/' + 'hsoliwal/com.synexia'])\n",
            "m3/a/gh.py": "import subprocess\nsubprocess.run(['gh', 'api', 'repos/hsoliwal/com.synexia/contents'])\n",
            "m3/a/fetch.sh": "curl -L https://github.com/hsoliwal/com.synexia/archive/x.zip -o x.zip\n",
        }
        for path, text in networked.items():
            self.put(path, text)
        self.seal(*networked)
        result = cs.scan(self.root)
        self.assertEqual((len(networked), 0), (result["counts"]["D_REPO_REF"], result["lineage"]["L_SEALED"]))
        self.assertEqual(sorted(networked), result["lineage_not_sealed"])

    def test_a_live_checkout_fetch_or_build_reference_outside_lineage_still_counts(self):
        self.put("make/m3/history/verify_handoff.py", self.SEALED)
        self.put(".github/workflows/w.yml", WORKFLOW)
        self.put("m3/b/pom.xml", POM.format(group="com.m3", artifact="b", deps=DEP.format(group="com.synexia.m3", artifact="a")))
        self.seal("make/m3/history/verify_handoff.py", ".github/workflows/w.yml", "m3/b/pom.xml")
        self.put("m3/a/clone.py", "import subprocess\nsubprocess.run(['git', 'clone', 'https://github.com/hsoliwal/com.synexia'])\n")
        self.put("m3/a/build.sh", "mvn -f ../hsoliwal/com.synexia/pom.xml install\n")
        self.put("m3/a/receipt.py", self.SEALED)  # the same bytes under a path that is not listed
        counts, lineage = self.lineage()
        self.assertEqual((1, 1, 1), (counts["D_CHECKOUT"], counts["D_SECRET"], counts["D_MAVEN"]))
        self.assertEqual((5, 3), (counts["D_REPO_REF"], lineage["L_SEALED"]))
        self.assertEqual("BLOCK", cs.verdict(cs.scan(self.root), {k: 99 for k in cs.KINDS}, True)[0])

    def test_a_malformed_or_out_of_scope_lineage_list_refuses(self):
        self.put("make/m3/history/verify_handoff.py", self.SEALED)
        oid = "0" * 40
        for text in ("path\tblob\n", "nope\n", f"path\tblob\treason\nm3/history/verify_handoff.py\t{oid}\tr",
                     f"path\tblob\treason\nm3/history/verify_handoff.py\tXYZ\tr\n",
                     f"path\tblob\treason\nm3/history/verify_handoff.py\t{oid}\t \n",
                     f"path\tblob\treason\nm3/history/verify_handoff.py\t{oid}\tr\nm3/history/verify_handoff.py\t{oid}\tr\n",
                     f"path\tblob\treason\nm3/docs/notes.md\t{oid}\tr\n",
                     f"path\tblob\treason\nm3/release/check_standalone.py\t{oid}\tr\n",
                     f"path\tblob\treason\nm3/qualification/r/retained-variants/pr1/x.py\t{oid}\tr\n"):
            with self.subTest(text=text):
                self.put(cs.LINEAGE_FILES, text)
                with self.assertRaises(ValueError):
                    cs.scan(self.root)

    # Recipe-ownership policy (2026-11-10): a listed guard line is P_POLICY; a guard that fetches or builds counts.
    GUARD = ('POLICY = {"reusable_recipe_owner": "hsoliwal/com.synexia"}\n'
             'def check(row):\n'
             '    if row["target_scope"] != "hsoliwal/com.synexia only":\n'
             '        raise SystemExit("recipe authoring and execution must remain exclusively in the donor")\n')

    def policy(self, rows):
        self.put(cs.POLICY_ROWS, "# notes\n" + cs.POLICY_ROWS_HEADER + "\n"
                 + "".join(f"{path}\t{line}\tfixture\n" for path, line in rows))

    def guard_rows(self, path):
        return [(path, 'POLICY = {"reusable_recipe_owner": "hsoliwal/com.synexia"}'),
                (path, 'if row["target_scope"] != "hsoliwal/com.synexia only":')]

    def test_a_listed_ownership_guard_is_policy_not_a_dependency(self):
        self.put("make/m3/compatibility/check_owner.py", self.GUARD)
        self.assertEqual(2, self.lineage()[0]["D_REPO_REF"])
        self.policy(self.guard_rows("make/m3/compatibility/check_owner.py"))
        result = cs.scan(self.root)
        self.assertEqual((0, 2, []), (result["counts"]["D_REPO_REF"], result["policy"]["P_POLICY"],
                                      result["policy_not_matched"]))
        self.assertEqual("PASS", cs.verdict(result, {k: 0 for k in cs.KINDS}, True)[0])
        self.assertIn("P_POLICY=2", cs.verdict(result, {k: 0 for k in cs.KINDS}, True)[1])
        self.commit()
        self.assertEqual(2, cs.scan(self.root, "HEAD")["policy"]["P_POLICY"])
        self.put("make/m3/compatibility/check_owner.py", self.GUARD.replace("only", "only, again"))
        result = cs.scan(self.root)  # one edited guard line: the file is no longer a pure policy guard
        self.assertEqual((2, 0), (result["counts"]["D_REPO_REF"], result["policy"]["P_POLICY"]))
        self.assertEqual(2, len(result["policy_not_matched"]))

    def test_an_ownership_guard_that_fetches_checks_out_or_builds_from_the_donor_still_counts(self):
        live = {
            "make/m3/compatibility/fetch_owner.py": self.GUARD + "import urllib.request\n"
                                               "urllib.request.urlopen('https://api.github.com/repos/hsoliwal/com.synexia')\n",
            "make/m3/compatibility/clone_owner.py": self.GUARD + "import subprocess\n"
                                               "subprocess.run(['git', 'clone', 'https://github.com/hsoliwal/com.synexia'])\n",
            "make/m3/compatibility/build_owner.py": self.GUARD + "import subprocess\n"
                                               "subprocess.run(['mvn', '-f', '../hsoliwal/com.synexia/pom.xml', 'install'])\n",
        }
        rows = []
        for path, text in live.items():
            self.put(path, text)
            rows += self.guard_rows(path)
        self.put("make/m3/compatibility/other.py", self.GUARD)  # the same guard text, not listed for this path
        self.policy(rows)
        result = cs.scan(self.root)
        self.assertEqual((3 * 3 + 2, 0), (result["counts"]["D_REPO_REF"], result["policy"]["P_POLICY"]))
        self.assertEqual(6, len(result["policy_not_matched"]))
        self.assertEqual("BLOCK", cs.verdict(result, {k: 99 for k in cs.KINDS}, True)[0])

    def test_a_malformed_policy_list_refuses(self):
        self.put("make/m3/compatibility/check_owner.py", self.GUARD)
        path = "make/m3/compatibility/check_owner.py"
        for text in (f"path\tline\n{path}\tx\n", f"path\tline\treason\n{path}\tPOLICY = 1\tr\n",
                     f"path\tline\treason\n{path}\t hsoliwal/com.synexia\tr\n",
                     f"path\tline\treason\n{path}\thsoliwal/com.synexia\t\n",
                     f"path\tline\treason\n{path}\thsoliwal/com.synexia\tr\n{path}\thsoliwal/com.synexia\tr\n",
                     f"path\tline\treason\nm3/docs/x.md\thsoliwal/com.synexia\tr\n"):
            with self.subTest(text=text):
                self.put(cs.POLICY_ROWS, text)
                with self.assertRaises(ValueError):
                    cs.scan(self.root)

    def test_unscanned_scoped_file_holds_release(self):
        self.put("m3/x/run.sh", "echo ok\n")
        result = cs.scan(self.root)
        cs.write_baseline(self.root, result)
        (self.root / "m3/x/run.sh").unlink()
        result = cs.scan(self.root)
        self.assertEqual(result["unscanned"], 1)
        self.assertEqual(cs.verdict(result, cs.read_baseline(self.root), True)[0], "HOLD")


if __name__ == "__main__":
    unittest.main()
