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
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import diversity.ResultListSelector;
import diversity.ScoreRanker;
import diversity.kernel.BM25Kernel;
import diversity.kernel.Kernel;
import diversity.kernel.TF;

import trec.evaldiv.doc.Doc;
import trec.evaldiv.loss.AllUSLoss;
import trec.evaldiv.loss.AllWSLoss;
import trec.evaldiv.loss.AspectLoss;
import trec.evaldiv.loss.NDEval10Losses;
import util.DevNullPrintStream;
import util.VectorUtils;

public class Evaluator {

	public final static String PATH_PREFIX = "files/trec/RESULTS/";
	
	// TODO: Need to optimize number of topics
	// TODO: Verify consistency of rankers when used multiple times with clearDocs
	// TODO: Need to do code profiling, can improve code with caching (e.g., similarity metrics, LDA)

	public static final boolean DEBUG = true;
	
	public static void doEval(
			List<String> query_names, 
			HashMap<String,String> docs, 
			Map<String,Query> query_content, 
			Map<String,QueryAspects> query_aspects,
			List<AspectLoss> loss_functions,
			List<ResultListSelector> tests,
			int num_results,
			String output_filename) throws Exception {
		
		PrintStream ps  = new PrintStream(new FileOutputStream(PATH_PREFIX + output_filename + ".txt"));
		PrintStream ps2 = new PrintStream(new FileOutputStream(PATH_PREFIX + output_filename + ".avg.txt"));
		PrintStream ps3  = new PrintStream(new FileOutputStream(PATH_PREFIX + output_filename + "_ndeval.txt"));
		PrintStream ps4 = new PrintStream(new FileOutputStream(PATH_PREFIX + output_filename + "_ndeval.avg.txt"));
		PrintStream err = new DevNullPrintStream(); //new PrintStream(new FileOutputStream(PATH_PREFIX + output_filename + ".errors.txt"));
	
		// Loop:
		// - go through each test (alg) t (a variant of MMR)
		//     - go through all queries q
		//        - add docs to test t for q
		//        - get result list for query q on test t
		//            - go through all loss functions l
		//                - evaluate loss
		
		int test_num = 1;
		for (ResultListSelector t : tests) {
			
			if (DEBUG)
				System.out.println("- Processing test '" + t.getDescription() + "'");

			// Maintain average US and WSL vectors
			double[] usl_vs_rank = new double[num_results];
			double[] wsl_vs_rank = new double[num_results];
			double[] ndeval = new double[num_results];
			
			// Build a cache for reuse of top-docs
			HashMap<String, HashSet<String>> top_docs = new HashMap<String, HashSet<String>>();
			
			int query_num = 0;
			for (String query : query_names) {

				///////////////////////////////////////////////////////////////////////
				// For a test and a query
				
				// Get query relevant info
				++query_num;
				Query q = query_content.get(query);
				QueryAspects qa = query_aspects.get(query);
				if (DEBUG) {
					System.out.println("- Processing query '" + query + "'");
					System.out.println("- Query details: " + q);
					//System.out.println("- Query aspects: " + qa);
				}

				// Add docs for query to test
				t.clearDocs();
				//Set<String> relevant_docs = qa.getRelevantDocs();
				Set<String> relevant_docs = qa.getAvailableDocs();

				if (DEBUG)
					System.out.println("- Evaluating with " + relevant_docs.size() + " docs");
				
				for (String doc_name : relevant_docs) {
					if (!docs.containsKey(doc_name))
						err.println("ERROR: '" + doc_name + "' not found for '" + query + "'");
					else
						t.addDoc(doc_name);
					//if (DEBUG)
					//	System.out.println("- Adding " + doc_name + " -> " + d.getDocContent());
				}
				
				// Get the results
				if (DEBUG)
					System.out.println("- Running alg: " + t.getDescription());
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
					
					// Maintain averages and export
					if (loss instanceof AllUSLoss) {
						usl_vs_rank = VectorUtils.Sum(usl_vs_rank, (double[])o);
						export(ps, query_num, test_num, 1, (double[])o);
					}
					if (loss instanceof AllWSLoss) {
						wsl_vs_rank = VectorUtils.Sum(wsl_vs_rank, (double[])o);
						export(ps, query_num, test_num, 2, (double[])o);
					}
					if (loss instanceof NDEval10Losses) {
						ndeval = VectorUtils.Sum(ndeval, (double[])o);
						export(ps3, query_num, test_num, 3, (double[])o);
					}
					ps.flush();
					ps3.flush();
				}
				
				///////////////////////////////////////////////////////////////////////
			}
			t.clearDocs();
			
			usl_vs_rank = VectorUtils.ScalarMultiply(usl_vs_rank, 1d/query_names.size());
			wsl_vs_rank = VectorUtils.ScalarMultiply(wsl_vs_rank, 1d/query_names.size());
			ndeval =      VectorUtils.ScalarMultiply(ndeval, 1d/query_names.size());
			
			System.out.println("==================================================");
			System.out.println("Exporting " + test_num + ": " + t.getDescription());
			export(ps2, -1, test_num, 1, usl_vs_rank);
			export(ps2, -1, test_num, 2, wsl_vs_rank);
			export(ps4, -1, test_num, 3, ndeval);
			
			++test_num;
		}
		
		ps.close();
		ps2.close();
		ps3.close();
		ps4.close();
		err.close();
	}
	
	public static void export(PrintStream ps, int query_num, int test_num, int loss_num, double[] v) {
		
		ps.print(query_num + "\t" + test_num + "\t" + loss_num);
		for (int i = 0; i < v.length; i++)
			ps.print("\t" + v[i]);
		ps.println();		
	}
	
}
