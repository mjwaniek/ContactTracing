package anansi.experiment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Single row of experiment result data
 * 
 * @author Marcin Waniek
 */
public class Row implements Iterable<Object> {

	private List<String> colNames;
	private List<Object> elements;
	
	public Row(List<String> colNames, List<Object> elements) {
		this.colNames = colNames;
		this.elements = elements;
	}
	
	public Row(Row r) {
		this.colNames = r.colNames;
		this.elements = new ArrayList<>(r.elements);
	}

	public List<String> getColNames() {
		return colNames;
	}
	
	public void setColNames(List<String> colNames) {
		this.colNames = colNames;
	}
	
	public List<Object> getElements() {
		return elements;
	}
	
	public List<String> getElementsAsStrings() {
		return elements.stream().map(o -> o.toString()).collect(Collectors.toList());
	}
	
	private int indexOf(String colName) {
		return colNames.indexOf(colName);
	}
	
	public int size() {
		return elements.size();
	}
	
	public void add(String s) {
		elements.add(s);
	}
	
	public void remove(String colName){
		remove(colNames.indexOf(colName));
	}
	
	public void remove(int i){
		elements.remove(i);
	}
	
	public String get(Integer i) {
		return elements.get(i).toString();
	}
	
	public String get(String colName) {
		return get(indexOf(colName));
	}
	
	public Boolean getBool(Integer i) {
		return Boolean.parseBoolean(elements.get(i).toString());
	}
	
	public Boolean getBool(String colName) {
		return getBool(indexOf(colName));
	}
	
	public Integer getInt(Integer i) {
		return Integer.parseInt(elements.get(i).toString());
	}
	
	public Integer getInt(String colName) {
		return getInt(indexOf(colName));
	}

	public Double getDouble(Integer i) {
		return Double.parseDouble(elements.get(i).toString());
	}
	
	public Double getDouble(String colName) {
		return getDouble(indexOf(colName));
	}
	
	public void set(Integer index, Object element) {
		elements.set(index, element.toString());
	}
	
	public void set(String colName, Object element) {
		set(indexOf(colName), element);
	}
	
	public void update(String colName, Function<String, String> f) {
		set(colName, f.apply(get(colName)));
	}
	
	public void updateInt(String colName, Function<Integer, Integer> f) {
		set(colName, f.apply(getInt(colName)));
	}
	
	public void updateDouble(String colName, Function<Double, Double> f) {
		set(colName, f.apply(getDouble(colName)));
	}
	
	public List<String> getKey(List<String> keyCols){
		List<String> res = new ArrayList<>();
		for (String col : keyCols)
			res.add(get(col));
		return res;
	}

	public List<String> getKey(String... keyCols){
		return getKey(Arrays.asList(keyCols));
	}
	
	public String concatVals(String... cols){
		return concatVals(Arrays.asList(cols), "-");
	}
	
	public String concatVals(List<String> cols){
		return concatVals(cols, "-");
	}
	
	public String concatVals(List<String> cols, String sep){
		String res = getKey(cols).stream().reduce("", (x, y) -> x + sep + y);
		if (!res.isEmpty())
			res = res.substring(1);
		return res;
	}
	
	@Override
	public String toString() {
		return elements.toString();
	}

	@Override
	public Iterator<Object> iterator() {
		return elements.iterator();
	}
	
	@Override
	public int hashCode() {
		return elements.hashCode();
	}
}
