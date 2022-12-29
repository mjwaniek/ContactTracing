package anansi.core.search;

import anansi.core.Graph;

/**
 * Algorithm visiting nodes of a graph in a DFS order, backtracking to visited
 * through new edges and storing the structure of a DFS tree.
 * 
 * @author Marcin Waniek
 */
public abstract class RecursiveDepthFirstSearch {

	protected Graph g;
	protected boolean[] visited;
	protected boolean[] processed;
	protected Integer[] parent;
	
	public RecursiveDepthFirstSearch(Graph g) {
		this.g = g;
		this.visited = new boolean[g.size()];
		this.processed = new boolean[g.size()];
		this.parent = new Integer[g.size()];
	}
	
	public boolean getVisited(int v) {
		return visited[v];
	}
	
	public Integer getParent(int v) {
		return parent[v];
	}

	public void runSearch(int start){
		preProcessRoot(start);
		rec(start);
		postProcessRoot(start);
	}
	
	private void rec(int v) {
		visited[v] = true;
		processNode(v);
		for (int w : g.getSuccs(v)) {
			if (!visited[w]) {
				parent[w] = v;
				processTreeEdge(v, w);
				rec(w);
				postProcessTreeEdge(v, w);
			} else
				touchVisited(v, w);
		}
		processed[v] = true;
		postProcessNode(v);
	}

	public void preProcessRoot(Integer root) {}
	
	public void postProcessRoot(Integer root) {}
	
	public void processNode(Integer v) {}
	
	public void postProcessNode(Integer v) {}

	public void processTreeEdge(Integer from, Integer to) {}

	public void postProcessTreeEdge(Integer from, Integer to) {}
	
	public void touchVisited(Integer from, Integer to) {}
}
