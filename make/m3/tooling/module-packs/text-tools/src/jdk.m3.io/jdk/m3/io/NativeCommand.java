// SPDX-License-Identifier: Apache-2.0
package jdk.m3.io;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** An exact executable and argv vector, never a shell command string. */
public record NativeCommand(Path executable, List<String> arguments) {
  public NativeCommand {
    executable = Objects.requireNonNull(executable, "executable").toAbsolutePath().normalize();
    if (!executable.isAbsolute()) throw new IllegalArgumentException("executable must be absolute");
    Objects.requireNonNull(arguments, "arguments");
    for (String argument : arguments) {
      if (argument == null || argument.indexOf('\0') >= 0) {
        throw new IllegalArgumentException("invalid argv element");
      }
    }
    arguments = List.copyOf(arguments);
  }

  /** Keeps arguments separate; shell expansion, globbing and redirection are not implicit. */
  public List<String> argv() {
    List<String> result = new ArrayList<>(arguments.size() + 1);
    result.add(executable.toString());
    result.addAll(arguments);
    return List.copyOf(result);
  }

  public static NativeCommand of(Path executable, String... arguments) {
    return new NativeCommand(executable, List.of(arguments));
  }
}
