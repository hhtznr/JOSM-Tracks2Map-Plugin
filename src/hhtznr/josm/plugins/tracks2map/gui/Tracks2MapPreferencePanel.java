package hhtznr.josm.plugins.tracks2map.gui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.openstreetmap.josm.gui.widgets.VerticallyScrollablePanel;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.spi.preferences.IPreferences;
import org.openstreetmap.josm.tools.GBC;

import hhtznr.josm.plugins.tracks2map.Tracks2MapPreferences;

/**
 * GUI component for defining plugin preferences.
 *
 * @author Harald Hetzner
 */
public class Tracks2MapPreferencePanel extends VerticallyScrollablePanel {

    private static final long serialVersionUID = 1L;

    static final class AutoSizePanel extends JPanel {
        private static final long serialVersionUID = 1L;

        AutoSizePanel() {
            super(new GridBagLayout());
        }

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }
    }

    private JTextField tfGPXDirectory = new JTextField();

    private JButton btSelectGPXDirectory = new JButton("Select");

    private JCheckBox cbRecursive = new JCheckBox("Recursive");

    /**
     * Constructs a new panel with the preferences.
     */
    public Tracks2MapPreferencePanel() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(buildPreferencePanel(), GBC.eop().anchor(GridBagConstraints.NORTHWEST).fill(GridBagConstraints.BOTH));

        initFromPreferences();
    }

    /**
     * Builds the panel for the preferences.
     *
     * @return Panel with the preferences.
     */
    private final JPanel buildPreferencePanel() {
        final JLabel lblGPXDirectory = new JLabel("GPX Track Directory:");

        tfGPXDirectory.setEditable(false);
        btSelectGPXDirectory.addActionListener(event -> showDirectoryChooser());

        JPanel pnl = new AutoSizePanel();
        GridBagConstraints gc = new GridBagConstraints();

        gc.gridy++;
        gc.gridx = 0;
        gc.anchor = GridBagConstraints.LINE_START;
        gc.insets = new Insets(5, 5, 0, 0);
        gc.fill = GridBagConstraints.NONE;
        gc.gridwidth = 1;
        gc.weightx = 0.0;
        pnl.add(lblGPXDirectory, gc);

        gc.gridx++;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        pnl.add(tfGPXDirectory, gc);

        gc.gridx++;
        gc.fill = GridBagConstraints.NONE;
        gc.weightx = 0.0;
        pnl.add(btSelectGPXDirectory, gc);

        gc.gridy++;
        gc.gridx = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridwidth = 3;
        gc.weightx = 1.0;
        pnl.add(cbRecursive, gc);

        // add an extra spacer, otherwise the layout is broken
        gc.gridy++;
        gc.gridwidth = 3;
        gc.fill = GridBagConstraints.BOTH;
        gc.weighty = 1.0;
        pnl.add(new JPanel(), gc);
        return pnl;
    }

    /**
     * Initializes the panel with the values from the preferences.
     */
    private final void initFromPreferences() {
        IPreferences pref = Config.getPref();
        tfGPXDirectory
                .setText(pref.get(Tracks2MapPreferences.GPX_DIRECTORY, Tracks2MapPreferences.DEFAULT_GPX_DIRECTORY));
        cbRecursive.setSelected(pref.getBoolean(Tracks2MapPreferences.RECURSIVE, true));
    }

    /**
     * Shows a file chooser for selection of the directory containing GPX files.
     */
    private void showDirectoryChooser() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select a directory with GPX files");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setAcceptAllFileFilterUsed(false);
        if (!tfGPXDirectory.getText().equals("")) {
            File currentDirectory = new File(tfGPXDirectory.getText());
            fileChooser.setCurrentDirectory(currentDirectory);
        }
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedDirectory = fileChooser.getSelectedFile();
            tfGPXDirectory.setText(selectedDirectory.getAbsolutePath());
        }

    }

    /**
     * Saves the current values to the preferences.
     */
    public void saveToPreferences() {
        IPreferences pref = Config.getPref();
        pref.put(Tracks2MapPreferences.GPX_DIRECTORY, tfGPXDirectory.getText());
        pref.putBoolean(Tracks2MapPreferences.RECURSIVE, cbRecursive.isSelected());
    }
}
