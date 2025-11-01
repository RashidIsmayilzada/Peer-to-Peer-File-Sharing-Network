#!/bin/bash

echo "=== P2P Peer Discovery Test ==="
echo

# Create test files
echo "Step 1: Creating test files..."
echo "File A content - shared by Peer 1" > fileA.txt
echo "File B content - shared by Peer 2" > fileB.txt
echo

# Start Peer 1 (seeds fileA.txt)
echo "Step 2: Starting Peer 1 (seeding fileA.txt)..."
java -jar target/peer.jar --seed fileA.txt --port 8001 > /tmp/peer1.log 2>&1 &
PEER1_PID=$!
sleep 3

# Extract File ID for fileA
FILE_A_ID=$(grep "File ID:" /tmp/peer1.log | tail -1 | awk '{print $NF}')
echo "Peer 1 File ID (fileA): $FILE_A_ID"
echo "Peer 1 PID: $PEER1_PID"
echo

# Start Peer 2 (seeds fileB.txt, connects to Peer 1)
echo "Step 3: Starting Peer 2 (seeding fileB.txt, connecting to Peer 1)..."
java -jar target/peer.jar --seed fileB.txt --port 8002 > /tmp/peer2.log 2>&1 &
PEER2_PID=$!
sleep 3

# Extract File ID for fileB
FILE_B_ID=$(grep "File ID:" /tmp/peer2.log | tail -1 | awk '{print $NF}')
echo "Peer 2 File ID (fileB): $FILE_B_ID"
echo "Peer 2 PID: $PEER2_PID"
echo

# Start Peer 3 (downloads fileA from Peer 1, connects via bootstrap)
echo "Step 4: Starting Peer 3 (downloading fileA via Peer 1)..."
java -jar target/peer.jar --download $FILE_A_ID --bootstrap localhost:8001 --port 8003 > /tmp/peer3.log 2>&1 &
PEER3_PID=$!
sleep 6

echo "Step 5: Checking results..."
echo

# Check if Peer 3 downloaded fileA
if [ -f "downloads/fileA.txt" ]; then
  echo "✅ Peer 3 successfully downloaded fileA.txt"

  # Compare contents
  if diff -q fileA.txt downloads/fileA.txt > /dev/null 2>&1; then
    echo "✅ File content matches!"
  else
    echo "❌ File content mismatch!"
  fi
else
  echo "❌ Peer 3 failed to download fileA.txt"
fi
echo

# Check peer discovery in logs
echo "Step 6: Checking peer discovery..."
echo

echo "Peer 1 log (checking for peer registration):"
grep -i "registered peer\|new peer discovered" /tmp/peer1.log | head -5 || echo "No peer discovery messages found"
echo

echo "Peer 2 log (checking for peer registration):"
grep -i "registered peer\|new peer discovered" /tmp/peer2.log | head -5 || echo "No peer discovery messages found"
echo

echo "Peer 3 log (checking for peer registration):"
grep -i "registered peer\|new peer discovered" /tmp/peer3.log | head -5 || echo "No peer discovery messages found"
echo

# Cleanup
echo "Cleaning up..."
kill $PEER1_PID $PEER2_PID $PEER3_PID 2>/dev/null
sleep 1
rm -f fileA.txt fileB.txt
rm -rf downloads/ .chunks/ manifests/

echo "Test complete!"
echo
echo "💡 To see full logs:"
echo "   tail -50 /tmp/peer1.log"
echo "   tail -50 /tmp/peer2.log"
echo "   tail -50 /tmp/peer3.log"
