#!/bin/bash

# Helper script to get your local IP address for P2P networking

echo "=== Your Network Information ==="
echo

# Detect OS
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    echo "Platform: macOS"
    echo
    echo "Local IP addresses:"
    ifconfig | grep "inet " | grep -v 127.0.0.1 | awk '{print "  " $2}'

elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    # Linux
    echo "Platform: Linux"
    echo
    echo "Local IP addresses:"
    ip addr show | grep "inet " | grep -v 127.0.0.1 | awk '{print "  " $2}' | cut -d'/' -f1

else
    # Unknown/Windows
    echo "Platform: Unknown (try 'ipconfig' on Windows)"
    echo
fi

echo
echo "=== How to Use ==="
echo
echo "1. On this device (Seeder), run:"
echo "   java -jar target/peer.jar --seed <file> --port 8001"
echo
echo "2. On another device (Downloader), run:"
echo "   java -jar target/peer.jar --download <FILE_ID> --bootstrap <YOUR_IP>:8001 --port 8002"
echo
echo "Replace <YOUR_IP> with one of the IP addresses listed above"
echo "Replace <FILE_ID> with the File ID shown when seeding"
echo
