package medic.gateway;

import org.junit.*;

import static org.junit.Assert.*;

public class UtilsTest {
	@Test
	public void args_shouldNotModifyStringArrays() {
		// given
		String[] SOME_ARGS = { "a", "b", "c" };

		// when
		String[] returned = Utils.args(SOME_ARGS);

		// then
		assertTrue(SOME_ARGS == returned);
	}

	@Test
	public void args_shouldTurnObjectArraysToStrings() {
		// given
		Object[] SOME_ARGS = { "a", 1, true };

		// when
		String[] returned = Utils.args(SOME_ARGS);

		// then
		assertArrayEquals(returned, new String[] {
			"a", "1", "true"
		});
	}
}
