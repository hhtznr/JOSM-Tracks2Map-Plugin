package hhtznr.josm.plugins.tracks2map;

import java.io.File;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.spi.preferences.IPreferences;
import org.openstreetmap.josm.tools.Utils;

import hhtznr.josm.plugins.tracks2map.gui.Tracks2MapDeleteAllGPXLayersAction;
import hhtznr.josm.plugins.tracks2map.gui.Tracks2MapOpenAction;
import hhtznr.josm.plugins.tracks2map.gui.Tracks2MapTabPreferenceSetting;

/**
 * Plugin class for opening all GPX tracks in the current map view.
 *
 * @author Harald Hetzner
 */
public class Tracks2MapPlugin extends Plugin {

    private final Tracks2MapOpenAction openAction;
    private final Tracks2MapDeleteAllGPXLayersAction deleteAllLayersAction;

    private Tracks2MapTabPreferenceSetting preferenceSetting = null;

    public Tracks2MapPlugin(PluginInformation info) {
        super(info);

        openAction = new Tracks2MapOpenAction();
        openAction.setEnabled(false);
        setGPXDirectoryFromPreferences();
        deleteAllLayersAction = new Tracks2MapDeleteAllGPXLayersAction();
        deleteAllLayersAction.setEnabled(false);
        MainMenu.add(MainApplication.getMenu().fileMenu, openAction, MainMenu.WINDOW_MENU_GROUP.ALWAYS);
        MainMenu.add(MainApplication.getMenu().fileMenu, deleteAllLayersAction, MainMenu.WINDOW_MENU_GROUP.ALWAYS);
    }

    /**
     * Sets the GPX directory and recursion from the preference settings.
     */
    public void setGPXDirectoryFromPreferences() {
        IPreferences pref = Config.getPref();
        String gpxDirectoryName = pref.get(Tracks2MapPreferences.GPX_DIRECTORY,
                Tracks2MapPreferences.DEFAULT_GPX_DIRECTORY);
        boolean recursive = pref.getBoolean(Tracks2MapPreferences.RECURSIVE, true);
        File gpxDirectory;
        if (Utils.isBlank(gpxDirectoryName))
            gpxDirectory = null;
        else
            gpxDirectory = new File(gpxDirectoryName);
        openAction.setGPXDirectory(gpxDirectory, recursive);
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
