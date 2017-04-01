package medic.gateway.alert;

import java.util.Random;

public final class DebugUtils {
	private static final String[] RANDOM_WORDS = {
		"I", "a", "action", "admirable", "air", "all", "an", "and", "angel", "animals", "appears", "apprehension", "beauty", "brave", "but", "canopy", "congregation", "custom", "delights", "disposition", "dust", "earth", "excellent", "exercises", "express", "faculty", "fire", "firmament", "forgone", "form", "foul", "frame", "fretted", "god", "goes", "golden", "goodly", "have", "heavily", "how", "in", "indeed", "infinite", "is", "it", "know", "late", "like", "look", "lost", "majestical", "man", "me", "mirth", "most", "moving", "my", "neither", "no", "noble", "nor", "not", "of", "other", "overhanging", "paragon", "pestilential", "piece", "promontory", "quintessence", "reason", "roof", "seems", "so", "sterile", "than", "that", "the", "thing", "this", "to", "vapours", "what", "wherefore", "why", "with", "woman", "work", "world", "yet", "you",
	};

	private DebugUtils() {}

	public static String randomPhoneNumber() {
		Random r = new Random();
		StringBuilder bob = new StringBuilder();
		bob.append('+');
		for(int i=0; i<10; ++i) bob.append(r.nextInt(10));
		return bob.toString();
	}

	public static String randomSmsContent() {
		Random r = new Random();
		int wordCount = r.nextInt(20) + 1;
		StringBuilder bob = new StringBuilder();
		for(int i=0; i<wordCount; ++i) {
			bob.append(' ');
			bob.append(RANDOM_WORDS[r.nextInt(RANDOM_WORDS.length)]);
		}
		return bob.substring(1);
	}
}
