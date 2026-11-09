// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.text;

import java.security.Permission;

/** Run in an isolated Java21 process: denied optional loading must not poison parsing. */
@SuppressWarnings("removal")
public final class CsvDeniedNativeContract {
    private CsvDeniedNativeContract() { }
    public static void main(String[] args) {
        String source = "a,\"b\"\"c\"\n";
        String baseline = CsvToolsBefore.serialize(CsvToolsBefore.parse(source, ',',
                CsvToolsBefore.Limits.defaults()), ',', "\n", 100);
        System.setSecurityManager(new SecurityManager() {
            @Override public void checkPermission(Permission permission) { }
            @Override public void checkLink(String library) {
                throw new SecurityException("native loading denied by CSV contract policy");
            }
        });
        for (int i = 0; i < 2; i++) {
            String actual = CsvTools.serialize(CsvTools.parse(source, ',', CsvTools.Limits.defaults()),
                    ',', "\n", 100);
            if (!baseline.equals(actual)) throw new AssertionError("denied load changed parser output");
        }
        if (NativeLineOps.available() || NativeLineOps.structuralAvailable())
            throw new AssertionError("denied optional library reported available");
        System.out.println("CSV_DENIED_NATIVE_CONTRACT_PASS baseline_and_repeated_candidate_equal");
    }
}
