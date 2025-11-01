package com.p2p.cli;

/**
 * Parses command-line arguments into a CLIArguments object.
 *
 * Supported arguments:
 * --seed <filepath>        : Seed a file (share it with network)
 * --download <fileId>      : Download a file by its ID
 * --port <number>          : Port to listen on (default: 6881)
 * --bootstrap <host:port>  : Bootstrap peer to connect to initially
 */
public class CLIParser {

    public static CLIArguments parse(String[] args) throws IllegalArgumentException {
        CLIArguments cliArgs = new CLIArguments();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            switch (arg) {
                case "--seed":
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("--seed requires a filepath");
                    }
                    cliArgs.setMode(CLIArguments.Mode.SEED);
                    cliArgs.setSeedFile(args[++i]);
                    break;

                case "--download":
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("--download requires a file ID");
                    }
                    cliArgs.setMode(CLIArguments.Mode.DOWNLOAD);
                    cliArgs.setDownloadFileId(args[++i]);
                    break;

                case "--port":
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("--port requires a port number");
                    }
                    try {
                        cliArgs.setPort(Integer.parseInt(args[++i]));
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Invalid port number: " + args[i]);
                    }
                    break;

                case "--bootstrap":
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("--bootstrap requires host:port");
                    }
                    cliArgs.setBootstrap(args[++i]);
                    break;

                case "--help":
                case "-h":
                    printUsage();
                    System.exit(0);
                    break;

                default:
                    throw new IllegalArgumentException("Unknown argument: " + arg);
            }
        }

        cliArgs.validate();
        return cliArgs;
    }

    private static void printUsage() {
        System.out.println("P2P File Sharing Network - Usage:");
        System.out.println();
        System.out.println("Seed a file:");
        System.out.println("  java -jar peer.jar --seed <filepath> --port <port>");
        System.out.println();
        System.out.println("Download a file:");
        System.out.println("  java -jar peer.jar --download <fileId> --bootstrap <host:port> --port <port>");
        System.out.println();
        System.out.println("Arguments:");
        System.out.println("  --seed <filepath>       : Path to file to share");
        System.out.println("  --download <fileId>     : File ID to download");
        System.out.println("  --port <port>           : Port to listen on (default: 6881)");
        System.out.println("  --bootstrap <host:port> : Bootstrap peer address");
        System.out.println("  --help, -h              : Show this help message");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -jar peer.jar --seed myfile.txt --port 6881");
        System.out.println("  java -jar peer.jar --download abc123 --bootstrap localhost:6881 --port 6882");
    }
}
