package hhtznr.josm.plugins.tracks2map.gui;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.IGpxTrack;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.io.importexport.GpxImporter;
import org.openstreetmap.josm.tools.Logging;
import org.xml.sax.SAXException;

import hhtznr.josm.plugins.tracks2map.utils.GPXUtils;

/**
 * A menu action for opening all GPX tracks crossing the current map view from a
 * directory defined by the user in the preferences.
 *
 * @author Harald Hetzner
 */
public class Tracks2MapOpenAction extends JosmAction {

    private static final long serialVersionUID = 1L;

    private File gpxDirectory = null;
    private boolean recursive = true;

    public Tracks2MapOpenAction() {
        super("Open all GPX tracks in map view", "tracks2map.svg",
                "Open all GPX tracks from the directory defined in the preferences which cross the current map view",
                null, true, "tracks2map-open", false);
    }

    /**
     * Sets the directory from which to read tracks from GPX files.
     *
     * @param gpxDirectory The director with the GPX files from which to read the
     *                     tracks.
     * @param recursive    If {@code true}, traverse subdirectories recursively.
     */
    public void setGPXDirectory(File gpxDirectory, boolean recursive) {
        this.gpxDirectory = gpxDirectory;
        this.recursive = recursive;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        MapFrame mapFrame = MainApplication.getMap();
        if (mapFrame == null)
            return;
        if (gpxDirectory == null) {
            Logging.error(
                    "Tracks2Map: You need to define the path of the directory with the GPX tracks in the preferences first");
            return;
        }
        if (!gpxDirectory.exists()) {
            Logging.error("Tracks2Map: GPX directory path '" + gpxDirectory.getAbsolutePath() + "' does not exist");
            return;
        }
        if (!gpxDirectory.isDirectory()) {
            Logging.error("Tracks2Map: GPX directory path '" + gpxDirectory.getAbsolutePath() + "' is not a directory");
            return;
        }
        if (!gpxDirectory.canRead()) {
            Logging.error("Tracks2Map: Permission for reading from GPX directory path '"
                    + gpxDirectory.getAbsolutePath() + "' is denied");
            return;
        }

        // Determine the bounds of the map view
        Bounds mapBounds = mapFrame.mapView.getLatLonBounds(mapFrame.mapView.getBounds());

        // List for collecting GPX data with tracks that run through the map view
        ArrayList<GpxData> gpxTracksIntersectingMap = new ArrayList<>();

        // List all GPX files
        List<File> gpxFiles = GPXUtils.listGPXFiles(gpxDirectory, recursive, null);
        for (File gpxFile : gpxFiles) {
            try {
                GpxData gpxData = GPXUtils.readGPXFile(gpxFile);
                Logging.info("Tracks2Map: Read GPX file " + gpxFile.getAbsolutePath());
                gpxData.storageFile = gpxFile;
                Collection<IGpxTrack> gpxTracks = gpxData.getTracks();
                for (IGpxTrack gpxTrack : gpxTracks) {
                    if (GPXUtils.trackIntersectsBounds(gpxTrack, mapBounds)) {
                        Logging.info("Tracks2Map: Track in GPX file '" + gpxFile.getAbsolutePath()
                                + "' intersects map view bounds");
                        // In case of intersection, collect the GPX data and continue with the next one
                        gpxTracksIntersectingMap.add(gpxData);
                        continue;
                    }
                }
            } catch (IOException | SAXException e) {
                Logging.error("Tracks2Map: " + e.toString());
            }
        }

        // Open the collected GPX data in new layers (as JOSM does it if the user conventionally opens GPX files)
        for (GpxData gpxData : gpxTracksIntersectingMap)
            GpxImporter.addLayers(GpxImporter.loadLayers(gpxData, true, gpxData.storageFile.getName()));
        // Zoom the map view back to the previous bounds
        mapFrame.mapView.zoomTo(mapBounds);
    }
}
