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
import diversity.ScoreRanker;
import diversity.kernel.BM25Kernel;
import diversity.kernel.Kernel;
import diversity.kernel.LDAKernel;
import diversity.kernel.PLSRKernel;
import diversity.kernel.TF;
import diversity.kernel.TFIDF;

import trec.evaldiv.doc.CLUEDoc;
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

public class TestCLUE {

	public final static boolean DEBUG = false;
	
	public final static int NUM_RESULTS = 10;
	
	public final static String CLUE_DOC_DIR = "files/trec/CLUE_DATA";
	public final static String QUERY_FILE   = "files/trec/wt09.topics.queries-only";
	public final static String ASPECT_FILE  = "files/trec/qrels.diversity";
	
	public final static String[] CLUE_QUERIES = new String[50];
	static {
		for (int i = 1; i <= 50; i++)
			CLUE_QUERIES[i-1] = "wt09-" + i;
	}	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

		// Build FT Document map
		HashMap<String,Doc> docs = new HashMap<String,Doc>();
		ArrayList<File> files = FileFinder.GetAllFiles(CLUE_DOC_DIR, ".clean", true);
		for (File f : files) {
			//System.out.println("Reading: " + f);
			Doc d = new CLUEDoc(f);
			docs.put(d._name, d);
			if (DEBUG) 
				System.out.println("CLUEDoc: " + f + " -> " + d + "\n - content: " + d.getDocContent());
		}
		System.out.println("Read " + docs.size() + " documents");
		
		// Build Query map
		HashMap<String,Query> queries = ReadCLUEQueries(QUERY_FILE);
		if (DEBUG) {
			for (Query q : queries.values())
				System.out.println("CLUEQuery: " + q + "\n - content: " + q.getQueryContent());
		}
		System.out.println("Read " + queries.size() + " queries");
		
		// Build the DocAspects
		HashMap<String,QueryAspects> aspects = ReadCLUEAspects(ASPECT_FILE);
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
		tests.add( new ScoreRanker( TF_kernel ));

		tests.add( new MMR(
				0.5d /* lambda: 0d is all weight on query sim */, 
				TF_kernel /* sim */,
				TF_kernel /* div */ ));
		
		tests.add( new ScoreRanker( TFIDF_kernel ));

		tests.add( new MMR(
				0.5d /* lambda: 0d is all weight on query sim */, 
				TFIDF_kernel /* sim */,
				TFIDF_kernel /* div */ ));
		
		tests.add( new ScoreRanker( BM25_kernel ));
		
		tests.add( new MMR(
				0.5d /* lambda: 0d is all weight on query sim */, 
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
		Evaluator.doEval(Arrays.asList(CLUE_QUERIES), docs, 
						 queries, aspects, loss_functions, tests, NUM_RESULTS, "clueweb");
	}

	///////////////////////////////////////////////////////////////////////////////
	//                              Helper Functions
	///////////////////////////////////////////////////////////////////////////////
	
	// Note: the TREC Query files have a rather non-standard format
	private static HashMap<String, Query> ReadCLUEQueries(String query_file) {
		HashMap<String, Query> queries = new HashMap<String, Query>();
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(query_file));
			String line = null;
			
			while ((line = br.readLine()) != null) {
				line = line.trim();
				String split[] = line.split("[:]");
				Query query = new Query(split[0], split[1], "", "");
				queries.put(query._name, query);
			}
			br.close();
		
		} catch (Exception e) {
			System.out.println("ERROR: " + e);
			e.printStackTrace();
			System.exit(1);
		}
		
		return queries;
	}

	public static HashMap<String,QueryAspects> ReadCLUEAspects(String aspect_file) {
		
		HashMap<String,QueryAspects> aspects = new HashMap<String,QueryAspects>();
		
		String line = null;
		try {
			BufferedReader br = new BufferedReader(new FileReader(aspect_file));
			HashMap<String,TreeSet<Integer>> cur_aspects = new HashMap<String,TreeSet<Integer>>();
			QueryAspects cur_qa = null;
			int max_aspect = -1;
			int last_query_id = -1;
			
			while ((line = br.readLine()) != null) {
				line = line.trim();
				String[] split = line.split("[\\s]");

				boolean relevant = split[3].equals("1");

				int query_id = new Integer(split[0]);
				int aspect_id = new Integer(split[1]);
				String doc = split[2];
				
				if (query_id != last_query_id) {
					// We're on a new query, add all aspects for old query
					
					// Calc stats for old query
					if (cur_qa != null)
						cur_qa.addAllAspects(cur_aspects, max_aspect);
										
					// Make a new query and rest aspects / max
					max_aspect = -1;
					cur_aspects.clear();
					cur_qa = new QueryAspects("wt09-" + query_id);
					aspects.put(cur_qa._queryName, cur_qa);
				}
				last_query_id = query_id;
				
				// Add aspect to current query
				TreeSet<Integer> aspect_set = cur_aspects.get(doc);
				if (aspect_set == null) {
					aspect_set = new TreeSet<Integer>();
					cur_aspects.put(doc, aspect_set);
				}
				if (relevant) {
					aspect_set.add(aspect_id);
					if (aspect_id > max_aspect)
						max_aspect = aspect_id;
				}
			}
			br.close();
			
			// End of file - add last aspect
			if (cur_qa != null)
				cur_qa.addAllAspects(cur_aspects, max_aspect);
			
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
