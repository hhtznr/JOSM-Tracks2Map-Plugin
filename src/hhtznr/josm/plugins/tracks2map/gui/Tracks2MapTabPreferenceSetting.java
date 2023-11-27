package hhtznr.josm.plugins.tracks2map.gui;

import javax.swing.Box;

import org.openstreetmap.josm.gui.preferences.DefaultTabPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.tools.GBC;

/**
 * Tracks2Map tab in preferences.
 *
 * @author Harald Hetzner
 */
public class Tracks2MapTabPreferenceSetting extends DefaultTabPreferenceSetting {

    private final Tracks2MapPreferencePanel preferencePanel;

    public Tracks2MapTabPreferenceSetting() {
        super("tracks2map", "Tracks2Map", "Preferences for opening of GPX tracks in map view");
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
        return false;
    }

}
