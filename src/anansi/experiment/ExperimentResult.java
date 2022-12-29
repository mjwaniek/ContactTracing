package anansi.experiment;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import anansi.utils.FileReaderWriter;
import anansi.utils.Ref;
import anansi.utils.Utils;

/**
 * A result of an experiment, consisting of a set of rows and a header.
 * 
 * @author Marcin Waniek
 */
public class ExperimentResult implements Iterable<Row> {

	public static final String RES_FILE = "res.csv";
	
	private String resultId;
	private File resultDir;
	
	private List<String> header;
	private List<String> colNames;
	private List<Row> rows;
	
	private List<File> otherFiles;
	
	/**
	 * Constructor for creating new result.
	 */	
	public ExperimentResult(String experimentName, String resultsDirPath, List<String> header, List<String> colNames){
		this(experimentName, true, resultsDirPath, header, colNames);
	}
	
	/**
	 * Constructor for creating new result.
	 */	
	public ExperimentResult(String experimentName, boolean timestamp, String resultsDirPath,
			List<String> header, List<String> colNames){
		if (timestamp)
			this.resultId = experimentName + "-" + Utils.timestamp();
		else
			this.resultId = experimentName;
		this.resultDir = Paths.get(resultsDirPath, resultId).toFile();
		this.header = header;
		this.colNames = colNames;
		this.rows = new ArrayList<>();
		this.otherFiles = new ArrayList<>();
	}
	
	/**
	 * Constructor for creating new result.
	 */	
	public ExperimentResult(String resultDirPath, List<String> header, List<String> colNames){
		Path p = Paths.get(resultDirPath);
		this.resultId = p.getFileName().toString();
		this.resultDir = p.toFile();
		this.header = header;
		this.colNames = colNames;
		this.rows = new ArrayList<>();
		this.otherFiles = new ArrayList<>();
	}
	
	/**
	 * Constructor for loading result from file.
	 */
	public ExperimentResult(File resultDir, List<String> header, List<String> colNames){
		this.resultId = resultDir.getName();
		this.resultDir = resultDir;
		this.header = header;
		this.colNames = colNames;
		this.rows = new ArrayList<>();
		this.otherFiles = new ArrayList<>();
	}
	
	/**
	 * Constructor for loading result from stream of rows.
	 */
	public ExperimentResult(File resultDir, List<String> header, Stream<Row> rows){
		this.resultId = resultDir.getName();
		this.resultDir = resultDir;
		this.header = header;
		this.rows = rows.collect(Collectors.toList());
		this.colNames = this.rows.isEmpty() ? new ArrayList<>() : this.rows.get(0).getColNames();
		this.rows.forEach(r -> r.setColNames(this.colNames));
		this.otherFiles = new ArrayList<>();
	}
	
	/**
	 * Copy constructor.
	 */
	public ExperimentResult(ExperimentResult res){
		this.resultId = res.resultId;
		this.resultDir = res.resultDir;
		this.header = new ArrayList<>(res.header);
		this.colNames = new ArrayList<>(res.colNames);
		this.rows = new ArrayList<>();
		res.rows.forEach(r -> this.rows.add(new Row(r)));
		this.rows.forEach(r -> r.setColNames(this.colNames));
		this.otherFiles = new ArrayList<>(res.otherFiles);
	}

	public File getResultDir() {
		return resultDir;
	}
	
	public void setResultDir(String resultDirPath) {
		this.resultDir = new File(resultDirPath);
	}
	
	public File getResultFile(){
		return Paths.get(resultDir.getAbsolutePath(), RES_FILE).toFile();
	}
	
	public List<String> getHeader() {
		return header;
	}
	
	public List<String> getColNames() {
		return colNames;
	}
	
	public List<Row> getRows() {
		return rows;
	}
	
	public Stream<Row> stream(){
		return rows.stream();
	}
	
	public int size() {
		return rows.size();
	}
	
	public void addAnotherFile(File f) {
		otherFiles.add(f);
	}
	
	public List<File> getOtherFiles() {
		return otherFiles;
	}
	
	public Row addRow(Object... elems){
		Row r = addRowDontPrint(elems);
		System.out.println(r.getElements());
		return r;
	}
	
	public Row addRowDontPrint(Object... elems){
		assert(elems.length == colNames.size());
		return addRowDirectlyDontPrint(new Row(colNames, 
				Arrays.stream(elems).map(o -> o == null ? "NULL" : o).collect(Collectors.toList())));
	}
	
	public Row addRowDirectly(Row r){
		addRowDirectlyDontPrint(r);
		System.out.println(r.getElements());
		return r;
	}

	public Row addRowDirectlyDontPrint(Row r){
		rows.add(r);
		r.setColNames(colNames);
		return r;
	}	
	
	public void addRowsDontPrint(Collection<Row> elems) {
		elems.forEach(r -> addRowDirectlyDontPrint(r));
	}
	
	public Row getLastRow(){
		return Utils.last(rows);
	}
	
	public void addColumn(String colName, Function<Row, String> f) {
		removeColumn(colName);
		colNames.add(colName);
		for (Row r : rows)
			r.add(f.apply(r));
	}
	
	public void addBoolColumn(String colName, Function<Row, Boolean> f) {
		removeColumn(colName);
		colNames.add(colName);
		for (Row r : rows)
			r.add(f.apply(r).toString());
	}
	
	public void addIntColumn(String colName, Function<Row, Integer> f) {
		removeColumn(colName);
		colNames.add(colName);
		for (Row r : rows)
			r.add(f.apply(r).toString());
	}
	
	public void addDoubleColumn(String colName, Function<Row, Double> f) {
		removeColumn(colName);
		colNames.add(colName);
		for (Row r : rows)
			r.add(f.apply(r).toString());
	}
	
	public void removeColumn(String colName){
		if (colNames.contains(colName)) {
			for (Row r : rows)
				r.remove(colName);
			colNames.remove(colName);
		}
	}
	
	public void renameColumn(String oldName, String newName) {
		if (colNames.contains(oldName))
			colNames.set(colNames.indexOf(oldName), newName);
	}
	
	public void expand(String colName, int headerIndex) {
		addColumn(colName, r -> header.get(headerIndex));
	}
	
	public void print() {
		print(size());
	}
	
	public void print(int k) {
		System.out.println(header);
		System.out.println(colNames);
		rows.stream().limit(k).forEach(r -> System.out.println(r));
	}

	@Override
	public Iterator<Row> iterator() {
		return rows.iterator();
	}
	
	public void clear() {
		rows.clear();
	}
	
	public ExperimentResult filter(Predicate<Row> filter){
		rows.removeIf(filter.negate());
		return this;
	}
	
	public void aggregateInPlace(List<String> aggrKey, List<String> aggrFields) {
		rows = ExperimentAggregator.aggregate(rows.stream(), aggrKey, aggrFields)
				.collect(Collectors.toList());
		colNames = rows.get(0).getColNames(); 
	}
	
	public ExperimentResult aggregateToNew(List<String> aggrKey, List<String> aggrFields) {
		return new ExperimentResult(resultDir, header,
				ExperimentAggregator.aggregate(rows.stream(), aggrKey, aggrFields));
	}
	
	public Map<List<String>, List<Row>> groupByKey(String... keyCols){
		return groupByKey(Arrays.asList(keyCols));
	}
	
	public Map<List<String>, List<Row>> groupByKey(List<String> keyCols){
		Map<List<String>, List<Row>> res = new HashMap<>();
		for (Row r : rows) {
			List<String> key = r.getKey(keyCols);
			if (!res.containsKey(key))
				res.put(key, new ArrayList<>());
			res.get(key).add(r);
		}
		return res;
	}
	
	public void addRelColumn(String newCol, String baseCol, List<String> keyCols, Predicate<Row> init) {
		Map<List<String>, Row> inits = new HashMap<List<String>, Row>();
		groupByKey(keyCols).forEach((key, rows) -> inits.put(key, rows.stream().filter(init).findAny().get()));
		addDoubleColumn(newCol, r -> r.getDouble(baseCol) / inits.get(r.getKey(keyCols)).getDouble(baseCol));
	}
	
	public void addDiffColumn(String newCol, String baseCol, List<String> keyCols, Predicate<Row> init) {
		Map<List<String>, Row> inits = new HashMap<List<String>, Row>();
		groupByKey(keyCols).forEach((key, rows) -> inits.put(key, rows.stream().filter(init).findAny().get()));
		addDoubleColumn(newCol, r -> r.getDouble(baseCol) - inits.get(r.getKey(keyCols)).getDouble(baseCol));
	}
	
	public void keepLastOnly(List<String> aggrKey, String timeCol) {
		keepLastOnly(aggrKey, timeCol, r -> false);
	}
	
	public void keepLastOnly(List<String> aggrKey, String timeCol, Predicate<Row> keepAlso) {
		List<String> keyCols = new ArrayList<>(aggrKey);
		keyCols.remove(timeCol);
		Set<Row> keep = new HashSet<>();
		for (List<Row> lr : groupByKey(keyCols).values())
			keep.add(Utils.argmax(lr, r -> r.getDouble(timeCol)));
		filter(r -> keep.contains(r) || keepAlso.test(r));
	}
	
	public void keepFirstOnly(List<String> aggrKey, String timeCol) {
		keepFirstOnly(aggrKey, timeCol, r -> false);
	}
	
	public void keepFirstOnly(List<String> aggrKey, String timeCol, Predicate<Row> keepAlso) {
		List<String> keyCols = new ArrayList<>(aggrKey);
		keyCols.remove(timeCol);
		Set<Row> keep = new HashSet<>();
		for (List<Row> lr : groupByKey(keyCols).values())
			keep.add(Utils.argmin(lr, r -> r.getDouble(timeCol)));
		filter(r -> keep.contains(r) || keepAlso.test(r));
	}

	public void fillTimeGaps(List<String> aggrKey, String timeCol, int lowerBound, int upperBound,
			Function<Integer, Boolean> tOk, Consumer<Row> processNew) {
		List<String> keyCols = new ArrayList<>(aggrKey);
		keyCols.remove(timeCol);
		groupByKey(keyCols).forEach((key, lr) -> {
			Collections.sort(lr, (o1, o2) -> o1.getInt(timeCol).compareTo(o2.getInt(timeCol)));
			for (Integer t = lowerBound; t < lr.get(0).getInt(timeCol); ++t)
				if (tOk.apply(t)){
					Row newRow = new Row(lr.get(0));
					newRow.set(timeCol, t);
					processNew.accept(newRow);
					addRowDirectlyDontPrint(newRow);
				}
			for (int i = 1; i < lr.size(); ++i)
				for (Integer t = lr.get(i - 1).getInt(timeCol) + 1; t < lr.get(i).getInt(timeCol); ++t)
					if (tOk.apply(t)){
						Row newRow = new Row(lr.get(i - 1));
						newRow.set(timeCol, t);
						processNew.accept(newRow);
						addRowDirectlyDontPrint(newRow);
					}
			for (Integer t = Utils.last(lr).getInt(timeCol) + 1; t <= upperBound; ++t)
				if (tOk.apply(t)){
					Row newRow = new Row(Utils.last(lr));
					newRow.set(timeCol, t.toString());
					processNew.accept(newRow);
					addRowDirectlyDontPrint(newRow);
				}
		});
	}
	
	public void fillTimeGaps(List<String> aggrKey, String timeCol, int lowerBound, int upperBound) {
		fillTimeGaps(aggrKey, timeCol, lowerBound, upperBound, t -> true, r -> {});
	}
	
	public void fillTimeGaps(List<String> aggrKey, String timeCol) {
		fillTimeGaps(aggrKey, timeCol, Integer.MAX_VALUE, Integer.MIN_VALUE, t -> true, r -> {});
	}
	
	public void unifyTimelines(List<String> aggrKey, String timeColumn, Consumer<Row> processNew) {
		List<String> keyCols = new ArrayList<>(aggrKey);
		keyCols.remove(timeColumn);
		Set<Integer> occuring = new HashSet<>();
		rows.stream().forEach(r -> occuring.add(r.getInt(timeColumn)));
		fillTimeGaps(keyCols, timeColumn, occuring.stream().mapToInt(x -> x.intValue()).min().getAsInt(),
				occuring.stream().mapToInt(x -> x.intValue()).max().getAsInt(), t -> occuring.contains(t), processNew);
	}
	
	public void unifyTimelines(List<String> aggrKey, String timeColumn) {
		unifyTimelines(aggrKey, timeColumn, r -> {});
	}

	public ExperimentResult saveResult(){
		return saveResult(getResultFile().getAbsolutePath());
	}
	
	public ExperimentResult saveResult(String filePath){
		FileReaderWriter.exportToCSV(
				Stream.concat(Stream.of(header, colNames), rows.stream().map(r -> r.getElementsAsStrings())), filePath);
		return this;
	}
	
	/**
	 * Returns a stream of rows from the file.
	 * <strong>The stream has to be closed</strong>.
	 */
	public static Stream<Row> loadRows(String resPath, Ref<List<String>> header){
		Path p = Paths.get(resPath);
		if (p.toFile().isDirectory())
			p = p.resolve(RES_FILE);
		try (Stream<String> hs = FileReaderWriter.getFileStream(p.toString())){
			header.set(FileReaderWriter.splitCSVLine(hs.findFirst().orElse("")));
			Stream<String> lines = FileReaderWriter.getFileStream(p.toString());
			List<String> colNames = new ArrayList<>();
			return lines.skip(1)
					.map(s -> FileReaderWriter.splitCSVLine(s))
					.filter(l -> colNames.isEmpty() ? !colNames.addAll(l) : true)
					.peek(l -> {
						if (l.size() < colNames.size())
							System.err.println("Incomplete row in " + resPath);
					})
					.map(l -> new Row(colNames, new ArrayList<>(l)));
		} catch (IOException e) {
			System.err.println("Error while reading experiment result file.");
			e.printStackTrace();
		}
		return null;
	}
	
	public static Stream<Row> loadRows(String resPath){
		return loadRows(resPath, new Ref<List<String>>());
	}
	
	public static List<String> loadHeader(String resPath){
		Path p = Paths.get(resPath);
		if (p.toFile().isDirectory())
			p = p.resolve(RES_FILE);
		try (Stream<String> hs = FileReaderWriter.getFileStream(p.toString())){
			return FileReaderWriter.splitCSVLine(hs.findFirst().orElse(""));
		} catch (IOException e) {
			System.err.println("Error while reading experiment result header.");
			e.printStackTrace();
		}
		return null;
	}
	
	public static ExperimentResult loadResult(String resPath) {
		Path p = Paths.get(resPath);
		if (p.toFile().isDirectory())
			p = Paths.get(resPath, RES_FILE);
		File resDir = p.toFile().getParentFile();
		if (p.toFile().exists())
			try (Stream<String> lines = FileReaderWriter.getFileStream(p.toString())) {
				ExperimentResult res = new ExperimentResult(resDir, new ArrayList<>(), new ArrayList<>());
				lines.map(s -> FileReaderWriter.splitCSVLine(s))
					.filter(l -> res.getHeader().isEmpty()
						? !res.getHeader().addAll(l)
						: (res.getColNames().isEmpty()
								? !res.getColNames().addAll(l)
								: true))
					.map(l -> new Row(res.getColNames(), new ArrayList<>(l)))
					.forEach(r -> res.addRowDirectlyDontPrint(r));
				for (File f : resDir.listFiles())
					if (!f.getName().equals(p.getFileName().toString()))
						res.addAnotherFile(f);
				return res;
			} catch (IOException e) {
				System.err.println("Error while reading experiment result file.");
				e.printStackTrace();
			}
		return null;
	}
	
	public static ExperimentResult merge(ExperimentResult... results) {
		ExperimentResult merged = new ExperimentResult(results[0]);
		for (int i = 1; i < results.length; ++i) {
			assert(results[i].colNames.equals(merged.colNames));
			results[i].stream().forEach(r -> merged.addRowDirectlyDontPrint(r));
		}
		return merged;
	}
}
