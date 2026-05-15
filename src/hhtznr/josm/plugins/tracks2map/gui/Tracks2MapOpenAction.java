package hhtznr.josm.plugins.tracks2map.gui;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;
import java.util.stream.Collectors;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.IGpxTrack;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.io.importexport.GpxImporter;
import org.openstreetmap.josm.gui.io.importexport.GpxImporter.GpxImporterData;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.MainLayerManager;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.Logging;
import org.xml.sax.SAXException;

import hhtznr.josm.plugins.tracks2map.data.TrackInfoCacheEntry;
import hhtznr.josm.plugins.tracks2map.data.TrackInfoCacheManager;
import hhtznr.josm.plugins.tracks2map.utils.GPXUtils;

/**
 * A menu action for opening all GPX tracks crossing the current map view from a
 * directory defined by the user in the preferences.
 *
 * @author Harald Hetzner
 */
public class Tracks2MapOpenAction extends JosmAction {

    private static final long serialVersionUID = 1L;

    private final TrackInfoCacheManager cacheManager;

    /**
     * Creates a new Tracks2Map open action.
     *
     * @param cacheManager  The track cache info manager.
     */
    public Tracks2MapOpenAction(TrackInfoCacheManager cacheManager) {
        super("Tracks2Map: Open all GPX tracks in map view", "tracks2map.svg",
                "Open all GPX tracks from the directory defined in the preferences which cross the current map view",
                null, true, "tracks2map-open", false);
        this.cacheManager = cacheManager;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        MapFrame mapFrame = MainApplication.getMap();
        if (mapFrame == null)
            return;

        HashMap<String, TrackInfoCacheEntry> cachedGPXFileInfo = cacheManager.getCacheEntries();
        // If no info on GPX tracks is cached yet ...
        if (cachedGPXFileInfo.isEmpty()) {
            // ... try to load the GPX track info from persistent cache file
            try {
                cacheManager.loadCache();
                cachedGPXFileInfo = cacheManager.getCacheEntries();
            } catch (IOException e) {
                Logging.error("Tracks2Map: Could not load cached GPX track info from cache file: " + e.toString());
            }

            // If no GPX track info could be retrieved from cache file, synchronize tracks
            // from disk
            if (cachedGPXFileInfo.isEmpty()) {
                Tracks2MapSyncCacheFromDiskAction.SyncCacheFromDiskTask task = new Tracks2MapSyncCacheFromDiskAction.SyncCacheFromDiskTask(
                        cacheManager);
                try {
                    MainApplication.worker.submit(task);
                } catch (RejectedExecutionException ex) {
                    Logging.error("Tracks2Map: Execution of GPX track synchronization task rejected: " + ex.toString());
                }
            }
        }

        OpenGPXTracksTask task = new OpenGPXTracksTask(cacheManager, mapFrame.mapView);
        try {
            MainApplication.worker.submit(task);
        } catch (RejectedExecutionException e) {
            Logging.error("Tracks2Map: Execution of GPX file open task rejected: " + e.toString());
        }
    }

    private static class OpenGPXTracksTask extends PleaseWaitRunnable {

        private final TrackInfoCacheManager cacheManager;
        private final MapView mapView;
        private boolean canceled = false;

        public OpenGPXTracksTask(TrackInfoCacheManager cacheManager, MapView mapView) {
            super("Opening all GPX tracks in map view");
            this.cacheManager = cacheManager;
            this.mapView = mapView;
        }

        @Override
        protected void cancel() {
            canceled = true;
        }

        @Override
        protected void realRun() throws SAXException, IOException, OsmTransferException {
            HashMap<String, TrackInfoCacheEntry> cachedGPXFileInfo = cacheManager.getCacheEntries();

            // Determine the bounds of the map view
            Bounds mapBounds = mapView.getLatLonBounds(mapView.getBounds());

            // Create a JOSM progress monitor for this task
            ProgressMonitor progressMonitor = getProgressMonitor();
            progressMonitor.setTicksCount(cachedGPXFileInfo.size());

            // Create a list of the cache entries, alphabetically sorted by the path
            List<TrackInfoCacheEntry> sortedGPXFileInfo = cachedGPXFileInfo.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey()).map(Map.Entry::getValue).collect(Collectors.toList());

            // List existing GPX layers
            MainLayerManager layerManager = MainApplication.getLayerManager();
            List<GpxLayer> existingGpxLayers = layerManager.getLayersOfType(GpxLayer.class);

            int numberOfTracksIntersectingMap = 0;

            for (TrackInfoCacheEntry cacheEntry : sortedGPXFileInfo) {
                if (canceled)
                    return;
                String gpxFilePath = cacheEntry.getGPXFilePath();
                progressMonitor.setCustomText(gpxFilePath);
                Bounds gpxDataBounds = cacheEntry.getBounds();
                if (mapBounds.intersects(gpxDataBounds)) {
                    try {
                        // Read the GPX data from the file
                        GpxData gpxData = GPXUtils.readGPXFile(Path.of(gpxFilePath));
                        Logging.info("Tracks2Map: Read GPX file " + gpxFilePath);
                        Collection<IGpxTrack> gpxTracks = gpxData.getTracks();

                        // Iterate over all tracks in the GPX data
                        for (IGpxTrack gpxTrack : gpxTracks) {
                            // Bounds of the track
                            Bounds trackBounds = gpxTrack.getBounds();
                            // Continue with next track if current track does not intersect map view
                            if (!mapBounds.intersects(trackBounds))
                                continue;
                            // Otherwise, check if the track actually runs through the map view
                            if (GPXUtils.trackIntersectsBounds(gpxTrack, mapBounds)) {
                                Logging.info("Tracks2Map: Track in GPX file '" + gpxFilePath
                                        + "' intersects map view bounds");
                                numberOfTracksIntersectingMap++;
                                // In case of intersection, the GPX data and continue with the next one
                                // Open the collected GPX data in new layers (as JOSM does it if the user
                                // conventionally opens GPX files)
                                String layerName = gpxData.storageFile.getName();
                                boolean addLayer = true;
                                // Check if a GPX layer by the same name already exists
                                for (GpxLayer layer : existingGpxLayers) {
                                    if (layer.getName().equals(layerName)) {
                                        Logging.info("Tracks2Map: Will not add a new GPX layer for GPX file '"
                                                + gpxData.storageFile.getAbsolutePath()
                                                + "'. GPX layer for this file name already exists.");
                                        addLayer = false;
                                        break;
                                    }
                                }
                                // Add a new GPX layer if no GPX layer with the same name exists
                                // (omit adding the same layer multiple times upon repeated execution)
                                if (addLayer) {
                                    GpxImporterData data = GpxImporter.loadLayers(gpxData, true, layerName);
                                    if (data.getMarkerLayer() != null)
                                        layerManager.addLayer(data.getMarkerLayer(), false);
                                    if (data.getGpxLayer() != null) {
                                        layerManager.addLayer(data.getGpxLayer(), false);
                                    }
                                }
                                progressMonitor.setExtraText(" (found tracks: " + numberOfTracksIntersectingMap + ")");
                                break;
                            }
                        }
                    } catch (IOException | SAXException e) {
                        // Remove GPX files causing errors from the cache
                        cacheManager.removeFromCache(gpxFilePath);
                        Logging.error("Tracks2Map: " + e.toString());
                    }
                }

                // Increase progress by one tick
                progressMonitor.worked(1);
            }
        }

        @Override
        protected void finish() {
        }
    }
}
