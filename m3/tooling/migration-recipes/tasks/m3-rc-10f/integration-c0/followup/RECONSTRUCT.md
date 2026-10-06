This flat 41-file review mirror is **NOT MAVEN-READY**: it omits two exact significant-trailing-whitespace fixtures. The complete qualified 43-file project is in the companion archive. From this task directory, the portable folder is `../../../../verification/ci8891-20261006/current-reconciliation/integration-c0/followup`.

```sh
python3 ../../../../verification/ci8891-20261006/current-reconciliation/integration-c0/followup/reconstruct.py --portable ../../../../verification/ci8891-20261006/current-reconciliation/integration-c0/followup --output /path/to/fresh-extraction
/path/to/admitted-maven/bin/mvn -o -B -ntp -Dmaven.repo.local=/path/to/admitted-cache -f /path/to/fresh-extraction/fc0/project/pom.xml test
```

PROJECT-READY.json pins all 43 logical files and the two archive-only fixtures. Their exact source hashes are `5ca191f85c48be0ffec139b94d0ed91e0ac58be73686773241255f651799ec4b` (lane28 verifier preimage) and `ff2c31f57aac81a7049612557a3757781927563bd2d7a7b598aa62a116e1d978` (TQ verifier preimage). This document is an unqualified publication envelope outside the project census. Reconstruction preserves source bytes and receipt paths; it does not turn later Maven execution into an original receipt.
