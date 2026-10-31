// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted for the M3JDK21 M3FS runtime from the pinned Synexia donor.
package com.m3.fs;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Filesystem retention policy for reusable CPU/JNI/GPU primitive precomputes.
 *
 * <p>Profiles describe portable data geometry, not backend authority. A GPU provider may consume a
 * profile only after its own capability, parity and crossover gates pass.</p>
 */
public final class M3FsPrecomputeCatalog {
  public enum Retention {
    KEEP,
    KEEP_IF_REUSED
  }

  public record Profile(
      String id,
      Retention retention,
      Set<M3FsPrecomputeBundle.Role> requiredRoles,
      Set<M3FsPrecomputeBundle.Role> optionalRoles,
      List<String> acceleratorOperations) {
    public Profile {
      id = requireId(id);
      retention = Objects.requireNonNull(retention, "retention");
      requiredRoles = Set.copyOf(Objects.requireNonNull(requiredRoles, "requiredRoles"));
      optionalRoles = Set.copyOf(Objects.requireNonNull(optionalRoles, "optionalRoles"));
      acceleratorOperations =
          List.copyOf(Objects.requireNonNull(acceleratorOperations, "acceleratorOperations"));
      if (requiredRoles.isEmpty()) throw new IllegalArgumentException("requiredRoles");
      EnumSet<M3FsPrecomputeBundle.Role> overlap =
          EnumSet.noneOf(M3FsPrecomputeBundle.Role.class);
      overlap.addAll(requiredRoles);
      overlap.retainAll(optionalRoles);
      if (!overlap.isEmpty()) throw new IllegalArgumentException("role overlap");
    }

    public void requireCompatible(M3FsPrecomputeBundle bundle) {
      validateRoles(bundle, id, mask(requiredRoles), mask(optionalRoles), false);
    }
    public void requireStrictCompatible(M3FsPrecomputeBundle bundle) {
      validateRoles(bundle, id, mask(requiredRoles), mask(optionalRoles), true);
    }
  }

  /** Workload-level composition of durable profiles and process-local state. */
  public record Pipeline(
      String id,
      List<String> requiredProfiles,
      List<String> optionalProfiles,
      List<String> runtimeOnlyState) {
    public Pipeline {
      id = requireId(id);
      requiredProfiles =
          Objects.requireNonNull(requiredProfiles, "requiredProfiles").stream()
              .map(M3FsPrecomputeCatalog::requireId)
              .toList();
      optionalProfiles =
          Objects.requireNonNull(optionalProfiles, "optionalProfiles").stream()
              .map(M3FsPrecomputeCatalog::requireId)
              .toList();
      runtimeOnlyState =
          Objects.requireNonNull(runtimeOnlyState, "runtimeOnlyState").stream()
              .map(M3FsPrecomputeCatalog::requireId)
              .toList();
      if (requiredProfiles.isEmpty()) {
        throw new IllegalArgumentException("requiredProfiles");
      }
      java.util.HashSet<String> seen = new java.util.HashSet<>();
      for (String profile : requiredProfiles) {
        if (!seen.add(profile)) throw new IllegalArgumentException("duplicate profile");
        profileIndex(profile);
      }
      for (String profile : optionalProfiles) {
        if (!seen.add(profile)) throw new IllegalArgumentException("duplicate profile");
        profileIndex(profile);
      }
      if (runtimeOnlyState.size() != new java.util.HashSet<>(runtimeOnlyState).size()) {
        throw new IllegalArgumentException("duplicate runtime state");
      }
    }

    public List<Profile> required() {
      return requiredProfiles.stream().map(M3FsPrecomputeCatalog::byId).toList();
    }

    public List<Profile> optional() {
      return optionalProfiles.stream().map(M3FsPrecomputeCatalog::byId).toList();
    }
  }


  public record PipelineEntry(String profile, M3PrecomputeKey key) {
    public PipelineEntry {
      profile = requireId(profile); key = Objects.requireNonNull(key, "key"); profileIndex(profile);
    }
    // Public projections are rebuilt from columns; equality follows durable key identity.
    @Override public boolean equals(Object other) {
      return other instanceof PipelineEntry entry && profile.equals(entry.profile)
          && key.id().equals(entry.key.id());
    }
    @Override public int hashCode() { return 31 * profile.hashCode() + key.id().hashCode(); }
  }

  /** Canonical pipeline metadata: profile ordinals, byte offsets and packed encoded keys. */
  public static final class PipelineManifest {
    private static final int FORMAT = 1;
    private static final int MAX_BYTES = 1024 * 1024;
    private final int pipeline;
    private final int[] profiles;
    private final int[] keyOffsets;
    private final byte[] keyBytes;
    private final byte[] canonical;
    private final byte[] id;

    public PipelineManifest(String pipelineId, List<PipelineEntry> entries) {
      pipeline = pipelineIndex(pipelineId);
      Objects.requireNonNull(entries, "entries");
      if (entries.size() < 1 || entries.size() > 256) throw new IllegalArgumentException("pipeline entry count");
      int[] inputProfiles = new int[entries.size()];
      byte[][] inputKeys = new byte[entries.size()][];
      int row = 0;
      for (PipelineEntry entry : entries) {
        Objects.requireNonNull(entry, "entry"); inputProfiles[row] = profileIndex(entry.profile());
        inputKeys[row++] = entry.key().canonicalBytes();
      }
      int[] order = M3Columns.order(entries.size(), (a,b) ->
          Integer.compare(position(pipeline,inputProfiles[a]),position(pipeline,inputProfiles[b])));
      profiles = new int[entries.size()]; keyOffsets = new int[entries.size()+1];
      java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
      for (int i=0;i<order.length;i++) {
        profiles[i] = inputProfiles[order[i]]; bytes.writeBytes(inputKeys[order[i]]);
        keyOffsets[i+1] = bytes.size();
      }
      keyBytes = bytes.toByteArray();
      validateBindings(); canonical = encode(); id = M3Columns.digest(canonical);
    }
    private PipelineManifest(int pipeline,int[] profiles,int[] offsets,byte[] keys) {
      this.pipeline=pipeline;this.profiles=profiles;keyOffsets=offsets;keyBytes=keys;
      validateBindings();canonical=encode();id=M3Columns.digest(canonical);
    }
    private void validateBindings() {
      if (profiles.length<1 || profiles.length>256 || keyOffsets.length!=profiles.length+1
          || keyOffsets[0]!=0 || keyOffsets[profiles.length]!=keyBytes.length)
        throw new IllegalArgumentException("pipeline column geometry");
      long seen=0L;int previous=-1;
      byte[] ids=new byte[profiles.length*32];
      for(int row=0;row<profiles.length;row++){
        int pos=position(pipeline,profiles[row]);
        if(pos<=previous)throw new IllegalArgumentException("duplicate or unordered pipeline profile");
        previous=pos;seen|=1L<<profiles[row];
        int from=keyOffsets[row],to=keyOffsets[row+1];
        if(from<0 || to<=from || to-from>MAX_BYTES)throw new IllegalArgumentException("pipeline key geometry");
        byte[] key=java.util.Arrays.copyOfRange(keyBytes,from,to);
        try { M3PrecomputeKey.validateCanonical(key); }
        catch(java.io.IOException invalid){throw new IllegalArgumentException("invalid pipeline key",invalid);}
        byte[] digest=digest(keyBytes,from,to-from);System.arraycopy(digest,0,ids,row*32,32);
        for(int earlier=0;earlier<row;earlier++)
          if(M3IdPairs.compare(ids,earlier*32,ids,row*32)==0)throw new IllegalArgumentException("duplicate pipeline key");
      }
      for(int i=REQUIRED_PROFILES_OFFSETS[pipeline];i<REQUIRED_PROFILES_OFFSETS[pipeline+1];i++)
        if((seen&(1L<<REQUIRED_PROFILES[i]))==0)throw new IllegalArgumentException("pipeline missing required profile");
    }
    public String pipelineId(){return PIPELINE_IDS[pipeline];}
    public List<PipelineEntry> entries(){
      return java.util.Collections.unmodifiableList(new java.util.AbstractList<>(){
        @Override public int size(){return profiles.length;}
        @Override public PipelineEntry get(int row){
          Objects.checkIndex(row,size());
          try { return new PipelineEntry(PROFILE_IDS[profiles[row]],M3PrecomputeKey.decode(
              java.util.Arrays.copyOfRange(keyBytes,keyOffsets[row],keyOffsets[row+1]))); }
          catch(java.io.IOException impossible){throw new AssertionError(impossible);}
        }
      });
    }
    public M3ContentId id(){return M3ContentId.of(id);}
    public List<String> runtimeOnlyState(){return strings(RUNTIME_STATE,RUNTIME_OFFSETS[pipeline],RUNTIME_OFFSETS[pipeline+1]);}
    public byte[] canonicalBytes(){return canonical.clone();}
    public M3ObjectRef publish(M3DirectoryContentStore store)throws java.io.IOException{
      return Objects.requireNonNull(store,"store").put(canonical,M3ObjectKind.PRECOMPUTE_PIPELINE_DESCRIPTOR);
    }
    public java.util.Optional<M3PrecomputeCache.MappedBundleSet> openMapped(M3PrecomputeCache cache,int maxLaneBytes,long maxTotalBytes)throws java.io.IOException{
      byte[] ids=new byte[profiles.length*32];
      for(int i=0;i<profiles.length;i++)System.arraycopy(digest(keyBytes,keyOffsets[i],keyOffsets[i+1]-keyOffsets[i]),0,ids,i*32,32);
      java.util.Optional<M3PrecomputeCache.MappedBundleSet> result=Objects.requireNonNull(cache,"cache").lookupMappedBundleIds(ids,maxLaneBytes,maxTotalBytes);
      if(result.isEmpty())return java.util.Optional.empty();
      M3PrecomputeCache.MappedBundleSet set=result.get();
      if(set.bundleCount()!=profiles.length)throw new java.io.IOException("pipeline bundle count mismatch");
      for(int i=0;i<profiles.length;i++){
        M3FsPrecomputeBundle bundle=set.bundleAt(i);
        if(!PROFILE_IDS[profiles[i]].equals(bundle.profile()))throw new java.io.IOException("pipeline key/profile mismatch");
        try{requireCompatible(bundle,true);}catch(IllegalArgumentException invalid){throw new java.io.IOException("pipeline bundle geometry mismatch",invalid);}
      }
      return result;
    }
    public static PipelineManifest load(M3DirectoryContentStore store,M3ObjectRef reference)throws java.io.IOException{
      Objects.requireNonNull(reference,"reference");
      if(reference.kind()!=M3ObjectKind.PRECOMPUTE_PIPELINE_DESCRIPTOR)throw new java.io.IOException("not a pipeline descriptor");
      return decode(Objects.requireNonNull(store,"store").readSmall(reference,MAX_BYTES));
    }
    public static PipelineManifest decode(byte[] encoded)throws java.io.IOException{
      byte[] bytes=Objects.requireNonNull(encoded,"encoded").clone();
      try(java.io.DataInputStream in=new java.io.DataInputStream(new java.io.ByteArrayInputStream(bytes))){
        if(in.readInt()!=FORMAT)throw new java.io.IOException("unsupported pipeline format");
        int pipeline=pipelineIndex(M3Columns.readString(in,false)),count=M3Columns.readCount(in,256,10);
        if(count==0)throw new java.io.IOException("empty pipeline");
        int[] profiles=new int[count],offsets=new int[count+1];java.io.ByteArrayOutputStream keys=new java.io.ByteArrayOutputStream();
        for(int i=0;i<count;i++){
          profiles[i]=profileIndex(M3Columns.readString(in,false));int size=in.readInt();
          if(size<1 || size>MAX_BYTES || size>in.available())throw new java.io.IOException("invalid pipeline key bytes");
          keys.writeBytes(in.readNBytes(size));offsets[i+1]=keys.size();
        }
        if(in.read()!=-1)throw new java.io.IOException("trailing pipeline bytes");
        PipelineManifest result=new PipelineManifest(pipeline,profiles,offsets,keys.toByteArray());
        if(!java.util.Arrays.equals(bytes,result.canonical))throw new java.io.IOException("non-canonical pipeline bytes");
        return result;
      }catch(IllegalArgumentException invalid){throw new java.io.IOException("invalid pipeline manifest",invalid);}
    }
    private byte[] encode(){
      try{
        java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();
        try(java.io.DataOutputStream out=new java.io.DataOutputStream(bytes)){
          out.writeInt(FORMAT);M3Columns.writeString(out,pipelineId());out.writeInt(profiles.length);
          for(int i=0;i<profiles.length;i++){
            M3Columns.writeString(out,PROFILE_IDS[profiles[i]]);int size=keyOffsets[i+1]-keyOffsets[i];
            out.writeInt(size);out.write(keyBytes,keyOffsets[i],size);
          }
        }return bytes.toByteArray();
      }catch(java.io.IOException impossible){throw new AssertionError(impossible);}
    }
    private static byte[] digest(byte[] source,int start,int size){
      try{java.security.MessageDigest d=java.security.MessageDigest.getInstance("SHA-256");d.update(source,start,size);return d.digest();}
      catch(java.security.NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
    }
  }
  private static final String[] PROFILE_IDS = {"candidate-stream-v1", "gpu-prepared-byte-segments-v1", "gpu-prepared-byte-machine-v1", "mindex-segmented-utf16-v1", "gpu-grep-segments-v1", "gpu-search-index-v1", "trigram-postings-v1", "posting-set-v1", "bitmap-set-v1", "compact-ascii-dfa-v1", "automaton-v1", "sorted-bounds-v1", "csr-graph-v1", "hash-signatures-v1", "window-range-v1", "tuple-lane-v1", "candidate-stream-wide-v1", "posting-set-wide-v1", "rank-select-bitmap-v1", "static-graph-v1", "join-index-v1", "posting-index-v1", "interval-index-v1", "dominator-tree-v1", "search-selectivity-v1"};
  private static final int[] RETENTIONS = {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1};
  private static final long[] REQUIRED_ROLES = {0x41L, 0x780000000000c1L, 0x384000000040000L, 0x3eL, 0x800000081L, 0xf8c2L, 0xc400L, 0xc000L, 0x20000L, 0x7800000000000L, 0xc0000L, 0x100000L, 0x1800000L, 0x2000000L, 0x30000000L, 0x400000000000000L, 0x800000001L, 0x1000010000L, 0x2000020000L, 0x18001800000L, 0x400500000L, 0x10c000L, 0x30000000L, 0x600000000000L, 0x100000000400L};
  private static final long[] OPTIONAL_ROLES = {0x300L, 0x0L, 0x0L, 0x0L, 0x2020000L, 0x106000020000L, 0x20000L, 0x30000L, 0x0L, 0x0L, 0x40L, 0x400000L, 0x20000L, 0xc000000L, 0x140000000L, 0x400020000L, 0x300L, 0x20000L, 0x4000000000L, 0x20000020000L, 0xc0000000000L, 0x20000L, 0x0L, 0x0L, 0x200000000L};
  private static final String[] OPERATIONS = {"GREP_I32_SUBSEQUENCE", "BYTE_SEGMENT_GREP", "BITMAP_AND_I64", "BYTE_AUTOMATON_MATCH64_SEGMENTS", "UTF16_SCATTER_GATHER", "BYTE_PATTERN_SCAN", "GREP_I32_SUBSEQUENCE", "SET_INTERSECTION_I32", "BITMAP_AND_I64", "LOWER_BOUND_I32_BATCH", "SET_INTERSECTION_I32", "BITMAP_AND_I64", "SET_INTERSECTION_I32", "SET_UNION_I32", "SET_DIFFERENCE_I32", "BITMAP_AND_I64", "BITMAP_OR_I64", "BITMAP_AND_NOT_I64", "BITMAP_POPCOUNT_I64", "ASCII_DFA_BATCH", "AUTOMATON_I32_FINAL_STATE_BATCH", "LOWER_BOUND_I32_BATCH", "UPPER_BOUND_I32_BATCH", "MEMBERSHIP_I32_SORTED", "CSR_FRONTIER_I32", "HASH64_BATCH", "MINHASH64_I32_ROWS", "SIMHASH64_I32_ROWS", "WINDOW_HASH64_I32", "SCAN_I64_INCLUSIVE", "GREP_I32_SUBSEQUENCE", "BITMAP_AND_I64", "BITMAP_OR_I64", "BITMAP_AND_NOT_I64", "BITMAP_XOR_I64", "BITMAP_POPCOUNT_I64", "POPCOUNT_I64", "CSR_FRONTIER_I32", "LOWER_BOUND_I32_BATCH", "UPPER_BOUND_I32_BATCH", "MEMBERSHIP_I32_SORTED", "GROUP_STARTS_I32_SORTED", "DIRECT_INNER_JOIN_I32_DISTINCT", "LOWER_BOUND_I32_BATCH", "SET_INTERSECTION_I32", "LOWER_BOUND_I32_BATCH", "UPPER_BOUND_I32_BATCH", "HISTOGRAM_I32_DENSE"};
  private static final int[] OP_OFFSETS = {0, 1, 3, 4, 5, 6, 10, 12, 15, 19, 20, 21, 24, 25, 28, 30, 30, 31, 31, 37, 38, 43, 45, 47, 47, 48};
  private static final String[] PIPELINE_IDS = {"gpu-prepared-byte-search-v1", "compact-ascii-dfa-v1", "gpu-grep-segments-v1", "gpu-grep-v1", "posting-query-v1", "static-graph-v1", "tuple-query-v1", "similarity-v1"};
  private static final int[] REQUIRED_PROFILES = {1, 2, 9, 4, 5, 21, 19, 15, 13};
  private static final int[] REQUIRED_PROFILES_OFFSETS = {0, 2, 3, 4, 5, 6, 7, 8, 9};
  private static final int[] OPTIONAL_PROFILES = {13, 24, 13, 24, 18, 18, 23, 8, 8, 11};
  private static final int[] OPTIONAL_PROFILES_OFFSETS = {0, 1, 1, 3, 5, 6, 8, 9, 10};
  private static final String[] RUNTIME_STATE = {"device-candidate-bitmap", "device-automaton", "kernel-handle", "stream", "match-output", "transfer-staging", "prepared-handle", "device-class-map", "device-transition-buffer", "device-accepting-buffer", "batch-workspace", "device-buffer", "kernel-handle", "stream", "match-output", "transfer-staging", "device-buffer", "kernel-handle", "stream", "candidate-output", "sort-scan-scratch", "intersection-output", "device-buffer", "scan-scratch", "frontier", "visited-scratch", "device-buffer", "result-page", "device-buffer", "topk-heap", "candidate-output", "device-buffer"};
  private static final int[] RUNTIME_OFFSETS = {0, 6, 11, 16, 21, 24, 27, 29, 32};

  private static final M3FsPrecomputeBundle.Role[] ROLE_VALUES=M3FsPrecomputeBundle.Role.values();
  private static final Retention[] RETENTION_VALUES=Retention.values();
  private M3FsPrecomputeCatalog(){}
  private static long mask(Set<M3FsPrecomputeBundle.Role> roles){
    long result=0L;for(M3FsPrecomputeBundle.Role role:roles)result|=1L<<role.ordinal();return result;
  }
  private static Set<M3FsPrecomputeBundle.Role> roleSet(long mask){
    EnumSet<M3FsPrecomputeBundle.Role> set=EnumSet.noneOf(M3FsPrecomputeBundle.Role.class);
    for(int i=0;i<ROLE_VALUES.length;i++)if((mask&(1L<<i))!=0)set.add(ROLE_VALUES[i]);return Set.copyOf(set);
  }
  private static List<String> strings(String[] source,int from,int to){return List.of(java.util.Arrays.copyOfRange(source,from,to));}
  static int profileIndex(String id){
    String checked=requireId(id);for(int i=0;i<PROFILE_IDS.length;i++)if(PROFILE_IDS[i].equals(checked))return i;
    throw new IllegalArgumentException("unknown precompute profile: "+checked);
  }
  private static int pipelineIndex(String id){
    String checked=requireId(id);for(int i=0;i<PIPELINE_IDS.length;i++)if(PIPELINE_IDS[i].equals(checked))return i;
    throw new IllegalArgumentException("unknown precompute pipeline: "+checked);
  }
  static void requireCompatible(M3FsPrecomputeBundle bundle,boolean strict){
    Objects.requireNonNull(bundle,"bundle");int row=profileIndex(bundle.profile());
    validateRoles(bundle,PROFILE_IDS[row],REQUIRED_ROLES[row],OPTIONAL_ROLES[row],strict);
  }
  private static void validateRoles(M3FsPrecomputeBundle bundle,String id,long required,long optional,boolean strict){
    Objects.requireNonNull(bundle,"bundle");
    if(!bundle.profile().equals(id))throw new IllegalArgumentException("bundle profile mismatch");
    long present=bundle.roleMask();
    if((present&required)!=required)throw new IllegalArgumentException("missing required precompute roles");
    if(strict && (present&~(required|optional))!=0)throw new IllegalArgumentException("unexpected precompute roles");
  }
  private static Profile profileAt(int row){
    return new Profile(PROFILE_IDS[row],RETENTION_VALUES[RETENTIONS[row]],roleSet(REQUIRED_ROLES[row]),
        roleSet(OPTIONAL_ROLES[row]),strings(OPERATIONS,OP_OFFSETS[row],OP_OFFSETS[row+1]));
  }
  public static Profile byId(String id){return profileAt(profileIndex(id));}
  public static List<Profile> profiles(){
    return java.util.Collections.unmodifiableList(new java.util.AbstractList<>(){
      @Override public int size(){return PROFILE_IDS.length;}
      @Override public Profile get(int row){return profileAt(Objects.checkIndex(row,size()));}
    });
  }
  public static List<Profile> forOperation(String operation){
    String checked=Objects.requireNonNull(operation,"operation").strip();
    if(checked.isEmpty())throw new IllegalArgumentException("operation");
    java.util.ArrayList<Profile> result=new java.util.ArrayList<>();
    for(int row=0;row<PROFILE_IDS.length;row++)for(int i=OP_OFFSETS[row];i<OP_OFFSETS[row+1];i++)
      if(OPERATIONS[i].equals(checked)){result.add(profileAt(row));break;}
    return List.copyOf(result);
  }
  private static Pipeline pipelineAt(int row){
    java.util.ArrayList<String> required=new java.util.ArrayList<>(),optional=new java.util.ArrayList<>();
    for(int i=REQUIRED_PROFILES_OFFSETS[row];i<REQUIRED_PROFILES_OFFSETS[row+1];i++)required.add(PROFILE_IDS[REQUIRED_PROFILES[i]]);
    for(int i=OPTIONAL_PROFILES_OFFSETS[row];i<OPTIONAL_PROFILES_OFFSETS[row+1];i++)optional.add(PROFILE_IDS[OPTIONAL_PROFILES[i]]);
    return new Pipeline(PIPELINE_IDS[row],required,optional,strings(RUNTIME_STATE,RUNTIME_OFFSETS[row],RUNTIME_OFFSETS[row+1]));
  }
  public static Pipeline pipelineById(String id){return pipelineAt(pipelineIndex(id));}
  public static List<Pipeline> pipelines(){
    return java.util.Collections.unmodifiableList(new java.util.AbstractList<>(){
      @Override public int size(){return PIPELINE_IDS.length;}
      @Override public Pipeline get(int row){return pipelineAt(Objects.checkIndex(row,size()));}
    });
  }
  private static int position(int pipeline,int profile){
    int pos=0;for(int i=REQUIRED_PROFILES_OFFSETS[pipeline];i<REQUIRED_PROFILES_OFFSETS[pipeline+1];i++,pos++)if(REQUIRED_PROFILES[i]==profile)return pos;
    for(int i=OPTIONAL_PROFILES_OFFSETS[pipeline];i<OPTIONAL_PROFILES_OFFSETS[pipeline+1];i++,pos++)if(OPTIONAL_PROFILES[i]==profile)return pos;
    throw new IllegalArgumentException("pipeline contains unrelated profile");
  }
  private static Set<M3FsPrecomputeBundle.Role> roles(M3FsPrecomputeBundle.Role... roles){return Set.of(roles);}
  private static Profile profile(String id,Retention retention,Set<M3FsPrecomputeBundle.Role> required,Set<M3FsPrecomputeBundle.Role> optional,String... operations){
    return new Profile(id,retention,required,optional,List.of(operations));
  }
  private static String requireId(String value){
    String checked=Objects.requireNonNull(value,"id").strip();
    if(!checked.matches("[a-z0-9][a-z0-9._-]{0,127}"))throw new IllegalArgumentException("profile id");return checked;
  }
}
