# SPDX-License-Identifier: Apache-2.0

from __future__ import annotations

import csv
import json
import tempfile
import unittest
from pathlib import Path

import absorption_plan


class AbsorptionPlanTest(unittest.TestCase):
    def test_join_is_count_independent_and_fail_closed(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            convergence = root / "SOURCE_CONVERGENCE.tsv"
            convergence.write_text(
                "path\tpreSha256\tpostSha256\tatomizationChanged\tpatternizationChanged"
                "\tdocumentationChanged\tfixedPoint\tstatus\tmessage\tcandidate\n"
                f"src/java.base/share/classes/p/A.java\t{'1'*64}\t{'2'*64}\ttrue\ttrue\ttrue\ttrue\tCONVERGED_CHANGED\t\tcandidates/a.java\n"
                f"src/java.base/share/classes/p/B.java\t{'3'*64}\t{'3'*64}\tfalse\tfalse\tfalse\tfalse\tHOLD\tparse\t\n"
                f"test/jdk/p/TestA.java\t{'4'*64}\t{'4'*64}\tfalse\tfalse\tfalse\ttrue\tCONVERGED_UNCHANGED\t\t\n",
                encoding="utf-8",
            )
            convergence_root = absorption_plan._sha256_bytes(convergence.read_bytes())
            (root / "SOURCE_CONVERGENCE.summary.tsv").write_text(
                "files\tchanged\tholds\tsemanticRoot\n"
                f"3\t1\t1\t{convergence_root}\n",
                encoding="utf-8",
            )

            features = root / "BACKPORT_WORK_QUEUE.tsv"
            features.write_text(
                "source_type\trelease\tidentity\ttitle\tdomain\tdisposition\tcurrent_pass"
                "\taction\trequired_scope\trisk\tdependency_or_commit\treason\n"
                "JEP\t27\tJEP-999\tCandidate\tlibrary\tcandidate\t2\tINVESTIGATE_DEPENDENCY_CLOSURE\tMODULE\tNORMAL\t\tproof\n"
                "JEP\t27\tJEP-998\tLanguage\tlanguage\treject-language\t1\tEXCLUDE_LANGUAGE\tLIBRARY_API\tINCOMPATIBLE\t\tgrammar\n"
                "JBS\t27\tJDK-9999999\tSeed\ttools\tcandidate-adapted\t5\tVERIFY_HASH_PINNED_RECIPE\tMODULE\tNORMAL\tabc\tseed\n",
                encoding="utf-8",
            )

            commits = root / "COMPATIBILITY_QUEUE.tsv"
            header = (
                "order\trelease\tcommit\tjbs_ids\tsubject\tdomain\tinventory_disposition"
                "\trisk\tscope_floor\tproof_lane\trecipe_strategy\tpriority"
                "\tcompatibility_state\tnext_action\tpaths\n"
            )
            commits.write_text(
                header
                + f"0\t27\t{'a'*40}\tJDK-1\tA\tcore-libs\treview\tMEDIUM\tFILE\tCORE_LIBRARY\tOPENREWRITE_OR_HASH_PINNED_JAVA\t35\tPENDING_COMPATIBILITY_PROOF\tPROVE_CORE_LIBRARY_BACKPORT\tsrc/java.base/share/classes/p/A.java\n"
                + f"1\t27\t{'b'*40}\tJDK-2\tB\tcore-libs\treview\tMEDIUM\tFILE\tCORE_LIBRARY\tOPENREWRITE_OR_HASH_PINNED_JAVA\t35\tPENDING_COMPATIBILITY_PROOF\tPROVE_CORE_LIBRARY_BACKPORT\tsrc/java.base/share/classes/p/B.java\n"
                + f"2\t27\t{'c'*40}\tJDK-3\tMissing\tcore-libs\treview\tMEDIUM\tFILE\tCORE_LIBRARY\tOPENREWRITE_OR_HASH_PINNED_JAVA\t35\tPENDING_COMPATIBILITY_PROOF\tPROVE_CORE_LIBRARY_BACKPORT\tsrc/java.base/share/classes/p/C.java\n"
                + f"3\t27\t{'d'*40}\tJDK-4\tBuild\tbuild\treview\tLOW\tFILE\tBUILD\tHASH_PINNED_VERBATIM_PATCH\t10\tPENDING_COMPATIBILITY_PROOF\tPROVE_BUILD_BACKPORT\tmake/Images.gmk\n"
                + f"4\t27\t{'e'*40}\tJDK-5\tTest\ttest\treview\tLOW\tFILE\tTEST\tOPENREWRITE_OR_HASH_PINNED_JAVA\t11\tPENDING_COMPATIBILITY_PROOF\tPROVE_TEST_BACKPORT\ttest/jdk/p/TestA.java\n",
                encoding="utf-8",
            )

            rows, actual_root = absorption_plan.build_plan(
                convergence, features, commits
            )
            self.assertEqual(convergence_root, actual_root)
            self.assertEqual(8, len(rows))
            self.assertEqual(
                ["AWAIT_FILE_ATOMS", "EXCLUDED_BY_JAVA21_CONTRACT", "AWAIT_FILE_ATOMS"],
                [row.baseline_state for row in rows[:3]],
            )
            self.assertEqual(
                [
                    "JAVA_BASELINE_CONVERGED",
                    "JAVA_BASELINE_HOLD",
                    "JAVA_BASELINE_MISSING",
                    "NO_JAVA_SOURCE",
                    "JAVA_BASELINE_CONVERGED",
                ],
                [row.baseline_state for row in rows[3:]],
            )
            self.assertTrue(all(not row.mutation_authority for row in rows))
            self.assertTrue(all(not row.promotion_authority for row in rows))

            out = root / "ABSORPTION_PLAN.tsv"
            summary = root / "ABSORPTION_PLAN.summary.json"
            root_file = root / "ABSORPTION_PLAN.sha256"
            semantic = absorption_plan.write(rows, actual_root, out, summary, root_file)
            self.assertEqual(semantic, root_file.read_text(encoding="utf-8").strip())
            parsed = json.loads(summary.read_text(encoding="utf-8"))
            self.assertEqual(3, parsed["feature_rows"])
            self.assertEqual(5, parsed["commit_rows"])
            self.assertFalse(parsed["mutation_authority"])
            self.assertFalse(parsed["promotion_authority"])

            with out.open("r", encoding="utf-8", newline="") as handle:
                emitted = list(csv.DictReader(handle, delimiter="\t"))
            self.assertEqual(8, len(emitted))
            self.assertEqual("false", emitted[0]["mutation_authority"])

    def test_rejects_noncontiguous_commit_queue_and_summary_drift(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            convergence = root / "SOURCE_CONVERGENCE.tsv"
            convergence.write_text(
                "path\tpreSha256\tpostSha256\tfixedPoint\tstatus\tcandidate\n"
                f"src/java.base/share/classes/p/A.java\t{'1'*64}\t{'1'*64}\ttrue\tCONVERGED_UNCHANGED\t\n",
                encoding="utf-8",
            )
            (root / "SOURCE_CONVERGENCE.summary.tsv").write_text(
                "files\tchanged\tholds\tsemanticRoot\n"
                f"1\t0\t0\t{'f'*64}\n",
                encoding="utf-8",
            )
            with self.assertRaisesRegex(ValueError, "summary drift"):
                absorption_plan.load_convergence(convergence)

            (root / "SOURCE_CONVERGENCE.summary.tsv").unlink()
            rows, convergence_root = absorption_plan.load_convergence(convergence)
            commits = root / "COMPATIBILITY_QUEUE.tsv"
            commits.write_text(
                "order\trelease\tcommit\tjbs_ids\tsubject\tdomain\tinventory_disposition"
                "\trisk\tscope_floor\tproof_lane\trecipe_strategy\tpriority"
                "\tcompatibility_state\tnext_action\tpaths\n"
                + f"1\t27\t{'a'*40}\t\tA\tbuild\treview\tLOW\tFILE\tBUILD\tHASH_PINNED_VERBATIM_PATCH\t10\tPENDING_COMPATIBILITY_PROOF\tPROVE_BUILD_BACKPORT\tmake/a.gmk\n",
                encoding="utf-8",
            )
            with self.assertRaisesRegex(ValueError, "order is not contiguous"):
                absorption_plan.commit_rows(commits, rows, convergence_root, 0)


if __name__ == "__main__":
    unittest.main()
