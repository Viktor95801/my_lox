// system
#include <string.h>
// nob.c
#define NOB_IMPLEMENTATION
#include "nob.h"
#include "build_config.h"

int app_state = EXIT_SUCCESS;

void CFLAGS(Nob_Cmd *cmd) {
    nob_cmd_append(cmd, "-Wall", "-Wextra", "-std=c23");
    #if EXTRA_WARNING
    nob_cmd_append(cmd, "-Wpedantic", "-Werror", );
    #endif
    #if DEBUG
    nob_cmd_append(cmd, "-g", "-Ofast");
    nob_cmd_append(cmd, "-DDEBUG", "-fsanitize=address", "-fsanitize=undefined");
    #elif RELEASE
    nob_cmd_append(cmd, "-O3");
    #elif DEV
    nob_cmd_append(cmd, "-Ofast");
    #endif
}
void BUILD_FLAGS(Nob_Cmd *cmd) {
    nob_cmd_append(cmd, "-o", BIN_PATH APP_NAME , "-I"INCLUDE_FOULDER);
}
void SOURCE_FILES(Nob_Cmd *cmd) {
    nob_cmd_append(cmd, "lox.c");
    Nob_File_Paths dir_paths = {0};
    if (!nob_read_entire_dir(SRC_FOULDER, &dir_paths)) return;
    nob_log(NOB_INFO, "found %d files in "SRC_FOULDER, dir_paths.count - 2);
    for (size_t i = 0; i < dir_paths.count; i++) {
        const char* file = dir_paths.items[i];
        if (!(strncmp(file, ".", 1) == 0 || strncmp(file, "..", 2) == 0)) {
            int len = strlen(file) - 1;
            if (strncmp(file + (len-1), ".c", 2) == 0) /*after this piece of code I feel a genius*/ {
                char to_append[256] = SRC_FOULDER;
                strncat_s(to_append, 256, dir_paths.items[i], 256);
                nob_cmd_append(cmd, to_append);
                nob_log(NOB_INFO, "\t%s (ends with .c)", dir_paths.items[i]);
            }
        }
    }
    nob_log(NOB_INFO, "END "SRC_FOULDER" listing\n");
    nob_da_free(dir_paths);
}

int main(int argc, char **argv) {
    NOB_GO_REBUILD_URSELF_PLUS(argc, argv, "build_config.h");
    Nob_Cmd cmd = {0};
    nob_cmd_append(&cmd, "cc");
    BUILD_FLAGS(&cmd);
    CFLAGS(&cmd);
    SOURCE_FILES(&cmd);
    if (!nob_cmd_run_sync_and_reset(&cmd)) return EXIT_FAILURE;

    for (int i = 1; i < argc; i++) {
        if (strncmp(argv[i], "--run", 5) == 0) {
            nob_cmd_append(&cmd, "./"BIN_PATH APP_NAME);
        } else {
            nob_cmd_append(&cmd, argv[i]);
        }
    }
    if (cmd.count > 0)
        if (!nob_cmd_run_sync_and_reset(&cmd)) return EXIT_FAILURE;
    return 0;
}