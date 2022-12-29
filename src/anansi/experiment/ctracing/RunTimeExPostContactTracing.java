package anansi.experiment.ctracing;

import java.io.File;
import java.util.List;
import java.util.stream.Stream;

import anansi.experiment.ExperimentAggregator;
import anansi.experiment.ExperimentResult;
import anansi.experiment.Row;
import anansi.temporal.TemporalGraph;
import anansi.temporal.epidemic.TemporalSEIRHDCovidModel;
import anansi.utils.Utils;

public class RunTimeExPostContactTracing extends RunExPostContactTracing {

	public static final int BREADTH = 10;
	public static final int WINDOW_OFFSET = 3;
	
	public static void main(String[] args) {
		RunTimeExPostContactTracing r = new RunTimeExPostContactTracing();
		int times = args.length > 0 ? Integer.parseInt(args[0]) : 10;
		int n = args.length > 1 ? Integer.parseInt(args[1]) : 10000;
		int avgDegr = args.length > 2 ? Integer.parseInt(args[2]) : 50;
		int timeFrom = args.length > 3 ? Integer.parseInt(args[3]) : 14;
		int timeTo = args.length > 4 ? Integer.parseInt(args[4]) : 56;
		int timeBy = args.length > 5 ? Integer.parseInt(args[5]) : 7;
		int budget = args.length > 6 ? Integer.parseInt(args[6]) : 100;
		
		for (int iter = 0; iter < times; ++iter)
			for (int timesteps = timeFrom; timesteps <= timeTo; timesteps += timeBy){
				r.runSingle(r.ter(n, avgDegr, timesteps), budget);
				r.runSingle(r.tws(n, avgDegr, .25, timesteps), budget);
				r.runSingle(r.tba(n, avgDegr, timesteps), budget);
			}
		
		r.aggregateAll();
		r.printCharts();
	}
	
	@Override
	public String getDirectoryName() {
		return "expost-tctracing-time";
	}

	@Override
	public void runSingle(Object... params) {
		TemporalGraph g = (TemporalGraph) params[0];
		int budget = (int) params[1];
		TemporalSEIRHDCovidModel em = new TemporalSEIRHDCovidModel();
		List<Integer> budgets = Utils.aList(budget);
		List<Integer> breadths = Utils.aList(BREADTH);
		List<Integer> windowOffsets = Utils.aList(WINDOW_OFFSET);
		new ExPostContactTracingExperiment(getDataPath(g), g, em, budgets, breadths, windowOffsets).perform();
	}
	
	@Override
	public List<ExperimentAggregator> getAggregators() {
		return Utils.aList(new TimeAggregator());
	}
	
	protected class TimeAggregator extends ExperimentAggregator {

		@Override
		public String getName() {
			return "time";
		}

		@Override
		protected Stream<Row> processEvery(Stream<Row> rows, List<String> header, File experimentDir) {
			return addRelColumn(expand(rows, header, experimentDir)).filter(r -> r.getInt("breadth") > 0).stream();
		}
		
		@Override
		protected Stream<Row> processGroup(Stream<Row> rows, File mergedDir) {
			return aggregate(rows, Utils.aList("graph", "model", "budget", "time", "breadth", "window", "stat"),
					Utils.aList("value", "rel"));
		}
		
		@Override
		protected ExperimentResult postprocessMerged(ExperimentResult res) {
			res.addColumn("graphRead", r -> readGraph(r.get("graph")));
			res.addColumn("label", r -> r.concatVals("model", "budget", "stat"));
			res.addColumn("labelY", r -> readStat(r.get("stat")));
			return res;
		}
	}
}
