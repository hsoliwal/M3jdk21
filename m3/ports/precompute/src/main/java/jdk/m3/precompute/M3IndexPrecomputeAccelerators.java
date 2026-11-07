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
public final class M3IndexPrecomputeAccelerators {
  private final List<M3IndexPrecomputeAccelerator> accelerators;

  private M3IndexPrecomputeAccelerators(List<M3IndexPrecomputeAccelerator> values) {
    ArrayList<M3IndexPrecomputeAccelerator> all = new ArrayList<>(values);
    all.add(M3JavaIndexPrecomputeAccelerator.INSTANCE);
    all.sort(Comparator
        .comparingInt(M3IndexPrecomputeAccelerator::priority).reversed()
        .thenComparing(M3IndexPrecomputeAccelerator::id));
    java.util.HashSet<String> ids = new java.util.HashSet<>();
    for (M3IndexPrecomputeAccelerator accelerator : all) {
      if (!ids.add(accelerator.id())) {
        throw new IllegalArgumentException("duplicate accelerator id " + accelerator.id());
      }
    }
    this.accelerators = List.copyOf(all);
  }

  public static M3IndexPrecomputeAccelerators of(M3IndexPrecomputeAccelerator... values) {
    Objects.requireNonNull(values, "values");
    return new M3IndexPrecomputeAccelerators(List.of(values.clone()));
  }

  public static M3IndexPrecomputeAccelerators load(ClassLoader loader) {
    Objects.requireNonNull(loader, "loader");
    ArrayList<M3IndexPrecomputeAccelerator> values = new ArrayList<>();
    ServiceLoader.load(M3IndexPrecomputeAccelerator.class, loader).forEach(values::add);
    values.add(M3JniIndexPrecomputeAccelerator.INSTANCE);
    return new M3IndexPrecomputeAccelerators(values);
  }

  public List<M3IndexPrecomputeAccelerator> all() { return accelerators; }

  public M3IndexPrecomputeAccelerator select(M3IndexPrecomputeKernel kernel, int rows) {
    Objects.requireNonNull(kernel, "kernel");
    if (rows < 0) throw new IllegalArgumentException("negative rows");
    for (M3IndexPrecomputeAccelerator accelerator : accelerators) {
      if (accelerator.available()
          && accelerator.supports(kernel)
          && rows >= accelerator.minimumRows()) {
        return accelerator;
      }
    }
    return M3JavaIndexPrecomputeAccelerator.INSTANCE;
  }
}
