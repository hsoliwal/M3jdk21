# Running the proofs from this record

The proof harnesses are stored as `*.java.txt` so the Java source gates of this repository do not read them as
product or test sources. Copy the folder of one leaf to a scratch directory, remove the `.txt` suffix from the
harness files, and run its `run_proof.sh` as its header says. The scripts name a stock JDK 21 through
`PROOF_JAVA_HOME` (default `C:/Program Files/Java/jdk-21`).
