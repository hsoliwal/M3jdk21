// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.fs;

/** Durable object roles carried by M3FileSystem references. */
public enum M3ObjectKind {
  RAW_BYTES,
  MINDEX_FILE_IMAGE,
  PRECOMPUTE_OUTPUT,
  PRECOMPUTE_DESCRIPTOR,
  FILE_MANIFEST,
  GENERATION_ROOT,
  MINDEX_SOURCE_FACTS_IMAGE,
  PRECOMPUTE_BUNDLE_DESCRIPTOR,
  PRECOMPUTE_PIPELINE_DESCRIPTOR
}
