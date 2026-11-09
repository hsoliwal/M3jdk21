# Copyright 2026 Hitesh Soliwal. SPDX-License-Identifier: Apache-2.0
"""Capture a bounded public-repository PR witness, separate from frozen Git truth."""
from __future__ import annotations

import argparse
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import re
import urllib.request
import urllib.parse


class SameHostRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        if (urllib.parse.urlparse(newurl).netloc != "api.github.com"
                or urllib.parse.urlparse(newurl).scheme != "https"):
            raise ValueError("cross-host API redirect refused")
        return super().redirect_request(req, fp, code, msg, headers, newurl)


def collect(snapshot: dict) -> dict:
    repo = snapshot["repository"]
    if not re.fullmatch(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+", repo):
        raise ValueError("invalid repository name")
    wanted = set(snapshot["pr_numbers"])
    if not wanted or any(not isinstance(n, int) or n < 1 for n in wanted):
        raise ValueError("invalid PR number boundary")
    headers = {"Accept": "application/vnd.github+json", "X-GitHub-Api-Version": "2022-11-28"}
    if os.environ.get("GH_TOKEN"):
        headers["Authorization"] = "Bearer " + os.environ["GH_TOKEN"]
    opener = urllib.request.build_opener(SameHostRedirect())
    found = {}
    for page in range(1, 101):
        url = f"https://api.github.com/repos/{repo}/pulls?state=all&per_page=100&sort=created&direction=asc&page={page}"
        with opener.open(urllib.request.Request(url, headers=headers), timeout=30) as response:
            raw = response.read(8 * 1024 * 1024 + 1)
        if len(raw) > 8 * 1024 * 1024:
            raise ValueError("API response budget exceeded")
        values = json.loads(raw)
        if not isinstance(values, list):
            raise ValueError("expected a PR list")
        for pr in values:
            if pr["number"] in wanted:
                found[pr["number"]] = {"number": pr["number"], "title": pr["title"], "state": pr["state"],
                                       "head_sha": pr["head"]["sha"], "base_ref": pr["base"]["ref"],
                                       "merge_commit_sha": pr.get("merge_commit_sha"), "merged_at": pr.get("merged_at"),
                                       "updated_at": pr["updated_at"], "html_url": pr["html_url"]}
        if len(values) < 100:
            break
    else:
        raise ValueError("pagination budget exceeded")
    if set(found) != wanted:
        raise ValueError("PR witness incomplete")
    return {"repository": repo, "captured_at": datetime.now(timezone.utc).isoformat(),
            "authority": "CURRENT_METADATA_WITNESS_NOT_RUNTIME_PROOF",
            "pulls": [found[n] for n in sorted(found)]}


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--snapshot", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    data = collect(json.loads(args.snapshot.read_text(encoding="utf-8")))
    with args.output.open("x", encoding="utf-8") as stream:
        stream.write(json.dumps(data, indent=2, sort_keys=True) + "\n")
    print("PR_WITNESS_CAPTURED count=" + str(len(data["pulls"])))
