/** Query aspect representation for TREC Interactive and CLUEWEB Diversity tracks
 *   
 * @author Scott Sanner (ssanner@gmail.com)
 */

package trec.evaldiv;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import trec.evaldiv.doc.Doc;

// An inner class data structure to store aspects for a doc
public class QueryAspects implements Comparable {
	public int _queryID = -1; 
	public String _queryName = null;
	public int _numAspects = -1;
	public HashMap<String, boolean[]> _aspects = null;
	public Set<String> _availableDocs = null;
	public double[] _freq = null;
	public double[] _weights = null;
	
	public QueryAspects(String query_name) {
		this(query_name, -1, null);
	}
	
	public QueryAspects(String query_name, int id, String doc_source) {
		_queryName = query_name;
		_queryID = id;
		_aspects = new HashMap<String,boolean[]>();
		_availableDocs = new HashSet<String>();
		if (doc_source != null) {
			File dir = new File(doc_source + File.separator + id);
			//System.out.println("Trying dir: " + doc_source + query_parts[1]);
			String[] available_docs = dir.list();
			for (String s : available_docs) {
				_availableDocs.add(s);
				//System.out.println(s);
			}
		}
	}
	
	public Set<String> getRelevantDocs() {
		return _aspects.keySet();
	}
	
	public Set<String> getAvailableDocs() {
		return _availableDocs;
	}
	
	public void addAllAspects(HashMap<String,TreeSet<Integer>> doc2aspects, int max_aspect) {

		_numAspects = max_aspect;
		for (Map.Entry<String,TreeSet<Integer>> e : doc2aspects.entrySet()) {

			String doc = e.getKey();
			TreeSet<Integer> aspects = e.getValue();
			boolean[] b_aspects = null;
			
			if (aspects != null && aspects.size() > 0) {			
				b_aspects = new boolean[_numAspects];
				for (Integer i : aspects) // aspects are never 0
					b_aspects[i-1] = true;
			}
			
			// Can be null
			_aspects.put(doc, b_aspects);
		}
	}
	
	public void addAspect(String doc, String aspect_str) {
		if (_numAspects < 0)
			_numAspects = aspect_str.length();
		
		//System.out.println("Adding aspect: " + doc + " :: " + aspect_str);
		boolean[] b_aspects = new boolean[_numAspects];
		int aspect_count = 0;
		for (int i = 0; i < _numAspects; i++)
			if ((b_aspects[i] = (aspect_str.charAt(i) == '1')))
				aspect_count++;
		
		if (aspect_count > 0)
			_aspects.put(doc, b_aspects);
		else
			_aspects.put(doc, null);
	}
	
	public void calcAspectStats() {
		_freq    = new double[_numAspects];
		_weights = new double[_numAspects];
		for (boolean[] aspects : _aspects.values()) {
			if (aspects != null)
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
		
		if (b == null)
			return "null";
		
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
	
	@Override
	public int compareTo(Object o) {
		return _queryName.compareTo(((QueryAspects)o)._queryName);
	}	
}