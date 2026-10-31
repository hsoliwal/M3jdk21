// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.fs;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class M3PrecomputePublicationDurabilityTest {
  @TempDir Path temp;
  @Test void concurrentDirectoryCreatorsAcceptOnlyRealDirectories() throws Exception {
    Path parent=temp.resolve("concurrent-shards");
    Files.createDirectory(parent);
    try(var pool=Executors.newFixedThreadPool(8)) {
      for(int round=0;round<96;round++) {
        Path shard=parent.resolve(Integer.toHexString(round));
        CountDownLatch start=new CountDownLatch(1);
        ArrayList<Future<Void>> creators=new ArrayList<>();
        for(int worker=0;worker<8;worker++) creators.add(pool.submit(()->{
          start.await(); M3DirectoryContentStore.durableCreateDirs(shard); return null;
        }));
        start.countDown();
        for(Future<Void> creator:creators) creator.get(20,TimeUnit.SECONDS);
        assertTrue(Files.isDirectory(shard,LinkOption.NOFOLLOW_LINKS));
      }
    }
    Path file=parent.resolve("file");Files.writeString(file,"preserve");
    assertThrows(IOException.class,()->M3DirectoryContentStore.durableCreateDirs(file));
    Path link=parent.resolve("link");Files.createSymbolicLink(link,parent.resolve("0"));
    assertThrows(IOException.class,()->M3DirectoryContentStore.durableCreateDirs(link));
    assertEquals("preserve",Files.readString(file));assertTrue(Files.isSymbolicLink(link));
  }
  @Test void competingCreateOnlyPublicationsPreserveExactlyOneWinner() throws Exception {
    M3DirectoryContentStore store=new M3DirectoryContentStore(temp.resolve("store"));
    M3PrecomputeCache a=new M3PrecomputeCache(store),b=new M3PrecomputeCache(store);
    M3PrecomputeKey key=new M3PrecomputeKey("race","cache",1,List.of(),Map.of());
    M3PrecomputeResult left=M3PrecomputeResult.value(key,store.put(new byte[]{1},M3ObjectKind.PRECOMPUTE_OUTPUT));
    M3PrecomputeResult right=M3PrecomputeResult.value(key,store.put(new byte[]{2},M3ObjectKind.PRECOMPUTE_OUTPUT));
    CountDownLatch start=new CountDownLatch(1);
    try(var pool=Executors.newFixedThreadPool(2)) {
      Future<Boolean> first=pool.submit(()->{start.await();try{a.publish(left);return true;}catch(IOException conflict){return false;}});
      Future<Boolean> second=pool.submit(()->{start.await();try{b.publish(right);return true;}catch(IOException conflict){return false;}});
      start.countDown(); boolean won=first.get(20,TimeUnit.SECONDS); assertNotEquals(won,second.get(20,TimeUnit.SECONDS));
      M3PrecomputeResult winner=won?left:right;
      assertEquals(winner.output(),a.lookup(key).orElseThrow().output());
      assertEquals(winner.output(),b.lookup(key).orElseThrow().output());
      Path pointer=pointer(store,key); byte[] frozen=Files.readAllBytes(pointer);
      assertEquals(65,frozen.length); a.publish(winner); assertArrayEquals(frozen,Files.readAllBytes(pointer));
      try(var files=Files.list(store.root().resolve("staging"))){assertEquals(0,files.count());}
    }
  }
  @Test void oversizedOrSymlinkPointersAreRefusedWithoutRetargeting() throws Exception {
    M3DirectoryContentStore store=new M3DirectoryContentStore(temp.resolve("invalid"));
    M3PrecomputeCache cache=new M3PrecomputeCache(store);
    M3PrecomputeKey key=new M3PrecomputeKey("pointer","shape",1,List.of(),Map.of());
    Path pointer=pointer(store,key);Files.createDirectories(pointer.getParent());Files.write(pointer,new byte[4096]);
    assertThrows(IOException.class,()->cache.lookup(key));
    Files.delete(pointer);Path external=temp.resolve("external");Files.writeString(external,"preserve",StandardCharsets.US_ASCII);
    Files.createSymbolicLink(pointer,external);
    assertThrows(IOException.class,()->cache.lookup(key));
    assertThrows(IOException.class,()->cache.publish(M3PrecomputeResult.empty(key)));
    assertEquals("preserve",Files.readString(external));assertTrue(Files.isSymbolicLink(pointer));
  }
  private static Path pointer(M3DirectoryContentStore store,M3PrecomputeKey key) {
    String hex=key.id().hex();return store.root().resolve("precompute-index").resolve(hex.substring(0,2)).resolve(hex.substring(2)+".ref");
  }
}
