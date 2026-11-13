This flat 41-file review mirror is **NOT MAVEN-READY**: it omits two exact significant-trailing-whitespace fixtures. The complete qualified 43-file project is in the companion archive. From this task directory, the portable folder is `../../../../verification/ci8891-20261006/current-reconciliation/integration-c0/followup`.

```sh
python3 ../../../../verification/ci8891-20261006/current-reconciliation/integration-c0/followup/reconstruct.py --portable ../../../../verification/ci8891-20261006/current-reconciliation/integration-c0/followup --output /path/to/fresh-extraction
/path/to/admitted-maven/bin/mvn -o -B -ntp -Dmaven.repo.local=/path/to/admitted-cache -f /path/to/fresh-extraction/fc0/project/pom.xml test
```

PROJECT-READY.json pins all 43 logical files and the two archive-only fixtures. Their exact source hashes are `c626a962c87b195dd31f4b0976b39981261f7c3a612050f175d29a774190fa57` (lane28 verifier preimage) and `b70271e08910aedb8e512d38ef67c8dc041de522027e6a11abe96e3cfe825617` (TQ verifier preimage). This document is an unqualified publication envelope outside the project census. Reconstruction preserves source bytes and receipt paths; it does not turn later Maven execution into an original receipt.
