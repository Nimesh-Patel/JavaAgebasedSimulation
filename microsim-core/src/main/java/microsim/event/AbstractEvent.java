package microsim.event;

import microsim.exception.SimulationException;

public abstract class AbstractEvent implements Event {

	private static long eventCounter = Long.MIN_VALUE;
	
	protected double time;
	protected int ordering;		//If two events have time fields with equal value, their ordering fields will determine the order in which the events are fired, with lower ordering values fired before high ordering values.  If ordering fields are also equal, the event that was scheduled first will be fired first in the schedule (determined comparing the eventNumber field).
	private long eventNumber = eventCounter++;		//Designed to break randomness of cases when time and ordering of two events is the same.  In this case, the first event that was scheduled will be fired first in the schedule.
	protected double loop;

	/** 
	   * Set the time, ordering and loop period of the event
	   * @param atTime - the absolute time for the event to be fired 
	   * @param withOrdering - the ordering of the event to be fired.  
	   * 	If two or more events share an absolute time, their order 
	   * 	can be specified using the ordering integer - an event 
	   * 	with a lower ordering value will be fired earlier.  If 
	   * 	two or more events have equal absolute time and ordering, 
	   * 	they will be fired in random order.
	   * @param withLoop - the time period between repeated firing of 
	   * 	the event.  If this parameter is set to 0, this event will 
	   * 	not be fired more than once.
	   */
	public void setTimeOrderingAndLoopPeriod(double atTime, int withOrdering, double withLoop) {
		time = atTime;
		ordering = withOrdering;
		loop = withLoop;
	}
	
//	/** Set the absolute time of the event, 
//	 *  NOTE - this method also sets the ordering of this event to the default value of 0
//	 *  and the loop period to 0 (i.e. this event is not looped).
//	 *  If the ordering or loop should have different values, either use the 
//	 *  setTimeOrderingAndLoopPeriod(double, int, double) method instead of this one,
//	 *  or individually set the ordering using setOrdering(int)
//	 *  and set the loop period using setLoop(double).
//	 * 
//	 * */
//	public void setTime(double atTime) {
//		setTimeOrderingAndLoopPeriod(atTime, 0, 0.);
//	}
	
//	/** Set the absolute time of the event and its loop period. 
//	 *  NOTE - this method sets the ordering of this event to 0, 
//	 *  so events sharing the same time will be fired in a random order.  
//	 *  If this event is to be fired before (after) another event,
//	 *  ensure that this event has an ordering integer value less (more) 
//	 *  than the ordering integer value of the other event. 
//	 *  To specify the order of the events in addition to the time and loop period, 
//	 *  use method setTime(double atTime, int withOrdering, double withLoop).
//	 * 
//	 * */
//	public void setTime(double atTime,  double withLoop) {
//		time = atTime;
////		ordering = withOrdering;
//		loop = withLoop;
//	}




	public abstract void fireEvent() throws SimulationException;

	/**
	 * Determines the natural ordering of events.  As such it determines 
	 * the order in which events are fired from the schedule.
	 * If two events have different time fields, the event with the lower 
	 * time field is fired before the event with the higher time field.
	 * If two events have time fields with equal value, their ordering 
	 * fields will determine the order in which the events are fired, 
	 * with lower ordering values fired before high ordering values.  If 
	 * ordering fields are also equal, the two events will be fired in 
	 * random order.
	 */
	public int compareTo(Event e) {
		//Ross Richardson: See Joshua Bloch's Effective Java (2nd Ed.) page 65.  "For floating-point fields, use Double.compare..."
		int compareDouble = Double.compare(time, e.getTime());
		if(compareDouble > 0)
			return 1;
		if(compareDouble < 0)
			return -1;
		
		//time and e.getTime() must be equal, so now check the ordering of events
		if (ordering > e.getOrdering())
			return 1;
		if (ordering < e.getOrdering())
			return -1;
		
		//time and ordering must be equal, so now check which event was scheduled first and return an int such that the first event is fired first.
		if(eventNumber > ((AbstractEvent)e).eventNumber) {
//			System.out.println("this eventNumber is greater:" + eventNumber + " vs " + ((AbstractEvent)e).eventNumber);
			return 1;
		}
		else if(eventNumber < ((AbstractEvent)e).eventNumber) {
//			System.out.println("the other eventNumber is greater:" + eventNumber + " vs " + ((AbstractEvent)e).eventNumber);
			return -1;
		}
		else throw new RuntimeException("Two events have the same eventNumber.  This should not be possible!\n" + Thread.currentThread().getStackTrace());
				
//		return 0;			//Time, ordering and eventNumber fields are equal - this should not be possible!

		// Michele Sonnessa:
		// NOTICE:
		// The following instruction has not been used because comparing
		// long values it is possible to overflow during the cast. It might
		// result in a wrong sign.
		// return ((SimEvent)o).getTime() - time;
	}
	
	/** Schedule event at the next loop time. 
	 * NOTE - this method does not change the ordering of the event.  
	 * Use the setOrdering(int) method if this is necessary.
	 * */
	public void setTimeAtNextLoop() {
		time += loop;
	}
	
	/** Get the next firing absolute time. */
	public double getTime() {
		return time;
	}
	
//	/** Set the loop length. */
//	public void setLoop(double newLoop) {
//		loop = newLoop;
//	}

	/** Get the loop length. */
	public double getLoop() {
		return loop;
	}
	
	/** Get the ordering of the event's next firing. */
	public int getOrdering() {
		return ordering;
	}
	
//	/** Set the ordering of the event's next firing. */
//	public void setOrdering(int ordering) {
//		this.ordering = ordering;
//	}
}
