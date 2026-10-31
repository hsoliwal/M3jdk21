# SPDX-License-Identifier: Apache-2.0
"""Refuse regression to incomplete platform claims and Windows checkout errors."""
from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[2]
WORKFLOW = ROOT / ".github/workflows/m3-jni-common.yml"
NATIVE = ROOT / "m3/native/CMakeLists.txt"


class NativePlatformWorkflowTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.workflow = WORKFLOW.read_text(encoding="utf-8")
        cls.cmake = NATIVE.read_text(encoding="utf-8")

    def test_windows_longpaths_precedes_first_checkout(self):
        host = self.workflow.split("  host-contract:", 1)[1].split(
            "  android-ndk-compile:", 1)[0]
        self.assertIn("if: runner.os == 'Windows'", host)
        self.assertIn("shell: pwsh", host)
        self.assertIn("git config --global core.longpaths true", host)
        self.assertLess(host.index("git config --global core.longpaths true"),
                        host.index("uses: actions/checkout@v4"))

    def test_host_matrix_includes_both_mac_architectures_and_linux_arm64(self):
        host = self.workflow.split("  host-contract:", 1)[1].split(
            "  android-ndk-compile:", 1)[0]
        self.assertIn("ubuntu-24.04-arm", host)
        self.assertIn("windows-2022", host)
        self.assertIn("macos-15-intel", host)
        self.assertIn("macos-15", host)
        self.assertIn("ctest --test-dir", host)

    def test_android_requires_actual_ndk_and_never_claims_vm_execution(self):
        android = self.workflow.split("  android-ndk-compile:", 1)[1].split(
            "  ios-arm64-compile:", 1)[0]
        self.assertIn("android.toolchain.cmake", android)
        self.assertIn("ANDROID_ABI=arm64-v8a", android)
        self.assertIn("ANDROID_PLATFORM=android-24", android)
        self.assertIn("runtime/device JNI tests remain pending", android)

    def test_ios_uses_xcode_sdk_and_existing_compile_only_cmake_owner(self):
        ios = self.workflow.split("  ios-arm64-compile:", 1)[1]
        self.assertIn("runs-on: macos-15", ios)
        self.assertIn("xcrun --sdk iphoneos --show-sdk-path", ios)
        self.assertIn("-DCMAKE_SYSTEM_NAME=iOS", ios)
        self.assertIn("-DCMAKE_OSX_ARCHITECTURES=arm64", ios)
        self.assertIn("-DCMAKE_TRY_COMPILE_TARGET_TYPE=STATIC_LIBRARY", ios)
        self.assertIn("-DBUILD_TESTING=OFF", ios)
        self.assertIn("M3_JNI_INCLUDE_DIRS", ios)
        self.assertIn("No iOS JVM", ios)
        self.assertIn("m3_jni_existing_owners OBJECT", self.cmake)
        self.assertIn("elseif(M3_JNI_INCLUDE_DIRS)", self.cmake)
        self.assertIn("if(NOT CMAKE_CROSSCOMPILING)", self.cmake)

    def test_portability_is_not_a_runtime_release_claim(self):
        readme = (ROOT / "m3/native/README.md").read_text(encoding="utf-8")
        self.assertIn("**not** an iOS runtime", readme)
        self.assertIn("**before**", readme)
        self.assertIn("Android/iOS product qualification", readme)


if __name__ == "__main__":
    unittest.main()
