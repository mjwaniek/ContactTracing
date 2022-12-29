package anansi.experiment;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import anansi.utils.Ref;
import anansi.utils.StatisticsCounter;
import anansi.utils.Utils;

/**
 * Aggregating results of a set of experiment results.
 * 
 * @author Marcin Waniek
 */
public abstract class ExperimentAggregator {
	
	public abstract String getName();
	
	protected Stream<File> preprocessGroupFiles(Stream<File> expResults) { return expResults; }
	
	protected Stream<Row> processEvery(Stream<Row> rows, List<String> header, File experimentDir) { return rows; }
	
	protected Stream<Row> processGroup(Stream<Row> rows, File groupDir) { return rows; }
	
	protected Stream<Row> processMerged(Stream<Row> rows, File mergedDir) { return rows; }

	protected ExperimentResult postprocessMerged(ExperimentResult res) { return res; }
	
	public void printCharts(ExperimentResult res) {}
	
	public String getAggregatorPath(String experimentDirPath){
		return Paths.get(experimentDirPath, getName()).toString();
	}
	
	public ExperimentResult aggregateResults(String experimentDirPath){
		File dataDir = Paths.get(experimentDirPath, ExperimentRunner.DATA_DIR_NAME).toFile();
		List<Stream<Row>> toClose = new ArrayList<>();
		List<Stream<Row>> groupStreams = new ArrayList<>();
		System.out.println("Aggregator " + getName());
		for (File groupDir : dataDir.listFiles())
			if (groupDir.isDirectory()) {
				System.out.println("\tAggregating " + groupDir.getName());
				List<Stream<Row>> expStreams = new ArrayList<>();
				preprocessGroupFiles(Arrays.stream(groupDir.listFiles()).filter(f -> f.isDirectory())
						.map(f -> Paths.get(f.getPath(), ExperimentResult.RES_FILE).toFile()).filter(f -> f.exists()))
						.forEach(expRes -> {
							Ref<List<String>> header = new Ref<>();
							Stream<Row> expStream = ExperimentResult.loadRows(expRes.getPath(), header);
							expStreams.add(processEvery(expStream, header.get(), expRes.getParentFile()));
							toClose.add(expStream);
						});
				groupStreams.add(processGroup(expStreams.stream().flatMap(i -> i), groupDir));
			}
		File mergedDir = Paths.get(experimentDirPath, getName()).toFile();
		ExperimentResult res = new ExperimentResult(mergedDir, Utils.aList(getName()),
				processMerged(groupStreams.stream().flatMap(i -> i), mergedDir));
		res = postprocessMerged(res);
		toClose.forEach(s -> s.close());
		return res;
	}
	
	/**
	 * Computing mean, standard deviation and 95%-confidence interval of data in a stream of rows.
	 */
	public static Stream<Row> aggregate(Stream<Row> rows, List<String> aggrKey, List<String> aggrFields){
		Map<List<String>, Map<String, StatisticsCounter>> counters = new HashMap<>();
		rows.forEach(r -> {
			List<String> key = r.getKey(aggrKey);
			Map<String, StatisticsCounter> m = counters.computeIfAbsent(key, __ -> new HashMap<>());
			aggrFields.forEach(af -> m.compute(af, (__,c) -> c == null
						? new StatisticsCounter(r.getDouble(af)) : c.addValue(r.getDouble(af))));
		});
		List<String> columns = new ArrayList<>();
		columns.addAll(aggrKey);
		for (String af : aggrFields) {
			columns.add(af + "Mean");
			columns.add(af + "SD");
			columns.add(af + "C95");
		}
		Stream<Row> res = counters.keySet().stream().map(key -> {
			List<Object> elems = new ArrayList<>();
			elems.addAll(key);
			aggrFields.forEach(af -> {
				StatisticsCounter c = counters.get(key).get(af); 
				elems.add(c.getMean());
				elems.add(c.getSD());
				elems.add(c.getConf95());
			});
			return new Row(columns, elems);
		});
		return res;
	}
	
	/**
	 * First step of computing mean, standard deviation and 95%-confidence interval of data in a stream of rows
	 * in a memory-efficient way. The step should be performed for every result.
	 */
	public static Stream<Row> lowMemoryAggregateStep1(Stream<Row> rows, List<String> aggrKey, List<String> aggrFields){
		Map<List<String>, Map<String, StatisticsCounter>> counters = new HashMap<>();
		rows.forEach(r -> {
			List<String> key = r.getKey(aggrKey);
			Map<String, StatisticsCounter> m = counters.computeIfAbsent(key, __ -> new HashMap<>());
			aggrFields.forEach(af -> m.compute(af, (__,c) -> c == null
						? new StatisticsCounter(r.getDouble(af)) : c.addValue(r.getDouble(af))));
		});
		List<String> columns = new ArrayList<>();
		columns.addAll(aggrKey);
		for (String af : aggrFields) {
			columns.add("_" + af + "Sum");
			columns.add("_" + af + "SumSq");
			columns.add("_" + af + "N");
		}
		Stream<Row> res = counters.keySet().stream().map(key -> {
			List<Object> elems = new ArrayList<>();
			elems.addAll(key);
			aggrFields.forEach(af -> {
				StatisticsCounter c = counters.get(key).get(af); 
				elems.add(c.getSum());
				elems.add(c.getSumSq());
				elems.add(c.getN());
			});
			return new Row(columns, elems);
		});
		return res;
	}
	
	/**
	 * Second step of computing mean, standard deviation and 95%-confidence interval of data in a stream of rows
	 * in a memory-efficient way. The step should be performed to obtain the final aggregated stream.
	 */
	public static Stream<Row> lowMemoryAggregateStep2(Stream<Row> rows, List<String> aggrKey, List<String> aggrFields){
		Map<List<String>, Map<String, StatisticsCounter>> counters = new HashMap<>();
		rows.forEach(r -> {
			List<String> key = r.getKey(aggrKey);
			Map<String, StatisticsCounter> m = counters.computeIfAbsent(key, __ -> new HashMap<>());
			aggrFields.forEach(af -> {
				double sum = r.getDouble("_" + af + "Sum");
				double sumSq = r.getDouble("_" + af + "SumSq");
				long n = r.getInt("_" + af + "N");
				m.compute(af, (__,c) -> c == null ? new StatisticsCounter(sum, sumSq, n) : c.addCounter(sum, sumSq, n));
			});
		});
		List<String> columns = new ArrayList<>();
		columns.addAll(aggrKey);
		for (String af : aggrFields) {
			columns.add(af + "Mean");
			columns.add(af + "SD");
			columns.add(af + "C95");
		}
		Stream<Row> res = counters.keySet().stream().map(key -> {
			List<Object> elems = new ArrayList<>();
			elems.addAll(key);
			aggrFields.forEach(af -> {
				StatisticsCounter c = counters.get(key).get(af); 
				elems.add(c.getMean());
				elems.add(c.getSD());
				elems.add(c.getConf95());
			});
			return new Row(columns, elems);
		});
		return res;
	}
	
	/**
	 * Computing sum of fields in data in a stream of rows.
	 */
	public static Stream<Row> sum(Stream<Row> rows, List<String> aggrKey, List<String> sumFields){
		Map<List<String>, Map<String, Double>> counters = new HashMap<>();
		rows.forEach(r -> {
			List<String> key = r.getKey(aggrKey);
			Map<String, Double> m = counters.computeIfAbsent(key, __ -> new HashMap<>());
			sumFields.forEach(sf -> m.compute(sf, (__,c) -> c == null ? r.getDouble(sf) : c + r.getDouble(sf)));
		});
		List<String> columns = new ArrayList<>();
		columns.addAll(aggrKey);
		for (String sf : sumFields)
			columns.add(sf + "Sum");
		Stream<Row> res = counters.keySet().stream().map(key -> {
			List<Object> elems = new ArrayList<>();
			elems.addAll(key);
			sumFields.forEach(sf -> elems.add(counters.get(key).get(sf)));
			return new Row(columns, elems);
		});
		return res;
	}
	
	/**
	 * Computing histogram of data in a stream of rows.
	 */
	public static Stream<Row> histogram(Stream<Row> rows, List<String> aggrKey){
		Map<List<String>, Integer> counters = new HashMap<>();
		rows.forEach(r -> {
			List<String> key = r.getKey(aggrKey);
			counters.compute(key, (__, x) -> x == null ? 1 : x + 1);
		});
		List<String> columns = new ArrayList<>();
		columns.addAll(aggrKey);
		columns.add("count");
		Stream<Row> res = counters.keySet().stream().map(key -> {
			List<Object> elems = new ArrayList<>();
			elems.addAll(key);
			elems.add(counters.get(key));
			return new Row(columns, elems);
		});
		return res;
	}
	
	public static Map<List<String>, List<Row>> groupByKey(Stream<Row> rows, List<String> keyCols){
		Map<List<String>, List<Row>> res = new HashMap<>();
		rows.forEach(r -> {
			List<String> key = r.getKey(keyCols);
			if (!res.containsKey(key))
				res.put(key, new ArrayList<>());
			res.get(key).add(r);
		}); 
		return res;
	}
}
