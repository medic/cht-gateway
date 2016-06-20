package medic.gateway.alert;

import org.junit.*;

import static org.junit.Assert.*;

public class ComposeSmsActivityTest {
	@Test
	public void getRecipients_shouldReturnEmptyArrayForEmptyRecipientList() {
		// given
		String inputText = "";

		// when
		String[] actualRecipients = SmsComposerUtils.getRecipients(inputText);

		// then
		assertArrayEquals(new String[] {}, actualRecipients);
	}

	@Test
	public void getRecipients_shouldReturnEmptyArrayForRecipientListWithoutUsefulContent() {
		// given
		String inputText = " \n : ; , ";

		// when
		String[] actualRecipients = SmsComposerUtils.getRecipients(inputText);

		// then
		assertArrayEquals(new String[] {}, actualRecipients);
	}

	@Test
	public void getRecipients_shouldSplitByLineAndSomePunctuation() {
		// given
		String inputText = "+111,+222\n+333 +444;555:666\n\n777 ,  888;;999";

		// when
		String[] actualRecipients = SmsComposerUtils.getRecipients(inputText);

		// then
		assertArrayEquals(new String[] { "+111", "+222", "+333", "+444", "555", "666", "777", "888", "999" },
				actualRecipients);
	}
}
