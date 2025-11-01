# How to Download Chunked Files - Complete Guide

This guide explains how the P2P download process works and how to use it.

---

## Understanding the Download Process

When you download a file in this P2P network:

1. **File is already chunked** by the seeder (automatically when they run `--seed`)
2. **You request the manifest** (tells you how many chunks, their hashes, etc.)
3. **You download each chunk** one by one
4. **Chunks are verified** (SHA-256 hash check)
5. **File is assembled** automatically from all chunks
6. **You become a seeder** - others can now download from you!

---

## Step-by-Step: Download a File

### Step 1: Get the File ID

When someone seeds a file, they'll see output like this:

```
=== FILE READY FOR SHARING ===
File ID: 8e0d5c6e96aa33d5b01c9552e46160f215e128f503db8ed3f49d17bbbc975ab1
Filename: myfile.txt
Share this File ID with others to let them download!
================================
```

**Copy that File ID** - you need it to download!

### Step 2: Run the Download Command

```bash
java -jar target/peer.jar --download <FILE_ID> --bootstrap <PEER_HOST>:<PEER_PORT> --port <YOUR_PORT>
```

**Parameters:**
- `<FILE_ID>`: The File ID you copied from the seeder
- `<PEER_HOST>`: IP or hostname of the seeder (use `localhost` for same machine)
- `<PEER_PORT>`: Port the seeder is listening on
- `<YOUR_PORT>`: Port for your peer (different from seeder's port!)

**Example:**
```bash
java -jar target/peer.jar \
  --download 8e0d5c6e96aa33d5b01c9552e46160f215e128f503db8ed3f49d17bbbc975ab1 \
  --bootstrap localhost:6881 \
  --port 6882
```

### Step 3: Watch the Download Progress

You'll see logs like:

```
Connected to peer at localhost:6881
Requesting manifest for file: 8e0d5c6e...
Received manifest: myfile.txt (4 chunks)
Requesting chunk 1/4
Received chunk 1/4 (262144 bytes)
Requesting chunk 2/4
Received chunk 2/4 (262144 bytes)
Requesting chunk 3/4
Received chunk 3/4 (262144 bytes)
Requesting chunk 4/4
Received chunk 4/4 (131072 bytes)
Assembling file...

=== DOWNLOAD COMPLETE ===
File saved to: downloads/8e0d5c6e.download
File size: 917504 bytes
You can now share this file with others!
=========================
```

### Step 4: Find Your Downloaded File

Your file will be in the `downloads/` directory:

```bash
ls -lh downloads/
```

The filename will be: `<first8chars_of_fileID>.download`

For example: `8e0d5c6e.download`

### Step 5: You're Now a Seeder!

After downloading, your peer **automatically becomes a seeder**. This means:
- ✅ Your chunks are stored in `.chunks/`
- ✅ Your peer server is running
- ✅ Other peers can download from YOU now!

Keep your peer running to share with others!

---

## Complete Example: Two Peers

### Scenario: Alice wants to share a file with Bob

**Alice's Machine (Terminal 1):**

```bash
# Alice creates a file
echo "Hello from Alice! This is my shared file." > alice-file.txt

# Alice seeds the file
java -jar target/peer.jar --seed alice-file.txt --port 6881
```

Alice sees:
```
File ID: abc123def456...
Filename: alice-file.txt
```

Alice shares the File ID with Bob (via email, chat, etc.)

**Bob's Machine (Terminal 2):**

```bash
# Bob downloads using Alice's File ID and address
java -jar target/peer.jar \
  --download abc123def456... \
  --bootstrap alice-ip:6881 \
  --port 6882
```

Bob sees:
```
Received manifest: alice-file.txt (1 chunks)
Requesting chunk 1/1
Received chunk 1/1 (44 bytes)

=== DOWNLOAD COMPLETE ===
File saved to: downloads/abc123de.download
```

**Bob verifies:**
```bash
cat downloads/abc123de.download
# Output: Hello from Alice! This is my shared file.
```

✅ Success! Bob has Alice's file!

---

## How Chunks Work Behind the Scenes

### When Alice Seeds:

1. **Chunking:**
   ```
   Original file: alice-file.txt (1 MB)
   ↓
   Chunk 0: 256 KB
   Chunk 1: 256 KB
   Chunk 2: 256 KB
   Chunk 3: 232 KB
   ```

2. **Hashing:**
   ```
   Chunk 0 → SHA-256 → abc123...
   Chunk 1 → SHA-256 → def456...
   Chunk 2 → SHA-256 → ghi789...
   Chunk 3 → SHA-256 → jkl012...
   ```

3. **Storage:**
   ```
   .chunks/
     ab/abc123...  (Chunk 0 stored by hash)
     de/def456...  (Chunk 1)
     gh/ghi789...  (Chunk 2)
     jk/jkl012...  (Chunk 3)
   ```

4. **Manifest Creation:**
   ```json
   {
     "fileId": "file-abc123...",
     "filename": "alice-file.txt",
     "fileSize": 1048576,
     "chunkSize": 262144,
     "chunks": [
       {"index": 0, "hash": "abc123...", "size": 262144},
       {"index": 1, "hash": "def456...", "size": 262144},
       {"index": 2, "hash": "ghi789...", "size": 262144},
       {"index": 3, "hash": "jkl012...", "size": 232448}
     ]
   }
   ```

### When Bob Downloads:

1. **Connect to Alice**
2. **Request Manifest:**
   ```
   Bob → Alice: "Give me manifest for file-abc123..."
   Alice → Bob: [sends manifest JSON]
   ```

3. **Download Each Chunk:**
   ```
   Bob → Alice: "Give me chunk 0 of file-abc123"
   Alice → Bob: [sends 256 KB of data + hash]
   Bob: [verifies hash matches] ✅
   Bob: [stores chunk in .chunks/ab/abc123...]

   Bob → Alice: "Give me chunk 1 of file-abc123"
   Alice → Bob: [sends 256 KB of data + hash]
   Bob: [verifies hash matches] ✅
   Bob: [stores chunk in .chunks/de/def456...]

   ... (continues for all chunks)
   ```

4. **Assemble File:**
   ```
   Bob reads: .chunks/ab/abc123... (Chunk 0)
   Bob reads: .chunks/de/def456... (Chunk 1)
   Bob reads: .chunks/gh/ghi789... (Chunk 2)
   Bob reads: .chunks/jk/jkl012... (Chunk 3)

   Bob writes: downloads/abc123de.download
   (All chunks concatenated in order)
   ```

5. **Verification:**
   ```
   Expected file size: 1,048,576 bytes
   Actual file size: 1,048,576 bytes
   ✅ File complete and verified!
   ```

---

## Multi-Peer Download (The P2P Magic!)

### Scenario: Charlie wants the file Bob just downloaded

**Bob is now seeding** (because he downloaded it!)

**Charlie's Machine:**
```bash
# Charlie can download from Bob instead of Alice!
java -jar target/peer.jar \
  --download abc123def456... \
  --bootstrap bob-ip:6882 \
  --port 6883
```

**What happens:**
- Charlie connects to Bob
- Bob sends the chunks he downloaded from Alice
- Charlie gets the same file!
- Charlie now becomes a seeder too!

**Result:** The more people download, the faster the network becomes! 🚀

---

## What Gets Downloaded?

When you download a file, these things happen on your machine:

### 1. Chunks Directory (`.chunks/`)
```
.chunks/
  ab/
    abc123def456... (Chunk 0)
  de/
    def456ghi789... (Chunk 1)
  gh/
    ghi789jkl012... (Chunk 2)
```

**Purpose:** Store chunks for future sharing

### 2. Downloads Directory (`downloads/`)
```
downloads/
  abc123de.download (The assembled file)
```

**Purpose:** Your usable file (rename it to use it!)

### 3. No Manifest (Yet)
Currently, the manifest isn't stored for downloaded files (minor TODO).
But you have all the chunks, so you can still seed!

---

## Verifying Your Download

### Check File Size
```bash
ls -lh downloads/*.download
```

Compare with the original file size from the manifest.

### Check Content
```bash
cat downloads/*.download
# or
file downloads/*.download
```

### Verify Integrity (Advanced)
```bash
# Compare checksums with original
shasum -a 256 original-file.txt
shasum -a 256 downloads/*.download

# They should match!
```

---

## Troubleshooting Downloads

### Problem: "Timeout waiting for manifest"

**Cause:** Can't connect to seeder or seeder not responding

**Solutions:**
1. Check seeder is still running
2. Verify bootstrap address is correct
3. Check firewall/network settings
4. Try a different port

### Problem: "Timeout waiting for chunk X"

**Cause:** Connection lost during download

**Solutions:**
1. Check network connection
2. Restart download (chunks already downloaded are stored!)
3. Try connecting to a different peer

### Problem: "Chunk hash mismatch"

**Cause:** Corrupted data during transfer

**Solutions:**
1. Chunk is automatically rejected
2. Download will retry or fail
3. Contact seeder to verify their chunks

### Problem: "File not found in downloads/"

**Cause:** Download failed before completion

**Solutions:**
1. Check `/tmp/peer*.log` for error messages
2. Verify seeder has the complete file
3. Check disk space on your machine

---

## Advanced: Downloading from Multiple Peers

**(Not yet implemented, but planned for Milestone E)**

In the future, you'll be able to download different chunks from different peers simultaneously:

```
You download from:
- Peer A: Chunks 0, 2, 4
- Peer B: Chunks 1, 3, 5
- Peer C: Chunks 6, 7, 8

All in parallel! Much faster! 🚀
```

---

## Tips for Better Downloads

### 1. Keep Seeding After Downloading
```bash
# After download completes, your peer keeps running
# Press Ctrl+C only when you want to stop sharing
```

### 2. Download Large Files Patiently
```
Small file (< 1 MB): ~1 second
Medium file (10 MB): ~5-10 seconds
Large file (100 MB): ~1-2 minutes
Very large file (1 GB): ~10-20 minutes
```

### 3. Rename Downloaded Files
```bash
# Downloaded files have generic names
mv downloads/abc123de.download ~/Documents/actual-filename.pdf
```

### 4. Check Available Disk Space
```bash
df -h .
# Make sure you have enough space for the download!
```

---

## Summary: Download Workflow

```
┌─────────────────────────────────────────┐
│ 1. Get File ID from seeder              │
│    Example: abc123def456...             │
└──────────────┬──────────────────────────┘
               ↓
┌─────────────────────────────────────────┐
│ 2. Run download command                 │
│    --download <FILE_ID>                 │
│    --bootstrap <HOST>:<PORT>            │
└──────────────┬──────────────────────────┘
               ↓
┌─────────────────────────────────────────┐
│ 3. Connect to seeder                    │
│    Send HELLO handshake                 │
└──────────────┬──────────────────────────┘
               ↓
┌─────────────────────────────────────────┐
│ 4. Request manifest                     │
│    Receive file metadata & chunk list   │
└──────────────┬──────────────────────────┘
               ↓
┌─────────────────────────────────────────┐
│ 5. Download chunks sequentially         │
│    For each chunk:                      │
│    - Request chunk                      │
│    - Receive data                       │
│    - Verify hash                        │
│    - Store in .chunks/                  │
└──────────────┬──────────────────────────┘
               ↓
┌─────────────────────────────────────────┐
│ 6. Assemble file                        │
│    Concatenate all chunks in order      │
│    Save to downloads/                   │
└──────────────┬──────────────────────────┘
               ↓
┌─────────────────────────────────────────┐
│ 7. Become seeder!                       │
│    Keep peer running to share           │
└─────────────────────────────────────────┘
```

---

## Quick Reference

### Download Command Template
```bash
java -jar target/peer.jar \
  --download <FILE_ID> \
  --bootstrap <HOST>:<PORT> \
  --port <YOUR_PORT>
```

### Common Port Numbers
- First peer (seeder): 6881
- Second peer: 6882
- Third peer: 6883
- And so on...

### Important Directories
- **`.chunks/`**: Chunk storage (don't delete!)
- **`manifests/`**: Manifest files (JSON)
- **`downloads/`**: Your downloaded files
- **`logs/`**: Application logs

### Getting Help
```bash
java -jar target/peer.jar --help
```

---

Happy downloading! 🎉

Remember: **The more you seed, the better the network becomes for everyone!** 🌐
