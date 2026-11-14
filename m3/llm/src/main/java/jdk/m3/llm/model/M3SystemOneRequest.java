// SPDX-License-Identifier: Apache-2.0
// Copyright © 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.llm.model;

import java.util.Objects;

/**
 * Provider-independent typed request with an exact immutable String state.
 *
 * <p>No default provider or built-in Jev implementation is implied. The caller supplies the
 * model name and separately establishes the model, tokenizer and execution-context versions.</p>
 */
public record M3SystemOneRequest(String model, String state, M3SystemOneSchema schema) {
  public M3SystemOneRequest {
    model = Objects.requireNonNull(model, "model");
    if (model.isBlank()) throw new IllegalArgumentException("model must not be blank");
    state = Objects.requireNonNull(state, "state");
    schema = Objects.requireNonNull(schema, "schema");
  }

  /**
   * Prepare the exact input for immutable model, tokenizer and execution-context versions.
   *
   * <p>The caller's context root must include preprocessing, policy and position/attention state
   * that affect evaluation. The configured model name and complete schema are also bound.
   * This operation neither evaluates the questions nor generates numerical encoder latents.</p>
   */
  public M3ModelInput prepare(String modelRoot, String tokenizerRoot, String contextRoot) {
    M3ModelInput.ExactHash context =
        new M3ModelInput.ExactHash("SYNEXIA_MINDEX_SYSTEM_ONE_PREPARED_CONTEXT_V1");
    context.text(model);
    context.text(M3ModelInput.requireRoot(contextRoot, "contextRoot"));
    return M3ModelInput.of(state, modelRoot, tokenizerRoot, schema.rootHash(), context.finish());
  }
}
