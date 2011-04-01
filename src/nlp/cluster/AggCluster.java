/**
 * NLP - Cluster: Test of Agglomerative Clustering.
 * 
 * @author Scott Sanner (ssanner@gmail.com)
 * @version 4/1/11
 *
 **/
package nlp.cluster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import util.DocUtils;
import util.MapList;
import util.Pair;

import diversity.kernel.Kernel;
import graph.Graph;

import nlp.filters.StopWordChecker;

public class AggCluster {

	private final String SPLIT_INDICATOR = "___";
	
	StopWordChecker swc = new StopWordChecker();
	
	Kernel _kernel = null;
	HashSet<String> _candidates = new HashSet<String>();
	HashMap<Pair,Double> _pair2Dist = new HashMap<Pair,Double>();
	HashMap<String,String> _docOrig = new HashMap<String,String>();
	HashMap<String,String> _nodeLabel = new HashMap<String,String>();
	HashMap<String,Integer> _nodeCount = new HashMap<String,Integer>();
	
	String _rootNode;
	MapList _parent2Children = null;
	
	public AggCluster(HashMap<String,String> docs, Kernel kernel) {
		_docOrig = docs;
		_kernel = kernel;
	}

	public void addDoc(String doc) {
		_candidates.add(doc);
	}

	public void computeClustering() {
		
		// Initialize the kernel
		_nodeLabel.clear();
		_kernel.clear();
		_kernel.init(_candidates);
		HashSet<String> temp_candidates = (HashSet<String>)_candidates;
		
		// Compute clustering
		_rootNode = null;
		_parent2Children = new MapList();
		
		while (temp_candidates.size() != 0) {
			
			// Find two closest candidates (largest similarity) to merge
			double max_sim = -Double.MAX_VALUE;
			String min_ci = null;
			String min_cj = null;
			ArrayList<String> candidates = new ArrayList<String>(temp_candidates);
			for (int i = 0; i < candidates.size(); i++) {
				String ci = candidates.get(i);
				for (int j = 0; j < candidates.size(); j++) {
					if (i == j)
						continue;
					String cj = candidates.get(j);
					
					double cur_sim = getSimilarity(ci, cj);
					if (cur_sim > max_sim) {
						min_ci = ci;
						min_cj = cj;
						max_sim = cur_sim;
					}
				}
			}
			
			// Now have closest candidates ci and cj... remove them and
			// join into parent node, record node link
			temp_candidates.remove(min_ci);
			temp_candidates.remove(min_cj);
			String ci_cj = min_ci + SPLIT_INDICATOR + min_cj;
			_parent2Children.putValue(ci_cj, min_ci);
			_parent2Children.putValue(ci_cj, min_cj);
			
			System.out.println(">> Adding " + ci_cj);
			
			if (temp_candidates.size() == 0) 
				_rootNode = ci_cj;
			else
				temp_candidates.add(ci_cj);
		}
		
		setNodeLabel(_rootNode);
		setNodeCount(_rootNode);
		
		System.out.println("Final distances: " + _pair2Dist);
	}

	public double getSimilarity(String doc1, String doc2) {
		
		// Look up in a fixed order
		if (doc1.compareTo(doc2) > 0) {
			String temp = doc1;
			doc1 = doc2;
			doc2 = temp;
		}
		
		// Look up in hash table
		Pair key = new Pair(doc1, doc2);
		Double similarity = _pair2Dist.get(key);
		if (similarity == null) {
			
			// Similarity is the max similarity between any pair from (keys1,keys2)
			similarity = -Double.MAX_VALUE;
			String[] keys1 = doc1.split("___");
			String[] keys2 = doc2.split("___");
			for (String s1 : keys1) 
				for (String s2 : keys2) {
					Object o1 = _kernel.getObjectRepresentation(s1);
					Object o2 = _kernel.getObjectRepresentation(s2);
					similarity = Math.max(similarity, _kernel.sim(o1, o2));
				}
			
			// Cache
			_pair2Dist.put(key, similarity);
		}
		
		return similarity;
	}
	
	public ArrayList<String> setNodeLabel(String cur) {
		
		ArrayList<String> cur_node = null;
		
		ArrayList children = _parent2Children.getValues(cur);
		if (children != null && children.size() >= 2) {
			String child1 = (String)children.get(0);
			String child2 = (String)children.get(1);
			
			cur_node = new ArrayList<String>();
			cur_node.addAll(setNodeLabel(child1));
			cur_node.retainAll(setNodeLabel(child2));
			
		} else {
			// This node is a child
			String text = _docOrig.get(cur);
			cur_node = DocUtils.Tokenize(text);
		}

		ArrayList<String> new_node_label = new ArrayList<String>();
		for (int i = 0; i < cur_node.size(); i++) {
			String cur_token = cur_node.get(i);
			if (!swc.isStopWord(cur_token) && !new_node_label.contains(cur_token))
				new_node_label.add(cur_token);
		}
		_nodeLabel.put(cur, new_node_label.toString());
		

		return new_node_label; 
	}
	
	public Integer setNodeCount(String cur) {
		
		int count = 1;
		
		ArrayList children = _parent2Children.getValues(cur);
		if (children != null && children.size() >= 2) {
			String child1 = (String)children.get(0);
			String child2 = (String)children.get(1);
			
			count += setNodeCount(child1);
			count += setNodeCount(child2);
		} 

		_nodeCount.put(cur, count);
		
		return count; 
	}

	public Graph getGraph() {
		Graph g = new Graph(true);
		g.setBottomToTop(false);
		
		for (Object o : _parent2Children.keySet()) {
			
			String parent = (String)o;
			
			ArrayList children = _parent2Children.getValues(o);
			if (children.size() == 0)
				continue;
			
			String child1 = (String)children.get(0);
			boolean child1_box = false;
			if (child1.indexOf(SPLIT_INDICATOR) < 0) {
				child1 = _docOrig.get(child1);
				child1_box = true;
			} else
				child1 = "" + child1.hashCode();

			String child2 = (String)children.get(1);
			boolean child2_box = false;
			if (child2.indexOf(SPLIT_INDICATOR) < 0) {
				child2 = _docOrig.get(child2);
				child2_box = true;
			} else
				child2 = "" + child2.hashCode();
			
			String parent_node_name = "" + parent.hashCode();
			g.addUniLink(parent_node_name, child1);
			if (child1_box) {
				g.addNodeColor(child1, "lightblue");
				g.addNodeStyle(child1, "filled");
				g.addNodeShape(child1, "box");
			}
			g.addUniLink(parent_node_name, child2);
			if (child2_box) {
				g.addNodeColor(child2, "lightblue");
				g.addNodeStyle(child2, "filled");
				g.addNodeShape(child2, "box");
			}
			String parent_label = _nodeLabel.get(parent);
			if (parent_label != null) {
				Integer node_count = _nodeCount.get(parent);
				g.addNodeLabel(parent_node_name, node_count + ": " + parent_label);
				g.addNodeColor(parent_node_name, "salmon");
				g.addNodeStyle(parent_node_name, "filled");
				g.addNodeShape(parent_node_name, "ellipse");
			}
			
			//g.addNodeColor("H", "lightblue");
			//g.addNodeShape("H", "box");
			//g.addNodeStyle("H", "filled");
		}
				
		return g;
	}

}
