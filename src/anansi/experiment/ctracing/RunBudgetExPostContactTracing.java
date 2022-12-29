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

public class RunBudgetExPostContactTracing extends RunExPostContactTracing {

	public static final int BREADTH = 10;
	public static final int WINDOW_OFFSET = 3;
		
	public static void main(String[] args) {
		RunBudgetExPostContactTracing r = new RunBudgetExPostContactTracing();
		int times = args.length > 0 ? Integer.parseInt(args[0]) : 10;
		int n = args.length > 1 ? Integer.parseInt(args[1]) : 10000;
		int avgDegr = args.length > 2 ? Integer.parseInt(args[2]) : 50;
		int budgetFrom = args.length > 3 ? Integer.parseInt(args[3]) : 25;
		int budgetTo = args.length > 4 ? Integer.parseInt(args[4]) : 200;
		int budgetBy = args.length > 5 ? Integer.parseInt(args[5]) : 25;
		
		for (int iter = 0; iter < times; ++iter) {
			r.runSingle(r.ter(n, avgDegr, TIMESTEPS), budgetFrom, budgetTo, budgetBy);
			r.runSingle(r.tws(n, avgDegr, .25, TIMESTEPS), budgetFrom, budgetTo, budgetBy);
			r.runSingle(r.tba(n, avgDegr, TIMESTEPS), budgetFrom, budgetTo, budgetBy);
		}
		
		r.aggregateAll();
		r.printCharts();
	}
	
	@Override
	public String getDirectoryName() {
		return "expost-tctracing-budget";
	}

	@Override
	public void runSingle(Object... params) {
		TemporalGraph g = (TemporalGraph) params[0];
		int budgetFrom = (int) params[1];
		int budgetTo = (int) params[2];
		int budgetBy = (int) params[3];
		TemporalSEIRHDCovidModel em = new TemporalSEIRHDCovidModel();
		List<Integer> budgets = Utils.aList();
		for (int b = budgetFrom; b <= budgetTo; b += budgetBy)
			budgets.add(b);
		List<Integer> breadths = Utils.aList(BREADTH);
		List<Integer> windowOffsets = Utils.aList(WINDOW_OFFSET);
		new ExPostContactTracingExperiment(getDataPath(g), g, em, budgets, breadths, windowOffsets).perform();
	}
	
	@Override
	public List<ExperimentAggregator> getAggregators() {
		return Utils.aList(new BudgetAggregator());
	}
	
	protected class BudgetAggregator extends ExperimentAggregator {

		@Override
		public String getName() {
			return "budget";
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
			res.addColumn("label", r -> r.concatVals("model", "time", "stat"));
			res.addColumn("labelY", r -> readStat(r.get("stat")));
			return res;
		}
	}
}
