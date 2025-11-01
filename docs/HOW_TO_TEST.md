# How to Test P2P File Sharing

This guide will walk you through testing the complete peer-to-peer file transfer system.

---

## Prerequisites

Make sure you have:
- ✅ Java 17 or later installed
- ✅ Built the project: `mvn clean package`
- ✅ The `peer.jar` file in `target/` directory

---

## Test 1: Simple File Transfer (2 Peers)

### Step 1: Create a Test File

```bash
# Create a test file with some content
echo "Hello from P2P Network! This file will be transferred between peers." > test.txt

# Check the file
cat test.txt
ls -lh test.txt
```

### Step 2: Start Peer 1 (Seeder)

Open **Terminal 1** and run:

```bash
java -jar target/peer.jar --seed test.txt --port 6881
```

**What to expect:**
```
=== FILE READY FOR SHARING ===
File ID: abc123def456...
Filename: test.txt
Share this File ID with others to let them download!
================================

Peer server started on port 6881 with ID: ...
```

**Important:** Copy the **File ID** - you'll need it for downloading!

### Step 3: Start Peer 2 (Downloader)

Open **Terminal 2** and run (replace `<FILE_ID>` with the actual File ID from Step 2):

```bash
java -jar target/peer.jar --download 060c184c2214f57d0ff805d76170768b984128797299a2b1b0e8b24b06882f84 --bootstrap localhost:6881 --port 6882
```

**What to expect:**
```
Connected to peer at localhost:6881
Requesting manifest for file: abc123def456...
Received manifest: test.txt (1 chunks)
Requesting chunk 1/1
Received chunk 1/1 (67 bytes)
Assembling file...

=== DOWNLOAD COMPLETE ===
File saved to: downloads/abc123de.download
File size: 67 bytes
You can now share this file with others!
=========================
```

### Step 4: Verify the Download

In **Terminal 3** (or press Ctrl+C in Terminal 2 first):

```bash
# Check the downloaded file
ls -lh downloads/
cat downloads/*.download

# Compare with original
diff test.txt downloads/*.download
```

If `diff` outputs nothing, the files are identical! ✅

---

## Test 2: Large File Transfer

Let's test with a larger file that will span multiple chunks:

### Step 1: Create a Larger Test File (1 MB)

```bash
# Create a 1 MB file
dd if=/dev/urandom of=largefile.bin bs=1024 count=1024

# Check the size
ls -lh largefile.bin
```

### Step 2: Seed the Large File

**Terminal 1:**
```bash
java -jar target/peer.jar --seed largefile.bin --port 6881
```

Copy the File ID!

### Step 3: Download from Another Peer

**Terminal 2:**
```bash
java -jar target/peer.jar --download <FILE_ID> --bootstrap localhost:6881 --port 6882
```

**You should see:**
```
Requesting chunk 1/4
Received chunk 1/4 (262144 bytes)
Requesting chunk 2/4
Received chunk 2/4 (262144 bytes)
Requesting chunk 3/4
Received chunk 3/4 (262144 bytes)
Requesting chunk 4/4
Received chunk 4/4 (262144 bytes)
```

### Step 4: Verify Integrity

```bash
# Compare checksums
shasum -a 256 largefile.bin
shasum -a 256 downloads/*.download

# The hashes should match!
```

---

## Test 3: Multi-Peer Network (3 Peers)

This demonstrates the true power of P2P: Peer 3 can download from Peer 2, who downloaded from Peer 1!

### Setup:

**Terminal 1 - Peer 1 (Original Seeder):**
```bash
echo "Original content from Peer 1" > original.txt
java -jar target/peer.jar --seed original.txt --port 6881
```
Copy the File ID!

**Terminal 2 - Peer 2 (Downloads from Peer 1, becomes seeder):**
```bash
java -jar target/peer.jar --download <FILE_ID> --bootstrap localhost:6881 --port 6882
```
Wait for download to complete. Peer 2 is now also seeding!

**Terminal 3 - Peer 3 (Downloads from Peer 2!):**
```bash
java -jar target/peer.jar --download <FILE_ID> --bootstrap localhost:6882 --port 6883
```

**Result:** Peer 3 successfully downloads from Peer 2, proving the P2P network works! 🎉

---

## Test 4: Verify Storage Structure

After running tests, check what was created:

```bash
# View chunk storage
ls -la .chunks/

# View manifests
ls -la manifests/

# View downloaded files
ls -la downloads/

# Check a manifest file
cat manifests/*.json | head -20
```

**You should see:**
- `.chunks/` with subdirectories (2-char hash prefixes)
- `manifests/` with JSON files (named by file ID)
- `downloads/` with downloaded files

---

## Test 5: Chunk Integrity Verification

Let's verify that chunks are stored correctly:

```bash
# List a chunk file
CHUNK_FILE=$(find .chunks -type f | head -1)
echo "Checking: $CHUNK_FILE"

# Calculate its hash
HASH=$(basename $CHUNK_FILE)
ACTUAL=$(shasum -a 256 $CHUNK_FILE | awk '{print $1}')

echo "Expected: $HASH"
echo "Actual:   $ACTUAL"

# They should match!
if [ "$HASH" = "$ACTUAL" ]; then
  echo "✅ Chunk integrity verified!"
else
  echo "❌ Chunk corrupted!"
fi
```

---

## Quick Test Script

Here's a complete automated test:

```bash
#!/bin/bash

echo "=== P2P File Transfer Test ==="

# Cleanup
rm -rf .chunks manifests downloads test-*.txt

# Create test file
echo "Test content for P2P transfer" > test-original.txt
echo "Created test file"

# Start seeder in background
java -jar target/peer.jar --seed test-original.txt --port 7001 > /tmp/peer1.log 2>&1 &
PEER1_PID=$!
sleep 3

# Extract File ID from log
FILE_ID=$(grep "File ID:" /tmp/peer1.log | awk '{print $NF}')
echo "File ID: $FILE_ID"

# Start downloader
java -jar target/peer.jar --download $FILE_ID --bootstrap localhost:7001 --port 7002 > /tmp/peer2.log 2>&1 &
PEER2_PID=$!

# Wait for download
sleep 5

# Kill both peers
kill $PEER1_PID $PEER2_PID 2>/dev/null
wait $PEER1_PID $PEER2_PID 2>/dev/null

# Verify
DOWNLOADED=$(ls downloads/*.download 2>/dev/null | head -1)
if [ -f "$DOWNLOADED" ]; then
  echo "✅ Download successful!"
  echo "Original:"
  cat test-original.txt
  echo -e "\nDownloaded:"
  cat $DOWNLOADED

  if diff -q test-original.txt $DOWNLOADED > /dev/null; then
    echo -e "\n✅ FILES MATCH - TEST PASSED!"
  else
    echo -e "\n❌ FILES DON'T MATCH - TEST FAILED!"
  fi
else
  echo "❌ Download failed!"
fi

# Cleanup
kill $PEER1_PID $PEER2_PID 2>/dev/null
```

Save as `test.sh`, make executable with `chmod +x test.sh`, and run with `./test.sh`

---

## Troubleshooting

### Issue: "Port already in use"
**Solution:** Change the port number or kill existing processes:
```bash
lsof -ti:6881 | xargs kill
```

### Issue: "File not found"
**Solution:** Make sure you're in the project directory where the file exists.

### Issue: "Timeout waiting for manifest"
**Solution:**
1. Make sure the seeder is still running
2. Check the File ID is correct
3. Verify the bootstrap address is correct

### Issue: "Connection refused"
**Solution:** Make sure the seeder started successfully and is listening on the specified port.

---

## What to Look For (Success Indicators)

✅ **Seeder:**
- File chunked into N pieces
- Manifest generated and stored
- Server started and listening
- File ID displayed

✅ **Downloader:**
- Connected to peer successfully
- HELLO handshake completed
- Manifest received
- All chunks downloaded
- File assembled
- File saved to downloads/

✅ **Verification:**
- `diff` shows no differences
- SHA-256 hashes match
- File sizes are identical
- Downloaded file is readable

---

## Next Steps

Once basic transfer works, you can test:
- Multiple simultaneous downloads
- Very large files (100+ MB)
- Network interruptions (kill and restart)
- Different file types (text, binary, images)

Enjoy testing your P2P network! 🚀
