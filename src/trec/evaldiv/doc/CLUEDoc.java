/** Document implementation for TREC Interactive track data (TREC Disks 4-5)
 *   
 * @author Scott Sanner (ssanner@gmail.com)
 */

package trec.evaldiv.doc;

import java.io.*;

import util.DocUtils;


public class CLUEDoc extends Doc {

	public CLUEDoc(File f) {
 
		/**
         * Parse the input url
         */
		try {
			_name = f.getName().split("[\\.]")[0];
			_title = "";
			_content = DocUtils.ReadFile(f);
			int first_line_file = _content.indexOf(_name);
			_content = _content.substring(first_line_file + _name.length() + 1, _content.length());
		} catch (Exception e) {
			System.out.println(e);			
		}

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		CLUEDoc d = new CLUEDoc(new File("files/trec/CLUE_DATA/Q1/clueweb09-en0001-02-21241.txt.clean"));
		System.out.println(d);
	}
}
