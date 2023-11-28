package hhtznr.josm.plugins.tracks2map.gui;

import javax.swing.Box;

import org.openstreetmap.josm.gui.preferences.DefaultTabPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.tools.GBC;

import hhtznr.josm.plugins.tracks2map.Tracks2MapPlugin;

/**
 * Tracks2Map tab in preferences.
 *
 * @author Harald Hetzner
 */
public class Tracks2MapTabPreferenceSetting extends DefaultTabPreferenceSetting {

    private final Tracks2MapPlugin plugin;
    private final Tracks2MapPreferencePanel preferencePanel;

    /**
     * Creates a new tab for Tracks2Map preference settings.
     *
     * @param plugin The plugin instance for which this tab is created.
     */
    public Tracks2MapTabPreferenceSetting(Tracks2MapPlugin plugin) {
        super("tracks2map", "Tracks2Map", "Preferences for opening of GPX tracks in map view");
        this.plugin = plugin;
        preferencePanel = new Tracks2MapPreferencePanel();

    }

    @Override
    public void addGui(PreferenceTabbedPane gui) {
        preferencePanel.add(Box.createVerticalGlue(), GBC.eol().fill());
        gui.createPreferenceTab(this).add(preferencePanel, GBC.eol().fill());
    }

    @Override
    public boolean ok() {
        preferencePanel.saveToPreferences();
        plugin.setGPXDirectoryFromPreferences();
        return false;
    }

}
