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

package probe;

import opennlp.tools.util.ext.ProviderSpec;
import opennlp.tools.util.ext.Providers;

/** Regression for generic ServiceLoader calls from a named OpenNLP module. */
public final class CustomProviderProbe {
  public static void main(String[] args) throws Exception {
    Module owner = Providers.class.getModule();
    if (!owner.isNamed()) {
      throw new AssertionError("OpenNLP must be a named module in this probe");
    }
    ProviderSpec spec = ProviderSpec.empty();
    CustomProvider provider = Providers.of(CustomProvider.class).select(spec);
    if (!"external-provider-created".equals(provider.create(spec))) {
      throw new AssertionError("external SPI resolution returned the wrong provider");
    }
    if (!owner.canUse(CustomProvider.class)) {
      throw new AssertionError("OpenNLP did not register the dynamically supplied SPI");
    }
    System.out.println("OPENNLP_CUSTOM_SPI_PASS");
  }
}
