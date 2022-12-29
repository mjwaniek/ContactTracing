package anansi.core;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import anansi.utils.Utils;

/**
 * Class keeping track of shortest paths between pairs of nodes using Dijkstra's algorithm.
 * 
 * @author Marcin Waniek
 */
public class ShortestPaths implements GraphChangeListener {

	public static final int LOW_MEMORY_THRESHOLD = 20000;
	private static final double DELTA = .000001;

	protected Graph g;
	protected double[][] spLength;
	protected int[][] spNumber;
	protected Integer[][] spStep;
	protected int lowMemorySpRoot;
	protected double[] lowMemorySpLength;
	protected int[] lowMemorySpNumber;
	protected Integer[] lowMemorySpStep;
	
	protected ShortestPaths(Graph g) {
		this.g = g;
		reactNotify();
	}
	
	public static ShortestPaths construct(Graph g){
		ShortestPaths res = new ShortestPaths(g);
		g.subscribe(res);
		return res;
	}
	
	public double getDistance(int i, int j){
		if (g.size() > LOW_MEMORY_THRESHOLD)
			return getLowMemoryDistance(i, j);
		if (spLength == null)
			recountDistances();
		return spLength[i][j];
	}

	public double getLowMemoryDistance(int i, int j){
		if (lowMemorySpRoot != i)
			recountLowMemoryDistances(i);
		return lowMemorySpLength[j];
	}
	
	public int getNumberOfShortestPaths(int i, int j){
		if (g.size() > LOW_MEMORY_THRESHOLD)
			return getLowMemoryNumberOfShortestPaths(i, j);
		if (spLength == null)
			recountDistances();
		return spNumber[i][j];
	}

	public int getLowMemoryNumberOfShortestPaths(int i, int j){
		if (lowMemorySpRoot != i)
			recountLowMemoryDistances(i);
		return lowMemorySpNumber[j];
	}
	
	public Integer getStep(int i, int j){
		if (g.size() > LOW_MEMORY_THRESHOLD)
			return getLowMemoryStep(i, j);
		if (spLength == null)
			recountDistances();
		return spStep[i][j];
	}
	
	public Integer getLowMemoryStep(int i, int j){
		if (lowMemorySpRoot != i)
			recountLowMemoryDistances(i);
		return lowMemorySpStep[j];
	}
	
	public Path getShortestPath(int from, int to){
		if (g.size() > LOW_MEMORY_THRESHOLD)
			return getLowMemoryShortestPath(from, to);
		if (spLength == null)
			recountDistances();
		Path p = new Path(from);
		while (p.getLast() != to)
			p.add(spStep[p.getLast()][to]);
		return p;
	}
	
	public Path getLowMemoryShortestPath(int from, int to){
		if (lowMemorySpRoot != from)
			recountLowMemoryDistances(from);
		Path p = new Path(to);
		while (p.get(0) != from)
			p.addFirst(Utils.argmin(g.getPreds(p.get(0)), i -> lowMemorySpLength[i] + edgeLength(i, p.get(0))));
		return p;
	}
	
	public List<Path> getAllShortestPaths(int from, int to){
		if (g.size() > LOW_MEMORY_THRESHOLD)
			return getLowMemoryShortestPaths(from, to);
		if (spLength == null)
			recountDistances();
		return findAllShortestPaths(from, to, spLength[from]);
	}
	
	public List<Path> getLowMemoryShortestPaths(int from, int to){
		if (lowMemorySpRoot != from)
			recountLowMemoryDistances(from);
		return findAllShortestPaths(from, to, lowMemorySpLength);
	}
	
	private void recountDistances(){
		spLength = new double[g.size()][g.size()];
		spNumber = new int[g.size()][g.size()];
		spStep = new Integer[g.size()][g.size()];
		for (int i = 0; i < spLength.length; ++i)
			for (int j = 0; j < spLength[i].length; ++j)
				spLength[i][j] = Double.POSITIVE_INFINITY;
		for (int i : g.nodes())
			findDistancesFrom(i, spLength[i], spNumber[i], spStep[i]);
	}

	private void recountLowMemoryDistances(int i){
		lowMemorySpRoot = i;
		lowMemorySpLength = new double[g.size()];
		lowMemorySpNumber = new int[g.size()];
		lowMemorySpStep = new Integer[g.size()];
		for (int k = 0; k < lowMemorySpLength.length; ++k)
			lowMemorySpLength[k] = Double.POSITIVE_INFINITY;
		findDistancesFrom(i, lowMemorySpLength, lowMemorySpNumber, lowMemorySpStep);
	}
	
	private void findDistancesFrom(int from, double[] dist, int[] paths, Integer step[]){
		dist[from] = 0;
		paths[from] = 1;
		PriorityQueue<Integer> q = new PriorityQueue<>(1, (o1,o2) -> Double.compare(dist[o1], dist[o2]));
		q.add(from);
		while (!q.isEmpty()) {
			int i = q.poll();
			for (int j : g.getSuccs(i))
				if (dist[i] + edgeLength(i, j) < dist[j]){
					dist[j] = dist[i] + edgeLength(i, j);
					q.remove(j);
					q.add(j);
					paths[j] = paths[i];
					step[j] = i == from ? j : step[i];
				} else if (dist[i] + edgeLength(i, j) == dist[j])
					paths[j] += paths[i];
		}
	}
	
	private List<Path> findAllShortestPaths(int from, int to, double[] dist){
		List<List<Path>> shortestPaths = new ArrayList<>();
		for (int i = 0; i < g.size(); ++i)
			shortestPaths.add(new ArrayList<Path>());
		shortestPaths.get(to).add(new Path(to));
		Double[] distToTo = new Double[g.size()];
		final double[] fdist = dist;
		distToTo[to] = 0.;
		g.nodesStream().boxed().sorted((i,j) -> Double.compare(fdist[j], fdist[i])).filter(i -> i != to).forEach(i ->{
			for (int j : g.getSuccs(i))
				if (distToTo[j] != null && (distToTo[i] == null || edgeLength(i, j) + distToTo[j] < distToTo[i]))
						distToTo[i] = edgeLength(i, j) + distToTo[j];
			for (int j : g.getSuccs(i))
				if (distToTo[j] != null && Math.abs(fdist[i] + edgeLength(i, j) + distToTo[j] - fdist[to]) < DELTA)
					for (Path p : shortestPaths.get(j))
						shortestPaths.get(i).add(p.getPrefixed(i));
		});
		return shortestPaths.get(from);
	}
	
	protected double edgeLength(int i, int j) {
		return 1.;
	}

	@Override
	public void notifyAdd(Graph g, Edge e) {
		reactNotify();
	}

	@Override
	public void notifyRemove(Graph g, Edge e) {
		reactNotify();
	}

	@Override
	public void notifyReset(Graph g) {
		reactNotify();
	}
	
	protected void reactNotify(){
		this.spLength = null;
		this.spNumber = null;
		this.spStep = null;
		this.lowMemorySpRoot = -1;
		this.lowMemorySpLength = null;
		this.lowMemorySpNumber = null;
		this.lowMemorySpStep = null;
		this.reversedLowMemorySpRoot = -1;
		this.reversedLowMemorySpLength = null;
		this.reversedLowMemorySpNumber = null;
	}
	
	// Methods for finding shortest paths *leading to* a given node in a linear memory
	
	protected int reversedLowMemorySpRoot;
	protected double[] reversedLowMemorySpLength;
	protected int[] reversedLowMemorySpNumber;
	
	public double getReversedLowMemoryDistance(int from, int to){
		if (reversedLowMemorySpRoot != to)
			recountReversedLowMemoryDistances(to);
		return reversedLowMemorySpLength[from];
	}

	public int getReversedLowMemoryNumberOfShortestPaths(int from, int to){
		if (reversedLowMemorySpRoot != to)
			recountReversedLowMemoryDistances(to);
		return reversedLowMemorySpNumber[from];
	}
	
	public Path getReversedLowMemoryShortestPath(int from, int to){
		if (reversedLowMemorySpRoot != to)
			recountReversedLowMemoryDistances(to);
		Path p = new Path(from);
		while (p.getLast() != to)
			p.add(Utils.argmin(g.getSuccs(p.getLast()), i -> reversedLowMemorySpLength[i] + edgeLength(p.getLast(), i)));
		return p;
	}
	
	private void recountReversedLowMemoryDistances(int to){
		reversedLowMemorySpRoot = to;
		reversedLowMemorySpLength = new double[g.size()];
		reversedLowMemorySpNumber = new int[g.size()];
		for (int k = 0; k < reversedLowMemorySpLength.length; ++k)
			reversedLowMemorySpLength[k] = Double.POSITIVE_INFINITY;
		findDistancesTo(to, reversedLowMemorySpLength, reversedLowMemorySpNumber);
	}
	
	private void findDistancesTo(int to, double[] dist, int[] paths){
		dist[to] = 0;
		paths[to] = 1;
		PriorityQueue<Integer> q = new PriorityQueue<>(1, (o1,o2) -> Double.compare(dist[o1], dist[o2]));
		q.add(to);
		while (!q.isEmpty()) {
			int i = q.poll();
			for (int j : g.getPreds(i))
				if (dist[i] + edgeLength(j, i) < dist[j]){
					dist[j] = dist[i] + edgeLength(j, i);
					q.remove(j);
					q.add(j);
					paths[j] = paths[i];
				} else if (dist[i] + edgeLength(j, i) == dist[j])
					paths[j] += paths[i];
		}
	}
}
