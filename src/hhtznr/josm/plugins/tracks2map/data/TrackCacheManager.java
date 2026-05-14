package hhtznr.josm.plugins.tracks2map.data;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.stream.JsonGenerator;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.tools.Logging;

/**
 * This class implements a cache manager for caching the bounds of previously
 * processed GPX tracks as well as saving them to disk and loading them from
 * disk in order to preserve the information across JSON sessions.
 *
 * @author Harald Hetzner
 */
public class TrackCacheManager {

    private final Path cacheFilePath;
    private HashMap<String, TrackCacheEntry> entries;

    /**
     * Creates a new track cache manager.
     *
     * @param cacheFilePath The path of the file where the cache is saved to
     *                      preserve it across JSON sessions.
     */
    public TrackCacheManager(Path cacheFilePath) {
        this.cacheFilePath = cacheFilePath;
        this.entries = new HashMap<>();
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
    public HashMap<String, TrackCacheEntry> getCacheEntries() {
        return entries;
    }

    /**
     * Sets the hash map with the cached GPX track bounds.
     *
     * @param entries A hash map with the cache GPX track bounds with the absolute
     *                paths of the GPX files being the keys.
     */
    public void setCacheEntries(HashMap<String, TrackCacheEntry> entries) {
        this.entries = entries;
    }

    /**
     * Loads the list of GPX track data from the cache file.
     *
     * @throws FileNotFoundException If the cache file does not exist.
     * @throws IOException           If the file reader cannot be created.
     */
    public void loadCache() throws FileNotFoundException, IOException {
        entries = new HashMap<>();
        if (!cacheFilePath.toFile().exists())
            throw new FileNotFoundException("Cache file '" + cacheFilePath.toString() + "' does not exist.");

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

                TrackCacheEntry entry = new TrackCacheEntry(filePath, lastModified, bounds);
                entries.put(filePath, entry);
            }
        }

        Logging.info("Tracks2Map: Loaded cached bounds of " + entries.size() + " GPX tracks from '"
                + cacheFilePath.toString() + "'");
    }

    /**
     * Saves the list of GPX track data to the cache file.
     *
     * @throws IOException If the cache file cannot be written.
     */
    public void saveCache() throws IOException {
        Logging.info("Tracks2Map: Saving cached bounds of GPX tracks to '" + cacheFilePath.toString() + "'");

        try (JsonGenerator gen = Json.createGenerator(Files.newBufferedWriter(cacheFilePath, StandardCharsets.UTF_8))) {
            gen.writeStartObject(); // {
            gen.writeStartArray("tracks"); // "tracks": [

            for (TrackCacheEntry entry : entries.values()) {
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

        Logging.info("Tracks2Map: Saved cached bounds of " + entries.size() + " GPX tracks to '"
                + cacheFilePath.toString() + "'");
    }
}
