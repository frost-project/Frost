/*
  MainFrameStatusBar.java / Frost
  Copyright (C) 2006  Frost Project <jtcfrost.sourceforge.net>

  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License as
  published by the Free Software Foundation; either version 2 of
  the License, or (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software
  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
*/
package frost.gui;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ToolTipManager;
import javax.swing.border.BevelBorder;

import frost.Core;
import frost.Settings;
import frost.fileTransfer.FileTransferInformation;
import frost.messaging.frost.threads.RunningMessageThreadsInformation;
import frost.util.gui.MiscToolkit;
import frost.util.gui.translation.Language;

/**
 * Represents the mainframe status bar.
 */
@SuppressWarnings("serial")
public class MainFrameStatusBar extends JPanel {

    private final Language language;

    private JLabel statusLabelTofup = null;
    private JLabel statusLabelTofdn = null;
    private JLabel statusLabelBoard = null;
    private JLabel statusMessageLabel = null;

    private JLabel downloadingFilesLabel = null;

    private JLabel uploadingFilesLabel = null;

    private JLabel fileListDownloadQueueSizeLabel = null;

    private RunningMessageThreadsInformation statusBarInformations = null;

    private static ImageIcon[] newMessage = new ImageIcon[2];

    public MainFrameStatusBar() {
        super();
        language = Language.getInstance();
        initialize();
    }

    private void initialize() {

        uploadingFilesLabel = new JLabel();
        downloadingFilesLabel = new JLabel();

        final JPanel p0 = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        p0.add(uploadingFilesLabel);
        p0.add(new JLabel(" "));
        p0.add(downloadingFilesLabel);
        p0.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        p0.setAlignmentY(Component.CENTER_ALIGNMENT);

        statusLabelTofup = new JLabel() {
            @Override
            public String getToolTipText(final MouseEvent me) {
                if( statusBarInformations == null ) {
                    return null;
                }
                final String txt = language.formatMessage("MainFrameStatusBar.tooltip.tofup",
                        Integer.toString(statusBarInformations.getUploadingMessagesCount()),
                        Integer.toString(statusBarInformations.getUnsentMessageCount()),
                        Integer.toString(statusBarInformations.getAttachmentsToUploadRemainingCount()));
                return txt;
            }
        };
        // dynamic tooltip
        ToolTipManager.sharedInstance().registerComponent(statusLabelTofup);
        final JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p1.add(statusLabelTofup);
        p1.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        p1.setAlignmentY(Component.CENTER_ALIGNMENT);

        statusLabelTofdn = new JLabel() {
            @Override
            public String getToolTipText(final MouseEvent me) {
                if( statusBarInformations == null ) {
                    return null;
                }
                final String txt = language.formatMessage("MainFrameStatusBar.tooltip.tofdn",
                        Integer.toString(statusBarInformations.getDownloadingBoardCount()),
                        Integer.toString(statusBarInformations.getRunningDownloadThreadCount()));
                return txt;
            }
        };
        // dynamic tooltip
        ToolTipManager.sharedInstance().registerComponent(statusLabelTofdn);
        final JPanel p2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p2.add(statusLabelTofdn);
        p2.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        p2.setAlignmentY(Component.CENTER_ALIGNMENT);

        JPanel p3 = null;
        // shown only if filesharing is enabled
        if( Core.isFreenetOnline() && !Core.frostSettings.getBoolValue(Settings.FILESHARING_DISABLE)) {
            fileListDownloadQueueSizeLabel = new JLabel() {
                @Override
                public String getToolTipText(final MouseEvent me) {
                    final String txt = language.getString("MainFrame.statusBar.tooltip.fileListDownloadQueueSize");
                    return txt;
                }
            };
            // dynamic tooltip
            ToolTipManager.sharedInstance().registerComponent(fileListDownloadQueueSizeLabel);

            p3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            p3.add(fileListDownloadQueueSizeLabel);
            p3.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
            p3.setAlignmentY(Component.CENTER_ALIGNMENT);
        }

        statusLabelBoard = new JLabel();
        final JPanel p4 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p4.add(statusLabelBoard);
        p4.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        p4.setAlignmentY(Component.CENTER_ALIGNMENT);

        statusMessageLabel = new JLabel();
        statusMessageLabel.setBorder(BorderFactory.createEmptyBorder(1,1,1,1));
        final JPanel p5 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p5.add(statusMessageLabel);
        p5.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        p5.setAlignmentY(Component.CENTER_ALIGNMENT);;

        newMessage[0] = MiscToolkit.loadImageIcon("/data/messagebright.gif");
        newMessage[1] = MiscToolkit.loadImageIcon("/data/messagedark.gif");
        statusMessageLabel.setIcon(newMessage[1]);

        int currGridX = 0;

        final GridBagConstraints gridBagConstraints0 = new GridBagConstraints();
        gridBagConstraints0.gridx = currGridX++;
        gridBagConstraints0.anchor = GridBagConstraints.CENTER;
        gridBagConstraints0.insets = new Insets(1, 2, 1, 1);
        gridBagConstraints0.fill = GridBagConstraints.VERTICAL;
        gridBagConstraints0.gridy = 0;

        final GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
        gridBagConstraints1.gridx = currGridX++;
        gridBagConstraints1.anchor = GridBagConstraints.CENTER;
        gridBagConstraints1.insets = new Insets(1, 1, 1, 1);
        gridBagConstraints1.fill = GridBagConstraints.VERTICAL;
        gridBagConstraints1.gridy = 0;

        final GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
        gridBagConstraints2.gridx = currGridX++;
        gridBagConstraints2.anchor = GridBagConstraints.CENTER;
        gridBagConstraints2.insets = new Insets(1, 1, 1, 1);
        gridBagConstraints2.fill = GridBagConstraints.VERTICAL;
        gridBagConstraints2.gridy = 0;

        GridBagConstraints gridBagConstraints3 = null;
        if( fileListDownloadQueueSizeLabel != null ) {
            gridBagConstraints3 = new GridBagConstraints();
            gridBagConstraints3.gridx = currGridX++;
            gridBagConstraints3.anchor = GridBagConstraints.CENTER;
            gridBagConstraints3.insets = new Insets(1, 1, 1, 1);
            gridBagConstraints3.fill = GridBagConstraints.VERTICAL;
            gridBagConstraints3.gridy = 0;
        }

        final GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
        gridBagConstraints4.gridx = currGridX++;
        gridBagConstraints4.anchor = GridBagConstraints.CENTER;
        gridBagConstraints4.insets = new Insets(1, 1, 1, 1);
        gridBagConstraints4.fill = GridBagConstraints.VERTICAL;
        gridBagConstraints4.gridy = 0;

        final GridBagConstraints gridBagConstraints5 = new GridBagConstraints();
        gridBagConstraints5.gridx = currGridX++;
        gridBagConstraints5.weightx = 1.0;
        gridBagConstraints5.gridy = 0;

        final GridBagConstraints gridBagConstraints6 = new GridBagConstraints();
        gridBagConstraints6.gridx = currGridX++;
        gridBagConstraints6.anchor = GridBagConstraints.CENTER;
        gridBagConstraints6.insets = new Insets(1, 1, 1, 2);
        gridBagConstraints6.gridy = 0;

        setLayout(new GridBagLayout());
        add(p0, gridBagConstraints0);
        add(p1, gridBagConstraints1);
        add(p2, gridBagConstraints2);
        if( fileListDownloadQueueSizeLabel != null ) {
            add(p3, gridBagConstraints3);
        }
        add(p4, gridBagConstraints4);
        add(new JLabel(""), gridBagConstraints5); // glue
        add(p5, gridBagConstraints6);
    }

    public void setStatusBarInformations(final FileTransferInformation finfo, final RunningMessageThreadsInformation info, final String selectedNode) {

        this.statusBarInformations = info;

        String newText;
        StringBuilder sb;

        if( finfo != null ) {
            sb = new StringBuilder()
                .append(language.getString("MainFrame.statusBar.uploading")).append(": ")
                .append(finfo.getUploadsRunning())
                .append(" ");
            if( finfo.getUploadsRunning() == 1 ) {
                sb.append(language.getString("MainFrame.statusBar.file"));
            } else {
                sb.append(language.getString("MainFrame.statusBar.files"));
            }
            uploadingFilesLabel.setText(sb.toString());

            sb = new StringBuilder()
                .append(language.getString("MainFrame.statusBar.downloading")).append(": ")
                .append(finfo.getDownloadsRunning())
                .append(" ");
            if( finfo.getUploadsRunning() == 1 ) {
                sb.append(language.getString("MainFrame.statusBar.file"));
            } else {
                sb.append(language.getString("MainFrame.statusBar.files"));
            }
            downloadingFilesLabel.setText(sb.toString());

            if( fileListDownloadQueueSizeLabel != null ) {
                sb = new StringBuilder().append(" ")
                    .append(language.getString("MainFrame.statusBar.fileListDownloadQueueSize")).append(": ")
                    .append(finfo.getFileListDownloadQueueSize()).append(" ");
                fileListDownloadQueueSizeLabel.setText(sb.toString());
            }
        }

        if( info != null ) {
            newText = new StringBuilder()
                .append(" ")
                .append(language.getString("MainFrame.statusBar.TOFUP")).append(": ")
                .append(info.getUploadingMessagesCount())
                .append("U / ")
                .append(info.getUnsentMessageCount())
                .append("W / ")
                .append(info.getAttachmentsToUploadRemainingCount())
                .append("A ")
                .toString();
            statusLabelTofup.setText(newText);

            newText = new StringBuilder()
                .append(" ")
                .append(language.getString("MainFrame.statusBar.TOFDO")).append(": ")
                .append(info.getDownloadingBoardCount())
                .append("B / ")
                .append(info.getRunningDownloadThreadCount())
                .append("T ")
                .toString();
            statusLabelTofdn.setText(newText);
        }

        newText = new StringBuilder()
            .append(" ")
            .append(language.getString("MainFrame.statusBar.selectedBoard")).append(": ")
            .append(selectedNode)
            .append(" ")
            .toString();
        statusLabelBoard.setText(newText);
    }

    public void showNewMessageIcon(final boolean show) {
        if (show) {
            statusMessageLabel.setIcon(newMessage[0]);
        } else {
            statusMessageLabel.setIcon(newMessage[1]);
        }
    }
}
