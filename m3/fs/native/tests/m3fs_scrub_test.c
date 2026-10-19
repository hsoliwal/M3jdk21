// SPDX-License-Identifier: Apache-2.0
#include "m3fs.h"

#include <sqlite3.h>

#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static int checks = 0;

static int check(int condition, const char *label) {
  ++checks;
  if (condition) return 1;
  (void)fprintf(stderr, "FAIL %s\n", label);
  return 0;
}

static void remove_database(const char *path) {
  char sidecar[1024];
  (void)remove(path);
  (void)snprintf(sidecar, sizeof(sidecar), "%s-wal", path);
  (void)remove(sidecar);
  (void)snprintf(sidecar, sizeof(sidecar), "%s-shm", path);
  (void)remove(sidecar);
}

static int sql(const char *path, const char *statement) {
  sqlite3 *db = NULL;
  if (sqlite3_open_v2(path, &db, SQLITE_OPEN_READWRITE, NULL) != SQLITE_OK) {
    sqlite3_close(db);
    return 0;
  }
  char *message = NULL;
  int rc = sqlite3_exec(db, statement, NULL, NULL, &message);
  sqlite3_free(message);
  sqlite3_close(db);
  return rc == SQLITE_OK;
}

int main(int argc, char **argv) {
  const char *path = argc > 1 ? argv[1] : "m3fs-scrub-test.sqlite";
  remove_database(path);

  m3fs *fs = NULL;
  if (!check(
          m3fs_open(path, M3FS_OPEN_CREATE, &fs) == M3FS_OK,
          "create")) return 1;
  if (!check(
          (m3fs_capabilities() & M3FS_CAP_BLOCK_SCRUB) != 0U,
          "scrub capability")) return 1;
  if (!check(m3fs_create(fs, "/data", 0644U) == M3FS_OK, "file")) return 1;

  unsigned char *block = (unsigned char *)malloc(M3FS_BLOCK_SIZE);
  if (!check(block != NULL, "allocation")) return 1;
  for (uint32_t index = 0U; index < M3FS_BLOCK_SIZE; ++index) {
    block[index] = (unsigned char)(index * 17U + 3U);
  }
  if (!check(
          m3fs_write(fs, "/data", 0U, block, M3FS_BLOCK_SIZE, NULL)
              == M3FS_OK,
          "write")) return 1;

  m3fs_scrub_report report;
  if (!check(m3fs_scrub(fs, 0U, &report) == M3FS_OK, "initial scrub")) return 1;
  if (!check(
          report.blocks_scanned == 1U
              && report.checksums_verified == 1U
              && report.checksums_missing == 0U
              && report.checksums_mismatched == 0U
              && report.checksums_backfilled == 0U,
          "initial checksum coverage")) return 1;
  m3fs_close(fs);
  fs = NULL;

  if (!check(sql(path, "DELETE FROM block_integrity"), "remove integrity row")) return 1;
  if (!check(
          m3fs_open(path, M3FS_OPEN_READ_ONLY, &fs) == M3FS_OK,
          "read-only open")) return 1;
  if (!check(m3fs_scrub(fs, 0U, &report) == M3FS_OK, "legacy scrub")) return 1;
  if (!check(
          report.blocks_scanned == 1U
              && report.checksums_verified == 0U
              && report.checksums_missing == 1U
              && report.checksums_mismatched == 0U
              && report.checksums_backfilled == 0U,
          "legacy missing coverage")) return 1;
  if (!check(
          m3fs_scrub(
              fs, M3FS_SCRUB_BACKFILL_MISSING, &report)
              == M3FS_READ_ONLY,
          "read-only backfill rejected")) return 1;
  m3fs_close(fs);
  fs = NULL;

  if (!check(m3fs_open(path, 0U, &fs) == M3FS_OK, "writable reopen")) return 1;
  if (!check(
          m3fs_scrub(
              fs, M3FS_SCRUB_BACKFILL_MISSING, &report)
              == M3FS_OK,
          "backfill")) return 1;
  if (!check(
          report.blocks_scanned == 1U
              && report.checksums_verified == 0U
              && report.checksums_missing == 1U
              && report.checksums_mismatched == 0U
              && report.checksums_backfilled == 1U,
          "backfill report")) return 1;
  if (!check(m3fs_scrub(fs, 0U, &report) == M3FS_OK, "post-backfill scrub")) return 1;
  if (!check(
          report.checksums_verified == 1U
              && report.checksums_missing == 0U
              && report.checksums_mismatched == 0U,
          "backfill retained")) return 1;
  m3fs_close(fs);
  fs = NULL;

  if (!check(
          sql(path, "UPDATE blocks SET data=zeroblob(65536) WHERE id=(SELECT min(id) FROM blocks)"),
          "inject payload corruption")) return 1;
  if (!check(
          m3fs_open(path, M3FS_OPEN_READ_ONLY, &fs) == M3FS_OK,
          "corrupt read-only open")) return 1;
  if (!check(m3fs_scrub(fs, 0U, &report) == M3FS_OK, "corrupt scrub completes")) return 1;
  if (!check(
          report.blocks_scanned == 1U
              && report.checksums_verified == 0U
              && report.checksums_missing == 0U
              && report.checksums_mismatched == 1U,
          "corruption detected")) return 1;
  m3fs_close(fs);

  free(block);
  remove_database(path);
  (void)printf("PASS %d M3FS block scrub checks\n", checks);
  return 0;
}
