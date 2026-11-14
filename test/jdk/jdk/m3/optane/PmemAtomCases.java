// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.optane;

import jdk.m3.optane.*;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

/**
 * Independent functional oracle for each public PMEMSDK API atom. The same
 * oracle runs under JUnit and without an external test dependency. Timing
 * separately measures each operation without a pass/fail throughput limit.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/36827ee8a798ef6ce07f6fc55a2ac371b1382c06/m3-pmem-sdk/src/test/java/test/PmemAtomCases.java blob bc3b13ce4f1b; recipe m3/optane/recipe/pmem-region-core-20261106/relocate.py v2
public final class PmemAtomCases {
    private PmemAtomCases() {}
    private static volatile long sink;
    private static final String VECTOR_ATOM =
            "topKCosineFloatLE(PmemRegion,long,long,float[],int)";
    private static final String VECTOR_PROGRESS_ATOM =
            "topKCosineFloatLE(PmemRegion,long,long,float[],int,java.util.function.LongConsumer)";

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static void expect(Class<? extends Throwable> type, Runnable action) {
        try { action.run(); }
        catch (Throwable e) {
            if (type.isInstance(e)) return;
            throw new AssertionError("wrong failure type: " + type.getSimpleName(), e);
        }
        throw new AssertionError("expected " + type.getSimpleName());
    }
    private static Path sparseFile(long size) throws Exception {
        Path path = Files.createTempFile("m3-atom-test-", ".bin");
        try (RandomAccessFile file=new RandomAccessFile(path.toFile(),"rw")) {file.setLength(size);}
        return path;
    }
    private static Set<String> methods(Class<?> owner) {
        Set<String> names=new TreeSet<>();
        for (Method m:owner.getDeclaredMethods()) {
            if (!Modifier.isPublic(m.getModifiers()) || m.isSynthetic()) continue;
            String atom=m.getName();
            if (owner==PmemSearch.class && atom.equals("topKCosineFloatLE")) {
                check(Modifier.isStatic(m.getModifiers()) &&
                        m.getReturnType()==PmemSearch.VectorMatches.class,
                        "unregistered vector return type or instance method: " + m);
                Class<?>[] parameters=m.getParameterTypes();
                if (Arrays.equals(parameters,new Class<?>[]{
                        PmemRegion.class,long.class,long.class,float[].class,int.class})) {
                    atom=VECTOR_ATOM;
                } else if (Arrays.equals(parameters,new Class<?>[]{
                        PmemRegion.class,long.class,long.class,float[].class,int.class,
                        java.util.function.LongConsumer.class})) {
                    atom=VECTOR_PROGRESS_ATOM;
                } else {
                    throw new AssertionError(
                            "unregistered vector overload requires per-signature atom: " + m);
                }
            }
            if (!names.add(atom)) {
                throw new AssertionError("unregistered public overload requires per-signature atom: " +
                        owner.getSimpleName() + "#" + m.getName());
            }
        }
        return names;
    }
    public static void verifyApiAtoms(Set<String> ids) {
        Set<String> expected=new TreeSet<>();
        for (Class<?> owner : new Class<?>[]{PmemRegion.class, PmemSdk.class, PmemTransfer.class, PmemSearch.class}) {
            expected.addAll(methods(owner));
        }
        check(expected.equals(ids), "API atom inventory changed: expected=" + expected + " registered=" + ids);
    }

    @SuppressWarnings("try")
    public static void verify(String atom) throws Exception {
        if (atom.equals("capabilities")) {
            PmemCapabilities caps=PmemSdk.capabilities();
            check(caps.platform()!=null && !caps.platform().isBlank(),"platform missing");
            check(caps.nativeAvailable() && caps.osPageSize()>0,"native Linux capability missing");
            return;
        }
        Path path=sparseFile((atom.equals("fileOffset") ? (1L<<31)+32768 : 131072));
        Path second=sparseFile(131072);
        try {
            long fileOffset=atom.equals("fileOffset") ? (1L<<31)+103 : 103;
            try(PmemRegion a=PmemSdk.open(path,fileOffset,16384,PmemAccess.READ_WRITE,PmemBackend.POSIX);
                PmemRegion b=PmemSdk.open(second,37,16384,PmemAccess.READ_WRITE,PmemBackend.POSIX)) {
                byte[] bytes={1,2,3,4,5,6,7,8};
                switch(atom) {
                    case "open" -> {
                        check(a.isOpen(),"open mapping inaccessible");
                        expect(IllegalArgumentException.class,
                                () -> {try {PmemSdk.open(path,-1,3,PmemAccess.READ_WRITE,PmemBackend.POSIX);}
                                       catch(java.io.IOException e) {throw new RuntimeException(e);}});
                    }
                    case "length" -> check(a.length()==16384,"length");
                    case "fileOffset" -> {
                        check(a.fileOffset()==fileOffset && a.fileOffset()>Integer.MAX_VALUE,"long offset lost");
                        a.write(7,bytes,0,8);
                        byte[] back=new byte[8];a.read(7,back,0,8);
                        check(Arrays.equals(bytes,back),"sparse long mapping");
                    }
                    case "path" -> check(a.path().equals(path),"path identity");
                    case "access" -> check(a.access()==PmemAccess.READ_WRITE,"access");
                    case "backend" -> check(a.backend()==PmemBackend.POSIX,"backend");
                    case "persistenceGranularity" -> check(a.persistenceGranularity()!=null,"granularity");
                    case "isOpen" -> check(a.isOpen(),"isOpen");
                    case "read" -> {
                        a.write(8,bytes,0,8);
                        byte[] dst=new byte[12];a.read(8,dst,2,8);
                        check(Arrays.equals(bytes,Arrays.copyOfRange(dst,2,10)),"read offset");
                        expect(IndexOutOfBoundsException.class,()->a.read(16382,dst,0,8));
                    }
                    case "write" -> {
                        a.write(10,bytes,2,4);
                        byte[] dst=new byte[4];a.read(10,dst,0,4);
                        check(Arrays.equals(dst,new byte[]{3,4,5,6}),"write array offset");
                        expect(IndexOutOfBoundsException.class,()->a.write(Long.MAX_VALUE,bytes,0,1));
                    }
                    case "persist" -> {
                        a.write(25,bytes,0,8);
                        a.persist(25,8);
                        try(PmemRegion reopen=PmemSdk.open(path,fileOffset,16384,PmemAccess.READ_ONLY,PmemBackend.POSIX)) {
                            byte[] dst=new byte[8];reopen.read(25,dst,0,8);
                            check(Arrays.equals(bytes,dst),"persist-reopen");
                        }
                    }
                    case "close" -> {
                        a.close();a.close();
                        check(!a.isOpen(),"close state");
                        expect(IllegalStateException.class,()->a.read(0,new byte[1],0,1));
                    }
                    case "copy" -> {
                        a.write(16,bytes,0,8);PmemTransfer.copy(a,16,b,128,8);
                        byte[] dst=new byte[8];b.read(128,dst,0,8);
                        check(Arrays.equals(bytes,dst),"copy parity");
                        PmemTransfer.copy(a,16,a,17,7);
                        a.read(17,dst,0,7);
                        check(Arrays.equals(Arrays.copyOfRange(dst,0,7),Arrays.copyOfRange(bytes,0,7)),"overlap memmove");
                    }
                    case "copyIfDifferent" -> {
                        a.write(16,bytes,0,8);
                        check(PmemTransfer.copyIfDifferent(a,16,b,128,8), "first copy must write");
                        check(!PmemTransfer.copyIfDifferent(a,16,b,128,8), "identical copy must skip");
                        b.write(129,new byte[]{99},0,1);
                        check(PmemTransfer.copyIfDifferent(a,16,b,128,8), "changed destination must be replaced");
                        check(PmemTransfer.equalRange(a,16,b,128,8), "copyIfDifferent parity");
                        check(!PmemTransfer.copyIfDifferent(a,16,a,16,8), "identical same-owner skip");
                        check(!PmemTransfer.copyIfDifferent(a,0,b,0,0), "zero-byte copy must skip");
                    }
                    case "indexOf" -> {
                        byte[] fixture={1,2,1,2,1,2,3,4};
                        a.write(80,fixture,0,fixture.length);
                        check(PmemSearch.indexOf(a,80,8,new byte[]{1,2,1,2,3})==82,
                                "KMP repeating-prefix fallback");
                        check(PmemSearch.indexOf(a,80,8,new byte[]{3})==86,
                                "single byte memchr fast path");
                        check(PmemSearch.indexOf(a,80,8,new byte[]{9})==-1,"absent needle");
                        check(PmemSearch.indexOf(a,16384,0,new byte[0])==16384,
                                "empty pattern at end");
                        expect(IndexOutOfBoundsException.class,
                                ()->PmemSearch.indexOf(a,16384,1,new byte[]{1}));
                    }
                    case "lowerBoundLongLE", "upperBoundLongLE" -> {
                        long[] values={Long.MIN_VALUE,-9,-9,0,3,3,Long.MAX_VALUE};
                        byte[] encoded=new byte[values.length*Long.BYTES];
                        java.nio.ByteBuffer buffer=java.nio.ByteBuffer.wrap(encoded)
                                .order(java.nio.ByteOrder.LITTLE_ENDIAN);
                        for(long value:values) buffer.putLong(value);
                        a.write(111,encoded,0,encoded.length);
                        boolean upper=atom.equals("upperBoundLongLE");
                        for(long key:new long[]{Long.MIN_VALUE,-10,-9,-8,0,2,3,4,Long.MAX_VALUE}) {
                            int expected=0;
                            while(expected<values.length &&
                                    (upper ? values[expected]<=key : values[expected]<key)) expected++;
                            long result=upper
                                    ? PmemSearch.upperBoundLongLE(a,111,values.length,key)
                                    : PmemSearch.lowerBoundLongLE(a,111,values.length,key);
                            check(result==expected,"sorted long search key="+key+" upper="+upper);
                        }
                        long empty=upper ? PmemSearch.upperBoundLongLE(a,16384,0,17)
                                : PmemSearch.lowerBoundLongLE(a,16384,0,17);
                        check(empty==0,"empty sorted range");
                        expect(IndexOutOfBoundsException.class,
                                ()->PmemSearch.lowerBoundLongLE(a,16384,1,0));
                        expect(IndexOutOfBoundsException.class,
                                ()->PmemSearch.upperBoundLongLE(a,0,Long.MAX_VALUE,0));
                    }
                    case VECTOR_ATOM, VECTOR_PROGRESS_ATOM -> {
                        boolean progress=atom.equals(VECTOR_PROGRESS_ATOM);
                        float[] query={1,0,0};
                        float[][] vectors={{1,0,0},{0,1,0},{2,0,0},{-1,0,0},{1,1,0},{0,0,1}};
                        byte[] encoded=new byte[vectors.length*query.length*Float.BYTES];
                        java.nio.ByteBuffer buffer=java.nio.ByteBuffer.wrap(encoded)
                                .order(java.nio.ByteOrder.LITTLE_ENDIAN);
                        for(float[] row:vectors) for(float component:row) buffer.putFloat(component)
                                ;
                        a.write(111,encoded,0,encoded.length);
                        java.util.List<Long> completed=new java.util.ArrayList<>();
                        PmemSearch.VectorMatches matches=vectorSearch(a,111,vectors.length,
                                query,5,progress,completed::add);
                        long[] expectedRows={0,2,4,1,5};
                        double[] expectedScores={1,1,1/Math.sqrt(2.0),0,0};
                        check(matches.size()==expectedRows.length,"bounded vector result count");
                        for(int i=0;i<expectedRows.length;i++) {
                            check(matches.row(i)==expectedRows[i],"exact vector rank/tie at " + i);
                            check(Math.abs(matches.score(i)-expectedScores[i])<1e-12,
                                    "exact vector cosine at " + i);
                        }
                        check(Arrays.equals(query,new float[]{1,0,0}),"vector query mutated");
                        if(progress) {
                            check(completed.equals(java.util.List.of(0L,6L)),
                                    "vector progress extent");
                            expect(IllegalStateException.class,()->vectorSearch(a,111,6,query,5,
                                    true,
                                    ignored->{throw new IllegalStateException("progress refused");})
                                            );
                            expect(NullPointerException.class,
                                    ()->PmemSearch.topKCosineFloatLE(a,111,6,query,5,null));
                        } else {
                            check(completed.isEmpty(),
                                    "convenience overload unexpectedly used caller progress");
                        }
                        check(vectorSearch(a,a.length(),0,query,5,progress,ignored->{}).size()==0,
                                "empty vector run at end");
                        expect(IndexOutOfBoundsException.class,
                                ()->vectorSearch(a,a.length(),1,query,5,progress,ignored->{}));
                        expect(IllegalArgumentException.class,
                                ()->vectorSearch(a,111,6,query,0,progress,ignored->{}));
                        expect(IllegalArgumentException.class,
                                ()->vectorSearch(a,111,6,new float[]{0,0,0},5,progress,ignored->{}))
                                        ;
                        buffer.putFloat(0,Float.NaN);a.write(111,encoded,0,encoded.length);
                        expect(IllegalArgumentException.class,
                                ()->vectorSearch(a,111,6,query,5,progress,ignored->{}));
                    }
                    case "equalRange" -> {
                        a.write(16,bytes,0,8);PmemTransfer.copy(a,16,b,128,8);
                        check(PmemTransfer.equalRange(a,16,b,128,8),"equal true");
                        b.write(129,new byte[]{99},0,1);
                        check(!PmemTransfer.equalRange(a,16,b,128,8),"equal false");
                    }
                    default -> throw new IllegalArgumentException("unregistered atom: "+atom);
                }
            }
        } finally {Files.deleteIfExists(path);Files.deleteIfExists(second);}
    }

    private static PmemSearch.VectorMatches vectorSearch(PmemRegion region,long offset,long rows,
            float[] query,int k,boolean progress,java.util.function.LongConsumer callback) {
        return progress ? PmemSearch.topKCosineFloatLE(region,offset,rows,query,k,callback)
                : PmemSearch.topKCosineFloatLE(region,offset,rows,query,k);
    }

    public static long benchmark(String atom,int iterations) throws Exception {
        if(iterations<1 || iterations>100_000) throw new IllegalArgumentException("invalid iterations");
        Path path=sparseFile(131072), other=sparseFile(131072);
        try {
            byte[] src=new byte[1024],dst=new byte[1024];
            for(int i=0;i<src.length;i++)src[i]=(byte)(i*31);
            try(PmemRegion a=PmemSdk.open(path,0,65536,PmemAccess.READ_WRITE,PmemBackend.POSIX);
                PmemRegion b=PmemSdk.open(other,0,65536,PmemAccess.READ_WRITE,PmemBackend.POSIX)) {
                a.write(0,src,0,src.length);
                byte[] ordered=new byte[128*Long.BYTES];
                java.nio.ByteBuffer orderedBytes=java.nio.ByteBuffer.wrap(ordered)
                        .order(java.nio.ByteOrder.LITTLE_ENDIAN);
                for(int k=0;k<128;k++) orderedBytes.putLong(k/3-40);
                a.write(2048,ordered,0,ordered.length);
                float[] vectorQuery={1,2,3,4,5,6,7,8};
                byte[] vectors=new byte[128*vectorQuery.length*Float.BYTES];
                java.nio.ByteBuffer vectorBytes=java.nio.ByteBuffer.wrap(vectors)
                        .order(java.nio.ByteOrder.LITTLE_ENDIAN);
                for(int row=0;row<128;row++) for(int column=0;column<vectorQuery.length;column++)
                    vectorBytes.putFloat((row*17+column*7)%31-15);
                a.write(4096,vectors,0,vectors.length);
                long[] completed={0};
                java.util.function.LongConsumer progress=rows->completed[0]=rows;
                long checksum=0L;
                long t0=System.nanoTime();
                for(int i=0;i<iterations;i++) {
                    switch(atom) {
                        case "capabilities" -> checksum+=PmemSdk.capabilities().osPageSize();
                        case "open" -> {try(PmemRegion c=PmemSdk.open(path,0,4096,PmemAccess.READ_ONLY,PmemBackend.POSIX)) {checksum+=c.length();}}
                        case "length" -> checksum+=a.length();
                        case "fileOffset" -> checksum+=a.fileOffset();
                        case "path" -> checksum+=a.path().getNameCount();
                        case "access" -> checksum+=a.access().ordinal();
                        case "backend" -> checksum+=a.backend().ordinal();
                        case "persistenceGranularity" -> checksum+=a.persistenceGranularity().ordinal();
                        case "isOpen" -> checksum+=a.isOpen()?1:0;
                        case "read" -> {a.read(0,dst,0,1024);checksum+=dst[i%1024];}
                        case "write" -> {a.write(0,src,0,1024);checksum+=src[i%1024];}
                        case "persist" -> {a.persist(0,4096);checksum++;}
                        case "close" -> {try(PmemRegion c=PmemSdk.open(path,0,4096,PmemAccess.READ_ONLY,PmemBackend.POSIX)) {checksum+=c.length();}}
                        case "copy" -> {PmemTransfer.copy(a,0,b,0,1024);checksum++;}
                        case "equalRange" -> checksum+=PmemTransfer.equalRange(a,0,b,0,1024)?1:0;
                        case "copyIfDifferent" -> checksum+=PmemTransfer.copyIfDifferent(a,0,b,0,1024)?1:0;
                        case "lowerBoundLongLE" -> {
                            checksum+=PmemSearch.lowerBoundLongLE(a,2048,128,31);
                        }
                        case "upperBoundLongLE" -> {
                            checksum+=PmemSearch.upperBoundLongLE(a,2048,128,31);
                        }
                        case "indexOf" -> checksum+=1+PmemSearch.indexOf(a,0,1024,
                                new byte[]{31,62,93});
                        case VECTOR_ATOM -> {
                            PmemSearch.VectorMatches matches=PmemSearch.topKCosineFloatLE(
                                    a,4096,128,vectorQuery,7);
                            checksum+=matches.size()+matches.row(0)+Double.doubleToRawLongBits(
                                    matches.score(0));
                        }
                        case VECTOR_PROGRESS_ATOM -> {
                            PmemSearch.VectorMatches matches=PmemSearch.topKCosineFloatLE(
                                    a,4096,128,vectorQuery,7,progress);
                            checksum+=completed[0]+matches.size()+matches.row(0)
                                    +Double.doubleToRawLongBits(matches.score(0));
                        }
                        default -> throw new IllegalArgumentException("unregistered atom: "+atom);
                    }
                }
                long elapsed=System.nanoTime()-t0;
                sink=checksum;
                check(elapsed>=0,"negative elapsed time");
                return elapsed;
            }
        } finally {Files.deleteIfExists(path);Files.deleteIfExists(other);}
    }

    /** Dependency-free oracle: exercised locally and reused by JUnit 5 dynamic tests. */
    public static void main(String[] args) throws Exception {
        Set<String> atoms=new TreeSet<>(Arrays.asList(
                "capabilities","open","length","fileOffset","path","access","backend",
                "persistenceGranularity","isOpen","read","write","persist","close","copy",
                        "equalRange","copyIfDifferent","indexOf","lowerBoundLongLE",
                        "upperBoundLongLE",
                VECTOR_ATOM,VECTOR_PROGRESS_ATOM));
        verifyApiAtoms(atoms);
        for(String atom:atoms) {verify(atom);System.out.println("PASS atom "+atom);}
        // Workloads are checksummed, but timings are never pass/fail performance thresholds.
        for(String atom:atoms) {
            long nanos=benchmark(atom,atom.equals("persist")?2:8);
            System.out.printf("PERF-SMOKE %s: %d ns / 8-or-fewer invocations%n",atom,nanos);
        }
        System.out.println(
        "PASS 21/21 public API atoms with individual correctness and performance workloads");
    }
}
