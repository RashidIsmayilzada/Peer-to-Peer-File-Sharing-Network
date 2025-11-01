# Multi-Device P2P File Sharing Setup Guide

This guide explains how to test and use the P2P network across multiple devices.

---

## Prerequisites

### On All Devices:
1. **Java 17 or higher** installed
2. **Same network** (e.g., all devices on same WiFi)
3. **Firewall configured** to allow incoming connections
4. **Build the project** on each device (or copy the JAR file)

---

## Step 1: Find Your Device IP Address

### On macOS/Linux:
```bash
# Find your local IP
ifconfig | grep "inet " | grep -v 127.0.0.1
```

### On Windows:
```bash
ipconfig
```

Look for your local IP address (usually starts with `192.168.x.x` or `10.x.x.x`)

---

## Step 2: Configure Firewall

The P2P application needs to accept incoming TCP connections on the port you specify.

### On macOS:
```bash
# Allow incoming connections on port 8001 (example)
# System Preferences → Security & Privacy → Firewall → Firewall Options
# Or via command line:
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --add /usr/bin/java
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --unblockapp /usr/bin/java
```

### On Linux (Ubuntu):
```bash
# Allow port 8001
sudo ufw allow 8001/tcp
```

### On Windows:
```bash
# Windows Firewall → Advanced Settings → Inbound Rules → New Rule
# Allow TCP port 8001
```

---

## Step 3: Test Network Connectivity

Before running P2P, verify devices can reach each other:

### From Device B, ping Device A:
```bash
ping 192.168.1.100
```

### Test if port is open:
```bash
# On Device A, start a simple listener (for testing only):
nc -l 8001

# On Device B, try to connect:
nc 192.168.1.100 8001
```

If you can type messages and see them on both sides, network is working!

---

## Step 4: Running P2P Across Devices

### Scenario: Share a file from Device A to Device B

**Device A (Seeder)** - IP: `192.168.1.100`

1. Build the project (if not already built):
   ```bash
   mvn clean package -DskipTests
   ```

2. Seed a file:
   ```bash
   java -jar target/peer.jar --seed myfile.pdf --port 8001
   ```

3. **Copy the File ID** that's displayed:
   ```
   === FILE READY FOR SHARING ===
   File ID: abc123def456789...
   Filename: myfile.pdf
   Share this File ID with others to let them download!
   ```

4. **Share this File ID** with Device B (via email, chat, etc.)

**Device B (Downloader)** - IP: `192.168.1.101`

1. Make sure you have the JAR file (build it or copy from Device A)

2. Download the file using Device A's IP:
   ```bash
   java -jar target/peer.jar \
     --download abc123def456789... \
     --bootstrap 192.168.1.100:8001 \
     --port 8002
   ```

3. Check the `downloads/` directory for your file!

---

## Step 5: Multi-Peer Swarm (3+ Devices)

Once Device B downloads the file, it automatically becomes a seeder too!

**Device C** can now download from either Device A or Device B:

```bash
# Download from Device B instead of A
java -jar target/peer.jar \
  --download abc123def456789... \
  --bootstrap 192.168.1.101:8002 \
  --port 8003
```

This creates a true P2P swarm! 🌐

---

## Common Issues & Solutions

### Issue 1: "Connection refused"

**Possible causes:**
- Seeder is not running
- Wrong IP address
- Firewall blocking connection
- Wrong port number

**Solutions:**
1. Verify seeder is running: check terminal output
2. Double-check IP address: `ifconfig` or `ipconfig`
3. Temporarily disable firewall to test
4. Make sure port numbers match

### Issue 2: "Timeout waiting for manifest"

**Possible causes:**
- Network connection is slow or unstable
- Seeder crashed or stopped
- File ID is incorrect

**Solutions:**
1. Check seeder logs: look for errors
2. Verify File ID is correct (copy-paste carefully)
3. Try on same network (WiFi) for better speed
4. Increase timeout in code (if needed)

### Issue 3: "Connection works locally but not remotely"

**Possible causes:**
- Devices on different networks
- NAT/router blocking connections
- Firewall on router

**Solutions:**
1. Make sure all devices are on **same WiFi network**
2. For Internet sharing (different networks), you'll need **port forwarding** on router
3. Or use same LAN/WiFi

---

## Testing Over Internet (Advanced)

To share files over the Internet (not just local network):

### Option 1: Port Forwarding
1. Configure your router to forward port 8001 to your device's local IP
2. Share your **public IP** + port with others
3. They connect using: `--bootstrap YOUR_PUBLIC_IP:8001`

Find your public IP: https://whatismyipaddress.com/

### Option 2: VPN
Use a VPN like Tailscale or Hamachi to create a virtual LAN

### Option 3: Ngrok (Easy Testing)
```bash
# On seeder device
ngrok tcp 8001

# Share the ngrok URL (e.g., 0.tcp.ngrok.io:12345) with downloaders
```

---

## Quick Reference

### Device A (Seeder):
```bash
# Get your IP
ifconfig | grep "inet " | grep -v 127.0.0.1

# Seed file
java -jar target/peer.jar --seed file.txt --port 8001

# Note: Keep this terminal running!
```

### Device B (Downloader):
```bash
# Download file
java -jar target/peer.jar \
  --download <FILE_ID> \
  --bootstrap <DEVICE_A_IP>:8001 \
  --port 8002
```

### Common Ports:
- First peer: 8001
- Second peer: 8002
- Third peer: 8003
- And so on...

---

## Network Requirements

| Scenario | Requirements | Notes |
|----------|-------------|-------|
| **Same computer** | None | Use `localhost` |
| **Same WiFi** | Firewall rules | Most common use case |
| **Same LAN** | Firewall rules | Works in office/home network |
| **Different networks** | Port forwarding | Requires router configuration |
| **Internet** | Public IP + port forward | Or use VPN/Ngrok |

---

## Verification Checklist

Before testing across devices, verify:

- [ ] Java 17+ installed on both devices
- [ ] JAR file built or copied to both devices
- [ ] Both devices on same network
- [ ] IP addresses identified correctly
- [ ] Firewall allows Java or specific port
- [ ] Can ping between devices
- [ ] Seeder is running and displaying File ID
- [ ] File ID copied correctly (no typos)

---

## Example: Complete Multi-Device Session

### Device A (MacBook) - 192.168.1.50
```bash
$ java -jar target/peer.jar --seed vacation-photos.zip --port 8001
=== FILE READY FOR SHARING ===
File ID: f7c3a8e9d2b1a0c4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0
Filename: vacation-photos.zip
Share this File ID with others to let them download!
```

### Device B (Windows PC) - 192.168.1.75
```bash
> java -jar peer.jar --download f7c3a8e9d2b1a0c4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0 --bootstrap 192.168.1.50:8001 --port 8002

Downloading file to temporary location: downloads/f7c3a8e9.tmp
Connected to peer at 192.168.1.50:8001
Requesting manifest for file: f7c3a8e9...
Received manifest: vacation-photos.zip (156 chunks)
Requesting chunk 1/156
...
=== DOWNLOAD COMPLETE ===
File saved to: downloads/vacation-photos.zip
Original filename: vacation-photos.zip
File size: 40894720 bytes
You can now share this file with others!
```

### Device C (Linux) - 192.168.1.92
```bash
$ java -jar peer.jar --download f7c3a8e9d2b1a0c4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0 --bootstrap 192.168.1.75:8002 --port 8003

# Now downloading from Device B instead of Device A!
# This is the power of P2P! 🚀
```

---

## Security Considerations

⚠️ **Important Security Notes:**

1. **No encryption yet**: Files are transferred in plain text
2. **No authentication**: Anyone with File ID can download
3. **Trust the network**: Only use on trusted networks
4. **Firewall protection**: Keep firewall enabled

These will be addressed in future milestones (Milestone F: NAT Traversal & Security).

---

## Next Steps

After successful multi-device testing:
- Continue to Milestone D: Peer Discovery (DHT)
- Implement Milestone E: Swarm & Scheduling (parallel chunk downloads)
- Add Milestone F: Security & Encryption

Happy P2P sharing! 🌐✨
