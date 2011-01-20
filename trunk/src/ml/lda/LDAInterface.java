/** Interface to Gregor Heinrich's LDA Gibbs sampler.  This class handles
 *  tokenization, stopword removal (if enabled), stemming (if enabled),
 *  and extracting the learned content from the Gibbs sampler. 
 *   
 * @author Scott Sanner (ssanner@gmail.com)
 */

package ml.lda;

import java.io.*;
import java.util.*;
import java.text.DecimalFormat;

import util.DocUtils;
import util.NumericalUtils;

import nlp.filters.*;

public class LDAInterface {

	public final static boolean DEBUG = false;
	
	public final static int DISPLAY_WORDS = 5;
	public final static boolean REMOVE_STOPWORDS = true;
	public final static boolean DO_STEM = false;
	
	public DecimalFormat _df = new DecimalFormat("#.###");
	
	public SimpleTokenizer _st  = new SimpleTokenizer();
	public StopWordChecker _swc = new StopWordChecker();
	public SnowballStemmer _sbs = new SnowballStemmer();
	
	public ArrayList<ArrayList<Integer>> _alDocs = new ArrayList<ArrayList<Integer>>();
	public ArrayList<ArrayList<String>>  _alDocsFull = new ArrayList<ArrayList<String>>();
	public int[][] _documents;
	
	public HashMap<String,Integer> _str2int  = new HashMap<String,Integer>();
	public HashMap<Integer,String> _int2orig = new HashMap<Integer,String>();
	public int _counter = 0;
	
	public int _M = -1;
	public int _V = -1;
	public int _K = -1;
	
	public double _dAlpha = -1d;
	public double _dBeta  = -1d;
	public double[][] _theta = null;
	public double[][] _phi   = null;
	
	ArrayList<ArrayList<String>> _alTopics;
	
	public LDAInterface() {

	}
	
	public void clearDocuments() {
		_alDocs.clear();
		_str2int.clear();
		_counter = 0;
	}
	
	public int addDocument(File file) {
	
		System.out.print("Loading '" + file.getName() + "'...");
	
		ArrayList<String> words = new ArrayList<String>();
		
		try {
	 		BufferedReader br = new BufferedReader(new FileReader(file));
	 		String line = null;
	 		while ((line = br.readLine()) != null) {
	 			// Seem to get better results using aggressive tokenization
	 			// rather than standard English word tokenization from _st.
	 			words.addAll(DocUtils.Tokenize(line));
	 			//words.addAll(_st.extractTokens(line, true /*lowercase*/));
	 		}
	 		br.close();
		} catch (Exception e) {
			System.out.println(e);
			return -1;
		}
		System.out.println(" done");
		
		return addDocument(words);
	}

	public int addDocument(ArrayList<String> words) {
		ArrayList<Integer> doc = new ArrayList<Integer>();
		ArrayList<String> docFull = new ArrayList<String>();
		for (int i = 0; i < words.size(); i++) {
			String orig_str = words.get(i);
			String pro_str  = words.get(i).toLowerCase();
			
			if (REMOVE_STOPWORDS && _swc.isStopWord(pro_str))
				continue;
			
			if (DO_STEM)
				pro_str = _sbs.stem(pro_str);
			
			Integer word_id = _str2int.get(pro_str);
			if (word_id == null) {
				word_id = _counter++;
				_str2int.put(pro_str, word_id);
				_int2orig.put(word_id, orig_str);
			}
			doc.add(word_id);
			docFull.add(pro_str);
		}
		_alDocs.add(doc);
		_alDocsFull.add(docFull);
		
		return _alDocs.size() - 1;
	}
	
	public String getDocString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < _documents.length; i++) {
			for (int j = 0; j < _documents[i].length; j++) {
				sb.append(_documents[i][j] + "=" + _int2orig.get(_documents[i][j]) + " ");
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	// Use defaults for alpha/beta... see LdaGibbsSampler
	public void infer(int num_topics, long rand_seed) {
		infer(num_topics, 50.0d/num_topics, 0.01d, rand_seed);
	}
	
	public void infer(int num_topics, double alpha, double beta, long rand_seed) {

        _dAlpha = alpha;
        _dBeta  = beta;

		// # Docs
		_M = _alDocs.size();
		
		// Convert ArrayList to int array
		_documents = new int[_M][];
		for (int i = 0; i < _M; i++) {
			int[] doc = new int[_alDocs.get(i).size()];
			for (int j = 0; j < doc.length; j++)
				doc[j] = _alDocs.get(i).get(j);
			_documents[i] = doc;
		}
		if (DEBUG) {
			System.out.println("Original list: " + _alDocsFull);
			System.out.println("Original list: " + _alDocs);
			System.out.println("Original docs:\n" + getDocString());
		}
		
        // Vocabulary size
        _V = _counter;
        
        // # Topics
        _K = num_topics;
        
        // good values alpha = 2, beta = .5
        //double alpha = 2.0d;
        //double beta =  0.5d;

        LdaGibbsSampler lda = new LdaGibbsSampler(_documents, _V, rand_seed);
        // Sample: 1000 (10000)
        // Burn-in: 100 (2000)
        // ThinInterval: DISPLAY (100)
        // Sample Lag: (10)
        lda.configure(1000, 100, 100, 1);
        //lda.configure(10000, 2000, 100, 10);
        lda.gibbs(_K);//, alpha, beta);

        _theta = lda.getTheta();
        _phi   = lda.getPhi();
	}
	
	/* Use the folding-in trick? */
	public double[] getNewDocTopics(String new_doc) {
		
		double[] topics = new double[_K];
		
		// Seem to get better results using aggressive tokenization
		// rather than standard English word tokenization from _st.
		ArrayList<String> words = DocUtils.Tokenize(new_doc);
		//ArrayList<String> words = _st.extractTokens(new_doc, true);
				
		for (String word : words) {		
			String stem_word = _sbs.stem(word);
			Integer w = _str2int.get(stem_word);
			if (w != null)
				for (int k = 0; k < _K; k++) {
					topics[k] += Math.log(_phi[k][w]);
				}
		}

		double log_total = topics[0];
		for (int k = 1; k < _K; k++) {
			log_total = NumericalUtils.LogSum(log_total, topics[k]);
		}
		for (int k = 0; k < _K; k++) {
			//topics[k] = Math.exp(topics[k])/Math.exp(log_total);
			topics[k] = Math.exp(topics[k] - log_total);
		}
		
		return topics;
	}	
	
	public ArrayList<String> getTopicName(int k) {
		if (_alTopics == null)
			buildTopicNames();
		return _alTopics.get(k);
	}

	public String toString() {
				
        StringBuilder sb = new StringBuilder();
		sb.append("\nDocument--Topic Associations, Theta[d][k]\n\n");
        
        sb.append("d\\k\t");
        for (int m = 0; m < _theta[0].length; m++) {
        	sb.append("   " + m % 10 + "    ");
        }
        sb.append("\n");
        for (int m = 0; m < _theta.length; m++) {
        	sb.append(m + "\t");
        	double total = 0d;
            for (int k = 0; k < _theta[m].length; k++) {
                // System.out.print(theta[m][k] + " ");
            	sb.append(LdaGibbsSampler.shadeDouble(_theta[m][k], 1) + " ");
            	total += _theta[m][k];
            }
            sb.append("\t" + _df.format(total) + "\n");
        }
    	sb.append("\t");
        for (int k = 0; k < _K; k++) {
        	double total = 0d;
        	for (int m = 0; m < _M; m++) {
        		total += _theta[m][k];
        	}
        	sb.append("\t" + _df.format(total));
        }
        sb.append("\n");
        sb.append("\n");
        //System.out.println("Topic--Term Associations, Phi[k][w] (beta=" + beta
        //   + ")");

        sb.append("k\\w\t");
        for (int w = 0; w < _phi[0].length; w++) {
        	sb.append((_int2orig.get(w)+"       ").substring(0, 7) + "\t");
        }
        sb.append("\n");
        for (int k = 0; k < _phi.length; k++) {
        	sb.append(k + "\t");
            for (int w = 0; w < _phi[k].length; w++) {
                sb.append(_df.format(_phi[k][w]) + "\t");
            	//sb.append(LdaGibbsSampler.shadeDouble(_phi[k][w], 1) + " ");
            }
            sb.append("\n");
        }
        
        // For each topic k, get the 3 maximal topic words
        buildTopicNames();
        sb.append("\nTopics by sorted words:\n");
        for (int k = 0; k < _phi.length; k++) {
        	sb.append(k + "\t" + _alTopics.get(k) + "\n");
        }
        
        sb.append("\nTopics by top " + DISPLAY_WORDS + " words:\n");
        for (int k = 0; k < _phi.length; k++) {
           	sb.append(k + ": " + _alTopics.get(k) + "\n");
        }
        
        return sb.toString();
	}
		
	public void buildTopicNames() {
        // For each topic k, get the maximal topic words
        _alTopics = new ArrayList<ArrayList<String>>();
        for (int k = 0; k < _phi.length; k++) {
        	ArrayList<String> al_topic = new ArrayList<String>();
        	HashMap<String,Double> topic = new HashMap<String,Double>();
            for (int w = 0; w < _phi[k].length; w++) {
            	topic.put(_int2orig.get(w), _phi[k][w]);
            }
            	            	
			List<Map.Entry<String, Double>> topic_sort = new ArrayList<Map.Entry<String, Double>>(
					topic.entrySet());
			Collections.sort(topic_sort,
			new Comparator<Map.Entry<String, Double>>() {
				public int compare(Map.Entry<String, Double> o1,
						Map.Entry<String, Double> o2) {
					if (o2.getValue() == null && o1.getValue() == null)
						return 0;
					int comp = o2.getValue().compareTo(o1.getValue());
					if (comp != 0)
						return comp;
					String s1 = o1.getKey();
					String s2 = o2.getKey();
					return s1.length() - s2.length();
				}
			});
			//sb.append(topic_sort.toString() + "\n");
			for (int i = 0; i < DISPLAY_WORDS; i++) {
				try { 
					al_topic.add(topic_sort.get(i).getKey());
				} catch (Throwable t) {
					System.out.println("ERROR: cannot get topic word");
					t.printStackTrace();
				}
			}
			_alTopics.add(al_topic);
        }
	}
	
	public static void main(String[] args) {

		LDAInterface lda = new LDAInterface();
		
		File dir = new File("files/testlda");
		
		File[] files = dir.listFiles();
		for (File file : files)
			lda.addDocument(file);
			
        System.out.println("Latent Dirichlet Allocation using Gibbs Sampling.");
		//lda.infer(15 /* NUM TOPICS */, 2.0 /* alpha */, 0.5 /* beta */, 123456 /* rand seed */);
		lda.infer(15 /* NUM TOPICS */, 123456 /* rand seed */);
		
		System.out.println("Results: ");// + lda);
		try {
			// TODO:
			// (1) Store topic and document marginals
			// (2) Sort by topic marginals
			// (3) Look at ratio for words... cutoff given decent dropoff?
			PrintStream ps = new PrintStream(new FileOutputStream("src/ml/lda/lda_out.txt"));
			ps.println(lda);
			ps.close();
		} catch (Exception e) {
			System.err.println(e);
		}
	
		ShowTopicsForQuery(lda, "planning under uncertainty");
		ShowTopicsForQuery(lda, "Internet search");
		ShowTopicsForQuery(lda, "first-order adds");
		ShowTopicsForQuery(lda, "gradient descent");
	}
	
	public static void ShowTopicsForQuery(LDAInterface lda, String query) {
		System.out.println("\n===\nQuery: " + query);
		double[] topics = lda.getNewDocTopics(query);
        for (int k = 0; k < lda._phi.length; k++) {
           System.out.println(k + "[" + lda._df.format(topics[k]) + "]: " + lda.getTopicName(k));
        }
	}
}
