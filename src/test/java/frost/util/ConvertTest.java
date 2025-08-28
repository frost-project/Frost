package frost.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

public class ConvertTest {

	private static final String INVALID_VALUE = "no value";

	@Test
	public void convertInteger() {
		assertEquals(123, Convert.toInteger("123"));
		assertEquals(123, Convert.toInteger(" 123 "));

		assertEquals("123", Convert.toString(123));

		assertThrows(IllegalArgumentException.class, () -> {
			Convert.toInteger(INVALID_VALUE);
		});
	}

	@Test
	public void convertStringList() {
		assertEquals(Arrays.asList("12", " 34 ", "aB"), Convert.toStringList("12; 34 ;aB"));

		assertEquals("12; 34 ;aB", Convert.toString(Arrays.asList("12", " 34 ", "aB")));

		// Special handling for string-list
		assertEquals(Arrays.asList(INVALID_VALUE), Convert.toStringList(INVALID_VALUE));
	}
}
