/**
 * NLP - Cluster: Test of Agglomerative Clustering.
 * 
 * @author Scott Sanner (ssanner@gmail.com)
 * @version 4/1/11
 *
 **/
package nlp.cluster;

import graph.Graph;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.text.DecimalFormat;

import util.DocUtils;

import diversity.MMR;
import diversity.ResultListSelector;
import diversity.ScoreRanker;
import diversity.kernel.BM25Kernel;
import diversity.kernel.LDAKernel;
import diversity.kernel.PLSRKernel;
import diversity.kernel.Kernel;
import diversity.kernel.TF;
import diversity.kernel.TFIDF;

public class AggClusterTest {

	public final static String DOC_NAME = "files/group/gt2.txt";
	public final static String HEADING_FILTER = "1";
	
	public final static int MAX_LINE_LENGTH = 80;
	
	//////////////////////////////////////////////////////////////////
	//                          Test Code
	//////////////////////////////////////////////////////////////////
	
	/** TestDiversity.main()
	 * 
	 * Requires 3 arguments: 
	 * 
	 *   arg 1: directory of files to rank
	 *   arg 2: directory for output
	 *   arg 3: query (enclose in 'single quotes')
	 * 
	 * @param args
	 * @throws FileNotFoundException
	 */
	public static void main(String[] args) throws Exception {
		
		// Set this to false to turn off display of computations
		ResultListSelector.SHOW_DEBUG = false;

		// Get docs
		HashMap<String,String> docs = GetDocs(DOC_NAME);
		
		// Determine number of topics based on data size for HEADING_FILTER
		int doc_count = 0;
		for (String doc : docs.keySet())
			if (doc.startsWith(HEADING_FILTER))
				doc_count++;
		int NUM_TOPICS = (int)((doc_count / 5d) + .5d);
		System.out.println("Number of documents / topics: " + doc_count + " / " + NUM_TOPICS);
		
		// Build a set of AggCluster algorithms with different kernels
		ArrayList<AggCluster> algs = new ArrayList<AggCluster>();

		// Instantiate all the kernels that we will use with the algorithms below
		Kernel TF_kernel    = new TF(docs, true /* query-relevant diversity */);
		Kernel TFIDF_kernel = new TFIDF(docs, true /* query-relevant diversity */);
		Kernel LDA_kernel   = new LDAKernel(docs, NUM_TOPICS /* NUM TOPICS - suggest 15 */, true /* spherical */, true /* query-relevant diversity */);
		Kernel BM25_kernel  = 
			new BM25Kernel(docs, 
				/* 0 for any disables effect */
				0.5d /* k1 - doc TF */, 
				0.5d /* k3 - query TF */,
				0.5d /* b - doc length penalty */ );
		
		// Add all MMR test variants (vary lambda and kernels)
		algs.add( new AggCluster(docs, TF_kernel) );
		algs.add( new AggCluster(docs, TFIDF_kernel) );
		algs.add( new AggCluster(docs, BM25_kernel) );
		algs.add( new AggCluster(docs, LDA_kernel) );
				
		// Add documents to each test in tests
		for (AggCluster alg : algs) {
			for (String doc : docs.keySet()) {
				if (doc.startsWith(HEADING_FILTER)) {
					//System.out.println("Adding: " + doc);
					alg.addDoc(doc);
				} //else
				//	System.out.println("Not adding: " + doc);
			}
		}
		
		// For each test in tests, build a ranked result list w.r.t. query 
		// and display results (both on stdout and exported to a file)
		for (AggCluster alg : algs) {
			alg.computeClustering();
			alg.getGraph().launchViewer(1024, 768);
			//System.in.read();
		}
		//ps.close();
	}
	
	//////////////////////////////////////////////////////////////////
	//                           Support Code
	//////////////////////////////////////////////////////////////////
	
	public static HashMap<String,String> GetDocs(String file) {
		
		HashMap<String,String> docs = new HashMap<String,String>();
		
		// Load docs from a directory		
		String content = DocUtils.ReadFile(new File(file), true);
		if (content == null) 
			System.out.println("ERROR: Could not read content for: " + file);
		else {
			//System.out.println(content); 
			String last_heading = null;
			String last_content = null;
			String[] split = content.split("\\n");
			for (String line : split) {
				line = line.trim();
				if (line.length() < 10 && line.matches("(\\d+\\.)*(\\d)+")) {
					last_heading = line;
				}
				else if (!line.startsWith("Submitted by")) {
					if (last_content == null)
						last_content = line;
					else last_content += " " + line;
				}
				else {
					docs.put(last_heading, last_content);
					System.out.println(last_heading + " -> " + last_content);
					last_heading = null;
					last_content = null;
				}
			}
			//System.exit(1);
		}
		
		return docs;
	}

	public static void ShowQueryResults(PrintStream ps2, ResultListSelector d, String query, int result_sz) {
	
		ArrayList<String> result_list = d.getResultList(query, result_sz);
		PrintStream[] printstreams = new PrintStream[] { System.out, ps2 };
		
		// Output to all printstreams (for now, stdout and a file)
		for (PrintStream ps : printstreams) {
			if (ps == null)
				continue;
				
			ps.println("\n// Query: '" + query + "'");
			ps.println("// ===");
			String query_sim_str = "";
			String query_div_str = "";
			if (d instanceof MMR) {
				Object query_sim_rep = ((MMR)d)._sim.getNoncachedObjectRepresentation(query); 
				query_sim_str = ((MMR)d)._sim.getObjectStringDescription(query_sim_rep);
				query_sim_str = query_sim_str.replace("\n", "\n// ");
				Object query_div_rep = ((MMR)d)._div.getNoncachedObjectRepresentation(query); 
				query_div_str = ((MMR)d)._div.getObjectStringDescription(query_div_rep);
				query_div_str = query_div_str.replace("\n", "\n// ");
			}
			ps.println("// ===\n// Sim representation of query: " + query_sim_str);
			ps.println("// ===\n// Div representation of query: " + query_div_str);
			ps.println("// ===\n// Result list: " + d.getDescription() + "\n// ===");
			for (int i = 0; i < result_list.size(); i++) {
				String content =   d._docs.get(result_list.get(i));
				if (content.length() > MAX_LINE_LENGTH)
					content = content.substring(0,MAX_LINE_LENGTH);
				ps.println((i+1) + "\t" + result_list.get(i) + "\t" + content);
			}
			ps.println();
		}
	}

	public static String CleanString(String s) {
		StringBuilder sb = new StringBuilder();
		char[] chars = s.toCharArray();
		char last_char = ' ';
		for (int i = 0; i < chars.length; i++) {
			if (chars[i] == ' ' && last_char != ' ')
				sb.append('_');
			else if (Character.isLetterOrDigit(chars[i]))
				sb.append(chars[i]);
			last_char = chars[i];
		}
		return sb.toString();
	}

	public static void Usage(String[] args) {
		System.out.println("\nYou provided arguments:");
		for (int i = 0; i < args.length; i++)
			System.out.println("arg[" + i + "] = '" + args[i] + "'");

		System.out.println("\nUsage: TestDiversity directory_of_files output_dir 'query'");
	}

}
