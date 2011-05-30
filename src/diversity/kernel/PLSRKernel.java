/** Probabilistic Latent Set Relevance Kernel
 * 
 *  Derivation due to Shengbo Guo, Scott Sanner, Thore Graepel;
 *  preliminary version appeared as
 *   
 *    Probabilistic Latent Maximal Marginal Relevance, SIGIR 2010, 
 *    S. Guo and S. Sanner.
 * 
 * @author Scott Sanner (ssanner@gmail.com)
 */

package diversity.kernel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


import util.DocUtils;
import util.VectorUtils;

import ml.lda.LDAInterface;

public class PLSRKernel extends Kernel {

	public final static boolean DEBUG = false;
	public final static long    RAND_SEED = 123456;
	
	public int _nTopics = -1;
	public double _dAlpha = -1d;
	public double _dBeta  = -1d;
	public boolean _bSpherical = false;
	public double[] ONE = null;
	
	public Set<String> _mPrevInit = null;
	public Map<String, double[]> _doc2topicVector = null;
	public Map<Integer,Integer> _hmHashCode2DocID = null;
	public LDAInterface _lda = new LDAInterface();
	
	// Now automatically set: double alpha, double beta,
	public PLSRKernel(HashMap<String,String> docs, 
			int num_topics, boolean spherical) {
		super(docs);
		_nTopics = num_topics;
		_dAlpha  = 50.0d/num_topics;
		_dBeta   = 0.01d;
		_bSpherical = spherical;
	}
	
	@Override
	public void clear() {
		ONE = null;
		_mPrevInit = null;
		_doc2topicVector = null;
		_hmHashCode2DocID = null;
		_lda = new LDAInterface();
	}

	@Override
	public Object getNoncachedObjectRepresentation(String content) {
		
		double[] topic_vector = null;
		
		// See if we can extract the content representation directly from LDA
		int hash_code = content.hashCode();
		Integer m = _hmHashCode2DocID.get(hash_code);
		if (m != null && m >= 0 && m < _lda._M) {
			
			// Document m found
			topic_vector = new double[_lda._K];
			for (int k = 0; k < _lda._K; k++)
				topic_vector[k] = _lda._theta[m][k];
			
		} else {
			// Document m not found, use "folding-in" trick to estimate topic_vector
			topic_vector = _lda.getNewDocTopics(content);
		}
		
		if (DEBUG) {
			if (content.length() > 100)
				content = content.substring(0,100);
			System.out.println("Content: " + content);
			for (int k = 0; k < _nTopics; k++)
				System.out.println("- " + DocUtils.DF3.format(topic_vector[k]) + ": " + _lda.getTopicName(k));
		}
		return topic_vector;
	}

	@Override
	// Because we'll get better topic models from docs that were explicitly
	// added to the LDA model, we retain the hashcode of these documents so
	// we can retrieve their direct LDA topic vectors from the LDA Theta 
	// matrix.  There is a small chance of a hashcode collision, but we'll
	// accept this small chance of error in favor of not having to do
	// a String equality test on the entire document.
	public void init(Set<String> docs) {
		
		if (docs == _mPrevInit)
			return; // Already initialized
		_mPrevInit = docs;

		_hmHashCode2DocID = new HashMap<Integer, Integer>();
		for (String doc : docs) {
			String content = _docs.get(doc);
			int doc_id = _lda.addDocument(DocUtils.Tokenize(content));
			int hash_code = content.hashCode(); // TODO: This is horrible, should be name ref!
			_hmHashCode2DocID.put(hash_code, doc_id);
		}
		//_lda.infer(_nTopics, _dAlpha, _dBeta, RAND_SEED);
		_lda.infer(_nTopics, _dAlpha, _dBeta, RAND_SEED);
		//if (DEBUG)
		//	System.out.println("LEARNED TOPICS:\n===============\n" + _lda);
		
		// TODO: Replace cache contents since it changes with every run of LDA
		//       (different document set)... MMR should always just reference
		//       by doc name.
		for (String doc : docs) {
			String content = _docs.get(doc);
			_reprCache.put(doc, getNoncachedObjectRepresentation(content));
		}
		
		ONE = new double[_nTopics];
		for (int i = 0; i < _nTopics; i++)
			ONE[i] = 1d;
	}

	@Override
	public double sim(Object s1, Object s2) {
		double[] d1 = (double[])s1;
		double[] d2 = (double[])s2;
		if (_bSpherical)
			return VectorUtils.CosSim(d1, d2);
		else
			return VectorUtils.DotProduct(d1, d2);
	}

	@Override
	public double sim(Object s1, Object s2, Object q) {
		//System.out.println("WARNING: sim(s1,s2,q) should not be called for PLSRKernel.");
		//new Exception().printStackTrace(System.out);
		//System.exit(1);
		
		HashSet<Object> s2_set = new HashSet<Object>();
		s2_set.add(s2);
		return setSim(s1, s2_set, q);
	}

	@Override
	public boolean supportsSetSim() { return true; }
	
	@Override
	// This is a bit more complex... need all previous documents!
	// ... for each topic t': use 1 - (1 - (prod_{i=1}^{k-1} (1 - P(t' | s_i))
	//                                     ===================================
	//                                         P(T != t' | s_1,...,s_{k-1})
	//                                ========================================
	//                                      P(T == t' | s_1,...,s_{k-1})
	// ... second part becomes similarity topic coverage by document set
	// ... this method < P(T | s_k), P(T == t' | s_1,...,s_{k-1}) >_P(T | q)
	public Double setSim(Object s1, Set<Object> s2, Object q) { 

		if (s2.size() == 0)
			return 0d;

		// P(T | s_k)			
		double[] d1 = (double[])s1;
		double[] d2 = null;
		for (Object key : s2) {
			double[] di = (double[])key;
			di = VectorUtils.ElementWiseSubtract(ONE, di);
			if (d2 == null) 
				d2 = di;
			else
				d2 = VectorUtils.ElementWiseMultiply(d2, di);
		}
		// P(T == t' | s_1,...,s_{k-1})
		//System.out.println("ONE: " + VectorUtils.GetString(ONE));
		//System.out.println("d2:  " + VectorUtils.GetString(d2));
		d2 = VectorUtils.ElementWiseSubtract(ONE, d2);
		
		// P(T | q)
		double[] w  = (double[])q;
		//System.out.println("d1: " + VectorUtils.GetString(d1));
		//System.out.println("d2: " + VectorUtils.GetString(d2));
		//System.out.println("w:  " + VectorUtils.GetString(w) + "\n");
		
		// TODO: not technically correct, but trying out
		if (_bSpherical)
			return VectorUtils.CosSim(d1, d2);//VectorUtils.WeightedCosSim(d1, d2, w);
		else
			return VectorUtils.DotProduct(d1, d2); //VectorUtils.WeightedDotProduct(d1, d2, w);
	}

	@Override
	public String getObjectStringDescription(Object obj) {
		StringBuilder sb = new StringBuilder();
		double[] topic_vector = (double[])obj; 
		for (int k = 0; k < _nTopics; k++)
			sb.append("\n- " + DocUtils.DF3.format(topic_vector[k]) + ": " + _lda.getTopicName(k));
		return sb.toString();
	}

	@Override
	public String getKernelDescription() {
		return "PLSRKernel: #topics=" + _nTopics + ", alpha=" + _dAlpha + 
			", beta=" + _dBeta + ", spherical=" + _bSpherical;
	}

}
