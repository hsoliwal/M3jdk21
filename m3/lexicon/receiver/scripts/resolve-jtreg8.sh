#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Resolve the executable actually supplied by the Ubuntu/Debian jtreg8 package.
# stdout: one absolute package-owned executable path; stderr: diagnostics.
set -euo pipefail
pkg='jtreg8'
min='8.2.1+1'
version="$(dpkg-query -W -f='${Version}' "$pkg")" || {
  echo "jtreg8 package is not installed" >&2
  exit 1
}
if ! dpkg --compare-versions "$version" ge "$min"; then
  echo "JDK source requires jtreg >= $min; installed $version" >&2
  exit 1
fi
# Do not use bare 'jtreg' from PATH: it might be jtreg5 or jtreg7. Package
# ownership is the authority, and different distro revisions use different
# locations/names for the jtreg8 launch script.
mapfile -t candidates < <(dpkg-query -L "$pkg" | LC_ALL=C sort -u)
for candidate in "${candidates[@]}"; do
  case "$candidate" in
    */bin/jtreg|*/bin/jtreg8|/usr/bin/jtreg|/usr/bin/jtreg8)
      if [[ -f "$candidate" && -x "$candidate" ]]; then
        # Validate that it is executable before propagating its location to
        # later steps. Retain the package-version check separately above.
        if "$candidate" -version > /dev/null 2>&1; then
          printf '%s\n' "$candidate"
          exit 0
        fi
      fi
      ;;
  esac
done
printf '%s\n' 'jtreg8 is installed but none of its package-owned launchers passed -version.' >&2
printf 'jtreg8 package version: %s\n' "$version" >&2
printf 'Available package launcher candidates:\n' >&2
printf '%s\n' "${candidates[@]}" | grep -E '/(jtreg8?|bin/)' >&2 || true
exit 1
