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
			_content = DocUtils.ReadFile(f, true);
			for (int i = 0; i < 7; i++) { // Discard first 6 lines
				//System.out.println(_content.indexOf('\n'));
				_content = _content.substring(_content.indexOf("\n") + 1, _content.length());
			}
			//System.out.println("Final: " + _content);
		} catch (Exception e) {
			System.out.println(e);		
			e.printStackTrace();
			System.exit(1);
		}

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		CLUEDoc d = new CLUEDoc(new File("../../Data/CIKM2011/ClueWeb-CatB/Clean/OKAPI-Result-Clean/1/clueweb09-en0000-06-20977"));
		System.out.println(d);
	}
}
