# M3 CircularFifoQueue index-wrap refinement — receiver proof

This qualification receives the canonical Synexia recipe
`m3-circular-fifo-index-wrap-20261018` into M3JDK21.

- preimage SHA-256: `71c4a3a325a99526f67228f6f34429b6f21b27d0bbfa8a5a3e5799442e8e9514`
- postimage SHA-256: `210697cba0058ec6c3ae9f53439f633773f11757c7b68369082e77a533f44650`
- Synexia recipe PR: `hsoliwal/com.synexia#12234`
- recipe head: `90dc3f9fd1f57748a0606e8a25e01e29aaba10fa`

The only production-source change is logical-to-physical index translation in
`get(int)`. The queue remains arbitrary-capacity, subclassable and
serialization-compatible. Existing Apache provider differential tests are
rerun unchanged; the checked JNI mask projection lane is rerun as well.

Problem taxonomy: circular array / ring-buffer coordinate translation.
LeetCode 622 and GeeksforGeeks circular queue are discovery/taxonomy sources
only. HackerRank Queue Using Two Stacks is recorded as a representation no-fit.
