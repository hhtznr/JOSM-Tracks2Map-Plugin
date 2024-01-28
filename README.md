# JOSM Tracks2Map Plugin
**Tracks2Map** is a plugin for the [OpenStreetMap](https://www.openstreetmap.org/) editor [JOSM](https://josm.openstreetmap.de/). It opens all GPX tracks in the current map view from a specified directory. Optionally, it can work recursively on subdirectories. Only those tracks, are opened that actually cross the map view. If available, associated markers are opened as well.

As the plugin may open a number of GPX track layers and GPX marker layers at a time, it also provides a menu option for deleting all GPX layers at a time.

## Building from source
**Tracks2Map** plugin is configured as <a href="https://www.eclipse.org/">Eclipse</a> Java project. The project directory must be placed in <code>josm/plugins</code> of the original <a href="https://josm.openstreetmap.de/svn/trunk">JOSM source</a> tree for <a href="https://github.com/hhtznr/JOSM-Tracks2Map-Plugin/blob/main/build.xml">build.xml</a> to work. The location of the JOSM project needs to be specified on the build path. The built plugin <code>Tracks2Map.jar</code> will be found in <code>josm/dist</code>.

## Installation
1. Copy the plugin JAR file `Tracks2Map.jar` into the JOSM plugins directory. Under Linux, the plugins directory should be located at `$HOME/.local/share/JOSM/plugins`.
2. Launch JOSM and select `Edit -> Preferences` from the menu bar.
3. In the opened preferences dialog, select the tab `Plugins`.
4. In the sub-tab `Plugins`, select radio button option `Available`.
5. Search for `Tracks2Map: [...]` in the list of available plugins and select the checkbox.
6. Click the `OK` button at the bottom.
7. It might be necessary to restart JOSM for the changes to take effect.

## Configuration
1. From the JOSM menu bar, select `Edit -> Preferences`.
2. In the opened preferences dialog, select the tab `Tracks2Map`.
3. Select the directory with your recorded or gathered GPX tracks via the file chooser dialog and check the `Recursive` option as needed.
4. Click the `OK` button at the bottom.

For further information and screenshots visit the [project wiki](https://github.com/hhtznr/JOSM-Tracks2Map-Plugin/wiki) or the [Track2Map plugin page on the OpenStreetMap wiki](https://wiki.openstreetmap.org/wiki/JOSM/Plugins/Tracks2Map).
