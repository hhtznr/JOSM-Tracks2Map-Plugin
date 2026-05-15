package hhtznr.josm.plugins.tracks2map.data;

import java.time.Instant;

import org.openstreetmap.josm.data.Bounds;

/**
 * Helper class for remembering modification time and overall bounds of GPX
 * files.
 *
 * @author Harald Hetzner
 */
public class TrackInfoCacheEntry {

    private String gpxFilePath;
    private Instant lastModified;
    private Bounds bounds;

    /**
     * Creates a new track info cache entry.
     *
     * @param gpxFilePath  The path of the GPX file.
     * @param lastModified The modification time of the GPX file.
     * @param bounds       The bounds of the GPX track.
     */
    public TrackInfoCacheEntry(String gpxFilePath, Instant lastModified, Bounds bounds) {
        this.gpxFilePath = gpxFilePath;
        this.lastModified = lastModified;
        this.bounds = bounds;
    }

    /**
     * Returns the path of the GPX file where the track is stored.
     *
     * @return The path of the GPX file.
     */
    public String getGPXFilePath() {
        return gpxFilePath;
    }

    /**
     * Sets the path of the GPX file where the track is stored.
     *
     * @param gpxFilePath The path of the GPX file.
     */
    public void setGPXFilePath(String gpxFilePath) {
        this.gpxFilePath = gpxFilePath;
    }

    /**
     * Returns the cached modification time of the GPX file.
     *
     * @return The modification time.
     */
    public Instant getLastModified() {
        return lastModified;
    }

    /**
     * Sets the cached modification time of the GPX file.
     *
     * @param lastModified The modification time.
     */
    public void setLastModified(Instant lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * Returns the bounds of the GPX track.
     *
     * @return The bounds.
     */
    public Bounds getBounds() {
        return bounds;
    }

    /**
     * Sets the bounds of the GPX track.
     *
     * @param bounds The bounds.
     */
    public void setBounds(Bounds bounds) {
        this.bounds = bounds;
    }
}
