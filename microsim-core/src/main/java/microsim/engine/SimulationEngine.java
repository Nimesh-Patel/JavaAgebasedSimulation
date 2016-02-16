package microsim.engine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;

import microsim.data.ExperimentManager;
import microsim.data.db.Experiment;
import microsim.event.EventList;
import microsim.event.SystemEventType;
import microsim.exception.SimulationException;
import microsim.exception.SimulationRuntimeException;

import org.apache.log4j.Logger;

/**
 * The simulation engine. The engine keeps a reference to an EventList object to
 * manage temporal sequence of events. Every object of the running simulation
 * can schedule events at a specified time point and the engine will notify to
 * it at the right time. The SimEngine stores a list of windows created by
 * models. Using the addSimWindow() method each simulation windows is managed by
 * the engine. It is able to show windows detroyed by user. When the windows is
 * shown the engine put the windows in the location where it was when the
 * project document was saved to disk. The window size is stored, too.
 * 
 * <p>
 * Title: JAS
 * </p>
 * <p>
 * Description: Java Agent-based Simulation library
 * </p>
 * <p>
 * Copyright (C) 2002 Michele Sonnessa
 * </p>
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 * 
 * @author Michele Sonnessa
 *         <p>
 */
public class SimulationEngine extends Thread {

	private static Logger log = Logger.getLogger(SimulationEngine.class);
	
	private int eventThresold = 0;

	private int currentRunNumber = 0;

	private Experiment currentExperiment = null;

	private String multiRunId = null;
	
	/**
	 * @supplierCardinality 1
	 */
	private EventList eventList;
	private List<SimulationManager> models;
	private Map<String, SimulationManager> modelMap;
	private boolean modelBuild = false;

	private static Random rnd;
	private long randomSeed;

	protected ArrayList<EngineListener> engineListeners;

	private boolean runningStatus = false;

	/** Abilita o disabilita la scrittura del collector */
	private boolean silentMode = false;
	
	/** Quando costruisco un modello se è disabilitato silent mode viene creato il db. Durante
	 * il run posso dinamicamente abilitare o disabilitare. Nel caso invece sia partito in silentMode
	 * il db non esiste e quindi il flag non può essere cambiato. */
	private boolean silentModeAvailable = true;
	
	private ClassLoader classLoader = null;
	
	private static SimulationEngine instance;	
		
	private Class<?> builderClass = null;
	
	private ExperimentBuilder experimentBuilder = null;
	
	/**
	 * @link dependency
	 * @stereotype use
	 * @supplierRole 1..
	 **/
	/* #SimModel lnkSimModel; */

	/**
	 * Build a new SimEngine with the given time unit.
	 * 
	 * @param timeUnit
	 *            The time uint id. See the public constants in the SimTime
	 *            class.
	 */
	protected SimulationEngine() {
		eventList = new EventList();
		models = new ArrayList<SimulationManager>();
		modelMap = new LinkedHashMap<String, SimulationManager>();
		randomSeed = System.currentTimeMillis();
		rnd = new Random(randomSeed);
		engineListeners = new ArrayList<EngineListener>();
		
		instance = this;
	}

	public boolean isSilentMode() {
		return silentMode;
	}

	public void setSilentMode(boolean silentMode) {
		if (silentMode && ! silentModeAvailable)
			return;
		
		this.silentMode = silentMode;
		ExperimentManager.getInstance().copyInputFolderStructure = ! silentMode;
		ExperimentManager.getInstance().saveExperimentOnDatabase = ! silentMode;
	}

	public Class<?> getBuilderClass() {
		return builderClass;
	}

	public ExperimentBuilder getExperimentBuilder() {
		return experimentBuilder;
	}

	public void setExperimentBuilder(ExperimentBuilder experimentBuilder) {
		this.experimentBuilder = experimentBuilder;
	}

	public boolean isSilentModeAvailable() {
		return silentModeAvailable;
	}

	@Deprecated
	public void setBuilderClass(Class<?> builderClass) {
		if (! ExperimentBuilder.class.isAssignableFrom(builderClass)) 
			throw new RuntimeException(builderClass + " does not implement ExperimentBuilder interface!");
		
		this.builderClass = builderClass;
	}

	public static SimulationEngine getInstance() {
		if (instance == null)
			instance = new SimulationEngine();
		
		return instance;
	}
	
	public int getCurrentRunNumber() {
		return currentRunNumber;
	}

	public void setCurrentRunNumber(int currentRunNumber) {
		this.currentRunNumber = currentRunNumber;
	}

	public Experiment getCurrentExperiment() {
		return currentExperiment;
	}

	public SimulationManager getManager(String id) {
		return modelMap.get(id);
	}
	
	public ClassLoader getClassLoader() {
		return classLoader;
	}

	public void setClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	/**
	 * Install a listener for events generated by the simulation engine.
	 * 
	 * @param engineListener
	 *            An object implementing the ISimEngineListener interface.
	 */
	public void addEngineListener(EngineListener engineListener) {
		engineListeners.add(engineListener);
	}

	public void removeEngineListener(EngineListener engineListener) {
		engineListeners.remove(engineListener);
	}

	public ArrayList<EngineListener> getEngineListeners() {
		return engineListeners;
	}

	public void setup() {
		if (builderClass != null)
			try {
				((ExperimentBuilder) builderClass.newInstance()).buildExperiment(this);
			} catch (InstantiationException e) {
				log.error(e.getMessage());
			} catch (IllegalAccessException e) {
				log.error(e.getMessage());
			}
		else if (experimentBuilder != null)
			experimentBuilder.buildExperiment(this);
		
		notifySimulationListeners(SystemEventType.Setup);
	}
	
	/**
	 * Return an array representing the running SimModels.
	 * 
	 * @return A list of running models.
	 */
	public SimulationManager[] getModelArray() {
		return models.toArray(new SimulationManager[] {});
	}

	/**
	 * Return a reference to the current EventList.
	 * 
	 * @return The event list.
	 */
	public EventList getEventList() {
		return eventList;
	}

	/**
	 * Return a reference to the current SimTime.
	 * 
	 * @return The current time object.
	 */
	public double getTime() {
		return eventList.getTime();
	}

	/**
	 * Return a reference to the current Random generator.
	 * 
	 * @return The current random generator.
	 */
	public static Random getRnd() {
		return rnd;
	}

	/**
	 * Make forSteps simulation steps.
	 * 
	 * @param forSteps
	 *            The number of steps to be done.
	 * @throws SimulationException
	 */
	public void step(int forSteps) throws SimulationException {
		for (int i = 0; i < forSteps; i++)
			step();
	}
	
	public void reset() {
		pause();
		eventList = new EventList();
		models = new ArrayList<SimulationManager>();
		modelMap = new LinkedHashMap<String, SimulationManager>();
		randomSeed = System.currentTimeMillis();
		rnd = new Random(randomSeed);		
	}

	/**
	 * Start simulation. A new thread starts and calls step() method until
	 * something stops it.
	 */
	public void startSimulation() {
		if (!isAlive())
			start();

		if (!modelBuild)
			buildModels();

		setRunningStatus(true);
		
		notifySimulationListeners(SystemEventType.Start);
	}

	/**
	 * Stop simulation. The running thread is freezed until next step is called.
	 */
	public void pause() {
		setRunningStatus(false);
		
		notifySimulationListeners(SystemEventType.Stop);
	}

	/** Stop the simulation, dispose everything and the quit the JVM. */
	public void quit() {
		pause();
		eventList = null;
		for (SimulationManager model : models) {
			model.dispose();
		}
		models.clear();
		notifySimulationListeners(SystemEventType.Shutdown);
		System.exit(0);
	}

	/**
	 * Notify the engine to manage a SimModel. This method is mandatory to let a
	 * model work. The current event list is joined to the given model.
	 * 
	 * @param model
	 *            The model to be added.
	 */
	public SimulationManager addSimulationManager(SimulationManager simulationManager) {
		modelMap.put(simulationManager.getId(), simulationManager);
		models.add(simulationManager);
		simulationManager.setEngine(this);
		
		return simulationManager;
	}

	public SimulationManager addSimulationManager(String managerClassName) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		SimulationManager simulationManager = null;
		if (classLoader != null)
			simulationManager = (SimulationManager) classLoader.loadClass(managerClassName).newInstance();
		else
			simulationManager = (SimulationManager) Class.forName(managerClassName).newInstance();
		return addSimulationManager(simulationManager);
	}
	
	/** Call the buildModel() method of each active SimModel. */
	public void buildModels() {
		currentExperiment = ExperimentManager.getInstance().createExperiment(multiRunId);
		
		silentModeAvailable = (! silentMode);
		
		notifySimulationListeners(SystemEventType.Build);
		
		try {			
			currentExperiment = ExperimentManager.getInstance().setupExperiment(currentExperiment, models.toArray(new SimulationManager[models.size()]));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (modelBuild)
			return;
		
		Iterator<SimulationManager> it = models.iterator();
		while (it.hasNext()) {
			final SimulationManager manager = it.next();
			manager.buildObjects();
			manager.buildSchedule();
		}

		modelBuild = true;
	}

	/**
	 * Return true if buildModels() method has been called. False otherwise.
	 * 
	 * @return True is models have been built, false otherwise.
	 */
	public boolean getModelBuildStatus() {
		return modelBuild;
	}

	/**
	 * Dispose from memory all running models. Return an array representing the
	 * Class of each disposed models. It is used by rebuildModels().
	 * 
	 * @return The list of disposed models.
	 */
	public synchronized Class<?>[] disposeModels() {
		eventList.clear();

		modelBuild = false;

		// Get models' class type and dispose
		Class<?>[] cls = new Class[models.size()];
		for (int i = 0; i < models.size(); i++) {
			SimulationManager model = (SimulationManager) models.get(i);
			cls[i] = model.getClass();
			model.dispose();
		}
		models.clear();
		modelMap.clear();
		
		System.gc();
		currentRunNumber = 0;

		silentModeAvailable = true;
		
		return cls;
	}

	/**
	 * Dispose and rebuild each running model. It is used to restart simulation.
	 */
	public void rebuildModels() {
		int k = currentRunNumber;
		disposeModels();
		currentRunNumber = k + 1;

		eventList.clear();
		
		setRandomSeed(randomSeed);
		
		notifySimulationListeners(SystemEventType.Restart);
		setup();
	}

	/**
	 * Return the current random seed.
	 * 
	 * @return The current random seed.
	 */
	public long getRandomSeed() {
		return randomSeed;
	}

	/**
	 * Set the current random seed.
	 * 
	 * @param newSeed
	 *            The new random seed.
	 */
	public void setRandomSeed(long newSeed) {
		rnd.setSeed((int) newSeed);
		randomSeed = newSeed;
	}

	/**
	 * Stops the simulation and call the simulationEnd method of each running
	 * model.
	 */
	public void end() {
		pause();
		eventList.clear();
		performAction(SystemEventType.End);
	}

	/**
	 * React to system events.
	 * 
	 * @param actionType
	 *            Reacts in case of EVENT_SIMULATION_END,
	 *            EVENT_SIMULATION_RESTART, EVENT_SHUTDOWN events.
	 */
	public void performAction(SystemEventType actionType) {
		switch (actionType) {
			case Stop:
				pause();
				Iterator<SimulationManager> it = models.iterator();
				while (it.hasNext())
					it.next().dispose();
				break;
			case Restart:
				rebuildModels();
				break;
			case Shutdown:
				quit();
				break;
			default:
				break;
		}

		for (ListIterator<EngineListener> it = engineListeners.listIterator(); it
				.hasNext();)
			it.next().onEngineEvent(actionType);
	}

	private synchronized void checkIdle() throws InterruptedException {
		while (!runningStatus)
			wait();
	}

	/** Return current simulation running status. */
	public boolean getRunningStatus() {
		return runningStatus;
	}

	/** Set current simulation running status. */
	public void setRunningStatus(boolean running) {
		runningStatus = running;
		if (runningStatus)
			resumeRun();
	}

	/** Set the delay time beetween two simulation steps. */
	public void setEventTimeTreshold(int millis) {
		eventThresold = millis;
	}

	private synchronized void resumeRun() {
		notify();
	}

	public synchronized void step() throws SimulationException {
		if (!modelBuild)
			buildModels();

		eventList.step();
		notifySimulationListeners(SystemEventType.Step);
		yield();		
	}

	protected synchronized void notifySimulationListeners(SystemEventType event) {
		if (engineListeners != null)
			for (EngineListener listener : engineListeners) {
				listener.onEngineEvent(event);
			}
	}
	
	/**
	 * Start the independent thread running simulation. It fire events only if
	 * running status is set to true.
	 */
	public void run() {
		// System.out.println("JAS enigne started at " + System.);
		/*
		 * while (true) { try { yield(); if (EVENT_TRESHOLD > 0)
		 * sleep(EVENT_TRESHOLD); } catch (Exception e) {
		 * System.out.println("Interrupt: " + e.getMessage()); }
		 * 
		 * if (runningStatus) step(); }
		 */
		while (true) {
			try {
				checkIdle();
			} catch (Exception e) {
			}

			try {
				step();
			} catch (SimulationException e1) {
				throw new SimulationRuntimeException(e1);
			}

			if (eventThresold > 0)
				try {
					sleep(eventThresold);
				} catch (Exception e) {
					log.error("Interrupt: " + e.getMessage());
				}
			// this is now called in step() method.
			else
				yield();
		}

	}
	
	public Random getRandom() {
//		return new Random(rnd.nextLong());
		return rnd;
	}

	public String getMultiRunId() {
		return multiRunId;
	}

	public void setMultiRunId(String multiRunId) {
		this.multiRunId = multiRunId;
	}

}