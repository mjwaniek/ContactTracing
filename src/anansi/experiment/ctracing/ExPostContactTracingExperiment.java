package anansi.experiment.ctracing;

import java.util.List;
import java.util.PriorityQueue;

import anansi.core.Coalition;
import anansi.experiment.Experiment;
import anansi.experiment.ExperimentResult;
import anansi.temporal.TemporalGraph;
import anansi.temporal.epidemic.TemporalEpidemicModel;
import anansi.utils.Utils;

public class ExPostContactTracingExperiment extends Experiment {
	
	protected static final double INIT_REVEAL_EXEMPT_PERC = .95;
	protected static final int INIT_DETECTED_SIZE = 10;
	protected static final int TESTED_PER_TRACE = 10;
	protected static final int WINDOW_SIZE = 7;
	protected static final double MEMORY_DEGRADATION = .001;
	protected static final int PAUSE_TIME = 7;
	
	public static final int HD_GRAPH = 1;
	public static final int HD_EPIDEMIC_MODEL = 2;
	public static final int HD_TIME = 3;

	protected TemporalGraph g;
	protected TemporalEpidemicModel em;
	protected List<Integer> budgets;
	protected List<Integer> breadths;
	protected List<Integer> windowOffsets;
	
	protected Coalition everInfected;
	protected Integer src;
	
	public ExPostContactTracingExperiment(String resDirPath, TemporalGraph g, TemporalEpidemicModel em,
			List<Integer> budgets, List<Integer> breadths, List<Integer> windowOffsets){
		super(resDirPath);
		this.g = g;
		this.em = em;
		this.budgets = budgets;
		this.breadths = breadths;
		this.windowOffsets = windowOffsets;
		this.everInfected = null;
		this.src = null;
	}

	@Override
	public String getName() {
		return "expost-tctracing-" + g.getName() + "-" + em.getName();
	}

	@Override
	protected List<String> getHeader() {
		return Utils.aList(getName(), g.getName(), em.getName(), Integer.toString(g.getT()));
	}

	@Override
	protected List<String> getColumnNames() {
		return Utils.aList("budget", "breadth", "window", "stat", "value");
	}
	
	@Override
	protected void perform(ExperimentResult res) {
		Coalition initDetected = genInitDetected(g.getT());
		preTracing(res);
		for (int budget : budgets)
			for (int breadth : breadths)
				for (int window : windowOffsets){
					Coalition detected = new Coalition(initDetected);
					runTracing(em, detected, budget, breadth, window);
					postTracing(res, budget, breadth, window, detected);
				}
	}
	
	protected Coalition genInitDetected(int totalTime) {
		do {
			src = g.nodesCoalition().getRandom();
			em.runDiffusion(src, g, PAUSE_TIME);
			duringPause();
			em.continueDiffusion(totalTime);
			everInfected = em.getEverInfectedAndAlive();
		} while (everInfected.size() < g.size() / 10);
		Coalition potInitDetected = everInfected.stream().boxed()
				.sorted((i, j) -> em.getInfectionTime(i).compareTo(em.getInfectionTime(j)))
				.skip((int)(everInfected.size() * INIT_REVEAL_EXEMPT_PERC)).collect(Coalition.getCollector());
		return potInitDetected.getRandom(INIT_DETECTED_SIZE);
	}
	
	protected void preTracing(ExperimentResult res) {
		res.addRow(0, 0, 0, "everDetected", everInfected.size());
		res.addRow(0, 0, 0, "nowDetected", em.getNowInfected().size());
	}
	
	protected void duringPause() {
	}
	
	protected void runTracing(TemporalEpidemicModel em, Coalition detected, int budget, int breadth, int window) {
		Coalition tested = new Coalition(detected);
		int balance = budget;
		PriorityQueue<Integer> traceQ = new PriorityQueue<>(
				(i,j) -> em.getInfectionTime(i).compareTo(em.getInfectionTime(j)));
		traceQ.addAll(detected.getNodes());			
		while (balance > 0 && !traceQ.isEmpty()) {	
			Coalition newDetected = new Coalition();
			for (int ix = 0; ix < breadth && !traceQ.isEmpty() && balance > 0; ++ix){
				int i = traceQ.poll();
				Coalition potTest = traceContacts(i, window, em, g.getT()).remove(tested);
				for (int j : potTest.getRandom(TESTED_PER_TRACE)){
					tested.add(j);
					if (em.getEverInfectedAndAlive().contains(j))
						newDetected.add(j);
				}
				--balance;
			}
			detected.add(newDetected);
			traceQ.addAll(newDetected.getNodes());
		}
	}
	
	protected Coalition traceContacts(int i, int window, TemporalEpidemicModel em, int tracingTime) {
		Coalition res = new Coalition();
		int t0 = Math.min(g.getT(), em.getInfectionTime(i) + window);
		for (int t = t0; t > t0 - WINDOW_SIZE; --t) {
			double rememberProb = Math.exp(-MEMORY_DEGRADATION * (tracingTime - t));
			for (int j : g.getTPreds(i, t))
				if (Utils.RAND.nextDouble() < rememberProb)
					res.add(j);
		}
		return res;
	}
	
	protected void postTracing(ExperimentResult res, int budget, int breadth, int window, Coalition detected) {
		int detSrc = Utils.argmin(detected, i -> em.getInfectionTime(i));
		res.addRow(budget, breadth, window, "everDetected", detected.size());
		res.addRow(budget, breadth, window, "nowDetected", detected.inplaceIntersect(em.getNowInfected()).count());
		res.addRow(budget, breadth, window, "srcTime", em.getInfectionTime(detSrc));
		res.addRow(budget, breadth, window, "srcDist", g.sp().getLowMemoryDistance(src, detSrc));
	}
}
