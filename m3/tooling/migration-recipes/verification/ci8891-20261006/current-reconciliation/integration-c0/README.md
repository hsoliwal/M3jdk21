# Fresh Ic0 source and proof evidence

This package reconstructs the complete 292-file Maven task at `ic0/project` and retains the actual fresh producer trials, raw logs, reports, source resources and exact output bodies. READBACK.json compares every extracted body with its selected original. SOURCE-CLOSURE.json maps original absolute input paths to included bodies or explicit exclusions. Old unavailable producer evidence has not been recreated or used as new admission.

The flat review mirror has 290 files; two significant-TAB before-image fixtures are archive-only. Run the checked reconstruction entry before Maven:

```sh
python3 /path/to/portable/reconstruct.py --portable /path/to/portable --output /path/to/fresh-extraction
/path/to/admitted-maven/bin/mvn -o -B -ntp -Dmaven.repo.local=/path/to/admitted-cache -f /path/to/fresh-extraction/ic0/project/pom.xml test
```

The entry reuses the unchanged archive owner, verifies every logical project body, and executes no proof gate. A subsequent Maven run is a new execution; the original absolute receipt paths remain unchanged. Use the exact admitted toolchain and dependency identities for comparison. Compiled classes, JDK/Maven/Python/cache bodies and settings are excluded under the unchanged owner and remain identity-only. This is source/evidence reconstruction, not a hermetic tool image or full receiving/JDK qualification.
