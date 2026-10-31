# M3 CircularFifoQueue index-wrap refinement — receiver proof

This qualification receives the canonical Synexia recipe
`m3-circular-fifo-index-wrap-20261018` into M3JDK21.

- preimage SHA-256: `5ae11bbc515e745783cbf7d1050e95e2e40c7cd0f61044f8ef0a684a418f3663`
- postimage SHA-256: `3d337f1123ce5c8f977a395a37e515198a72fce967953d59e8a9a379463d8186`
- Synexia recipe PR: `hsoliwal/com.synexia#12234`
- recipe head: `90dc3f9fd1f57748a0606e8a25e01e29aaba10fa`

The only production-source change is logical-to-physical index translation in
`get(int)`. The queue remains arbitrary-capacity, subclassable and
serialization-compatible. Existing Apache provider differential tests are
rerun unchanged; the checked JNI mask projection lane is rerun as well.

Problem taxonomy: circular array / ring-buffer coordinate translation.
LeetCode 622 and GeeksforGeeks circular queue are discovery/taxonomy sources
only. HackerRank Queue Using Two Stacks is recorded as a representation no-fit.
