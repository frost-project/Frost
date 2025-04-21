/*
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package frost.util.gui;

import java.awt.Component;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/**
 * @author <a href="mailto:droden@gmail.com">David Roden</a>, ET
 */
public class FileProgressBar extends JPanel implements TableCellRenderer {

	private static final long serialVersionUID = 1L;

//    public interface IFileProgress {
//        // states for a block inside a file
//        public static final int STATE_DONE       = 0;
//        public static final int STATE_WAITING    = 1;
//        public static final int STATE_REQUESTING = 2;
//        public static final int STATE_SAVING     = 3;
//        public static final int STATE_ERROR      = 4;
//    }
//	
//	private FreenetFile freenetFile;
//    
//	private static Color[] stateColors = new Color[] {
//        new Color(0.0f, 0.7f, 0.0f),    // done
//        new Color(0.3f, 0.3f, 0.3f),    // waiting
//        new Color(0.0f, 0.0f, 1.0f),    // requesting
//        new Color(0.55f, 0.55f, 0.25f), // saving
//        new Color(1.0f, 0.0f, 0.0f)     // error
//    };
//	private boolean showPercentage = true;
//	private double roundness = 0.5;
//	private Font percentageFont = new Font("Tahoma", 0, 12);
//
//	public FileProgressBar() {
//	}
//
//	public void setRoundness(double roundness) {
//		this.roundness = roundness;
//	}
//
//	public void setShowPercentage(boolean showPercentage) {
//		this.showPercentage = showPercentage;
//	}
//
//	private void paintBar(Graphics g, int xLeft, int xRight, int height, Color color) {
//		for (int y = 0; y < height; y++) {
//			double f = Math.cos((y - height / 2.0d) * Math.PI / height) * (roundness) + (1 - roundness);
//			Color roundColor = new Color((int) (color.getRed() * f), (int) (color.getGreen() * f), (int) (color.getBlue() * f));
//			g.setColor(roundColor);
//			g.drawLine(xLeft, y, xRight, y);
//		}
//	}
//
//	protected void paintComponent(Graphics g) {
//		int width = getWidth();
//		int height = getHeight();
//		switch (freenetFile.fileStatus) {
//			case activated:
//			case deactivated:
//				paintBlocks(g, width, height);
//				break;
//			case complete:
//				paintBar(g, 0, width - 1, height, Color.GREEN);
//				break;
//			case error:
//				paintBar(g, 0, width - 1, height, Color.RED);
//		}
//	}
//
//	private void paintBlocks(Graphics g, int width, int height) {
//		int totalBlocks = freenetFile.blocksStatus.length;
//		int completeBlocks = 0;
//		if (totalBlocks > 0) {
//			for (int x = 0; x < width; x++) {
//				int block = (int) (((double) x / width) * totalBlocks);
//				int newBlock = block;
//				int nx = x;
//				while (newBlock == block) {
//					nx++;
//					newBlock = (int) (((double) nx / width) * totalBlocks);
//				}
//				nx--;
//				if (freenetFile.blocksStatus[block] == BlockStatus.available) {
//					completeBlocks++;
//				}
//				Color color = stateColors.get(freenetFile.blocksStatus[block]);
//				paintBar(g, x, nx, height, color);
//				x = nx;
//			}
//			g.setColor(new Color(0, 192, 0));
//			g.drawRect(1, 1, width * completeBlocks / totalBlocks - 1, 1);
//			g.setColor(Color.BLACK);
//			g.drawRect(0, 0, width * completeBlocks / totalBlocks, 3);
//			if (showPercentage) {
//				g.setFont(percentageFont);
//				g.setColor(Color.WHITE);
//				String text = (completeBlocks * 100 / totalBlocks) + "%";
//				FontMetrics metrics = g.getFontMetrics();
//				Rectangle2D textBounds = metrics.getStringBounds(text, g);
//				g.drawString(text, width / 2 - (int) textBounds.getWidth() / 2, (height - 1) / 2 + (int) textBounds.getHeight() / 2 - 1);
//			}
//		} else {
//			paintBar(g, 0, width - 1, height, stateColors.get(BlockStatus.notAvailable));
//			if (showPercentage) {
//				g.setFont(percentageFont);
//				g.setColor(Color.WHITE);
//				String text = "0%";
//				FontMetrics metrics = g.getFontMetrics();
//				Rectangle2D textBounds = metrics.getStringBounds(text, g);
//				g.drawString(text, width / 2 - (int) textBounds.getWidth() / 2, (height - 1) / 2 + (int) textBounds.getHeight() / 2 - 1);
//			}
//		}
//	}
//
//	public Dimension getPreferredSize() {
//		return new Dimension(super.getPreferredSize().width, 21);
//	}
//
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
//		freenetFile = (FreenetFile)value;
		return this;
	}
}
