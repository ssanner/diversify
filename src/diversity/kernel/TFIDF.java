/** Term Frequency - Inverse Document Frequency (TF-IDF) Kernel
 *   
 * @author Scott Sanner (ssanner@gmail.com)
 */

package diversity.kernel;

import java.util.HashMap;
import java.util.Map;


import util.DocUtils;
import util.VectorUtils;

public class TFIDF extends Kernel {

	public final static boolean DEBUG = false;
	
	public boolean _bReweightedSimilarity = false;
	public double  _dDefaultIDF = -1d;
	public Map<Object, Double> _hmKey2IDF = null;
	public Map<String, String> _mPrevInit = null;
	
	public TFIDF(boolean weighted_similarity) {
		_bReweightedSimilarity = weighted_similarity;
	}

	public void clear() {
		_dDefaultIDF = -1d;
		_hmKey2IDF = null;
		_mPrevInit = null;	
	}
	
	public void init(Map<String, String> docs) {
		if (docs == _mPrevInit)
			return; // Already initialized
		_mPrevInit = docs;
		_hmKey2IDF = new HashMap<Object,Double>();
		for (Map.Entry<String, String> e : docs.entrySet()) {
			String content = e.getValue();
			Map<Object,Double> features = DocUtils.ConvertToFeatureMap(content);
			features = VectorUtils.ConvertToBoolean(features);
			_hmKey2IDF = VectorUtils.Sum(_hmKey2IDF, features);
		}
		for (Object key : _hmKey2IDF.keySet()) {
			Double idf = (docs.size() + 1d)/ (_hmKey2IDF.get(key) + 1d);
			_hmKey2IDF.put(key, Math.log(idf));
		}
		_dDefaultIDF = Math.log((docs.size() + 1d) / 1d);
		if (DEBUG)
			System.out.println("IDF after log: " + _hmKey2IDF);
	}

	public Object getObjectRepresentation(String content) {
		Map<Object,Double> features = DocUtils.ConvertToFeatureMap(content);
		for (Object key : features.keySet())
			if (!_hmKey2IDF.containsKey(key))
				_hmKey2IDF.put(key, _dDefaultIDF);
		features = VectorUtils.ElementWiseMultiply(features, _hmKey2IDF);
		return features;
	}
	
	public double sim(Object o1, Object o2) {
		Map<Object, Double> s1 = (Map<Object, Double>)o1;
		Map<Object, Double> s2 = (Map<Object, Double>)o2;
		return VectorUtils.CosSim(s1, s2);
	}

	public double sim(Object o1, Object o2, Object ow) {

		Map<Object, Double> s1 = (Map<Object, Double>)o1;
		Map<Object, Double> s2 = (Map<Object, Double>)o2;

		if (_bReweightedSimilarity) { 			
			Map<Object, Double> w  = (Map<Object, Double>)ow;
			return VectorUtils.WeightedCosSim(s1, s2, w);
		} else
			return VectorUtils.CosSim(s1, s2);
	}

	@Override
	public String getObjectStringDescription(Object obj) {
		return obj.toString();
	}
	
	@Override
	public String getKernelDescription() {
		// TODO Auto-generated method stub
		return "TFIDF (reweighted_sim=" + _bReweightedSimilarity + ")";
	}

}
