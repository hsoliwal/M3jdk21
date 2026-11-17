#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Unit tests for gate.py: GitHub's filter pattern cheat sheet and the trigger decisions (stdlib only).
Run: python3 .github/checks-superset/test_gate.py"""
import os
import sys
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gate  # noqa: E402


class Ev(gate.Event):
    def __init__(self, name, ref, files=None, payload=None, base_ref=""):
        self.name, self.ref, self.payload = name, ref, payload or {"action": "synchronize"}
        self._files, self.fetched = files, True
        os.environ["GITHUB_BASE_REF"] = base_ref


class Patterns(unittest.TestCase):
    CASES = [  # (patterns, value, expected) from the GitHub docs filter pattern cheat sheet
        (["feature/*"], "feature/my-branch", True), (["feature/*"], "feature/a/b", False),
        (["feature/**"], "feature/mona/the/octocat", True), (["*"], "main", True), (["*"], "releases/x", False),
        (["**"], "releases/mona", True), (["*feature"], "ver-10-feature", True), (["v2*"], "v2.9", True),
        (["v[12].[0-9]+.[0-9]+"], "v1.10.1", True), (["v[12].[0-9]+.[0-9]+"], "v3.0.0", False),
        (["*"], "README.md", True), (["*"], "a/README.md", False), (["*.jsx?"], "page.js", True),
        (["*.jsx?"], "page.jsx", True), (["*.js"], "js/index.js", False), (["**.js"], "src/js/app.js", True),
        (["docs/*"], "docs/mona/octocat.txt", False), (["docs/**"], "docs/mona/octocat.txt", True),
        (["docs/**/*.md"], "docs/README.md", True), (["docs/**/*.md"], "docs/a/markdown/file.md", True),
        (["**/docs/**"], "space/docs/plan/space.doc", True), (["**/README.md"], "README.md", True),
        (["**/README.md"], "js/README.md", True), (["**/*src/**"], "my-src/code/js/app.js", True),
        (["**/*-post.md"], "path/their-post.md", True), (["**/migrate-*.sql"], "db/sept/migrate-v1.sql", True),
        (["*.md", "!README.md"], "hello.md", True), (["*.md", "!README.md"], "README.md", False),
        (["*.md", "!README.md"], "docs/hello.md", False), (["*.md", "!README.md", "README*"], "README.md", True),
    ]

    def test_cheat_sheet(self):
        for pats, value, want in self.CASES:
            self.assertEqual(gate.matched(pats, value), want, f"{pats} vs {value}")


class Decisions(unittest.TestCase):
    def test_push_branch_and_paths(self):
        m = {"push": {"branches": ["master"], "paths": ["src/**"]}}
        self.assertTrue(gate.decide("k", m, Ev("push", "refs/heads/master", ["src/a.java"])))
        self.assertFalse(gate.decide("k", m, Ev("push", "refs/heads/master", ["doc/a.md"])))
        self.assertFalse(gate.decide("k", m, Ev("push", "refs/heads/topic", ["src/a.java"])))
        self.assertFalse(gate.decide("k", m, Ev("push", "refs/heads/master", [])))

    def test_tags_need_a_tag_filter_and_ignore_paths(self):
        self.assertFalse(gate.decide("k", {"push": {"branches": ["**"]}}, Ev("push", "refs/tags/v1", [])))
        self.assertTrue(gate.decide("k", {"push": {"tags": ["v*"], "paths": ["x/**"]}}, Ev("push", "refs/tags/v1", [])))
        self.assertTrue(gate.decide("k", {"push": {}}, Ev("push", "refs/tags/v1", [])))
        self.assertFalse(gate.decide("k", {"push": {"tags": ["v*"]}}, Ev("push", "refs/heads/v1", ["a"])))

    def test_branches_ignore_and_paths_ignore(self):
        m = {"push": {"branches-ignore": ["master", "pr/*"], "paths-ignore": ["doc/**"]}}
        self.assertFalse(gate.decide("k", m, Ev("push", "refs/heads/pr/1", ["src/a"])))
        self.assertFalse(gate.decide("k", m, Ev("push", "refs/heads/topic", ["doc/a.md"])))
        self.assertTrue(gate.decide("k", m, Ev("push", "refs/heads/topic", ["doc/a.md", "src/a"])))

    def test_pull_request_base_types_and_fail_open(self):
        m = {"pull_request": {"branches": ["master"], "paths": ["src/**"]}}
        self.assertTrue(gate.decide("k", m, Ev("pull_request", "", ["src/a"], base_ref="master")))
        self.assertFalse(gate.decide("k", m, Ev("pull_request", "", ["src/a"], base_ref="jdk21u")))
        self.assertTrue(gate.decide("k", m, Ev("pull_request", "", None, base_ref="master")))
        closed = Ev("pull_request", "", ["src/a"], {"action": "closed"}, "master")
        self.assertFalse(gate.decide("k", m, closed))
        self.assertTrue(gate.decide("k", {"pull_request": {"types": ["closed"]}}, closed))

    def test_dispatch_selection(self):
        m = {"file": "a.yml", "workflow_dispatch": {"inputs": []}}
        self.assertTrue(gate.decide("a", m, Ev("workflow_dispatch", "", payload={"inputs": {"select": "all"}})))
        self.assertTrue(gate.decide("a", m, Ev("workflow_dispatch", "", payload={"inputs": {"select": "x, a.yml"}})))
        self.assertFalse(gate.decide("a", m, Ev("workflow_dispatch", "", payload={"inputs": {"select": "b"}})))
        self.assertFalse(gate.decide("a", {"file": "a.yml"}, Ev("workflow_dispatch", "", payload={})))


if __name__ == "__main__":
    unittest.main()
