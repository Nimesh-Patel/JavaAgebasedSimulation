package microsim.alignment.outcome;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import microsim.engine.SimulationEngine;
import microsim.event.EventListener;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;

/**
 * @author Ross Richardson
 */
public class ResamplingAlignment<T extends EventListener> extends AbstractOutcomeAlignment<T> {


	//-----------------------------------------------------------------------------------
	//
	// Align share of population (to align absolute numbers, see alternative methods below)
	//
	//------------------------------------------------------------------------------------
	/**
	 * Align share of population by resampling 
	 * 
	 * @param agentList
	 * @param filter
	 * @param closure
	 * @param targetShare
	 */
	public void align(List<T> agentList, Predicate filter, AlignmentOutcomeClosure<T> closure, double targetShare) {
		align(agentList, filter, closure, targetShare, -1);		//No maximum Resampling Attempts specified, so pass a negative number to be handled appropriately within the method.
	}
	
	/**
	 * Align share of population by resampling.  Includes argument specifying the maximum number of attempts before terminating the algorithm.
	 */
	@Override
	public void align(List<T> agentList, Predicate filter, AlignmentOutcomeClosure<T> closure, double targetShare, int maxResamplingAttempts) {
		
		if(targetShare > 1.) {
			targetShare = 1.;
			System.out.println("WARNING! ResamplingAlignment target is greater than 1 (meaning 100%)!  This is impossible, so target will be redefined to be 1.");
			System.out.println(Arrays.toString(Thread.currentThread().getStackTrace()));
		} else if(targetShare < 0.) {
			targetShare = 0.;
			System.out.println("WARNING! ResamplingAlignment target is negative!  This is impossible, so target will be redefined to be 0.");
			System.out.println(Arrays.toString(Thread.currentThread().getStackTrace()));
		}
		
		List<T> list = new ArrayList<T>();		
		if (filter != null)
			CollectionUtils.select(agentList, filter, list);
		else
			list.addAll(agentList);
		
		Collections.shuffle(list, SimulationEngine.getRnd());
		int n = list.size();
		double sum = 0;
		
		// compute total number of simulated positive outcomes
		for (int i=0; i<n; i++) {
			T agent = list.get(i);
			sum += (closure.getOutcome(agent) ? 1 : 0); 
		}
		double avgResampleAttemptPerCapita = 20.; 
		if(maxResamplingAttempts < sum) {			//This will catch the case where maxResamplingAttempts is not included in the arguments.  Also it provides a lower bound for the user to specify, which is the size of the subset of the population whose outcomes need changing.  Anything less, and the number is automatically enlarged (in the line below).
			maxResamplingAttempts = (int)(avgResampleAttemptPerCapita * sum);	//This creates a default value of 20 times the size of the subset of the population to be resampled in order to move the delta towards 0 by 1.  Therefore, in order to improve delta by 1, a member of the population undergoing alignment will be resampled up to a maximum of 20 times on average in order to change their outcome, before the alignment algorithm will give up and terminate.  
		}
		
//		if(sum == 0) {
//			System.out.println("Warning!  The filtered population of objects passed to the Resampling Alignment algorithm all have false outcomes initially, which means that the existing heterogeneity of the objects is not enough to provide variation in the outcomes of the alignment.  It may be the case that resampling them will not produce enough changes in outcomes to reach the alignment target.  Check the procedure for calculating the outcomes to see if there is any reason that only one of the outcomes occurs.");
//			System.out.println(Arrays.toString(Thread.currentThread().getStackTrace()));
//		} else if(sum == n) {
//			System.out.println("Warning!  The filtered population of objects passed to the Resampling Alignment algorithm all have true outcomes initially, which means that the existing heterogeneity of the objects is not enough to provide variation in the outcomes of the alignment.  It may be the case that resampling them will not produce enough changes in outcomes to reach the alignment target.  Check the procedure for calculating the outcomes to see if there is any reason that only one of the outcomes occurs.");
//			System.out.println(Arrays.toString(Thread.currentThread().getStackTrace()));
//		}
		
		// compute difference between simulation and target
		double delta = sum - targetShare * n;
//		System.out.println("start delta is ," + delta + " and size of list is " + list.size() + " sum is ," + sum);
		
		int count = 0;
		
		// if too many positive outcomes (delta is positive)
		if(delta > 0) {
			while ((Math.abs(delta) > 1.) && (count < maxResamplingAttempts)) {
				T agent = list.get(SimulationEngine.getRnd().nextInt(list.size()));
				//			System.out.println("count " + count);
				if (closure.getOutcome(agent)) {
					count++;
					closure.resample(agent);
					if (!closure.getOutcome(agent)) { 
						delta--;
//						System.out.println("delta is now," + delta + " count was " + count);
						count = 0;
					}
				}

			}
		}
		else if(delta < 0) {	// if too few positive outcomes (delta is negative)
			while ((Math.abs(delta) > 1.) && (count < maxResamplingAttempts)) {
				T agent = list.get(SimulationEngine.getRnd().nextInt(list.size()));
				if (!closure.getOutcome(agent)) {	
					count++;
					closure.resample(agent);
					if (closure.getOutcome(agent)) { 	
						delta++;
//						System.out.println("delta is now," + delta + " count was " + count);
						count = 0;
					}
				}
			}
		}
		
		if(count >= maxResamplingAttempts) { 
			System.out.println("Resampling Alignment Algorithm has reached the maximum number of resample attempts (on average, " + avgResampleAttemptPerCapita + " attempts per object to be aligned) and has terminated.  Alignment may have failed.  The difference between the population in the system with the desired outcome and the target number is " + delta + " (" + (delta*100./((double)n)) + " percent).  If this is too large, check the resampling method and the subset of population to understand why not enough of the population are able to change their outcomes.");
			System.out.println(Arrays.toString(Thread.currentThread().getStackTrace()));
		}
//		System.out.println("final delta is ," + delta);
		
	}
	
	//-----------------------------------------------------------------------------------
	//
	// Align absolute numbers
	//
	//------------------------------------------------------------------------------------
	/**
	 * Align share of population by resampling 
	 * 
	 * @param agentList
	 * @param filter
	 * @param closure
	 * @param targetShare
	 */
	public void align(List<T> agentList, Predicate filter, AlignmentOutcomeClosure<T> closure, int targetNumber) {
		align(agentList, filter, closure, targetNumber, -1);		//No maximum Resampling Attempts specified, so pass a negative number to be handled appropriately within the method.
	}
	
	/**
	 * Align share of population by resampling.  Includes argument specifying the maximum number of attempts before terminating the algorithm.
	 */
//	@Override
	public void align(List<T> agentList, Predicate filter, AlignmentOutcomeClosure<T> closure, int targetNumber, int maxResamplingAttempts) {
		
		if(targetNumber > agentList.size()) {
			targetNumber = agentList.size();
			System.out.println("WARNING! ResamplingAlignment targetNumber is larger than the population size!  This is impossible to reach, so target number will be redefined to be the population size.");
			System.out.println(Arrays.toString(Thread.currentThread().getStackTrace()));
		} else if(targetNumber < 0) {
			targetNumber = 0;
			System.out.println("WARNING! ResamplingAlignment target is negative!  This is impossible to reach, so target number will be redefined to be 0.");
			System.out.println(Arrays.toString(Thread.currentThread().getStackTrace()));
		}
		
		List<T> list = new ArrayList<T>();		
		if (filter != null)
			CollectionUtils.select(agentList, filter, list);
		else
			list.addAll(agentList);
		
		Collections.shuffle(list, SimulationEngine.getRnd());
		int n = list.size();
		int sum = 0;
		
		// compute total number of simulated positive outcomes
		for (int i=0; i<n; i++) {
			T agent = list.get(i);
			sum += (closure.getOutcome(agent) ? 1 : 0); 
		}
		int avgResampleAttemptPerCapita = 20; 
		if(maxResamplingAttempts < sum) {			//This will catch the case where maxResamplingAttempts is not included in the arguments.  Also it provides a lower bound for the user to specify, which is the size of the subset of the population whose outcomes need changing.  Anything less, and the number is automatically enlarged (in the line below).
			maxResamplingAttempts = avgResampleAttemptPerCapita * sum;	//This creates a default value of 20 times the size of the subset of the population to be resampled in order to move the delta towards 0 by 1.  Therefore, in order to improve delta by 1, a member of the population undergoing alignment will be resampled up to a maximum of 20 times on average in order to change their outcome, before the alignment algorithm will give up and terminate.  
		}
		
//		if(sum == 0) {
//			System.out.println("Warning!  The filtered population of objects passed to the Resampling Alignment algorithm all have false outcomes initially, which means that the existing heterogeneity of the objects is not enough to provide variation in the outcomes of the alignment.  It may be the case that resampling them will not produce enough changes in outcomes to reach the alignment target.  Check the procedure for calculating the outcomes to see if there is any reason that only one of the outcomes occurs.");
//			System.out.println(Arrays.toString(Thread.currentThread().getStackTrace()));
//		} else if(sum == n) {
//			System.out.println("Warning!  The filtered population of objects passed to the Resampling Alignment algorithm all have true outcomes initially, which means that the existing heterogeneity of the objects is not enough to provide variation in the outcomes of the alignment.  It may be the case that resampling them will not produce enough changes in outcomes to reach the alignment target.  Check the procedure for calculating the outcomes to see if there is any reason that only one of the outcomes occurs.");
//			System.out.println(Arrays.toString(Thread.currentThread().getStackTrace()));
//		}
		
		// compute difference between simulation and target
		double delta = sum - targetNumber;
//		System.out.println("start delta is ," + delta + " and size of list is " + list.size() + " sum is ," + sum);
		
		int count = 0;
		
		// if too many positive outcomes (delta is positive)
		if(delta > 0) {
			while ( (delta > 0) && (count < maxResamplingAttempts) ) {
				T agent = list.get(SimulationEngine.getRnd().nextInt(list.size()));
				//			System.out.println("count " + count);
				if (closure.getOutcome(agent)) {
					count++;
					closure.resample(agent);
					if (!closure.getOutcome(agent)) { 
						delta--;
//						System.out.println("delta is now," + delta + " count was " + count);
						count = 0;
					}
				}

			}
		}
		else if(delta < 0) {	// if too few positive outcomes (delta is negative)
			while ( (delta < 0) && (count < maxResamplingAttempts) ) {
				T agent = list.get(SimulationEngine.getRnd().nextInt(list.size()));
				if (!closure.getOutcome(agent)) {	
					count++;
					closure.resample(agent);
					if (closure.getOutcome(agent)) { 	
						delta++;
//						System.out.println("delta is now," + delta + " count was " + count);
						count = 0;
					}
				}
			}
		}
		
		if(count >= maxResamplingAttempts) { 
			System.out.println("Resampling Alignment Algorithm has reached the maximum number of resample attempts (on average, " + avgResampleAttemptPerCapita + " attempts per object to be aligned) and has terminated.  Alignment may have failed.  The difference between the population in the system with the desired outcome and the target number is " + delta + " (" + (delta*100./((double)n)) + " percent).  If this is too large, check the resampling method and the subset of population to understand why not enough of the population are able to change their outcomes.");
			System.out.println(Arrays.toString(Thread.currentThread().getStackTrace()));
		}
//		System.out.println("final delta is ," + delta);
		
	}

}
