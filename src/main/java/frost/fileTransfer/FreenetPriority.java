package frost.fileTransfer;

import frost.util.gui.translation.Language;

public enum FreenetPriority {
	MAXIMUM(0),
	VERY_HIGH(1),
	HIGH(2),
	MEDIUM(3),
	LOW(4),
	VERY_LOW(5),
	PAUSE(6);
	
	final int priority;
	
	FreenetPriority(final int priority) {
		this.priority = priority;
	}
	
	public String toString() {
		return getName();
	}
	
	public int getNumber() {
		return priority;
	}
	
	public String getName() {
		return Language.getInstance().getString("Common.priority.priority" + priority);
	}

	public static String getName(final int priority) {
		return getPriority(priority).getName();
	}

	static public FreenetPriority getPriority(final int priority) {
		for (FreenetPriority value : values()) {
			if (value.getNumber() == priority) {
				return value;
			}
		}
		throw new IllegalArgumentException("No such priority");
	}
}
