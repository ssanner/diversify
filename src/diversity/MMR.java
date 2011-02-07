/** MMR Shell Algorithm (requires similarity and diversity kernels)
 * 
 * @author Scott Sanner (ssanner@gmail.com)
 */

package diversity;

import java.util.*;

import diversity.kernel.Kernel;

public class MMR extends ResultListSelector {
	
	public double _dLambda;
	public Kernel _sim;
	public Kernel _div;
	
	// Constructor
	public MMR(double lambda, Kernel sim, Kernel div) { 
		_dLambda = lambda;
		_sim = sim;
		_div = div;
	}
	
	public void addDoc(String doc_name, String content) {
		_docOrig.put(doc_name, content);
	}
	
	public void clearDocs() {
		_docRepr.clear();
		_docOrig.clear();
		_sim.clear();
		_div.clear();
	}
	
	public void initDocs() {

		// The similarity kernel may need to do pre-processing (e.g., LDA training)
		_sim.init(_docOrig); // LDA should maintain keys for mapping later
		_div.init(_docOrig); // LDA should maintain keys for mapping later
		
		// Store local representation for later use with kernels
		// (should we let _sim handle everything and just interact with keys?)
		for (Map.Entry<String, String> e : _docOrig.entrySet()) {
			Object repr = _sim.getObjectRepresentation(e.getValue());
			_docRepr.put(e.getKey(), repr);
		}
	}
	
	// Compute the MMR of a sentence
	// MMR = argmax_s Sim(s,q) - max_s' Sim(s,s')
	public double computeMMRScore(String doc_name, Set<String> S, Object query) {
		
		Object features = _docRepr.get(doc_name);
		double sim_score = _sim.sim(features, query);
		double sim_other = -1d;
		
		if (_div.supportsSetSim()) {
			
			// Compute set similarity all in one
			Set<Object> other_feature_set = new HashSet<Object>();
			for (Object other : S)
				other_feature_set.add(_docRepr.get(other));
			sim_other = _div.setSim(features, other_feature_set, query);
			
		} else {
			
			// Take max over all other doc similarities
			for (String other : S) {
				Object other_features = _docRepr.get(other);
				double cur_sim_other = _div.sim(features, other_features, query);
				if (cur_sim_other > sim_other)
					sim_other = cur_sim_other;
			}
		}
		
		if (SHOW_DEBUG)
			System.out.println("- Sim: " + sim_score + ", Penalty: " + sim_other + " -- " + doc_name + ": " + features + "; query: " + query);

		return (1d - _dLambda)*sim_score - (_dLambda)*sim_other;
	}
	
	//try {
	//} catch (Exception e) {
	//	System.out.println("ERROR: diversity comparison to " + other + " / " + other_features);
	//	System.out.println(e.toString());
	//	e.printStackTrace(System.out);
	//	System.exit(1);
	//}

	@Override
	public ArrayList<String> getResultList(String query, int list_size) {
		
		ArrayList<String> result_list = new ArrayList<String>();

		// Intialize document set
		initDocs();
		
		// Get representation for query
		Object query_repr = _sim.getObjectRepresentation(query);

		// Initialize the set of all sentences minus
		// the selected sentences
		Set<String> R_MINUS_S = new HashSet<String>(_docRepr.keySet());

		// Initialize the selected set of sentences (empty)
		Set<String> S = new HashSet<String>();
		
		// Add one sentence at a time according to MMR metric
		for (int i = 0; i < list_size; i++) {
			
			if (SHOW_DEBUG)
				System.out.println("MMR Iteration: " + (i+1));
						
			// Find the best new sentence to add
			double cur_max_score = -1d;
			String cur_best_sent = null;
			for (String key : R_MINUS_S) {
				// MMR = argmax_s Sim(s,q) - max_s' Sim(s,s') 
				double score = computeMMRScore(key, S, query_repr);
				if (score > cur_max_score) {
					cur_max_score = score;
					cur_best_sent = key;
				}
			}
			
			// Add the new summary sentence to S, remove it
			// from R, and add it to the summary array being
			// returned
			S.add(cur_best_sent);
			R_MINUS_S.remove(cur_best_sent);
			
			if (SHOW_DEBUG)
				System.out.println("Chose: " + cur_max_score + ": " + cur_best_sent + "\n");
			result_list.add(cur_best_sent);
		}
			
		// Return the sentences (for now in arbitrary order)
		return result_list;
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return "MMR (lambda=" + _dLambda + ") -- sim: " + 
			_sim.getKernelDescription() + " -- div: " + _div.getKernelDescription();
	}
}
