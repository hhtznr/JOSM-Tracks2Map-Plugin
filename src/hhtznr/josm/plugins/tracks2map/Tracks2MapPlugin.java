package hhtznr.josm.plugins.tracks2map;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.spi.preferences.IPreferences;

import hhtznr.josm.plugins.tracks2map.data.TrackInfoCacheManager;
import hhtznr.josm.plugins.tracks2map.gui.Tracks2MapDeleteAllGPXLayersAction;
import hhtznr.josm.plugins.tracks2map.gui.Tracks2MapOpenAction;
import hhtznr.josm.plugins.tracks2map.gui.Tracks2MapSyncCacheFromDiskAction;
import hhtznr.josm.plugins.tracks2map.gui.Tracks2MapTabPreferenceSetting;

/**
 * Plugin class for opening all GPX tracks in the current map view.
 *
 * @author Harald Hetzner
 */
public class Tracks2MapPlugin extends Plugin {

    private final Tracks2MapOpenAction openAction;
    private final Tracks2MapSyncCacheFromDiskAction syncAction;
    private final Tracks2MapDeleteAllGPXLayersAction deleteAllLayersAction;

    private final TrackInfoCacheManager cacheManager;

    private Tracks2MapTabPreferenceSetting preferenceSetting = null;

    /**
     * Initializes the plugin.
     *
     * @param info context information about the plugin.
     */
    public Tracks2MapPlugin(PluginInformation info) {
        super(info);

        File cacheFile = new File(Preferences.main().getDirs().getCacheDirectory(true), "Tracks2Map.json");
        cacheManager = new TrackInfoCacheManager(cacheFile.toPath());
        setGPXDirectoryFromPreferences();

        openAction = new Tracks2MapOpenAction(cacheManager);
        openAction.setEnabled(false);
        syncAction = new Tracks2MapSyncCacheFromDiskAction(cacheManager);
        syncAction.setEnabled(true);
        deleteAllLayersAction = new Tracks2MapDeleteAllGPXLayersAction();
        deleteAllLayersAction.setEnabled(false);
        MainMenu.add(MainApplication.getMenu().fileMenu, openAction, MainMenu.WINDOW_MENU_GROUP.ALWAYS);
        MainMenu.add(MainApplication.getMenu().fileMenu, syncAction, MainMenu.WINDOW_MENU_GROUP.ALWAYS);
        MainMenu.add(MainApplication.getMenu().fileMenu, deleteAllLayersAction, MainMenu.WINDOW_MENU_GROUP.ALWAYS);
        // Register a shutdown thread that will save the cache with the track bounds to
        // file
        Runtime.getRuntime().addShutdownHook(new Thread("tracks2map-shutdown") {
            @Override
            public void run() {
                try {
                    cacheManager.saveCache();
                } catch (IOException e) {
                    System.err.println("Tracks2Map: Could not save the cached GPX track bounds to '"
                            + cacheManager.getCacheFilePath().toString() + "'");
                }
            }
        });
    }

    /**
     * Returns the track cache manager used for caching the bounds of previously
     * processed GPX tracks as well as saving them to disk and loading them from
     * disk
     *
     * @return The track cache manager.
     */
    public TrackInfoCacheManager getTrackCacheManager() {
        return cacheManager;
    }

    /**
     * Sets the GPX directory and recursion from the preference settings.
     */
    public void setGPXDirectoryFromPreferences() {
        IPreferences pref = Config.getPref();
        String gpxDirectoryName = pref.get(Tracks2MapPreferences.GPX_DIRECTORY,
                Tracks2MapPreferences.DEFAULT_GPX_DIRECTORY);
        boolean recursive = pref.getBoolean(Tracks2MapPreferences.RECURSIVE, true);
        Path gpxDirectory;
        if (gpxDirectoryName.isBlank())
            gpxDirectory = null;
        else
            gpxDirectory = Path.of(gpxDirectoryName);
        cacheManager.setGPXDirectory(gpxDirectory, recursive);
    }

    /**
     * Called after Main.mapFrame is initialized. (After the first data is loaded).
     * You can use this callback to tweak the newFrame to your needs, as example
     * install an alternative Painter.
     */
    @Override
    public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame) {
        openAction.setEnabled(newFrame != null);
        deleteAllLayersAction.setEnabled(newFrame != null);
    }

    /**
     * Called in the preferences dialog to create a preferences page for the plugin,
     * if any available.
     *
     * @return The preferences page.
     */
    @Override
    public PreferenceSetting getPreferenceSetting() {
        if (preferenceSetting == null)
            preferenceSetting = new Tracks2MapTabPreferenceSetting(this);
        return preferenceSetting;
    }
}
