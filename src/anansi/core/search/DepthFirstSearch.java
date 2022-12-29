package anansi.core.search;

import java.util.Deque;

/**
 * Algorithm visiting nodes of a graph in a DFS order.
 * 
 * @author Marcin Waniek
 */
public abstract class DepthFirstSearch extends SearchAlgorithm{

	@Override
	protected Integer poll(Deque<Integer> q) {
		return q.pollLast();
	}
}
