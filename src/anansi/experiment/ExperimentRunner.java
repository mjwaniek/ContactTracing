package anansi.experiment;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import anansi.core.Graph;
import anansi.core.GraphGenerator;
import anansi.utils.FileReaderWriter;
import anansi.utils.Ref;
import anansi.utils.Utils;

/**
 * Running network experiments.
 * 
 * @author Marcin Waniek
 */
public abstract class ExperimentRunner {
	
	public static final String OUTPUT_DIR_NAME = "output";
	public static final String DATA_DIR_NAME = "data"; 

	public abstract String getDirectoryName();
	
	public abstract void runSingle(Object... params);
	
	public abstract List<ExperimentAggregator> getAggregators();
	
	public String getExperimentDirPath() {
		return Paths.get(OUTPUT_DIR_NAME, getDirectoryName()).toString();
	}
	
	private static String getDataDirPath(String experimentDirPath){
		return Paths.get(experimentDirPath, DATA_DIR_NAME).toString();
	}

	public String getDataPath(){
		return this.getDataPath((Graph)null);
	}
	
	public String getDataPath(Graph g){
		return getDataPath(g != null ? g.getName() : "basic");
	}
	
	public String getDataPath(String name){
		return Paths.get(getDataDirPath(getExperimentDirPath()), name).toString();
	}
	
	public void runMany(Graph g, int times, Object... params){
		if (g != null)
			g.resetGraph();
		for (int i = 0; i < times; ++i) {
			runSingle(Utils.concat(new Object[] {g}, params));
			if (g != null)
				g.resetGraph();
		}
	}
	
	public void runMany(int times, Object... params){
		for (int i = 0; i < times; ++i)
			runSingle(params);
	}
	
	public void runBarabasiAlbertDirected(int n, int avgDegree, int times, boolean directed, Object... params){
		for (int i = 0; i < times; ++i)
			runSingle(Utils.concat(
					new Object[] {GraphGenerator.generateBarabasiAlbertGraph(n, avgDegree, directed)}, params));
	}
	
	public void runBarabasiAlbert(int n, int avgDegree, int times, Object... params){
		runBarabasiAlbertDirected(n, avgDegree, times, false, params);
	}
	
	public void runScaleFreeConfigurationModelK(int n, double alpha, int kMin, int kMax, int times, Object... params){
		for (int i = 0; i < times; ++i)
			runSingle(Utils.concat(
					new Object[] {GraphGenerator.generateScaleFreeConfigurationModel(n, alpha, kMin, kMax)}, params));
	}
	
	public void runScaleFreeConfigurationModel(int n, double alpha, int times, Object... params){
		for (int i = 0; i < times; ++i)
			runSingle(Utils.concat(
					new Object[] {GraphGenerator.generateScaleFreeConfigurationModel(n, alpha)}, params));
	}
	
	public void runErdosRenyiDirected(int n, int avgDegree, int times, boolean directed, Object... params){
		for (int i = 0; i < times; ++i)
			runSingle(Utils.concat(
					new Object[] {GraphGenerator.generateErdosRenyiGraph(n, avgDegree, directed)}, params));
	}
	
	public void runErdosRenyi(int n, int avgDegree, int times, Object... params){
		runErdosRenyiDirected(n, avgDegree, times, false, params);
	}
	
	public void runSmallWorldDirected(int n, int avgDegree, double beta, int times, boolean directed, Object... params){
		for (int i = 0; i < times; ++i)
			runSingle(Utils.concat(
					new Object[] {GraphGenerator.generateSmallWorldGraph(n, avgDegree, beta, directed)}, params));
	}
	
	public void runSmallWorld(int n, int avgDegree, double beta, int times, Object... params){
		runSmallWorldDirected(n, avgDegree, beta, times, false, params);
	}
	
	public void runPruferTrees(int n, int times, Object... params){
		for (int i = 0; i < times; ++i)
			runSingle(Utils.concat(new Object[] {GraphGenerator.generatePruferTree(n)}, params));
	}
	
	public boolean isRandom(Graph g){
		String s = g.getName().substring(0, 3);
		return "ba-".equals(s) || "er-".equals(s) || "ws-".equals(s)
				|| "dba".equals(s) || "der".equals(s) || "dws".equals(s);
	}
	
	public void forEachResult(Consumer<ExperimentResult> c) {
		File dataDir = new File(getDataDirPath(getExperimentDirPath()));
		if (dataDir.exists())
			for (File graphDir : dataDir.listFiles())
				for (File resDir : graphDir.listFiles())
					c.accept(ExperimentResult.loadResult(resDir.getAbsolutePath()));
	}
	
	public Stream<ExperimentResult> streamResults(){
		List<ExperimentResult> results = new ArrayList<>();
		forEachResult(res -> results.add(res));
		return results.stream();
	}
	
	public Stream<Row> getRowStream(){
		return streamResults().flatMap(res -> res.stream());
	}
	
	public Stream<File> streamResultDirs(){
		List<File> results = new ArrayList<>();
		File dataDir = new File(getDataDirPath(getExperimentDirPath()));
		if (dataDir.exists())
			for (File graphDir : dataDir.listFiles())
				for (File resDir : graphDir.listFiles())
					results.add(resDir);
		return results.stream();
	}
	
	public Stream<List<String>> streamHeaders(){
		return streamResultDirs().map(dir -> ExperimentResult.loadHeader(dir.getAbsolutePath()));
	}
	
	public void mergeAll(boolean removeParts) {
		System.out.println("Starting merging...");
		File dataDir = new File(getDataDirPath(getExperimentDirPath()));
		if (dataDir.exists())
			for (File graphDir : dataDir.listFiles()) {
				System.out.println("Merging " + graphDir.getName());
				File[] resDirs = graphDir.listFiles();
				if (resDirs.length > 0) {
					Ref<ExperimentResult> merge = new Ref<>(null);
					for (File resDir : resDirs) {
						Stream<Row> rs = ExperimentResult.loadRows(resDir.getAbsolutePath());
						if (merge.get() == null)
							merge.set(new ExperimentResult(
									graphDir.toPath().resolve("merged").toFile(), Utils.aList(), rs));
						else
							rs.forEach(r -> merge.get().addRowDirectlyDontPrint(r));
						rs.close();
						if (removeParts)
							FileReaderWriter.deleteFile(resDir.getAbsolutePath());
					}
					merge.get().saveResult();
				}
			}
		System.out.println("Finished merging.\n");
	}
	
	public void aggregateAll(){
		System.out.println("Starting aggregation...");
		for (ExperimentAggregator ea : getAggregators())
			ea.aggregateResults(getExperimentDirPath()).saveResult();
		System.out.println("Finished aggregation.\n");
	}
	
	public void printCharts() {
		System.out.println("Starting printing...");
		for (ExperimentAggregator ea : getAggregators()) {
			System.out.println("Printer " + ea.getName());
			String path = ea.getAggregatorPath(getExperimentDirPath());
			if (new File(path).exists())
				ea.printCharts(ExperimentResult.loadResult(path));
			else
				System.out.println("\tResults file does not exist");
		}
		System.out.println("Finished printing.\n");
	}
	
	public void filterRows(Predicate<Row> f) {
		forEachResult(res ->{ 
			res.filter(f);
			res.saveResult();
		});
	}
	
	public void filterIncompleteRows() {
		forEachResult(res ->{
			int beforeSize = res.size(); 
			res.filter(r -> r.size() == res.getColNames().size());
			if (res.size() < beforeSize) {
				System.out.println("Removed " + (beforeSize - res.size()) + " rows from " + res.getResultFile());
				res.saveResult();
			}
		});
	}
}
