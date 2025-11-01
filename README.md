# P2P File Sharing Network

A BitTorrent-like peer-to-peer file sharing system built with Java and Netty. Share files directly between devices without a central server, with automatic peer discovery, chunked transfer, and SHA-256 verification.

## 🚀 Quick Start

### Build
```bash
mvn clean package
```

### Share a File
```bash
java -jar target/peer.jar --seed myfile.txt --port 8001
```

### Download a File
```bash
java -jar target/peer.jar --download <FILE_ID> --bootstrap <HOST>:8001 --port 8002
```

## ✨ Features

- 🌐 Fully decentralized (no server needed)
- 📦 Chunked transfer (256KB chunks)
- ✅ SHA-256 verification
- 💾 Memory efficient streaming (O(1) memory usage)
- 🔍 Automatic peer discovery
- 🌍 Cross-device support

## 📖 Documentation

- **[SYSTEM_GUIDE.md](docs/SYSTEM_GUIDE.md)** - Complete system walkthrough
- [ARCHITECTURE.md](docs/ARCHITECTURE.md) - System architecture
- [MULTI_DEVICE_SETUP.md](docs/MULTI_DEVICE_SETUP.md) - Cross-device setup
- [HOW_TO_TEST.md](docs/HOW_TO_TEST.md) - Testing guide

## 🛠️ Tech Stack

Java 17 | Netty 4.x | Jackson | Maven | SLF4J + Logback

## 📝 License

MIT License
