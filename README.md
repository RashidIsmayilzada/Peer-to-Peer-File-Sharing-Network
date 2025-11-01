# P2P File Sharing Network

A production-ready BitTorrent-like peer-to-peer file sharing system built with Java and Netty. Share files directly between devices without a central server.

## ✨ Features

- 🌐 **Fully Decentralized** - No central server required for file transfer
- 📦 **Chunked Transfer** - Files split into 256KB chunks for efficient distribution
- ✅ **Data Integrity** - SHA-256 verification ensures 100% data integrity
- 💾 **Memory Efficient** - Streaming downloads handle multi-GB files with O(1) memory
- 🔍 **Peer Discovery** - Automatic peer discovery and swarm building
- 🌍 **Cross-Device** - Works across different devices and networks
- ⚡ **High Performance** - Asynchronous I/O with Netty framework
- 📝 **Well Documented** - Comprehensive guides and documentation

## 🚀 Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.6+

### Build the Project

```bash
mvn clean package
```

This creates an executable JAR: `target/peer.jar`

### Share a File (Seeder)

```bash
java -jar target/peer.jar --seed myfile.txt --port 8001
```

**Output:**
```
=== FILE READY FOR SHARING ===
File ID: c592dbd3012ad93de57a7fcbd6727f1aeef372c30f53029490a7cb0937edf658
Filename: myfile.txt
Share this File ID with others!
================================
```

### Download a File (Leecher)

```bash
java -jar target/peer.jar \
  --download c592dbd3012ad93de57a7fcbd6727f1aeef372c30f53029490a7cb0937edf658 \
  --bootstrap localhost:8001 \
  --port 8002
```

### Get Help

```bash
java -jar target/peer.jar --help
```

## 🌐 Multi-Device Setup

Share files between different computers on the same or different networks!

### Quick Setup

1. **Find your IP address:**
   ```bash
   ./scripts/get-my-ip.sh
   ```

2. **On Device 1 (Seeder):**
   ```bash
   java -jar target/peer.jar --seed movie.mp4 --port 8001
   # Copy the File ID displayed
   ```

3. **On Device 2 (Downloader):**
   ```bash
   java -jar target/peer.jar \
     --download <FILE_ID> \
     --bootstrap <DEVICE_1_IP>:8001 \
     --port 8002
   ```

📚 **Complete guide:** [docs/MULTI_DEVICE_SETUP.md](docs/MULTI_DEVICE_SETUP.md)

## 📖 Documentation

Comprehensive documentation is available in the `docs/` folder:

| Document | Description |
|----------|-------------|
| **[SYSTEM_GUIDE.md](docs/SYSTEM_GUIDE.md)** | **Complete system guide with step-by-step explanations** |
| [ARCHITECTURE.md](docs/ARCHITECTURE.md) | System architecture, components, and data flow |
| [DOWNLOAD_GUIDE.md](docs/DOWNLOAD_GUIDE.md) | How chunked downloads work with examples |
| [MULTI_DEVICE_SETUP.md](docs/MULTI_DEVICE_SETUP.md) | Cross-device setup and troubleshooting |
| [HOW_TO_TEST.md](docs/HOW_TO_TEST.md) | Testing instructions and scenarios |
| [TEST_RESULTS.md](docs/TEST_RESULTS.md) | Complete testing documentation |

**👉 Start here:** [docs/SYSTEM_GUIDE.md](docs/SYSTEM_GUIDE.md) for a comprehensive walkthrough!

## 🏗️ Architecture

### High-Level Overview

```
┌─────────────────────────────────────────────────────────┐
│                    P2P Network                          │
│                                                         │
│  ┌──────────┐      ┌──────────┐      ┌──────────┐     │
│  │  Peer A  │◄────►│  Peer B  │◄────►│  Peer C  │     │
│  │  (Seed)  │      │(Leecher) │      │(Leecher) │     │
│  └──────────┘      └──────────┘      └──────────┘     │
│                                                         │
│        Direct peer-to-peer connections                  │
│        No central server required                       │
└─────────────────────────────────────────────────────────┘
```

### Technology Stack

- **Java 17** - Core programming language
- **Netty 4.x** - High-performance asynchronous networking
- **Jackson** - JSON serialization for protocol messages
- **Lombok** - Reduces boilerplate code
- **SLF4J + Logback** - Comprehensive logging
- **Maven** - Build and dependency management

### Project Structure

```
p2p-network/
├── src/main/java/com/p2p/
│   ├── core/           # Data models (Manifest, ChunkInfo, PeerInfo)
│   ├── protocol/       # Protocol messages (7 message types)
│   ├── network/        # Netty server/client and handlers
│   ├── discovery/      # Peer registry and discovery
│   ├── chunking/       # File chunking and manifest generation
│   ├── storage/        # Chunk and manifest storage
│   └── cli/            # Command-line interface
├── docs/               # Comprehensive documentation
│   ├── SYSTEM_GUIDE.md        # Complete system walkthrough
│   ├── ARCHITECTURE.md        # Architecture details
│   ├── DOWNLOAD_GUIDE.md      # Download process
│   └── MULTI_DEVICE_SETUP.md  # Cross-device setup
├── scripts/            # Helper scripts for testing
│   ├── test-complete.sh       # End-to-end test
│   ├── test-peer-discovery.sh # Peer discovery test
│   └── get-my-ip.sh           # Get local IP
├── .chunks/            # Chunk storage (gitignored)
├── manifests/          # Manifest storage (gitignored)
├── downloads/          # Downloaded files (gitignored)
└── target/             # Build output (gitignored)
```

## 🎯 Current Status

### ✅ Completed Features

- **Milestone A: Basic Networking**
  - TCP server/client with Netty
  - HELLO message exchange
  - Concurrent connection handling
  - Robust error handling

- **Milestone B: Chunking & Hashing**
  - 256KB chunk size
  - SHA-256 hashing for verification
  - Manifest generation
  - Efficient chunk storage

- **Milestone C: Chunk Transfer & Verification**
  - Request/response protocol
  - Chunk integrity verification
  - File reassembly
  - Streaming downloads (memory-efficient)

- **Milestone D: Peer Discovery**
  - Peer registry with thread-safe operations
  - Peer list exchange protocol
  - Automatic peer registration
  - Multi-peer swarm support

### 🚧 Next: Milestone E - Swarm & Scheduling

Planned features:
- Download from multiple peers simultaneously
- Intelligent chunk scheduling (rarest-first algorithm)
- Upload/download rate management
- Connection pooling optimization
- Bandwidth throttling

## 🧪 Testing

### Automated Tests

```bash
# Test complete download flow with 3 peers
./scripts/test-complete.sh

# Test peer discovery mechanism
./scripts/test-peer-discovery.sh

# Get your local IP address
./scripts/get-my-ip.sh
```

### Manual Testing

**Terminal 1 (Seeder):**
```bash
java -jar target/peer.jar --seed test.txt --port 6881
```

**Terminal 2 (Downloader):**
```bash
java -jar target/peer.jar --download <FILE_ID> --bootstrap localhost:6881 --port 6882
```

**Terminal 3 (Another Downloader):**
```bash
java -jar target/peer.jar --download <FILE_ID> --bootstrap localhost:6881 --port 6883
```

See [docs/HOW_TO_TEST.md](docs/HOW_TO_TEST.md) for detailed testing scenarios.

## 🔧 Development

### Build Commands

```bash
# Compile source code
mvn compile

# Run tests (when available)
mvn test

# Package into JAR
mvn package

# Clean build artifacts
mvn clean

# Full clean build
mvn clean package
```

### Configuration

**Frame Size:** 2MB (configurable in `PeerServer.java` and `DownloadManager.java`)
- Sufficient for 256KB chunks + Base64/JSON overhead
- Prevents memory exhaustion
- Standard industry practice

**Chunk Size:** 256KB
- Optimal balance between transfer efficiency and memory usage
- Standard in BitTorrent protocol

**Timeout:** 120 seconds
- Allows for large chunk transfers on slower networks
- Configurable in `DownloadManager.java`

## 📊 Performance Characteristics

### Memory Usage

| Operation | Memory Usage | Notes |
|-----------|--------------|-------|
| Chunking file | O(1) | Only one 256KB chunk in memory |
| Downloading | O(1) | Streaming to disk, no accumulation |
| Seeding | O(1) | Read chunk from disk on demand |
| Manifest | O(n) | n = number of chunks (minimal) |

### Network Overhead

| Component | Overhead | Reason |
|-----------|----------|--------|
| Base64 encoding | +33% | Binary data in JSON |
| JSON structure | ~1-2% | Message metadata |
| Length prefix | 4 bytes | TCP framing |
| **Total** | **~35%** | Per chunk transfer |

### Example Performance

```
File: 10MB (10,485,760 bytes)
Chunks: 40 (256KB each)
Network transfer: ~13.5MB (with overhead)
Memory usage: ~2MB max (frame buffer)
```

## 🔒 Security Considerations

### Current Security Features

✅ **Data Integrity**
- SHA-256 hashing for all chunks
- Automatic verification on download
- Tampered data detected immediately

✅ **File Verification**
- Manifest hash serves as file fingerprint
- Cannot substitute different file with same ID

### Known Limitations

⚠️ **No Encryption**
- Data sent in plaintext (Base64, not encrypted)
- Susceptible to eavesdropping

⚠️ **No Authentication**
- No peer identity verification
- Susceptible to Sybil attacks

⚠️ **No Access Control**
- Anyone with file ID can download
- No permission system

### Future Enhancements

See [docs/SYSTEM_GUIDE.md](docs/SYSTEM_GUIDE.md#security-considerations) for recommended security improvements.

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

## 🐛 Troubleshooting

### Common Issues

**Connection refused:**
```bash
# Check if peer is running
netstat -an | grep 8001

# Check firewall settings
# macOS: System Preferences → Security & Privacy → Firewall
# Linux: sudo iptables -L
```

**Chunk verification failed:**
- Retry download
- Verify seeder has intact chunks
- Check network stability

**OutOfMemoryError:**
```bash
# Increase heap size (usually not needed with streaming)
java -Xmx4g -jar target/peer.jar ...
```

See [docs/SYSTEM_GUIDE.md](docs/SYSTEM_GUIDE.md#troubleshooting) for complete troubleshooting guide.

## 📚 Learn More

- **System Guide:** [docs/SYSTEM_GUIDE.md](docs/SYSTEM_GUIDE.md) - Complete walkthrough
- **Protocol Details:** [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md#protocol-specification)
- **How P2P Works:** [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md#what-is-p2p-file-sharing)
- **Data Flow:** [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md#how-data-flows)
- **Component Details:** [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md#component-details)

## 📝 License

MIT License - See LICENSE file for details

## 🤝 Contributing

This is a learning project demonstrating distributed systems concepts. Contributions welcome!

**How to contribute:**
1. Read [docs/SYSTEM_GUIDE.md](docs/SYSTEM_GUIDE.md) to understand the architecture
2. Check the [roadmap](#roadmap) for upcoming features
3. Pick a feature or improvement
4. Implement and test thoroughly
5. Submit a pull request

**Contribution areas:**
- New features from roadmap
- Performance optimizations
- Bug fixes
- Documentation improvements
- Test coverage
- Security enhancements

## 🎓 Educational Value

This project demonstrates:
- ✅ Distributed systems design and implementation
- ✅ Network programming with TCP/IP
- ✅ Asynchronous I/O patterns with Netty
- ✅ Protocol design and implementation
- ✅ Data integrity and verification
- ✅ Memory-efficient streaming
- ✅ Thread-safe concurrent programming
- ✅ Clean architecture and separation of concerns

Perfect for learning:
- How BitTorrent works under the hood
- Building real-world network applications
- Distributed systems challenges
- Performance optimization techniques

---

**Built as a learning project to understand distributed systems and network programming**

*Star ⭐ this repo if you find it useful!*
