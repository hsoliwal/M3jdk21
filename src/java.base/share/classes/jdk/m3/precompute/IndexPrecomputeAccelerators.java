// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;

/** Deterministic accelerator discovery and size-aware selection. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/0616288bfa7d91eb0abb754c7a29707f10318752/synexia-index-precompute/src/main/java/com/synexia/precompute/IndexPrecomputeAccelerators.java blob 7fc4c5f41bed; recipe m3/ports/precompute/recipe/planner-family-20261107/relocate.py v4
public final class IndexPrecomputeAccelerators {
  private final List<IndexPrecomputeAccelerator> accelerators;

  private IndexPrecomputeAccelerators(List<IndexPrecomputeAccelerator> values) {
    ArrayList<IndexPrecomputeAccelerator> all = new ArrayList<>(values);
    all.add(JavaIndexPrecomputeAccelerator.INSTANCE);
    all.sort(Comparator
        .comparingInt(IndexPrecomputeAccelerator::priority).reversed()
        .thenComparing(IndexPrecomputeAccelerator::id));
    java.util.HashSet<String> ids = new java.util.HashSet<>();
    for (IndexPrecomputeAccelerator accelerator : all) {
      if (!ids.add(accelerator.id())) {
        throw new IllegalArgumentException("duplicate accelerator id " + accelerator.id());
      }
    }
    this.accelerators = List.copyOf(all);
  }

  public static IndexPrecomputeAccelerators of(IndexPrecomputeAccelerator... values) {
    Objects.requireNonNull(values, "values");
    return new IndexPrecomputeAccelerators(List.of(values.clone()));
  }

  public static IndexPrecomputeAccelerators load(ClassLoader loader) {
    Objects.requireNonNull(loader, "loader");
    ArrayList<IndexPrecomputeAccelerator> values = new ArrayList<>();
    ServiceLoader.load(IndexPrecomputeAccelerator.class, loader).forEach(values::add);
    values.add(JniIndexPrecomputeAccelerator.INSTANCE);
    return new IndexPrecomputeAccelerators(values);
  }

  public List<IndexPrecomputeAccelerator> all() { return accelerators; }

  public IndexPrecomputeAccelerator select(IndexPrecomputeKernel kernel, int rows) {
    Objects.requireNonNull(kernel, "kernel");
    if (rows < 0) throw new IllegalArgumentException("negative rows");
    for (IndexPrecomputeAccelerator accelerator : accelerators) {
      if (accelerator.available()
          && accelerator.supports(kernel)
          && rows >= accelerator.minimumRows()) {
        return accelerator;
      }
    }
    return JavaIndexPrecomputeAccelerator.INSTANCE;
  }
}
