package hhtznr.josm.plugins.tracks2map.gui;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.tools.Logging;

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
    public void actionPerformed(ActionEvent e) {
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

        List<File> gpxFiles = listGPXFiles(gpxDirectory, recursive, null);
        for (File gpxFile : gpxFiles)
            Logging.info("Tracks2Map: Found GPX file " + gpxFile.getAbsolutePath());
    }

    /**
     * Returns a list with all GPX files in the directory (provided that read
     * permissions are granted).
     *
     * @param directory   The directory from which to list the GPX files.
     * @param recursive   If {@code true}, traverse subdirectories recursively.
     * @param gpxFileList A list with GPX files to which to append the GPX files in
     *                    the specified directory (needed for recursion) or
     *                    {@code null} if a new list should be created.
     * @return The list of readable GPX files in the directory and its
     *         subdirectories if recursion was specfied.
     */
    private static List<File> listGPXFiles(File directory, boolean recursive, List<File> gpxFileList) {
        if (gpxFileList == null)
            gpxFileList = new ArrayList<>();
        // List the GPX directory and filter out files with GPX extension
        // https://www.baeldung.com/java-list-directory-files
        try {
            File[] filesInDirectory = directory.listFiles();
            Set<File> gpxFiles = Stream.of(filesInDirectory).filter(
                    file -> !file.isDirectory() && file.canRead() && file.getName().toLowerCase().endsWith(".gpx"))
                    .collect(Collectors.toSet());
            gpxFileList.addAll(gpxFiles);

            if (recursive) {
                Set<File> subDirectories = Stream.of(filesInDirectory)
                        .filter(file -> file.isDirectory() && file.canRead()).collect(Collectors.toSet());
                for (File subDirectory : subDirectories)
                    listGPXFiles(subDirectory, recursive, gpxFileList);
            }

        } catch (SecurityException e) {
            Logging.error("Tracks2Map: " + e.toString());
        }
        return gpxFileList;
    }

}
