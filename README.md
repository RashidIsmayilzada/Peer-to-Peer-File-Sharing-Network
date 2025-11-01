# P2P File Sharing Network

A BitTorrent-like peer-to-peer file sharing application built with Java and Netty.

## 🚀 Quick Start

### Build the Project
```bash
mvn clean package
```

This creates an executable JAR: `target/peer.jar`

### Run a Seeder (Share a file)
```bash
java -jar target/peer.jar --seed myfile.txt --port 6881
```

### Run a Downloader (Download a file)
```bash
java -jar target/peer.jar --download <fileId> --bootstrap localhost:6881 --port 6882
```

### Get Help
```bash
java -jar target/peer.jar --help
```

## 🌐 Multi-Device Support

The P2P network works across different devices!

### Quick Setup:

1. **Find your IP address:**
   ```bash
   ./scripts/get-my-ip.sh
   ```

2. **On Device 1 (Seeder):**
   ```bash
   java -jar target/peer.jar --seed myfile.txt --port 8001
   # Note the File ID displayed
   ```

3. **On Device 2 (Downloader):**
   ```bash
   java -jar target/peer.jar \
     --download <FILE_ID> \
     --bootstrap <DEVICE_1_IP>:8001 \
     --port 8002
   ```

📚 **See [docs/MULTI_DEVICE_SETUP.md](docs/MULTI_DEVICE_SETUP.md) for complete instructions**

## 📖 Documentation

All documentation is available in the `docs/` folder:

- **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)** - Comprehensive system architecture guide
  - What P2P file sharing is and how it works
  - System components and data flow
  - Implementation details with examples

- **[docs/DOWNLOAD_GUIDE.md](docs/DOWNLOAD_GUIDE.md)** - Complete download process explanation
  - How chunked file downloads work
  - Step-by-step download instructions
  - Multi-peer download scenarios

- **[docs/MULTI_DEVICE_SETUP.md](docs/MULTI_DEVICE_SETUP.md)** - Cross-device testing guide
  - How to share files between different computers
  - Network configuration and firewall setup
  - Troubleshooting connection issues

- **[docs/TEST_RESULTS.md](docs/TEST_RESULTS.md)** - Complete testing documentation
- **[docs/TEST_RESULTS_MILESTONE_D.md](docs/TEST_RESULTS_MILESTONE_D.md)** - Peer discovery testing

**Start with docs/ARCHITECTURE.md if you're new to P2P concepts!**

## 🏗️ Current Status

### ✅ Completed Milestones

- **Milestone A: Basic Networking** - TCP server/client, HELLO messages, concurrent connections
- **Milestone B: Chunking & Hashing** - 256KB chunks, SHA-256 hashing, manifest generation
- **Milestone C: Chunk Transfer** - Request/response protocol, chunk verification, file reassembly
- **Milestone D: Peer Discovery** - Peer registry, peer list exchange, multi-peer support

### 🚧 Next: Milestone E - Swarm & Scheduling

Coming next:
- Download from multiple peers simultaneously
- Intelligent chunk scheduling (rarest first)
- Upload/download rate management
- Connection pooling optimization

## 🎯 Project Goals

**MVP Goal:** Peers discover each other, exchange file chunks, verify integrity, and reassemble files end-to-end without a central server.

**Tech Stack:**
- Java 17
- Netty 4.x (async networking)
- Jackson (JSON serialization)
- SLF4J + Logback (logging)
- Maven (build)

## 📦 Project Structure

```
p2p-network/
├── src/main/java/com/p2p/
│   ├── core/           # Data models (Manifest, ChunkInfo, PeerInfo)
│   ├── protocol/       # Message types (HELLO, CHUNK_REQUEST, etc.)
│   ├── network/        # Netty server/client and handlers
│   ├── discovery/      # Peer registry and discovery
│   ├── transfer/       # Download/upload managers
│   ├── chunking/       # File chunking and manifest generation
│   ├── storage/        # Chunk and manifest storage
│   └── cli/            # Command-line interface
├── docs/               # All project documentation
│   ├── ARCHITECTURE.md
│   ├── DOWNLOAD_GUIDE.md
│   ├── MULTI_DEVICE_SETUP.md
│   └── TEST_RESULTS*.md
├── scripts/            # Helper scripts for testing
│   ├── test-complete.sh
│   ├── test-peer-discovery.sh
│   └── get-my-ip.sh
├── .chunks/            # Chunk storage (gitignored)
├── manifests/          # Manifest storage (gitignored)
├── downloads/          # Downloaded files (gitignored)
└── target/             # Build output (gitignored)
```

## 🧪 Testing

### Automated Test Scripts

```bash
# Test complete download flow (3 peers)
./scripts/test-complete.sh

# Test peer discovery (3 peers with discovery)
./scripts/test-peer-discovery.sh

# Get your local IP address
./scripts/get-my-ip.sh
```

### Manual Test: Connection Between Two Peers

**Terminal 1 (Seeder):**
```bash
java -jar target/peer.jar --seed test.txt --port 6881
```

**Terminal 2 (Downloader):**
```bash
java -jar target/peer.jar --download <FILE_ID> --bootstrap localhost:6881 --port 6882
```

See `docs/HOW_TO_TEST.md` for detailed testing instructions.

## 🔧 Development

### Compile
```bash
mvn compile
```

### Run Tests
```bash
mvn test
```

### Package
```bash
mvn package
```

### Clean
```bash
mvn clean
```

## 📚 Learn More

- **Protocol:** See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md#protocol-specification) for message format
- **How P2P Works:** See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md#what-is-p2p-file-sharing)
- **Data Flow:** See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md#how-data-flows)
- **Components:** See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md#component-details)

## 🗺️ Roadmap

- [x] Milestone A: Basic Networking
- [x] Milestone B: Chunking & Hashing
- [x] Milestone C: Chunk Transfer & Verification
- [x] Milestone D: Peer Discovery
- [ ] Milestone E: Swarm & Scheduling
- [ ] Milestone F: NAT Traversal & Security
- [ ] Milestone G: UX & Observability
- [ ] Milestone H: Testing & QA
- [ ] Milestone I: Packaging & Demo

## 📝 License

MIT License - See LICENSE file for details

## 🤝 Contributing

This is a learning project. Contributions welcome!

1. Read [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) to understand the system
2. Pick a TODO item from the roadmap
3. Implement and test
4. Submit a PR

---

**Built with ❤️ as a learning project to understand distributed systems**
