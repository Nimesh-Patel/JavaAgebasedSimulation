package microsim.alignment.probability;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;

import microsim.engine.SimulationEngine;

public class SidewalkAlignment<T> extends AbstractProbabilityAlignment<T> {

	@Override
	public void align(List<T> agentList, Predicate filter, AlignmentProbabilityClosure<T> closure, double targetShare) {
		if (targetShare < 0 || targetShare > 1) {
			System.err.println("target probability must lie in [0,1]");
			System.exit(0);
		}
		
		List<T> list = new ArrayList<T>();		
		if (filter != null)
			CollectionUtils.select(agentList, filter, list);
		else
			list.addAll(agentList);
		
		Collections.shuffle(list, SimulationEngine.getRnd());
		int n = list.size();
		double sum = 0;
		
		sum = 0;
		for (int i=0; i<n; i++) {
			T agent = list.get(i);
			
			// update cumulated probability
			sum += closure.getProbability(agent);
			
			// set individual probability to 1 if there is a change of the integer part of the cumulated probability, 0 otherwise
			if (sum == Math.rint(sum)) 
				closure.align(agent, 1.0);				
			else 
				closure.align(agent, 0.0);				
		}
		
	}

}
