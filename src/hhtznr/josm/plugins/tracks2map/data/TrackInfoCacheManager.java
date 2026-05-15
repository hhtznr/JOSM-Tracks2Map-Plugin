package hhtznr.josm.plugins.tracks2map.data;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.stream.JsonGenerator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.Logging;
import org.xml.sax.SAXException;

import hhtznr.josm.plugins.tracks2map.utils.GPXUtils;

/**
 * This class implements a cache manager for caching the bounds of previously
 * processed GPX tracks as well as saving them to disk and loading them from
 * disk in order to preserve the information across JSON sessions.
 *
 * @author Harald Hetzner
 */
public class TrackInfoCacheManager {

    private final Path cacheFilePath;
    private HashMap<String, TrackInfoCacheEntry> cacheEntries;
    private Path gpxDirectory = null;
    private boolean recursive = true;
    private boolean cacheUpdated = false;
    private boolean syncCanceled = false;

    /**
     * Creates a new track cache manager.
     *
     * @param cacheFilePath The path of the file where the cache is saved to
     *                      preserve it across JSON sessions.
     */
    public TrackInfoCacheManager(Path cacheFilePath) {
        this.cacheFilePath = cacheFilePath;
        this.cacheEntries = new HashMap<>();
    }

    /**
     * Returns the path of the cache file.
     *
     * @return The path of the cache file.
     */
    public Path getCacheFilePath() {
        return cacheFilePath;
    }

    /**
     * Returns a hash map with the cached GPX track bounds.
     * <tr>
     * Note: {@link #loadCache()} has to be executed in order to load the cached
     * data from the cache file.
     *
     * @return A hash map with the cached GPX track bounds with the absolute paths
     *         of the GPX files being the keys.
     */
    public HashMap<String, TrackInfoCacheEntry> getCacheEntries() {
        return cacheEntries;
    }

    /**
     * Sets the hash map with the cached GPX track bounds.
     *
     * @param entries A hash map with the cache GPX track bounds with the absolute
     *                paths of the GPX files being the keys.
     */
    public void setCacheEntries(HashMap<String, TrackInfoCacheEntry> entries) {
        this.cacheEntries = entries;
    }

    /**
     * Loads the list of GPX track data from the cache file.
     *
     * @throws IOException If the file reader cannot be created.
     */
    public void loadCache() throws IOException {
        cacheEntries = new HashMap<>();
        if (!cacheFilePath.toFile().exists()) {
            Logging.info("Tracks2Map: Cache file '" + cacheFilePath.toString() + "' does not exist.");
            return;
        }

        // FileReader may throw FileNotFoundException
        try (JsonReader reader = Json.createReader(Files.newBufferedReader(cacheFilePath, StandardCharsets.UTF_8))) {
            JsonObject rootObj = reader.readObject();
            JsonArray tracksArray = rootObj.getJsonArray("tracks");
            for (int i = 0; i < tracksArray.size(); i++) {
                JsonObject entryObj = tracksArray.getJsonObject(i);
                String filePath = entryObj.getString("filePath");
                JsonObject boundsObj = entryObj.getJsonObject("bounds");
                JsonObject neObj = boundsObj.getJsonObject("northEast");
                JsonObject swObj = boundsObj.getJsonObject("southWest");

                LatLon northEast = new LatLon(neObj.getJsonNumber("lat").doubleValue(),
                        neObj.getJsonNumber("lon").doubleValue());
                LatLon southWest = new LatLon(swObj.getJsonNumber("lat").doubleValue(),
                        swObj.getJsonNumber("lon").doubleValue());
                Bounds bounds = new Bounds(southWest, northEast);

                Instant lastModified = Instant.parse(entryObj.getString("lastModified"));

                TrackInfoCacheEntry entry = new TrackInfoCacheEntry(filePath, lastModified, bounds);
                cacheEntries.put(filePath, entry);
            }
        }
        cacheUpdated = false;

        Logging.info("Tracks2Map: Loaded cached bounds of " + cacheEntries.size() + " GPX tracks from '"
                + cacheFilePath.toString() + "'");
    }

    /**
     * Removes the track info for a GPX file, specified by its absolute path, from
     * the cache.
     *
     * @param gpxFilePath The absolute path of the GPX file.
     * @return {@code true} if a track info entry was cached for the provided GPX
     *         file path and removed.
     */
    public TrackInfoCacheEntry removeFromCache(String gpxFilePath) {
        TrackInfoCacheEntry removedEntry = cacheEntries.remove(gpxFilePath);
        if (removedEntry != null)
            cacheUpdated = true;
        return removedEntry;
    }

    /**
     * Returns the directory in which the GPX files, for which track info is cached,
     * are stored on disk.
     *
     * @return The path of the GPX directory.
     */
    public Path getGPXDirectory() {
        return gpxDirectory;
    }

    /**
     * Returns whether the recursive scanning of the GPX directory for GPX files is
     * enabled.
     *
     * @return {@code true} if recursive scanning for GPX files is enabled.
     */
    public boolean isGPXDirectoryScanRecursive() {
        return recursive;
    }

    /**
     * Sets the directory from which to read tracks from GPX files.
     *
     * @param gpxDirectory The directory with the GPX files from which to read the
     *                     tracks.
     * @param recursive    If {@code true}, traverse subdirectories recursively.
     */
    public void setGPXDirectory(Path gpxDirectory, boolean recursive) {
        this.gpxDirectory = gpxDirectory;
        this.recursive = recursive;
    }

    /**
     * Synchronized the cache with the track info from the GPX files in the GPX
     * directory.
     *
     * @param progressMonitor A progress monitor dialog to inform the user about the
     *                        progress.
     * @throws IOException If an I/O exception occurs upon finding GPX files in the
     *                     GPX directory.
     */
    public void syncCacheFromDisk(ProgressMonitor progressMonitor) throws IOException {
        syncCanceled = false;

        if (gpxDirectory == null) {
            Logging.error(
                    "Tracks2Map: You need to define the path of the directory with the GPX tracks in the preferences first");
            return;
        }
        if (!Files.exists(gpxDirectory)) {
            Logging.error(
                    "Tracks2Map: GPX directory path '" + gpxDirectory.toAbsolutePath().toString() + "' does not exist");
            return;
        }
        if (!Files.isDirectory(gpxDirectory)) {
            Logging.error("Tracks2Map: GPX directory path '" + gpxDirectory.toAbsolutePath().toString()
                    + "' is not a directory");
            return;
        }
        if (!Files.isReadable(gpxDirectory)) {
            Logging.error("Tracks2Map: Permission for reading from GPX directory path '"
                    + gpxDirectory.toAbsolutePath().toString() + "' is denied");
            return;
        }

        Set<String> filesOnDisk = new HashSet<>();

        // Set maximum depth: 1 means only the directory itself, MAX_VALUE means
        // infinite recursion
        int maxDepth = recursive ? Integer.MAX_VALUE : 1;

        List<Path> paths;
        try (Stream<Path> stream = Files.find(gpxDirectory, maxDepth, (path, attrs) -> attrs.isRegularFile())) {
            paths = stream.collect(Collectors.toList());
        }

        if (paths.isEmpty()) {
            Logging.info("Tracks2Map: No GPX files found in GPX directory '" + gpxDirectory.toAbsolutePath().toString()
                    + "'");
            if (!cacheEntries.isEmpty()) {
                Logging.info("Tracks2Map: Clearing in-memory cache with GPX file info");
                cacheEntries.clear();
                cacheUpdated = true;
            }
            return;
        }
        progressMonitor.setTicksCount(paths.size());

        int numberOfProcessedPaths = 0;
        // Discover new files and update modified files
        for (Path path : paths) {
            if (syncCanceled)
                return;
            String absolutePath = path.toAbsolutePath().toString();
            filesOnDisk.add(absolutePath);
            progressMonitor.setCustomText(absolutePath);

            try {
                Instant currentLastModified = Instant.ofEpochMilli(Files.getLastModifiedTime(path).toMillis());

                TrackInfoCacheEntry cachedTrackInfo = cacheEntries.get(absolutePath);

                // If the track is not known to the cache or the version on disk is newer
                if (cachedTrackInfo == null || currentLastModified.isAfter(cachedTrackInfo.getLastModified())) {
                    // Found a brand-new file on disk
                    // Read the GPX data from the file
                    GpxData gpxData = GPXUtils.readGPXFile(path);
                    Bounds trackBounds;
                    try {
                        trackBounds = GPXUtils.getTrackBounds(gpxData);
                    } catch (IOException e) {
                        continue;
                    }
                    TrackInfoCacheEntry newEntry = new TrackInfoCacheEntry(absolutePath, currentLastModified,
                            trackBounds);
                    cacheEntries.put(absolutePath, newEntry);
                    cacheUpdated = true;
                }
            } catch (IOException | SAXException e) {
                Logging.error("Tracks2Map: Failed to process GPX file: " + absolutePath + " -> " + e.getMessage());
            }
            numberOfProcessedPaths++;
            // Increase progress by one tick
            progressMonitor.worked(1);
            progressMonitor.setExtraText(" (" + numberOfProcessedPaths + " of " + paths.size() + ")");
        }
        Logging.info("Tracks2Map: Processed " + numberOfProcessedPaths + " GPX files for cache synchronization");

        if (syncCanceled)
            return;

        // Identify and remove dead entries no longer present in the scanned scope
        // returns true if the set changed as a result of the call
        if (cacheEntries.keySet().retainAll(filesOnDisk)) {
            cacheUpdated = true;
        }
    }

    /**
     * Cancels synchronization of the cache from disk if it is running.
     */
    public void cancelSync() {
        syncCanceled = true;
    }

    /**
     * Saves the list of GPX track data to the cache file.
     *
     * @throws IOException If the cache file cannot be written.
     */
    public void saveCache() throws IOException {
        if (!cacheUpdated && cacheFilePath.toFile().exists()) {
            Logging.info("Tracks2Map: No changes to cached bounds of GPX tracks; therfore not updating '"
                    + cacheFilePath.toString() + "'");
            return;
        }

        Logging.info("Tracks2Map: Saving cached bounds of GPX tracks to '" + cacheFilePath.toString() + "'");

        try (JsonGenerator gen = Json.createGenerator(Files.newBufferedWriter(cacheFilePath, StandardCharsets.UTF_8))) {
            gen.writeStartObject(); // {
            gen.writeStartArray("tracks"); // "tracks": [

            for (TrackInfoCacheEntry entry : cacheEntries.values()) {
                Bounds bounds = entry.getBounds();
                LatLon northEast = bounds.getMax();
                LatLon southWest = bounds.getMin();

                gen.writeStartObject(); // {
                gen.write("filePath", entry.getGPXFilePath());
                gen.write("lastModified", entry.getLastModified().toString());

                gen.writeStartObject("bounds"); // "bounds": {

                gen.writeStartObject("northEast"); // "northEast": {
                gen.write("lat", northEast.lat());
                gen.write("lon", northEast.lon());
                gen.writeEnd(); // }

                gen.writeStartObject("southWest"); // "southWest": {
                gen.write("lat", southWest.lat());
                gen.write("lon", southWest.lon());
                gen.writeEnd(); // }

                gen.writeEnd(); // } bounds
                gen.writeEnd(); // } entry
            }

            gen.writeEnd(); // ] tracks array
            gen.writeEnd(); // } root object
        }

        Logging.info("Tracks2Map: Saved cached bounds of " + cacheEntries.size() + " GPX tracks to '"
                + cacheFilePath.toString() + "'");
    }
}
