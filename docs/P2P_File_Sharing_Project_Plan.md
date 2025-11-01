
# Peer-to-Peer File Sharing Network (BitTorrent Lite)

## 🧠 Concept
A decentralized file sharing app with no central server — peers discover and exchange file chunks directly.

**Tech Stack:**
- Java sockets / Netty / gRPC for peer communication  
- SHA-256 hashing for file integrity  
- Optional: DHT (Distributed Hash Table) for peer discovery  

**Highlights:**
- Demonstrates networking, concurrency, and distributed systems knowledge  
- Rare and valuable skill set for backend and infra engineering  

---

## 🚀 Project Goals
- **MVP goal:** Peers discover each other, exchange file chunks, verify integrity, and reassemble files end-to-end without a central server.
- **Nice-to-have:** NAT traversal, DHT for discovery (Kademlia), progress visualization, rarest-first piece selection, optional encryption.
- **Learning outcomes:** Networking, concurrency, chunking/hashing, distributed discovery, fault tolerance.

---

## 🏗️ High-Level Architecture
**Peer process:** Each peer has a listener (server) + client component.  
**Networking layers:**
- Transport: TCP (Netty or java.nio) for chunk transfer; UDP for control/DHT if needed.
- RPC: Optional gRPC for structured control messages (peer list, metadata).
**Storage:**
- `.chunks/` folder for chunk storage.
- Metadata database (H2/SQLite or JSON file).
**Discovery options:**
1. Bootstrap list (MVP).
2. LAN broadcast/mDNS.
3. DHT (Kademlia) for decentralized discovery.
**Integrity:** SHA-256 for each chunk + manifest file.  
**Chunking:** Fixed-size chunks (256–512 KiB).

---

## 🎯 MVP Feature Set
1. Peer-to-peer TCP communication (Netty or sockets).
2. File chunking + SHA-256 per chunk.
3. Protocol for file discovery and chunk exchange.
4. Download and reassembly with integrity check.
5. CLI for seeding and downloading.
6. Logging and transfer metrics.

---

## 🪜 Step-by-Step Plan

### Milestone A — Basic Networking
- Create repo, setup Maven/Gradle build.
- Peer process skeleton (CLI args: `--seed`, `--download`, `--bootstrap`).
- Implement TCP server (Netty) for “hello” messages.
✅ **Goal:** Two peers connect and exchange metadata.

### Milestone B — Chunking & Hashing
- Implement file splitter and SHA-256 hashing.
- Create manifest `{fileId, filename, size, chunkSize, chunkHashes[]}`.
✅ **Goal:** Seeder generates manifest and stores chunks.

### Milestone C — Chunk Transfer & Verification
- Implement chunk request/response.
- Client verifies SHA-256 and reassembles file.
- Add concurrent downloads with thread pool or async Netty.
✅ **Goal:** Verified end-to-end transfer.

### Milestone D — Peer Discovery
- Start with bootstrap list (MVP).
- Optionally implement mDNS or DHT (Kademlia).
✅ **Goal:** Peer can find and connect to others automatically.

### Milestone E — Swarm & Scheduling
- Implement piece selection (sequential → rarest-first).
- Add choking/unchoking, upload slots, timeouts.
✅ **Goal:** Efficient parallel chunk exchange.

### Milestone F — NAT Traversal & Security
- Implement NAT hole punching or UPnP.
- Optional: TLS on control channel.
✅ **Goal:** Secure and reachable peers.

### Milestone G — UX & Observability
- CLI with progress, peer list, and logs.
- Optional web UI for swarm visualization.
✅ **Goal:** Usable demo with clear status.

### Milestone H — Testing & QA
- Unit tests: chunking, hashing, manifest, network handling.
- Integration tests with multiple peers.
✅ **Goal:** Reliable operation under faults.

### Milestone I — Packaging & Demo
- Build runnable JARs.
- Demo: 3 peers exchanging one file.
✅ **Goal:** Complete working proof of concept.

---

## 🧩 Protocol Example (gRPC)
```proto
message Hello { string peerId = 1; repeated string files = 2; }
message Manifest { string fileId = 1; string filename = 2; int64 size = 3; int32 chunkSize = 4; repeated bytes chunkHashes = 5; }
service Peer {
  rpc Announce(Hello) returns (Ack);
  rpc GetManifest(FileId) returns (Manifest);
  rpc RequestChunk(ChunkRequest) returns (stream ChunkData);
}
```

---

## 🗂️ Data Structures
- **Manifests:** `manifests/<manifestHash>.json`
- **Chunks:** `chunks/<first2hex>/<fullhash>`
- **Peer table:** JSON or SQLite database.

---

## ⚙️ Concurrency & Performance
- Use Netty for non-blocking I/O.
- Bounded thread pool for hashing and disk I/O.
- Limit parallel downloads per peer.

---

## 🧪 Testing Checklist
- ✅ Chunker produces correct hashes.
- ✅ File reassembles identical to original.
- ✅ Multi-peer transfer successful.
- ✅ Corrupted chunk detection.
- ✅ NAT and latency simulation tests.

---

## 🔐 Security
- Verify hashes before write.
- Limit request rate per peer.
- Optional: TLS and authentication.

---

## 📦 Deliverables
- `peer.jar` CLI tool.
- Demo scripts: `seed.sh`, `download.sh`.
- README and protocol documentation.
- Test suite and performance metrics.

**Demo steps:**
```bash
# Peer A seeds a file
java -jar peer.jar --seed bigfile.iso --port 6881

# Peer B joins the network
java -jar peer.jar --bootstrap localhost:6881 --port 6882

# Peer C downloads
java -jar peer.jar --download <manifestHash> --bootstrap localhost:6882
```

---

## 🧠 Advanced Extensions
- Full DHT (Kademlia) implementation.
- BitTorrent magnet link support.
- Credit/incentive system.
- GUI interface.

---

## 🛠️ Suggested Libraries
- **Java:** JDK 17+  
- **Networking:** Netty / gRPC  
- **Crypto:** MessageDigest / BouncyCastle  
- **Storage:** SQLite / JSON  
- **Testing:** JUnit5, Testcontainers  
- **Monitoring:** SLF4J + Logback, Micrometer  

---

## ✅ Acceptance Criteria
- Seeder advertises manifest.
- Downloader fetches chunks from one or more peers.
- Integrity verified via SHA-256.
- Basic CLI interface and logging.

---

## 🔧 Starter Checklist
1. Setup repo and build system.
2. Implement file chunker + hash generator.
3. Create simple TCP server + CLI.
4. Test chunk transfer between two local peers.
5. Verify file integrity.

---
