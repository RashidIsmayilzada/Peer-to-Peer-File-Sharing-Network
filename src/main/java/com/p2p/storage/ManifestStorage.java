package com.p2p.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.p2p.core.Manifest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

// Manages storage and retrieval of file manifests
public class ManifestStorage {
    private static final Logger logger = LoggerFactory.getLogger(ManifestStorage.class);

    private final Path storageRoot;
    private final ObjectMapper objectMapper;
    private final Map<String, Manifest> manifestCache;

    // Creates a ManifestStorage with default directory "manifests"
    public ManifestStorage() {
        this(Paths.get("manifests"));
    }

    // Creates a ManifestStorage with custom directory
    public ManifestStorage(Path storageRoot) {
        this.storageRoot = storageRoot;
        this.objectMapper = new ObjectMapper();
        this.manifestCache = new HashMap<>();
        ensureStorageExists();
    }

    // Stores a manifest to disk as JSON with filename = fileId.json
    public void storeManifest(Manifest manifest) throws IOException {
        String fileId = manifest.getFileId();
        Path manifestPath = getManifestPath(fileId);

        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(manifestPath.toFile(), manifest);

        manifestCache.put(fileId, manifest);

        logger.info("Stored manifest: {} ({})", fileId.substring(0, 8) + "...", manifest.getFilename());
    }

    // Retrieves a manifest by file ID
    public Manifest retrieveManifest(String fileId) throws IOException {
        if (manifestCache.containsKey(fileId)) {
            return manifestCache.get(fileId);
        }

        Path manifestPath = getManifestPath(fileId);
        if (!Files.exists(manifestPath)) {
            logger.warn("Manifest not found: {}", fileId);
            return null;
        }

        Manifest manifest = objectMapper.readValue(manifestPath.toFile(), Manifest.class);

        manifestCache.put(fileId, manifest);

        logger.debug("Retrieved manifest: {} ({})", fileId.substring(0, 8) + "...", manifest.getFilename());
        return manifest;
    }

    // Checks if a manifest exists for the given file ID
    public boolean hasManifest(String fileId) {
        if (manifestCache.containsKey(fileId)) {
            return true;
        }
        return Files.exists(getManifestPath(fileId));
    }

    // Deletes a manifest
    public void deleteManifest(String fileId) throws IOException {
        Path manifestPath = getManifestPath(fileId);
        if (Files.exists(manifestPath)) {
            Files.delete(manifestPath);
            manifestCache.remove(fileId);
            logger.debug("Deleted manifest: {}", fileId.substring(0, 8) + "...");
        }
    }

    // Returns all file IDs that have stored manifests
    public Map<String, Manifest> getAllManifests() throws IOException {
        Map<String, Manifest> manifests = new HashMap<>();

        manifests.putAll(manifestCache);

        if (Files.exists(storageRoot)) {
            Files.list(storageRoot)
                    .filter(path -> path.toString().endsWith(".json"))
                    .forEach(path -> {
                        try {
                            String filename = path.getFileName().toString();
                            String fileId = filename.substring(0, filename.length() - 5);

                            if (!manifests.containsKey(fileId)) {
                                Manifest manifest = objectMapper.readValue(path.toFile(), Manifest.class);
                                manifests.put(fileId, manifest);
                                manifestCache.put(fileId, manifest);
                            }
                        } catch (IOException e) {
                            logger.error("Failed to read manifest: {}", path, e);
                        }
                    });
        }

        return manifests;
    }

    // Gets the file path for a manifest with the given file ID
    private Path getManifestPath(String fileId) {
        return storageRoot.resolve(fileId + ".json");
    }

    // Ensures the storage root directory exists
    private void ensureStorageExists() {
        try {
            Files.createDirectories(storageRoot);
            logger.debug("Manifest storage root: {}", storageRoot.toAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to create manifest storage directory", e);
            throw new RuntimeException("Failed to initialize manifest storage", e);
        }
    }

    // Returns the storage root path
    public Path getStorageRoot() {
        return storageRoot;
    }
}
