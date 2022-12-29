package anansi.core.search;

import java.util.Deque;

/**
 * Algorithm visiting nodes of a graph in a BFS order.
 * 
 * @author Marcin Waniek
 */
public abstract class BreadthFirstSearch extends SearchAlgorithm {

	@Override
	protected Integer poll(Deque<Integer> q) {
		return q.pollFirst();
	}
}
