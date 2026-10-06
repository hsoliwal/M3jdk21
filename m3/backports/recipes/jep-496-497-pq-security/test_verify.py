# SPDX-License-Identifier: Apache-2.0
import csv
import tempfile
import unittest
from pathlib import Path

import verify


class PqSecurityInventoryVerifierTest(unittest.TestCase):
    def test_repository_packet_is_exact(self):
        verify.verify(Path(__file__).resolve().parent)

    def test_public_api_exclusion_drift_fails(self):
        source = Path(__file__).resolve().parent
        with tempfile.TemporaryDirectory() as directory:
            packet = Path(directory)
            for name in ("DEPENDENCY_GRAPH.tsv", "PATHS.tsv"):
                (packet / name).write_text(
                    (source / name).read_text(encoding="utf-8"),
                    encoding="utf-8",
                )
            path_file = packet / "PATHS.tsv"
            text = path_file.read_text(encoding="utf-8")
            path_file.write_text(
                text.replace(
                    "JEP-496\tsrc/java.base/share/classes/java/security/spec/"
                    "NamedParameterSpec.java\tMODIFY\tEXCLUDE_DEFAULT",
                    "JEP-496\tsrc/java.base/share/classes/java/security/spec/"
                    "NamedParameterSpec.java\tMODIFY\tCANDIDATE",
                    1,
                ),
                encoding="utf-8",
            )
            with self.assertRaises(AssertionError):
                verify.verify(packet)

    def test_dependency_reorder_fails(self):
        source = Path(__file__).resolve().parent
        with tempfile.TemporaryDirectory() as directory:
            packet = Path(directory)
            graph = list(
                csv.DictReader(
                    (source / "DEPENDENCY_GRAPH.tsv").open(
                        encoding="utf-8", newline=""
                    ),
                    delimiter="\t",
                )
            )
            graph[1], graph[2] = graph[2], graph[1]
            with (packet / "DEPENDENCY_GRAPH.tsv").open(
                "w", encoding="utf-8", newline=""
            ) as handle:
                writer = csv.DictWriter(
                    handle,
                    fieldnames=graph[0].keys(),
                    delimiter="\t",
                    lineterminator="\n",
                )
                writer.writeheader()
                writer.writerows(graph)
            (packet / "PATHS.tsv").write_text(
                (source / "PATHS.tsv").read_text(encoding="utf-8"),
                encoding="utf-8",
            )
            with self.assertRaises(AssertionError):
                verify.verify(packet)


if __name__ == "__main__":
    unittest.main()
