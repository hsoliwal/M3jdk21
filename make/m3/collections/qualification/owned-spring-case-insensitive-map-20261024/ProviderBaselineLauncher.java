/* Copyright 2026 Synexia. SPDX-License-Identifier: Apache-2.0 */
public final class ProviderBaselineLauncher {
    private ProviderBaselineLauncher() { }
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("exact provider classname required");
        SpringCaseInsensitiveMapProtocolProof.main(new String[]{args[0], "SpringCaseInsensitiveMapHookCases"});
        SpringCaseInsensitiveMapEntryCompatibilityObserver.main(args);
    }
}
