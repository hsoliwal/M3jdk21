# Owned MinMaxPriorityQueue attribution

The complete min-max array heap, public Builder/factories, dual-ended operations and movement-aware iterator are adapted from Google Guava33.4.8, commit f06690fa3e874f65515e8fd338a74d636e2c792f, owner blob76fecc6e32c687c3005ce988c2823ee4218e5511, Copyright2010 The Guava Authors, Apache-2.0. The original header remains in the owned source. Selected comparator, precondition and overflow atoms retain applicable2007-2011 Guava author attribution. The unmodified upstream Apache-2.0 license remains donors/guava/LICENSE.

Modifications2026 Hitesh Soliwal and contributors: com.m3.collections.guava.M3MinMaxPriorityQueue relocation; qualified existing M3LinkedList/M3ArrayList private array stores; operation-temporary Object[2] movement results; private owned comparator/validation atoms. No OpenJDK GPL implementation body was copied. JDK abstract/interface defaults remain runtime scaffold.

Existing first-party M3LinkedList/M3ArrayList and their qualified core dependency retain their existing Apache-2.0 headers and are reused in place. Caller Guava Ordering is an explicit API callback dependency; the queue stores payloads in its owned Object[] and the private buffers store references in M3 arrays, with no provider collection delegates.

The public comparator() return contract is Comparator. Inherited extra Guava Ordering collection factories on private Ordering subclasses are explicitly outside this queue ownership claim and fixture surface; they are never invoked by this owner. Own helper class/wire identities are scoped separately from required caller dispatch and comparator graph behavior.
