/*
 ImmutableAreasDocument.java / Frost
 Copyright (C) 2003  Frost Project <jtcfrost.sourceforge.net>

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
package frost.util.gui;

import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.PlainDocument;
import javax.swing.text.Position;

public class ImmutableAreasDocument extends PlainDocument {

	private static final long serialVersionUID = 1L;

	private final ArrayList<ImmutableArea> immutableAreas = new ArrayList<ImmutableArea>();
	private final MessageFilter filter = new MessageFilter();

	private class MessageFilter extends DocumentFilter {

		public static final int CLIPPING_OUTSIDE = 0;
		public static final int CLIPPING_INSIDE = 1;
		public static final int CLIPPING_BOTH_SIDES = 2;
		public static final int CLIPPING_LEFT = 3;
		public static final int CLIPPING_RIGHT = 4;

		public MessageFilter() {
			super();
		}

		@Override
        public void replace(final FilterBypass fb, final int offset, final int length, final String text, final AttributeSet attrs)
			throws BadLocationException {

			boolean allowReplace = true;
			boolean noAreasEnabled = true;
			final Iterator<ImmutableArea> areas = immutableAreas.iterator();
			while (areas.hasNext() && allowReplace) {
				final ImmutableArea area = areas.next();
				if (area.isEnabled()) {
					int clippingValue;
					if (length == 0) {
						clippingValue = clip(area, offset, length, false);
					} else {
						clippingValue = clip(area, offset, length, true);
					}
					final int endOffset = offset + length;
					final Position position = createPosition(offset);
					final Position endPosition = createPosition(endOffset);

					if (clippingValue != CLIPPING_OUTSIDE) {
						noAreasEnabled = false;
					}
					if ((clippingValue == CLIPPING_LEFT) || (clippingValue == CLIPPING_BOTH_SIDES)) {
						final int newStringLength = text.length() - (endOffset - area.getStartPos());
						final int newLength = area.getStartPos() - offset;
						if (newStringLength >= newLength) {
							fb.replace(offset, newLength, substring(text, 0, newStringLength), attrs);
						} else {
							fb.replace(offset, newLength, substring(text, 0, newLength), attrs);
						}

					}
					if ((clippingValue == CLIPPING_RIGHT) || (clippingValue == CLIPPING_BOTH_SIDES)){
						final int newStart = area.getEndPos() - position.getOffset();
						final int newLength = endPosition.getOffset() - area.getEndPos();
						if (newStart >= text.length()) {
							fb.replace(area.getEndPos(), newLength, "", attrs);
						} else {
							fb.replace(area.getEndPos(), newLength, text.substring(newStart), attrs);
						}
					}
					allowReplace = false;
				}
			}
			if (noAreasEnabled) {
				super.replace(fb, offset, length, text, attrs);
			}
		}

		@Override
        public void insertString(final FilterBypass fb, final int offset, final String string, final AttributeSet attr)
			throws BadLocationException {

			boolean noAreasEnabled = true;
			boolean allowInsert = true;
			final Iterator<ImmutableArea> areas = immutableAreas.iterator();
			while (areas.hasNext() && allowInsert) {
				final ImmutableArea area = areas.next();
				if (area.isEnabled()) {
					final int clippingValue = clip(area, offset, string.length(), false);
					if (clippingValue != CLIPPING_OUTSIDE) {
						noAreasEnabled = false;
						allowInsert = false;
					}
				}
			}
			if (noAreasEnabled) {
				super.insertString(fb, offset, string, attr);
			}
		}

		@Override
        public void remove(final FilterBypass fb, final int offset, final int length) throws BadLocationException {

			boolean allowRemove = true;
			boolean noAreasEnabled = true;
			final Iterator<ImmutableArea> areas = immutableAreas.iterator();
			while (areas.hasNext() && allowRemove) {
				final ImmutableArea area = areas.next();
				if (area.isEnabled()) {
					final int clippingValue = clip(area, offset, length, true);
					final int endOffset = offset + length;
//					final Position position = createPosition(offset);
					final Position endPosition = createPosition(endOffset);

					if (clippingValue != CLIPPING_OUTSIDE) {
						noAreasEnabled = false;
					}
					if ((clippingValue == CLIPPING_LEFT) || (clippingValue == CLIPPING_BOTH_SIDES))  {
						final int newLength = area.getStartPos() - offset;
						fb.remove(offset, newLength);
					}
					if ((clippingValue == CLIPPING_RIGHT) || (clippingValue == CLIPPING_BOTH_SIDES)) {
						final int newLength = endPosition.getOffset() - area.getEndPos();
						fb.remove(area.getEndPos(), newLength);
					}
					allowRemove = false;
				}
			}
			if (noAreasEnabled) {
				super.remove(fb, offset, length);
			}
		}

		private int clip(final ImmutableArea area, final int offset, final int length, final boolean isRemoving) {
			final int endOffset = offset + length;
			boolean offsetOutsideLeft = false;
			boolean offsetOutsideRight = false;
			boolean endOffsetOutsideLeft = false;
			boolean endOffsetOutsideRight = false;

			if (isRemoving) {
				if (offset < area.getStartPos()) {
					offsetOutsideLeft = true;
				} else if (offset >= area.getEndPos()) {
					offsetOutsideRight = true;
				}
				if (endOffset < area.getStartPos()) {
					endOffsetOutsideLeft = true;
				} else if (endOffset >= area.getEndPos()) {
					endOffsetOutsideRight = true;
				}
			} else {
				if (offset <= area.getStartPos()) {
					offsetOutsideLeft = true;
				} else if (offset >= area.getEndPos()) {
					offsetOutsideRight = true;
				}
				if (endOffset <= area.getStartPos()) {
					endOffsetOutsideLeft = true;
				} else if (endOffset >= area.getEndPos()) {
					endOffsetOutsideRight = true;
				}
			}

			if (offsetOutsideLeft) {
				if (endOffsetOutsideLeft) {
					return CLIPPING_OUTSIDE;
				}
				if (endOffsetOutsideRight) {
					return CLIPPING_BOTH_SIDES;
				}
				return CLIPPING_LEFT;
			}
			if (offsetOutsideRight) {
				return CLIPPING_OUTSIDE;
			}
			if (endOffsetOutsideRight) {
				return CLIPPING_RIGHT;
			}
			return CLIPPING_INSIDE;
		}

		private String substring(final String text, int beginIndex, int endIndex) {
			if (beginIndex < 0) {
				beginIndex = 0;
			}
			if (endIndex > text.length()) {
				endIndex = text.length();
			}
			return text.substring(beginIndex, endIndex);
		}
	}

	public ImmutableAreasDocument() {
		super();
		setDocumentFilter(filter);
	}

	public void addImmutableArea(final ImmutableArea newArea) {
		immutableAreas.add(newArea);
	}

	public void removeImmutableArea(final ImmutableArea area) {
		immutableAreas.remove(area);
	}

	public ImmutableAreasDocument(final Content c) {
		super(c);
		setDocumentFilter(filter);
	}
}
