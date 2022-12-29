package anansi.core.search;

import java.util.Deque;
import java.util.LinkedList;

import anansi.core.Graph;

/**
 * Representation of an algorithm visiting nodes of a graph in a given order.
 * 
 * @author Marcin Waniek
 */
public abstract class SearchAlgorithm {
	
	protected boolean[] visited;
	protected Integer[] parent;
	
	protected abstract Integer poll(Deque<Integer> q);
	
	public abstract void process(int v);
	public void preProcessRoot(Integer root) {}
	public void postProcessRoot(Integer root) {}
	
	public void runSearch(Graph g, int start){
		visited = new boolean[g.size()];
		parent = new Integer[g.size()];
		preProcessRoot(start);
		Deque<Integer> q = new LinkedList<>();
		q.add(start);
		while(!q.isEmpty()){
			int i = poll(q);
			visited[i] = true;
			process(i);
			for (int j : g.getSuccs(i))
				if (!visited[j] && !q.contains(j)) {
					q.add(j);
					parent[j] = i;
				}
		}
		postProcessRoot(start);
	}
}
