# P2P File Sharing System - Comprehensive Guide

## Table of Contents
1. [System Overview](#system-overview)
2. [Architecture](#architecture)
3. [Core Components](#core-components)
4. [Protocol Flow](#protocol-flow)
5. [Data Structures](#data-structures)
6. [Network Communication](#network-communication)
7. [File Processing Pipeline](#file-processing-pipeline)
8. [Peer Discovery](#peer-discovery)
9. [Download Process](#download-process)
10. [Step-by-Step Execution Flow](#step-by-step-execution-flow)

---

## System Overview

This is a BitTorrent-like peer-to-peer (P2P) file sharing system built with Java and Netty. The system allows users to share files directly between computers without a central server.

### Key Features
- **Decentralized**: No central server required for file transfer
- **Chunked Transfer**: Files are split into 256KB chunks for efficient transfer
- **Verification**: SHA-256 hashing ensures data integrity
- **Streaming**: Memory-efficient streaming downloads support multi-GB files
- **Peer Discovery**: Automatic peer discovery and swarm building
- **Cross-Device**: Works across different devices on same or different networks

### Technology Stack
- **Java 17**: Core programming language
- **Netty 4.x**: High-performance asynchronous networking
- **Jackson**: JSON serialization for protocol messages
- **Lombok**: Reduces boilerplate code
- **SLF4J + Logback**: Comprehensive logging

---

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        P2P Network                          │
│                                                             │
│  ┌──────────┐         ┌──────────┐         ┌──────────┐   │
│  │  Peer A  │◄───────►│  Peer B  │◄───────►│  Peer C  │   │
│  │  (Seed)  │         │(Leecher) │         │(Leecher) │   │
│  └──────────┘         └──────────┘         └──────────┘   │
│       │                     │                     │         │
│       └─────────────────────┴─────────────────────┘         │
│              Direct peer-to-peer connections                │
└─────────────────────────────────────────────────────────────┘
```

### Component Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                         Peer Node                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                  CLI Layer                          │   │
│  │  (PeerCLI, CLIParser, CLIArguments)                │   │
│  └─────────────────────────────────────────────────────┘   │
│                          │                                  │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              Network Layer                          │   │
│  │  ┌──────────────┐            ┌──────────────┐      │   │
│  │  │ PeerServer   │            │ PeerClient   │      │   │
│  │  │ (Incoming)   │            │ (Outgoing)   │      │   │
│  │  └──────────────┘            └──────────────┘      │   │
│  │         │                            │              │   │
│  │  ┌──────────────┐            ┌──────────────┐      │   │
│  │  │ServerHandler │            │ClientHandler │      │   │
│  │  └──────────────┘            └──────────────┘      │   │
│  │                                                     │   │
│  │  ┌─────────────────────────────────────────┐       │   │
│  │  │        MessageCodec                     │       │   │
│  │  │  (JSON Encoding/Decoding)               │       │   │
│  │  └─────────────────────────────────────────┘       │   │
│  └─────────────────────────────────────────────────────┘   │
│                          │                                  │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              Protocol Layer                         │   │
│  │  (Message Types: HELLO, MANIFEST, CHUNK, etc.)     │   │
│  └─────────────────────────────────────────────────────┘   │
│                          │                                  │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              Business Logic Layer                   │   │
│  │  ┌──────────────┐  ┌──────────────┐                │   │
│  │  │DownloadMgr   │  │ PeerRegistry │                │   │
│  │  └──────────────┘  └──────────────┘                │   │
│  │  ┌──────────────┐  ┌──────────────┐                │   │
│  │  │FileChunker   │  │FileAssembler │                │   │
│  │  └──────────────┘  └──────────────┘                │   │
│  │  ┌──────────────┐                                   │   │
│  │  │ManifestGen   │                                   │   │
│  │  └──────────────┘                                   │   │
│  └─────────────────────────────────────────────────────┘   │
│                          │                                  │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              Storage Layer                          │   │
│  │  ┌──────────────┐  ┌──────────────┐                │   │
│  │  │ChunkStorage  │  │ManifestStore │                │   │
│  │  └──────────────┘  └──────────────┘                │   │
│  │         │                   │                       │   │
│  │     .chunks/           manifests/                   │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## Core Components

### 1. Data Models (com.p2p.core)

#### ChunkInfo
```java
public class ChunkInfo {
    private int index;       // Chunk index in file (0-based)
    private String hash;     // SHA-256 hash for verification
    private long size;       // Chunk size in bytes
}
```
- Represents metadata for a single chunk
- Used for verification during download

#### Manifest
```java
public class Manifest {
    private final String fileId;           // Unique file identifier (SHA-256)
    private final String filename;         // Original filename
    private final long fileSize;           // Total file size in bytes
    private final int chunkSize;           // Size of each chunk (256KB)
    private final List<ChunkInfo> chunks;  // List of all chunks
}
```
- Contains all metadata about a shared file
- Acts as a "recipe" for downloading the complete file

#### PeerInfo
```java
public class PeerInfo {
    private String peerId;                 // Unique peer identifier
    private String host;                   // IP address or hostname
    private int port;                      // Port number
    private long lastSeen;                 // Last communication timestamp
    private List<String> availableFiles;   // File IDs this peer has
}
```
- Stores information about other peers in the network
- Tracks which files each peer has available

### 2. Protocol Messages (com.p2p.protocol)

#### Message Types
```java
public enum MessageType {
    HELLO,                // Initial peer handshake
    MANIFEST_REQUEST,     // Request file manifest
    MANIFEST_RESPONSE,    // Send file manifest
    CHUNK_REQUEST,        // Request file chunk
    CHUNK_RESPONSE,       // Send file chunk
    PEER_LIST_REQUEST,    // Request known peers
    PEER_LIST_RESPONSE    // Send known peers
}
```

#### Message Hierarchy
```
Message (abstract base class)
├── HelloMessage
├── ManifestRequestMessage
├── ManifestResponseMessage
├── ChunkRequestMessage
├── ChunkResponseMessage
├── PeerListRequestMessage
└── PeerListResponseMessage
```

### 3. Network Layer (com.p2p.network)

#### PeerServer
- Listens for incoming connections from other peers
- Uses Netty's ServerBootstrap for asynchronous I/O
- Manages multiple concurrent connections
- Delegates message handling to PeerServerHandler

#### PeerClient
- Connects to other peers as a client
- Uses Netty's Bootstrap for outgoing connections
- Sends requests and receives responses
- Delegates message handling to PeerClientHandler

#### MessageCodec
- Converts between Java objects and JSON
- Adds 4-byte length prefix to messages
- Works with LengthFieldBasedFrameDecoder for TCP framing

### 4. Business Logic Layer

#### DownloadManager
- Orchestrates the download process
- Connects to peer, requests manifest
- Downloads chunks sequentially
- Verifies each chunk's hash
- Streams chunks directly to disk (memory efficient)

#### FileChunker
- Splits files into 256KB chunks
- Calculates SHA-256 hash for each chunk
- Stores chunks in .chunks/ directory

#### ManifestGenerator
- Creates manifest from chunked file
- Generates unique file ID (SHA-256 of manifest)
- Stores manifest as JSON

#### FileAssembler
- Reassembles file from chunks
- Verifies each chunk before assembly
- Creates final output file

#### PeerRegistry
- Tracks all known peers
- Maps file IDs to peers that have them
- Thread-safe (uses ConcurrentHashMap)

### 5. Storage Layer

#### ChunkStorage
```
.chunks/
├── ba/
│   └── ba9339da...
├── cd/
│   └── cd4f21ab...
└── ef/
    └── ef892345...
```
- Stores chunks in subdirectories (first 2 chars of hash)
- Efficient retrieval by hash

#### ManifestStorage
```
manifests/
├── c592dbd3....json
├── a1b2c3d4....json
└── e5f6g7h8....json
```
- Stores manifests as JSON files
- Filename is the file ID

---

## Protocol Flow

### 1. Handshake (HELLO)

```
Peer A                          Peer B
  │                               │
  │────── TCP Connect ────────────►│
  │                               │
  │────── HELLO ──────────────────►│
  │  (peerId, port, files[])      │
  │                               │
  │◄───── HELLO ───────────────────│
  │  (peerId, port, files[])      │
  │                               │
  │  ✓ Both peers registered      │
```

**HELLO Message Structure:**
```json
{
  "type": "HELLO",
  "peerId": "uuid-1234-5678",
  "port": 8001,
  "availableFiles": ["fileId1", "fileId2"]
}
```

### 2. Manifest Exchange

```
Downloader                      Seeder
  │                               │
  │── MANIFEST_REQUEST ──────────►│
  │  (fileId)                     │
  │                               │
  │◄─ MANIFEST_RESPONSE ──────────│
  │  (manifest with chunks)       │
  │                               │
  │  ✓ Know file structure        │
```

**MANIFEST_REQUEST:**
```json
{
  "type": "MANIFEST_REQUEST",
  "fileId": "c592dbd3..."
}
```

**MANIFEST_RESPONSE:**
```json
{
  "type": "MANIFEST_RESPONSE",
  "manifest": {
    "fileId": "c592dbd3...",
    "filename": "movie.mp4",
    "fileSize": 1048576,
    "chunkSize": 262144,
    "chunks": [
      {"index": 0, "hash": "ba9339da...", "size": 262144},
      {"index": 1, "hash": "cd4f21ab...", "size": 262144},
      {"index": 2, "hash": "ef892345...", "size": 262144},
      {"index": 3, "hash": "12ab34cd...", "size": 262144}
    ]
  }
}
```

### 3. Chunk Transfer

```
Downloader                      Seeder
  │                               │
  │── CHUNK_REQUEST ─────────────►│
  │  (fileId, chunkIndex=0)       │
  │                               │
  │◄─ CHUNK_RESPONSE ─────────────│
  │  (data, hash)                 │
  │                               │
  │  ✓ Verify hash                │
  │  ✓ Write to disk              │
  │                               │
  │── CHUNK_REQUEST ─────────────►│
  │  (fileId, chunkIndex=1)       │
  │                               │
  │◄─ CHUNK_RESPONSE ─────────────│
  │  (data, hash)                 │
  │                               │
  │  ... repeat for all chunks    │
```

**CHUNK_REQUEST:**
```json
{
  "type": "CHUNK_REQUEST",
  "fileId": "c592dbd3...",
  "chunkIndex": 0
}
```

**CHUNK_RESPONSE:**
```json
{
  "type": "CHUNK_RESPONSE",
  "fileId": "c592dbd3...",
  "chunkIndex": 0,
  "data": "base64EncodedChunkData...",
  "hash": "ba9339da..."
}
```

### 4. Peer Discovery

```
New Peer                    Bootstrap Peer              Other Peers
  │                               │                           │
  │── HELLO ─────────────────────►│                           │
  │                               │                           │
  │◄─ HELLO ──────────────────────│                           │
  │  (now registered)             │                           │
  │                               │                           │
  │── PEER_LIST_REQUEST ─────────►│                           │
  │                               │                           │
  │◄─ PEER_LIST_RESPONSE ─────────│                           │
  │  (list of known peers)        │                           │
  │                               │                           │
  │  ✓ Now knows about all peers  │                           │
  │                               │                           │
  │─────── HELLO ─────────────────┼──────────────────────────►│
  │                               │                           │
  │◄────── HELLO ─────────────────┼───────────────────────────│
  │                               │                           │
  │  ✓ Connected to entire swarm  │                           │
```

**PEER_LIST_REQUEST:**
```json
{
  "type": "PEER_LIST_REQUEST"
}
```

**PEER_LIST_RESPONSE:**
```json
{
  "type": "PEER_LIST_RESPONSE",
  "peers": [
    {
      "peerId": "peer-abc",
      "host": "192.168.1.100",
      "port": 8002,
      "availableFiles": ["fileId1"]
    },
    {
      "peerId": "peer-def",
      "host": "192.168.1.101",
      "port": 8003,
      "availableFiles": ["fileId1", "fileId2"]
    }
  ]
}
```

---

## Data Structures

### File Storage Structure

```
project-root/
├── .chunks/                    # Chunk storage
│   ├── ba/
│   │   └── ba9339da...         # Chunk file (binary)
│   ├── cd/
│   │   └── cd4f21ab...
│   └── ef/
│       └── ef892345...
│
├── manifests/                  # Manifest storage
│   ├── c592dbd3....json        # Manifest file
│   └── a1b2c3d4....json
│
└── downloads/                  # Downloaded files
    ├── movie.mp4
    └── document.pdf
```

### Chunk Storage Details

**Chunk Filename:** First 2 characters of hash create subdirectory
```
Hash: ba9339da8c71f2b8d0e5a123456789abcdef
Path: .chunks/ba/ba9339da8c71f2b8d0e5a123456789abcdef
```

**Why subdirectories?**
- Prevents too many files in single directory
- Improves filesystem performance
- Faster lookups

### Manifest File Format

**File: manifests/c592dbd3....json**
```json
{
  "fileId": "c592dbd3012ad93de57a7fcbd6727f1aeef372c30f53029490a7cb0937edf658",
  "filename": "movie.mp4",
  "fileSize": 10485760,
  "chunkSize": 262144,
  "chunks": [
    {
      "index": 0,
      "hash": "ba9339da8c71f2b8d0e5a123456789abcdef1234567890abcdef1234567890ab",
      "size": 262144
    },
    {
      "index": 1,
      "hash": "cd4f21ab9d82e3c9f1a6b234567890abcdef1234567890abcdef1234567890cd",
      "size": 262144
    }
    // ... more chunks
  ]
}
```

---

## Network Communication

### TCP Connection Management

#### Server Side (Incoming Connections)
```java
// PeerServer.start()
ServerBootstrap bootstrap = new ServerBootstrap()
    .group(bossGroup, workerGroup)
    .channel(NioServerSocketChannel.class)
    .childHandler(new ChannelInitializer<SocketChannel>() {
        @Override
        protected void initChannel(SocketChannel ch) {
            ch.pipeline()
                .addLast(new LengthFieldBasedFrameDecoder(
                    100 * 1024 * 1024,  // Max frame size: 100MB
                    0,                   // Length field offset
                    4,                   // Length field length
                    0,                   // Length adjustment
                    4))                  // Initial bytes to strip
                .addLast(new MessageCodec())
                .addLast(new PeerServerHandler(this));
        }
    });
```

**Pipeline Flow:**
```
Incoming Bytes → LengthFieldBasedFrameDecoder → MessageCodec → PeerServerHandler
                 (handles TCP framing)          (JSON decode)   (business logic)
```

#### Client Side (Outgoing Connections)
```java
// PeerClient.connect()
Bootstrap bootstrap = new Bootstrap()
    .group(group)
    .channel(NioSocketChannel.class)
    .handler(new ChannelInitializer<SocketChannel>() {
        @Override
        protected void initChannel(SocketChannel ch) {
            ch.pipeline()
                .addLast(new LengthFieldBasedFrameDecoder(...))
                .addLast(new MessageCodec())
                .addLast(new PeerClientHandler());
        }
    });
```

### Message Encoding/Decoding

#### Encoding (Java Object → Network Bytes)
```
Message Object
    ↓
ObjectMapper.writeValueAsString(msg)
    ↓
JSON String: {"type":"HELLO","peerId":"..."}
    ↓
UTF-8 Bytes: [123, 34, 116, 121, 112, 101, ...]
    ↓
Add Length Prefix (4 bytes): [0, 0, 1, 50, 123, 34, ...]
    ↓
ByteBuf → Network
```

#### Decoding (Network Bytes → Java Object)
```
Network → ByteBuf
    ↓
LengthFieldBasedFrameDecoder: Strip length prefix
    ↓
ByteBuf (just JSON bytes)
    ↓
UTF-8 Decode: {"type":"HELLO","peerId":"..."}
    ↓
ObjectMapper.readValue(json, Message.class)
    ↓
Message Object (HelloMessage, ChunkRequest, etc.)
```

### Frame Format

```
┌────────────┬──────────────────────────────────┐
│   Length   │         JSON Message             │
│  (4 bytes) │      (variable length)           │
├────────────┼──────────────────────────────────┤
│ 0x00000132 │ {"type":"HELLO","peerId":"..."} │
└────────────┴──────────────────────────────────┘
```

**Why length prefix?**
- TCP is a stream protocol (no message boundaries)
- Length prefix allows decoder to know when message is complete
- Prevents partial messages or concatenated messages

---

## File Processing Pipeline

### Seeding a File (Upload)

```
┌─────────────┐
│  Input File │
│  movie.mp4  │
└─────┬───────┘
      │
      ▼
┌─────────────────────────────────────┐
│   Step 1: FileChunker               │
│   - Read file in 256KB chunks       │
│   - Calculate SHA-256 for each      │
│   - Save to .chunks/                │
└─────┬───────────────────────────────┘
      │
      │  Output: List<ChunkInfo>
      │
      ▼
┌─────────────────────────────────────┐
│   Step 2: ManifestGenerator         │
│   - Create Manifest object          │
│   - Calculate file ID (manifest hash)│
│   - Save to manifests/              │
└─────┬───────────────────────────────┘
      │
      │  Output: Manifest with fileId
      │
      ▼
┌─────────────────────────────────────┐
│   Step 3: PeerServer                │
│   - Add fileId to availableFiles    │
│   - Start listening for requests    │
│   - Share file ID with user         │
└─────────────────────────────────────┘
```

### Downloading a File

```
┌─────────────────────────────────────┐
│   Input: fileId + bootstrap peer    │
└─────┬───────────────────────────────┘
      │
      ▼
┌─────────────────────────────────────┐
│   Step 1: DownloadManager.connect   │
│   - Connect to bootstrap peer       │
│   - Send HELLO                      │
│   - Receive HELLO                   │
└─────┬───────────────────────────────┘
      │
      ▼
┌─────────────────────────────────────┐
│   Step 2: Request Manifest          │
│   - Send MANIFEST_REQUEST           │
│   - Receive MANIFEST_RESPONSE       │
│   - Parse manifest                  │
└─────┬───────────────────────────────┘
      │
      │  Output: Manifest object
      │
      ▼
┌─────────────────────────────────────┐
│   Step 3: Download Chunks (Streaming)│
│   - Open FileOutputStream           │
│   - For each chunk:                 │
│     1. Send CHUNK_REQUEST           │
│     2. Receive CHUNK_RESPONSE       │
│     3. Verify hash                  │
│     4. Write directly to file       │
│     5. Flush to disk                │
└─────┬───────────────────────────────┘
      │
      │  Output: Complete file
      │
      ▼
┌─────────────────────────────────────┐
│   Step 4: Finalize                  │
│   - Rename temp file to original name│
│   - Add to local availableFiles     │
│   - Now can seed to others          │
└─────────────────────────────────────┘
```

### Chunk Processing Details

#### Chunking (FileChunker)
```java
File input = new File("movie.mp4");  // 10MB file
int chunkSize = 256 * 1024;          // 256KB

FileInputStream fis = new FileInputStream(input);
byte[] buffer = new byte[chunkSize];
int chunkIndex = 0;

while ((bytesRead = fis.read(buffer)) != -1) {
    // Calculate hash
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    digest.update(buffer, 0, bytesRead);
    String hash = bytesToHex(digest.digest());

    // Save chunk
    chunkStorage.store(hash, Arrays.copyOf(buffer, bytesRead));

    // Create ChunkInfo
    chunks.add(new ChunkInfo(chunkIndex++, hash, bytesRead));
}

// Result: 40 chunks (10MB / 256KB = 40)
```

#### Streaming Download
```java
File outputFile = new File("downloads/movie.mp4");
FileOutputStream fos = new FileOutputStream(outputFile);

for (int i = 0; i < manifest.getChunkCount(); i++) {
    // Request chunk
    ChunkRequestMessage request = new ChunkRequestMessage(fileId, i);
    channel.writeAndFlush(request);

    // Wait for response
    ChunkResponseMessage response = awaitResponse();

    // Verify hash
    String expectedHash = manifest.getChunk(i).getHash();
    String actualHash = calculateHash(response.getDataBytes());
    if (!expectedHash.equals(actualHash)) {
        throw new IOException("Chunk verification failed!");
    }

    // Write directly to disk (no memory accumulation)
    fos.write(response.getDataBytes());
    fos.flush();
}

fos.close();
```

**Memory Efficiency:**
- Old approach: Store all chunks in memory → O(n) memory
- New approach: Stream to disk immediately → O(1) memory
- Enables downloading files larger than available RAM

---

## Peer Discovery

### Discovery Process

```
Scenario: Peer C wants to join a swarm with Peer A and Peer B

┌─────────┐              ┌─────────┐              ┌─────────┐
│ Peer A  │              │ Peer B  │              │ Peer C  │
│ (Seed)  │              │(Leecher)│              │  (New)  │
└────┬────┘              └────┬────┘              └────┬────┘
     │                        │                        │
     │                        │    1. Connect to       │
     │                        │       bootstrap        │
     │◄───────────────────────┼────────────────────────│
     │                        │                        │
     │    2. HELLO exchange   │                        │
     │───────────────────────►│                        │
     │◄───────────────────────┤                        │
     │                        │                        │
     │ ✓ Peer C registered    │                        │
     │   in Peer A's registry │                        │
     │                        │                        │
     │    3. Request peer list│                        │
     │◄───────────────────────┼────────────────────────│
     │                        │                        │
     │    4. Send peer list   │                        │
     │    (includes Peer B)   │                        │
     │───────────────────────►│                        │
     │                        │                        │
     │                        │    5. Connect to B     │
     │                        │◄───────────────────────│
     │                        │                        │
     │                        │    6. HELLO exchange   │
     │                        │───────────────────────►│
     │                        │◄───────────────────────│
     │                        │                        │
     │                        │ ✓ Peer C now knows A&B │
     │                        │ ✓ Can download from both│
```

### PeerRegistry Implementation

```java
public class PeerRegistry {
    // Map: peerId → PeerInfo
    private final Map<String, PeerInfo> peers = new ConcurrentHashMap<>();

    // Map: fileId → Set of peerIds that have it
    private final Map<String, Set<String>> fileProviders = new ConcurrentHashMap<>();

    // Add a peer to registry
    public void addPeer(PeerInfo peerInfo) {
        peers.put(peerInfo.getPeerId(), peerInfo);

        // Index files
        for (String fileId : peerInfo.getAvailableFiles()) {
            fileProviders
                .computeIfAbsent(fileId, k -> ConcurrentHashMap.newKeySet())
                .add(peerInfo.getPeerId());
        }
    }

    // Get all peers that have a specific file
    public List<PeerInfo> getPeersWithFile(String fileId) {
        Set<String> peerIds = fileProviders.get(fileId);
        if (peerIds == null) return Collections.emptyList();

        return peerIds.stream()
            .map(peers::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}
```

### Automatic Registration on HELLO

```java
// PeerServerHandler.handleHello()
private void handleHello(ChannelHandlerContext ctx, HelloMessage msg) {
    // Get peer's IP address
    InetSocketAddress remoteAddress =
        (InetSocketAddress) ctx.channel().remoteAddress();
    String host = remoteAddress.getAddress().getHostAddress();

    // Create PeerInfo
    PeerInfo peerInfo = new PeerInfo(
        msg.getPeerId(),
        host,
        msg.getPort(),
        msg.getAvailableFiles()
    );

    // Register in peer registry
    server.getPeerRegistry().addPeer(peerInfo);

    // Send our HELLO back
    sendHello(ctx);
}
```

---

## Download Process

### Complete Download Flow

```
┌──────────────────────────────────────────────────────────────┐
│  User Command: java -jar peer.jar --download <fileId>        │
│                --bootstrap 192.168.1.100:8001                │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────┐
│  Step 1: Parse CLI Arguments                                 │
│  - Extract fileId: c592dbd3...                              │
│  - Extract bootstrap host: 192.168.1.100                    │
│  - Extract bootstrap port: 8001                             │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────┐
│  Step 2: Start Local PeerServer                             │
│  - Create PeerServer on port 8002                           │
│  - Start listening for incoming connections                 │
│  - Initialize storage (chunks, manifests)                   │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────┐
│  Step 3: Create DownloadManager                             │
│  - Initialize with bootstrap peer info                      │
│  - Initialize with local storage                            │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────┐
│  Step 4: Connect to Bootstrap Peer                          │
│                                                              │
│  PeerClient.connect("192.168.1.100", 8001)                  │
│  - Create Netty Bootstrap                                   │
│  - Set up pipeline (Decoder → Codec → Handler)              │
│  - Establish TCP connection                                 │
│  - Wait for connection to complete                          │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────┐
│  Step 5: Send HELLO Message                                 │
│                                                              │
│  HelloMessage {                                              │
│    peerId: "my-peer-id-uuid"                                │
│    port: 8002                                               │
│    availableFiles: []  // Empty, we're downloading          │
│  }                                                           │
│                                                              │
│  - Encode to JSON                                           │
│  - Add length prefix                                        │
│  - Send over TCP                                            │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────┐
│  Step 6: Receive HELLO Response                             │
│                                                              │
│  HelloMessage {                                              │
│    peerId: "bootstrap-peer-uuid"                            │
│    port: 8001                                               │
│    availableFiles: ["c592dbd3..."]  // Has our file!        │
│  }                                                           │
│                                                              │
│  - Decode from network bytes                                │
│  - Parse JSON to HelloMessage                               │
│  - Connection established                                   │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────┐
│  Step 7: Request File Manifest                              │
│                                                              │
│  ManifestRequestMessage {                                    │
│    fileId: "c592dbd3..."                                    │
│  }                                                           │
│                                                              │
│  - Send request                                             │
│  - Wait for response                                        │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────┐
│  Step 8: Receive Manifest                                   │
│                                                              │
│  ManifestResponseMessage {                                   │
│    manifest: {                                              │
│      fileId: "c592dbd3..."                                  │
│      filename: "movie.mp4"                                  │
│      fileSize: 10485760 (10MB)                              │
│      chunkSize: 262144 (256KB)                              │
│      chunks: [                                              │
│        {index: 0, hash: "ba9339...", size: 262144},         │
│        {index: 1, hash: "cd4f21...", size: 262144},         │
│        ... (40 chunks total)                                │
│      ]                                                       │
│    }                                                         │
│  }                                                           │
│                                                              │
│  - Parse manifest                                           │
│  - Display: "Downloading movie.mp4 (10MB, 40 chunks)"       │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────┐
│  Step 9: Create Output File                                 │
│                                                              │
│  File tempFile = new File("downloads/c592dbd3.tmp")         │
│  FileOutputStream fos = new FileOutputStream(tempFile)       │
│                                                              │
│  - Create temporary file                                    │
│  - Open for writing                                         │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────┐
│  Step 10: Download Chunks (Loop)                            │
│                                                              │
│  For chunkIndex = 0 to 39:                                  │
│                                                              │
│    10a. Request Chunk                                       │
│    ─────────────────────────────────                        │
│    ChunkRequestMessage {                                     │
│      fileId: "c592dbd3..."                                  │
│      chunkIndex: 0                                          │
│    }                                                         │
│    - Send request                                           │
│    - Wait for response                                      │
│                                                              │
│    10b. Receive Chunk                                       │
│    ─────────────────────────────────                        │
│    ChunkResponseMessage {                                    │
│      fileId: "c592dbd3..."                                  │
│      chunkIndex: 0                                          │
│      data: "iVBORw0KG..." (Base64)                          │
│      hash: "ba9339..."                                      │
│    }                                                         │
│    - Decode Base64 to bytes                                 │
│    - Chunk size: 262144 bytes                               │
│                                                              │
│    10c. Verify Chunk                                        │
│    ─────────────────────────────────                        │
│    byte[] chunkData = base64Decode(response.data)           │
│    String actualHash = sha256(chunkData)                    │
│    String expectedHash = manifest.chunks[0].hash            │
│                                                              │
│    if (actualHash != expectedHash) {                        │
│      throw new IOException("Chunk corrupted!")              │
│    }                                                         │
│                                                              │
│    10d. Write to Disk                                       │
│    ─────────────────────────────────                        │
│    fos.write(chunkData)                                     │
│    fos.flush()                                              │
│    - Immediately write to file (no memory accumulation)     │
│    - Progress: "Downloaded 1/40 chunks (2.5%)"              │
│                                                              │
│  Repeat for all 40 chunks...                                │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────┐
│  Step 11: Finalize Download                                 │
│                                                              │
│  fos.close()                                                 │
│  - Close output stream                                      │
│                                                              │
│  File finalFile = new File("downloads/movie.mp4")           │
│  tempFile.renameTo(finalFile)                               │
│  - Rename temp file to original filename                    │
│                                                              │
│  Display: "Download complete: movie.mp4"                    │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────┐
│  Step 12: Become a Seeder                                   │
│                                                              │
│  - Store manifest in local manifests/                       │
│  - Add fileId to availableFiles list                        │
│  - Can now share this file with others                      │
│  - Server continues running, ready to upload                │
└──────────────────────────────────────────────────────────────┘
```

---

## Step-by-Step Execution Flow

### Example: Complete File Sharing Session

#### Scenario
- **Peer A** (192.168.1.100:8001) has `movie.mp4`
- **Peer B** (192.168.1.101:8002) wants to download it
- **Peer C** (192.168.1.102:8003) joins later

---

### Phase 1: Peer A Seeds the File

```bash
# Terminal 1: Peer A
$ java -jar target/peer.jar --seed movie.mp4 --port 8001
```

**Internal Process:**

1. **Parse Arguments**
   ```
   Mode: SEED
   File: movie.mp4
   Port: 8001
   ```

2. **Generate Peer ID**
   ```java
   String peerId = UUID.randomUUID().toString();
   // peerId = "a1b2c3d4-5678-90ab-cdef-1234567890ab"
   ```

3. **Chunk the File**
   ```
   File: movie.mp4 (10,485,760 bytes = 10MB)
   Chunk size: 262,144 bytes (256KB)
   Number of chunks: 40

   Chunking:
   - Read chunk 0 (262144 bytes) → hash: ba9339da...
   - Read chunk 1 (262144 bytes) → hash: cd4f21ab...
   - Read chunk 2 (262144 bytes) → hash: ef892345...
   ...
   - Read chunk 39 (262144 bytes) → hash: 12ab34cd...

   Stored in: .chunks/ba/ba9339da...
               .chunks/cd/cd4f21ab...
               ...
   ```

4. **Generate Manifest**
   ```json
   {
     "fileId": "c592dbd3012ad93de57a7fcbd6727f1aeef372c30f53029490a7cb0937edf658",
     "filename": "movie.mp4",
     "fileSize": 10485760,
     "chunkSize": 262144,
     "chunks": [
       {"index": 0, "hash": "ba9339da...", "size": 262144},
       {"index": 1, "hash": "cd4f21ab...", "size": 262144},
       ...
     ]
   }
   ```

   Stored in: `manifests/c592dbd3....json`

5. **Start Server**
   ```
   11:09:24.714 [main] INFO PeerServer - Peer server started on port 8001
   Peer ID: a1b2c3d4-5678-90ab-cdef-1234567890ab

   === FILE READY FOR SHARING ===
   File ID: c592dbd3012ad93de57a7fcbd6727f1aeef372c30f53029490a7cb0937edf658
   Filename: movie.mp4
   Share this File ID with others!
   ================================
   ```

---

### Phase 2: Peer B Downloads the File

```bash
# Terminal 2: Peer B
$ java -jar target/peer.jar \
    --download c592dbd3012ad93de57a7fcbd6727f1aeef372c30f53029490a7cb0937edf658 \
    --bootstrap 192.168.1.100:8001 \
    --port 8002
```

**Internal Process:**

1. **Parse Arguments**
   ```
   Mode: DOWNLOAD
   File ID: c592dbd3...
   Bootstrap: 192.168.1.100:8001
   Port: 8002
   ```

2. **Start Local Server**
   ```
   11:10:15.234 [main] INFO PeerServer - Peer server started on port 8002
   Peer ID: b2c3d4e5-6789-01ab-cdef-234567890abc
   ```

3. **Connect to Peer A**
   ```
   11:10:15.456 [main] INFO DownloadManager - Connecting to 192.168.1.100:8001
   11:10:15.567 [main] INFO PeerClient - Connected to 192.168.1.100:8001
   ```

4. **Handshake (HELLO Exchange)**
   ```
   Peer B → Peer A:
   {
     "type": "HELLO",
     "peerId": "b2c3d4e5-6789-01ab-cdef-234567890abc",
     "port": 8002,
     "availableFiles": []
   }

   Peer A → Peer B:
   {
     "type": "HELLO",
     "peerId": "a1b2c3d4-5678-90ab-cdef-1234567890ab",
     "port": 8001,
     "availableFiles": ["c592dbd3..."]
   }

   11:10:15.678 [main] INFO DownloadManager - Handshake complete
   ```

5. **Request Manifest**
   ```
   Peer B → Peer A:
   {
     "type": "MANIFEST_REQUEST",
     "fileId": "c592dbd3..."
   }

   11:10:15.789 [main] INFO DownloadManager - Requesting manifest
   ```

6. **Receive Manifest**
   ```
   Peer A → Peer B:
   {
     "type": "MANIFEST_RESPONSE",
     "manifest": { ... }
   }

   11:10:15.890 [main] INFO DownloadManager - Received manifest for movie.mp4
   11:10:15.891 [main] INFO DownloadManager - File size: 10MB, 40 chunks
   ```

7. **Download Chunks**
   ```
   11:10:16.000 [main] INFO DownloadManager - Downloading chunk 0/40
   Peer B → Peer A:
   {
     "type": "CHUNK_REQUEST",
     "fileId": "c592dbd3...",
     "chunkIndex": 0
   }

   Peer A → Peer B:
   {
     "type": "CHUNK_RESPONSE",
     "fileId": "c592dbd3...",
     "chunkIndex": 0,
     "data": "iVBORw0KG..." (262144 bytes, Base64)
     "hash": "ba9339da..."
   }

   11:10:16.123 [main] DEBUG DownloadManager - Chunk 0 verified and saved
   11:10:16.124 [main] INFO DownloadManager - Progress: 1/40 (2.5%)

   11:10:16.200 [main] INFO DownloadManager - Downloading chunk 1/40
   ... (repeat for all chunks)

   11:10:20.456 [main] INFO DownloadManager - Progress: 40/40 (100%)
   ```

8. **Finalize Download**
   ```
   11:10:20.567 [main] INFO DownloadManager - Download complete
   11:10:20.568 [main] INFO DownloadManager - Saved as: downloads/movie.mp4

   === DOWNLOAD COMPLETE ===
   File: movie.mp4
   Size: 10MB
   Location: downloads/movie.mp4
   ========================
   ```

---

### Phase 3: Peer C Joins and Downloads

```bash
# Terminal 3: Peer C
$ java -jar target/peer.jar \
    --download c592dbd3012ad93de57a7fcbd6727f1aeef372c30f53029490a7cb0937edf658 \
    --bootstrap 192.168.1.100:8001 \
    --port 8003
```

**Internal Process:**

1. **Connect to Bootstrap (Peer A)**
   ```
   11:15:00.000 [main] INFO DownloadManager - Connecting to 192.168.1.100:8001
   11:15:00.123 [main] INFO PeerClient - Connected
   ```

2. **HELLO Exchange with Peer A**
   ```
   Peer C ↔ Peer A: HELLO exchange
   11:15:00.234 [main] INFO DownloadManager - Handshake complete
   ```

3. **Request Peer List**
   ```
   Peer C → Peer A:
   {
     "type": "PEER_LIST_REQUEST"
   }

   Peer A → Peer C:
   {
     "type": "PEER_LIST_RESPONSE",
     "peers": [
       {
         "peerId": "b2c3d4e5...",
         "host": "192.168.1.101",
         "port": 8002,
         "availableFiles": ["c592dbd3..."]
       }
     ]
   }

   11:15:00.345 [main] INFO DownloadManager - Discovered 1 other peer
   ```

4. **Download from Multiple Peers**
   ```
   Now Peer C can download chunks from:
   - Peer A (192.168.1.100:8001)
   - Peer B (192.168.1.101:8002)

   (Current implementation downloads from one peer,
    but architecture supports multi-peer downloads)
   ```

---

### Network Traffic Example

Let's analyze the actual bytes sent over the network for one chunk transfer:

#### CHUNK_REQUEST

**Message Object:**
```java
ChunkRequestMessage {
  fileId: "c592dbd3...",
  chunkIndex: 0
}
```

**JSON Encoding:**
```json
{"type":"CHUNK_REQUEST","fileId":"c592dbd3012ad93de57a7fcbd6727f1aeef372c30f53029490a7cb0937edf658","chunkIndex":0}
```

**Byte Array (UTF-8):**
```
Length: 132 bytes
```

**Wire Format (with length prefix):**
```
[0, 0, 0, 132] + [123, 34, 116, 121, 112, 101, 34, ...]
 └─ 4 bytes ─┘   └──────── 132 bytes ────────────┘
   (length)              (JSON message)

Total: 136 bytes
```

#### CHUNK_RESPONSE

**Message Object:**
```java
ChunkResponseMessage {
  fileId: "c592dbd3...",
  chunkIndex: 0,
  data: [262144 bytes, Base64 encoded],
  hash: "ba9339da..."
}
```

**Base64 Encoding:**
```
Raw bytes: 262144 bytes
Base64: 262144 * 4/3 = 349525 bytes
```

**JSON:**
```json
{
  "type":"CHUNK_RESPONSE",
  "fileId":"c592dbd3...",
  "chunkIndex":0,
  "data":"iVBORw0KGgoAAAANSUhEUgAA..." (349525 chars),
  "hash":"ba9339da..."
}
```

**Wire Format:**
```
Length: ~349700 bytes
[0, 5, 86, 68] + [123, 34, 116, ...]
 └─ 4 bytes ─┘   └── 349700 bytes ──┘

Total: ~349704 bytes (~341 KB)
```

**Overhead Calculation:**
```
Raw chunk: 262144 bytes (256 KB)
Over network: 349704 bytes (341 KB)
Overhead: 87560 bytes (33% increase due to Base64 + JSON)
```

---

### Error Handling

#### Chunk Verification Failure

```java
ChunkInfo expected = manifest.getChunk(chunkIndex);
String expectedHash = expected.getHash();

byte[] chunkData = response.getDataBytes();
String actualHash = calculateSHA256(chunkData);

if (!expectedHash.equals(actualHash)) {
    logger.error("Chunk {} verification failed!", chunkIndex);
    logger.error("Expected: {}", expectedHash);
    logger.error("Actual: {}", actualHash);
    throw new IOException("Chunk corrupted or tampered!");
}
```

**Output:**
```
11:10:17.123 [main] ERROR DownloadManager - Chunk 5 verification failed!
11:10:17.124 [main] ERROR DownloadManager - Expected: cd4f21ab...
11:10:17.125 [main] ERROR DownloadManager - Actual: 12345678...
Exception in thread "main" java.io.IOException: Chunk corrupted or tampered!
```

#### Connection Timeout

```java
ChannelFuture future = bootstrap.connect(host, port);
if (!future.await(30, TimeUnit.SECONDS)) {
    throw new IOException("Connection timeout after 30 seconds");
}
```

**Output:**
```
11:10:15.456 [main] INFO PeerClient - Connecting to 192.168.1.100:8001
11:10:45.456 [main] ERROR PeerClient - Connection timeout
Exception in thread "main" java.io.IOException: Connection timeout after 30 seconds
```

---

## Performance Characteristics

### Memory Usage

| Operation | Memory Usage | Notes |
|-----------|--------------|-------|
| Chunking | O(1) | Only one chunk in memory at a time |
| Downloading | O(1) | Streaming to disk, no accumulation |
| Seeding | O(1) | Read chunk from disk when requested |
| Manifest | O(n) | n = number of chunks (minimal) |

### Network Bandwidth

| File Size | Chunks | Network Data | Overhead |
|-----------|--------|--------------|----------|
| 1 MB | 4 | ~1.33 MB | 33% |
| 10 MB | 40 | ~13.3 MB | 33% |
| 100 MB | 400 | ~133 MB | 33% |
| 1 GB | 4096 | ~1.33 GB | 33% |

**Overhead Sources:**
- Base64 encoding: +33%
- JSON structure: minimal
- Length prefix: 4 bytes per message

### Throughput

**Single Peer Download:**
```
Network: 100 Mbps
Effective: ~75 Mbps (after overhead)
256KB chunk: ~0.027 seconds
10MB file (40 chunks): ~1.08 seconds
```

**Multi-Peer Download (Future Enhancement):**
```
3 peers @ 100 Mbps each
Effective: ~225 Mbps aggregate
10MB file: ~0.36 seconds
```

---

## Security Considerations

### Current Security Features

1. **Data Integrity**
   - SHA-256 hashing for all chunks
   - Automatic verification on download
   - Tampered data detected immediately

2. **File ID Verification**
   - Manifest hash serves as file fingerprint
   - Cannot substitute different file with same ID

### Limitations (Future Work)

1. **No Encryption**
   - Data sent in plaintext (Base64, not encrypted)
   - Susceptible to eavesdropping

2. **No Authentication**
   - No peer identity verification
   - Susceptible to Sybil attacks

3. **No Access Control**
   - Anyone can download if they have file ID
   - No permission system

### Recommended Enhancements

1. **TLS/SSL**
   - Encrypt all network communication
   - Use Netty's SslHandler

2. **Peer Authentication**
   - Public key cryptography
   - Signed HELLO messages

3. **File Encryption**
   - Encrypt chunks before storage
   - Symmetric key shared via secure channel

---

## Troubleshooting

### Common Issues

#### 1. "Connection refused"

**Problem:**
```
Exception: java.net.ConnectException: Connection refused
```

**Causes:**
- Bootstrap peer not running
- Wrong IP address or port
- Firewall blocking connection

**Solution:**
```bash
# Verify peer is running
netstat -an | grep 8001

# Check firewall (macOS)
sudo pfctl -s rules

# Check firewall (Linux)
sudo iptables -L
```

#### 2. "Chunk verification failed"

**Problem:**
```
ERROR: Chunk 5 verification failed!
```

**Causes:**
- Network corruption (rare)
- Bug in chunking/assembly
- Disk corruption

**Solution:**
- Retry download
- Check disk health
- Verify seeder's chunks are intact

#### 3. "OutOfMemoryError"

**Problem:**
```
Exception: java.lang.OutOfMemoryError: Java heap space
```

**Causes:**
- Very large file (pre-streaming fix)
- Memory leak

**Solution:**
```bash
# Increase heap size
java -Xmx4g -jar peer.jar ...

# Current version uses streaming (should not happen)
```

#### 4. "File not found"

**Problem:**
```
ERROR: File abc123 not found
```

**Causes:**
- Wrong file ID
- Seeder doesn't have file
- Manifest not generated

**Solution:**
- Verify file ID is correct
- Check seeder's availableFiles list
- Re-seed the file

---

## Future Enhancements

### Milestone E: Swarm & Scheduling
- Download from multiple peers simultaneously
- Rarest-first chunk selection
- Upload/download rate management

### Milestone F: NAT Traversal & Security
- UPnP for automatic port forwarding
- STUN/TURN for NAT traversal
- TLS encryption
- Peer authentication

### Milestone G: UX & Observability
- Web UI for management
- Real-time progress bars
- Network visualization
- Performance metrics

### Milestone H: Testing & QA
- Unit tests for all components
- Integration tests
- Stress testing
- Security audits

### Milestone I: Packaging & Demo
- Docker containers
- Installation packages
- Demo video
- Comprehensive documentation

---

## Conclusion

This P2P file sharing system demonstrates core concepts of distributed systems:

- **Decentralization**: No single point of failure
- **Scalability**: More peers = more bandwidth
- **Reliability**: Data integrity through hashing
- **Efficiency**: Chunking and streaming

The architecture is modular and extensible, making it suitable for learning, experimentation, and enhancement.

For more information, see:
- [ARCHITECTURE.md](ARCHITECTURE.md) - System architecture details
- [DOWNLOAD_GUIDE.md](DOWNLOAD_GUIDE.md) - Download process guide
- [MULTI_DEVICE_SETUP.md](MULTI_DEVICE_SETUP.md) - Cross-device setup
- [HOW_TO_TEST.md](HOW_TO_TEST.md) - Testing instructions
