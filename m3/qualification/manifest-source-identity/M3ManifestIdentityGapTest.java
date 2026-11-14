// Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0
import jdk.m3.precompute.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
public final class M3ManifestIdentityGapTest {
 @Test public void sourceIdentityMustBeUnique() {
  var descriptors=List.of(new IndexPrecomputeManifest.SourceDescriptor("same","a".repeat(64),"fixture"),
      new IndexPrecomputeManifest.SourceDescriptor("same","b".repeat(64),"fixture"));
  assertThrows(IllegalArgumentException.class,()->new IndexPrecomputeManifest("g",0,0,false,0,0,0,false,descriptors,List.of(),Map.of(),0,List.of(),Set.of(),Map.of(),Map.of()));
 }
}
