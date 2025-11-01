#!/bin/bash
set -e

echo "=== P2P Complete End-to-End Test ==="
echo

# Create test file
echo "Step 1: Creating test file..."
echo "This is a test file for P2P transfer. It contains some data to verify integrity." > mytest.txt
cat mytest.txt
echo

# Start seeder
echo "Step 2: Starting Peer 1 (Seeder)..."
java -jar target/peer.jar --seed mytest.txt --port 8001 > /tmp/peer1.log 2>&1 &
PEER1_PID=$!
sleep 4

# Extract File ID
FILE_ID=$(grep "File ID:" /tmp/peer1.log | head -1 | awk '{print $NF}')
echo "File ID: $FILE_ID"
echo "Peer 1 PID: $PEER1_PID"
echo

# Start downloader  
echo "Step 3: Starting Peer 2 (Downloader)..."
java -jar target/peer.jar --download $FILE_ID --bootstrap localhost:8001 --port 8002 > /tmp/peer2.log 2>&1 &
PEER2_PID=$!
sleep 6

# Check results
echo "Step 4: Checking results..."
echo

# Check for downloaded file with original name
if [ -f "downloads/mytest.txt" ]; then
  DOWNLOADED="downloads/mytest.txt"
  echo "✅ File downloaded with original name: $DOWNLOADED"
  echo
  echo "Original content:"
  cat mytest.txt
  echo
  echo "Downloaded content:"
  cat $DOWNLOADED
  echo

  if diff -q mytest.txt $DOWNLOADED > /dev/null 2>&1; then
    echo "✅ SUCCESS! Files are identical!"
    echo "✅ BONUS: Original filename preserved!"
  else
    echo "❌ FAILED! Files differ!"
  fi
else
  echo "❌ Download failed - no file found with original name"
  echo
  echo "Files in downloads/:"
  ls -la downloads/ 2>/dev/null || echo "No downloads directory"
  echo
  echo "Peer 1 log:"
  tail -20 /tmp/peer1.log
  echo
  echo "Peer 2 log:"
  tail -20 /tmp/peer2.log
fi

# Cleanup
echo
echo "Cleaning up..."
kill $PEER1_PID $PEER2_PID 2>/dev/null || true
wait $PEER1_PID $PEER2_PID 2>/dev/null || true

echo "Test complete!"
