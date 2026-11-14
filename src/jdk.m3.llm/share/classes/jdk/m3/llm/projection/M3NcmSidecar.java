// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.projection;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Immutable learned-code evidence bound to exact source/context and a supplied hierarchy.
 * No text, latent tensor, codebook, model or native pointer is retained. The caller is
 * responsible for establishing the provenance of supplied latent/code evidence. These
 * lossy codes never establish exact text equality or semantic truth.
 */
public final class M3NcmSidecar {
    private static final int MAGIC=0x4e434d53, VERSION=1, HEADER_BYTES=112;
    private static final int MAX_BYTES=64*1024*1024;
    private static final AtomicLong NEXT_STORAGE=new AtomicLong();
    private static final String STORAGE_NAMESPACE=UUID.randomUUID().toString();
    private final M3SemanticIdentity identity;
    private final String hierarchyRoot,rootHash,storageKey;
    private final int batch,totalCodes;
    private final int[] offsets,vocabulary,codes;

    private M3NcmSidecar(M3SemanticIdentity identity,M3NcmHierarchy hierarchy,
                             int[] ownedCodes,int batch) {
        this.identity=Objects.requireNonNull(identity,"identity");
        Objects.requireNonNull(hierarchy,"hierarchy");
        this.totalCodes=hierarchy.totalCodes();
        if (batch <= 0 || (long)batch*totalCodes != ownedCodes.length
                || (long)HEADER_BYTES+4L*ownedCodes.length > MAX_BYTES)
            throw new IllegalArgumentException("NCM sidecar shape or byte budget");
        this.batch=batch;
        this.offsets=hierarchy.prefixOffsets();
        this.vocabulary=new int[hierarchy.levelCount()];
        for (int level=0;level<vocabulary.length;level++) vocabulary[level]=hierarchy.vocabularySize(level);
        for (int row=0;row<batch;row++) for (int level=0;level<vocabulary.length;level++)
            for (int pos=offsets[level];pos<offsets[level+1];pos++) {
                int code=ownedCodes[row*totalCodes+pos];
                if (code < 0 || code >= vocabulary[level]) throw new IllegalArgumentException("level-local code ID");
            }
        codes=ownedCodes;
        hierarchyRoot=hierarchy.fingerprint();
        var hash=M3SemanticIdentity.hasher("M3_NCM_SIDECAR_V1")
                .text(identity.rootHash()).text(hierarchyRoot).integer(batch).integer(codes.length);
        for (int code:codes) hash.integer(code);
        rootHash=hash.finish();
        // Different copies of equal codes own different physical arrays. Budget each copy;
        // multiple facet views of this same immutable instance share this storage claim.
        long storage=NEXT_STORAGE.updateAndGet(value->Math.incrementExact(value));
        storageKey="mindex-ncm-sidecar-v1:"+STORAGE_NAMESPACE+":"+storage+":"+rootHash;
    }

    public static M3NcmSidecar of(M3SemanticIdentity identity,M3NcmHierarchy hierarchy,
                                      int[] codes,int batch) {
        Objects.requireNonNull(codes,"codes");
        if ((long)HEADER_BYTES+4L*codes.length > MAX_BYTES)
            throw new IllegalArgumentException("NCM sidecar byte budget");
        return new M3NcmSidecar(identity,hierarchy,codes.clone(),batch);
    }

    public M3SemanticIdentity identity() { return identity; }
    public String hierarchyRoot() { return hierarchyRoot; }
    public String rootHash() { return rootHash; }
    public String storageKey() { return storageKey; }
    public int batch() { return batch; }
    public int[] codes() { return codes.clone(); }
    public int[] prefixOffsets() { return offsets.clone(); }

    /** Conservative per-instance budget: exact primitive lanes plus 4KiB metadata allowance. */
    public long retainedBytes() { return 4096L+4L*(codes.length+offsets.length+vocabulary.length); }

    public int codeAt(int row,int level,int position) {
        Objects.checkIndex(row,batch);
        Objects.checkIndex(level,vocabulary.length);
        Objects.checkIndex(position,offsets[level+1]-offsets[level]);
        return codes[row*totalCodes+offsets[level]+position];
    }

    public boolean matches(M3SemanticIdentity source,M3NcmHierarchy hierarchy) {
        return identity.rootHash().equals(Objects.requireNonNull(source,"source").rootHash())
                && hierarchyRoot.equals(Objects.requireNonNull(hierarchy,"hierarchy").fingerprint());
    }

    public float[] decode(M3NcmHierarchy hierarchy) {
        if (!hierarchyRoot.equals(Objects.requireNonNull(hierarchy,"hierarchy").fingerprint()))
            throw new IllegalArgumentException("NCM hierarchy mismatch");
        return hierarchy.decode(codes,batch);
    }

    public byte[] toBytes() { return toBytes(MAX_BYTES); }

    /** Versioned, big-endian data-only envelope with bound identity and integrity digest. */
    public byte[] toBytes(int maxBytes) {
        int size=Math.toIntExact(HEADER_BYTES+4L*codes.length);
        if (maxBytes < size) throw new IllegalArgumentException("NCM sidecar serialization budget");
        ByteBuffer out=ByteBuffer.allocate(size).order(ByteOrder.BIG_ENDIAN);
        out.putInt(MAGIC).putInt(VERSION)
                .put(HexFormat.of().parseHex(identity.rootHash()))
                .put(HexFormat.of().parseHex(hierarchyRoot)).putInt(batch).putInt(codes.length)
                .put(HexFormat.of().parseHex(rootHash));
        for (int code:codes) out.putInt(code);
        return out.array();
    }

    /** Refuses version, owner/model, hierarchy, shape, code or integrity mismatch before returning. */
    public static M3NcmSidecar fromBytes(M3SemanticIdentity identity,
                                             M3NcmHierarchy hierarchy,byte[] bytes) {
        Objects.requireNonNull(identity,"identity");
        Objects.requireNonNull(hierarchy,"hierarchy");
        Objects.requireNonNull(bytes,"bytes");
        if (bytes.length < HEADER_BYTES || bytes.length > MAX_BYTES)
            throw new IllegalArgumentException("NCM sidecar envelope length");
        ByteBuffer in=ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
        if (in.getInt() != MAGIC || in.getInt() != VERSION)
            throw new IllegalArgumentException("NCM sidecar envelope version");
        String sourceRoot=readRoot(in),hierarchyRoot=readRoot(in);
        int batch=in.getInt(),count=in.getInt();
        String expected=readRoot(in);
        if (!identity.rootHash().equals(sourceRoot) || !hierarchy.fingerprint().equals(hierarchyRoot)
                || batch <= 0 || count < 0 || (long)batch*hierarchy.totalCodes() != count
                || (long)HEADER_BYTES+4L*count != bytes.length)
            throw new IllegalArgumentException("NCM sidecar owner/model/hierarchy or shape mismatch");
        int[] codes=new int[count];
        for (int i=0;i<count;i++) codes[i]=in.getInt();
        M3NcmSidecar result=new M3NcmSidecar(identity,hierarchy,codes,batch);
        if (!result.rootHash.equals(expected)) throw new IllegalArgumentException("NCM sidecar integrity mismatch");
        return result;
    }

    private static String readRoot(ByteBuffer in) {
        byte[] root=new byte[32]; in.get(root); return HexFormat.of().formatHex(root);
    }
}
