package hhtznr.josm.plugins.tracks2map;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;

import hhtznr.josm.plugins.tracks2map.gui.Tracks2MapOpenAction;
import hhtznr.josm.plugins.tracks2map.gui.Tracks2MapTabPreferenceSetting;

/**
 * Plugin class for opening all GPX tracks in the current map view.
 *
 * @author Harald Hetzner
 */
public class Tracks2MapPlugin extends Plugin {

    private Tracks2MapOpenAction openAction;

    private Tracks2MapTabPreferenceSetting preferenceSetting = null;

    public Tracks2MapPlugin(PluginInformation info) {
        super(info);

        openAction = new Tracks2MapOpenAction();
        openAction.setEnabled(false);
        MainMenu.addWithCheckbox(MainApplication.getMenu().fileMenu, openAction, MainMenu.WINDOW_MENU_GROUP.ALWAYS);
    }

    /**
     * Called after Main.mapFrame is initialized. (After the first data is loaded).
     * You can use this callback to tweak the newFrame to your needs, as example
     * install an alternative Painter.
     */
    @Override
    public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame) {
        openAction.setEnabled(newFrame != null);
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
