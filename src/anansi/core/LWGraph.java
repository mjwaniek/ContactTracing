package anansi.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Stream;

/**
 * Representation of a labeled and weighted graph.
 * 
 * @author Marcin Waniek
 */
public class LWGraph<V,E> extends Graph {

	private List<V> labels;
	private Map<V, Coalition> indices;
	private List<Map<Integer, E>> weights;
	private Stack<WeightedChange> history;
	
	public LWGraph(String name, int n, boolean directed){
		super(name, n, directed);
		initLW(null);
	}
	
	public LWGraph(String name, int n){
		super(name, n);
		initLW(null);
	}
	
	public LWGraph(Graph g) {
		super(g);
		initLW(null);
		g.edgesStream().forEach(e -> setWeight(e, null));
	}
	
	public LWGraph(LWGraph<V,E> g) {
		this(g, new ArrayList<>(g.labels));
		weights = new ArrayList<>();
		for (int i = 0; i < size(); ++i)
			weights.add(new HashMap<>(g.weights.get(i)));
	}
	
	public LWGraph(String name, List<V> labels, boolean directed){
		super(name, labels.size(), directed);
		initLW(labels);
	}
	
	public LWGraph(String name, List<V> labels){
		super(name, labels.size());
		initLW(labels);
	}
	
	public LWGraph(Graph g, List<V> labels) {
		super(g);
		initLW(labels);
	}
	
	private void initLW(List<V> labels) {
		this.indices = new HashMap<>();
		if (labels == null) {
			this.labels = new ArrayList<>();
			for (int i = 0; i < size(); ++i)
				this.labels.add(null);
			indices.put(null, Coalition.getFull(size()));
		} else {
			this.labels = labels;
			for (int i = 0; i < size(); ++i) {
				final int fi = i;
				indices.compute(labels.get(fi), (v,c) -> c == null ? new Coalition(fi) : c.add(fi));
			}
		}
		this.weights = new ArrayList<>();
		for (int i = 0; i < size(); ++i)
			this.weights.add(new HashMap<>());
	}
	
	public V l(int i) {
		return labels.get(i);
	}
	
	public List<V> getLabels() {
		return labels;
	}
	
	public void setLabel(int i, V label) {
		indices.get(labels.get(i)).remove(i);
		indices.compute(label, (v,c) -> c == null ? new Coalition(i) : c.add(i));
		labels.set(i, label);
	}
	
	public int findNode(V v){
		return indices.get(v).getAny();
	}
	
	public boolean containsLabel(V v){
		return indices.containsKey(v);
	}
	
	public Coalition findAllNodes(V v){
		return indices.get(v);
	}
	
	public boolean addLWEdge(V v, V u){
		return addEdge(findNode(v), findNode(u));
	}
	
	public boolean addEdge(int i, int j){
		return addEdge(i, j, null);
	}

	public boolean addEdge(Edge e){
		return addEdge(e.i(), e.j());
	}
	
	public boolean addEdge(int i, int j, E w){
		if (i != j && !containsEdge(i, j)){
			performAddEdge(i, j, w);
			if (history != null)
				history.push(new WeightedAddition(i, j));
			notifyListenersAdd(new Edge(i, j, isDirected()));
			return true;
		} else
			return false;
	}
	
	public boolean addEdge(Edge e, E w){
		return addEdge(e.i(), e.j(), w);
	}
	
	public boolean addLWEdge(V v, V u, E w){
		return addEdge(findNode(v), findNode(u), w);
	}
	
	public boolean removeEdge(int i, int j){
		if (i != j && containsEdge(i, j)){
			if (history != null)
				history.push(new WeightedRemoval(i, j, w(i, j)));
			performRemoveEdge(i, j);
			notifyListenersRemove(new Edge(i, j, isDirected()));
			return true;
		} else
			return false;
	}
	
	public boolean removeEdge(Edge e){
		return removeEdge(e.i(), e.j());
	}
	
	public boolean removeLWEdge(V v, V u){
		return removeEdge(findNode(v), findNode(u));
	}
	
	protected void performAddEdge(int i, int j, E w) {
		super.performAddEdge(i, j);
		if (weights != null)
			performSetWeight(i, j, w);
	}
	
	@Override
	protected void performRemoveEdge(int i, int j) {
		super.performRemoveEdge(i, j);
		weights.get(i).remove(j);
		if (!isDirected())
			weights.get(j).remove(i);
	}
	
	public boolean containsLWEdge(V v, V u){
		return containsEdge(findNode(v), findNode(u));
	}
	
	public void swapLabels(int i, int j) {
		Collections.swap(labels, i, j);
		indices.get(l(i)).remove(j).add(i);
		indices.get(l(j)).remove(i).add(j); 
	}
	
	public E w(int i, int j) {
		return weights.get(i).get(j);
	}
	
	public E w(Edge e) {
		return w(e.i(), e.j());
	}
	
	public E w(V v, V u) {
		return weights.get(findNode(v)).get(findNode(u));
	}
	
	public void setWeight(int i, int j, E w) {
		if (history != null)
			history.push(new SettingWeight(i, j, w(i,j)));
		performSetWeight(i, j, w);
		notifyListenersOther(e(i,j));
	}
	
	public void setWeight(Edge e, E w) {
		setWeight(e.i(), e.j(), w);
	}
	
	public void setWeight(V v, V u, E w) {
		setWeight(findNode(v), findNode(u), w);
	}
	
	protected void performSetWeight(int i, int j, E w) {
		weights.get(i).put(j, w);
		if (!isDirected())
			weights.get(j).put(i, w);
	}
	
	public Stream<V> getSuccsLabels(V v){
		return getSuccs(findNode(v)).stream().mapToObj(j -> l(j));
	}
	
	public Stream<V> getPredsLabels(V v){
		return getPreds(findNode(v)).stream().mapToObj(j -> l(j));
	}
	
	public Stream<V> getNeighsLabels(V v){
		return getNeighs(findNode(v)).stream().mapToObj(j -> l(j));
	}
	
	@Override
	public void startRecordingHistory(){
		history = new Stack<>();
	}
	
	@Override
	public void stopRecordingHistory(){
		history = null;
	}

	public boolean isRecordingHistory(){
		return history != null;
	}

	public Edge getLastChange() {
		return history.peek().getEdge();
	}

	public Stream<Edge> getChanges() {
		return history.stream().map(c -> c.getEdge());
	}
	
	@Override
	public void resetGraph(){
		if (history != null){
			while (!history.empty())
				history.pop().revert();
			notifyListenersReset();
		}
	}
	
	@Override
	public void revertChanges(int k){
		if (history != null){
			for (int i = 0; i < k; ++i)
				if (!history.empty())
					history.pop().revert();
		}
	}
	
	@Override
	public int historySize(){
		return history == null ? 0 : history.size();
	}
	
	private abstract class WeightedChange {
		
		protected int i;
		protected int j;
		protected E prevW;
		
		public WeightedChange(int i, int j, E prevW) {
			this.i = i;
			this.j = j;
			this.prevW = prevW;
		}

		public Edge getEdge() {
			return LWGraph.this.e(i, j);
		}
		
		public abstract void revert();
	}
	
	private class WeightedAddition extends WeightedChange {
		
		public WeightedAddition(int i, int j) {
			super(i, j, null);
		}
		
		@Override
		public void revert() {
			performRemoveEdge(i, j);
			notifyListenersRemove(new Edge(i, j, isDirected()));
		}
	}
	
	private class WeightedRemoval extends WeightedChange {
		
		public WeightedRemoval(int i, int j, E prevW) {
			super(i, j, prevW);
		}
		
		@Override
		public void revert() {
			performAddEdge(i, j, prevW);
			notifyListenersAdd(new Edge(i, j, isDirected()));
		}
	}
	
	private class SettingWeight extends WeightedChange {
		
		public SettingWeight(int i, int j, E prevW) {
			super(i, j, prevW);
		}
		
		@Override
		public void revert() {
			performSetWeight(i, j, prevW);
		}
	}
}
