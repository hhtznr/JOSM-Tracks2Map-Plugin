package hhtznr.josm.plugins.tracks2map;

import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;

import hhtznr.josm.plugins.tracks2map.gui.Tracks2MapTabPreferenceSetting;

/**
 * Plugin class for opening all GPX tracks in the current map view.
 *
 * @author Harald Hetzner
 */
public class Tracks2MapPlugin extends Plugin {

    private Tracks2MapTabPreferenceSetting preferenceSetting = null;

    public Tracks2MapPlugin(PluginInformation info) {
        super(info);
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
            preferenceSetting = new Tracks2MapTabPreferenceSetting();
        return preferenceSetting;
    }
}
