package trec.evaldiv.loss;

import java.util.List;

import trec.evaldiv.QueryAspects;

public class AllWSLoss extends AspectLoss {

	@Override
	public Object eval(QueryAspects qa, List<String> docs) {
		
		double scores[] = new double[docs.size()];
		for (int r = 1; r < docs.size(); r++)
			scores[r-1] = qa.getWeightedSubtopicLoss(docs, r); 
		
		return scores;
	}

}
