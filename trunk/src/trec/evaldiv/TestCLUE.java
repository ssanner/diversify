/** Code to load and evaluate TREC 6-8 Interactive track
 *   
 * @author Scott Sanner (ssanner@gmail.com)
 */

package trec.evaldiv;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.DecimalFormat;
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
import trec.evaldiv.loss.NDEval10Losses;
import trec.evaldiv.loss.USLoss;
import trec.evaldiv.loss.WSLoss;
import util.FileFinder;

////////////////////////////////////////////////////////////////////
// Evaluates Different Diversification Algorithms on ClueWeb Content
////////////////////////////////////////////////////////////////////

public class TestCLUE {

	public final static boolean DEBUG = false;
	public final static DecimalFormat _df = new DecimalFormat("#.###");
	
	public final static int NUM_RESULTS = 20;
	
	//public final static String CLUE_DOC_DIR1 = "../../Data/CIKM2011/ClueWeb-CatB/Clean/OKAPI-Result-Clean/1";
	public final static String CLUE_DOC_DIR  = 
		System.getProperty("os.name").toLowerCase().startsWith("windows")
		? "../../Data/CIKM2011/ClueWeb-CatB/Clean/OKAPI-Result-Clean"
		: "files/CIKM2011/ClueWeb-CatB/Clean/OKAPI-Result-Clean";
	
	public final static String QUERY_FILE_2009 = "files/trec/wt09.topics.queries-only";
	public final static String QUERY_FILE_2010 = "files/trec/wt10.topics.queries-only";
	
	public final static String ASPECT_FILE  = "files/trec/qrels.diversity.all";
	
	public static String[] CLUE_QUERIES = null;
	static {
		ArrayList<String> queries = new ArrayList<String>();
		for (int i = 1; i <= 100 /*100*/; i++) {
			queries.add((i <= 50 ? "wt09-" : "wt10-") + i);
		}
		
		// Remove these results which had < 2 relevant documents in retrieval set
		//		wt09-44	0.002	1	598
		//		wt10-54	0	0	254
		//		wt10-55	0	0	126
		//		wt10-59	0	0	46
		//		wt10-70	0	0	98
		//		wt10-71	0	0	90
		//		wt10-72	0	0	83
		//		wt10-83	0	0	82
		//		wt10-92	0	0	29
		//		wt10-95	?	0	0 <-- no relevant aspects listed 
		//		wt10-100 ?	0	0 <-- no relevant aspects listed
		queries.remove("wt09-44");
		queries.remove("wt10-100");
		queries.remove("wt10-54");
		queries.remove("wt10-55");
		queries.remove("wt10-59");
		queries.remove("wt10-70");
		queries.remove("wt10-71");
		queries.remove("wt10-72");
		queries.remove("wt10-83");
		queries.remove("wt10-92");
		queries.remove("wt10-95");
		
		CLUE_QUERIES = new String[queries.size()];
		CLUE_QUERIES = queries.toArray(CLUE_QUERIES);
		System.out.println(queries.size() + " queries: " + queries);
	}	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

		// Build Document map per query
		HashMap<String,String> docs = new HashMap<String,String>();
		ArrayList<File> files = FileFinder.GetAllFiles(CLUE_DOC_DIR, "", true);
		int count = 0;
		for (File f : files) {
			//System.out.println("Reading: " + f);
			Doc d = new CLUEDoc(f);
			docs.put(d._name, d.getDocContent());
			if (DEBUG) 
				System.out.println("CLUEDoc: " + f + " -> " + d + "\n - content: " + d.getDocContent());
			if (++count % 500 == 0)
				System.out.println("Read " + count + " documents");
		}
		System.out.println("Read total of " + count + " (unique: " + docs.size() + ") documents");
		
		// Build Query map
		Map<String,Query> queries = ReadCLUEQueries(Arrays.asList(new String[] {
				QUERY_FILE_2009, QUERY_FILE_2010 }));
		if (DEBUG) {
			for (Query q : queries.values())
				System.out.println("CLUEQuery: " + q + "\n - content: " + q.getQueryContent());
		}
		System.out.println("Read " + queries.size() + " queries");
		
		// Build the DocAspects
		Map<String,QueryAspects> aspects = ReadCLUEAspects(ASPECT_FILE, CLUE_DOC_DIR);
		System.out.println("Read " + aspects.size() + " query aspects");
		if (true || DEBUG) {
			for (QueryAspects q : aspects.values()) {
				Set<String> reldocs = q.getRelevantDocs();
				Set<String> availdocs = q.getAvailableDocs();
				Set<String> avail_and_rel_docs = new HashSet<String>(availdocs);
				avail_and_rel_docs.retainAll(reldocs);
				double frac_avail = avail_and_rel_docs.size() / (double)reldocs.size();
				if (true /*avail_and_rel_docs.size() < 2*/) {
					System.out.print(q._queryName);
					System.out.println("\t" + _df.format(frac_avail) + "\t" + avail_and_rel_docs.size() + "\t" + reldocs.size());
				}
			}
		}
	
		// Build the Loss functions
		ArrayList<AspectLoss> loss_functions = new ArrayList<AspectLoss>();
		//loss_functions.add(new USLoss());
		//loss_functions.add(new WSLoss());
		//loss_functions.add(new AvgUSLoss());
		//loss_functions.add(new AvgWSLoss());
		loss_functions.add(new AllUSLoss());
		loss_functions.add(new AllWSLoss());
		loss_functions.add(new NDEval10Losses());
		
		// Build the TREC tests
		// Build a new result list selectors... all use the greedy MMR approach,
		// each simply selects a different similarity metric
		ArrayList<ResultListSelector> tests = new ArrayList<ResultListSelector>();
		
		// Instantiate all the kernels that we will use with the algorithms below
		Kernel TF_kernel    = new TF(docs, true /* query-relevant diversity */);
		Kernel TFIDF_kernel = new TFIDF(docs, true /* query-relevant diversity */);
		Kernel LDA10_kernel   = new LDAKernel(docs, 10 /* NUM TOPICS - suggest 15 */, false /* spherical */, false /* query-relevant diversity */);
		Kernel PLSR10_kernel  = new PLSRKernel(docs, 10 /* NUM TOPICS - suggest 15 */, false /* spherical */);
		Kernel LDA15_kernel   = new LDAKernel(docs, 15 /* NUM TOPICS - suggest 15 */, false /* spherical */, false /* query-relevant diversity */);
		Kernel LDA15_sph_kernel   = new LDAKernel(docs, 15 /* NUM TOPICS - suggest 15 */, true /* spherical */, false /* query-relevant diversity */);
		Kernel LDA15_qr_kernel   = new LDAKernel(docs, 15 /* NUM TOPICS - suggest 15 */, false /* spherical */, true /* query-relevant diversity */);
		Kernel LDA15_qr_sph_kernel   = new LDAKernel(docs, 15 /* NUM TOPICS - suggest 15 */, true /* spherical */, true /* query-relevant diversity */);
		Kernel PLSR15_kernel  = new PLSRKernel(docs, 15 /* NUM TOPICS - suggest 15 */, false /* spherical */);
		Kernel PLSR15_sph_kernel  = new PLSRKernel(docs, 15 /* NUM TOPICS - suggest 15 */, true /* spherical */);
		Kernel LDA20_kernel   = new LDAKernel(docs, 20 /* NUM TOPICS - suggest 15 */, false /* spherical */, false /* query-relevant diversity */);
		Kernel PLSR20_kernel  = new PLSRKernel(docs, 20 /* NUM TOPICS - suggest 15 */, false /* spherical */);
		Kernel BM25_kernel  = 
			new BM25Kernel(docs, 
				/* 0 for any disables effect */
				0.5d /* k1 - doc TF */, 
				0.5d /* k3 - query TF */,
				0.5d /* b - doc length penalty */ );
		
		// Add all MMR test variants (vary lambda and kernels)
//		tests.add( new ScoreRanker( docs, TF_kernel ));
//
//		tests.add( new MMR( docs, 
//				0.5d /* lambda: 0d is all weight on query sim */, 
//				TF_kernel /* sim */,
//				TF_kernel /* div */ ));
//		
//		tests.add( new ScoreRanker( docs, TFIDF_kernel ));

	//		tests.add( new ScoreRanker( docs, BM25_kernel ));
	//
	//		tests.add( new MMR( docs, 
	//				0.5d /* lambda: 0d is all weight on query sim */, 
	//				TFIDF_kernel /* sim */,
	//				TFIDF_kernel /* div */ ));
	//		
	//		tests.add( new MMR( docs, 
	//				0.5d /* lambda: 0d is all weight on query sim */, 
	//				BM25_kernel  /* sim */,
	//				TFIDF_kernel /* div */ )); /* cannot use BM25 for diversity, not symmetric */

//		tests.add( new MMR( docs, 
//				0.5d /* lambda: 0d is all weight on query sim */, 
//				LDA10_kernel /* sim */,
//				LDA10_kernel /* div */ ));
//
		tests.add( new MMR( docs, 
				0.5d /* lambda: 0d is all weight on query sim */, 
				BM25_kernel /* sim */,
				LDA15_kernel /* div */ ));

		tests.add( new MMR( docs, 
				0.5d /* lambda: 0d is all weight on query sim */, 
				BM25_kernel /* sim */,
				LDA15_sph_kernel /* div */ ));

		tests.add( new MMR( docs, 
				0.5d /* lambda: 0d is all weight on query sim */, 
				BM25_kernel /* sim */,
				LDA15_qr_kernel /* div */ ));

		tests.add( new MMR( docs, 
				0.5d /* lambda: 0d is all weight on query sim */, 
				BM25_kernel /* sim */,
				LDA15_qr_sph_kernel /* div */ ));

//		tests.add( new MMR( docs, 
//				0.5d /* lambda: 0d is all weight on query sim */, 
//				LDA20_kernel /* sim */,
//				LDA20_kernel /* div */ ));
//
//		tests.add( new MMR( docs, 
//				0.5d /* lambda: 0d is all weight on query sim */, 
//				PLSR10_kernel /* sim */,
//				PLSR10_kernel /* div */ ));
//
		tests.add( new MMR( docs, 
				0.5d /* lambda: 0d is all weight on query sim */, 
				BM25_kernel /* sim */,
				PLSR15_kernel /* div */ ));

		tests.add( new MMR( docs, 
				0.5d /* lambda: 0d is all weight on query sim */, 
				BM25_kernel /* sim */,
				PLSR15_sph_kernel /* div */ ));

//		tests.add( new MMR( docs, 
//				0.5d /* lambda: 0d is all weight on query sim */, 
//				PLSR20_kernel /* sim */,
//				PLSR20_kernel /* div */ ));

		// Evaluate results of different query processing algorithms
		Evaluator.doEval(Arrays.asList(CLUE_QUERIES), docs, 
						 queries, aspects, loss_functions, tests, NUM_RESULTS, "clueweb");
	}

	///////////////////////////////////////////////////////////////////////////////
	//                              Helper Functions
	///////////////////////////////////////////////////////////////////////////////
	
	// Note: the TREC Query files have a rather non-standard format
	private static Map<String, Query> ReadCLUEQueries(List<String> query_files) {
		TreeMap<String, Query> queries = new TreeMap<String, Query>();
		BufferedReader br;
		try {
			for (String query_file : query_files) {
				br = new BufferedReader(new FileReader(query_file));
				String line = null;
				
				while ((line = br.readLine()) != null) {
					line = line.trim();
					String split[] = line.split("[:]");
					Query query = new Query(split[0], split[1], "", "");
					queries.put(query._name, query);
				}
				br.close();
			}		
		} catch (Exception e) {
			System.out.println("ERROR: " + e);
			e.printStackTrace();
			System.exit(1);
		}
		
		return queries;
	}

	public static Map<String,QueryAspects> ReadCLUEAspects(String aspect_file, String file_root) {
		
		Map<String,QueryAspects> aspects = new TreeMap<String,QueryAspects>();
		
		String line = null;
		HashSet<Integer> ids = new HashSet<Integer>();
		for (int i = 1; i <= 100; i++) ids.add(i);
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
					cur_qa = new QueryAspects((query_id <= 50 ? "wt09-" : "wt10-") + query_id, query_id, file_root);
					aspects.put(cur_qa._queryName, cur_qa);
					ids.remove(query_id);
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
		
		// No aspects available
		//aspects.put("wt10-95", new QueryAspects("wt10-95", file_root));
		//aspects.put("wt10-100", new QueryAspects("wt10-100", file_root));
		
		return aspects;
	}
}
