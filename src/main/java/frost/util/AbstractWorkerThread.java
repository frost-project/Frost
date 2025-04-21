package frost.util;

import java.util.EventListener;
import java.util.EventObject;

import javax.swing.event.EventListenerList;

public abstract class AbstractWorkerThread  extends Thread {

	public class WorkerFinishedEvent extends EventObject {

		private static final long serialVersionUID = 1L;

		public WorkerFinishedEvent(Object source) {
            super(source);
        }
    }

    public interface WorkerFinishedListener extends EventListener {
        public void workerFinished(WorkerFinishedEvent event);
    }

    /**
     * A list of event listeners for this component.
     */
    protected EventListenerList listenerList = new EventListenerList();

    public AbstractWorkerThread() {
        super();
    }
    
    /**
     * Adds an <code>LanguageListener</code> to the Language.
     * @param listener the <code>LanguageListener</code> to be added
     */
    public void addWorkerFinishedListener(WorkerFinishedListener listener) {
        listenerList.add(WorkerFinishedListener.class, listener);
    }

    /**
     * Returns an array of all the <code>LanguageListener</code>s added
     * to this Language with addLanguageListener().
     *
     * @return all of the <code>LanguageListener</code>s added or an empty
     *         array if no listeners have been added
     */
    public WorkerFinishedListener[] getWorkerFinishedListeners() {
        return (listenerList.getListeners(WorkerFinishedListener.class));
    }

    /**
     * Removes an <code>LanguageListener</code> from the Language.
     * @param listener the <code>LanguageListener</code> to be removed
     */
    public void removeWorkerFinishedListener(WorkerFinishedListener listener) {
        listenerList.remove(WorkerFinishedListener.class, listener);
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.  The event instance
     * is lazily created using the <code>event</code>
     * parameter.
     *
     * @param event  the <code>LanguageEvent</code> object
     * @see EventListenerList
     */
    protected void fireWorkerFinished(WorkerFinishedEvent event) {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        WorkerFinishedEvent e = null;
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == WorkerFinishedListener.class) {
                // Lazily create the event:
                if (e == null) {
                    e = new WorkerFinishedEvent(this);
                }
                ((WorkerFinishedListener) listeners[i + 1]).workerFinished(e);
            }
        }
    }
}
