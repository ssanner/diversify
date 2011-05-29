/** Frequently used document utilities (reading files, tokenization, extraction
 *  of TF vectors).
 *   
 * @author Scott Sanner (ssanner@gmail.com)
 */

package util;

import java.io.*;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import nlp.filters.SimpleTokenizer;

public class DocUtils {
	
	public static final SimpleTokenizer ST = new SimpleTokenizer();
	
	public static final String SPLIT_TOKENS = "[!\"#$%&'()*+,./:;<=>?\\[\\]^`{|}~\\s]"; // missing: [_-@]
		
	public final static DecimalFormat DF2 = new DecimalFormat("#.##");
	public final static DecimalFormat DF3 = new DecimalFormat("#.###");

	public static String ReadFile(File f) {
		return ReadFile(f, false);
	}
	
	public static String ReadFile(File f, boolean keep_newline) {
		try {
			StringBuilder sb = new StringBuilder();
			java.io.BufferedReader br = new BufferedReader(new FileReader(f));
			String line = null;
			while ((line = br.readLine()) != null) {
				//System.out.println(line);
				sb.append((sb.length()> 0 ? (keep_newline ? "\n" : " ") : "") + line);
			}
			br.close();
			return sb.toString();
		} catch (Exception e) {
			System.out.println("ERROR: " + e);
			return null;
		}
	}

	// Note: this split-based tokenizer breaks words down into smaller 
	//       components than ST but the results seem improved.
	public static ArrayList<String> Tokenize(String sent) {
		ArrayList<String> result = new ArrayList<String>();
		String tokens[] = sent.split(SPLIT_TOKENS); 
		//ArrayList<String> tokens = ST.extractTokens(sent, true);
		for (String token : tokens) {
			token = token.trim().toLowerCase();
			if (token.length() == 0)
				continue;
			result.add(token);
		}
		return result;
	}
	
	// Return a feature map for a sentence
	// Note: this split-based tokenizer breaks words down into smaller 
	//       components than ST but the results seem improved.
	public static Map<Object,Double> ConvertToFeatureMap(String sent) {
		Map<Object,Double> map = new HashMap<Object,Double>();
		String tokens[] = sent.split(SPLIT_TOKENS);
		//ArrayList<String> tokens = ST.extractTokens(sent, true);
		for (String token : tokens) {
			token = token.trim().toLowerCase();
			if (token.length() == 0)
				continue;
			if (map.containsKey(token))
				map.put(token, map.get(token) + 1d);
			else
				map.put(token, 1d);
		}
		return map;
	}
}
