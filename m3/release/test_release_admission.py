# SPDX-License-Identifier: Apache-2.0
from __future__ import annotations

import importlib.util
import json
import tempfile
import unittest
from pathlib import Path

MODULE_PATH = Path(__file__).with_name("verify_release_admission.py")
spec = importlib.util.spec_from_file_location("m3_release_admission", MODULE_PATH)
release = importlib.util.module_from_spec(spec)
assert spec.loader is not None
spec.loader.exec_module(release)


class ReleaseAdmissionUnitTest(unittest.TestCase):
    def test_unique_json_rejects_duplicates(self):
        with self.assertRaises(ValueError):
            json.loads('{"a":1,"a":2}', object_pairs_hook=release.unique_object)

    def test_contract_priority_is_exact(self):
        contract = json.loads((Path(__file__).with_name("release-contract.json")).read_text())
        self.assertEqual(release.EXPECTED_PRIORITY, contract["priority_order"])

    def test_unqualified_claims_remain_not_claimed(self):
        contract = json.loads((Path(__file__).with_name("release-contract.json")).read_text())
        for key in release.UNQUALIFIED_CLAIMS:
            self.assertEqual("NOT_CLAIMED", contract["current_claims"][key])

    def test_sha256_is_stable(self):
        with tempfile.TemporaryDirectory() as td:
            path = Path(td) / "x"
            path.write_bytes(b"m3\n")
            self.assertEqual(
                "3831561589b9de83145d35aebf9d2b55a9843a09e8b7754bedd56173e25b3aa6",
                release.sha256(path),
            )


if __name__ == "__main__":
    unittest.main()
