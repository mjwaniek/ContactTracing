package anansi.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import anansi.utils.Utils;

/**
 * Representation of a coalition - a group of nodes.
 * 
 * @author Marcin Waniek
 */
public class Coalition implements Iterable<Integer> {
		
	private HashSet<Integer> nodes;
	
	public Coalition() {
		this.nodes = new HashSet<>();
	}
	
	public Coalition(Collection<Integer> col) {
		this.nodes = new HashSet<>(col);
	}
	
	public Coalition(Integer[] arr) {
		this();
		for (int i : arr)
			this.add(i);
	}

	public Coalition(Coalition c) {
		this.nodes = new HashSet<>(c.nodes);
	}
	
	public Coalition(int... nodes) {
		this();
		for (int i : nodes)
			this.add(i);
	}
	
	public static Coalition getFull(int n){
		Coalition res = new Coalition();
		for (int i = 0; i < n; ++i)
			res.add(i);
		return res;
	}

	public int size(){
		return nodes.size();
	}

	public boolean isEmpty(){
		return nodes.isEmpty();
	}

	public HashSet<Integer> getNodes() {
		return nodes;
	}

	public Integer[] asArray(){
		Integer[] res = new Integer[nodes.size()];
		int i = 0;
		for (Integer v : nodes)
			res[i++] = v;
		return res;
	}
	
	public List<Integer> asList(){
		List<Integer> res = new ArrayList<>();
		for (Integer v : nodes)
			res.add(v);
		return res;
	}
	
	public IntStream stream(){
		return nodes.stream().mapToInt(v -> v.intValue());
	}

	@Override
	public Iterator<Integer> iterator() {
		return nodes.iterator();
	}
	
	public Integer getAny(){
		return isEmpty() ? null : nodes.iterator().next();
	}
	
	public Integer getRandom(){
		return isEmpty() ? null : stream().skip(Utils.RAND.nextInt(size())).findFirst().getAsInt();
	}
	
	public Coalition getRandom(int k){
		Coalition res = new Coalition();
		for (int t = 0; t < Math.min(k, size()); ++t)
			res.add(stream().filter(i -> !res.contains(i)).skip(Utils.RAND.nextInt(size() - res.size()))
					.findFirst().getAsInt());
		return res;
	}
	
	public boolean contains(int i){
		return nodes.contains(i);
	}
	
	// Operations that perform changes in the Coalition or create new one

	public Coalition clear() {
		nodes.clear();
		return this;
	}
	
	public Coalition add(int i){
		nodes.add(i);
		return this;
	}

	public Coalition add(Coalition c){
		nodes.addAll(c.nodes);
		return this;
	}
	
	public Coalition addAll(Collection<Integer> ii){
		nodes.addAll(ii);
		return this;
	}

	public static Coalition add(Coalition... cs){
		Coalition res = new Coalition(cs[0]);
		for (int i = 1; i < cs.length; ++i)
			res.add(cs[i]);
		return res;
	}

	public static Coalition add(Coalition c1, int i){
		Coalition res = new Coalition(c1);
		res.add(i);
		return res;
	}

	public Coalition remove(int i){
		nodes.remove(i);
		return this;
	}

	public Coalition remove(Coalition c){
		nodes.removeAll(c.nodes);
		return this;
	}
	
	public Coalition removeAll(Collection<Integer> ii){
		nodes.removeAll(ii);
		return this;
	}

	public static Coalition diff(Coalition c1, Coalition c2){
		return new Coalition(c1.inplaceDiff(c2).boxed().collect(Collectors.toSet()));
	}

	public static Coalition diff(Coalition c1, int i){
		Coalition res = new Coalition(c1);
		res.remove(i);
		return res;
	}

	public int removeAny() {
		int i = getAny();
		remove(i);
		return i;
	}
	
	public int removeRandom() {
		int i = getRandom();
		remove(i);
		return i;
	}
	
	public Coalition keepOnly(Coalition c){
		return filter(i -> c.contains(i));
	}
	
	public Coalition filter(Predicate<Integer> p) {
		for (Iterator<Integer> iter = nodes.iterator(); iter.hasNext();){
			int i = iter.next();
			if (!p.test(i))
				iter.remove();
		}
		return this;
	}
	
	public static Coalition intersect(Coalition c1, Coalition c2){
		return new Coalition(c1.inplaceIntersect(c2).boxed().collect(Collectors.toSet()));
	}
	
	
	// Inplace operations that do not change existing Coalition, nor create new one
	
	public IntStream inplaceAdd(Coalition c){
		return Coalition.inplaceAdd(this, c);
	}

	public IntStream inplaceDiff(Coalition c){
		return Coalition.inplaceDiff(this, c);
	}
	
	public IntStream inplaceIntersect(Coalition c){
		return Coalition.inplaceIntersect(this, c);
	}
	
	public static IntStream inplaceAdd(Coalition c1, Coalition c2){
		Coalition small = c1.size() < c2.size() ? c1 : c2;
		Coalition big = c1.size() < c2.size() ? c2 : c1;
		return IntStream.concat(big.stream(), small.stream().filter(v -> !big.contains(v)));
	}

	public static IntStream inplaceDiff(Coalition c1, Coalition c2){
		return c1.stream().filter(v -> !c2.contains(v));
	}
	
	public static IntStream inplaceIntersect(Coalition c1, Coalition c2){
		Coalition small = c1.size() < c2.size() ? c1 : c2;
		Coalition big = c1.size() < c2.size() ? c2 : c1;
		return small.stream().filter(v -> big.contains(v));
	}
	
	public static Coalition getRandom(int k, int n){
		Coalition res = new Coalition();
		for (int t = 0; t < k; ++t)
			res.add(IntStream.range(0, n).filter(i -> !res.contains(i)).skip(Utils.RAND.nextInt(n - t))
					.findFirst().getAsInt());
		return res;
	}
	
	@Override
	public boolean equals(Object o){
		if(o == null)
			return false;
		if(!(o instanceof Coalition))
			return false;
		Coalition c = (Coalition) o;
		return nodes.equals(c.nodes);
	}

	@Override
	public String toString(){
		return nodes.toString(); 
	}
	
	@Override
	public int hashCode() {
		return nodes.hashCode();
	}
	
	public static CoalitionCollector getCollector(){
		return new CoalitionCollector();
	}
	
	private static class CoalitionCollector implements Collector<Integer, Coalition, Coalition> {

		private static Set<Characteristics> CHARS;
		
		@Override
		public BiConsumer<Coalition, Integer> accumulator() {
			return (c,i) -> c.add(i);
		}

		@Override
		public Set<Characteristics> characteristics() {
			if (CHARS == null){
				CHARS =  new HashSet<>();
				CHARS.add(Characteristics.IDENTITY_FINISH);
				CHARS.add(Characteristics.UNORDERED);
			}
			return CHARS;
		}

		@Override
		public BinaryOperator<Coalition> combiner() {
			return (c1,c2) -> c1.add(c2);
		}

		@Override
		public Function<Coalition, Coalition> finisher() {
			return c -> c;
		}

		@Override
		public Supplier<Coalition> supplier() {
			return () -> new Coalition();
		}
	}
}
