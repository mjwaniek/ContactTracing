package anansi.core.search;

import java.util.Iterator;
import java.util.Stack;

import anansi.core.Graph;

/**
 * Algorithm visiting nodes of a graph in a DFS order, without recursive calls.
 * 
 * @author Marcin Waniek
 */
public class NonRecursiveDepthFirstSearch {

	protected Graph g;
	protected boolean[] visited;
	protected boolean[] processed;
	protected Integer[] parent;
	private Stack<Record> stack;
	
	public NonRecursiveDepthFirstSearch(Graph g) {
		this.g = g;
		this.visited = new boolean[g.size()];
		this.processed = new boolean[g.size()];
		this.parent = new Integer[g.size()];
		this.stack = new Stack<>();
	}
	
	public void runSearch() {
		for (int v : g.nodes())
			if (!visited[v])
				runSearch(v);
	}
	
	public void runSearch(int start){
		preProcessRoot(start);
		stack.push(new Record(start));
		while (!stack.isEmpty()) {
			Record rec = stack.peek();
			if (!visited[rec.v]) {
				visited[rec.v] = true;
				processNode(rec.v);
			}
			if (rec.iter.hasNext()) {
				int w = rec.iter.next();
				if (!visited[w]) {
					parent[w] = rec.v;
					processTreeEdge(rec.v, w);
					stack.push(new Record(w));
				} else
					touchVisited(rec.v, w);
			} else {
				processed[rec.v] = true;
				postProcessNode(rec.v);
				stack.pop();
				postProcessTreeEdge(parent[rec.v], rec.v);
			}
		}
		postProcessRoot(start);
	}
	
	protected Iterator<Integer> getSuccessors(int v){
		return g.getSuccs(v).iterator();
	}
	
	public void preProcessRoot(Integer root) {}
	public void postProcessRoot(Integer root) {}
	public void processNode(Integer v) {}
	public void postProcessNode(Integer v) {}
	public void processTreeEdge(Integer from, Integer to) {}
	public void postProcessTreeEdge(Integer from, Integer to) {}
	public void touchVisited(Integer from, Integer to) {}
	
	private class Record {
		private int v;
		private Iterator<Integer> iter;
		
		public Record(int v) {
			this.v = v;
			this.iter = getSuccessors(v);
		}
	}
}
