package hhtznr.josm.plugins.tracks2map.gui;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.RejectedExecutionException;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.Logging;
import org.xml.sax.SAXException;

import hhtznr.josm.plugins.tracks2map.data.TrackInfoCacheManager;

public class Tracks2MapSyncCacheFromDiskAction extends JosmAction {

    private static final long serialVersionUID = 1L;

    private final TrackInfoCacheManager cacheManager;

    /**
     * Creates a new Tracks2Map action to synchronize the track info cache from GPX
     * files in the GPX directory on disk.
     *
     * @param cacheManager The track cache info manager.
     */
    public Tracks2MapSyncCacheFromDiskAction(TrackInfoCacheManager cacheManager) {
        super("Tracks2Map: Sync GPX tracks from disk", "dialogs/refresh.svg",
                "Synchronize cached track info from GPX files on disk", null, true, "tracks2map-sync-from-disk", false);
        this.cacheManager = cacheManager;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        SyncCacheFromDiskTask task = new SyncCacheFromDiskTask(cacheManager);
        try {
            MainApplication.worker.submit(task);
        } catch (RejectedExecutionException ex) {
            Logging.error("Tracks2Map: Execution of GPX track synchronization task rejected: " + ex.toString());
        }
    }

    public static class SyncCacheFromDiskTask extends PleaseWaitRunnable {

        private final TrackInfoCacheManager cacheManager;

        /**
         * Creates a new task for synchronization of the track info cache from GPX files
         * in the GPX directory on disk.
         *
         * @param cacheManager The track cache info manager.
         */
        public SyncCacheFromDiskTask(TrackInfoCacheManager cacheManager) {
            super("Sync GPX files from disk");
            this.cacheManager = cacheManager;
        }

        @Override
        protected void cancel() {
            cacheManager.cancelSync();
        }

        @Override
        protected void realRun() throws SAXException, IOException, OsmTransferException {
            Path gpxDirectory = cacheManager.getGPXDirectory();
            if (gpxDirectory == null) {
                Logging.error(
                        "Tracks2Map: Cannot sync GPX tracks. You need to define the path of the directory with the GPX tracks in the preferences first.");
                return;
            }
            cacheManager.syncCacheFromDisk(getProgressMonitor());
        }

        @Override
        protected void finish() {
        }
    }
}
