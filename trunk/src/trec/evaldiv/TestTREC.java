package trec.evaldiv;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;

import trec.evaldiv.doc.Doc;
import trec.evaldiv.doc.TRECDoc;
import util.FileFinder;

/*** Evaluates different algorithms (PLMMR, MMR) on TREC queries from 
 *   Yue and Joachims, ICML 2008.
 * 
 * @author ssanner
 *
 */
public class TestTREC {

	public final static boolean DEBUG = true;
	
	public final static String TREC_DOC_DIR = "files/trec/TREC_DATA";
	public final static String QUERY_FILE   = "files/trec/TRECQuery.txt";
	public final static String ASPECT_FILE  = "files/trec/TRECQueryAspects.txt";
	
	public final static String[] TREC_QUERIES = 
		{ "307i", "322i", "326i", "347i", "352i", "353i", 
		  "357i", "362i", "366i", "387i", "392i", "408i", 
		  "414i", "428i", "431i", "438i", "446i" };
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		// Build FT Document map
		HashMap<String,Doc> docs = new HashMap<String,Doc>();
		ArrayList<File> files = FileFinder.GetAllFiles(TREC_DOC_DIR, "", true);
		for (File f : files) {
			Doc d = new TRECDoc(f);
			docs.put(d._name, d);
			//if (DEBUG) 
			//	System.out.println("TRECDoc: " + f + " -> " + d + "\n - content: " + d.getDocContent());
		}
		System.out.println("Read " + docs.size() + " documents");
		
		// Build Query map
		HashMap<String,Query> queries = ReadTRECQueries(QUERY_FILE);
		if (DEBUG) {
			for (Query q : queries.values())
				System.out.println("TRECQuery: " + q + "\n - content: " + q.getQueryContent());
		}
		System.out.println("Read " + queries.size() + " queries");
		
		// Build the DocAspects
		HashMap<String,QueryAspects> aspects = ReadTRECAspects(ASPECT_FILE);
		System.out.println("Read " + aspects.size() + " query aspects");
		if (DEBUG) {
			for (QueryAspects q : aspects.values())
				System.out.println(q + "\n");
		}
	
		
		// Evaluate results of different query processing algorithms

	}

	public enum FilePos { NOTHING, NUMBER, TITLE, DESC, OTHER };

	// Note: the TREC Query files have a rather non-standard format
	private static HashMap<String, Query> ReadTRECQueries(String query_file) {
		HashMap<String, Query> queries = new HashMap<String, Query>();
		BufferedReader br;
		FilePos last_read = FilePos.NOTHING;
		try {
			br = new BufferedReader(new FileReader(query_file));
			String line = null;
			Query cur_query = null;
			
			while ((line = br.readLine()) != null) {
				line = line.trim();
				if (line.indexOf("----") >= 0 || line.length() == 0) {
					continue;
				} else if (line.indexOf("Number:") >= 0) {
					if (last_read == FilePos.OTHER) {
						queries.put(cur_query._name, cur_query);
					}
					cur_query = new Query("", "", "", "");
					last_read = FilePos.NUMBER;
				} else if (line.indexOf("Title:") >= 0) {
					last_read = FilePos.TITLE;
				} else if (line.indexOf("Description:") >= 0) {
					last_read = FilePos.DESC;
				} else if (line.indexOf("Instances:") >= 0) {
					last_read = FilePos.OTHER;
				} else {
					switch (last_read) {
						case NOTHING: 
							break;
						case NUMBER:
							cur_query._name += " " + line;
							break;
						case TITLE:
							cur_query._title += " " + line;
							break;
						case DESC: 
							cur_query._description += " " + line;
							break;
						case OTHER:
							cur_query._rest += " " + line;
							break;
					}
				}
			}
			if (last_read == FilePos.OTHER) {
				queries.put(cur_query._name, cur_query);
			}
			br.close();
		
		} catch (Exception e) {
			System.out.println("ERROR: " + e);
			e.printStackTrace();
			System.exit(1);
		}
		
		return queries;
	}

	public static HashMap<String,QueryAspects> ReadTRECAspects(String aspect_file) {
		
		HashMap<String,QueryAspects> aspects = new HashMap<String,QueryAspects>();
		
		String line = null;
		try {
			BufferedReader br = new BufferedReader(new FileReader(aspect_file));
			
			while ((line = br.readLine()) != null) {
				line = line.trim();
				String[] split = line.split("[\\s]");
				String query_name = split[0];
				String doc_name = split[1];
				String aspect_str = split[split.length-1];
				QueryAspects qa = aspects.get(query_name);
				if (qa == null) {
					qa = new QueryAspects(query_name);
					aspects.put(query_name, qa);
				}
				qa.addAspect(doc_name, aspect_str);
			}
			br.close();
			
			// Calculate all aspect stats (e.g., needed for Weighted Subtopic Loss)
			for (QueryAspects q : aspects.values())
				q.calcAspectStats();

		} catch (Exception e) {
			System.out.println("ERROR: " + e + "\npossibly at " + line);
			e.printStackTrace();
			System.exit(1);
		}
		
		return aspects;
	}
}
