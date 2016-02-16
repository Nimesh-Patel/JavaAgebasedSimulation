package microsim.statistics.regression;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;





import microsim.data.MultiKeyCoefficientMap;
import microsim.engine.SimulationEngine;
import microsim.statistics.IDoubleSource;
import cern.jet.random.Normal;
import cern.jet.random.engine.MersenneTwister;

public class MultiProbitRegression<T extends Enum<T>> implements IMultipleChoiceRegression<T> {

	private Random random;
	
	private Normal normalRV;
	
	private Map<T, MultiKeyCoefficientMap> maps = null;
		
	public MultiProbitRegression(Map<T, MultiKeyCoefficientMap> maps) {		
		random = SimulationEngine.getRnd();
		this.maps = maps;
		for(T event : maps.keySet()) {
			for(Object key : (maps.get(event)).keySet()) {
				if((maps.get(event)).getValue(key).equals(0)) {
					System.out.print("\nWarning - Regression object has been created with a regression co-efficient of 0 for regressor " + key.toString() + ". Check whether it is desired to have a regression co-efficient of 0 (which means the regressor has no influence on the regression score).  Although the simulation will run, consider removing unnecessary regression co-efficients to improve computational efficiency.  Stack Trace is:"
							+ "\n" + Arrays.toString(Thread.currentThread().getStackTrace()));
				}
			}
		}
		normalRV = new Normal(0.0, 1.0, new MersenneTwister(random.nextInt()));
	}

	public MultiProbitRegression(Map<T, MultiKeyCoefficientMap> maps, Random random) {			
		this.random = random;
		this.maps = maps;
		for(T event : maps.keySet()) {
			for(Object key : (maps.get(event)).keySet()) {
				if((maps.get(event)).getValue(key).equals(0)) {
					System.out.print("\nWarning - Regression object has been created with a regression co-efficient of 0 for regressor " + key.toString() + ". Check whether it is desired to have a regression co-efficient of 0 (which means the regressor has no influence on the regression score).  Although the simulation will run, consider removing unnecessary regression co-efficients to improve computational efficiency.  Stack Trace is:"
							+ "\n" + Arrays.toString(Thread.currentThread().getStackTrace()));
				}
			}
		}
		normalRV = new Normal(0.0, 1.0, new MersenneTwister(random.nextInt()));
	}
	
	/**
	 *
	 * Warning - only use when MultiProbitRegression's maps field has values that are MultiKeyCoefficientMaps with only one key.  This method only looks at the first key of the MultiKeyCoefficientMap field of LinearRegression, so any other keys that are used to distinguish a unique multiKey (i.e. if the first key occurs more than once) will be ignored! If the first key of the multiKey appears more than once, the method would return an incorrect value, so will throw an exception.   
	 * @param values
	 * @return
	 */
	public double getProbitTransformOfScore(T event, Map<String, Double> values) {		
		final double score = LinearRegression.computeScore(maps.get(event), values);		
		return (double) normalRV.cdf(score);
	}
	
	public double getProbitTransformOfScore(T event, Object individual) {
		final double score = LinearRegression.computeScore(maps.get(event), individual);
		return (double) normalRV.cdf(score);		
	}
	
	//Original version was incorrect - did not normalise probabilities.  Corrected by Ross Richardson.
//	@Override
	public T eventType(Object individual) {		
		Map<T, Double> probs = new LinkedHashMap<T, Double>();

		double denominator = 0.; 
		
		for (T event : maps.keySet()) {
			double probitTransformOfScore = getProbitTransformOfScore(event, individual);
			probs.put(event, probitTransformOfScore);
			denominator += probitTransformOfScore;			
		}
		
		//Check whether there is a base case that has not been included in the regression specification variable (maps).
		T k = null;
		T[] eventProbs = (T[]) k.getClass().getEnumConstants();
		int countNullEventProbs = 0;
		for (int i = 0; i < eventProbs.length; i++) {
			if (probs.get(eventProbs[i]) == null) {						//The multiprobit regression can go without specifying coefficients for 1 of the outcomes as the probability of this event can be determined by the residual of the other probabilities.
				countNullEventProbs++;									//Check no more than one event has null prob, so that it is valid to take the residual to find the probability
				if(countNullEventProbs > 1) {
					throw new RuntimeException("countNullEventProbs > 1 in MultiProbitRegression object!  More than one event does not have a probability, so the residual cannot be used for the missing probabilities.");
				}
				else {
					denominator += 0.5;		//We include the base case, where score = 0 (as betas are set to zero).  The normalRV.cdf(0) = 0.5 (as the standard normal distribution is symmetric).  The other cases have already been incremented into the denominator.
					probs.put((T) eventProbs[i], 0.5/denominator);		//The normalised probability of the base case is 0.5/denominator as the 0.5 comes from applying the probit transform (the cumulative standard normal distribution) to the score of 0, and the denominator is the sum of probit transforms for all events.				
				}						
			}			
		}
		
		//Normalise the probabilities of the events specified in the regression maps
		for (T event : maps.keySet()) {		//Only iterate through the cases specified in the regression maps - the base case has already been normalised.
			double probitTransformOfScoreForEvent = probs.get(event);
			probs.put(event, probitTransformOfScoreForEvent/denominator);		//Normalise the probit transform of score (the application of the standard normal cumulative distribution to the score) of the event by the sum for all events
		}
		
		double[] probArray = new double[probs.size()]; 
		for (int i = 0; i < eventProbs.length; i++) {
			probArray[i] = probs.get(eventProbs[i]);
		}
		
		return RegressionUtils.event(eventProbs, probArray, random);				
	}

//	@Override
	/**
	 *
	 * Warning - only use when MultiProbitRegression's maps field has values that are MultiKeyCoefficientMaps with only one key.  This method only looks at the first key of the MultiKeyCoefficientMap field of LinearRegression, so any other keys that are used to distinguish a unique multiKey (i.e. if the first key occurs more than once) will be ignored! If the first key of the multiKey appears more than once, the method would return an incorrect value, so will throw an exception.   
	 * @param values
	 * @return
	 */
	public T eventType(Map<String, Double> values) {
		Map<T, Double> probs = new LinkedHashMap<T, Double>();

		double denominator = 0.0;

		for (T event : maps.keySet()) {
			double probitTransformOfScore = getProbitTransformOfScore(event, values);
			probs.put(event, probitTransformOfScore);
			denominator += probitTransformOfScore;			
		}

		//Check whether there is a base case that has not been included in the regression specification variable (maps).
		T k = null;
		T[] eventProbs = (T[]) k.getClass().getEnumConstants();
		int countNullEventProbs = 0;
		for (int i = 0; i < eventProbs.length; i++) {
			if (probs.get(eventProbs[i]) == null) {						//The multiprobit regression can go without specifying coefficients for 1 of the outcomes as the probability of this event can be determined by the residual of the other probabilities.
				countNullEventProbs++;									//Check no more than one event has null prob, so that it is valid to take the residual to find the probability
				if(countNullEventProbs > 1) {
					throw new RuntimeException("countNullEventProbs > 1 in MultiProbitRegression object!  More than one event does not have a probability, so the residual cannot be used for the missing probabilities.");
				}
				else {
					denominator += 0.5;		//We include the base case, where score = 0 (as betas are set to zero).  The normalRV.cdf(0) = 0.5 (as the standard normal distribution is symmetric).  The other cases have already been incremented into the denominator.
					probs.put((T) eventProbs[i], 0.5/denominator);		//The normalised probability of the base case is 0.5/denominator as the 0.5 comes from applying the probit transform (the cumulative standard normal distribution) to the score of 0, and the denominator is the sum of probit transforms for all events.				
				}						
			}			
		}

		//Normalise the probabilities of the events specified in the regression maps
		for (T event : maps.keySet()) {		//Only iterate through the cases specified in the regression maps - the base case has already been normalised.
			double probitTransformOfScoreForEvent = probs.get(event);
			probs.put(event, probitTransformOfScoreForEvent/denominator);		//Normalise the probit transform of score (the application of the standard normal cumulative distribution to the score) of the event by the sum for all events
		}

		double[] probArray = new double[probs.size()]; 
		for (int i = 0; i < eventProbs.length; i++) {
			probArray[i] = probs.get(eventProbs[i]);
		}

		return RegressionUtils.event(eventProbs, probArray, random);		
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////
	// New methods 
	// @author Ross Richardson
	//////////////////////////////////////////////////////////////////////////////////////////////
	
	public <E extends Enum<E>> double getProbitTransformOfScore(T event, IDoubleSource iDblSrc, Class<E> Regressors) {
		final double score = LinearRegression.computeScore(maps.get(event), iDblSrc, Regressors);
		return (double) normalRV.cdf(score);		
	}
	
	public <E extends Enum<E>> T eventType(IDoubleSource iDblSrc, Class<E> Regressors, Class<T> enumType) {		
		Map<T, Double> probs = new LinkedHashMap<T, Double>();

		double denominator = 0.; 
		
		for (T event : maps.keySet()) {
			double probitTransformOfScore = getProbitTransformOfScore(event, iDblSrc, Regressors);
			probs.put(event, probitTransformOfScore);
			denominator += probitTransformOfScore;			
		}
		
		//Check whether there is a base case that has not been included in the regression specification variable (maps).
//		T k = null;
//		T[] eventProbs = (T[]) k.getClass().getEnumConstants();		//Results in Null Pointer Exception
		T[] eventProbs = enumType.getEnumConstants();
//		T[] eventProbs = (T[]) maps.keySet().getClass().getEnumConstants();		//Results in Null Pointer Exception

		int countNullEventProbs = 0;
		for (int i = 0; i < eventProbs.length; i++) {
			if (probs.get(eventProbs[i]) == null) {						//The multiprobit regression can go without specifying coefficients for 1 of the outcomes as the probability of this event can be determined by the residual of the other probabilities.
				countNullEventProbs++;									//Check no more than one event has null prob, so that it is valid to take the residual to find the probability
				if(countNullEventProbs > 1) {
					throw new RuntimeException("countNullEventProbs > 1 in MultiProbitRegression object!  More than one event does not have a probability, so the residual cannot be used for the missing probabilities.");
				}
				else {
					denominator += 0.5;		//We include the base case, where score = 0 (as betas are set to zero).  The normalRV.cdf(0) = 0.5 (as the standard normal distribution is symmetric).  The other cases have already been incremented into the denominator.
					probs.put((T) eventProbs[i], 0.5/denominator);		//The normalised probability of the base case is 0.5/denominator as the 0.5 comes from applying the probit transform (the cumulative standard normal distribution) to the score of 0, and the denominator is the sum of probit transforms for all events.				
				}						
			}			
		}
		
		//Normalise the probabilities of the events specified in the regression maps
		for (T event : maps.keySet()) {		//Only iterate through the cases specified in the regression maps - the base case has already been normalised.
			double probitTransformOfScoreForEvent = probs.get(event);
			probs.put(event, probitTransformOfScoreForEvent/denominator);		//Normalise the probit transform of score (the application of the standard normal cumulative distribution to the score) of the event by the sum for all events
		}
		
		double[] probArray = new double[probs.size()]; 
		for (int i = 0; i < eventProbs.length; i++) {
			probArray[i] = probs.get(eventProbs[i]);
		}
		
		return RegressionUtils.event(eventProbs, probArray, random);				
	}

	
}
