// SPDX-License-Identifier: Apache-2.0
#include "m3fs.h"

#include <errno.h>
#include <inttypes.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static void usage(const char *argv0) {
  static const char *const commands[] = {
      "format DB", "mkdir DB PATH [MODE]", "touch DB PATH [MODE]",
      "put DB PATH HOSTFILE", "get DB PATH HOSTFILE|-", "write DB PATH OFFSET TEXT",
      "truncate DB PATH SIZE", "ls DB PATH", "stat DB PATH", "link DB EXISTING NEW",
      "symlink DB TARGET LINK", "readlink DB PATH", "rename DB FROM TO", "unlink DB PATH",
      "rmdir DB PATH", "verify DB", "scrub DB [--backfill]", "stats DB", "checkpoint DB",
      "clone DB EXISTING NEW", "ls-page DB PATH LIMIT [AFTER_NAME]"};
  fputs("usage:\n", stderr);
  for (size_t i = 0U; i < sizeof(commands) / sizeof(commands[0]); ++i) {
    fprintf(stderr, "  %s %s\n", argv0, commands[i]);
  }
}

static int fail_status(const char *operation, int status) {
  fprintf(stderr, "%s: %s (%d)\n", operation, m3fs_status_string(status), status);
  return 1;
}

static int parse_u64(const char *text, uint64_t *out) {
  if (text == NULL || text[0] == '\0' || text[0] == '-') return 0;
  errno = 0;
  char *end = NULL;
  unsigned long long value = strtoull(text, &end, 0);
  if (errno != 0 || end == text || *end != '\0') return 0;
  *out = (uint64_t)value;
  return 1;
}

static const char *kind_name(uint32_t kind) {
  switch (kind) {
    case M3FS_KIND_FILE: return "file";
    case M3FS_KIND_DIRECTORY: return "dir";
    case M3FS_KIND_SYMLINK: return "symlink";
    default: return "?";
  }
}

static int print_entry(
    void *context,
    const char *name,
    uint32_t name_bytes,
    const m3fs_stat *stat) {
  (void)context;
  printf(
      "%s\t%" PRIu64 "\t%" PRIu64 "\t%.*s\n",
      kind_name(stat->kind),
      stat->inode,
      stat->size,
      (int)name_bytes,
      name);
  return M3FS_OK;
}

static int command_put(m3fs *fs, const char *path, const char *host) {
  FILE *input = fopen(host, "rb");
  if (input == NULL) {
    perror("open input");
    return 1;
  }
  int status = m3fs_create(fs, path, 0644U);
  if (status != M3FS_OK && status != M3FS_EXISTS) {
    fclose(input);
    return fail_status("create", status);
  }
  status = m3fs_truncate(fs, path, 0U);
  if (status != M3FS_OK) {
    fclose(input);
    return fail_status("truncate", status);
  }

  unsigned char *buffer = (unsigned char *)malloc(1024U * 1024U);
  if (buffer == NULL) {
    fclose(input);
    fputs("allocation failed\n", stderr);
    return 1;
  }
  uint64_t offset = 0U;
  int result = 0;
  for (;;) {
    size_t count = fread(buffer, 1U, 1024U * 1024U, input);
    if (count != 0U) {
      status = m3fs_write(
          fs, path, offset, buffer, (uint32_t)count, NULL);
      if (status != M3FS_OK) {
        result = fail_status("write", status);
        break;
      }
      offset += (uint64_t)count;
    }
    if (count < 1024U * 1024U) {
      if (ferror(input)) {
        perror("read input");
        result = 1;
      }
      break;
    }
  }
  free(buffer);
  fclose(input);
  return result;
}

static int command_get(m3fs *fs, const char *path, const char *host) {
  int use_stdout = strcmp(host, "-") == 0;
  FILE *output = use_stdout ? stdout : fopen(host, "wb");
  if (output == NULL) {
    perror("open output");
    return 1;
  }
  unsigned char *buffer = (unsigned char *)malloc(1024U * 1024U);
  if (buffer == NULL) {
    if (!use_stdout) fclose(output);
    fputs("allocation failed\n", stderr);
    return 1;
  }
  uint64_t offset = 0U;
  int result = 0;
  for (;;) {
    uint32_t count = 0U;
    int status = m3fs_read(fs, path, offset, buffer, 1024U * 1024U, &count);
    if (status != M3FS_OK) {
      result = fail_status("read", status);
      break;
    }
    if (count == 0U) break;
    if (fwrite(buffer, 1U, count, output) != count) {
      perror("write output");
      result = 1;
      break;
    }
    offset += count;
  }
  free(buffer);
  if (!use_stdout && fclose(output) != 0) result = 1;
  return result;
}

int main(int argc, char **argv) {
  if (argc < 3) {
    usage(argv[0]);
    return 2;
  }
  const char *command = argv[1];
  const char *database = argv[2];
  uint32_t flags = strcmp(command, "format") == 0 ? M3FS_OPEN_CREATE : 0U;
  m3fs *fs = NULL;
  int status = m3fs_open(database, flags, &fs);
  if (status != M3FS_OK) return fail_status("open", status);

  int result = 0;
  if (strcmp(command, "format") == 0) {
    status = m3fs_verify(fs);
    if (status != M3FS_OK) result = fail_status("verify", status);
  } else if (strcmp(command, "mkdir") == 0 && (argc == 4 || argc == 5)) {
    uint64_t mode = 0755U;
    if (argc == 5 && !parse_u64(argv[4], &mode)) result = 2;
    else {
      status = m3fs_mkdir(fs, argv[3], (uint32_t)mode);
      if (status != M3FS_OK) result = fail_status("mkdir", status);
    }
  } else if (strcmp(command, "touch") == 0 && (argc == 4 || argc == 5)) {
    uint64_t mode = 0644U;
    if (argc == 5 && !parse_u64(argv[4], &mode)) result = 2;
    else {
      status = m3fs_create(fs, argv[3], (uint32_t)mode);
      if (status != M3FS_OK && status != M3FS_EXISTS) {
        result = fail_status("touch", status);
      }
    }
  } else if (strcmp(command, "put") == 0 && argc == 5) {
    result = command_put(fs, argv[3], argv[4]);
  } else if (strcmp(command, "get") == 0 && argc == 5) {
    result = command_get(fs, argv[3], argv[4]);
  } else if (strcmp(command, "write") == 0 && argc == 6) {
    uint64_t offset = 0U;
    if (!parse_u64(argv[4], &offset)) result = 2;
    else {
      status = m3fs_write(
          fs, argv[3], offset, argv[5], (uint32_t)strlen(argv[5]), NULL);
      if (status != M3FS_OK) result = fail_status("write", status);
    }
  } else if (strcmp(command, "truncate") == 0 && argc == 5) {
    uint64_t size = 0U;
    if (!parse_u64(argv[4], &size)) result = 2;
    else {
      status = m3fs_truncate(fs, argv[3], size);
      if (status != M3FS_OK) result = fail_status("truncate", status);
    }
  } else if (strcmp(command, "ls") == 0 && argc == 4) {
    status = m3fs_list(fs, argv[3], print_entry, NULL);
    if (status != M3FS_OK) result = fail_status("ls", status);
  } else if (strcmp(command, "stat") == 0 && argc == 4) {
    m3fs_stat stat;
    status = m3fs_stat_path(fs, argv[3], &stat);
    if (status != M3FS_OK) result = fail_status("stat", status);
    else {
      printf(
          "kind=%s inode=%" PRIu64 " size=%" PRIu64
          " nlink=%" PRIu32 " mode=%" PRIu32
          " generation=%" PRIu64 " mtime_ns=%" PRIu64 "\n",
          kind_name(stat.kind), stat.inode, stat.size, stat.nlink, stat.mode,
          stat.generation, stat.mtime_ns);
    }
  } else if (strcmp(command, "clone") == 0 && argc == 5) {
    uint64_t inode = 0U;
    status = m3fs_clone_file(fs, argv[3], argv[4], &inode);
    if (status != M3FS_OK) result = fail_status("clone", status);
    else printf("inode=%" PRIu64 "\n", inode);
  } else if (strcmp(command, "ls-page") == 0 && (argc == 5 || argc == 6)) {
    uint64_t limit = 0U;
    if (!parse_u64(argv[4], &limit) || limit == 0U || limit > M3FS_MAX_PAGE_ENTRIES) {
      result = 2;
    } else {
      uint32_t count = 0U, more = 0U;
      status = m3fs_list_page(fs, argv[3], argc == 6 ? argv[5] : NULL,
          (uint32_t)limit, print_entry, NULL, &count, &more);
      if (status != M3FS_OK) result = fail_status("ls-page", status);
      else fprintf(stderr, "entries=%" PRIu32 " has_more=%" PRIu32 "\n", count, more);
    }
  } else if (strcmp(command, "link") == 0 && argc == 5) {
    status = m3fs_link(fs, argv[3], argv[4]);
    if (status != M3FS_OK) result = fail_status("link", status);
  } else if (strcmp(command, "symlink") == 0 && argc == 5) {
    status = m3fs_symlink(fs, argv[3], argv[4]);
    if (status != M3FS_OK) result = fail_status("symlink", status);
  } else if (strcmp(command, "readlink") == 0 && argc == 4) {
    char target[M3FS_MAX_PATH_BYTES + 1U];
    uint32_t bytes = 0U;
    status = m3fs_readlink(
        fs, argv[3], target, M3FS_MAX_PATH_BYTES, &bytes);
    if (status != M3FS_OK) result = fail_status("readlink", status);
    else {
      target[bytes] = '\0';
      printf("%s\n", target);
    }
  } else if (strcmp(command, "rename") == 0 && argc == 5) {
    status = m3fs_rename(fs, argv[3], argv[4]);
    if (status != M3FS_OK) result = fail_status("rename", status);
  } else if (strcmp(command, "unlink") == 0 && argc == 4) {
    status = m3fs_unlink(fs, argv[3]);
    if (status != M3FS_OK) result = fail_status("unlink", status);
  } else if (strcmp(command, "rmdir") == 0 && argc == 4) {
    status = m3fs_rmdir(fs, argv[3]);
    if (status != M3FS_OK) result = fail_status("rmdir", status);
  } else if (strcmp(command, "verify") == 0 && argc == 3) {
    status = m3fs_verify(fs);
    if (status != M3FS_OK) result = fail_status("verify", status);
    else puts("OK");
  } else if (strcmp(command, "scrub") == 0 && (argc == 3 || argc == 4)) {
    uint32_t scrub_flags = 0U;
    if (argc == 4) {
      if (strcmp(argv[3], "--backfill") != 0) result = 2;
      else scrub_flags = M3FS_SCRUB_BACKFILL_MISSING;
    }
    if (result == 0) {
      m3fs_scrub_report scrub;
      status = m3fs_scrub(fs, scrub_flags, &scrub);
      if (status != M3FS_OK) result = fail_status("scrub", status);
      else {
        printf(
            "blocks=%" PRIu64 " verified=%" PRIu64 " missing=%" PRIu64
            " mismatched=%" PRIu64 " backfilled=%" PRIu64 "\n",
            scrub.blocks_scanned, scrub.checksums_verified, scrub.checksums_missing,
            scrub.checksums_mismatched, scrub.checksums_backfilled);
        if (scrub.checksums_mismatched != 0U) result = 1;
      }
    }
  } else if (strcmp(command, "stats") == 0 && argc == 3) {
    m3fs_stats stats;
    status = m3fs_get_stats(fs, &stats);
    if (status != M3FS_OK) result = fail_status("stats", status);
    else {
      printf(
          "inodes=%" PRIu64 " dirents=%" PRIu64 " logical=%" PRIu64
          " unique_blocks=%" PRIu64 " block_refs=%" PRIu64
          " physical=%" PRIu64 "\n",
          stats.inode_count, stats.dirent_count, stats.logical_bytes,
          stats.unique_blocks, stats.block_references, stats.physical_block_bytes);
    }
  } else if (strcmp(command, "checkpoint") == 0 && argc == 3) {
    status = m3fs_checkpoint(fs);
    if (status != M3FS_OK) result = fail_status("checkpoint", status);
  } else {
    usage(argv[0]);
    result = 2;
  }

  m3fs_close(fs);
  return result;
}
