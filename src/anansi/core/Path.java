package anansi.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

/**
 * Path between nodes of a graph.
 * 
 * @author Marcin Waniek
 */
public class Path implements Iterable<Integer> {
	
	private List<Integer> nodes;
	
	public Path(){
		this.nodes = new ArrayList<>();
	}
	
	public Path(Path p){
		this(p.nodes);
	}
	
	public Path(int... nodes){
		this.nodes = new ArrayList<>();
		for (int i : nodes)
			add(i);
	}
	
	public Path(List<Integer> nodes){
		this();
		nodes.forEach(i -> this.nodes.add(i));
	}
	
	public Path(Stream<Integer> nodes){
		this();
		nodes.forEach(i -> this.nodes.add(i));
	}
	
	public Integer get(int index){
		return nodes.get(index);
	}
	
	public Integer getFromEnd(int index){
		return get(size() - 1 - index);
	}
	
	public Integer getFirst(){
		return get(0);
	}
	
	public Integer getLast(){
		return getFromEnd(0);
	}
	
	public int size(){
		return nodes.size();
	}
	
	public boolean isEmpty() {
		return nodes.isEmpty();
	}
	
	public boolean contains(int i) {
		return nodes.contains(i);
	}
	
	public boolean contains(Graph g, Edge e) {
		return edgeStream(g).filter(pe -> pe.equals(e)).findAny().isPresent();
	}
	
	public List<Integer> asList(){
		return nodes;
	}
	
	public void add(int i){
		nodes.add(i);
	}
	
	public void addFirst(int i){
		nodes.add(0, i);
	}
	
	public Path add(Path p){
		for (int i : p)
			add(i);
		return this;
	}
	
	public void addDropFirst(Path p){
		p.stream().skip(1).forEach(i -> add(i));
	}
	
	public boolean removeLast(){
		if (size() > 0) {
			nodes.remove(size() - 1);
			return true;
		} else
			return false;
	}
	
	public Path getExtended(int i){
		Path res = new Path(this.nodes);
		res.add(i);
		return res;
	}
	
	public Path getPrefixed(int i){
		Path res = new Path(i);
		nodes.forEach(j -> res.add(j));
		return res;
	}
	
	public Path getReversed(){
		Path res = new Path();
		for (int i = size() - 1; i >= 0; --i)
			res.add(get(i));
		return res;
	}
	
	public Stream<Integer> stream(){
		return nodes.stream();
	}
	
	public Stream<Edge> edgeStream(Graph g){
		int[] prev = new int[1];
		prev[0] = nodes.get(0);
		return nodes.stream().skip(1).map(i -> {
			Edge e = g.e(prev[0], i);
			prev[0] = i;
			return e;
		});
	}
	
	@Override
	public Iterator<Integer> iterator() {
		return nodes.iterator();
	}
	
	@Override
		public int hashCode() {
			return nodes.hashCode();
		}
	
	@Override
	public String toString() {
		String res = "[ ";
		for (Integer i : nodes)
			res += i + " ";
		res += "]";
		return res;
	}
	
	public static PathCollector getCollector(){
		return new PathCollector();
	}
	
	private static class PathCollector implements Collector<Integer, Path, Path> {

		private static Set<Characteristics> CHARS;
		
		@Override
		public BiConsumer<Path, Integer> accumulator() {
			return (p,i) -> p.add(i);
		}

		@Override
		public Set<Characteristics> characteristics() {
			if (CHARS == null){
				CHARS =  new HashSet<>();
				CHARS.add(Characteristics.IDENTITY_FINISH);
			}
			return CHARS;
		}

		@Override
		public BinaryOperator<Path> combiner() {
			return (c1,c2) -> c1.add(c2);
		}

		@Override
		public Function<Path, Path> finisher() {
			return p -> p;
		}

		@Override
		public Supplier<Path> supplier() {
			return () -> new Path();
		}
	}
}
