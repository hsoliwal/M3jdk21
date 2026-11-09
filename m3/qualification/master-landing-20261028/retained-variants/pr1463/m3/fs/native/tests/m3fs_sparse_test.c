// SPDX-License-Identifier: Apache-2.0
#include "m3fs.h"

#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static unsigned checks;
#define CHECK(c) do { ++checks; if (!(c)) {   fprintf(stderr, "FAIL line %d: %s\n", __LINE__, #c); exit(1); } } while (0)
#define OK(c) CHECK((c) == M3FS_OK)

typedef struct ranges {
  uint64_t values[16];
  uint32_t pairs;
  int abort_status;
} ranges;

static int collect_range(void *context, uint64_t offset, uint64_t length) {
  ranges *out = (ranges *)context;
  if (out->abort_status != 0) return out->abort_status;
  CHECK(length > 0U);
  CHECK(out->pairs < 8U);
  if (out->pairs != 0U) {
    CHECK(out->values[(out->pairs - 1U) * 2U]
        + out->values[(out->pairs - 1U) * 2U + 1U] < offset);
  }
  out->values[out->pairs * 2U] = offset;
  out->values[out->pairs * 2U + 1U] = length;
  out->pairs++;
  return M3FS_OK;
}

static void expect_byte(
    m3fs *fs, const char *path, uint64_t offset, unsigned char expected) {
  unsigned char value = 0xffU;
  uint32_t read = 0U;
  OK(m3fs_read(fs, path, offset, &value, 1U, &read));
  CHECK(read == 1U);
  CHECK(value == expected);
}

int main(int argc, char **argv) {
  CHECK(argc == 2);
  m3fs *fs = NULL;
  OK(m3fs_open(argv[1], M3FS_OPEN_CREATE, &fs));

  uint32_t caps = m3fs_capabilities();
  CHECK((caps & M3FS_CAP_PUNCH_HOLE) != 0U);
  CHECK((caps & M3FS_CAP_ALLOCATED_RANGES) != 0U);
  uint64_t features = m3fs_feature_bits();
  CHECK((features & M3FS_FEATURE_HOLE_PUNCH) != 0U);
  CHECK((features & M3FS_FEATURE_ALLOCATED_RANGE_QUERY) != 0U);

  OK(m3fs_create(fs, "/f", 0644U));
  unsigned char *block = (unsigned char *)malloc(M3FS_BLOCK_SIZE);
  CHECK(block != NULL);
  memset(block, 'A', M3FS_BLOCK_SIZE);
  OK(m3fs_write(fs, "/f", 0U, block, M3FS_BLOCK_SIZE, NULL));
  memset(block, 'B', M3FS_BLOCK_SIZE);
  OK(m3fs_write(
      fs, "/f", M3FS_BLOCK_SIZE, block, M3FS_BLOCK_SIZE, NULL));
  memset(block, 'C', M3FS_BLOCK_SIZE);
  OK(m3fs_write(
      fs, "/f", 2U * (uint64_t)M3FS_BLOCK_SIZE,
      block, M3FS_BLOCK_SIZE, NULL));
  free(block);

  m3fs_stat before;
  OK(m3fs_stat_path(fs, "/f", &before));
  CHECK(before.size == 3U * (uint64_t)M3FS_BLOCK_SIZE);

  ranges all = {{0}, 0U, 0};
  OK(m3fs_allocated_ranges(
      fs, "/f", 0U, before.size, collect_range, &all));
  CHECK(all.pairs == 1U);
  CHECK(all.values[0] == 0U && all.values[1] == before.size);

  m3fs_stats stats_before, stats_after;
  OK(m3fs_get_stats(fs, &stats_before));
  CHECK(stats_before.unique_blocks == 3U);
  CHECK(stats_before.block_references == 3U);

  OK(m3fs_punch_hole(
      fs,
      "/f",
      M3FS_BLOCK_SIZE,
      M3FS_BLOCK_SIZE));
  m3fs_stat after_middle;
  OK(m3fs_stat_path(fs, "/f", &after_middle));
  CHECK(after_middle.size == before.size);
  CHECK(after_middle.generation == before.generation + 1U);
  expect_byte(fs, "/f", M3FS_BLOCK_SIZE, 0U);
  expect_byte(fs, "/f", 2U * (uint64_t)M3FS_BLOCK_SIZE, (unsigned char)'C');

  ranges split = {{0}, 0U, 0};
  OK(m3fs_allocated_ranges(
      fs, "/f", 0U, before.size, collect_range, &split));
  CHECK(split.pairs == 2U);
  CHECK(split.values[0] == 0U);
  CHECK(split.values[1] == M3FS_BLOCK_SIZE);
  CHECK(split.values[2] == 2U * (uint64_t)M3FS_BLOCK_SIZE);
  CHECK(split.values[3] == M3FS_BLOCK_SIZE);

  OK(m3fs_get_stats(fs, &stats_after));
  CHECK(stats_after.unique_blocks == 2U);
  CHECK(stats_after.block_references == 2U);

  /* Partial punch stays allocated but zeroes only the selected bytes. */
  OK(m3fs_punch_hole(fs, "/f", 100U, 1000U));
  expect_byte(fs, "/f", 99U, (unsigned char)'A');
  expect_byte(fs, "/f", 100U, 0U);
  expect_byte(fs, "/f", 1099U, 0U);
  expect_byte(fs, "/f", 1100U, (unsigned char)'A');
  ranges clipped = {{0}, 0U, 0};
  OK(m3fs_allocated_ranges(
      fs,
      "/f",
      M3FS_BLOCK_SIZE / 2U,
      2U * (uint64_t)M3FS_BLOCK_SIZE,
      collect_range,
      &clipped));
  CHECK(clipped.pairs == 2U);
  CHECK(clipped.values[0] == M3FS_BLOCK_SIZE / 2U);
  CHECK(clipped.values[1] == M3FS_BLOCK_SIZE / 2U);
  CHECK(clipped.values[2] == 2U * (uint64_t)M3FS_BLOCK_SIZE);
  CHECK(clipped.values[3] == M3FS_BLOCK_SIZE / 2U);

  /* Hole punching a clone must not mutate the source's shared block reference. */
  uint64_t clone = 0U;
  OK(m3fs_clone_file(fs, "/f", "/clone", &clone));
  CHECK(clone > 1U);
  OK(m3fs_punch_hole(
      fs,
      "/clone",
      2U * (uint64_t)M3FS_BLOCK_SIZE,
      M3FS_BLOCK_SIZE));
  expect_byte(fs, "/clone", 2U * (uint64_t)M3FS_BLOCK_SIZE, 0U);
  expect_byte(fs, "/f", 2U * (uint64_t)M3FS_BLOCK_SIZE, (unsigned char)'C');

  m3fs_stat stable;
  OK(m3fs_stat_path(fs, "/f", &stable));
  OK(m3fs_punch_hole(fs, "/f", stable.size + 10U, 100U));
  m3fs_stat stable_after;
  OK(m3fs_stat_path(fs, "/f", &stable_after));
  CHECK(stable_after.generation == stable.generation);

  ranges aborted = {{0}, 0U, M3FS_BUSY};
  CHECK(m3fs_allocated_ranges(
      fs, "/f", 0U, stable.size, collect_range, &aborted) == M3FS_BUSY);
  CHECK(aborted.pairs == 0U);

  OK(m3fs_verify(fs));
  m3fs_close(fs);

  OK(m3fs_open(argv[1], M3FS_OPEN_READ_ONLY, &fs));
  CHECK(m3fs_punch_hole(fs, "/f", 0U, 1U) == M3FS_READ_ONLY);
  ranges readonly = {{0}, 0U, 0};
  OK(m3fs_allocated_ranges(
      fs, "/f", 0U, stable.size, collect_range, &readonly));
  CHECK(readonly.pairs == 2U);
  m3fs_close(fs);

  printf("PASS sparse-native checks=%u hole-punch=PASS allocated-ranges=PASS cow=PASS\n", checks);
  return 0;
}
