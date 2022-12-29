package anansi.core;

import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.IntStream;

/**
 * Representation of an edge.
 * 
 * @author Marcin Waniek
 */
public class Edge implements Iterable<Integer>, Comparable<Edge> {

	private int first;
	private int second;
	private boolean directed;

	public Edge(int first, int second, boolean directed) {
		this.first = first;
		this.second = second;
		this.directed = directed;
	}

	public Integer i() {
		return first;
	}

	public Integer j() {
		return second;
	}
	
	public Integer getOther(int i){
		if (i == first)
			return second;
		else if (i == second)
			return first;
		else
			return null;
	}

	public boolean isDirected() {
		return directed;
	}
	
	public boolean contains(int i) {
		return first == i || second == i;
	}
	
	public boolean incident(Edge e) {
		return first == e.i() || first == e.j() || second == e.i() || second == e.j();
	}
	
	public Edge getReversed() {
		return new Edge(second, first, directed);
	}

	@Override
	public String toString() {
		if (directed)
			return "<" + first + "," + second + ">";
		else
			return "(" + Math.min(first, second) + "," + Math.max(first, second) + ")";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || !(o instanceof Edge))
			return false;
		Edge e = (Edge)o;
		return directed == e.directed && ((first == e.first && second == e.second)
				|| (first == e.second && second == e.first && !directed));
	}

	@Override
	public int hashCode() {
		int a = directed ? first : Math.min(first, second);
		int b = directed ? second : Math.max(first, second);
		return 31 * (31 * (31 + (directed ? 1231 : 1237)) + a) + b;
	}

	@Override
	public Iterator<Integer> iterator() {
		return Arrays.asList(new Integer[]{first, second}).iterator();
	}

	public IntStream stream(){
		return IntStream.of(first, second);
	}
	
	@Override
	public int compareTo(Edge e) {
		int a = directed ? first : Math.min(first, second);
		int b = directed ? second : Math.max(first, second);
		int ea = e.directed ? e.first : Math.min(e.first, e.second);
		int eb = e.directed ? e.second : Math.max(e.first, e.second);
		int res = Integer.compare(a, ea);
		if (res != 0)
			return res;
		res = Integer.compare(b, eb);
		if (res != 0)
			return res;
		return Boolean.compare(directed, e.directed);
	}
}
