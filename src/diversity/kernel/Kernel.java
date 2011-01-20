/** Abstract class for a kernel specification... subclasses must 
 *  implement the abstract methods.
 *   
 * @author Scott Sanner (ssanner@gmail.com)
 */

package diversity.kernel;

import java.util.Set;
import java.util.Map;

// TODO: BM25, 

/** Computes a similarity **/
public abstract class Kernel {
	public abstract double sim(Object s1, Object s2);
	public abstract double sim(Object s1, Object s2, Object q);
	public boolean supportsSetSim() { return false; }
	public Double setSim(Object s1, Set<Object> s2, Object q) { return null; } 
	public abstract void init(Map<String,String> docs);
	/* Should only be called after init(...) has been called */
	public abstract Object getObjectRepresentation(String content);
	public abstract String getObjectStringDescription(Object obj);
	public abstract String getKernelDescription();
}
