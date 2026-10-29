# SPDX-License-Identifier: Apache-2.0
"""Source-only checks for the hosted M3JDK21 JDK21u platform toolchains.

Passing these tests is not evidence that full JDK build or jtreg succeeded.
"""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[2]

def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")

class HostedJdkToolchainContract(unittest.TestCase):
    def test_hosted_platforms_are_supported_runners(self):
        main = read(".github/workflows/main.yml")
        self.assertIn("macos-15-intel", main)
        self.assertIn("macos-15", main)
        self.assertIn("windows-2022", main)
        self.assertNotIn("macos-11", main)
        self.assertNotIn("windows-2019", main)
        self.assertIn("test-macos-aarch64:", main)
        self.assertIn("xcode-toolset-version: '16.4'", main)

    def test_windows_checkout_uses_long_paths_before_checkout(self):
        for file in (".github/workflows/build-windows.yml", ".github/workflows/test.yml"):
            with self.subTest(file=file):
                src = read(file)
                self.assertIn("shell: pwsh", src)
                self.assertLess(src.index("git config --global core.longpaths true"),
                                src.index("uses: actions/checkout@v6"))
                self.assertNotIn("windows-2019", src)
        self.assertIn("Visual Studio/2022/Enterprise",
                      read(".github/workflows/build-windows.yml"))

    def test_macos_native_bootstrap_is_host_specific(self):
        src = read(".github/workflows/build-macos.yml")
        tests = read(".github/workflows/test.yml")
        self.assertIn("runs-on: ${{ inputs.runs-on }}", src)
        self.assertIn("platform: ${{ inputs.platform }}", src)
        self.assertIn("test -d \"$xcode_dir\"", src)
        self.assertIn("$(brew --prefix make)/libexec/gnubin", src)
        self.assertIn("xcode-toolset-version:", tests)
        self.assertNotIn("Xcode_11.7.app", tests)

    def test_exact_official_bootjdk_binaries_cover_macos_arm64(self):
        src = read("make/conf/github-actions.conf")
        for name in ("LINUX_X64", "MACOS_X64", "MACOS_AARCH64", "WINDOWS_X64"):
            with self.subTest(target=name):
                self.assertRegex(src, rf"(?m)^{name}_BOOT_JDK_URL=https://")
                self.assertRegex(src, rf"(?m)^{name}_BOOT_JDK_SHA256=[a-f0-9]{{64}}$")
        self.assertIn("actions/cache@v5",
                      read(".github/actions/get-bootjdk/action.yml"))

    def test_test_framework_actions_keep_supported_checkout(self):
        self.assertIn("actions/checkout@v6",
                      read(".github/actions/get-jtreg/action.yml"))
        self.assertIn("actions/checkout@v6",
                      read(".github/actions/get-gtest/action.yml"))
        self.assertIn("actions/cache@v5",
                      read(".github/actions/get-jtreg/action.yml"))

if __name__ == "__main__":
    unittest.main()
