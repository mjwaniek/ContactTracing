package anansi.experiment.ctracing;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import anansi.experiment.ExperimentAggregator;
import anansi.experiment.ExperimentResult;
import anansi.experiment.Row;
import anansi.temporal.TemporalGraph;
import anansi.temporal.epidemic.TemporalSEIRHDCovidModel;
import anansi.utils.Utils;

public class RunParamsExPostContactTracing extends RunExPostContactTracing {

	public static final int BREADTH = 10;
	public static final int WINDOW_OFFSET = 3;
	
	public static void main(String[] args) {
		RunParamsExPostContactTracing r = new RunParamsExPostContactTracing();
		int times = args.length > 0 ? Integer.parseInt(args[0]) : 10;
		int n = args.length > 1 ? Integer.parseInt(args[1]) : 10000;
		int avgDegr = args.length > 2 ? Integer.parseInt(args[2]) : 50;
		int budget = args.length > 3 ? Integer.parseInt(args[3]) : 100;
		
		for (int iter = 0; iter < times; ++iter){
			r.runSingle(r.ter(n, avgDegr, TIMESTEPS), budget);
			r.runSingle(r.tws(n, avgDegr, .25, TIMESTEPS), budget);
			r.runSingle(r.tba(n, avgDegr, TIMESTEPS), budget);
		}
		
		r.aggregateAll();
		r.printCharts();
	}
	
	@Override
	public String getDirectoryName() {
		return "expost-tctracing-params";
	}
	
	@Override
	public void runSingle(Object... params) {
		TemporalGraph g = (TemporalGraph) params[0];
		int budget = (int)params[1];
		TemporalSEIRHDCovidModel em = new TemporalSEIRHDCovidModel();
		List<Integer> budgets = Utils.aList(budget);
		List<Integer> breadths = Utils.aList(BREADTH);
		List<Integer> windowOffsets = Utils.aList(WINDOW_OFFSET);
		List<Double> presymptomaticPeriods = Utils.aList();
		for (int day = 1; day <= 7; ++day)
			presymptomaticPeriods.add(1. / day);
		List<Double> transmissionRates = Utils.aList();
		for (int beta = 500; beta <= 1250; beta += 125)
			transmissionRates.add(beta * .0001);
		new ParamsExPostContactTracingExperiment(getDataPath(g), g, em, budgets, breadths, windowOffsets, presymptomaticPeriods,
				transmissionRates).perform();
	}
	
	@Override
	public List<ExperimentAggregator> getAggregators() {
		return Utils.aList(new R0Aggregator(), new ParamsAggregator());
	}

	protected class R0Aggregator extends ExperimentAggregator {

		@Override
		public String getName() {
			return "r0";
		}

		@Override
		protected Stream<Row> processEvery(Stream<Row> rows, List<String> header, File experimentDir) {
			ExperimentResult res = addRelColumn(expand(rows, header, experimentDir),
					Utils.aList("presymptomatic", "transmission", "stat"));
			Map<List<String>, Double> r0 = res.stream().filter(r -> r.get("stat").equals("avgInfected"))
					.collect(Collectors.toMap(r -> r.getKey("presymptomatic", "transmission"), r -> r.getDouble("value")));
			Map<List<String>, Double> inf = res.stream()
					.filter(r -> r.getInt("breadth") == 0 && r.get("stat").equals("everDetected"))
					.collect(Collectors.toMap(r -> r.getKey("presymptomatic", "transmission"), r -> r.getDouble("value")));
			res.addDoubleColumn("r0", r -> r0.get(r.getKey("presymptomatic", "transmission")));
			res.addDoubleColumn("infected", r -> inf.get(r.getKey("presymptomatic", "transmission")));
			res.filter(r -> !r.getDouble("r0").isInfinite());
			return res.stream();
		}
		
		@Override
		protected Stream<Row> processGroup(Stream<Row> rows, File mergedDir) {
			return aggregate(rows,
					Utils.aList("graph","model","budget","time","breadth","window","stat","presymptomatic","transmission"),
					Utils.aList("value", "rel", "r0", "infected"));
		}
		
		@Override
		protected ExperimentResult postprocessMerged(ExperimentResult res) {
			res.addColumn("label", r -> r.concatVals("model", "budget", "time", "breadth", "window", "stat"));
			res.addColumn("graphRead", r -> readGraph(r.get("graph")));
			res.addColumn("statRead", r -> readStat(r.get("stat")));
			return res;
		}
	}
	
	protected class ParamsAggregator extends ExperimentAggregator {

		@Override
		public String getName() {
			return "params";
		}

		@Override
		protected Stream<Row> processEvery(Stream<Row> rows, List<String> header, File experimentDir) {
			return addRelColumn(expand(rows, header, experimentDir), Utils.aList("presymptomatic", "transmission", "stat"))
					.filter(r -> !r.getDouble("value").isInfinite()).stream();
		}
		
		@Override
		protected Stream<Row> processGroup(Stream<Row> rows, File mergedDir) {
			return aggregate(rows,
					Utils.aList("graph","model","budget","time","breadth","window","stat","presymptomatic","transmission"),
					Utils.aList("value","rel"));
		}
		
		@Override
		protected ExperimentResult postprocessMerged(ExperimentResult res) {
			res.addDoubleColumn("presDays", r -> 1. / r.getDouble("presymptomatic"));
			res.addColumn("highColor", r -> r.getInt("breadth") == 0 ? "darkgreen"
					: r.get("stat").startsWith("src") ? "khaki" : graphColor(r.get("graph")));
			res.addColumn("lowColor", r -> r.get("stat").startsWith("src") ? graphColor(r.get("graph")) : "khaki");
			res.addColumn("label", r -> r.concatVals("graph", "model", "budget", "time", "breadth", "window", "stat"));
			res.addBoolColumn("init", r -> r.getInt("breadth") == 0);
			res.addBoolColumn("post", r -> r.getInt("breadth") > 0);
			return res;
		}
	}
}
