// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.ast;

import com.synexia.indexstring.MatIndexString;
import java.util.Objects;
import javax.tools.Diagnostic;

/** Immutable indexed compiler diagnostic captured while building a partial or complete MIndexAST. */
public record MIndexASTDiagnostic(
    Diagnostic.Kind kind,
    long line,
    long column,
    long startPosition,
    long endPosition,
    MatIndexString code,
    MatIndexString message) {

  public MIndexASTDiagnostic {
    Objects.requireNonNull(kind, "kind");
    Objects.requireNonNull(code, "code");
    Objects.requireNonNull(message, "message");
  }
}
