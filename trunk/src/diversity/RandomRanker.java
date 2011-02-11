/** Algorithm for ranking documents similarity score with query
 *  (requires a similarity kernel)	
 * 
 * @author Scott Sanner (ssanner@gmail.com)
 */

package diversity;

import java.util.*;

import util.Permutation;

import diversity.kernel.Kernel;

public class RandomRanker extends ResultListSelector {
	
	// Constructor
	public RandomRanker(HashMap<String, String> docs) { 
		super(docs);
	}
	
	public void addDoc(String doc_name) {
		_docOrig.add(doc_name);
	}
	
	public void clearDocs() {
		_docRepr.clear();
		_docOrig.clear();
	}
	
	@Override
	public void initDocs() {
		
	}

	@Override
	public ArrayList<String> getResultList(String query, int list_size) {
		
		ArrayList<String> result_list = new ArrayList<String>();

		// Get the list of doc names and a random index permutation
		int num_docs = _docRepr.size();
		String[] docs = new String[num_docs];
		int[] permutation = Permutation.permute(num_docs);
		
		// Return the permutation
		for (int i = 0; i < list_size && i < num_docs; i++)
			result_list.add(docs[permutation[i]]);
		
		return result_list;		
	}
	
	@Override
	public String getDescription() {
		return "RandomRanker";
	}
}
