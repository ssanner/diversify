/** Tokenizer for specific Locales, builds on Java packages for tokenization.
 *   
 *  Note: this tokenizer respects word units such as "John's".  When using
 *        kernels, it appears that more aggressive tokenization works better,
 *        but this assertion needs to be empirically validated.
 *   
 * @author Scott Sanner (ssanner@gmail.com)
 */

package nlp.filters;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Locale;

public class SimpleTokenizer {

	BreakIterator _iter = null;
	
	public SimpleTokenizer() {
		Locale currentLocale = new Locale ("en","US");
		_iter = BreakIterator.getWordInstance(currentLocale);
	}
	
	public SimpleTokenizer(Locale l) {
		_iter = BreakIterator.getWordInstance(l);
	}
	
	public ArrayList<String> extractTokens(String text, boolean lowercase) {
		
		ArrayList<String> tokens = new ArrayList<String>();
		
		_iter.setText(text);
		int start = _iter.first();
		int end = _iter.next();

		while (end != BreakIterator.DONE) {
			String word = text.substring(start, end);
			if (Character.isLetterOrDigit(word.charAt(0))) {
				if (lowercase)
					word = word.toLowerCase();
				tokens.add(word);
			}
			start = end;
			end = _iter.next();
		}
		
		return tokens;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		String test = "This sentence is to test whether Mr. Sushi, a well-known restaurant, can be distiguished from a person's name.";
		System.out.println("Sentence: " + test);

		SimpleTokenizer st = new SimpleTokenizer();
		ArrayList<String> tokens = st.extractTokens(test, false); 
		System.out.println("Tokens:");
		for (String token : tokens) {
			System.out.println("'" + token + "'");
		}
	}

}
