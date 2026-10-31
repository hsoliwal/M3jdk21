// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.runtime.test;

import java.io.*;
import java.lang.module.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.*;
import com.m3.file.M3ModuleResources;
import com.m3.eclipse.connect.M3ModuleConnector;
import com.m3.collections.M3IndexMaskCompactor.Backend;
import com.m3.runtime.spring.M3ResourcePatternResolver;
public class M3ModuleLifecycleReviewTest {
  private static Backend backend() {
    return Boolean.getBoolean("m3.module.resources.native-required") ? Backend.NATIVE_REQUIRED : Backend.JAVA;
  }
  @org.junit.jupiter.api.Test void directoryAliasContract() throws Exception { directoryAlias(); }
  @org.junit.jupiter.api.Test void reentrantOpenContract() throws Exception { reentrantOpen(); }
  @org.junit.jupiter.api.Test void manifestCleanupContract() throws Exception { manifestCleanup(); }
  @org.junit.jupiter.api.Test void removedResourceContract() throws Exception { removedResource(); }

  static ModuleReference ref(ModuleReader reader) {
    return new ModuleReference(ModuleDescriptor.newModule("review.fixture").build(), URI.create("memory:/fixture")) {
      public ModuleReader open() {return reader;}
    };
  }
  static abstract class Reader implements ModuleReader {
    final String[] names;
    Reader(String... names) {this.names=names;}
    public Stream<String> list() {return Arrays.stream(names);}
    public Optional<URI> find(String name) {return Optional.of(URI.create("file:/"+name));}
    public void close() throws IOException {}
  }
  static void directoryAlias() throws Exception {
    var connector = new M3ModuleConnector(backend());
    connector.register("m3:alias", ref(new Reader("assets/") {}));
    var content = connector.connect("m3:alias").orElseThrow().getContent();
    content.open();
    String actual = content.getEntry("assets").orElseThrow().getName();
    content.close();
    if (!actual.equals("assets/")) throw new AssertionError("directory alias: expected assets/, actual "+actual);
  }
  static void reentrantOpen() throws Exception {
    var holder = new AtomicReference<M3ModuleResources>();
    var closes = new AtomicInteger();
    var reader = new Reader("payload") {
      public Optional<InputStream> open(String name) throws IOException {
        holder.get().close();
        return Optional.of(new ByteArrayInputStream(new byte[]{1}) {public void close(){closes.incrementAndGet();}});
      }
    };
    var owner = M3ModuleResources.open(ref(reader));
    holder.set(owner);
    InputStream returned = null;
    try { returned = owner.openStream("payload"); }
    catch (java.nio.channels.ClosedChannelException expected) {}
    owner.close();
    int actualCloses = closes.get();
    boolean returnedHandle = returned != null;
    if (returned != null) returned.close();
    if (returnedHandle || actualCloses != 1) throw new AssertionError("reentrant open: returnedHandle="+returnedHandle+", raw closes before caller cleanup="+actualCloses);
  }
  static void manifestCleanup() throws Exception {
    var connector = new M3ModuleConnector(backend());
    var original = new IOException("original manifest read");
    var cleanup = new IllegalStateException("reader cleanup");
    connector.register("m3:failure", ref(new Reader("META-INF/MANIFEST.MF") {
      public Optional<InputStream> open(String name) {return Optional.of(new InputStream(){public int read() throws IOException {throw original;}});}
      public void close() {throw cleanup;}
    }));
    Throwable actual = null;
    try {connector.connect("m3:failure").orElseThrow().getContent().open();}
    catch(Throwable caught){actual=caught;}
    if (actual != original || !Arrays.asList(actual.getSuppressed()).contains(cleanup)) throw new AssertionError("manifest cleanup: expected original IOException with suppressed cleanup, actual "+actual, actual);
  }
  static void removedResource() throws Exception {
    var present = new AtomicBoolean(true);
    var owner = M3ModuleResources.open(ref(new Reader("payload") {
      public Optional<URI> find(String name) {return present.get()?super.find(name):Optional.empty();}
      public Optional<InputStream> open(String name) {return present.get()?Optional.of(new ByteArrayInputStream(new byte[]{1})):Optional.empty();}
    }));
    var resolver = new M3ResourcePatternResolver(List.of(owner), M3ModuleLifecycleReviewTest.class.getClassLoader(), backend());
    var resource = resolver.moduleResources("payload")[0];
    present.set(false);
    boolean exists = resource.exists(), readable = resource.isReadable();
    boolean missing = owner.find("payload").isEmpty();
    owner.close();
    if (!missing || exists || readable) throw new AssertionError("removed resource: exists="+exists+", readable="+readable+", reader missing="+missing);
  }
  interface Check {void run() throws Exception;}
  public static void main(String[] args) throws Exception {
    int failed=0;
    Check[] checks={M3ModuleLifecycleReviewTest::directoryAlias,M3ModuleLifecycleReviewTest::reentrantOpen,M3ModuleLifecycleReviewTest::manifestCleanup,M3ModuleLifecycleReviewTest::removedResource};
    String[] names={"directoryAlias","reentrantOpen","manifestCleanup","removedResource"};
    for(int i=0;i<checks.length;i++) {
      try{checks[i].run();System.out.println("GREEN "+names[i]);}
      catch(AssertionError failure){failed++;System.out.println("RED "+names[i]+": "+failure.getMessage());}
    }
    if(failed!=0) throw new AssertionError("Review witnesses failed: "+failed+"/"+checks.length);
  }
}
