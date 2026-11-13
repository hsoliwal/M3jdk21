// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.optane;

import jdk.m3.optane.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Independent array/ByteBuffer oracles for the ordinary-file backend. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/36827ee8a798ef6ce07f6fc55a2ac371b1382c06/m3-pmem-sdk/src/junit/java/test/PmemFileBackendJUnitTest.java blob 5d8fef1dd7f6; recipe make/m3/optane/recipe/pmem-region-core-20261106/relocate.py v2
public final class PmemFileBackendJUnitTest {
    private static PmemRegion open(Path p, long n, PmemBackend b) throws Exception {
        return PmemSdk.open(p, 0, n, PmemAccess.READ_WRITE, b);
    }
    private static byte[] read(PmemRegion r) {
        byte[] b = new byte[(int) r.length()]; r.read(0, b, 0, b.length); return b;
    }
    private static long index(byte[] b, int start, int count, byte[] p) {
        outer: for (int i=start; i<=start+count-p.length; i++) {
            for (int j=0; j<p.length; j++) if (b[i+j]!=p[j]) continue outer;
            return i;
        }
        return -1;
    }
    @Test void metadataIoAndPersistenceWithoutNativeDependency() throws Exception {
        assertEquals(0, PmemBackend.AUTO.ordinal()); assertEquals(1, PmemBackend.POSIX.ordinal());
        assertEquals(2, PmemBackend.PMDK.ordinal()); assertEquals(3, PmemBackend.FILE.ordinal());
        Path p=Files.createTempFile("pmem-file-", ".bin");
        try {
            Files.write(p, new byte[128]);
            try (PmemRegion r=PmemSdk.open(p, 7, 97, PmemAccess.READ_WRITE, PmemBackend.FILE)) {
                assertEquals(p, r.path()); assertEquals(7, r.fileOffset()); assertEquals(97, r.length());
                assertEquals(PmemBackend.FILE, r.backend()); assertEquals(PmemAccess.READ_WRITE, r.access());
                assertEquals(PmemGranularity.PAGE, r.persistenceGranularity()); assertTrue(r.isOpen());
                byte[] b={1,2,3,4}; r.write(2,b,1,2); r.persist(0,97);
                byte[] out=new byte[6]; r.read(2,out,3,2); assertArrayEquals(new byte[]{0,0,0,2,3,0},out);
                r.read(97,out,0,0); r.write(97,b,0,0); r.persist(97,0);
            }
            assertEquals(2, Files.readAllBytes(p)[9]); assertEquals(3, Files.readAllBytes(p)[10]);
        } finally { Files.deleteIfExists(p); }
    }
    @Test void randomizedSearchTransferAndNativeDifferential() throws Exception {
        byte[] a=new byte[180_017], b=new byte[a.length]; new Random(1397).nextBytes(a);
        Path p=Files.createTempFile("pmem-file-", ".bin"), q=Files.createTempFile("pmem-posix-", ".bin");
        try {
            Files.write(p,a); Files.write(q,b);
            try (PmemRegion f=open(p,a.length,PmemBackend.FILE); PmemRegion n=open(q,b.length,PmemBackend.POSIX)) {
                PmemTransfer.copy(f,0,n,0,a.length); assertArrayEquals(a,read(n));
                assertTrue(PmemTransfer.equalRange(f,0,n,0,a.length));
                assertFalse(PmemTransfer.copyIfDifferent(f,0,n,0,a.length));
                n.write(65_535,new byte[]{42},0,1);
                assertTrue(PmemTransfer.copyIfDifferent(n,0,f,0,a.length)); a[65_535]=42;
                assertArrayEquals(a,read(f));
                Random rng=new Random(871);
                for (int run=0;run<120;run++) {
                    int start=rng.nextInt(a.length+1), count=rng.nextInt(a.length-start+1);
                    int k=rng.nextInt(20); byte[] pattern=new byte[k]; rng.nextBytes(pattern);
                    if (k<=count && run%2==0) System.arraycopy(a,start+rng.nextInt(count-k+1),pattern,0,k);
                    long expected=index(a,start,count,pattern);
                    assertEquals(expected,PmemSearch.indexOf(f,start,count,pattern));
                    assertEquals(expected,PmemSearch.indexOf(n,start,count,pattern));
                }
                byte[] cross={9,8,7,6,5,4}; f.write(65_533,cross,0,cross.length);
                assertEquals(65_533,PmemSearch.indexOf(f,0,a.length,cross));
                assertEquals(a.length,PmemSearch.indexOf(f,a.length,0,new byte[0]));
                assertEquals(-1,PmemSearch.indexOf(f,a.length,0,new byte[]{1}));
            }
        } finally { Files.deleteIfExists(p); Files.deleteIfExists(q); }
    }
    @Test void overlappingMemmoveAliasesAndFileToFile() throws Exception {
        byte[] data=new byte[200_000]; new Random(17).nextBytes(data);
        Path p=Files.createTempFile("pmem-overlap-", ".bin"), q=Files.createTempFile("pmem-copy-", ".bin");
        Path hard=p.resolveSibling(p.getFileName()+".link");
        try {
            Files.write(p,data); Files.write(q,new byte[data.length]); Files.createLink(hard,p);
            try (PmemRegion f=open(p,data.length,PmemBackend.FILE); PmemRegion g=open(q,data.length,PmemBackend.FILE);
                 PmemRegion alias=PmemSdk.open(hard,3,data.length-3,PmemAccess.READ_WRITE,PmemBackend.FILE);
                 PmemRegion nativeAlias=open(p,data.length,PmemBackend.POSIX)) {
                System.arraycopy(data,0,data,11,170_000); PmemTransfer.copy(f,0,f,11,170_000); assertArrayEquals(data,read(f));
                System.arraycopy(data,11,data,0,170_000); PmemTransfer.copy(f,11,f,0,170_000); assertArrayEquals(data,read(f));
                byte[] expected=data.clone(); System.arraycopy(expected,0,expected,7,150_000);
                assertTrue(PmemTransfer.copyIfDifferent(f,0,f,7,150_000)); assertArrayEquals(expected,read(f));
                assertFalse(PmemTransfer.copyIfDifferent(f,0,f,0,f.length()));
                PmemTransfer.copy(f,0,g,0,f.length()); assertTrue(PmemTransfer.equalRange(f,0,g,0,f.length()));
                assertThrows(IllegalArgumentException.class,()->PmemTransfer.copy(f,0,alias,0,100));
                assertThrows(IllegalArgumentException.class,()->PmemTransfer.copyIfDifferent(alias,0,f,3,100));
                assertThrows(IllegalArgumentException.class,()->PmemTransfer.copy(f,0,nativeAlias,1,100));
                assertTrue(PmemTransfer.equalRange(f,3,alias,0,100));
                PmemTransfer.copy(f,0,alias,150_000,100); // same file, disjoint physical spans
                PmemTransfer.copy(f,f.length(),alias,alias.length(),0);
                assertFalse(PmemTransfer.copyIfDifferent(f,f.length(),alias,alias.length(),0));
            }
        } finally { Files.deleteIfExists(hard); Files.deleteIfExists(p); Files.deleteIfExists(q); }
    }
    @Test void pathReplacementDoesNotBypassAliasRefusal() throws Exception {
        for (PmemBackend other : new PmemBackend[]{PmemBackend.FILE, PmemBackend.POSIX}) {
            Path directory=Files.createTempDirectory("pmem-identity-");
            Path source=directory.resolve("source.bin"), alias=directory.resolve("alias.bin");
            Path replacement=directory.resolve("replacement.bin");
            byte[] original=new byte[200_000], unrelated=new byte[original.length];
            new Random(1223).nextBytes(original); Arrays.fill(unrelated,(byte)73);
            try {
                Files.write(source,original); Files.createLink(alias,source); Files.write(replacement,unrelated);
                try (PmemRegion a=open(source,original.length,PmemBackend.FILE);
                     PmemRegion b=open(alias,original.length,other)) {
                    Files.move(replacement,source,java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    assertThrows(IllegalArgumentException.class,()->PmemTransfer.copy(a,0,b,11,170_000));
                    assertArrayEquals(original,read(a)); assertArrayEquals(original,read(b));
                    assertThrows(IllegalArgumentException.class,()->PmemTransfer.copyIfDifferent(a,0,b,11,170_000));
                    assertArrayEquals(original,Files.readAllBytes(alias));
                    assertArrayEquals(unrelated,Files.readAllBytes(source));
                }
            } finally {
                Files.deleteIfExists(replacement); Files.deleteIfExists(source); Files.deleteIfExists(alias); Files.delete(directory);
            }
        }
    }
    @Test void unknownFileKeysRefusePotentialDistinctOwnerAliases() throws Exception {
        Path archive=Files.createTempFile("pmem-null-key-", ".zip"); Files.delete(archive);
        try (java.nio.file.FileSystem fs=java.nio.file.FileSystems.newFileSystem(archive,java.util.Map.of("create","true"))) {
            Path p=fs.getPath("/a.bin"),q=fs.getPath("/b.bin");
            byte[] data=new byte[128]; for(int i=0;i<data.length;i++)data[i]=(byte)i;
            Files.write(p,data); Files.write(q,new byte[data.length]);
            assertNull(Files.readAttributes(p,java.nio.file.attribute.BasicFileAttributes.class).fileKey());
            PmemRegion first;
            try { first=open(p,data.length,PmemBackend.FILE); }
            catch (IllegalArgumentException unsupportedNoFollow) {
                // ZIP rejects NOFOLLOW_LINKS. A provider unable to honor secure open must refuse admission.
                assertThrows(IllegalArgumentException.class,()->FileChannel.open(p,StandardOpenOption.READ,
                        StandardOpenOption.WRITE,java.nio.file.LinkOption.NOFOLLOW_LINKS));
                return;
            }
            try (PmemRegion a=first;PmemRegion b=open(q,data.length,PmemBackend.FILE)) {
                PmemTransfer.copy(a,0,b,0,0);
                PmemTransfer.copy(a,0,a,1,16); // same channel remains a known owner even without a provider key
                PmemTransfer.copy(a,0,b,100,16); // absolute spans cannot overlap regardless of identity
                assertTrue(PmemTransfer.equalRange(a,0,b,100,16));
                assertThrows(IllegalStateException.class,()->PmemTransfer.copy(a,0,b,0,16));
                assertThrows(IllegalStateException.class,()->PmemTransfer.copyIfDifferent(a,0,b,0,16));
            }
        } finally { Files.deleteIfExists(archive); }
    }
    @Test void finalSymlinkIsRejectedForBothFileAccessModes() throws Exception {
        Path directory=Files.createTempDirectory("pmem-final-symlink-");
        Path regular=directory.resolve("regular.bin"),link=directory.resolve("link.bin");
        byte[] original={11,22,33,44};
        try {
            Files.write(regular,original); Files.createSymbolicLink(link,regular.getFileName());
            for(PmemAccess access:PmemAccess.values())
                assertThrows(java.io.IOException.class,()->PmemSdk.open(link,0,original.length,access,PmemBackend.FILE));
            assertArrayEquals(original,Files.readAllBytes(regular));
        } finally { Files.deleteIfExists(link);Files.deleteIfExists(regular);Files.delete(directory); }
    }
    @Test void signedLittleEndianBoundsAndLongOffsets() throws Exception {
        long[] values={Long.MIN_VALUE,Long.MIN_VALUE,-8,-1,0,0,1,Long.MAX_VALUE};
        long fileOffset=(1L<<31)+19;
        Path p=Files.createTempFile("pmem-sparse-", ".bin");
        try {
            ByteBuffer buf=ByteBuffer.allocate(values.length*8+3).order(ByteOrder.LITTLE_ENDIAN);
            buf.position(3); for(long x:values) buf.putLong(x);
            try (FileChannel c=FileChannel.open(p,StandardOpenOption.WRITE)) {
                buf.flip(); while(buf.hasRemaining()) fileOffset+=c.write(buf,fileOffset);
            }
            long begin=(1L<<31)+19;
            try(PmemRegion f=PmemSdk.open(p,begin,values.length*8+3,PmemAccess.READ_WRITE,PmemBackend.FILE)) {
                for(long key:new long[]{Long.MIN_VALUE,-9,-8,-1,0,1,2,Long.MAX_VALUE}) {
                    int lo=0,hi=0; while(lo<values.length && values[lo]<key)lo++;
                    while(hi<values.length && values[hi]<=key)hi++;
                    assertEquals(lo,PmemSearch.lowerBoundLongLE(f,3,values.length,key));
                    assertEquals(hi,PmemSearch.upperBoundLongLE(f,3,values.length,key));
                }
                assertEquals(0,PmemSearch.lowerBoundLongLE(f,f.length(),0,0));
                assertThrows(IndexOutOfBoundsException.class,()->PmemSearch.lowerBoundLongLE(f,0,Long.MAX_VALUE,0));
                assertThrows(IndexOutOfBoundsException.class,()->PmemTransfer.copy(f,Long.MAX_VALUE,f,0,8));
                assertThrows(IndexOutOfBoundsException.class,()->f.persist(1,Long.MAX_VALUE));
                assertThrows(IllegalArgumentException.class,()->PmemSearch.indexOf(f,0,0,new byte[PmemSearch.MAX_PATTERN_BYTES+1]));
            }
            assertThrows(IllegalArgumentException.class,()->PmemSdk.open(p,Long.MAX_VALUE,2,PmemAccess.READ_WRITE,PmemBackend.FILE));
            assertThrows(java.io.IOException.class,()->PmemSdk.open(p,begin,1_000,PmemAccess.READ_WRITE,PmemBackend.FILE));
        } finally { Files.deleteIfExists(p); }
    }
    @Test void readonlyAndClosedEmptyOperations() throws Exception {
        Path p=Files.createTempFile("pmem-readonly-", ".bin"); Files.write(p,new byte[32]);
        try {
            PmemRegion f=PmemSdk.open(p,0,32,PmemAccess.READ_ONLY,PmemBackend.FILE);
            assertThrows(IllegalStateException.class,()->f.write(0,new byte[0],0,0));
            assertThrows(IllegalStateException.class,()->f.persist(0,0));
            assertThrows(IllegalStateException.class,()->PmemTransfer.copy(f,0,f,0,0));
            f.close(); f.close(); assertFalse(f.isOpen());
            assertThrows(IllegalStateException.class,()->f.read(0,new byte[0],0,0));
            assertThrows(IllegalStateException.class,()->PmemSearch.indexOf(f,0,0,new byte[0]));
            assertThrows(IllegalStateException.class,()->PmemSearch.lowerBoundLongLE(f,0,0,0));
            assertThrows(IllegalStateException.class,()->PmemTransfer.equalRange(f,0,f,0,0));
            assertThrows(IndexOutOfBoundsException.class,()->f.read(-1,new byte[0],0,0));
        } finally { Files.deleteIfExists(p); }
    }
    @SuppressWarnings("try")
    @Test void concurrentCloseAndOppositeTransfersRemainSafe() throws Exception {
        Path p=Files.createTempFile("pmem-concurrent-", ".bin"),q=Files.createTempFile("pmem-concurrent-", ".bin");
        Files.write(p,new byte[131_072]); Files.write(q,new byte[131_072]);
        try(PmemRegion a=open(p,131_072,PmemBackend.FILE); PmemRegion b=open(q,131_072,PmemBackend.FILE)) {
            CountDownLatch start=new CountDownLatch(1); AtomicReference<Throwable> error=new AtomicReference<>();
            Runnable forward=()->{try {start.await();for(int i=0;i<200;i++)PmemTransfer.copy(a,0,b,0,a.length());}
                catch(IllegalStateException closed){ /* close is allowed to win */ }
                catch(Throwable t){error.compareAndSet(null,t);}};
            Runnable backward=()->{try {start.await();for(int i=0;i<200;i++)PmemTransfer.copy(b,0,a,0,a.length());}
                catch(IllegalStateException closed){ /* close is allowed to win */ }
                catch(Throwable t){error.compareAndSet(null,t);}};
            Thread t1=new Thread(forward),t2=new Thread(backward);t1.start();t2.start();start.countDown();
            a.close();t1.join(TimeUnit.SECONDS.toMillis(10));t2.join(TimeUnit.SECONDS.toMillis(10));
            assertFalse(t1.isAlive());assertFalse(t2.isAlive());assertNull(error.get());
            assertThrows(IllegalStateException.class,()->PmemTransfer.copy(a,0,b,0,0));
        } finally { Files.deleteIfExists(p); Files.deleteIfExists(q); }
    }
    @Test void boundedWorkloadObservation() throws Exception {
        Path p=Files.createTempFile("pmem-workload-", ".bin"); byte[] block=new byte[1<<20]; Arrays.fill(block,(byte)'a');
        try {
            Files.write(p,block); try(PmemRegion f=open(p,block.length,PmemBackend.FILE)) {
                long before=System.nanoTime(); long hit=0;
                for(int i=0;i<8;i++)hit+=PmemSearch.indexOf(f,0,f.length(),new byte[]{'a','a','b'});
                assertEquals(-8,hit);
                System.out.printf("FILE observation: scanned=%d bytes elapsed_ns=%d transfer_scratch_limit=65536 scan_scratch_limit=65536%n",8L*block.length,System.nanoTime()-before);
            }
        } finally { Files.deleteIfExists(p); }
    }
}
