package hhtznr.josm.plugins.tracks2map.gui;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.IGpxTrack;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.io.importexport.GpxImporter;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.OsmTransferException;
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

    private static HashMap<String, GPXFileInfo> fileInfoCache = new HashMap<>();

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

        OpenGPXTracksTask task = new OpenGPXTracksTask(gpxDirectory, recursive, mapFrame.mapView);
        try {
            MainApplication.worker.submit(task);
        } catch (RejectedExecutionException e) {
            Logging.error("Tracks2Map: Execution of GPX file open task rejected: " + e.toString());
        }
    }

    public static class OpenGPXTracksTask extends PleaseWaitRunnable {

        private final File gpxDirectory;
        private final boolean recursive;
        private final MapView mapView;
        private boolean canceled = false;

        public OpenGPXTracksTask(File gpxDirectory, boolean recursive, MapView mapView) {
            super("Opening all GPX tracks in map view");
            this.gpxDirectory = gpxDirectory;
            this.recursive = recursive;
            this.mapView = mapView;
        }

        @Override
        protected void cancel() {
            canceled = true;
        }

        @Override
        protected void realRun() throws SAXException, IOException, OsmTransferException {
            // Determine the bounds of the map view
            Bounds mapBounds = mapView.getLatLonBounds(mapView.getBounds());

            // List for collecting GPX data with tracks that run through the map view
            ArrayList<GpxData> gpxTracksIntersectingMap = new ArrayList<>();

            // List all GPX files and sort them by their absolute path
            List<File> gpxFiles = GPXUtils.listGPXFiles(gpxDirectory, recursive, null);
            Collections.sort(gpxFiles, (File file1, File file2) -> {
                return file1.getAbsolutePath().compareTo(file2.getAbsolutePath());
            });
            // Setup a JOSM progress monitor for this task
            ProgressMonitor progressMonitor = getProgressMonitor();
            progressMonitor.setTicksCount(gpxFiles.size());

            HashMap<String, GPXFileInfo> fileInfoCacheNew = new HashMap<>();
            for (File gpxFile : gpxFiles) {
                if (canceled)
                    return;
                String gpxFilePath = gpxFile.getAbsolutePath();
                getProgressMonitor().setCustomText(gpxFilePath);

                GPXFileInfo fileInfo = Tracks2MapOpenAction.fileInfoCache.get(gpxFilePath);
                long gpxFileLastModified = gpxFile.lastModified();
                // GPX file not known yet, known and modified or known and intersects map view
                if (fileInfo == null || fileInfo.lastModified < gpxFileLastModified
                        || mapBounds.intersects(fileInfo.bounds)) {
                    try {
                        // Read the GPX data from the file
                        GpxData gpxData = GPXUtils.readGPXFile(gpxFile);
                        Logging.info("Tracks2Map: Read GPX file " + gpxFilePath);
                        gpxData.storageFile = gpxFile;
                        Collection<IGpxTrack> gpxTracks = gpxData.getTracks();
                        Bounds overallTrackBounds = null;
                        for (IGpxTrack gpxTrack : gpxTracks) {
                            // Establish overall bounds of the tracks in the file
                            Bounds trackBounds = gpxTrack.getBounds();
                            if (overallTrackBounds == null)
                                overallTrackBounds = trackBounds;
                            else
                                overallTrackBounds.extend(trackBounds);

                            // If the overall track bounds do not intersect the given bounds, there is
                            // nothing left to check
                            if (!mapBounds.intersects(trackBounds))
                                continue;
                            if (GPXUtils.trackIntersectsBounds(gpxTrack, mapBounds)) {
                                Logging.info("Tracks2Map: Track in GPX file '" + gpxFilePath
                                        + "' intersects map view bounds");
                                // In case of intersection, collect the GPX data and continue with the next one
                                gpxTracksIntersectingMap.add(gpxData);
                                progressMonitor
                                        .setExtraText(" (found tracks: " + gpxTracksIntersectingMap.size() + ")");
                            }
                        }
                        // Remember the information on the GPX file
                        fileInfoCacheNew.put(gpxFilePath, new GPXFileInfo(gpxFileLastModified, overallTrackBounds));
                    } catch (IOException | SAXException e) {
                        Logging.error("Tracks2Map: " + e.toString());
                    }
                }
                // GPX file already known, unmodified and does not intersect map view
                else {
                    fileInfoCacheNew.put(gpxFilePath, fileInfo);
                }
                // Increase progress by one tick
                progressMonitor.worked(1);
            }

            // Exchange the previous cache against the new cache
            Tracks2MapOpenAction.fileInfoCache = fileInfoCacheNew;

            // Open the collected GPX data in new layers (as JOSM does it if the user
            // conventionally opens GPX files)
            for (GpxData gpxData : gpxTracksIntersectingMap) {
                if (canceled)
                    break;
                GpxImporter.addLayers(GpxImporter.loadLayers(gpxData, true, gpxData.storageFile.getName()));
            }

            // Zoom the map view back to the previous bounds
            mapView.zoomTo(mapBounds);
        }

        @Override
        protected void finish() {
        }
    }

    /**
     * Helper class for remembering modification time and overall bounds of GPX
     * files.
     */
    private static class GPXFileInfo {
        public long lastModified;
        public Bounds bounds;

        public GPXFileInfo(long lastModified, Bounds bounds) {
            this.lastModified = lastModified;
            this.bounds = bounds;
        }
    }
}
