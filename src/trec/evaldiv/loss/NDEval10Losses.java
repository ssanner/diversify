/** Loss function implementation for diversity evaluation
 *   
 * @author Scott Sanner (ssanner@gmail.com)
 */

package trec.evaldiv.loss;

import java.io.*;
import java.util.*;

import trec.evaldiv.QueryAspects;

public class NDEval10Losses extends AspectLoss {

	public final static String NDEVAL = "src/trec/evaldiv/loss/ndeval10" + 
		(System.getProperty("os.name").toLowerCase().startsWith("windows") ? ".exe" : "");
	public final static String NDEVAL_TMP = "ndeval_tmp.txt";

	public String NDEVAL_QRELS = null;

	// 51 Q0 clueweb09-en0007-89-20413 1 24.0733 OKAPI-RUN
	// 51 Q0 clueweb09-en0007-81-08143 2 24.047 OKAPI-RUN
	// 51 Q0 clueweb09-en0007-17-32780 3 23.8035 OKAPI-RUN

	public NDEval10Losses(String qrels) {
		NDEVAL_QRELS = qrels;
	}
	
	@Override
	public String getName() {
		return "NDEval10";
	}
	
	@Override
	public Object eval(QueryAspects qa, List<String> docs) {
		
        Process p;
        double[] ret_val = null;
		try {

			// First export the format needed by ndeval10
			PrintStream ndeval_tmp = new PrintStream(new FileOutputStream(NDEVAL_TMP));
			int rank = 1;
			for (String doc : docs) {
				ndeval_tmp.println(qa._queryID + " Q0 " + doc + " " + rank + " " + (100-rank) + " ANON_ALG");
				++rank;
			}
			ndeval_tmp.close();
			
			// Next call ndeval10 and get the results
			//System.out.println("> " + NDEVAL + " " + NDEVAL_QRELS + " " + NDEVAL_TMP);
			p = Runtime.getRuntime().exec(NDEVAL + " " + NDEVAL_QRELS + " " + NDEVAL_TMP);
	        BufferedReader process_out = new BufferedReader(new InputStreamReader(p.getInputStream()));
	        BufferedReader process_out_err = new BufferedReader(new InputStreamReader(p.getErrorStream()));
	        PrintWriter    process_in  = new PrintWriter(p.getOutputStream(), true);
	
	        // Provide input to process (could come from any stream)
	        //for (String line : process_input) {
	        //        process_in.println(line);
	        //}
	        process_in.close(); // Need to close input stream so process exits!!!
	
	        // Get output from process (can also be used by BufferedReader to get
	        // line-by-line... see how fis_reader is constructed).
	        String line = null;
	        //System.out.println("NDEVAL output");
	        while ((line = process_out.readLine()) != null) {
	            // process line output by process
	        	//System.out.println("NDEval: " + line);
	            if (line.startsWith("ANON_ALG") && !(line.indexOf("amean") >= 0)) {
	            	String[] split = line.split(",");
	            	ret_val = new double[split.length - 2];
	            	if (split.length != 23) {
	            		System.out.println("Unexpected output length: " + split.length);
	            		for (int i = 0; i < split.length; i++)
		            		System.out.println(i + " " + split[i]);
	            		System.exit(1);
	            	}
	            	for (int i = 2; i < split.length; i++) {
	            		ret_val[i - 2] = new Double(split[i]);
	            		//System.out.println((i-2) + " " + ret_val[i - 2]);
	            	}
	            }
	        }
	        process_out.close();
	        
	        while ((line = process_out_err.readLine()) != null) {
	            // process error output by process
	            System.err.println(line);
	        }
	        process_out_err.close();
	        
	        // Wait for the process to exit
	        // runid,topic,ERR-IA@5,ERR-IA@10,ERR-IA@20,nERR-IA@5,nERR-IA@10,
	        //            nERR-IA@20,alpha-DCG@5,alpha-DCG@10,alpha-DCG@20,
	        //            alpha-nDCG@5,alpha-nDCG@10,alpha-nDCG@20,NRBP,nNRBP,
	        //            MAP-IA,P-IA@5,P-IA@10,P-IA@20,strec@5,strec@10,strec@20

	        p.waitFor();
	        
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println(e);
			e.printStackTrace();
		}
		
		return ret_val;
	}

}
