package medic.gateway;

import org.junit.*;

import static org.junit.Assert.*;

public class UtilsTest {
	@Test
	public void args_shouldNotModifyStringArrays() {
		// given
		String[] args = { "a", "b", "c" };

		// when
		String[] returned = Utils.args(args);

		// then
		assertSame(args, returned);
	}

	@Test
	public void args_shouldTurnObjectArraysToStrings() {
		// given
		Object[] args = { "a", 1, true };

		// when
		String[] returned = Utils.args(args);

		// then
		assertArrayEquals(returned, new String[] {
			"a", "1", "true"
		});
	}
}
