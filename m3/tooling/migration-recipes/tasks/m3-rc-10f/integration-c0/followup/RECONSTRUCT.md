This flat 41-file review mirror is **NOT MAVEN-READY**: it omits two exact significant-trailing-whitespace fixtures. The complete qualified 43-file project is in the companion archive. From this task directory, the portable folder is `../../../../verification/ci8891-20261006/current-reconciliation/integration-c0/followup`.

```sh
python3 ../../../../verification/ci8891-20261006/current-reconciliation/integration-c0/followup/reconstruct.py --portable ../../../../verification/ci8891-20261006/current-reconciliation/integration-c0/followup --output /path/to/fresh-extraction
/path/to/admitted-maven/bin/mvn -o -B -ntp -Dmaven.repo.local=/path/to/admitted-cache -f /path/to/fresh-extraction/fc0/project/pom.xml test
```

PROJECT-READY.json pins all 43 logical files and the two archive-only fixtures. Their exact source hashes are `d56788db191b5e3361d3e35e7f07c4395b38aec4b7c0a07dcd620620f4c4687a` (lane28 verifier preimage) and `269cd10606a9de7629ec0d1f4dccce2866644f979c06958d808e107b6c9188bb` (TQ verifier preimage). This document is an unqualified publication envelope outside the project census. Reconstruction preserves source bytes and receipt paths; it does not turn later Maven execution into an original receipt.
