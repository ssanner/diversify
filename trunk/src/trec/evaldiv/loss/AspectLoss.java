package trec.evaldiv.loss;

import java.util.List;

import trec.evaldiv.QueryAspects;

public abstract class AspectLoss {

	public abstract Object eval(QueryAspects qa, List<String> docs);
}
