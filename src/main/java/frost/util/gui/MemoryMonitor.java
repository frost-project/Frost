package frost.util.gui;
/*
 * @(#)MemoryMonitor.java	1.26 99/04/23
 *
 * Copyright (c) 1998, 1999 by Sun Microsystems, Inc. All Rights Reserved.
 *
 * Sun grants you ("Licensee") a non-exclusive, royalty free, license to use,
 * modify and redistribute this software in source and binary code form,
 * provided that i) this copyright notice and license appear on all copies of
 * the software; and ii) Licensee does not utilize the software in a manner
 * which is disparaging to Sun.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING ANY
 * IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE
 * LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING
 * OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS
 * LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT,
 * INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF
 * OR INABILITY TO USE SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGES.
 *
 * This software is not designed or intended for use in on-line control of
 * aircraft, air traffic, aircraft navigation or aircraft communications; or in
 * the design, construction, operation or maintenance of any nuclear
 * facility. Licensee represents and warrants that it will not use or
 * redistribute the Software for such purposes.
 */

/**
 * 05/07/31, added sequential system.gc() call
 */
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

/**
 * Tracks Memory allocated & used, displayed in graph form.
 */
public class MemoryMonitor extends JPanel {

	private static final long serialVersionUID = 1L;

	public Surface surf;
//    JPanel controls;
//    boolean doControls;
//    JTextField tf;
//    JCheckBox box;

    JFrame dialog = null;
    boolean isShown = false;

	public static void main(String[] args) {
		MemoryMonitor mem = new MemoryMonitor();
		mem.showDialog();
	}

    private JFrame getDialog() {
        if( dialog == null ) {
            dialog = new JFrame();
            dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
//            dialog.setAlwaysOnTop(true); // TODO: since 1.5 !!!
            dialog.getContentPane().add(this);
            dialog.setSize(225,130);
            dialog.setTitle("Frost Memory Monitor");
            final ImageIcon frameIcon = MiscToolkit.loadImageIcon("/data/toolbar/utilities-system-monitor.png");
            dialog.setIconImage(frameIcon.getImage());
            dialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(final WindowEvent e) {
                    surf.stop();
                    isShown = false;
                    dialog.setVisible(false);
                }
            });
            // center on screen
            final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            final Dimension splashscreenSize = dialog.getSize();
            if (splashscreenSize.height > screenSize.height) {
                splashscreenSize.height = screenSize.height;
            }
            if (splashscreenSize.width > screenSize.width) {
                splashscreenSize.width = screenSize.width;
            }
            dialog.setLocation(
                (screenSize.width - splashscreenSize.width) / 2,
                (screenSize.height - splashscreenSize.height) / 2);

        }
        return dialog;
    }

    public void showDialog() {
        if( isShown ) {
            return;
        }
        surf.start();
        isShown = true;
        getDialog().setVisible(true);
    }

    public MemoryMonitor() {
        setLayout(new BorderLayout());
//        setBorder(new TitledBorder(new EtchedBorder(), "Memory Monitor"));
        add(surf = new Surface());
//        controls = new JPanel();
//        controls.setToolTipText("click to start/stop monitoring + memory saving");
//        Font font = new Font("serif", Font.PLAIN, 10);
//        JLabel label;
//        tf = new JTextField("1000");
//        tf.setPreferredSize(new Dimension(40,20));
//        controls.add(tf);
//        controls.add(label = new JLabel("ms"));
//
//        box = new JCheckBox("call gc()");
//        box.setPreferredSize(new Dimension(80,20));
//        box.setSelected(true);
//        controls.add(box);
//
//        controls.setPreferredSize(new Dimension(100, 80));
//        controls.setMaximumSize(new Dimension(100, 80));
//        label.setFont(font);
//        label.setForeground(Color.black);
//        addMouseListener(new MouseAdapter() {
//            public void mouseClicked(MouseEvent e)
//			{
//               removeAll();
//
//                if ((doControls = !doControls))
//				{
//                   surf.stop();
//                   add(controls);
//				}
//				else
//				{
//                   try {
//					long val = Long.parseLong(tf.getText().trim());
//
//					if (val >= 50)
//						surf.sleepAmount = val;
//                   } catch (Exception ex) {}
//
//                   surf.start();
//                   add(surf);
//				}
//				validate();
//				repaint();
//            }
//        });
    }

	public class Surface extends JPanel implements Runnable {

		private static final long serialVersionUID = 1L;

		public Thread thread;
        public long sleepAmount = 1000;
        private int w, h;
        private BufferedImage bimg;
        private Graphics2D big;
        private final Font font = new Font("Times New Roman", Font.PLAIN, 11);
        private final Runtime r = Runtime.getRuntime();
        private int columnInc;
        private int pts[];
        private int ptNum;
        private int ascent, descent;
//        private float freeMemory, totalMemory;
        private final Rectangle graphOutlineRect = new Rectangle();
        private final Rectangle2D mfRect = new Rectangle2D.Float();
        private final Rectangle2D muRect = new Rectangle2D.Float();
        private final Line2D graphLine = new Line2D.Float();
        private final Color graphColor = new Color(46, 139, 87);
        private final Color mfColor = new Color(0, 100, 0);
        private String usedStr;

		private int gc_counter = 0;

        public Surface() {
            setBackground(Color.black);
//            addMouseListener(new MouseAdapter() {
//                public void mouseClicked(MouseEvent e) {
//                    if (thread == null) start(); else stop();
//                }
//            });
        }

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(200, 100);
        }

        @Override
        public void paint(final Graphics g) {

            if (big == null) {
                return;
            }

            big.setBackground(getBackground());
            big.clearRect(0,0,w,h);

            final float freeMemory = r.freeMemory();
            final float totalMemory = r.totalMemory();
            final float maxMemory = r.maxMemory();

            // .. Draw allocated and used strings ..
            big.setColor(Color.green);
            big.drawString(String.valueOf((int) totalMemory/1024) + "K allocated",  4.0f, ascent+0.5f);
            big.drawString(String.valueOf((int) maxMemory/1024) + "K max",  110, ascent+0.5f);

            usedStr = String.valueOf(((int) (totalMemory - freeMemory))/1024) + "K used";
            big.drawString(usedStr, 4, h-descent);
            big.drawString(Thread.activeCount() + " threads", 110, h-descent);

            // Calculate remaining size
            final float ssH = ascent + descent;
            final float remainingHeight = (h - (ssH*2) - 0.5f);
            final float blockHeight = remainingHeight/10;
            final float blockWidth = 20.0f;
//            float remainingWidth = (float) (w - blockWidth - 10);

            // .. Memory Free ..
            big.setColor(mfColor);
            final int MemUsage = (int) ((freeMemory / totalMemory) * 10);
            int i = 0;
            for ( ; i < MemUsage ; i++) {
                mfRect.setRect(5,ssH+i*blockHeight,
                                blockWidth,blockHeight-1);
                big.fill(mfRect);
            }

            // .. Memory Used ..
            big.setColor(Color.green);
            for ( ; i < 10; i++)  {
                muRect.setRect(5,ssH+i*blockHeight,
                                blockWidth,blockHeight-1);
                big.fill(muRect);
            }

            // .. Draw History Graph ..
            big.setColor(graphColor);
            final int graphX = 30;
            final int graphY = (int) ssH;
            final int graphW = w - graphX - 5;
            final int graphH = (int) remainingHeight;
            graphOutlineRect.setRect(graphX, graphY, graphW, graphH);
            big.draw(graphOutlineRect);

            final int graphRow = graphH/10;

            // .. Draw row ..
            for (int j = graphY; j <= graphH+graphY; j += graphRow) {
                graphLine.setLine(graphX,j,graphX+graphW,j);
                big.draw(graphLine);
            }

            // .. Draw animated column movement ..
            final int graphColumn = graphW/15;

            if (columnInc == 0) {
                columnInc = graphColumn;
            }

            for (int j = graphX+columnInc; j < graphW+graphX; j+=graphColumn) {
                graphLine.setLine(j,graphY,j,graphY+graphH);
                big.draw(graphLine);
            }

            --columnInc;

            if (pts == null) {
                pts = new int[graphW];
                ptNum = 0;
            } else if (pts.length != graphW) {
                int tmp[] = null;
                if (ptNum < graphW) {
                    tmp = new int[ptNum];
                    System.arraycopy(pts, 0, tmp, 0, tmp.length);
                } else {
                    tmp = new int[graphW];
                    System.arraycopy(pts, pts.length-tmp.length, tmp, 0, tmp.length);
                    ptNum = tmp.length - 2;
                }
                pts = new int[graphW];
                System.arraycopy(tmp, 0, pts, 0, tmp.length);
            } else {
                big.setColor(Color.yellow);
                pts[ptNum] = (int)(graphY+graphH*(freeMemory/totalMemory));
                for (int j=graphX+graphW-ptNum, k=0;k < ptNum; k++, j++) {
                    if (k != 0) {
                        if (pts[k] != pts[k-1]) {
                            big.drawLine(j-1, pts[k-1], j, pts[k]);
                        } else {
                            big.fillRect(j, pts[k], 1, 1);
                        }
                    }
                }
                if (ptNum+2 == pts.length) {
                    // throw out oldest point
                    for (int j = 1;j < ptNum; j++) {
                        pts[j-1] = pts[j];
                    }
                    --ptNum;
                } else {
                    ptNum++;
                }
            }

            // each 5 seconds do a System.gc() if free memory is smaller than 2MB (compared to allocated memory)
			if (gc_counter > 4) {
				//if (!Common.isRunningProcess() && thread != null && box.isSelected() && freeMemory < 2048000L)
				if (thread != null /*&& box.isSelected()*/ && freeMemory < 2048000L) {
					big.setColor(Color.red);
					big.fillRect(84, h - descent - 6, 4, 4);

					System.gc();
				}

				gc_counter = 0;
			} else {
				gc_counter++;
            }

            g.drawImage(bimg, 0, 0, this);
        }

        public void start() {
            thread = new Thread(this);
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.setName("MemoryMonitor");
            thread.start();
        }

        public synchronized void stop() {
            thread = null;
            notify();
        }

        public void run() {

            final Thread me = Thread.currentThread();

//            while (thread == me && !isShowing() || getSize().width == 0) {
//                try {
//                    thread.sleep(500);
//                } catch (InterruptedException e) { return; }
//            }

            while (thread == me /*&& isShowing()*/) {
                final Dimension d = getSize();
                if (d.width != w || d.height != h) {
                    w = d.width;
                    h = d.height;
                    bimg = (BufferedImage) createImage(w, h);
                    big = bimg.createGraphics();
                    big.setFont(font);
                    final FontMetrics fm = big.getFontMetrics(font);
                    ascent = fm.getAscent();
                    descent = fm.getDescent();
                }

                paint(getGraphics()); // draw also when hidden

//                repaint(); // draw only when shown

                try {
                    Thread.sleep(sleepAmount);
                } catch (final InterruptedException e) { break; }
            }
            thread = null;
        }
    }
}
