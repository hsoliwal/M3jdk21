#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
"""Fail-closed Maven *declared dependency* census for current M3 consumer modules.

This does NOT assert the full transitive Maven graph, API parity or binary source inlining.
Provider-dependent interop remains blocked from Maven deploy until source-vendored.
"""
from __future__ import annotations

import argparse
from dataclasses import dataclass
from pathlib import Path
import xml.etree.ElementTree as ET

NS = "{http://maven.apache.org/POM/4.0.0}"
MODULES = (
    ("m3/arrays/pom.xml", "core"),
    ("m3/collections/pom.xml", "core"),
    ("m3/collections/interop/pom.xml", "optional-interop"),
)
FIRST_PARTY = "com.m3"


class DependencyBoundaryError(ValueError):
    """Dependency leakage, missing input or invalid quarantine."""


@dataclass(frozen=True)
class Dependency:
    group: str
    artifact: str
    scope: str
    optional: bool

    @property
    def gav(self) -> str:
        return f"{self.group}:{self.artifact}"


@dataclass(frozen=True)
class Result:
    path: str
    status: str
    external: tuple[Dependency, ...]


def child_text(element: ET.Element, name: str, default: str = "") -> str:
    node = element.find(NS + name)
    return (node.text or "").strip() if node is not None else default


def dependencies(project: ET.Element) -> tuple[Dependency, ...]:
    # Only project-owned <project><dependencies>. Build plugins, test plugins and
    # dependencyManagement do not become the artifact's direct runtime graph.
    owner = project.find(NS + "dependencies")
    if owner is None:
        return ()
    out = []
    for node in owner.findall(NS + "dependency"):
        group = child_text(node, "groupId")
        artifact = child_text(node, "artifactId")
        scope = child_text(node, "scope", "compile")
        if not group or not artifact:
            raise DependencyBoundaryError("incomplete dependency coordinate")
        if scope not in ("compile", "runtime", "provided", "test", "system", "import"):
            raise DependencyBoundaryError("unreviewed Maven dependency scope: " + scope)
        # 'provided' and 'optional' can still be referenced at runtime by public
        # provider signatures; neither is proof of an independent consumer jar.
        out.append(Dependency(group, artifact, scope, child_text(node, "optional") == "true"))
    return tuple(out)


def deploy_quarantined(project: ET.Element) -> bool:
    build = project.find(NS + "build")
    if build is None:
        return False
    plugins = build.find(NS + "plugins")
    if plugins is None:
        return False
    found = []
    for plugin in plugins.findall(NS + "plugin"):
        if (child_text(plugin, "groupId", "org.apache.maven.plugins") == "org.apache.maven.plugins"
                and child_text(plugin, "artifactId") == "maven-deploy-plugin"):
            config = plugin.find(NS + "configuration")
            found.append(child_text(config, "skip") if config is not None else "")
    # No profile may re-enable deploy while unresolved direct provider deps exist.
    profiles = project.find(NS + "profiles")
    if profiles is not None:
        for profile in profiles.findall(NS + "profile"):
            for plugin in profile.findall("./" + NS + "build/" + NS + "plugins/" + NS + "plugin"):
                if child_text(plugin, "artifactId") == "maven-deploy-plugin":
                    return False
    return found == ["true"]


def inspect_pom(source: str, path: str, role: str) -> Result:
    try:
        root = ET.fromstring(source)
    except ET.ParseError as exc:
        raise DependencyBoundaryError("invalid Maven XML: " + path) from exc
    if root.tag != NS + "project":
        raise DependencyBoundaryError("unknown Maven model namespace: " + path)
    deps = dependencies(root)
    # Test-scoped entries are excluded from deployable runtime obligations.
    # 'provided' and 'optional' are deliberately NOT a way around the boundary.
    external = tuple(d for d in deps if d.group != FIRST_PARTY and d.scope not in ("test", "import"))
    if role == "core":
        if external:
            raise DependencyBoundaryError("consumer-facing external dependency: " + path
                                         + " -> " + ",".join(d.gav for d in external))
        return Result(path, "CORE_DECLARED_DIRECT_CLOSURE_PASS", ())
    if role == "optional-interop":
        if external and not deploy_quarantined(root):
            raise DependencyBoundaryError("provider-dependent interop must be deploy-quarantined: "
                                         + path + " -> " + ",".join(d.gav for d in external))
        if external:
            return Result(path, "PROVIDER_SOURCE_VENDING_HOLD_DEPLOY_QUARANTINED", external)
        return Result(path, "PROVIDER_DECLARED_DIRECT_CLOSURE_PASS", ())
    raise DependencyBoundaryError("unknown consumer role: " + role)


def verify(root: Path) -> tuple[Result, ...]:
    absolute = root.resolve(strict=True)
    results = []
    for relative, role in MODULES:
        path = absolute / relative
        if path.is_symlink() or not path.is_file() or not path.resolve().is_relative_to(absolute):
            raise DependencyBoundaryError("missing or symlinked Maven model: " + relative)
        if path.stat().st_size > 2_000_000:
            raise DependencyBoundaryError("unexpectedly large Maven model: " + relative)
        results.append(inspect_pom(path.read_text(encoding="utf-8"), relative, role))
    return tuple(results)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[3])
    args = parser.parse_args()
    try:
        results = verify(args.root)
    except (DependencyBoundaryError, OSError, UnicodeError) as error:
        print("M3_DECLARED_DEPENDENCY_BOUNDARY_REFUSED", str(error))
        return 1
    for result in results:
        print(result.status, result.path, "externals=" + str(len(result.external)),
              ",".join(d.gav for d in result.external))
    print("M3_DECLARED_DEPENDENCY_BOUNDARY_PASS_WITH_PROVIDER_HOLD",
          "checked=" + str(len(results)), "full_transitive_graph=NOT_VERIFIED")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
