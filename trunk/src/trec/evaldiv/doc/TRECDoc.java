package trec.evaldiv.doc;

import java.io.*;
import java.util.*;

import org.apache.xerces.parsers.DOMParser;
//import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class TRECDoc extends Doc {

	public TRECDoc(File f) {
 
		/**
         * Parse the input url
         */
		Document doc = null;
		try {
	        DOMParser parser = new DOMParser();
	        InputStream byteStream = new FileInputStream(f);
	        parser.parse(new org.xml.sax.InputSource(byteStream));
	        doc = parser.getDocument();
		} catch (Exception e) {
			System.out.println(e);
			
		}

		//NodeList nodes = (NodeList)XPathQuery(doc, "//HEADLINE");
		//for (int i = 0; i < nodes.getLength(); i++) {
		//    System.out.println(nodes.item(i).getNodeValue() + ": " + nodes.item(i).getTextContent()); 
		//}
		_name = (String)XPathQuery(doc, "//DOCNO");
		_title = (String)XPathQuery(doc, "//HEADLINE");
		_content = (String)XPathQuery(doc, "//TEXT");

		//System.out.println(this);
        //PrintNode(doc, "", 0);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		TRECDoc d = new TRECDoc(new File("files/trec/TREC_DATA/FT911-1147.xml"));
		System.out.println(d);
	}
}
