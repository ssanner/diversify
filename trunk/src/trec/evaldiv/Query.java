package trec.evaldiv;

public class Query {

	// Constants
	public static final boolean INCLUDE_QUERY_TITLE       = true;
	public static final boolean INCLUDE_QUERY_DESCRIPTION = true;
	public static final boolean INCLUDE_QUERY_OTHER       = true;
	
	// Data Members
	public String _name;
	public String _title;
	public String _description;
	public String _rest;
	//public List<Aspect> _lDocAspect;
	
	// Constructors
	public Query(String name, String title, String description, String rest) {
		_name = name;
		_title = title;
		_description = description;
		_rest = rest;
	}
	
	public String toString() {
		return _name + " -> [ " + _title + ", " + _description + ", " + _rest + " ]"; 
	}
	
	public String getQueryContent() {
		StringBuilder sb = new StringBuilder();
		if (INCLUDE_QUERY_TITLE) {
			sb.append(_title);
		} 
		if (INCLUDE_QUERY_DESCRIPTION) {
			sb.append((sb.length() > 0 ? " " : "") + _description);			
		}
		if (INCLUDE_QUERY_OTHER) {
			sb.append((sb.length() > 0 ? " " : "") + _rest);
		}
		return sb.toString();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
