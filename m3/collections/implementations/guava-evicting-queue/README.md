M3EvictingQueue is one complete mapped Guava33.4.8-jre queue body in an independent namespace.
Use com.m3.collections.guava.M3EvictingQueue.create(maxSize).
Its final/private-factory class owns a dynamic Object[] ring with primitive offsets/state
and bulk masks. It retains no provider implementation parent, delegate or stored entry nodes.
The owner links only JDK APIs; the module POM declares no Guava production dependency.

The mapped class has its own nominal and serialized identity. It implements Queue and
Serializable, with the qualified Guava operation/signature mapping; it is not a Guava concrete subtype.
The separate qualification packet holds the frozen provider/reference protocol, public-surface
and optional existing-mask Java/JNI projection proofs. Maven packaging and hosted CI are pending.
The larger five-library/native-operation scope remains unfinished.

Java21 debug information and all-lint warnings-as-errors are configured.
META-INF/LICENSE.txt, NOTICE.txt and THIRD_PARTY_NOTICES.md retain the applicable upstream material.
Build outputs route through m3.module.output.

Private serialization uses the owned V2 sentinel-array format. Capacity/count and exact image shape are validated; ObjectInputStream applies its real array filter before a defensive clone separates private storage. The earlier unpublished V1 format is not a compatibility target. See qualification/serialization-review for actual old failure and new passing witnesses.
