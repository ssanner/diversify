/** Document representation for TREC Interactive and CLUEWEB Diversity tracks
 *   
 * @author Scott Sanner (ssanner@gmail.com)
 */

package trec.evaldiv.doc;

import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import util.MapList;

import javax.xml.xpath.*;

public abstract class Doc {
	
	public final static boolean USE_TITLE = true;
	public final static boolean USE_CONTENT = true;
	
	public final int DISPLAY_DOCLEN = 100;

	//public static MapList _queryToDocNames = new MapList();
	public static XPath _xpath = XPathFactory.newInstance().newXPath();
	
	public String _name;
	public String _title;
	public String _content;
	public String _docContent;

	public String toString() {
		String content = _content;
		if (content.length() > DISPLAY_DOCLEN)
			content = content.substring(0, DISPLAY_DOCLEN);
		return _name + " :: " + _title + " : " + content;
	}
	
	public String getDocContent() {
		if (_docContent != null)
			return _docContent;
		
		StringBuilder sb = new StringBuilder();
		if (USE_TITLE) {
			sb.append(_title);
		} 
		if (USE_CONTENT) {
			sb.append((sb.length() > 0 ? " " : "") + _content);			
		}
		_docContent = sb.toString();
		return _docContent;
	}
	
	/**
     * Creates a string buffer of spaces
     * @param depth the number of spaces
     * @return string of spaces
     */
    public static StringBuffer Pad(int depth) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < depth; i++)
			sb.append("  ");
		return sb;
	}

    /**
     * Print the DOM tree on stdout
     * @param n root node of a document
     * @param prefix
     * @param depth
     */
	public static void PrintNode(Node n, String prefix, int depth) {
		
		try {			
			System.out.print("\n" + Pad(depth) + "[" + n.getNodeName());
			NamedNodeMap m = n.getAttributes();
			for (int i = 0; m != null && i < m.getLength(); i++) {
				Node item = m.item(i);
				System.out.print(" " + item.getNodeName() + "=" + item.getNodeValue());
			}
			System.out.print("] ");
			
			NodeList cn = n.getChildNodes();
			
			for (int i = 0; cn != null && i < cn.getLength(); i++) {
				Node item = cn.item(i);
				if (item.getNodeType() == Node.TEXT_NODE) {
					String val = item.getNodeValue().trim();
					if (val.length() > 0) System.out.print(" \"" + item.getNodeValue().trim() + "\"");
				} else
					PrintNode(item, prefix, depth+2);
			}
		} catch (Exception e) {
			System.out.println(Pad(depth) + "Exception e: ");
		}
	}
	
	public static Object XPathQuery(Document doc, String query) {
		
		try {
			XPathExpression xPathExpression = _xpath.compile(query);
			return xPathExpression.evaluate(doc);
		} catch (XPathExpressionException e) {
			e.printStackTrace();
			return null;
		}
	}
}
