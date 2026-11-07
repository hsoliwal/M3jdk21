// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.indexstring.type;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;
import jdk.m3.intern.M3InternSpace;

/**
 * Append-only canonical arena for Java class metadata.
 *
 * <p>This mirrors the MIndexString/MIndexAST geometry: tiny external handles, pooled immutable text,
 * primitive columns, primitive relationship arenas and exact collision verification. No
 * {@code Class<?>}, {@code Method}, {@code Field}, collection, map or per-member descriptor object
 * is retained after admission.</p>
 */
public final class MIndexClassPool {
  private static final int MAX_ARRAY = Integer.MAX_VALUE - 8;
  private static final double LOAD = 0.65d;
  private static final int MAX_TABLE_CAPACITY = 1 << 30;
  private static final long HASH_SEED = 0x9e3779b97f4a7c15L;
  private static final MIndexClassKind[] CLASS_KINDS = MIndexClassKind.values();
  private static final MIndexMemberKind[] MEMBER_KINDS = MIndexMemberKind.values();

  private final M3InternSpace textSpace;
  private final int languageId;

  private volatile int[] namespaceIds;
  private volatile byte[] kinds;
  private volatile int[] modifiers;
  private volatile int[] binaryNameHandles;
  private volatile int[] packageNameHandles;
  private volatile int[] moduleNameHandles;
  private volatile int[] superHandles;
  private volatile int[] interfaceStarts;
  private volatile int[] interfaceCounts;
  private volatile int[] memberStarts;
  private volatile int[] memberCounts;
  private volatile long[] identityHashes;
  private volatile long[] exactHashes;
  private volatile long[] structuralHashes;
  private volatile long[] logicHashes;
  private volatile long[] simHashes;
  private volatile int[] slots;
  private volatile int size;

  private volatile int[] interfaceArena;
  private volatile int interfaceSize;

  // Logical rows preserve owner identity and canonical member order. Payload IDs are internal.
  private volatile int[] memberOwners;
  private volatile int[] memberPayloadIds;
  // Indexed exact tuples share these columns and the four precomputed hashes.
  private volatile int[] memberPayloadSlots;
  private volatile int memberPayloadSize;
  private volatile byte[] memberKinds;
  private volatile int[] memberNameHandles;
  private volatile int[] memberDescriptorHandles;
  private volatile int[] memberModifierBits;
  private volatile long[] memberExactHashes;
  private volatile long[] memberStructuralHashes;
  private volatile long[] memberLogicHashes;
  private volatile long[] memberSimHashes;
  private volatile int memberSize;

  /** A pool over its own fresh text space. */
  public MIndexClassPool(int languageId) {
    this(new M3InternSpace(), languageId);
  }

  /** A pool over a caller-owned text space; text handles are atom ids of that space. */
  public MIndexClassPool(M3InternSpace textSpace, int languageId) {
    this.textSpace = Objects.requireNonNull(textSpace, "textSpace");
    if (languageId < 0) throw new IllegalArgumentException("negative languageId");
    this.languageId = languageId;

    int initial = 16;
    namespaceIds = new int[initial];
    kinds = new byte[initial];
    modifiers = new int[initial];
    binaryNameHandles = new int[initial];
    packageNameHandles = new int[initial];
    moduleNameHandles = new int[initial];
    superHandles = new int[initial];
    interfaceStarts = new int[initial];
    interfaceCounts = new int[initial];
    memberStarts = new int[initial];
    memberCounts = new int[initial];
    identityHashes = new long[initial];
    exactHashes = new long[initial];
    structuralHashes = new long[initial];
    logicHashes = new long[initial];
    simHashes = new long[initial];
    slots = new int[32];

    interfaceArena = new int[32];

    memberOwners = new int[32];
    memberPayloadIds = new int[32];
    memberPayloadSlots = new int[64];
    memberKinds = new byte[32];
    memberNameHandles = new int[32];
    memberDescriptorHandles = new int[32];
    memberModifierBits = new int[32];
    memberExactHashes = new long[32];
    memberStructuralHashes = new long[32];
    memberLogicHashes = new long[32];
    memberSimHashes = new long[32];
  }

  public M3InternSpace textSpace() { return textSpace; }
  public int languageId() { return languageId; }
  public int size() { return size; }
  public int retainedInterfaceHandleCount() { return interfaceSize; }
  public int retainedMemberCount() { return memberSize; }
  /** Distinct interned strings of this pool's text space. */
  public long retainedTextAtomCount() { return textSpace.atoms().size(); }

  public MIndexClass value(int handle) {
    checkHandle(handle);
    return new MIndexClass(this, handle);
  }

  /**
   * Canonically admits a declared type.
   *
   * <p>Hierarchy references must already exist in this pool. That single rule makes the retained
   * hierarchy a forward-built DAG and prevents cycles without retaining a graph object per class.</p>
   */
  public synchronized MIndexClass intern(MIndexClassSpec spec) {
    Objects.requireNonNull(spec, "spec");
    validateReferences(spec);

    int binaryHandle = textHandle(spec.binaryName());
    int packageHandle = textHandle(spec.packageName());
    int moduleHandle = textHandle(spec.moduleName());
    long identity = identityHash(spec.namespaceId(), moduleHandle, binaryHandle);

    int existing = findIdentity(spec.namespaceId(), moduleHandle, binaryHandle, identity);
    if (existing != 0) {
      requireSameDefinition(existing, spec, packageHandle);
      return new MIndexClass(this, existing);
    }

    int[] interfaces = spec.interfaceHandles();
    MIndexClassMemberSpec[] members = spec.members();
    reserveClasses(size + 2);
    reserveTable(size + 1);
    reserveInterfaces(Math.addExact(interfaceSize, interfaces.length));
    reserveMembers(Math.addExact(memberSize, members.length));

    int handle = size + 1;
    int interfaceStart = interfaceSize;
    System.arraycopy(interfaces, 0, interfaceArena, interfaceSize, interfaces.length);
    interfaceSize += interfaces.length;

    int memberStart = memberSize;
    long memberExactFold = HASH_SEED ^ 0x4d454d4245524558L;
    long memberStructuralFold = HASH_SEED ^ 0x4d454d4245525354L;
    long memberLogicFold = HASH_SEED ^ 0x4d454d4245524c47L;
    long memberSimFold = 0L;
    int memberOrdinal = 0;
    for (MIndexClassMemberSpec member : members) {
      int nameHandle = textHandle(member.name());
      int descriptorHandle = textHandle(member.descriptor());
      int payload = internMemberPayload(member, nameHandle, descriptorHandle);
      int index = memberStart + memberOrdinal;
      memberOwners[index] = handle;
      memberPayloadIds[index] = payload;
      memberExactFold = combine(memberExactFold, memberExactHashes[payload]);
      memberStructuralFold = combine(memberStructuralFold, memberStructuralHashes[payload]);
      memberLogicFold = combine(memberLogicFold, memberLogicHashes[payload]);
      memberSimFold ^= Long.rotateLeft(memberSimHashes[payload], memberOrdinal & 63);
      memberOrdinal++;
    }

    // Publish complete member rows before publishing their class handle below.
    memberSize = memberStart + members.length;
    namespaceIds[handle] = spec.namespaceId();
    kinds[handle] = (byte) spec.kind().ordinal();
    modifiers[handle] = spec.modifiers();
    binaryNameHandles[handle] = binaryHandle;
    packageNameHandles[handle] = packageHandle;
    moduleNameHandles[handle] = moduleHandle;
    superHandles[handle] = spec.superClassHandle();
    interfaceStarts[handle] = interfaceStart;
    interfaceCounts[handle] = interfaces.length;
    memberStarts[handle] = memberStart;
    memberCounts[handle] = members.length;
    identityHashes[handle] = identity;

    long relationExact = relationFold(spec.superClassHandle(), interfaces, HashKind.IDENTITY);
    long relationStructural = relationFold(spec.superClassHandle(), interfaces, HashKind.STRUCTURAL);
    long relationLogic = relationFold(spec.superClassHandle(), interfaces, HashKind.LOGIC);

    exactHashes[handle] =
        avalanche(combine(
            combine(combine(identity, spec.kind().ordinal()), spec.modifiers()),
            combine(combine(textFeature(packageHandle), relationExact), memberExactFold)));
    structuralHashes[handle] =
        avalanche(combine(
            combine(spec.kind().ordinal(), spec.modifiers()),
            combine(relationStructural, memberStructuralFold)));
    logicHashes[handle] =
        avalanche(combine(
            combine(spec.kind().ordinal(), spec.modifiers()),
            combine(relationLogic, memberLogicFold)));
    simHashes[handle] =
        avalanche(simFeature(
            spec.kind().ordinal(), spec.modifiers(), relationStructural, memberSimFold));

    int mask = slots.length - 1;
    int slot = mixToInt(identity) & mask;
    while (slots[slot] != 0) slot = (slot + 1) & mask;
    slots[slot] = handle;

    // Publish last. Readers never observe a handle before all of its primitive columns are filled.
    size = handle;
    return new MIndexClass(this, handle);
  }

  /** Returns an existing class without creating one, or {@code null} on a miss. */
  public synchronized MIndexClass find(int namespaceId, String moduleName, String binaryName) {
    if (namespaceId < 0) throw new IllegalArgumentException("negative namespaceId");
    Objects.requireNonNull(moduleName, "moduleName");
    Objects.requireNonNull(binaryName, "binaryName");
    for (int handle = 1; handle <= size; handle++) {
      if (namespaceIds[handle] == namespaceId
          && textEquals(moduleNameHandles[handle], moduleName)
          && textEquals(binaryNameHandles[handle], binaryName)) {
        return new MIndexClass(this, handle);
      }
    }
    return null;
  }

  public MIndexClassSnapshot snapshot() {
    return MIndexClassSnapshot.of(this);
  }

  /**
   * Occupied primitive data bytes, excluding lookup tables, spare capacity, array headers and text.
   * Logical owner rows and retained immutable member payloads are counted separately.
   */
  public long primitivePayloadBytes() {
    long classes = size;
    long members = memberSize;
    return classes * (
            Integer.BYTES * 10L
                + Byte.BYTES
                + Long.BYTES * 5L)
        + interfaceSize * (long) Integer.BYTES
        + members * Integer.BYTES * 2L
        + memberPayloadSize * (
            Integer.BYTES * 3L
                + Byte.BYTES
                + Long.BYTES * 4L);
  }

  int namespaceId(int handle) { checkHandle(handle); return namespaceIds[handle]; }
  MIndexClassKind kind(int handle) {
    checkHandle(handle);
    return CLASS_KINDS[Byte.toUnsignedInt(kinds[handle])];
  }
  int modifiers(int handle) { checkHandle(handle); return modifiers[handle]; }
  String binaryName(int handle) { checkHandle(handle); return text(binaryNameHandles[handle]); }
  String packageName(int handle) { checkHandle(handle); return text(packageNameHandles[handle]); }
  String moduleName(int handle) { checkHandle(handle); return text(moduleNameHandles[handle]); }
  int superClassHandle(int handle) { checkHandle(handle); return superHandles[handle]; }
  int interfaceCount(int handle) { checkHandle(handle); return interfaceCounts[handle]; }
  int interfaceHandle(int handle, int ordinal) {
    int count = interfaceCount(handle);
    Objects.checkIndex(ordinal, count);
    return interfaceArena[interfaceStarts[handle] + ordinal];
  }
  int[] interfaceHandles(int handle) {
    int count = interfaceCount(handle);
    int start = interfaceStarts[handle];
    return Arrays.copyOfRange(interfaceArena, start, start + count);
  }
  int memberCount(int handle) { checkHandle(handle); return memberCounts[handle]; }
  MIndexClassMember member(int handle, int ordinal) {
    int count = memberCount(handle);
    Objects.checkIndex(ordinal, count);
    return new MIndexClassMember(this, memberStarts[handle] + ordinal);
  }

  int memberOwner(int index) { checkMember(index); return memberOwners[index]; }
  MIndexMemberKind memberKind(int index) {
    checkMember(index);
    return MEMBER_KINDS[Byte.toUnsignedInt(memberKinds[memberPayloadIds[index]])];
  }
  String memberName(int index) { checkMember(index); return text(memberNameHandles[memberPayloadIds[index]]); }
  String memberDescriptor(int index) {
    checkMember(index); return text(memberDescriptorHandles[memberPayloadIds[index]]);
  }
  int memberModifiers(int index) { checkMember(index); return memberModifierBits[memberPayloadIds[index]]; }
  long memberExactHash64(int index) { checkMember(index); return memberExactHashes[memberPayloadIds[index]]; }
  long memberStructuralHash64(int index) { checkMember(index); return memberStructuralHashes[memberPayloadIds[index]]; }
  long memberLogicHash64(int index) { checkMember(index); return memberLogicHashes[memberPayloadIds[index]]; }
  long memberSimHash64(int index) { checkMember(index); return memberSimHashes[memberPayloadIds[index]]; }

  long identityHash64(int handle) { checkHandle(handle); return identityHashes[handle]; }
  long exactHash64(int handle) { checkHandle(handle); return exactHashes[handle]; }
  long structuralHash64(int handle) { checkHandle(handle); return structuralHashes[handle]; }
  long logicHash64(int handle) { checkHandle(handle); return logicHashes[handle]; }
  long simHash64(int handle) { checkHandle(handle); return simHashes[handle]; }

  byte[] sha256(int handle) {
    checkHandle(handle);
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      putUtf8(digest, "MINDEX-CLASS-1");
      putInt(digest, namespaceIds[handle]);
      putUtf8(digest, textValue(moduleNameHandles[handle]));
      putUtf8(digest, textValue(binaryNameHandles[handle]));
      putUtf8(digest, textValue(packageNameHandles[handle]));
      putInt(digest, Byte.toUnsignedInt(kinds[handle]));
      putInt(digest, modifiers[handle]);
      int parent = superHandles[handle];
      putUtf8(digest, parent == 0 ? "" : identityText(parent));
      int interfaceCount = interfaceCounts[handle];
      putInt(digest, interfaceCount);
      int interfaceStart = interfaceStarts[handle];
      for (int i = 0; i < interfaceCount; i++) {
        putUtf8(digest, identityText(interfaceArena[interfaceStart + i]));
      }
      int count = memberCounts[handle];
      putInt(digest, count);
      int start = memberStarts[handle];
      for (int i = 0; i < count; i++) {
        int index = memberPayloadIds[start + i];
        putInt(digest, Byte.toUnsignedInt(memberKinds[index]));
        putUtf8(digest, textValue(memberNameHandles[index]));
        putUtf8(digest, textValue(memberDescriptorHandles[index]));
        putInt(digest, memberModifierBits[index]);
      }
      return digest.digest();
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException("SHA-256 unavailable", impossible);
    }
  }

  int directParentCount(int handle) {
    checkHandle(handle);
    return (superHandles[handle] == 0 ? 0 : 1) + interfaceCounts[handle];
  }

  int directParentHandle(int handle, int ordinal) {
    int count = directParentCount(handle);
    Objects.checkIndex(ordinal, count);
    int superHandle = superHandles[handle];
    if (superHandle != 0) {
      if (ordinal == 0) return superHandle;
      ordinal--;
    }
    return interfaceArena[interfaceStarts[handle] + ordinal];
  }

  private void validateReferences(MIndexClassSpec spec) {
    int current = size;
    int parent = spec.superClassHandle();
    if (parent > current) throw new IllegalArgumentException("unknown/future superclass handle " + parent);
    if (parent != 0) {
      MIndexClassKind parentKind = kind(parent);
      if (parentKind == MIndexClassKind.INTERFACE || parentKind == MIndexClassKind.ANNOTATION) {
        throw new IllegalArgumentException("superclass handle names an interface: " + parent);
      }
    }
    if ((spec.kind() == MIndexClassKind.INTERFACE || spec.kind() == MIndexClassKind.ANNOTATION)
        && parent != 0) {
      throw new IllegalArgumentException("interface/annotation cannot retain a class superclass");
    }
    for (int handle : spec.interfaceHandles()) {
      if (handle > current) throw new IllegalArgumentException("unknown/future interface handle " + handle);
      MIndexClassKind interfaceKind = kind(handle);
      if (interfaceKind != MIndexClassKind.INTERFACE
          && interfaceKind != MIndexClassKind.ANNOTATION) {
        throw new IllegalArgumentException("interface handle names a class: " + handle);
      }
      if (handle == parent && handle != 0) {
        throw new IllegalArgumentException("same direct type cannot be superclass and interface");
      }
    }
  }

  private int findIdentity(int namespaceId, int moduleHandle, int binaryHandle, long hash) {
    int[] table = slots;
    int mask = table.length - 1;
    int slot = mixToInt(hash) & mask;
    for (int probe = 0; probe < table.length; probe++) {
      int handle = table[slot];
      if (handle == 0) return 0;
      if (identityHashes[handle] == hash
          && namespaceIds[handle] == namespaceId
          && textHandleEquals(moduleNameHandles[handle], moduleHandle)
          && textHandleEquals(binaryNameHandles[handle], binaryHandle)) {
        return handle;
      }
      slot = (slot + 1) & mask;
    }
    return 0;
  }

  /** Called only while holding this pool's admission monitor. Hashes never establish equality. */
  private int internMemberPayload(MIndexClassMemberSpec member, int nameHandle, int descriptorHandle) {
    int kind = member.kind().ordinal();
    int bits = member.modifiers();
    long hash = memberPayloadHash(kind, nameHandle, descriptorHandle, bits);
    int[] table = memberPayloadSlots;
    int mask = table.length - 1;
    int slot = mixToInt(hash) & mask;
    for (int probes = 0; probes < table.length; probes++) {
      int encoded = table[slot];
      if (encoded == 0) break;
      int payload = encoded - 1;
      if (Byte.toUnsignedInt(memberKinds[payload]) == kind
          && memberNameHandles[payload] == nameHandle
          && memberDescriptorHandles[payload] == descriptorHandle
          && memberModifierBits[payload] == bits) return payload;
      slot = (slot + 1) & mask;
    }

    int payload = memberPayloadSize;
    int required = Math.addExact(payload, 1);
    reserveMemberPayloads(required);
    boolean indexed = reserveMemberPayloadTable(required);
    memberKinds[payload] = (byte) kind;
    memberNameHandles[payload] = nameHandle;
    memberDescriptorHandles[payload] = descriptorHandle;
    memberModifierBits[payload] = bits;
    long descriptorFeature = textFeature(descriptorHandle);
    long nameFeature = textFeature(nameHandle);
    memberExactHashes[payload] = combine(
        combine(combine(HASH_SEED, kind), bits), combine(nameFeature, descriptorFeature));
    memberStructuralHashes[payload] = combine(
        combine(HASH_SEED ^ 0x5354525543544d45L, kind), combine(bits, descriptorFeature));
    memberLogicHashes[payload] = combine(
        combine(HASH_SEED ^ 0x4c4f4749434d454dL, kind), combine(bits, descriptorFeature));
    memberSimHashes[payload] = simFeature(kind, bits, descriptorFeature, nameFeature);

    // Index saturation must not lower the incumbent member-arena capacity. Existing indexed
    // tuples remain reusable; later uncached tuples retain ordinary private payload rows.
    if (!indexed) {
      memberPayloadSize = required;
      return payload;
    }

    table = memberPayloadSlots;
    mask = table.length - 1;
    slot = mixToInt(hash) & mask;
    for (int probes = 0; probes < table.length; probes++) {
      if (table[slot] == 0) {
        table[slot] = required; // zero is empty; payload coordinates themselves are zero-based.
        memberPayloadSize = required;
        return payload;
      }
      slot = (slot + 1) & mask;
    }
    throw new IllegalStateException("member payload table capacity");
  }

  private static long memberPayloadHash(int kind, int name, int descriptor, int bits) {
    // Canonical text handles are pool-local coordinates, never a cross-pool/runtime identity.
    return avalanche(((long) name << Integer.SIZE) ^ Integer.toUnsignedLong(descriptor)
        ^ Long.rotateLeft(Integer.toUnsignedLong(bits), 17) ^ ((long) kind << 57));
  }

  private static int memberPayloadTableCapacity(int current, int required) {
    if (required > (int) (MAX_TABLE_CAPACITY * LOAD)) return 0;
    return required <= (int) (current * LOAD) ? current : tableCapacity(required);
  }

  private boolean reserveMemberPayloadTable(int required) {
    int capacity = memberPayloadTableCapacity(memberPayloadSlots.length, required);
    if (capacity == 0) return false;
    if (capacity == memberPayloadSlots.length) return true;
    int[] replacement = new int[capacity];
    int mask = replacement.length - 1;
    for (int payload = 0; payload < memberPayloadSize; payload++) {
      long hash = memberPayloadHash(Byte.toUnsignedInt(memberKinds[payload]),
          memberNameHandles[payload], memberDescriptorHandles[payload], memberModifierBits[payload]);
      int slot = mixToInt(hash) & mask;
      while (replacement[slot] != 0) slot = (slot + 1) & mask;
      replacement[slot] = payload + 1;
    }
    memberPayloadSlots = replacement;
    return true;
  }

  private void requireSameDefinition(int handle, MIndexClassSpec spec, int packageHandle) {
    if (kind(handle) != spec.kind()
        || modifiers[handle] != spec.modifiers()
        || !textHandleEquals(packageNameHandles[handle], packageHandle)
        || superHandles[handle] != spec.superClassHandle()
        || interfaceCounts[handle] != spec.interfaceCount()
        || memberCounts[handle] != spec.memberCount()) {
      throw conflicting(handle);
    }
    for (int i = 0; i < spec.interfaceCount(); i++) {
      if (interfaceHandle(handle, i) != spec.interfaceHandle(i)) throw conflicting(handle);
    }
    int start = memberStarts[handle];
    for (int i = 0; i < spec.memberCount(); i++) {
      MIndexClassMemberSpec expected = spec.member(i);
      int index = start + i;
      int payload = memberPayloadIds[index];
      if (memberKind(index) != expected.kind()
          || memberModifierBits[payload] != expected.modifiers()
          || !textEquals(memberNameHandles[payload], expected.name())
          || !textEquals(memberDescriptorHandles[payload], expected.descriptor())) {
        throw conflicting(handle);
      }
    }
  }

  private IllegalStateException conflicting(int handle) {
    return new IllegalStateException(
        "conflicting MIndexClass definition for " + identityText(handle));
  }

  private long identityHash(int namespaceId, int moduleHandle, int binaryHandle) {
    return avalanche(combine(
        combine(HASH_SEED ^ Integer.toUnsignedLong(namespaceId), textFeature(moduleHandle)),
        textFeature(binaryHandle)));
  }

  private long relationFold(int superHandle, int[] interfaces, HashKind hashKind) {
    long result = HASH_SEED ^ 0x52454c4154494f4eL;
    if (superHandle != 0) result = combine(result, hashKind.value(this, superHandle));
    for (int handle : interfaces) result = combine(result, hashKind.value(this, handle));
    return avalanche(result ^ interfaces.length);
  }

  private String text(int textHandle) {
    return textSpace.string(textHandle);
  }

  /** Atom ids of an M3InternSpace are dense (1..2^24), so they fit the int handle lanes. */
  private int textHandle(String value) {
    return Math.toIntExact(textSpace.internString(value));
  }

  private String textValue(int textHandle) {
    return textSpace.string(textHandle);
  }

  private boolean textEquals(int textHandle, String value) {
    return textSpace.string(textHandle).equals(value);
  }

  private boolean textHandleEquals(int left, int right) {
    return left == right || textValue(left).equals(textValue(right));
  }

  private long textFeature(int textHandle) {
    String value = textSpace.string(textHandle);
    long compact =
        ((long) value.hashCode() << Integer.SIZE)
            ^ Integer.toUnsignedLong(value.length());
    return avalanche(compact ^ 0x5445585446454154L);
  }

  private String identityText(int handle) {
    return namespaceIds[handle] + ":"
        + textValue(moduleNameHandles[handle]) + ":"
        + textValue(binaryNameHandles[handle]);
  }

  private static long simFeature(long a, long b, long c, long d) {
    long first = avalanche(a ^ 0x1111111111111111L);
    long second = avalanche(b ^ 0x2222222222222222L);
    long third = avalanche(c ^ 0x3333333333333333L);
    long fourth = avalanche(d ^ 0x4444444444444444L);
    // The incumbent four-vote rule sets ties to one: at least two of four bits are set.
    return (first & second) | (first & third) | (first & fourth)
        | (second & third) | (second & fourth) | (third & fourth);
  }

  private static long combine(long left, long right) {
    return avalanche(left ^ Long.rotateLeft(right + HASH_SEED, 17));
  }

  private static long avalanche(long value) {
    long z = value;
    z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
    z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
    return z ^ (z >>> 31);
  }

  private static int mixToInt(long value) {
    long z = avalanche(value);
    return (int) (z ^ (z >>> 32));
  }

  private static void putInt(MessageDigest digest, int value) {
    digest.update((byte) (value >>> 24));
    digest.update((byte) (value >>> 16));
    digest.update((byte) (value >>> 8));
    digest.update((byte) value);
  }

  private static void putUtf8(MessageDigest digest, String value) {
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    putInt(digest, bytes.length);
    digest.update(bytes);
  }

  private void reserveClasses(int required) {
    if (required <= namespaceIds.length) return;
    int next = grown(namespaceIds.length, required);
    namespaceIds = Arrays.copyOf(namespaceIds, next);
    kinds = Arrays.copyOf(kinds, next);
    modifiers = Arrays.copyOf(modifiers, next);
    binaryNameHandles = Arrays.copyOf(binaryNameHandles, next);
    packageNameHandles = Arrays.copyOf(packageNameHandles, next);
    moduleNameHandles = Arrays.copyOf(moduleNameHandles, next);
    superHandles = Arrays.copyOf(superHandles, next);
    interfaceStarts = Arrays.copyOf(interfaceStarts, next);
    interfaceCounts = Arrays.copyOf(interfaceCounts, next);
    memberStarts = Arrays.copyOf(memberStarts, next);
    memberCounts = Arrays.copyOf(memberCounts, next);
    identityHashes = Arrays.copyOf(identityHashes, next);
    exactHashes = Arrays.copyOf(exactHashes, next);
    structuralHashes = Arrays.copyOf(structuralHashes, next);
    logicHashes = Arrays.copyOf(logicHashes, next);
    simHashes = Arrays.copyOf(simHashes, next);
  }

  private void reserveInterfaces(int required) {
    if (required <= interfaceArena.length) return;
    interfaceArena = Arrays.copyOf(interfaceArena, grown(interfaceArena.length, required));
  }

  private void reserveMembers(int required) {
    if (required <= memberOwners.length) return;
    int next = grown(memberOwners.length, required);
    memberOwners = Arrays.copyOf(memberOwners, next);
    memberPayloadIds = Arrays.copyOf(memberPayloadIds, next);
  }

  private void reserveMemberPayloads(int required) {
    if (required <= memberKinds.length) return;
    int next = grown(memberKinds.length, required);
    memberKinds = Arrays.copyOf(memberKinds, next);
    memberNameHandles = Arrays.copyOf(memberNameHandles, next);
    memberDescriptorHandles = Arrays.copyOf(memberDescriptorHandles, next);
    memberModifierBits = Arrays.copyOf(memberModifierBits, next);
    memberExactHashes = Arrays.copyOf(memberExactHashes, next);
    memberStructuralHashes = Arrays.copyOf(memberStructuralHashes, next);
    memberLogicHashes = Arrays.copyOf(memberLogicHashes, next);
    memberSimHashes = Arrays.copyOf(memberSimHashes, next);
  }

  private void reserveTable(int expected) {
    if (expected <= (int) (slots.length * LOAD)) return;
    int capacity = tableCapacity(expected);
    int[] replacement = new int[capacity];
    int mask = capacity - 1;
    for (int handle = 1; handle <= size; handle++) {
      int slot = mixToInt(identityHashes[handle]) & mask;
      while (replacement[slot] != 0) slot = (slot + 1) & mask;
      replacement[slot] = handle;
    }
    slots = replacement;
  }

  private static int tableCapacity(int expected) {
    long required = Math.max(2L, (long) Math.ceil(Math.max(1, expected) / LOAD));
    int capacity = 2;
    while (capacity < required) {
      if (capacity >= MAX_TABLE_CAPACITY) throw new IllegalStateException("class table too large");
      capacity <<= 1;
    }
    return capacity;
  }

  private static int grown(int current, int required) {
    if (required < 0 || required > MAX_ARRAY) throw new IllegalStateException("class arena capacity");
    long next = Math.max(required, Math.max(2L, current + (current >>> 1) + 1L));
    return (int) Math.min(MAX_ARRAY, next);
  }

  private void checkHandle(int handle) {
    if (handle < 1 || handle > size) {
      throw new IndexOutOfBoundsException("MIndexClass handle " + handle);
    }
  }

  private void checkMember(int index) {
    if (index < 0 || index >= memberSize) {
      throw new IndexOutOfBoundsException("MIndexClass member " + index);
    }
  }

  private enum HashKind {
    IDENTITY {
      @Override long value(MIndexClassPool pool, int handle) {
        return pool.identityHashes[handle];
      }
    },
    STRUCTURAL {
      @Override long value(MIndexClassPool pool, int handle) {
        return pool.structuralHashes[handle];
      }
    },
    LOGIC {
      @Override long value(MIndexClassPool pool, int handle) {
        return pool.logicHashes[handle];
      }
    };

    abstract long value(MIndexClassPool pool, int handle);
  }
}
