/*
  DateFunTest.java / Frost
  Copyright (C) 2023  Frost Project <jtcfrost.sourceforge.net>

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
package frost.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import frost.junit.InitExtension;

@ExtendWith(InitExtension.class)
public class DateFunTest {

	@Test
	public void testDateTime() {
		// Original object
		OffsetDateTime dateTimeOrg = OffsetDateTime.of(2006, 1, 2, 3, 4, 5, 6000000, DateFun.getTimeZone());
		assertEquals("2006-01-02T03:04:05.006Z", dateTimeOrg.toString());
		assertEquals(1136171045006L, DateFun.toMilli(dateTimeOrg));

		// Copy using original object
		OffsetDateTime dateTimeCopyObj = OffsetDateTime.from(dateTimeOrg);
		assertEquals(dateTimeOrg.toString(), dateTimeCopyObj.toString());
		assertEquals(DateFun.toMilli(dateTimeOrg), DateFun.toMilli(dateTimeCopyObj));

		// Copy using timestamp + timezone
		OffsetDateTime dateTimeCopyMillis = DateFun.toOffsetDateTime(DateFun.toMilli(dateTimeOrg),
				DateFun.getTimeZone());
		assertEquals(dateTimeOrg.toString(), dateTimeCopyMillis.toString());
		assertEquals(DateFun.toMilli(dateTimeOrg), DateFun.toMilli(dateTimeCopyMillis));

		// Plus days
		OffsetDateTime dateTimePlusDays = dateTimeOrg.plusDays(35);
		assertEquals("2006-02-06T03:04:05.006Z", dateTimePlusDays.toString());
		assertEquals(DateFun.toMilli(dateTimeOrg) + Duration.ofDays(35).toMillis(), DateFun.toMilli(dateTimePlusDays));

		// Minus days
		OffsetDateTime dateTimeMinusDays = dateTimePlusDays.minusDays(35);
		assertEquals(dateTimeOrg.toString(), dateTimeMinusDays.toString());
		assertEquals(DateFun.toMilli(dateTimePlusDays) - Duration.ofDays(35).toMillis(),
				DateFun.toMilli(dateTimeMinusDays));

		// Without time
		OffsetDateTime dateTimeWithoutTime = DateFun.toStartOfDay(dateTimeOrg);
		assertEquals("2006-01-02T00:00Z", dateTimeWithoutTime.toString());
		assertEquals(DateFun.toMilli(dateTimeOrg) - Duration.ofHours(3).toMillis() - Duration.ofMinutes(4).toMillis()
				- Duration.ofSeconds(5).toMillis() - 6, DateFun.toMilli(dateTimeWithoutTime));

		// Format
		assertEquals("2006.1.2", DateFun.FORMAT_DATE.format(dateTimeOrg));
		assertEquals("2006.01.02", DateFun.FORMAT_DATE_EXT.format(dateTimeOrg));
		assertEquals("02.01.2006", DateFun.FORMAT_DATE_VISIBLE.format(dateTimeOrg));
		assertEquals("03:04:05", DateFun.FORMAT_TIME_PLAIN.format(dateTimeOrg));
		assertEquals("3:4:5GMT", DateFun.FORMAT_TIME.format(dateTimeOrg));
		assertEquals("03:04:05GMT", DateFun.FORMAT_TIME_EXT.format(dateTimeOrg));
		assertEquals("03:04:05 GMT", DateFun.FORMAT_TIME_VISIBLE.format(dateTimeOrg));
		assertEquals("02.01.2006 03:04:05 GMT", DateFun.FORMAT_DATE_TIME_VISIBLE.format(dateTimeOrg));

		// Parse date
		OffsetDateTime dateTimeParsedDate = DateFun.parseDate("2006.1.2", DateFun.FORMAT_DATE, DateFun.getTimeZone());
		assertEquals("2006-01-02T00:00Z", dateTimeParsedDate.toString());
		assertEquals(DateFun.toMilli(dateTimeWithoutTime), DateFun.toMilli(dateTimeParsedDate));

		// Parse time
		OffsetDateTime dateTimeParsedTime = DateFun.parseTime("03:04:05GMT", DateFun.FORMAT_TIME,
				DateFun.getTimeZone());
		assertEquals("1970-01-01T03:04:05Z", dateTimeParsedTime.toString());
		assertEquals(DateFun.toMilli(dateTimeOrg) - DateFun.toMilli(dateTimeWithoutTime) - 6,
				DateFun.toMilli(dateTimeParsedTime));
	}
}
