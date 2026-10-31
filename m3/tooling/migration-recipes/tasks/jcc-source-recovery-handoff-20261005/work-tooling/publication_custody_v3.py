"""Read the reviewed source V3 receipt without upgrading custody to raw readback."""
from __future__ import annotations

import copy
import hashlib
import json
import re
from pathlib import Path

BASE = Path(__file__).resolve().parent
PROTOCOL = "EXACT_CREATE_AND_TREE_IDENTITY_FOR_TWO_INVALID_UTF8_FIXTURES"
MANIFEST_SHA256 = "9270a6b4098960e2b7c7bd87b17325b648c4de363957f36028bacc9c1955e328"
EXPECTATIONS_SHA256 = "548b98ef8531b369f4c378bfdebf681e514a5b46da15bcf32db9ab491a7ce8d5"
VERIFIER_SHA256 = "45d1e83ba81dfaed9434fddb7df9f4962b7fbbb8cb1c9b29f90754c8fcf38baa"
SOURCE_INPUT = "be92c62ece9023b5c33676716a1076d00e26120a"
SOURCE_ROOT = "3c4f32b66633a251ba2c117090830252a7e2da03"


def bound_json(spec):
    path = Path(spec["local_path"])
    assert path.is_absolute() and path.is_file() and not path.is_symlink()
    raw = path.read_bytes()
    assert hashlib.sha256(raw).hexdigest() == spec["sha256"], path
    return json.loads(raw)


def admit_publication_custody(publication):
    if not __debug__:
        raise RuntimeError("Optimized Python disables V3 custody admission assertions")
    pub = publication
    assert pub["schema"] == "jcc-source-recovery-publication/1"
    assert pub["status"] == "PUBLISHED_CUSTODY_VERIFIED"
    assert pub["repository"] == "hsoliwal/com.synexia"
    assert pub["branch"] == "aix/jcc-source-merge-recovery-20261005"
    assert pub["parent"] == pub["source_input_revision"] == SOURCE_INPUT
    assert pub["source_input_root"] == SOURCE_ROOT
    assert re.fullmatch("[0-9a-f]{40}", pub["commit"])
    assert pub["commit"] not in (SOURCE_INPUT, "d1cf2d81ee74a4a837f78e0cd2e4ccae2b3d4906", "9dab34bd9e34e4d661004482248b5b02d94e0816", "0" * 40)
    assert re.fullmatch("[0-9a-f]{40}", pub["tree"])
    assert pub["branch_commit_parent_tree_pr_verified"] is True
    assert pub["commit_parent_tree_and_ref_verified"] is True
    assert pub["ref_update_forced"] is False
    assert pub["manifest_sha256"] == MANIFEST_SHA256
    assert pub["verification_script_sha256"] == VERIFIER_SHA256
    assert pub["base_audit_script_sha256"] == "839c35c611de2dc02f2a3f1233131b24c79d77e109aaf010df9689dc8e04893b"
    assert pub["publisher_script_sha256"] == "3cd42b4b5c4ba1e0eb01cace40bff7cb2fe967d65ac4e9a646b70d7daf4404fd"
    counts = {"required_blob_bodies_compared_byte_for_byte": 6,
              "required_blob_identities_verified_without_body_readback": 2,
              "production_blob_bodies_compared_byte_for_byte": 4,
              "nul_fixture_bodies_compared_byte_for_byte": 2}
    assert all(type(pub[key]) is int and pub[key] == value for key, value in counts.items())
    assert pub["malformed_utf8_body_readback"] is False
    assert pub["all_required_blob_bodies_read_back"] is False
    assert pub["custody_protocol"] == PROTOCOL
    expected = bound_json({"local_path": str(BASE / "custody-v3-proposal/LOCAL_EXPECTATIONS.json"), "sha256": EXPECTATIONS_SHA256})
    direct_rows = expected["mandatory_direct_production_bodies"] + expected["mandatory_direct_NUL_fixture_bodies"]
    identity_rows = expected["exact_content_addressed_invalid_utf8_exceptions"]
    direct = {row["git_blob_sha1"]: row for row in direct_rows}
    exceptions = {row["git_blob_sha1"]: row for row in identity_rows}
    assert len(direct) == 6 and len(exceptions) == 2 and not set(direct) & set(exceptions)
    designated = bound_json({"local_path": str(BASE / "source-prepublication/DESIGNATED_COMPARISON.json"),
                            "sha256": "39e7fe930028e5e7561414fbf8f75f247c634182c19648f6c0e6b6d0849d055a"})
    allowed_direct = {row["git_blob_sha1"]: row for row in designated["sources"]}
    assert len(allowed_direct) == 19
    allowed_direct.update(direct)
    assert len(allowed_direct) == 21 and not set(allowed_direct) & set(exceptions)
    observed_direct = pub["blob_body_readbacks"]
    observed_shas = {row["git_blob"] for row in observed_direct}
    assert len(observed_direct) == len(observed_shas)
    assert set(direct) <= observed_shas <= set(allowed_direct)
    for row in observed_direct:
        prior = allowed_direct[row["git_blob"]]
        assert (row["bytes"], row["sha256"]) == (prior["bytes"], prior["sha256"])

    manifest = bound_json({"local_path": pub["manifest_path"], "sha256": MANIFEST_SHA256})
    rows = manifest["rows"]
    declared = {row["path"]: {key: row[key] for key in ("path", "mode", "git_blob", "sha256", "bytes")} for row in rows}
    actual = pub["tree_entries_verified"]
    assert len(rows) == len(declared) == len(actual) == pub["payload_files"]
    assert len({row["path"] for row in actual}) == len(actual)
    assert {row["path"]: row for row in actual} == declared
    assert pub["payload_bytes"] == sum(row["bytes"] for row in rows)
    assert pub["updates"] == 4 and pub["additions"] == len(rows) - 4 and pub["exact_restorations"] == 1039
    for row in direct_rows + identity_rows:
        assert declared[row["path"]] == {"path": row["path"], "mode": row["mode"], "git_blob": row["git_blob_sha1"], "sha256": row["sha256"], "bytes": row["bytes"]}

    custody = pub["raw_fixture_custody"]
    assert custody["protocol"] == PROTOCOL
    assert custody["raw_create_requests_and_responses_verified"] == 4
    assert custody["verification_commit"] == "9dab34bd9e34e4d661004482248b5b02d94e0816"
    assert custody["verification_tree"] == "bf2b8fe43cda47f0c6363c8d376f61419e647e4d"
    assert custody["probe_direct_body_reads"] == custody["final_direct_raw_body_reads"] == 2
    assert custody["probe_failed_utf8_body_reads"] == custody["final_failed_utf8_body_reads"] == 2
    assert custody["identity_only_allowlist"] == sorted(exceptions)
    assert custody["assumption"] == "Authentic GitHub object identifiers and ordinary Git content addressing bind the exact uploaded bytes; the two malformed UTF-8 remote byte arrays and their SHA-256 values were not recovered."
    assert custody["publication_qualification"] == "Custody only; no source test or receiving acceptance promotion."
    identities = custody["blob_identity_custody"]
    assert len(identities) == 2 and {row["git_blob"] for row in identities} == set(exceptions)
    for row in identities:
        prior = exceptions[row["git_blob"]]
        assert (row["path"], row["bytes"], row["sha256_local_uploaded_bytes"]) == (prior["path"], prior["bytes"], prior["sha256"])
        assert row["method"] == "BASE64_CREATE_AND_REMOTE_GIT_TREE_IDENTITY"
        assert row["raw_body_readback"] is False and row["remote_sha256_observed"] is False
        assert row["creation_response_git_sha"] == prior["git_blob_sha1"]
        assert row["verification_tree_size"] == row["final_tree_size"] == prior["bytes"]
        local = Path(prior["local_path"]).read_bytes()
        assert len(local) == prior["bytes"] and hashlib.sha256(local).hexdigest() == prior["sha256"]
        assert hashlib.sha1(f"blob {len(local)}\0".encode() + local).hexdigest() == prior["git_blob_sha1"]
        replacement = local.decode("utf-8", errors="replace").encode("utf-8")
        assert replacement != local
        for key in ("failed_probe_read", "failed_final_read"):
            failed = row[key]
            assert failed["path"] == prior["path"] and failed["git_blob"] == prior["git_blob_sha1"]
            assert failed["raw_body_readback"] is False and failed["remote_sha256_observed"] is False
            assert failed["read_attempt"] == "FAILED_UTF8_REENCODING"
            assert failed["returned_encoded_body_bytes"] == len(replacement)
            assert failed["returned_encoded_body_sha256"] == hashlib.sha256(replacement).hexdigest()
    failure_ref = custody["failed_readback_observation"]
    assert failure_ref["sha256"] == "224b3baa99e71c846cc96319c0a829eec3521d75f68731332ac2fa59fa214112"
    assert bound_json(failure_ref)["invalid_utf8_example"]["result"] == "FAIL_INVALID_UTF8_REENCODED"
    raw_input = bound_json(pub["raw_fixture_custody_input"])
    assert raw_input["schema"] == "jcc-two-utf8-fixture-custody-input/1"
    assert raw_input["repository"] == pub["repository"] and raw_input["protocol"] == PROTOCOL
    assert raw_input["preflight_sha256"] == pub["preflight_sha256"] and raw_input["manifest_sha256"] == MANIFEST_SHA256
    assert raw_input["failed_readback_observation"] == failure_ref
    remote = bound_json({"local_path": pub["readback_path"], "sha256": pub["readback_sha256"]})
    assert remote["custody_protocol"] == PROTOCOL and remote["raw_fixture_custody_input"] == pub["raw_fixture_custody_input"]
    assert remote["commit"]["sha"] == pub["commit"] and remote["commit"]["tree"]["sha"] == pub["tree"]
    assert remote["ref"]["ref"] == "refs/heads/" + pub["branch"] and remote["ref"]["object"]["sha"] == pub["commit"]
    assert remote["pull_request"]["html_url"] == pub["pull_request"]["url"]

    return {"status": pub["status"], "protocol": PROTOCOL, **counts,
            "observed_direct_body_readbacks": len(observed_direct),
            "malformed_utf8_body_readback": False, "all_required_blob_bodies_read_back": False,
            "blob_body_readbacks": copy.deepcopy(observed_direct),
            "raw_fixture_custody": copy.deepcopy(custody),
            "raw_fixture_custody_input": copy.deepcopy(pub["raw_fixture_custody_input"]),
            "verification_script_sha256": pub["verification_script_sha256"],
            "reviewed_raw_custody_helper_sha256": "45fe2a39cc936f94e20cf0051f3844e6d6b5ba59b87b2d3ca175bb60917603be",
            "qualification": pub["qualification"],
            "scope": "Verified source publication custody; six mandatory direct body comparisons, separately recorded additional designated-owner reads, and exactly two explicit invalid-UTF-8 Git-identity obligations. This is not universal remote raw-byte or remote SHA256 readback."}
