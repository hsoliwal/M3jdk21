#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
"""Contract and refusal oracles for M3 declared direct runtime-dependency custody."""
import sys
from pathlib import Path
from tempfile import TemporaryDirectory
from unittest import TestCase, main

sys.path.insert(0, str(Path(__file__).resolve().parent))
from no_external_runtime import (  # noqa: E402
    DependencyBoundaryError, inspect_pom, verify, MODULES,
)


def pom(deps="", plugins="", profiles=""):
    return ("<project xmlns='http://maven.apache.org/POM/4.0.0'>"
            "<modelVersion>4.0.0</modelVersion><groupId>com.m3</groupId>"
            "<artifactId>fixture</artifactId><version>1</version>"
            "<dependencies>" + deps + "</dependencies><build><plugins>" + plugins
            + "</plugins></build><profiles>" + profiles + "</profiles></project>")


def dep(group, artifact="library", scope="compile", optional=""):
    return ("<dependency><groupId>" + group + "</groupId><artifactId>"
            + artifact + "</artifactId><scope>" + scope + "</scope>"
            + ("<optional>true</optional>" if optional else "") + "</dependency>")


INSTALL_QUARANTINE = ("<plugin><groupId>org.apache.maven.plugins</groupId>"
                      "<artifactId>maven-install-plugin</artifactId><version>3.1.3</version>"
                      "<configuration><skip>true</skip></configuration></plugin>")
DEPLOY_QUARANTINE = ("<plugin><groupId>org.apache.maven.plugins</groupId>"
                     "<artifactId>maven-deploy-plugin</artifactId><version>3.1.3</version>"
                     "<configuration><skip>true</skip></configuration></plugin>")
QUARANTINE = INSTALL_QUARANTINE + DEPLOY_QUARANTINE

class DependencyBoundaryTests(TestCase):
    def test_core_only_test_libraries(self):
        result = inspect_pom(pom(dep("org.junit.jupiter", scope="test")), "core.xml", "core")
        self.assertEqual(result.status, "CORE_DECLARED_DIRECT_CLOSURE_PASS")

    def test_core_first_party_dependency(self):
        result = inspect_pom(pom(dep("com.m3", "m3-arrays")), "core.xml", "core")
        self.assertEqual(len(result.external), 0)

    def test_core_external_compile_fails(self):
        with self.assertRaisesRegex(DependencyBoundaryError, "external dependency"):
            inspect_pom(pom(dep("com.google.guava")), "core.xml", "core")

    def test_provider_dependency_without_quarantine_fails(self):
        with self.assertRaisesRegex(DependencyBoundaryError, "install/deploy-quarantined"):
            inspect_pom(pom(dep("org.springframework")), "interop.xml", "optional-interop")

    def test_quarantined_provider_remains_explicit_hold(self):
        result = inspect_pom(pom(dep("org.springframework"), QUARANTINE),
                             "interop.xml", "optional-interop")
        self.assertEqual(result.status, "PROVIDER_SOURCE_VENDING_HOLD_INSTALL_DEPLOY_QUARANTINED")
        self.assertEqual(len(result.external), 1)

    def test_install_missing_refuses_deploy_only(self):
        with self.assertRaisesRegex(DependencyBoundaryError, "install/deploy-quarantined"):
            inspect_pom(pom(dep("org.springframework"), DEPLOY_QUARANTINE),
                        "interop.xml", "optional-interop")

    def test_install_false_or_property_is_not_quarantine(self):
        for state in ("false", "${maven.install.skip}"):
            wrong = INSTALL_QUARANTINE.replace("<skip>true</skip>", "<skip>" + state + "</skip>")
            with self.assertRaisesRegex(DependencyBoundaryError, "install/deploy-quarantined"):
                inspect_pom(pom(dep("org.springframework"), wrong + DEPLOY_QUARANTINE),
                            "interop.xml", "optional-interop")

    def test_duplicate_install_plugin_refused(self):
        with self.assertRaisesRegex(DependencyBoundaryError, "install/deploy-quarantined"):
            inspect_pom(pom(dep("org.springframework"), INSTALL_QUARANTINE + QUARANTINE),
                        "interop.xml", "optional-interop")

    def test_install_profile_override_refused(self):
        override = ("<profile><id>force-install</id><build><plugins><plugin>"
                    "<artifactId>maven-install-plugin</artifactId>"
                    "<configuration><skip>false</skip></configuration>"
                    "</plugin></plugins></build></profile>")
        with self.assertRaisesRegex(DependencyBoundaryError, "install/deploy-quarantined"):
            inspect_pom(pom(dep("org.springframework"), QUARANTINE, override),
                        "interop.xml", "optional-interop")

    def test_install_plugin_management_override_refused(self):
        managed = ("<pluginManagement><plugins><plugin>"
                   "<artifactId>maven-install-plugin</artifactId>"
                   "<configuration><skip>false</skip></configuration>"
                   "</plugin></plugins></pluginManagement>")
        source = pom(dep("org.springframework"), QUARANTINE).replace("</build>", managed + "</build>")
        with self.assertRaisesRegex(DependencyBoundaryError, "install/deploy-quarantined"):
            inspect_pom(source, "interop.xml", "optional-interop")

    def test_provided_is_not_dependency_closure(self):
        with self.assertRaisesRegex(DependencyBoundaryError, "external dependency"):
            inspect_pom(pom(dep("org.springframework", scope="provided")), "core.xml", "core")

    def test_optional_is_not_dependency_closure(self):
        with self.assertRaisesRegex(DependencyBoundaryError, "external dependency"):
            inspect_pom(pom(dep("org.springframework", optional="true")), "core.xml", "core")

    def test_plugin_dependencies_do_not_count_as_artifact_dependencies(self):
        plugin = "<plugin><artifactId>maven-compiler-plugin</artifactId><dependencies>"
        plugin += dep("org.apache.maven.plugins") + "</dependencies></plugin>"
        result = inspect_pom(pom("", plugin), "core.xml", "core")
        self.assertEqual(result.external, ())

    def test_profile_override_can_not_enable_publication(self):
        override = ("<profile><id>force-deploy</id><build><plugins><plugin>"
                    "<artifactId>maven-deploy-plugin</artifactId>"
                    "<configuration><skip>false</skip></configuration>"
                    "</plugin></plugins></build></profile>")
        with self.assertRaisesRegex(DependencyBoundaryError, "install/deploy-quarantined"):
            inspect_pom(pom(dep("org.springframework"), QUARANTINE, override),
                        "interop.xml", "optional-interop")

    def test_missing_maven_owner_is_refused(self):
        with TemporaryDirectory() as temp:
            root = Path(temp)
            for relative, _ in MODULES[:-1]:
                file = root / relative
                file.parent.mkdir(parents=True, exist_ok=True)
                file.write_text(pom(), encoding="utf-8")
            with self.assertRaisesRegex(DependencyBoundaryError, "missing or symlinked"):
                verify(root)

    def test_unapproved_scope_or_malformed_xml_refused(self):
        with self.assertRaisesRegex(DependencyBoundaryError, "unreviewed Maven"):
            inspect_pom(pom(dep("com.m3", scope="magic")), "core.xml", "core")
        with self.assertRaisesRegex(DependencyBoundaryError, "invalid Maven XML"):
            inspect_pom("<project>", "bad.xml", "core")


if __name__ == "__main__":
    main()
