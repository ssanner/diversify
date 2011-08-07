/** Code to load and evaluate TREC 6-8 Interactive track
 *   
 * @author Scott Sanner (ssanner@gmail.com)
 */

package trec.evaldiv;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
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

///////////////////////////////////////////////////////////////////////////////
// Evaluates Different Diversification Algorithms on TREC 6-8 Interactive Track
///////////////////////////////////////////////////////////////////////////////

public class TestTREC {

	public final static boolean DEBUG = false;
	
	public final static int NUM_RESULTS = 20;
	
	public final static String TREC_DOC_DIR = "../../Data/CIKM2011/TREC6-8";
	public final static String TREC_QRELS   = "files/trec/qrels.trec.all";
	public final static String QUERY_FILE   = "files/trec/TRECQuery.txt";
	public final static String ASPECT_FILE  = "files/trec/TRECQueryAspects.txt";
	
	public final static String[] TREC_QUERIES = 
		{ "307"/*, "322", "326", "347", "352", "353", 
		  "357", "362", "366", "387", "392", "408", 
		  "414", "428", "431", "438", "446"*/ };
	
	static ArrayList<String> ALL_QUERIES = new ArrayList<String>(Arrays.asList(TREC_QUERIES));
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		
		// Build Query map
		HashMap<String,Query> queries = ReadTRECQueries(QUERY_FILE);
		if (DEBUG) {
			for (Query q : queries.values())
				System.out.println("TRECQuery: " + q + "\n - content: " + q.getQueryContent());
		}
		System.out.println("Read " + queries.size() + " queries");
		
		// Build the DocAspects
		HashMap<String,QueryAspects> aspects = ReadTRECAspects(ASPECT_FILE, TREC_DOC_DIR);
		System.out.println("Read " + aspects.size() + " query aspects");
		if (DEBUG) {
			for (QueryAspects q : aspects.values())
				System.out.println(q + "\n");
		}
		//ExportQRels(aspects, TREC_QRELS); System.exit(1);

		// Build FT Document map
		HashMap<String,String> docs = new HashMap<String,String>();
		ArrayList<File> files = FileFinder.GetAllFiles(TREC_DOC_DIR, "", true);
		for (File f : files) {
			//System.out.println(f.toString());
			String[] filename_split = f.toString().split("[\\\\]");
			String query_num = filename_split[filename_split.length - 2];
			if (!ALL_QUERIES.contains(query_num)) {
				//System.out.println("Not in queries, skipping...");
				continue;
			}
			
			Doc d = new TRECDoc(f);
			docs.put(d._name, d.getDocContent());
			if (DEBUG) 
				System.out.println("TRECDoc: " + f + " -> " + query_num + "/" + d._name/*+ d + "\n - content: " + d.getDocContent()*/);
		}
		System.out.println("Read " + docs.size() + " documents");
		//for (Object key : Doc._queryToDocNames.keySet()) {
		//	ArrayList al = Doc._queryToDocNames.getValues(key);
		//	System.out.println(key + " " + al.size() + " : " + al);
		//}
		//System.out.println(Doc._queryToDocNames);
	
		// Build the Loss functions
		ArrayList<AspectLoss> loss_functions = new ArrayList<AspectLoss>();
		//loss_functions.add(new USLoss());
		//loss_functions.add(new WSLoss());
		//loss_functions.add(new AvgUSLoss());
		//loss_functions.add(new AvgWSLoss());
		loss_functions.add(new AllUSLoss());
		loss_functions.add(new AllWSLoss());
		loss_functions.add(new NDEval10Losses(TREC_QRELS));
		
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

//			tests.add( new ScoreRanker( docs, BM25_kernel ));
//	
//			tests.add( new MMR( docs, 
//					0.25d /* lambda: 0d is all weight on query sim */, 
//					TFIDF_kernel /* sim */,
//					TFIDF_kernel /* div */ ));
//			
			tests.add( new MMR( docs, 
					0.5d /* lambda: 0d is all weight on query sim */, 
					BM25_kernel  /* sim */,
					TFIDF_kernel /* div */ )); /* cannot use BM25 for diversity, not symmetric */
//			
//			tests.add( new MMR( docs, 
//					0.5d /* lambda: 0d is all weight on query sim */, 
//					TFIDF_kernel /* sim */,
//					TFIDF_kernel /* div */ ));
//			
//			tests.add( new MMR( docs, 
//					0.5d /* lambda: 0d is all weight on query sim */, 
//					BM25_kernel  /* sim */,
//					TFIDF_kernel /* div */ )); /* cannot use BM25 for diversity, not symmetric */
//			
//			tests.add( new MMR( docs, 
//					0.75d /* lambda: 0d is all weight on query sim */, 
//					TFIDF_kernel /* sim */,
//					TFIDF_kernel /* div */ ));
//			
//			tests.add( new MMR( docs, 
//					0.75d /* lambda: 0d is all weight on query sim */, 
//					BM25_kernel  /* sim */,
//					TFIDF_kernel /* div */ )); /* cannot use BM25 for diversity, not symmetric */

		tests.add( new MMR( docs, 
				0.5d /* lambda: 0d is all weight on query sim */, 
				LDA15_kernel /* sim */,
				LDA15_kernel /* div */ ));

	//		tests.add( new MMR( docs, 
	//				0.5d /* lambda: 0d is all weight on query sim */, 
	//				BM25_kernel /* sim */,
	//				LDA15_kernel /* div */ ));
	//
	//		tests.add( new MMR( docs, 
	//				0.5d /* lambda: 0d is all weight on query sim */, 
	//				BM25_kernel /* sim */,
	//				LDA15_sph_kernel /* div */ ));

	//		tests.add( new MMR( docs, 
	//				0.5d /* lambda: 0d is all weight on query sim */, 
	//				BM25_kernel /* sim */,
	//				LDA15_qr_kernel /* div */ ));
	//
	//		tests.add( new MMR( docs, 
	//				0.5d /* lambda: 0d is all weight on query sim */, 
	//				BM25_kernel /* sim */,
	//				LDA15_qr_sph_kernel /* div */ ));

//		tests.add( new MMR( docs, 
//				0.5d /* lambda: 0d is all weight on query sim */, 
//				LDA20_kernel /* sim */,
//				LDA20_kernel /* div */ ));

		tests.add( new MMR( docs, 
				0.5d /* lambda: 0d is all weight on query sim */, 
				PLSR15_kernel /* sim */,
				PLSR15_kernel /* div */ ));
		
//					tests.add( new MMR( docs, 
//							0.5d /* lambda: 0d is all weight on query sim */, 
//							BM25_kernel /* sim */,
//							LDA15_kernel /* div */ ));
			
//-					tests.add( new MMR( docs, 
//							0.5d /* lambda: 0d is all weight on query sim */, 
//							BM25_kernel /* sim */,
//							PLSR15_kernel /* div */ ));
					
//					tests.add( new MMR( docs, 
//							0.25d /* lambda: 0d is all weight on query sim */, 
//							BM25_kernel /* sim */,
//							LDA15_kernel /* div */ ));
					
//-					tests.add( new MMR( docs, 
//							0.25d /* lambda: 0d is all weight on query sim */, 
//							BM25_kernel /* sim */,
//							PLSR15_kernel /* div */ ));
			//
			//		tests.add( new MMR( docs, 
			//				0.75d /* lambda: 0d is all weight on query sim */, 
			//				BM25_kernel /* sim */,
			//				LDA15_kernel /* div */ ));
			//
			//		tests.add( new MMR( docs, 
			//				0.75d /* lambda: 0d is all weight on query sim */, 
			//				BM25_kernel /* sim */,
			//				PLSR15_kernel /* div */ ));

	//		tests.add( new MMR( docs, 
	//				0.5d /* lambda: 0d is all weight on query sim */, 
	//				BM25_kernel /* sim */,
	//				PLSR15_sph_kernel /* div */ ));

//		tests.add( new MMR( docs, 
//				0.5d /* lambda: 0d is all weight on query sim */, 
//				PLSR20_kernel /* sim */,
//				PLSR20_kernel /* div */ ));

		// Evaluate results of different query processing algorithms
		Evaluator.doEval(Arrays.asList(TREC_QUERIES), docs, 
						 queries, aspects, loss_functions, tests, NUM_RESULTS, "trec68_test");
	}

	///////////////////////////////////////////////////////////////////////////////
	//                              Helper Functions
	///////////////////////////////////////////////////////////////////////////////
	
	public static void ExportQRels(HashMap<String, QueryAspects> aspects2, String filename) {
		try {
			TreeMap<String, QueryAspects> aspects = new TreeMap<String, QueryAspects>(aspects2);
			PrintStream ps = new PrintStream(new FileOutputStream(filename));
			for (Map.Entry<String, QueryAspects> e : aspects.entrySet()) {
				String query = e.getKey();
				QueryAspects qa = e.getValue();
				qa.calcAspectStats();
				for (Map.Entry<String, boolean[]> e2 : qa._aspects.entrySet()) {
					boolean[] aspect_array = e2.getValue();
					if (aspect_array == null) {
						ps.println(query + " 0 " + e2.getKey() + " 0");
						continue;
					}
					//System.out.println(query + " " + e2.getKey() + " " + QueryAspects.getAspectsAsStr(aspect_array));
					for (int i = 0; i < aspect_array.length; i++) {
						if (aspect_array[i])
							ps.println(query + " " + qa.getContiguousID(i) + " " + e2.getKey() + " 1");
					}
				}
			}
			ps.close();
		} catch (Exception e) {
			System.out.println(e);
			e.printStackTrace();
			System.exit(1);
		}
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
							cur_query._name = line.substring(0, line.length() - 1); // Should only be one line
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

	public static HashMap<String,QueryAspects> ReadTRECAspects(String aspect_file, String file_root) {
		
		HashMap<String,QueryAspects> aspects = new HashMap<String,QueryAspects>();
		
		String line = null;
		try {
			BufferedReader br = new BufferedReader(new FileReader(aspect_file));
			
			while ((line = br.readLine()) != null) {
				line = line.trim();
				String[] split = line.split("[\\s]");
				String query_name = split[0].substring(0, split[0].length() - 1);
				String doc_name = split[1];
				String aspect_str = split[split.length-1];
				QueryAspects qa = aspects.get(query_name);
				if (qa == null) {
					qa = new QueryAspects(query_name, new Integer(query_name), file_root + "/" + query_name);
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
