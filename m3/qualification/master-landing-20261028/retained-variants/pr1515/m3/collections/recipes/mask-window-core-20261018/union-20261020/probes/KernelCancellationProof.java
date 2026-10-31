// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;
import java.nio.file.*;
import java.util.concurrent.CancellationException;
public final class KernelCancellationProof {
 private KernelCancellationProof() { }
 public static void main(String[] args) {
  Path root=Path.of(args[0]);
  M3GpuToolJoinManifest.Resources resolver=n->Files.newInputStream(root.resolve(n.substring(6)));
  try { M3GpuToolJoinManifest.load("/task/",resolver,()->{throw new CancellationException("synthetic");});throw new AssertionError(); }
  catch(CancellationException expected){ }
  Thread.currentThread().interrupt();
  try {M3GpuToolJoinManifest.load("/task/",resolver,()->{});throw new AssertionError();}
  catch(CancellationException expected){if(!Thread.currentThread().isInterrupted())throw new AssertionError();}
  finally{Thread.interrupted();}
  System.out.println("KERNEL_CANCEL checks=2 PASS; not native or live-wrapper cancellation");
 }
}
