// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.swap;

import com.synexia.nativeinterop.JniLibraryLoader;
import com.synexia.nativeinterop.NativeLibrarySpec;
import java.nio.file.Path;
import java.util.Objects;

/** One idempotent native-library owner shared by SQLite swap and page bulk JNI. */
final class SwapNativeLibrary {
  private static NativeLibrarySpec selected =
      NativeLibrarySpec.fromProperties(
          "synexia.mindex.swap.library",
          "synexia_mindex_swap");
  private static boolean loaded;

  private SwapNativeLibrary() {}

  static synchronized void load() {
    if (loaded) return;
    JniLibraryLoader.load(selected);
    loaded = true;
  }

  /**
   * Select an explicit library path before the first load.
   *
   * <p>This preserves the old SqliteSwapStore behavior: once the swap library has been loaded, a
   * later load request is idempotent rather than replacing the process-native implementation.</p>
   */
  static synchronized void load(Path absoluteLibrary) {
    Path checked = Objects.requireNonNull(absoluteLibrary, "absoluteLibrary");
    if (!checked.isAbsolute()) {
      throw new IllegalArgumentException("absolute library path required");
    }
    if (loaded) return;
    selected = NativeLibrarySpec.at(checked.normalize());
    JniLibraryLoader.load(selected);
    loaded = true;
  }

  static synchronized boolean loaded() {
    return loaded;
  }
}
