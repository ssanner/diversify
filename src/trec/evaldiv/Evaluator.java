/** Main Evaluator for TREC Interactive and CLUEWEB Diversity tracks
 *   
 * @author Scott Sanner (ssanner@gmail.com)
 */

package trec.evaldiv;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import diversity.ResultListSelector;
import diversity.ScoreRanker;
import diversity.kernel.BM25Kernel;
import diversity.kernel.Kernel;

import trec.evaldiv.doc.Doc;
import trec.evaldiv.loss.AllUSLoss;
import trec.evaldiv.loss.AllWSLoss;
import trec.evaldiv.loss.AspectLoss;
import util.VectorUtils;

public class Evaluator {

	public final static String PATH_PREFIX = "files/trec/RESULTS/";
	
	// TODO: If use all docs need to do a BM25 query
	// TODO: Need to optimize number of topics
	// TODO: Verify consistency of rankers when used with clearDocs
	public static final boolean USE_ALL_DOCS = true;
	public static final int     NUM_TOP_DOCS = 50;

	public static final boolean DEBUG = true;
	
	public static void doEval(
			List<String> query_names, 
			HashMap<String,Doc> docs, 
			HashMap<String,Query> query_content, 
			HashMap<String,QueryAspects> query_aspects,
			List<AspectLoss> loss_functions,
			List<ResultListSelector> tests,
			int num_results,
			String output_filename) throws Exception {
		
		if (USE_ALL_DOCS)
			output_filename += "_alldocs";
		
		PrintStream ps  = new PrintStream(new FileOutputStream(PATH_PREFIX + output_filename + ".txt"));
		PrintStream ps2 = new PrintStream(new FileOutputStream(PATH_PREFIX + output_filename + ".avg.txt"));
		PrintStream err = new PrintStream(new FileOutputStream(PATH_PREFIX + output_filename + ".errors.txt"));
	
		// Loop:
		// - go through each test t (a variant of MMR)
		//     - go through all queries q
		//        - add docs to test t for q
		//        - get result list for query q on test t
		//            - go through all loss functions l
		//                - evaluate loss
		
		int test_num = 0;
		for (ResultListSelector t : tests) {
			
			if (DEBUG)
				System.out.println("- Processing test '" + t.getDescription() + "'");

			// Maintain average US and WSL vectors
			double[] usl_vs_rank = new double[num_results];
			double[] wsl_vs_rank = new double[num_results];
			
			// Build a cache for reuse of top-docs
			HashMap<String, HashSet<String>> top_docs = new HashMap<String, HashSet<String>>();
			
			int query_num = 0;
			for (String query : query_names) {

				// Get query relevant info
				++query_num;
				Query q = query_content.get(query);
				QueryAspects qa = query_aspects.get(query);
				if (DEBUG) {
					System.out.println("- Processing query '" + query + "'");
					System.out.println("- Query details: " + q);
					//System.out.println("- Query aspects: " + qa);
				}

				// Get top docs if needed
				if (USE_ALL_DOCS && !top_docs.containsKey(query)) {
					
					ScoreRanker s = new ScoreRanker( new BM25Kernel(
							0.5d /* k1 - doc TF */, 
							0.5d /* k3 - query TF */,
							0.5d /* b - doc length penalty */ ));
					
					// Add all available docs to ScoreRanker
					for (Doc d : docs.values()) 
						s.addDoc(d._name, d.getDocContent());
					
					top_docs.put(query, new HashSet<String>( 
						s.getResultList(q.getQueryContent(), NUM_TOP_DOCS) ));
					
				} 
								
				// Add docs for query to test
				t.clearDocs();
				Set<String> relevant_docs = null;
				if (USE_ALL_DOCS)		
					relevant_docs = top_docs.get(query);
				else 
					relevant_docs = qa.getRelevantDocs();
				
				for (String doc_name : relevant_docs) {
					Doc d = docs.get(doc_name);
					if (d == null)
						err.println("ERROR: '" + doc_name + "' not found for '" + query + "'");
					else
						t.addDoc(doc_name, d.getDocContent());
					//if (DEBUG)
					//	System.out.println("- Adding " + doc_name + " -> " + d.getDocContent());
				}
				
				// Get the results
				ArrayList<String> result_list = t.getResultList(q.getQueryContent(), num_results);
				if (DEBUG)
					System.out.println("- Result list: " + result_list);
				
				// Evaluate all loss functions on the results
				for (AspectLoss loss : loss_functions) {
					Object o = loss.eval(qa, result_list);
					String loss_result_str = null;
					if (o instanceof double[]) {
						loss_result_str = VectorUtils.GetString((double[])o);
					} else {
						loss_result_str = o.toString();
					}
					
					// Display results to screen for now
					System.out.println("==================================================");
					System.out.println("Query: " + q._name + " -> " + q.getQueryContent());
					System.out.println("MMR Alg: " + t.getDescription());
					System.out.println("Loss Function: " + loss.getName());
					System.out.println("Evaluation: " + loss_result_str);
					
					// Maintain averages
					if (loss instanceof AllUSLoss) {
						usl_vs_rank = VectorUtils.Sum(usl_vs_rank, (double[])o);
						export(ps, query_num, test_num, 1, (double[])o);
					}
					if (loss instanceof AllWSLoss) {
						wsl_vs_rank = VectorUtils.Sum(wsl_vs_rank, (double[])o);
						export(ps, query_num, test_num, 2, (double[])o);
					}
				}
			}
			t.clearDocs();
			
			usl_vs_rank = VectorUtils.ScalarMultiply(usl_vs_rank, 1d/query_names.size());
			wsl_vs_rank = VectorUtils.ScalarMultiply(wsl_vs_rank, 1d/query_names.size());
			
			System.out.println("==================================================");
			System.out.println("Exporting " + ++test_num + ": " + t.getDescription());
			export(ps2, -1, test_num, 1, usl_vs_rank);
			export(ps2, -1, test_num, 2, wsl_vs_rank);
		}
		
		ps.close();
		ps2.close();
		err.close();
	}
	
	public static void export(PrintStream ps, int query_num, int test_num, int loss_num, double[] v) {
		
		ps.print(query_num + "\t" + test_num + "\t" + loss_num);
		for (int i = 0; i < v.length; i++)
			ps.print("\t" + v[i]);
		ps.println();		
	}
	
}
