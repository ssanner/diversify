/** MMR Shell Algorithm (requires similarity and diversity kernels)
 * 
 * @author Scott Sanner (ssanner@gmail.com)
 */

package diversity;

import java.util.*;

import util.Pair;
import util.Triple;

import diversity.kernel.Kernel;

// TODO: Refactor addDoc so that it takes a doc repository and a doc name...
//       doc repository must track representations for kernels and provide
//       these on demand.
public class MMR extends ResultListSelector {
	
	public double _dLambda;
	public Kernel _sim;
	public Kernel _div;
	public HashMap<Pair,Double>   _simCache;
	public HashMap<Triple,Double> _divCache;
	
	// Constructor
	public MMR(HashMap<String, String> docs, double lambda, Kernel sim, Kernel div) { 
		super(docs);
		_dLambda = lambda;
		_sim = sim;
		_div = div;
		_simCache = new HashMap<Pair,Double>();
		_divCache = new HashMap<Triple,Double>();
	}
	
	public void addDoc(String doc_name) {
		_docOrig.add(doc_name);
	}
	
	public void clearDocs() {
		_docRepr.clear();
		_docRepr2.clear();
		_docOrig.clear();
		_sim.clear();
		_div.clear();
		// No need to clear sim and div caches, these are local and conditioned
		// on query and we expect that the sim and div kernels will not change.
	}
	
	public void initDocs() {

		// The similarity kernel may need to do pre-processing (e.g., LDA training)
		_sim.init(_docOrig); // LDA should maintain keys for mapping later
		_div.init(_docOrig); // LDA should maintain keys for mapping later
		
		// Store local representation for later use with kernels
		// (should we let _sim handle everything and just interact with keys?)
		for (String doc : _docOrig) {
			Object repr = _sim.getObjectRepresentation(doc);
			_docRepr.put(doc, repr);
			if (_sim != _div) {
				Object repr2 = _div.getObjectRepresentation(doc);
				_docRepr2.put(doc, repr2);			
			}
		}
	}
	
	// Compute the MMR of a sentence
	// MMR = argmax_s Sim(s,q) - max_s' Sim(s,s')
	public double computeMMRScore(String doc_name, Set<String> S, 
			Object query_sim, Object query_div) {

		String query_key = query_sim.toString();

		Object features = _docRepr.get(doc_name);
		Object features2;
		Double sim_score = null;
		Pair sim_key = new Pair(doc_name, query_key);
		if ((sim_score = _simCache.get(sim_key)) == null) {
			sim_score = _sim.sim(features, query_sim);
			_simCache.put(sim_key, sim_score);
		}
		features2 = (_sim != _div ? _docRepr2 : _docRepr).get(doc_name);

		Double sim_other = null;
		
		if (_div.supportsSetSim()) {
			
			// Compute set similarity all in one
			Set<Object> other_feature_set = new HashSet<Object>();
			for (Object other : S)
				other_feature_set.add((_sim != _div ? _docRepr2 : _docRepr).get(other));
			
			Triple div_key = new Triple(doc_name, other_feature_set.hashCode(), query_key);
			if ((sim_other = _divCache.get(div_key)) == null) {
				sim_other = _div.setSim(features2, other_feature_set, query_div);
				_divCache.put(div_key, sim_other);
			}

		} else {
			
			// Take max over all other doc similarities
			sim_other = -1d;
			for (String other : S) {
				Object other_features = (_sim != _div ? _docRepr2 : _docRepr).get(other);
				Double cur_sim_other = null;
				Triple div_key = new Triple(doc_name, other, query_key);
				if ((cur_sim_other = _divCache.get(div_key)) == null) {
					cur_sim_other = _div.sim(features2, other_features, query_div);
					_divCache.put(div_key, cur_sim_other);
				}
				
				if (cur_sim_other > sim_other)
					sim_other = cur_sim_other;
			}
		}
		
		if (SHOW_DEBUG)
			System.out.println("- Sim: " + sim_score + ", Penalty: " + sim_other + " -- " + doc_name + ": " + features + "; query: " + query_sim);

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
		Object query_repr_sim = _sim.getNoncachedObjectRepresentation(query);
		Object query_repr_div = _div.getNoncachedObjectRepresentation(query);

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
				double score = computeMMRScore(key, S, query_repr_sim, query_repr_div);
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
