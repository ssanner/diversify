/** Abstract Class for a generic ResultList Selector... subclasses
 *  must implement the abstract methods.  The subclass MMR implements
 *  a greedy result list selection method, but other subclasses might
 *  choose other approaches (e.g., global optimization).
 * 
 * @author Scott Sanner (ssanner@gmail.com)
 */

package diversity;

import java.util.*;

// Classes should extend this one and implement
public abstract class ResultListSelector {

	public static boolean SHOW_DEBUG = false;

	public Map<String, String> _docs     = new HashMap<String, String>();
	public Map<String, Object> _docRepr  = new HashMap<String, Object>();
	public Map<String, Object> _docRepr2 = new HashMap<String, Object>();
	public Set<String>         _docOrig  = new HashSet<String>();

	public ResultListSelector(HashMap<String, String> docs) {
		_docs = docs;
	}
	
	public abstract ArrayList<String> getResultList(String query, int list_size);

	public abstract void addDoc(String doc_name);

	public abstract void initDocs();

	public abstract void clearDocs();

	public abstract String getDescription();
	
	public String getDoc(String doc_name) {
		return _docs.get(doc_name);
	}
}
