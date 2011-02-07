/** Code to load and evaluate TREC 6-8 Interactive track
 *   
 * @author Scott Sanner (ssanner@gmail.com)
 */

package trec.evaldiv;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

import diversity.MMR;
import diversity.ResultListSelector;
import diversity.kernel.BM25Kernel;
import diversity.kernel.Kernel;
import diversity.kernel.LDAKernel;
import diversity.kernel.PLSRKernel;
import diversity.kernel.TF;
import diversity.kernel.TFIDF;

import trec.evaldiv.doc.Doc;
import trec.evaldiv.doc.TRECDoc;
import trec.evaldiv.loss.AllUSLoss;
import trec.evaldiv.loss.AllWSLoss;
import trec.evaldiv.loss.AspectLoss;
import trec.evaldiv.loss.AvgUSLoss;
import trec.evaldiv.loss.AvgWSLoss;
import trec.evaldiv.loss.USLoss;
import trec.evaldiv.loss.WSLoss;
import util.FileFinder;

///////////////////////////////////////////////////////////////////////////////
// Evaluates Different Diversification Algorithms on TREC 6-8 Interactive Track
///////////////////////////////////////////////////////////////////////////////

public class TestTREC {

	public final static boolean DEBUG = false;
	
	public final static int NUM_RESULTS = 10;
	
	public final static String TREC_DOC_DIR = "files/trec/TREC_DATA";
	public final static String QUERY_FILE   = "files/trec/TRECQuery.txt";
	public final static String ASPECT_FILE  = "files/trec/TRECQueryAspects.txt";
	
	public final static String[] TREC_QUERIES = 
		{ "307i", "322i", "326i", "347i", "352i"/*, "353i", 
		  "357i", "362i", "366i", "387i", "392i", "408i", 
		  "414i", "428i", "431i", "438i", "446i"*/ };
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

		// Build FT Document map
		HashMap<String,Doc> docs = new HashMap<String,Doc>();
		ArrayList<File> files = FileFinder.GetAllFiles(TREC_DOC_DIR, "", true);
		for (File f : files) {
			Doc d = new TRECDoc(f);
			docs.put(d._name, d);
			if (DEBUG) 
				System.out.println("TRECDoc: " + f + " -> " + d + "\n - content: " + d.getDocContent());
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
	
		// Build the Loss functions
		ArrayList<AspectLoss> loss_functions = new ArrayList<AspectLoss>();
		//loss_functions.add(new USLoss());
		//loss_functions.add(new WSLoss());
		//loss_functions.add(new AvgUSLoss());
		//loss_functions.add(new AvgWSLoss());
		loss_functions.add(new AllUSLoss());
		loss_functions.add(new AllWSLoss());
		
		// Build the TREC tests
		// Build a new result list selectors... all use the greedy MMR approach,
		// each simply selects a different similarity metric
		ArrayList<ResultListSelector> tests = new ArrayList<ResultListSelector>();
		
		// Instantiate all the kernels that we will use with the algorithms below
		Kernel TF_kernel    = new TF(true /* query-relevant diversity */);
		Kernel TFIDF_kernel = new TFIDF(true /* query-relevant diversity */);
		Kernel LDA_kernel   = new LDAKernel(15 /* NUM TOPICS - suggest 15 */, true /* spherical */, true /* query-relevant diversity */);
		Kernel PLSR_kernel  = new PLSRKernel(15 /* NUM TOPICS - suggest 15 */, false /* spherical */);
		Kernel BM25_kernel  = 
			new BM25Kernel( /* 0 for any disables effect */
				0.5d /* k1 - doc TF */, 
				0.5d /* k3 - query TF */,
				0.5d /* b - doc length penalty */ );
		
		// Add all MMR test variants (vary lambda and kernels)
		tests.add( new MMR(
				0.5d /* lambda: 0d is all weight on query sim */, 
				TF_kernel /* sim */,
				TF_kernel /* div */ ));
		
		tests.add( new MMR(
				0.5d /* lambda: 0d is all weight on query sim */, 
				TFIDF_kernel /* sim */,
				TFIDF_kernel /* div */ ));
		
		tests.add( new MMR(
				0.5d /* lambda: 0d is all weight on query sim */, 
				BM25_kernel  /* sim */,
				TFIDF_kernel /* div */ )); /* cannot use BM25 for diversity, not symmetric */
		
		tests.add( new MMR(
				0.0d /* lambda: 0d is **all weight** on query sim */, 
				BM25_kernel  /* sim */,
				TFIDF_kernel /* div */ )); /* cannot use BM25 for diversity, not symmetric */

		tests.add( new MMR(
				0.5d /* lambda: 0d is all weight on query sim */, 
				LDA_kernel /* sim */,
				LDA_kernel /* div */ ));

		tests.add( new MMR(
				0.5d /* lambda: 0d is all weight on query sim */, 
				PLSR_kernel /* sim */,
				PLSR_kernel /* div */ ));

		
		// Evaluate results of different query processing algorithms
		Evaluator.doEval(Arrays.asList(TREC_QUERIES), docs, 
						 queries, aspects, loss_functions, tests, NUM_RESULTS);
	}

	///////////////////////////////////////////////////////////////////////////////
	//                              Helper Functions
	///////////////////////////////////////////////////////////////////////////////
	
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
							cur_query._name = line; // Should only be one line
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
