/** Simple triple data structure
 *   
 * @author Scott Sanner (ssanner@gmail.com)
 */

package util;

public class Triple implements Comparable {

	public Object _o1;
	public Object _o2;
	public Object _o3;

	public Triple(Object o1, Object o2, Object o3) {
		_o1 = o1;
		_o2 = o2;
		_o3 = o3;
	}

	public int hashCode() {
		return _o1.hashCode() + _o2.hashCode() + _o3.hashCode();
	}

	public boolean equals(Object o) {
		if (o instanceof Triple) {
			Triple p = (Triple) o;
			return (_o1.equals(p._o1) && _o2.equals(p._o2) && _o3.equals(p._o3));
		} else {
			return false;
		}
	}

	// Perform an ordered comparison, o1's then o2's if o1's are equal
	public int compareTo(Object o) {

		Triple p = (Triple) o;
		Comparable c1 = (Comparable) _o1;
		Comparable p_c1 = (Comparable) p._o1;
		int comp_o1 = c1.compareTo(p_c1);
		if (comp_o1 != 0) {
			return comp_o1;
		}

		Comparable c2 = (Comparable) _o2;
		Comparable p_c2 = (Comparable) p._o2;
		int comp_o2 = c2.compareTo(p_c2);
		if (comp_o2 != 0) {
			return comp_o2;
		}
		
		Comparable c3 = (Comparable) _o3;
		Comparable p_c3 = (Comparable) p._o3;
		return c3.compareTo(p_c3);
	}
}
