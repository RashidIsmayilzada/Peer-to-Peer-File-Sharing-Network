# P2P File Sharing Network - Architecture Guide

## Table of Contents
1. [What is P2P File Sharing?](#what-is-p2p-file-sharing)
2. [High-Level Architecture](#high-level-architecture)
3. [Core Concepts Explained](#core-concepts-explained)
4. [Project Structure](#project-structure)
5. [How Data Flows](#how-data-flows)
6. [Component Details](#component-details)
7. [Protocol Specification](#protocol-specification)
8. [What's Implemented vs TODO](#whats-implemented-vs-todo)

---

## What is P2P File Sharing?

**Traditional File Download (Client-Server):**
```
You → Internet → Single Server → File comes back to you
```
- If the server is slow or down, you're stuck
- Server pays for all bandwidth
- Slow for large files

**Peer-to-Peer (P2P) File Sharing:**
```
You ← → Peer 1 (has pieces 1-5)
You ← → Peer 2 (has pieces 6-10)
You ← → Peer 3 (has pieces 11-15)
```
- Download different pieces from multiple peers simultaneously
- Faster and more reliable
- No single point of failure
- As you download pieces, you can share them with others

**Real-world examples:** BitTorrent, Spotify (hybrid), Bitcoin blockchain

---

## High-Level Architecture

Our P2P system has three main components:

```
┌─────────────────────────────────────┐
│         YOUR PEER                   │
│                                     │
│  ┌──────────┐      ┌──────────┐   │
│  │  SERVER  │      │  CLIENT  │   │
│  │ (Listen) │      │(Connect) │   │
│  └────┬─────┘      └─────┬────┘   │
│       │                   │         │
│       │    ┌──────────┐   │        │
│       └────┤  STORAGE ├───┘        │
│            │ (Chunks) │             │
│            └──────────┘             │
└─────────────────────────────────────┘
         ↕                    ↕
    OTHER PEERS          OTHER PEERS
```

**Key Point:** Every peer is BOTH a server AND a client!
- **Server component:** Listens for connections and shares chunks
- **Client component:** Connects to others and requests chunks

---

## Core Concepts Explained

### 1. **Chunks** (Think: Puzzle Pieces)
Imagine you want to share a 10MB video file. Instead of sending the whole thing at once:
- Split it into 40 pieces of 256KB each
- Number them: chunk 0, chunk 1, chunk 2, ... chunk 39
- Calculate a "fingerprint" (hash) for each piece
- Now peers can download different pieces from different sources

**Why chunking?**
- Download from multiple peers simultaneously
- Verify each piece independently (if corrupted, re-download just that piece)
- Start watching before download completes (streaming)

### 2. **Manifest** (Think: Recipe Card)
Before downloading, you need to know:
- What's the filename?
- How big is the file?
- How many chunks are there?
- What's the hash of each chunk? (to verify integrity)

The **manifest** is like a recipe card that describes the file:
```json
{
  "fileId": "abc123...",
  "filename": "movie.mp4",
  "fileSize": 10485760,
  "chunkSize": 262144,
  "chunks": [
    {"index": 0, "hash": "d4f3a2...", "size": 262144},
    {"index": 1, "hash": "8c2b1e...", "size": 262144},
    ...
  ]
}
```

### 3. **SHA-256 Hash** (Think: Fingerprint)
A hash is like a unique fingerprint for data:
- Input: "Hello World" → Hash: "a591a6d40bf420404a011733cfb7b190d62c65bf0bcda32b57b277d9ad9f146e"
- Change ONE letter: "Hello World!" → Completely different hash
- Same input ALWAYS produces the same hash
- Impossible to reverse (can't get original data from hash)

**Why we use it:**
1. Verify chunk integrity: If hash matches, data is correct
2. Detect corruption: If hash doesn't match, chunk was corrupted in transit
3. File identification: Use hash as unique file ID

### 4. **Netty** (Think: Advanced Phone System)
Netty is a framework for network communication. Think of it like a sophisticated phone system:

**Without Netty (raw Java sockets):**
```
You have to:
- Manually answer each call
- Handle one call at a time
- Write code for every detail (busy signals, dropped calls, etc.)
```

**With Netty:**
```
- Automatically handles thousands of simultaneous calls
- Each call gets its own "line" (channel)
- Built-in reliability and error handling
- You just focus on what to say (protocol)
```

**Netty Pipeline Concept:**
```
Incoming bytes → Decoder → Your handler → Encoder → Outgoing bytes
     ↓              ↓            ↓            ↓           ↓
  Raw TCP    →  JSON/Message → Business Logic → Message → Raw TCP
```

### 5. **Protocol** (Think: Language)
Just like humans need a common language to communicate, peers need a protocol:

```
HELLO Message:
"Hi, I'm peer-123, I have files [abc, def], I'm on port 6881"

MANIFEST_REQUEST:
"Can you send me the manifest for file abc?"

MANIFEST_RESPONSE:
"Sure! Here's the manifest: {filename: movie.mp4, chunks: [...]}"

CHUNK_REQUEST:
"Please send me chunk #5 of file abc"

CHUNK_RESPONSE:
"Here's chunk #5: [binary data...]"
```

---

## Project Structure

```
com.p2p/
├── core/                    # Data structures
│   ├── ChunkInfo.java       # Info about one chunk (index, hash, size)
│   ├── Manifest.java        # File metadata + all chunk info
│   └── PeerInfo.java        # Info about a peer (ID, address, port)
│
├── protocol/                # Communication messages
│   ├── Message.java         # Base class for all messages
│   ├── MessageType.java     # Enum: HELLO, CHUNK_REQUEST, etc.
│   ├── HelloMessage.java    # First message when peers connect
│   ├── ManifestRequestMessage.java
│   ├── ManifestResponseMessage.java
│   ├── ChunkRequestMessage.java
│   └── ChunkResponseMessage.java
│
├── network/                 # Networking layer (Netty)
│   ├── MessageCodec.java    # Converts messages ↔ bytes
│   ├── PeerServer.java      # Listens for incoming connections
│   ├── PeerServerHandler.java # Handles incoming messages
│   ├── PeerClient.java      # Connects to other peers
│   └── PeerClientHandler.java # Handles responses from peers
│
└── cli/                     # Command-line interface
    ├── CLIArguments.java    # Holds parsed arguments
    ├── CLIParser.java       # Parses command-line args
    └── PeerCLI.java         # Main entry point
```

---

## How Data Flows

### Scenario 1: Peer A Seeds a File

```
1. User runs: java -jar peer.jar --seed movie.mp4 --port 6881

2. PeerCLI starts PeerServer on port 6881

3. Server listens for connections:
   ┌─────────────┐
   │ PeerServer  │  "I'm listening on port 6881"
   └─────────────┘
         │
         ↓
   [Waiting for peers to connect...]

4. TODO: Split movie.mp4 into chunks, generate manifest
```

### Scenario 2: Peer B Downloads from Peer A

```
1. User runs: java -jar peer.jar --download abc123 --bootstrap localhost:6881 --port 6882

2. PeerCLI:
   - Starts PeerServer on port 6882 (so B can also share)
   - Starts PeerClient to connect to localhost:6881

3. Connection established:
   Peer B (Client) ──────────→ Peer A (Server)
                   TCP connection

4. Peer B sends HELLO:
   B → A: "Hi, I'm peer-456, I have [], I'm on port 6882"

5. Peer A responds with HELLO:
   A → B: "Hi, I'm peer-123, I have [abc123], I'm on port 6881"

6. TODO: B requests manifest, downloads chunks, verifies hashes, reassembles file
```

### Scenario 3: Multi-Peer Download (The P2P Magic)

```
Peer A has:  [1, 2, 3, 4, 5, 6, 7, 8]  (full file)
Peer B has:  [1, 2, 3]                  (partial)
Peer C wants: [1, 2, 3, 4, 5, 6, 7, 8]  (downloading)

Timeline:
1. C connects to A and B
2. C asks A: "Send me chunks 1, 2, 3"
3. C asks B: "Send me chunks 4, 5, 6"  (parallel!)
4. As C gets chunks, it becomes a seeder too!
5. New peer D can download from A, B, AND C

This is why P2P is fast: more peers = more speed!
```

---

## Component Details

### 1. MessageCodec (The Translator)

**What it does:** Converts between Java objects and network bytes

```
ENCODING (Sending):
HelloMessage object → JSON string → Bytes → Network
     ↓                    ↓            ↓
{peerId: "123"}  → "{\"peerId\":\"123\"}" → [01101000...]

DECODING (Receiving):
Network → Bytes → JSON string → HelloMessage object
   ↓        ↓          ↓              ↓
[01101000...] → "{\"peerId\":\"123\"}" → {peerId: "123"}
```

**Format:**
```
[4 bytes: length][N bytes: JSON data]
[00 00 00 15]    [{"type":"HELLO",...}]
```

### 2. PeerServer (Your House)

**What it does:** Listens for peers who want to connect

**Netty components:**
- **Boss Group:** Doorman who greets new visitors (accepts connections)
- **Worker Group:** Staff who handle each visitor (process requests)
- **Channel:** One visitor's connection
- **Pipeline:** Steps each message goes through

```java
// Boss accepts connection
Boss: "New visitor from 192.168.1.100!"

// Worker sets up pipeline
Worker: "Set up: Bytes → MessageCodec → PeerServerHandler"

// Handler processes message
Handler: "They want chunk #5, let me fetch it..."
```

### 3. PeerClient (Your Car)

**What it does:** Connects you to other peers

```java
// Connect to peer
Bootstrap: "Drive to localhost:6881"

// Set up communication
Pipeline: "Bytes → MessageCodec → PeerClientHandler"

// Send message
Client: "Send HELLO message"

// Receive response
Handler: "Got HELLO back, now I can request chunks!"
```

---

## Protocol Specification

### Message Format

All messages are JSON with a `type` field:

```json
{
  "type": "HELLO",
  "peerId": "uuid-1234",
  "availableFiles": ["abc123", "def456"],
  "port": 6881
}
```

### Message Types

#### 1. HELLO (Handshake)
**Direction:** Both ways (when connection established)
```json
{
  "type": "HELLO",
  "peerId": "peer-123",
  "availableFiles": ["abc123"],
  "port": 6881
}
```

#### 2. MANIFEST_REQUEST
**Direction:** Downloader → Seeder
```json
{
  "type": "MANIFEST_REQUEST",
  "fileId": "abc123"
}
```

#### 3. MANIFEST_RESPONSE
**Direction:** Seeder → Downloader
```json
{
  "type": "MANIFEST_RESPONSE",
  "manifest": {
    "fileId": "abc123",
    "filename": "movie.mp4",
    "fileSize": 10485760,
    "chunkSize": 262144,
    "chunks": [...]
  }
}
```

#### 4. CHUNK_REQUEST
**Direction:** Downloader → Seeder
```json
{
  "type": "CHUNK_REQUEST",
  "fileId": "abc123",
  "chunkIndex": 5
}
```

#### 5. CHUNK_RESPONSE
**Direction:** Seeder → Downloader
```json
{
  "type": "CHUNK_RESPONSE",
  "fileId": "abc123",
  "chunkIndex": 5,
  "data": "base64-encoded-bytes...",
  "hash": "sha256-hash..."
}
```

---

## What's Implemented vs TODO

### ✅ Implemented (Milestone A - Basic Networking)

1. **Project Foundation:**
   - ✅ Maven build with all dependencies
   - ✅ Logging configuration
   - ✅ Package structure

2. **Core Data Models:**
   - ✅ ChunkInfo (chunk metadata)
   - ✅ Manifest (file metadata)
   - ✅ PeerInfo (peer information)

3. **Protocol Messages:**
   - ✅ All 5 message types defined
   - ✅ JSON serialization with Jackson
   - ✅ Message codec for encoding/decoding

4. **Networking:**
   - ✅ PeerServer (Netty TCP server)
   - ✅ PeerClient (Netty TCP client)
   - ✅ Message handlers (skeleton)
   - ✅ Connection handling

5. **CLI:**
   - ✅ Argument parser
   - ✅ Help documentation
   - ✅ Main entry point
   - ✅ Seed and download mode structure

**Current Status:** Two peers can connect and exchange HELLO messages!

### 🚧 TODO (Next Milestones)

**Milestone B - Chunking & Hashing:**
- ❌ File chunking implementation
- ❌ SHA-256 hash generation
- ❌ Manifest file generation and storage
- ❌ Chunk storage (`.chunks/` directory)

**Milestone C - Chunk Transfer:**
- ❌ Implement MANIFEST_REQUEST/RESPONSE handling
- ❌ Implement CHUNK_REQUEST/RESPONSE handling
- ❌ Chunk verification (hash checking)
- ❌ File reassembly
- ❌ Progress tracking

**Milestone D - Peer Discovery:**
- ❌ Bootstrap peer list
- ❌ Peer table storage
- ❌ Peer discovery protocol

**Milestone E+ - Advanced Features:**
- ❌ Concurrent downloads from multiple peers
- ❌ Rarest-first piece selection
- ❌ Upload/download rate limiting
- ❌ NAT traversal
- ❌ DHT (Distributed Hash Table)
- ❌ Progress UI

---

## Testing the Current Implementation

### Test 1: Start a Seed Peer
```bash
# Terminal 1
java -jar target/peer.jar --seed somefile.txt --port 6881
```

**What happens:**
- Server starts on port 6881
- Waits for connections
- Logs: "Peer server started on port 6881 with ID: xxx"

### Test 2: Connect a Download Peer
```bash
# Terminal 2
java -jar target/peer.jar --download abc123 --bootstrap localhost:6881 --port 6882
```

**What happens:**
- Server starts on port 6882
- Client connects to localhost:6881
- HELLO messages exchanged
- Logs show the handshake

**Expected output:**
```
Terminal 1 (Seeder):
22:47:36.046 [main] INFO  PeerServer - Peer server started on port 6881
22:47:40.123 [worker-1] INFO  PeerServerHandler - New connection from: /127.0.0.1:54321
22:47:40.125 [worker-1] INFO  PeerServerHandler - Received HELLO from peer: peer-456

Terminal 2 (Downloader):
22:47:40.046 [main] INFO  PeerServer - Peer server started on port 6882
22:47:40.123 [main] INFO  PeerClient - Connected to peer at localhost:6881
22:47:40.125 [worker-1] INFO  PeerClientHandler - Received HELLO from peer: peer-123
```

---

## Key Takeaways

1. **P2P = Everyone is both client and server**
   - You download (client) and upload (server) simultaneously

2. **Chunking makes P2P possible**
   - Small pieces can come from anywhere
   - Easy to verify integrity
   - Parallel downloads

3. **Netty handles the hard networking stuff**
   - You focus on business logic
   - Netty handles thousands of connections
   - Clean separation: bytes ↔ messages ↔ logic

4. **Protocol is the language**
   - Simple JSON messages
   - Type-safe with Java classes
   - Easy to extend

5. **Current status: Foundation complete!**
   - Next: Implement file chunking and chunk transfer
   - Then: Multiple peer coordination
   - Finally: Advanced features (DHT, NAT traversal, etc.)

---

## Next Steps

When you're ready to continue:
1. Implement file chunking (split files into chunks)
2. Generate and store manifests
3. Implement chunk request/response handling
4. Add hash verification
5. Test end-to-end file transfer

The foundation is solid - now we build the features on top!
