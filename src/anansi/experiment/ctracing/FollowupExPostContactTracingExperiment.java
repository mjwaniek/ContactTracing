package anansi.experiment.ctracing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import anansi.core.Coalition;
import anansi.experiment.ExperimentResult;
import anansi.temporal.TemporalGraph;
import anansi.temporal.epidemic.TemporalSEIRHDCovidModel;
import anansi.utils.Utils;

public class FollowupExPostContactTracingExperiment extends ExPostContactTracingExperiment {

	public static final int PCR_TIMEOUT = 7;
	protected static final int WINDOW_OFFSET = 3;
	
	public static final int HD_BUDGET = 4;
	public static final int HD_PRE_TRACING_TIME = 5;
	
	protected int preTracingTime;

	public FollowupExPostContactTracingExperiment(String resDirPath, TemporalGraph g, TemporalSEIRHDCovidModel em,
			int budget, int preTracingTime) {
		super(resDirPath, g, em, Utils.aList(budget), null, null);
		this.preTracingTime = preTracingTime;
	}

	@Override
	public String getName() {
		return "followup-expost-tctracing-" + g.getName() + "-" + em.getName();
	}

	@Override
	protected List<String> getHeader() {
		return Utils.aList(getName(), g.getName(), em.getName(), Integer.toString(g.getT()), Integer.toString(budgets.get(0)),
				Integer.toString(preTracingTime));
	}

	@Override
	protected List<String> getColumnNames() {
		return Utils.aList("pcrPerc", "stat", "value", "baseline");
	}
	
	@Override
	protected void perform(ExperimentResult res) {
		Coalition initDetected = genInitDetected(preTracingTime);
		
		for (int pcrPerc = 0; pcrPerc <= 100; pcrPerc += 10) {
			int pcrBudget = pcrPerc * budgets.get(0) / 100;
			int tracingBudget = budgets.get(0) - pcrBudget;
			TemporalSEIRHDCovidModel tem = new TemporalSEIRHDCovidModel((TemporalSEIRHDCovidModel)em);
			Map<Integer, Integer> lastTest = new HashMap<>();
			Coalition detected = new Coalition(initDetected);
			PriorityQueue<Integer> traceQ = new PriorityQueue<>(
					(i,j) -> tem.getInfectionTime(i).compareTo(tem.getInfectionTime(j)));
			for (int t = preTracingTime + 1; t <= g.getT(); ++t) {
				tem.continueDiffusion(t);
				int ft = t;
				Coalition potentialTest = g.nodesStream().boxed()
						.filter(i -> ft - lastTest.getOrDefault(i, 0) > PCR_TIMEOUT && !detected.contains(i))
						.collect(Coalition.getCollector());
				potentialTest.getRandom(pcrBudget).forEach(i -> testNode(i, ft, tem, detected, traceQ, lastTest));
				int remainingTracingBudget = tracingBudget;
				while (remainingTracingBudget > 0 && !traceQ.isEmpty()) {
					int root = traceQ.poll();
					Coalition traced = traceContacts(root, WINDOW_OFFSET, tem, t)
							.filter(i -> ft - lastTest.getOrDefault(i, 0) > PCR_TIMEOUT && !detected.contains(i));
					while (remainingTracingBudget > 0 && !traced.isEmpty()) {
						testNode(traced.removeRandom(), ft, tem, detected, traceQ, lastTest);
						--remainingTracingBudget;
					}
				}
				potentialTest = g.nodesStream().boxed()
						.filter(i -> ft - lastTest.getOrDefault(i, 0) > PCR_TIMEOUT && !detected.contains(i))
						.collect(Coalition.getCollector());
				potentialTest.getRandom(tracingBudget).forEach(i -> testNode(i, ft, tem, detected, traceQ, lastTest));
			} 
			int detSrc = Utils.argmin(detected, i -> tem.getInfectionTime(i));
			res.addRow(pcrPerc, "detected", detected.size(), false);
			res.addRow(pcrPerc, "nowInfected", tem.getNowInfected().size(), false);
			res.addRow(pcrPerc, "everInfected", tem.getEverInfected().size(), false);
			res.addRow(pcrPerc, "srcTime", tem.getInfectionTime(detSrc), false);
			res.addRow(pcrPerc, "srcDist", g.sp().getLowMemoryDistance(src, detSrc), false);
			
			// Baseline - all tracing at the end
			TemporalSEIRHDCovidModel btem = new TemporalSEIRHDCovidModel((TemporalSEIRHDCovidModel)em);
			detected.clear().add(initDetected);
			PriorityQueue<Integer> bTraceQ = new PriorityQueue<>(
					(i,j) -> btem.getInfectionTime(i).compareTo(btem.getInfectionTime(j)));
			btem.continueDiffusion(g.getT());
			pcrBudget *= (g.getT() - preTracingTime);
			tracingBudget *= (g.getT() - preTracingTime);
			Coalition.diff(g.nodesCoalition(), detected).getRandom(pcrBudget)
					.forEach(i -> testNode(i, g.getT(), btem, detected, bTraceQ, lastTest));
			int remainingTracingBudget = tracingBudget;
			while (remainingTracingBudget > 0 && !bTraceQ.isEmpty()) {
				int root = bTraceQ.poll();
				Coalition traced = traceContacts(root, WINDOW_OFFSET, btem, g.getT()).remove(detected);
				while (remainingTracingBudget > 0 && !traced.isEmpty()) {
					testNode(traced.removeRandom(), g.getT(), btem, detected, bTraceQ, lastTest);
					--remainingTracingBudget;
				}
			} 
			detSrc = Utils.argmin(detected, i -> btem.getInfectionTime(i));
			res.addRow(pcrPerc, "detected", detected.size(), true);
			res.addRow(pcrPerc, "nowInfected", btem.getNowInfected().size(), true);
			res.addRow(pcrPerc, "everInfected", btem.getEverInfected().size(), true);
			res.addRow(pcrPerc, "srcTime", btem.getInfectionTime(detSrc), true);
			res.addRow(pcrPerc, "srcDist", g.sp().getLowMemoryDistance(src, detSrc), true);
		}
	}
	
	protected void testNode(int i, int t, TemporalSEIRHDCovidModel tem, Coalition detected, PriorityQueue<Integer> traceQ,
			Map<Integer, Integer> lastTest) {
		lastTest.put(i, t);
		if (tem.getNowInfected().contains(i)) {
			tem.vaccinate(i);
			detected.add(i);
			traceQ.add(i);
		}
	}
}
