package com.p2p.cli;

import lombok.Data;

/**
 * Holds parsed command-line arguments.
 *
 * Example usage:
 * java -jar peer.jar --seed myfile.txt --port 6881
 * java -jar peer.jar --download abc123 --bootstrap localhost:6881 --port 6882
 */
@Data
public class CLIArguments {
    /**
     * Mode: SEED or DOWNLOAD
     */
    private Mode mode;

    /**
     * Port to listen on (default: 6881)
     */
    private int port = 6881;

    /**
     * Path to file to seed (for SEED mode)
     */
    private String seedFile;

    /**
     * File ID to download (for DOWNLOAD mode)
     */
    private String downloadFileId;

    /**
     * Bootstrap peer address in format "host:port" (for DOWNLOAD mode)
     */
    private String bootstrap;

    /**
     * Operating mode
     */
    public enum Mode {
        SEED,     // Share a file with others
        DOWNLOAD  // Download a file from others
    }

    /**
     * Validates that required arguments are present for the selected mode.
     */
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
