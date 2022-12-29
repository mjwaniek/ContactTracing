package anansi.experiment.ctracing;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import anansi.experiment.ExperimentAggregator;
import anansi.experiment.ExperimentResult;
import anansi.experiment.Row;
import anansi.temporal.TemporalGraph;
import anansi.temporal.epidemic.TemporalSEIRHDCovidModel;
import anansi.utils.Utils;

public class RunFollowupExPostContactTracing extends RunExPostContactTracing {

	public static final int PRE_CTRACING_TIMESTEPS = 28;
	public static final int TIMESTEPS = 56;

	public static void main(String[] args) {
		RunFollowupExPostContactTracing r = new RunFollowupExPostContactTracing();
		int times = args.length > 0 ? Integer.parseInt(args[0]) : 10;
		int n = args.length > 1 ? Integer.parseInt(args[1]) : 1000;
		int avgDegr = args.length > 2 ? Integer.parseInt(args[2]) : 50;
		int budget = args.length > 3 ? Integer.parseInt(args[3]) : 100;
		
		for (int iter = 0; iter < times; ++iter) {
			r.runSingle(r.ter(n, avgDegr, TIMESTEPS), budget);
			r.runSingle(r.tws(n, avgDegr, .25, TIMESTEPS), budget);
			r.runSingle(r.tba(n, avgDegr, TIMESTEPS), budget);
		}
		
		r.aggregateAll();
		r.printCharts();
	}

	@Override
	public String getDirectoryName() {
		return "expost-tctracing-followup";
	}

	@Override
	public void runSingle(Object... params) {
		TemporalGraph g = (TemporalGraph) params[0];
		int budget = (int)params[1];
		TemporalSEIRHDCovidModel em = new TemporalSEIRHDCovidModel();
		new FollowupExPostContactTracingExperiment(getDataPath(g), g, em, budget, PRE_CTRACING_TIMESTEPS).perform();
	}

	@Override
	public List<ExperimentAggregator> getAggregators() {
		return Utils.aList(new FollowupTradeoffAggregator(), new FollowupLineAggregator());
	}
	
	protected class FollowupTradeoffAggregator extends ExperimentAggregator {

		@Override
		public String getName() {
			return "followup-tradeoff";
		}

		@Override
		protected Stream<Row> processEvery(Stream<Row> rows, List<String> header, File experimentDir) {
			Map<List<String>, List<Row>> grouped = new ExperimentResult(experimentDir, header, rows)
					.groupByKey("pcrPerc", "baseline");
			ExperimentResult res = new ExperimentResult(experimentDir, header,
					Utils.aList("pcrPerc", "baseline", "stat1", "stat2", "value1", "value2"));
			grouped.forEach((key, rs) -> Utils.sublistsOfSize(rs, 2).forEach(pr -> {
					res.addRowDontPrint(key.get(0), key.get(1), pr.get(0).get("stat"), pr.get(1).get("stat"),
							pr.get(0).get("value"), pr.get(1).get("value"));
					res.addRowDontPrint(key.get(0), key.get(1), pr.get(1).get("stat"), pr.get(0).get("stat"),
							pr.get(1).get("value"), pr.get(0).get("value"));
				}));
			return expand(res.stream(), header, experimentDir).stream();
		}
		
		@Override
		protected Stream<Row> processGroup(Stream<Row> rows, File mergedDir) {
			return aggregate(rows, Utils.aList("graph", "model", "time", "budget", "pcrPerc", "baseline", "stat1", "stat2"),
					Utils.aList("value1", "value2"));
		}
		
		@Override
		protected ExperimentResult postprocessMerged(ExperimentResult res) {
			List<String> keep = Utils.aList("detected", "srcDist");
			res.filter(r -> keep.contains(r.get("stat1")) && keep.contains(r.get("stat2")));
			res.addColumn("label", r -> r.concatVals("model", "time", "budget", "stat1", "stat2"));
			res.addColumn("graphRead", r -> readGraph(r.get("graph")));
			res.addColumn("baselineRead", r -> readBaseline(r.getBool("baseline")));
			res.addColumn("stat1Read", r -> readStat(r.get("stat1")));
			res.addColumn("stat2Read", r -> readStat(r.get("stat2")));
			return res;
		}
	}
	
	protected class FollowupLineAggregator extends ExperimentAggregator {

		@Override
		public String getName() {
			return "followup-line";
		}

		@Override
		protected Stream<Row> processEvery(Stream<Row> rows, List<String> header, File experimentDir) {
			return expand(rows, header, experimentDir).stream();
		}
		
		@Override
		protected Stream<Row> processGroup(Stream<Row> rows, File mergedDir) {
			return aggregate(rows, Utils.aList("graph", "model", "time", "budget", "baseline", "pcrPerc", "stat"),
					Utils.aList("value"));
		}
		
		@Override
		protected ExperimentResult postprocessMerged(ExperimentResult res) {
			res.addColumn("label", r -> r.concatVals("model", "time", "budget", "stat"));
			res.addColumn("graphRead", r -> readGraph(r.get("graph")));
			res.addColumn("statRead", r -> readStat(r.get("stat")));
			res.addColumn("baselineRead", r -> readBaseline(r.getBool("baseline")));
			res.addColumn("grouping", r -> r.concatVals("graph", "baseline"));
			return res;
		}
	}
	
	protected static ExperimentResult expand(Stream<Row> rows, List<String> header, File experimentDir) {
		ExperimentResult er = new ExperimentResult(experimentDir, header, rows);
		er.expand("graph", FollowupExPostContactTracingExperiment.HD_GRAPH);
		er.expand("model", FollowupExPostContactTracingExperiment.HD_EPIDEMIC_MODEL);
		er.expand("time", FollowupExPostContactTracingExperiment.HD_TIME);
		er.expand("budget", FollowupExPostContactTracingExperiment.HD_BUDGET);
		er.expand("ptTime", FollowupExPostContactTracingExperiment.HD_PRE_TRACING_TIME);
		return er;
	}

	protected static String readBaseline(boolean baseline) {
		return baseline ? "Tracing at the end" : "Tracing throughout";
	}
}
