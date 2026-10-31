// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring;

import com.synexia.indexstring.fs.M3ContentId;
import com.synexia.indexstring.fs.M3PrecomputeKey;
import com.synexia.indexstring.precompute.MIndexPrecomputePasses;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Exact code-unit identity and explicit compilation context over the existing FS key format. */
final class MIndexRegexPrecomputeKey {
  private static final String PROGRAM_OWNER = "9999e8afe1ec44de58edd644c0ca278415fb7a379f6cdacfe9dbf7011ed77131";
  private static final String CODEC_OWNER = "09f4ef3d52a432b5a46bd784a91728352d121241dca9ab010df7a06f38d01dc7";
  private MIndexRegexPrecomputeKey() {}

  static M3PrecomputeKey key(MIndexRegexPrecompute.Request request) {
    String runtime = System.getProperty("java.vendor", "") + "\0"
        + System.getProperty("java.runtime.version", "");
    return new M3PrecomputeKey("regex-execution", "mindex-regex-full-tables", 1,
        List.of(M3ContentId.parse(MIndexPrecomputePasses.sha256Utf16(request.expression()))),
        Map.of("programOwnerSha256", PROGRAM_OWNER, "codecOwnerSha256", CODEC_OWNER,
            "semantics", request.semantics().name(),
            "maxStates", Integer.toString(request.maxStates()),
            "compilerRevision", request.compilerRevision(),
            "codecRevision", request.codecRevision(),
            "runtimeUtf16Sha256", MIndexPrecomputePasses.sha256Utf16(runtime)));
  }

  static String revision(String value) {
    String checked = Objects.requireNonNull(value, "revision");
    if (checked.isEmpty() || checked.length() > 256) {
      throw new IllegalArgumentException("revision length");
    }
    for (int index = 0; index < checked.length(); index++) {
      char unit = checked.charAt(index);
      if (unit < '!' || unit > '~') throw new IllegalArgumentException("revision must be ASCII");
    }
    return checked;
  }
}
