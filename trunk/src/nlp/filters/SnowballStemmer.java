/** Interface to Snowball Stemmer.
 *   
 * @author Scott Sanner (ssanner@gmail.com)
 */

package nlp.filters;

import org.tartarus.snowball.*;
import org.tartarus.snowball.ext.*;

public class SnowballStemmer {

    public int STEM_REPEAT_TIMES = 1; // How many times to repeat stemming

    public org.tartarus.snowball.SnowballStemmer _stemmer;
    
	public SnowballStemmer() {
		_stemmer = new englishStemmer();
	}
	
	public SnowballStemmer(int stem_repeat_times) {
		STEM_REPEAT_TIMES = stem_repeat_times;
	}

    public String stem(String s) {
	    _stemmer.setCurrent(s);
	    for (int i = STEM_REPEAT_TIMES; i != 0; i--) {
	    	_stemmer.stem();
	    }
	    return _stemmer.getCurrent();
    }

	public static void main(String args[]) {
		SnowballStemmer sbs = new SnowballStemmer();
		Test(sbs, "is");
		Test(sbs, "Scott");
		Test(sbs, "had");
		Test(sbs, "went");
		Test(sbs, "crawled");
		Test(sbs, "delegated");
		Test(sbs, "delegator");
		Test(sbs, "redelegating");
		Test(sbs, "exploration");
		Test(sbs, "buses");
	}
	
	public static void Test(SnowballStemmer sbs, String s) {
		System.out.println("'" + s + "' stem: '" + sbs.stem(s) + "'");
	}

}
