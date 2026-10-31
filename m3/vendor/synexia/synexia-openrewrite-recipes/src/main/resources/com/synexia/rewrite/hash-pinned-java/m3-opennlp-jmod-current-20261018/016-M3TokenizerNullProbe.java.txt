/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package opennlp.tools.tokenize;

import java.lang.management.ManagementFactory;
import java.util.Objects;

/** Test-only fresh JVM probe: exact helpful NPE messages without profile-dependent fast throws. */
public final class M3TokenizerNullProbe {
  private M3TokenizerNullProbe() { }

  private static String failure(boolean baseline) {
    try {
      if (baseline) {
        M3BaselineSimpleTokenizer.INSTANCE.tokenizePos(null);
      } else {
        SimpleTokenizer.INSTANCE.tokenizePos(null);
      }
    } catch (NullPointerException exception) {
      return exception.getMessage();
    }
    throw new AssertionError("null was accepted");
  }

  public static void main(String[] args) {
    var flags = ManagementFactory.getRuntimeMXBean().getInputArguments();
    if (!flags.contains("-Xint") || !flags.contains("-XX:-OmitStackTraceInFastThrow")) {
      throw new AssertionError("the message oracle requires an isolated non-fast-throw JVM");
    }
    for (int trial = 0; trial < 20000; trial++) {
      boolean keep = (trial & 1) == 0;
      M3BaselineSimpleTokenizer.INSTANCE.setKeepNewLines(keep);
      SimpleTokenizer.INSTANCE.setKeepNewLines(keep);
      String expected = failure(true);
      String actual = failure(false);
      if (expected == null || !Objects.equals(expected, actual)) {
        throw new AssertionError("different null diagnostics: " + expected + " / " + actual);
      }
    }
    System.out.println("M3_NULL_MESSAGE_PROBE_PASS comparisons=20000");
  }
}
