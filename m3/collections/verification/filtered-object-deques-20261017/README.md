# M3JDK21 filtered object Deque receiver

Exact first-party Synexia source: `synexia-primitives/src/main/java/com/synexia/primitives/M3FilteredDeques.java`.
Product receiver: `m3/collections/src/main/java/com/m3/collections/M3FilteredDeques.java`.
Source seals: `4861ecd5be1718dea49cb97daf1f0bfe6d057e762546e4143a598ba1b08b64ed` to `6342d1acb4f406dc5b3c086d2db18966d7a5fc7f2b2a14c680ca520133165f0a`.
The class implements JDK Deque<E>, Queue<E> and stack-role push/pop
while reusing physical backing owners and preserving hidden rows.

Run `bash m3/collections/verification/filtered-object-deques-20261017/verify.sh`
for direct Java21 verification. This is not a java.base replacement, matched
JDK build, OpenRewrite SDK or full Maven reactor gate. JNI symbols unchanged.

Canonical GitHub source: [hsoliwal/com.synexia PR #12182](https://github.com/hsoliwal/com.synexia/pull/12174), SHA `3307c77ae6ae6048d1665b27cddf1503fe3a1752`.
