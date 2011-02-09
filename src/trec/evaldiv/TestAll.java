/** Run ClueWeb and TREC 6-8 Interactive Track Tests
 *   
 * @author Scott Sanner (ssanner@gmail.com)
 */

package trec.evaldiv;

public class TestAll {

	public static void main(String[] args) throws Exception {
		
		Evaluator.USE_ALL_DOCS = false;
		//TestTREC.main(null);
		//TestCLUE.main(null);
		
		Evaluator.USE_ALL_DOCS = true;
		TestTREC.main(null);
		//TestCLUE.main(null);
	}

}
