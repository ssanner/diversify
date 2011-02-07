package trec.evaldiv.loss;

import java.util.List;

import trec.evaldiv.QueryAspects;

public class USLoss extends AspectLoss {

	@Override
	public Object eval(QueryAspects qa, List<String> docs) {
		return qa.getUniformSubtopicLoss(docs, docs.size());
	}

}
