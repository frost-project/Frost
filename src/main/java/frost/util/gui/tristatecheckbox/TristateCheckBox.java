/*
  TristateCheckBox.java / Frost
  Copyright (C) 2007  Frost Project <jtcfrost.sourceforge.net>

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
package frost.util.gui.tristatecheckbox;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ActionMapUIResource;

/**
 * Derived from  The Java Specialists' Newsletter Issue 145 (2007-05-25)
 * by Dr. Heinz M. Kabutz
 */
public final class TristateCheckBox extends JCheckBox {

	private static final long serialVersionUID = 1L;

	// Listener on model changes to maintain correct focusability
    private final ChangeListener enableListener = new ChangeListener() {
        public void stateChanged(ChangeEvent e) {
            TristateCheckBox.this.setFocusable(getModel().isEnabled());
        }
    };

    public TristateCheckBox() {
        this(null, null, TristateState.INDETERMINATE);
    }

    public TristateCheckBox(final String text) {
        this(text, null, TristateState.INDETERMINATE);
    }

    public TristateCheckBox(final String text, final Icon icon, final TristateState initial) {
        super(text, icon);

        // Set default single model
        setModel(new TristateButtonModel(initial));

        // override action behaviour
        super.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                TristateCheckBox.this.iterateState();
            }
        });
        final ActionMap actions = new ActionMapUIResource();
		actions.put("pressed", new AbstractAction() {

			private static final long serialVersionUID = 1L;

			public void actionPerformed(final ActionEvent e) {
                TristateCheckBox.this.iterateState();
            }
        });
        actions.put("released", null);
        SwingUtilities.replaceUIActionMap(this, actions);
    }

    // Next two methods implement new API by delegation to model
    public void setIndeterminate() {
        getTristateModel().setIndeterminate();
    }

    public boolean isIndeterminate() {
        return getTristateModel().isIndeterminate();
    }

    public TristateState getState() {
        return getTristateModel().getState();
    }

    /**
     * @return  Boolean.TRUE if selected, Boolean.FALSE if not selected, null if dont care
     */
    public Boolean getBooleanState() {
        if( getState() == TristateState.INDETERMINATE ) {
            return null;
        } else if( getState() == TristateState.SELECTED ) {
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }

    // Overrides superclass method
    @Override
    public void setModel(final ButtonModel newModel) {
        super.setModel(newModel);

        // Listen for enable changes
        if( model instanceof TristateButtonModel ) {
            model.addChangeListener(enableListener);
        }
    }

    // Empty override of superclass method
    @Override
    public void addMouseListener(final MouseListener l) {
    }

    // Mostly delegates to model
    private void iterateState() {
        // Maybe do nothing at all?
        if( !getModel().isEnabled() ) {
            return;
        }

        grabFocus();

        // Iterate state
        getTristateModel().iterateState();

        // Fire ActionEvent
        int modifiers = 0;
        final AWTEvent currentEvent = EventQueue.getCurrentEvent();
        if( currentEvent instanceof InputEvent ) {
			modifiers = ((InputEvent) currentEvent).getModifiersEx();
        } else if( currentEvent instanceof ActionEvent ) {
            modifiers = ((ActionEvent) currentEvent).getModifiers();
        }
        fireActionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, getText(), System.currentTimeMillis(),
                modifiers));
    }

    //Convenience cast
    public TristateButtonModel getTristateModel() {
        return (TristateButtonModel) super.getModel();
    }
}
