package trec.evaldiv.loss;

import java.util.List;

import trec.evaldiv.QueryAspects;

public abstract class AspectLoss {

	public String getName() {
		String[] split = this.getClass().toString().split("[\\.]");
		return split[split.length - 1];
	}
	
	public abstract Object eval(QueryAspects qa, List<String> docs);
}
