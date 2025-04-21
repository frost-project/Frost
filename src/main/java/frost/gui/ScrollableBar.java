/*
 * ===========================================================================
 * Copyright 2004 by Volker H. Simonis. All rights reserved. GPL v2 license.
 * ===========================================================================
 */
package frost.gui;

import java.awt.Component;

import javax.swing.JComponent;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

public class ScrollableBar extends JComponent implements SwingConstants {

	private static final long serialVersionUID = 1L;

	static {
    UIManager.put("ScrollableBarUI", 
                  "frost.gui.ScrollableBarUI");
  }
 
  public ScrollableBar(Component comp) {
    this(comp, HORIZONTAL);
  }

  public ScrollableBar(Component comp, int orientation) {
    this.comp = comp;
    if (orientation == HORIZONTAL) {
      horizontal = true;
    }
    else {
      horizontal = false;
    }
    small = true; // Arrow size on scroll button.
    inc = 10;      // Scroll width in pixels.
    updateUI();
  }

  @Override
public String getUIClassID() {
    return "ScrollableBarUI";
  }

  @Override
public void updateUI() {
    setUI(UIManager.getUI(this));
    invalidate();
  }

  public Component getComponent() {
    return comp;
  }

  public void setComponent(Component comp) {
    if (this.comp != comp) {
      Component old = this.comp;
      this.comp = comp;
      firePropertyChange("component", old, comp);
    }
  }

  public int getIncrement() {
    return inc;
  }

  public void setIncrement(int inc) {
    if (inc > 0 && inc != this.inc) {
      int old = this.inc;
      this.inc = inc;
      firePropertyChange("increment", old, inc);
    }
  }

  public boolean isHorizontal() {
    return horizontal;
  }
  
  public boolean isSmallArrows() {
    return small;
  }

  public void setSmallArrows(boolean small) {
    if (small != this.small) {
      boolean old = this.small;
      this.small = small;
      firePropertyChange("smallArrows", old, small);
    }
  }

  private Component comp;
  private boolean horizontal, small;
  private int inc;
}
