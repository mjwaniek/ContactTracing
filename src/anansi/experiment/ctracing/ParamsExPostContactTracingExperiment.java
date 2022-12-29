package anansi.experiment.ctracing;

import java.util.List;

import anansi.core.Coalition;
import anansi.experiment.ExperimentResult;
import anansi.temporal.TemporalGraph;
import anansi.temporal.epidemic.TemporalSEIRHDCovidModel;
import anansi.utils.Utils;

public class ParamsExPostContactTracingExperiment extends ExPostContactTracingExperiment {

	protected TemporalSEIRHDCovidModel em;
	protected List<Double> presymptomaticPeriods;
	protected List<Double> transmissionRates;
	private double pauseAvgInfected;
	
	public ParamsExPostContactTracingExperiment(String resDirPath, TemporalGraph g, TemporalSEIRHDCovidModel em,
			List<Integer> budgets, List<Integer> breadths, List<Integer> windowOffsets, List<Double> presymptomaticPeriods,
			List<Double> transmissionRates) {
		super(resDirPath, g, em, budgets, breadths, windowOffsets);
		this.em = em;
		this.presymptomaticPeriods = presymptomaticPeriods;
		this.transmissionRates = transmissionRates;
		this.pauseAvgInfected = 0.;
	}

	@Override
	public String getName() {
		return "params-expost-tctracing-" + g.getName() + "-" + em.getName();
	}

	@Override
	protected List<String> getColumnNames() {
		return Utils.aList("budget", "breadth", "window", "stat", "value", "presymptomatic", "transmission");
	}
	
	@Override
	protected void perform(ExperimentResult res) {
		for (double presymptomatic : presymptomaticPeriods)
			for (double transmission : transmissionRates) {
				em.setMuP(presymptomatic).setBeta(transmission);
				
				Coalition initDetected = genInitDetected(g.getT());
				res.addRow(0, 0, 0, "everDetected", everInfected.size(), presymptomatic, transmission);
				res.addRow(0, 0, 0, "nowDetected", em.getNowInfected().size(), presymptomatic, transmission);
				res.addRow(0, 0, 0, "avgInfected", pauseAvgInfected, presymptomatic, transmission);
				
				for (int budget : budgets)
					for (int breadth : breadths)
						for (int window : windowOffsets){
							Coalition detected = new Coalition(initDetected);
							runTracing(em, detected, budget, breadth, window);
							int detSrc = Utils.argmin(detected, i -> em.getInfectionTime(i));
							res.addRow(budget, breadth, window, "everDetected", detected.size(), presymptomatic, transmission);
							res.addRow(budget, breadth, window, "nowDetected",
									detected.inplaceIntersect(em.getNowInfected()).count(), presymptomatic, transmission);
							res.addRow(budget, breadth, window, "srcTime", em.getInfectionTime(detSrc),
									presymptomatic, transmission);
							res.addRow(budget, breadth, window, "srcDist", g.sp().getLowMemoryDistance(src, detSrc),
									presymptomatic, transmission);
						}	
			}
	}
	
	@Override
	protected void duringPause() {
		pauseAvgInfected = em.getAverageInfected();
	}
}
