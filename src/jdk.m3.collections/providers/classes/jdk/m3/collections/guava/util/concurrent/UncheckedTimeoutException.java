/*
 * Copyright (C) 2006 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package jdk.m3.collections.guava.util.concurrent;

import jdk.m3.collections.guava.annotations.GwtIncompatible;
import jdk.m3.collections.guava.annotations.J2ktIncompatible;

/**
 * Unchecked version of {@link java.util.concurrent.TimeoutException}.
 *
 * @author Kevin Bourrillion
 * @since 1.0
 */
@J2ktIncompatible
@GwtIncompatible
public class UncheckedTimeoutException extends RuntimeException {
  /** Constructs a new {@code UncheckedTimeoutException} with {@code null} as its detail message. */
  public UncheckedTimeoutException() {}

  /** Constructs a new {@code UncheckedTimeoutException} with the specified detail message. */
  public UncheckedTimeoutException(String message) {
    super(message);
  }

  /** Constructs a new {@code UncheckedTimeoutException} with the specified cause. */
  public UncheckedTimeoutException(Throwable cause) {
    super(cause);
  }

  /**
   * Constructs a new {@code UncheckedTimeoutException} with the specified detail message and cause.
   */
  public UncheckedTimeoutException(String message, Throwable cause) {
    super(message, cause);
  }

  private static final long serialVersionUID = 0;
}
