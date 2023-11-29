package hhtznr.josm.plugins.tracks2map.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.IGpxTrack;
import org.openstreetmap.josm.data.gpx.IGpxTrackSegment;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.io.GpxReader;
import org.openstreetmap.josm.tools.Logging;
import org.xml.sax.SAXException;

/**
 * This class provides static utility methods for work with GPX files and GPX
 * data.
 *s
 * @author Harald Hetzner
 */
public class GPXUtils {

    private GPXUtils() {
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
    public static List<File> listGPXFiles(File directory, boolean recursive, List<File> gpxFileList) {
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

    /**
     * Reads the GPX data from a GPX file.
     *
     * @param gpxFile The GPX file to read.
     * @return The GPX data read from the file.
     * @throws IOException  Thrown if an I/O error occurs trying to read the file.
     * @throws SAXException Thrown if a SAX parsing error occurs.
     */
    public static GpxData readGPXFile(File gpxFile) throws IOException, SAXException {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(gpxFile);
            GpxReader gpxReader = new GpxReader(inputStream);
            gpxReader.parse(false);
            return gpxReader.getGpxData();
        } finally {
            if (inputStream != null)
                inputStream.close();
        }
    }

    /**
     * Checks if a GPX track intersects given bounds.
     *
     * @param gpxTrack The GPX track for which to perform the intersection check.
     * @param bounds   The (map) bounds for which to perform the intersection check.
     * @return {@code true} if the track runs through the bounds (or at least one of
     *         the way points touches them), {@code false} otherwise.
     */
    public static boolean trackIntersectsBounds(IGpxTrack gpxTrack, Bounds bounds) {
        // If the overall track bounds do not intersect the given bounds, there is
        // nothing left to check
        if (!bounds.intersects(gpxTrack.getBounds()))
            return false;
        // Otherwise check if the bounds of one of the track segments intersect the
        // given bounds
        for (IGpxTrackSegment segment : gpxTrack.getSegments()) {
            if (bounds.intersects(segment.getBounds())) {
                // If the bounds of the track segment intersect the given bounds, check if the
                // given bounds contain one of its way points
                // (It is still possible that the track segment goes around the given bounds but
                // does not intersect it)
                for (WayPoint wayPoint : segment.getWayPoints()) {
                    if (bounds.contains(wayPoint.getCoor()))
                        return true;
                }
            }
        }
        return false;
    }
}
