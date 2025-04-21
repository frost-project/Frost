/*
 * ===========================================================================
 * Copyright 2004 by Volker H. Simonis. All rights reserved. GPL v2 license.
 * ===========================================================================
 */
package frost.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JViewport;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.ViewportLayout;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ComponentUI;

public class ScrollableBarUI extends ComponentUI
                             implements SwingConstants,
                                        MouseListener,
                                        ChangeListener,
                                        PropertyChangeListener {

  private ScrollableBar sb;
  private JViewport scroll;
  private JButton scrollF, scrollB;
  private boolean pressed = false;
  private int inc;

  public static ComponentUI createUI(final JComponent c) {
    return new ScrollableBarUI();
  }

@Override
public void installUI(final JComponent c) {

    sb = (ScrollableBar)c;

    inc = sb.getIncrement();
    final boolean small = sb.isSmallArrows();

    // Create the Buttons
    final Object o = UIManager.get( "ScrollBar.width" );
    final int sbSize;
    if (o != null && o instanceof Number) {
        sbSize = ((Number)o).intValue();
    } else {
        sbSize = 24;
    }

    scrollB = createButton(sb.isHorizontal()?WEST:NORTH, sbSize, small);
    scrollB.setVisible(false);
    scrollB.addMouseListener(this);

    scrollF = createButton(sb.isHorizontal()?EAST:SOUTH, sbSize, small);
    scrollF.setVisible(false);
    scrollF.addMouseListener(this);

    final int axis = sb.isHorizontal()?BoxLayout.X_AXIS:BoxLayout.Y_AXIS;
    sb.setLayout(new BoxLayout(sb, axis));

	scroll = new JViewport() {

		private static final long serialVersionUID = 1L;

		// Create a customized layout manager
        @Override
        protected LayoutManager createLayoutManager() {
			return new ViewportLayout() {

				private static final long serialVersionUID = 1L;

				@Override
            public Dimension minimumLayoutSize(final Container parent) {
                final Component view = ((JViewport)parent).getView();
                if (view == null) {
                  return new Dimension(4, 4);
                }
                else {
                  final Dimension d = view.getPreferredSize();
                  if (sb.isHorizontal()) {
                    return new Dimension(4, (int)d.getHeight());
                  }
                  else {
                    return new Dimension((int)d.getWidth(), 4);
                  }
                }
              }
            };
        }
      };

    final Component box = sb.getComponent();
    scroll.setView(box);

    sb.add(scrollB);
    sb.add(scroll);
    sb.add(scrollF);

    // Install the change listeners
    scroll.addChangeListener(this);
    sb.addPropertyChangeListener(this);
  }

  @Override
public void uninstallUI(final JComponent c) {
    sb.remove(scrollB);
    sb.remove(scroll);
    sb.remove(scrollF);
    scroll.setViewPosition(new Point(0,0));
    // Remove the change listeners
    scroll.removeChangeListener(this);
    sb.removePropertyChangeListener(this);
  }

  protected JButton createButton(final int direction, final int width, final boolean small) {
    final JButton button = new ScrollButton(direction, width, small);
    button.setAlignmentX(0.5f);
    button.setAlignmentY(0.5f);
    return button;
  }

  // PropertyChangeListner methods.

  public void propertyChange(final PropertyChangeEvent evt) {
    if ("increment".equals(evt.getPropertyName())) {
      inc = ((Integer)evt.getNewValue()).intValue();
    }
    else if ("smallArrows".equals(evt.getPropertyName())) {
      final boolean small = ((Boolean)evt.getNewValue()).booleanValue();
      ((ScrollButton)scrollB).setSmallArrows(small);
      ((ScrollButton)scrollF).setSmallArrows(small);
    }
    else if ("component".equals(evt.getPropertyName())) {
      scroll.setView((Component)evt.getNewValue());
    }
  }

  // ChangeListner methods.

  public void stateChanged(final ChangeEvent e) {
    final boolean cond = sb.isHorizontal() ?
      sb.getWidth() < scroll.getViewSize().width:
      sb.getHeight() < scroll.getViewSize().height;
    if (cond) {
      scrollB.setVisible(true);
      scrollF.setVisible(true);
    }
    else {
      scrollB.setVisible(false);
      scrollF.setVisible(false);
      sb.doLayout();
    }
  }

  // MouseListener methods.

  public void mouseClicked(final MouseEvent e) {
  }

  public void mouseEntered(final MouseEvent e) {
  }

  public void mouseExited(final MouseEvent e) {
    pressed = false;
  }

  public void mouseReleased(final MouseEvent e) {
    pressed = false;
  }

  public void mousePressed(final MouseEvent e) {
    pressed = true;
    final Object o = e.getSource();
    final Thread scroller = new Thread(new Runnable() {
      public void run() {
        int accl = 500;
        while (pressed) {
          final Point p = scroll.getViewPosition();
          // ... "Compute new view position"
          if (sb.isHorizontal()) {
            if (o == scrollB) {
              p.x -= inc;
              if (p.x < 0) {
                p.x = 0;
                scroll.setViewPosition(p);
                return;
              }
            }
            else {
              if (scroll.getViewSize().width - p.x -
                  scroll.getExtentSize().width > inc) {
                p.x += inc;
              }
              else {
                p.x = scroll.getViewSize().width -
                  scroll.getExtentSize().width;
                scroll.setViewPosition(p);
                return;
              }
            }
          }
          else {
            if (o == scrollB) {
              p.y -= inc;
              if (p.y < 0) {
                p.y = 0;
                scroll.setViewPosition(p);
                return;
              }
            }
            else {
              if (scroll.getViewSize().height - p.y -
                  scroll.getExtentSize().height > inc) {
                p.y += inc;
              }
              else {
                p.y = scroll.getViewSize().height -
                  scroll.getExtentSize().height;
                scroll.setViewPosition(p);
                return;
              }
            }
          }
          // ...
          scroll.setViewPosition(p);
          try {
            Thread.sleep(accl);
            if (accl <= 10) {
                accl = 10;
            } else {
                accl /= 2;
            }
          } catch (final InterruptedException ie) {}
        }
      }
    });
    scroller.start();
  }
}
