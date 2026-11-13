# M3IndexPathRank PR1540: exact-source qualification packet

Status: prepared; none of the eight planned actions has run. The source copies retain the exact existing Git blobs, including Apache-2.0 notices. No Java implementation, test or POM was generated or edited.

The candidate is the PR1540 owner at 4fd2abe76a3a63f5c363f69b4a5f979540a95bdb. The baseline is the exact incumbent owner blob6bc439b69f739608141acf6ee3800e5b5b0163c3. Both versions compile separately against the same two existing proof sources. Planned actions are baseline strict compile, SelfTest, WordWalkProof and javap, followed by the corresponding four candidate actions. Every command uses an absolute JDK21 tool path, owned K outputs and an isolated classpath. The global JVM directory lock must cover all eight actions.

SelfTest reports an observed rank-check count and900 randomized paths; the count is not a total assertion count and is not filled from unrelated historical receipts. WordWalkProof must report exactly6300 independent windows. Corresponding baseline/candidate stdout bytes and hashes must match, including javap -protected output for the owner and literal nested $Window class. Native job termination, input/tool freshness and storage/receipt gates remain required; exit0 alone is insufficient.

This is a bounded collection proving-ground packet. It does not execute the canonical Synexia hash-pinned recipe, JNI, Maven, a product image or a benchmark, and it does not advance the user's global active family or grant merge/promotion authority. The existing canonical recipe is separately tracked in com.synexia PR12545. Storage protection remains ACTIVE.
