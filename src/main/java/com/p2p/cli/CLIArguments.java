package com.p2p.cli;

import lombok.Data;

// Holds parsed command-line arguments
@Data
public class CLIArguments {
    private Mode mode;
    private int port = 6881;
    private String seedFile;
    private String downloadFileId;
    private String bootstrap;

    public enum Mode {
        SEED,
        DOWNLOAD
    }

    // Validates that required arguments are present for the selected mode
    public void validate() throws IllegalArgumentException {
        if (mode == null) {
            throw new IllegalArgumentException("Mode must be specified (--seed or --download)");
        }

        if (port < 1024 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1024 and 65535");
        }

        if (mode == Mode.SEED) {
            if (seedFile == null || seedFile.isEmpty()) {
                throw new IllegalArgumentException("Seed mode requires --seed <filepath>");
            }
        } else if (mode == Mode.DOWNLOAD) {
            if (downloadFileId == null || downloadFileId.isEmpty()) {
                throw new IllegalArgumentException("Download mode requires --download <fileId>");
            }
            if (bootstrap == null || bootstrap.isEmpty()) {
                throw new IllegalArgumentException("Download mode requires --bootstrap <host:port>");
            }
        }
    }
}
