// SPDX-License-Identifier: Apache-2.0
#include "m3fs.h"
#include <stdio.h>
#include <stdint.h>
#define CHECK(x) do { ++checks; if (!(x)) { fprintf(stderr, "FAIL line %d: %s\n", __LINE__, #x); return 1; } } while (0)
#define OK(x) CHECK((x) == M3FS_OK)
int main(int argc, char **argv) {
  unsigned checks = 0U;
  CHECK(argc == 2);
  m3fs *fs = NULL, *snapshot = NULL, *readonly = NULL;
  OK(m3fs_open(argv[1], M3FS_OPEN_CREATE, &fs));
  OK(m3fs_create(fs, "/f", 0644U));
  OK(m3fs_mkdir(fs, "/d", 0755U));
  m3fs_stat a, b;
  OK(m3fs_stat_path(fs, "/f", &a));
  OK(m3fs_open(argv[1], M3FS_OPEN_READ_ONLY | M3FS_OPEN_READ_SNAPSHOT, &snapshot));
  OK(m3fs_set_metadata(fs, "/f", 0444U, 1234567U, M3FS_METADATA_MODE | M3FS_METADATA_MTIME));
  OK(m3fs_stat_path(fs, "/f", &b));
  CHECK(b.mode == 0444U && b.mtime_ns == 1234567U && b.generation == a.generation + 1U);
  OK(m3fs_stat_path(snapshot, "/f", &b));
  CHECK(b.mode == a.mode && b.mtime_ns == a.mtime_ns && b.generation == a.generation);
  OK(m3fs_set_metadata(fs, "/f", 0444U, 1234567U, 3U));
  OK(m3fs_stat_path(fs, "/f", &b));
  CHECK(b.generation == a.generation + 1U);
  OK(m3fs_set_metadata(fs, "/f", 0600U, UINT64_MAX, M3FS_METADATA_MODE));
  OK(m3fs_stat_path(fs, "/f", &b));
  CHECK(b.mode == 0600U && b.mtime_ns == 1234567U && b.generation == a.generation + 2U);
  OK(m3fs_set_metadata(fs, "/f", UINT32_MAX, 9U, M3FS_METADATA_MTIME));
  OK(m3fs_stat_path(fs, "/f", &b));
  CHECK(b.mode == 0600U && b.mtime_ns == 9U && b.generation == a.generation + 3U);
  OK(m3fs_set_metadata(fs, "/d", 0700U, 10U, 3U));
  OK(m3fs_stat_path(fs, "/d", &b));
  CHECK(b.mode == 0700U && b.mtime_ns == 10U);
  CHECK(m3fs_set_metadata(fs, "/f", 0U, 0U, 4U) == M3FS_INVALID);
  CHECK(m3fs_set_metadata(fs, "/f", 010000U, 0U, 1U) == M3FS_INVALID);
  CHECK(m3fs_set_metadata(fs, "/f", 0U, UINT64_MAX, 2U) == M3FS_INVALID);
  CHECK(m3fs_set_metadata(fs, "/missing", 0U, 0U, 0U) == M3FS_NOT_FOUND);
  OK(m3fs_open(argv[1], M3FS_OPEN_READ_ONLY, &readonly));
  CHECK(m3fs_set_metadata(readonly, "/f", 0U, 0U, 0U) == M3FS_READ_ONLY);
  CHECK(m3fs_capabilities() == 16383U && m3fs_feature_bits() == UINT64_C(2097151));
  m3fs_close(readonly); m3fs_close(snapshot); m3fs_close(fs);
  printf("PASS metadata checks=%u\n", checks);
  return 0;
}
