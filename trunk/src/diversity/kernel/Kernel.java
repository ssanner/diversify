/** Abstract class for a kernel specification... subclasses must 
 *  implement the abstract methods.
 *   
 * @author Scott Sanner (ssanner@gmail.com)
 */

package diversity.kernel;

import java.util.HashMap;
import java.util.Set;
import java.util.Map;

// TODO: BM25, 

/** Computes a similarity **/
public abstract class Kernel {
	public HashMap<String,String> _docs = null;
	public HashMap<String,Object> _reprCache = new HashMap<String,Object>();
	public Kernel(HashMap<String,String> docs) {
		_docs = docs;
	}
	public abstract double sim(Object s1, Object s2);
	public abstract double sim(Object s1, Object s2, Object q);
	public boolean supportsSetSim() { return false; }
	public Double setSim(Object s1, Set<Object> s2, Object q) { return null; } 
	public abstract void clear();
	public abstract void init(Set<String> docs);
	/* Should only be called after init(...) has been called */
	public Object getObjectRepresentation(String doc_name) {
		Object repr = null;
		if ((repr = _reprCache.get(doc_name)) != null)
			return repr;
		String content = _docs.get(doc_name);
		repr = getNoncachedObjectRepresentation(content);
		_reprCache.put(doc_name, repr);
		return repr;
	}
	public abstract Object getNoncachedObjectRepresentation(String content);
	public abstract String getObjectStringDescription(Object obj);
	public abstract String getKernelDescription();
}
