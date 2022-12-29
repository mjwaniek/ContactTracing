package anansi.experiment.ctracing;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import anansi.experiment.ExperimentAggregator;
import anansi.experiment.ExperimentResult;
import anansi.experiment.ExperimentRunner;
import anansi.experiment.Row;
import anansi.temporal.TemporalGraph;
import anansi.temporal.TemporalGraphGenerator;
import anansi.temporal.epidemic.TemporalSEIRHDCovidModel;
import anansi.utils.Utils;

public class RunExPostContactTracing extends ExperimentRunner {

	public static final int TIMESTEPS = 28;
	public static final double RANDOM_OVERLAP = .9;
	public static final int INTERVAL_MIN = 1;
	public static final int INTERVAL_MAX = 7;
	public static final double INTERVAL_COEFF = 2.2;

	public static void main(String[] args) {
		RunExPostContactTracing r = new RunExPostContactTracing();
		int times = args.length > 0 ? Integer.parseInt(args[0]) : 10;
		int n = args.length > 1 ? Integer.parseInt(args[1]) : 10000;
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
		return "expost-tctracing";
	}

	@Override
	public void runSingle(Object... params) {
		TemporalGraph g = (TemporalGraph) params[0];
		int budget = (int)params[1];
		TemporalSEIRHDCovidModel em = new TemporalSEIRHDCovidModel();
		List<Integer> budgets = Utils.aList(budget);
		List<Integer> breadths = IntStream.rangeClosed(1, Math.min(10, budget)).boxed().collect(Collectors.toList());
		List<Integer> windowOffsets = IntStream.range(0, ExPostContactTracingExperiment.WINDOW_SIZE).boxed()
				.collect(Collectors.toList());
		new ExPostContactTracingExperiment(getDataPath(g), g, em, budgets, breadths, windowOffsets).perform();
	}
	
	protected TemporalGraph ter(int n, int avgDegr, int timesteps) {
		return TemporalGraphGenerator.generateErdosRenyiTGraph(n, avgDegr, timesteps, contactsPerEdge(timesteps),
				RANDOM_OVERLAP, INTERVAL_MIN, INTERVAL_MAX, INTERVAL_COEFF);
	}
	
	protected TemporalGraph tws(int n, int avgDegr, double beta, int timesteps) {
		return TemporalGraphGenerator.generateSmallWorldTGraph(n, avgDegr, beta, timesteps, contactsPerEdge(timesteps),
				RANDOM_OVERLAP, INTERVAL_MIN, INTERVAL_MAX, INTERVAL_COEFF);
	}
	
	protected TemporalGraph tba(int n, int avgDegr, int timesteps) {
		return TemporalGraphGenerator.generateBarabasiAlbertTGraph(n, avgDegr, timesteps, contactsPerEdge(timesteps),
				RANDOM_OVERLAP, INTERVAL_MIN, INTERVAL_MAX, INTERVAL_COEFF);
	}
	
	protected int contactsPerEdge(int timesteps) {
		return (int)(10. * timesteps / 28.);
	}

	@Override
	public List<ExperimentAggregator> getAggregators() {
		return Utils.aList(new TradeoffBarSmallAggregator(), new TradeoffBarAggregator(), new TradeoffScatterAggregator(),
				new HeatmapAggregator());
	}
	
	protected class TradeoffBarAggregator extends TradeoffScatterAggregator {

		@Override
		public String getName() {
			return "tradeoff-bar";
		}
		
		@Override
		protected ExperimentResult postprocessMerged(ExperimentResult res) {
			Map<List<String>, List<Row>> grouped =
					res.groupByKey("graph", "model", "budget", "time", "stat1", "stat2");
			ExperimentResult newRes = new ExperimentResult(res.getResultDir(), res.getHeader(),
					Utils.aList("graph", "model", "budget", "time", "stat1", "stat2", "slope", "slopeC95"));
			grouped.forEach((key, rs) -> {
				SimpleRegression reg = new SimpleRegression();
				rs.forEach(r -> reg.addData(
						r.getDouble("value1Mean") * (r.get("stat1").startsWith("src") ? -1. : 1.),
						r.getDouble("value2Mean") * (r.get("stat2").startsWith("src") ? -1. : 1.)));
				newRes.addRowDontPrint(key.get(0), key.get(1), key.get(2), key.get(3), key.get(4), key.get(5),
						reg.getSlope(), reg.getSlopeConfidenceInterval());
			});
			newRes.addIntColumn("size", r -> Integer.parseInt(r.get("graph").split("-")[1]));
			newRes.addColumn("graphRead", r -> { switch (r.get("graph").split("-")[0]) {
				case "tba" : return "Barabasi-Albert";
				case "ter" : return "Erdos-Renyi";
				case "tws" : return "Watts-Strogatz";
				default: return Utils.capitalize(r.get("graph"));
			}});
			newRes.addColumn("stat1Read", r -> readStat(r.get("stat1")) + " change");
			newRes.addColumn("stat2Read", r -> readStatNarrow(r.get("stat2")));
			newRes.addColumn("label", r -> r.concatVals("model", "size", "budget", "time", "stat1"));
			return newRes;
		}
	}
	
	protected class TradeoffBarSmallAggregator extends TradeoffBarAggregator {

		@Override
		public String getName() {
			return "tradeoff-bar-small";
		}
		
		@Override
		protected ExperimentResult postprocessMerged(ExperimentResult res) {
			res = super.postprocessMerged(res);
			res.addColumn("label", r -> r.concatVals("model", "size", "budget", "time", "stat1", "stat2"));
			res.addColumn("labelY", r -> readStat(r.get("stat2")) + " change");
			return res;
		}
	}
	
	protected class TradeoffScatterAggregator extends ExperimentAggregator {

		@Override
		public String getName() {
			return "tradeoff-scatter";
		}

		@Override
		protected Stream<Row> processEvery(Stream<Row> rows, List<String> header, File experimentDir) {
			Map<List<String>, List<Row>> grouped =
					new ExperimentResult(experimentDir, header, rows.filter(r -> r.getInt("breadth") > 0))
							.groupByKey("budget", "breadth", "window");
			ExperimentResult res = new ExperimentResult(experimentDir, header,
					Utils.aList("budget", "breadth", "window", "stat1", "stat2", "value1", "value2"));
			grouped.forEach((key, rs) -> Utils.sublistsOfSize(rs, 2).forEach(pr -> {
					res.addRowDontPrint(key.get(0), key.get(1), key.get(2), pr.get(0).get("stat"), pr.get(1).get("stat"),
							pr.get(0).get("value"), pr.get(1).get("value"));
					res.addRowDontPrint(key.get(0), key.get(1), key.get(2), pr.get(1).get("stat"), pr.get(0).get("stat"),
							pr.get(1).get("value"), pr.get(0).get("value"));
				}));
			return expand(res.stream(), header, experimentDir).stream();
		}
		
		@Override
		protected Stream<Row> processGroup(Stream<Row> rows, File mergedDir) {
			return aggregate(rows, Utils.aList("graph", "model", "budget", "time", "breadth", "window", "stat1", "stat2"),
					Utils.aList("value1", "value2"));
		}
		
		@Override
		protected ExperimentResult postprocessMerged(ExperimentResult res) {
			res.addIntColumn("size", r -> Integer.parseInt(r.get("graph").split("-")[1]));
			res.addColumn("graphRead", r -> readGraph(r.get("graph")));
			res.addColumn("stat1Read", r -> readStat(r.get("stat1")));
			res.addColumn("stat2Read", r -> readStat(r.get("stat2")));
			res.addBoolColumn("revX", r -> r.get("stat1").startsWith("src"));
			res.addBoolColumn("revY", r -> r.get("stat2").startsWith("src"));
			res.addColumn("label", r -> r.concatVals("model", "size", "budget", "time", "stat1", "stat2"));
			return res;
		}
	}
	
	protected class HeatmapAggregator extends ExperimentAggregator {

		@Override
		public String getName() {
			return "heat";
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
			res.addColumn("highColor", r -> r.get("stat").startsWith("src") ? "khaki" : "firebrick3");
			res.addColumn("lowColor", r -> r.get("stat").startsWith("src") ? "navyblue" : "khaki");
			res.addColumn("label", r -> r.concatVals("graph", "model", "budget", "time", "stat"));
			res.forEach(r -> r.set("window", r.getInt("window") - 3));
			return res;
		}
	}

	protected static ExperimentResult expand(Stream<Row> rows, List<String> header, File experimentDir) {
		ExperimentResult er = new ExperimentResult(experimentDir, header, rows);
		er.expand("graph", ExPostContactTracingExperiment.HD_GRAPH);
		er.expand("model", ExPostContactTracingExperiment.HD_EPIDEMIC_MODEL);
		er.expand("time", ExPostContactTracingExperiment.HD_TIME);
		return er;
	}
	
	protected static ExperimentResult addRelColumn(ExperimentResult res, List<String> key) {
		Map<List<String>,Double> init = res.stream().filter(r -> r.getInt("breadth") == 0)
				.collect(Collectors.toMap( r -> r.getKey(key), r -> r.getDouble("value")));
		res.addDoubleColumn("rel", r -> r.getDouble("value") / init.getOrDefault(r.getKey(key), 1.));
		return res;
	}
	
	protected static ExperimentResult addRelColumn(ExperimentResult res) {
		return addRelColumn(res, Utils.aList("stat"));
	}
	
	protected String readGraph(String gName) {
		switch (gName.split("-")[0]) {
			case "tba" : return "Barabasi-Albert";
			case "ter" : return "Erdos-Renyi";
			case "tws" : return "Watts-Strogatz";
			default: return Utils.capitalize(gName);
		}
	}
	
	protected String readStat(String stat) {
		switch (stat) {
			case "everDetected": return "Detected infected and recovered";
			case "nowDetected": return "Detected infected";
			case "detected": return "Detected infected";
			case "srcTime": return "Earliest detected infection";
			case "srcDist": return "Distance to the real source";
			default: return Utils.capitalize(stat);
		}
	}
	
	protected String readStatNarrow(String stat) {
		switch (stat) {
			case "everDetected": return "Detected\\nI and R";
			case "nowDetected": return "Detected I";
			case "srcTime": return "Earliest detected\\ninfection time";
			case "srcDist": return "Distance\\nto the source";
			default: return Utils.capitalize(stat);
		}
	}
	
	protected String graphColor(String gName) {
		switch (gName.split("-")[0]) {
			case "tba" : return "#e41a1c";
			case "ter" : return "#377eb8";
			case "tws" : return "#4daf4a";
			default: return "black";
		}
	}
}
