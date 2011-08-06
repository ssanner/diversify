/** Document implementation for TREC Interactive track data (TREC Disks 4-5)
 *   
 * @author Scott Sanner (ssanner@gmail.com)
 */

package trec.evaldiv.doc;

import java.io.*;
import java.util.*;

import org.apache.xerces.parsers.DOMParser;
//import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import util.DocUtils;


public class TRECDoc extends Doc {

	public TRECDoc(File f) {
 
		/**
         * Parse the input url
         */
		Document doc = null;
		try {
	        DOMParser parser = new DOMParser();
	        String s = "<xml>\n" + DocUtils.ReadFile(f, true) + "\n</xml>";
	        ByteArrayInputStream bs = new ByteArrayInputStream(s.getBytes());
	        parser.parse(new org.xml.sax.InputSource(bs));
	        doc = parser.getDocument();
		} catch (Exception e) {
			System.out.println(e);
		}

		//System.out.println(this);
        //PrintNode(doc, "", 0);
        //System.exit(1);

		//NodeList nodes = (NodeList)XPathQuery(doc, "//HEADLINE");
		//for (int i = 0; i < nodes.getLength(); i++) {
		//    System.out.println(nodes.item(i).getNodeValue() + ": " + nodes.item(i).getTextContent()); 
		//}
		_name = f.getName(); //(String)XPathQuery(doc, "//DOCNO");
		_title = (String)XPathQuery(doc, "//HEADLINE");
		_content = (String)XPathQuery(doc, "//TEXT");

		String[] filename_split = f.toString().split("[\\\\]");
		String query_num = filename_split[filename_split.length - 2];
		//_queryToDocNames.putValue(query_num, _name);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		File f = new File("../../Data/CIKM2011/TREC6-8/301/FT911-4747");
		System.out.println("Loading: " + f.getAbsolutePath());
		TRECDoc d = new TRECDoc(f);
		System.out.println("NAME: " + d._name);
		System.out.println(d);
	}
}
