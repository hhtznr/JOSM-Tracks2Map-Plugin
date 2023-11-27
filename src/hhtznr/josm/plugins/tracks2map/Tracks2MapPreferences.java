package hhtznr.josm.plugins.tracks2map;

/**
 * Property keys and default values for the {@code Tracks2MapPlugin}.
 *
 * @author Harald Hetzner
 */
public class Tracks2MapPreferences {

    private Tracks2MapPreferences() {
    }

    /**
     * Property key for the path of the directory containing GPX files with the
     * tracks to be opened.
     */
    public static final String GPX_DIRECTORY = "tracks2map.gpx-directory.path";

    /**
     * Property key for defining if subdirectories of the GPX directory should be
     * traversed recursively.
     */
    public static final String RECURSIVE = "tracks2map.gpx-directory.recursive";

    /**
     * Default property value of the GPX directory path: {@code null}.
     */
    public static final String DEFAULT_GPX_DIRECTORY = null;

    /**
     * Default property value for recursive traversal of the GPX directory:
     * {@code true}.
     */
    public static final boolean DEFAULT_RECURSIVE = true;

}
