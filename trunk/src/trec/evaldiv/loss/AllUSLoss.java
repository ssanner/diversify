/** Loss function implementation for diversity evaluation
 *   
 * @author Scott Sanner (ssanner@gmail.com)
 */

package trec.evaldiv.loss;

import java.util.List;

import trec.evaldiv.QueryAspects;

public class AllUSLoss extends AspectLoss {

	@Override
	public Object eval(QueryAspects qa, List<String> docs) {
		
		double scores[] = new double[docs.size()];
		for (int r = 1; r < docs.size(); r++)
			scores[r-1] = qa.getUniformSubtopicLoss(docs, r); 
		
		return scores;
	}

}
