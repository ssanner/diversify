/** Loss function implementation for diversity evaluation
 *   
 * @author Scott Sanner (ssanner@gmail.com)
 */

package trec.evaldiv.loss;

import java.util.List;

import trec.evaldiv.QueryAspects;

public class AvgWSLoss extends AspectLoss {

	@Override
	public Object eval(QueryAspects qa, List<String> docs) {
		
		double score_sum = 0d; 
		for (int r = 1; r < docs.size(); r++)
			score_sum += qa.getWeightedSubtopicLoss(docs, r); 
		
		return score_sum / (double)docs.size();
	}

}
