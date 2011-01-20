/** Some filters that may be useful for text processing.
 * 
 * @author Kishor Gawande (Kishor.Gawande@nicta.com.au)
 * @author Scott Sanner (ssanner@gmail.com) 
 */

package nlp.filters;

import java.util.*;

public class TextFilters {
	
	public static StopWordChecker _swc = new StopWordChecker();
	
	public static Map<String,String> filterNamedEntities(Map<String,String> m) {
		HashMap<String,String> m2 = new HashMap<String,String>();
		for (Map.Entry<String, String> me : m.entrySet()) 
			if (MeetsNamedEntityRequirements(me.getKey()))
				m2.put(me.getKey(), me.getValue());
		return m2;
	}
	
	public static boolean MeetsNamedEntityRequirements(String str) {
		if (str.trim().length() <= 2)
			return false;
		if (str.indexOf("\n") >= 0 || str.indexOf("\r") >= 0
				|| str.indexOf("\t") >= 0)
			return false;
		if (str.indexOf("<") >= 0 || str.indexOf(">") >= 0)
			return false;
		if (str.indexOf("www") >= 0)
			return false;
		if (str.indexOf(".") >= 0
				&& (str.indexOf("com") >= 0 || str.indexOf("au") >= 0
						|| str.indexOf("org") >= 0 || str.indexOf("net") >= 0))
			return false;
		if (str.indexOf("email") >= 0 || str.indexOf("Email") >= 0
				|| str.indexOf("EMAIL") >= 0 || str.indexOf("mailto") >= 0)
			return false;
		// Added by KDG ignore words
		if (_swc.isStopWord(str.toLowerCase()))
			return false;

		if (str.indexOf("Comment") >= 0)
			return false;
		if (str.indexOf("Comments") >= 0)
			return false;
		if (str.indexOf("Search Box") >= 0)
			return false;
		if (str.indexOf("Navigation") >= 0)
			return false;
		if (str.indexOf("Section") >= 0)
			return false;
		if (str.indexOf("Text Version") >= 0)
			return false;
		if (str.endsWith("."))
			return false;

		// check for lower case and punctuation characters
		// Todo - use regular expressions
		int lowerCnt = 0;
		for (int i = 0; i < str.length(); i++) {
			Character c = (str.charAt(i));
			if (!Character.isLetterOrDigit(c) && !Character.isSpaceChar(c)) {
				return false;
			}
			if (Character.isLowerCase(c)) {
				lowerCnt++;
			}
		}
		if (lowerCnt == str.length()) {
			return false;
		}

		// End KDG

		return true;
	}

	public static String FilterSentence(String str) {
		StringBuilder sb = new StringBuilder();
		for (int index = 0; index < str.length(); index++) {
			char c = str.charAt(index);
			if (c != '\r' && c != '\n' && c != '\t')
				sb.append(c);
			else if (sb.length() > 0 && sb.charAt(sb.length() - 1) != ' ')
				sb.append(' ');
		}
		return sb.toString().trim();
	}


}
