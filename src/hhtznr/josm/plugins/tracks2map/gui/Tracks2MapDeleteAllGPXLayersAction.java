package hhtznr.josm.plugins.tracks2map.gui;

import java.awt.event.ActionEvent;
import java.util.List;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.MainLayerManager;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;

/**
 * A menu action for deleting all open GPX track layers and GPX marker layers at
 * once.
 *
 * @author Harald Hetzner
 */
public class Tracks2MapDeleteAllGPXLayersAction extends JosmAction {

    private static final long serialVersionUID = 1L;

    public Tracks2MapDeleteAllGPXLayersAction() {
        super("Delete all GPX track and marker layers", "delete-all-gpx.svg", "Delete all GPX track and GPX marker layers at once", null,
                true, "tracks2map-delete-all-gpx-layers", false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        MainLayerManager layerManager = MainApplication.getLayerManager();
        List<GpxLayer> gpxLayers = layerManager.getLayersOfType(GpxLayer.class);
        List<MarkerLayer> markerLayers = layerManager.getLayersOfType(MarkerLayer.class);
        for (GpxLayer layer : gpxLayers)
            layerManager.removeLayer(layer);
        for (MarkerLayer layer : markerLayers)
            layerManager.removeLayer(layer);
    }

}
