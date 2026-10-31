## HumbleUI Skija 0.143.18 and HumbleUI Types 0.2.0

### Origin

The Java sources of the module `jdk.m3.graphics` are derived from:

- HumbleUI Skija 0.143.18 (https://github.com/HumbleUI/Skija), `shared/java`: 217 types.
- HumbleUI Types 0.2.0 (https://github.com/HumbleUI/Types), sources of `io.github.humbleui:types:0.2.0`: 7 types.

Both are licensed under the Apache License, Version 2.0.

### Changes made by Synexia (2026-10-31)

The changes were mechanical, and the tools that made them are in `m3/tooling/skija-absorb`:

- Lombok annotations were expanded with `lombok.jar delombok` 1.18.48, then the generated markers were removed. One helper
  class was added, `jdk.m3.graphics.impl.M3SneakyThrow`.
- The JetBrains documentation annotations were removed: `@ApiStatus`, `@NotNull`, `@Nullable` and `@Contract`.
- Packages were renamed: `io.github.humbleui.skija[.x]` and `io.github.humbleui.types` became `jdk.m3.graphics[.x]`, and
  `io.github.humbleui.skija.impl` became `jdk.m3.graphics.impl`.
- Every type `T` was renamed `M3T`.

The public API is otherwise the donor's, member for member: the javap surface is equal after the rename.

### Apache License 2.0

```
                                 Apache License
                           Version 2.0, January 2004
                        http://www.apache.org/licenses/

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

The full text of the license is in the Skija repository's LICENSE file, and is reproduced in this repository at
`m3/tooling/skija-absorb/LICENSE-Apache-2.0.txt`.
