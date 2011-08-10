/** Term Frequency - Inverse Document Frequency (TF-IDF) Kernel
 *   
 * @author Scott Sanner (ssanner@gmail.com)
 */

package diversity.kernel;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


import util.DocUtils;
import util.VectorUtils;

public class PLSRKernelTFIDF extends Kernel {

	public final static boolean DEBUG = false;
	
	public double  _dDefaultIDF = -1d;
	public Map<Object, Double> _hmKey2IDF = null;
	public Set<String>         _mPrevInit = null;
	
	public PLSRKernelTFIDF(HashMap<String,String> docs) {
		super(docs);
	}

	// Kernels are reused by different MMR invocations
	// TODO: Is it OK to continue caching this information?
	public void clear() {
		_dDefaultIDF = -1d;
		_hmKey2IDF = null;
		_mPrevInit = null;	
	}
	
	public void init(Set<String> docs) {
		if (docs == _mPrevInit)
			return; // Already initialized
		_mPrevInit = docs;
		_hmKey2IDF = new HashMap<Object,Double>();
		for (String doc : docs) {
			Map<Object,Double> features = (Map<Object,Double>)getObjectRepresentation(doc);
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

	public Object getNoncachedObjectRepresentation(String content) {
		Map<Object,Double> features = DocUtils.ConvertToFeatureMap(content);
		for (Object key : features.keySet())
			if (!_hmKey2IDF.containsKey(key))
				_hmKey2IDF.put(key, _dDefaultIDF);
		features = VectorUtils.ElementWiseMultiply(features, _hmKey2IDF);
		return VectorUtils.NormalizeL1(features);
	}
	
	public double sim(Object o1, Object o2) {
		Map<Object, Double> s1 = (Map<Object, Double>)o1;
		Map<Object, Double> s2 = (Map<Object, Double>)o2;
		return VectorUtils.DotProduct(s1, s2);
	}

	public double sim(Object o1, Object o2, Object ow) {

		Map<Object, Double> s1 = (Map<Object, Double>)o1;
		Map<Object, Double> s2 = (Map<Object, Double>)o2;
		Map<Object, Double> w  = (Map<Object, Double>)ow;
		return VectorUtils.WeightedDotProduct(s1, s2, w);
	}

	@Override
	public String getObjectStringDescription(Object obj) {
		return obj.toString();
	}
	
	@Override
	public String getKernelDescription() {
		// TODO Auto-generated method stub
		return "PLSRKernelTFIDF";
	}

}
