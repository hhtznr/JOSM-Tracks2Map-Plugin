package hhtznr.josm.plugins.tracks2map.gui;

import java.awt.event.ActionEvent;

import org.openstreetmap.josm.actions.JosmAction;

/**
 * A menu action for opening all GPX tracks crossing the current map view from a
 * directory defined by the user in the preferences.
 *
 * @author Harald Hetzner
 */
public class Tracks2MapOpenAction extends JosmAction {

    private static final long serialVersionUID = 1L;

    public Tracks2MapOpenAction() {
        super("Open all GPX tracks in map view", "tracks2map.svg",
                "Open all GPX tracks from the directory defined in the preferences which cross the current map view",
                null, true, "tracks2map-open", false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
    }

}
