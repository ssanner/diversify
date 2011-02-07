package trec.evaldiv;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import trec.evaldiv.doc.Doc;

// An inner class data structure to store aspects for a doc
public class QueryAspects {
	public String _queryName = null;
	public int _numAspects = -1;
	public HashMap<String, boolean[]> _aspects = null;
	public double[] _freq = null;
	public double[] _weights = null;
	
	public QueryAspects(String query_name) {
		_queryName = query_name;
		_aspects = new HashMap<String,boolean[]>();
	}
	
	public void addAspect(String doc, String aspect_str) {
		if (_numAspects < 0)
			_numAspects = aspect_str.length();
		
		System.out.println("Adding aspect: " + doc + " :: " + aspect_str);
		boolean[] b_aspects = new boolean[_numAspects];
		int aspect_count = 0;
		for (int i = 0; i < _numAspects; i++)
			if ((b_aspects[i] = (aspect_str.charAt(i) == '1')))
				aspect_count++;
		
		if (aspect_count > 0)
			_aspects.put(doc, b_aspects);
	}
	
	public void calcAspectStats() {
		_freq    = new double[_numAspects];
		_weights = new double[_numAspects];
		for (boolean[] aspects : _aspects.values()) {
			for (int i = 0; i < _numAspects; i++)
				if (aspects[i])
					_freq[i]++;
		}

		int total = 0;
		for (int i = 0; i < _numAspects; i++)
			total += _freq[i];
		
		for (int i = 0; i < _numAspects; i++)
			_weights[i] = _freq[i] / (double)total;
	}
	
	public String toString() {
		
		StringBuilder sb = new StringBuilder("Query Aspects for '" + _queryName + "'\n========================");
		sb.append("\n- Doc Aspects:\n");
		for (Map.Entry<String, boolean[]> e : _aspects.entrySet())
			sb.append(e.getKey() + " -> " + getAspectsAsStr(e.getValue()) + "\n");
	
		sb.append("- Freq -> ");
		for (int i = 0; i < _numAspects; i++)
			sb.append(i + ":" + _freq[i] + " ");
		sb.append("\n- Weight -> ");
		for (int i = 0; i < _numAspects; i++)
			sb.append(i + ":" + _weights[i] + " ");
	
		return sb.toString();
	}
	
	public static String getAspectsAsStr(boolean[] b) {
		
		StringBuilder sb = new StringBuilder(" ");
		for (int i = 0; i < b.length; i++)
			if (b[i])
				sb.append(" " + i);
		return sb.toString().substring(1,sb.length());
	}
	
	public double getUniformSubtopicLoss(List<String> doc_names, int k) {
		boolean[] b_aspects = new boolean[_numAspects];
		int doc_count = 0;
		for (String doc_name : doc_names) {
			if (doc_count++ >= k)
				break;
			boolean[] d_aspects = _aspects.get(doc_name);
			if (d_aspects != null) {
				for (int i = 0; i < _numAspects; i++)
					b_aspects[i] = b_aspects[i] || d_aspects[i];
			}
		}
		int count_aspects = 0;
		for (int i = 0; i < _numAspects; i++)
			if (b_aspects[i])
				count_aspects++;
		
		
		return count_aspects / (double)_numAspects;
	}
	
	public double getWeightedSubtopicLoss(List<String> doc_names, int k) {
		boolean[] b_aspects = new boolean[_numAspects];
		int doc_count = 0;
		for (String doc_name : doc_names) {
			if (doc_count++ >= k)
				break;
			boolean[] d_aspects = _aspects.get(doc_name);
			if (d_aspects != null) {
				for (int i = 0; i < _numAspects; i++)
					b_aspects[i] = b_aspects[i] || d_aspects[i];
			}
		}
		
		double weight = 0d;
		for (int i = 0; i < _numAspects; i++)
			if (b_aspects[i])
				weight += _weights[i]; // If all aspects on, weight will be 1.0

		return weight;
	}
}