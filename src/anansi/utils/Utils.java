package anansi.utils;

import java.awt.Color;
import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.math3.util.CombinatoricsUtils;

/**
 * A set of useful methods
 * 
 * @author Marcin Waniek
 */
public class Utils {

	public static final Random RAND = new Random();
	public static final String WHITESPACE = "\\s+";
	
	/**
	 * Current time in the yyMMdd-HHmmss format.
	 */
	public static String timestamp(){
		return new SimpleDateFormat("yyMMdd-HHmmss-SSS").format(new Date());
	}
	
	public static String capitalize(String s) {
		return s.substring(0,1).toUpperCase() + s.substring(1);
	}
	
	public static String hex(Color c) {
		return "#" + Integer.toHexString(c.getRGB()).substring(2).toUpperCase();
	}
	
	/**
	 * Readable format of milliseconds.
	 */
	public static String timeDesc(long ms){
		if (ms < 1000)
			return ms + "ms";
		long s = ms / 1000;
		long m = s / 60;
		long h = m / 60;
		String res = s + "s";
		if (m > 0)
			res = m + "m " + (s % 60) + "s";
		if (h > 0)
			res = h + "h " + (m % 60) + "m " + (s % 60) + "s";
		return res;
	}
	
	public static <T> T getRandom(List<T> l) {
		return l.size() > 0 ? l.get(RAND.nextInt(l.size())) : null;
	}
	
	public static <T> T getRandom(Stream<T> s) {
		return getRandom(s.collect(Collectors.toList()));
	}
	
	public static <T> T getRandom(Stream<T> s, int size) {
		return s.skip(RAND.nextInt(size)).findFirst().orElse(null);
	}
	
	/**
	 * Finds the element of collection that maximizes given function.
	 */
	public static <T> T argmax(Iterable<T> it, Function<T,Number> f){
		ArgMaxCounter<T> counter = new ArgMaxCounter<>();
		it.forEach(t -> counter.update(t, f.apply(t).doubleValue()));
		return counter.res;
	}
	
	/**
	 * Finds the element of stream that maximizes given function.
	 */
	public static <T> T argmax(Stream<T> s, Function<T,Number> f){
		ArgMaxCounter<T> counter = new ArgMaxCounter<>();
		s.forEach(t -> counter.update(t, f.apply(t).doubleValue()));
		return counter.res;
	}
	
	private static class ArgMaxCounter<T> {
		private T res;
		private double resVal;
		private int equalCount;
		
		public ArgMaxCounter() {
			this.res = null;
			this.resVal = 0;
			this.equalCount = 0;
		}
		
		public void update(T elem, double val) {
			if (res == null || val > resVal){
				equalCount = 0;
				res = elem;
				resVal = val;
			} else if (val == resVal){
				++equalCount;
				if (RAND.nextDouble() >= (double)equalCount/(equalCount + 1)){
					res = elem;
					resVal = val;
				}
			}
		}
	}
	
	/**
	 * Finds the element of collection that maximizes given function.
	 */
	public static <T> T argmin(Iterable<T> it, final Function<T,Number> f){
		return argmax(it, t -> -f.apply(t).doubleValue());
	}
	
	/**
	 * Finds the element of stream that maximizes given function.
	 */
	public static <T> T argmin(Stream<T> s, final Function<T,Number> f){
		return argmax(s, t -> -f.apply(t).doubleValue());
	}
	
	public static <T> T last(T[] a) {
		return a[a.length - 1];
	}
	
	public static <T> T last(List<T> l) {
		return l.get(l.size() - 1);
	}

	/**
	 * Concatenate two arrays.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] concat(T[] a, T[] b){
		T[] res = (T[]) Array.newInstance(a.getClass().getComponentType(), a.length + b.length);
		int i = 0;
		for (T t : a)
			res[i++] = t;
		for (T t : b)
			res[i++] = t;
		return res;
	}

	@SafeVarargs
	public static <T> List<T> concat(List<T>... lists){
		List<T> res = new ArrayList<>();
		for (List<T> l : lists)
			res.addAll(l);
		return res;
	}
	
	@SafeVarargs
	public static <T> List<T> append(List<T> l, T... elems){
		List<T> res = new ArrayList<>();
		res.addAll(l);
		for (T t : elems)
			res.add(t);
		return res;
	}
	
	@SafeVarargs
	public static <T> List<T> remove(List<T> l, T... elems){
		List<T> res = new ArrayList<>();
		res.addAll(l);
		for (T t : elems)
			res.remove(t);
		return res;
	}
	
	/**
	 * Filter the map based on keys.
	 */
	public static <K,V> void filter(Map<K,V> m, Predicate<K> f) {
		m.keySet().stream().filter(f.negate()).collect(Collectors.toList()).forEach(k -> m.remove(k));
	}
	
	/**
	 * String representation of an array.
	 */
	public static <T> String toString(T[] a){
		String res = "[ ";
		for (T t : a)
			res += t + " ";
		res += "]";
		return res;
	}

	/**
	 * String representation of a int array.
	 */
	public static String toString(int[] a){
		String res = "[ ";
		for (int t : a)
			res += t + " ";
		res += "]";
		return res;
	}

	/**
	 * String representation of a double array.
	 */
	public static String toString(double[] a){
		String res = "[ ";
		for (double t : a)
			res += t + " ";
		res += "]";
		return res;
	}
	
	/**
	 * Euclidean distance between two points.
	 */
	public static double distance(double x1, double y1, double x2, double y2) {
		return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
	}
	
	/**
	 * Vector multiplication.
	 */
	public static double mult(double[] a, double[] b) {
		double res = 0.;
		for (int i = 0; i < Math.min(a.length, b.length); ++i)
			res += a[i] * b[i];
		return res;
	}
	
	/**
	 * Saves iterable elements to a list.
	 */
	public static <T> List<T> toList(Iterable<T> it){
		List<T> res = new ArrayList<>();
		for (T t : it)
			res.add(t);
		return res;
	}
	
	/**
	 * Creates list in place.
	 */
	@SafeVarargs
	public static <T> List<T> aList(T... elems){
		List<T> res = new ArrayList<>();
		for (T elem : elems)
			res.add(elem);
		return res;
	}
	
	public static <T> List<T> asList(Stream<T> elems){
		List<T> res = new ArrayList<>();
		elems.forEach(elem -> res.add(elem));
		return res;
	}
	
	public static <T> List<T> asList(T[] elems){
		List<T> res = new ArrayList<>();
		for (T elem : elems)
			res.add(elem);
		return res;
	}
	
	public static List<Integer> asList(int[] elems){
		List<Integer> res = new ArrayList<>();
		for (int elem : elems)
			res.add(elem);
		return res;
	}
	
	public static List<Double> asList(double[] elems){
		List<Double> res = new ArrayList<>();
		for (double elem : elems)
			res.add(elem);
		return res;
	}
	
	public static List<Boolean> asList(boolean[] elems){
		List<Boolean> res = new ArrayList<>();
		for (boolean elem : elems)
			res.add(elem);
		return res;
	}
	
	/**
	 * Returns file extension.
	 */
	public static String getFileExtension(File f){
		return f.getName().substring(f.getName().lastIndexOf(".") + 1);
	}
	
	/**
	 * Checks whether the application is running on Windows OS.
	 */
	public static boolean runningOnWindows() {
		String osName = System.getProperty("os.name"); 
		return "windows".equals(osName.substring(0, Math.min(7, osName.length())).toLowerCase());
	}
	
	/**
	 * Reverse a list.
	 */
	public static <T> List<T> reverse(List<T> l){
		List<T> res = new ArrayList<>(l);
		Collections.reverse(res);
		return res;
	}
	
	/**
	 * Generates all sublist of given size.
	 */
	public static <T> Iterable<List<T>> sublistsOfSize(List<T> l, int size){
		assert(size >= 0 && size <= l.size());
		return new Iterable<List<T>>() {
			@Override
			public Iterator<List<T>> iterator() {
				return new Iterator<List<T>>() {
					
					private int[] counter = new int[size];
					
					{
						for (int i = 0; i < size; ++i)
							counter[i] = i;
					}

					@Override
					public boolean hasNext() {
						return counter != null;
					}

					@Override
					public List<T> next() {
						List<T> res = new ArrayList<>();
						for (int i : counter)
							res.add(l.get(i));
						int i = size - 1;
						while (i >= 0 && counter[i] == l.size() - size + i)
							--i;
						if (i < 0)
							counter = null;
						else {
							++counter[i];
							for (int j = i + 1; j < size; ++j)
								counter[j] = counter[j-1] + 1;
						}
						return res;
					}
				};
			}
		};
	}
	
	/**
	 * Generates all sublist of a given list.
	 */
	public static <T> Iterable<List<T>> allSublists(List<T> l){
		return new Iterable<List<T>>() {
			@Override
			public Iterator<List<T>> iterator() {
				return new Iterator<List<T>>() {
					private long counter = 0;

					@Override
					public boolean hasNext() {
						return counter < (1l << l.size());
					}

					@Override
					public List<T> next() {
						List<T> res = new ArrayList<>();
						for (int i = 0; i < l.size(); ++i)
							if (((counter >> i) & 1l) == 1)
								res.add(l.get(i));
						++counter;
						return res;
					}
				};
			}
		};
	}
	
	/**
	 * Computes all permutations of a given list. 
	 */
    public static <T> List<List<T>> permutations(final List<T> items) {
        return IntStream.range(0, (int)CombinatoricsUtils.factorial(items.size()))
        		.mapToObj(i -> permutation(i, items))
        		.collect(Collectors.toList());
    }

    private static <T> List<T> permutation(final int count, final List<T> input, final List<T> output) {
        if (input.isEmpty())
        	return output;
        final int fact = (int)CombinatoricsUtils.factorial(input.size() - 1);
        output.add(input.remove(count / fact));
        return permutation(count % fact, input, output);
    }

    private static <T> List<T> permutation(final int count, final List<T> items) {
        return permutation(count, new ArrayList<>(items), new ArrayList<>());
    }
    
    /**
     * Rounds d to the multiplicity of delta.
     */
    public static double round(double d, double delta) {
    	return delta * Math.round(d / delta);
    }
    
    public static String format(double d, int decimalPlaces) {
    	return String.format("%." + decimalPlaces + "f", d);
    }
	
	/**
	 * Computing mean value.
	 */
	public static Double mean(Collection<Double> data){
		Double res = data.stream().reduce(0., Double::sum); 
		if (data.size() > 0)
			res /= data.size();
		return res;
	}
	
	/**
	 * Computing standard deviation.
	 */
	public static Double sd(Collection<Double> data){
		Double res = 0.;
		if (data.size() > 0){
			Double mean = mean(data);
			for (Double x : data)
				res += (x - mean)*(x - mean);
			res /= data.size();
			res = Math.sqrt(res);
		}
		return res;
	}
	
	/**
	 * Computing 95% confidence interval.
	 */
	public static Double conf95(Collection<Double> data){
		Double res = 0.;
		if (data.size() > 0){
			Double sd = sd(data);
			res = 1.96 * sd / Math.sqrt(data.size());
		}
		return res;
	}
	
	/**
	 * Hurwicz zeta function
	 */
	public static double hurwiczZeta(double s, double q, double prec) {
		double res = 0.;
		int n = 0;
		double d = Double.POSITIVE_INFINITY;
		while (d > prec) {
			d = Math.pow(q + n++, -s);
			res += d;
		}
		return res; 
	}
	
	/**
	 * Method choosing which steps of process to report, when we want to limit the number of inner samples.
	 * Returns a map from chosen steps to a percentage of completion.
	 */
	public static Map<Integer,List<Double>> stepsToReport(int totalSteps, int innerSamples){
		assert (totalSteps >= 2);
		Map<Integer,List<Double>> res = new HashMap<>();
		res.put(0, new ArrayList<Double>());
		res.get(0).add(0.);
		if (innerSamples > 0){
			double pStep = 1./(innerSamples + 1);
			int j = 0;
			for (int i = 1; i <= innerSamples; ++i)
				while (j < totalSteps - 1)
					if (((double)j+1)/(totalSteps - 1) > i * pStep){
						if (!res.containsKey(j))
							res.put(j, new ArrayList<Double>());
						res.get(j).add(i * pStep);
						break;
					} else
						++j;
		}
		res.put(totalSteps - 1, new ArrayList<Double>());
		res.get(totalSteps - 1).add(1.);
		return res;
	}
	
	/**
	 * Export data in laTex table format.
	 */
	public static void exportToLatexTable(List<List<String>> tab){
		PrintWriter out = new PrintWriter(System.out);
		out.println("\\begin{tabular}{" + " l".repeat(tab.get(0).size()) + " }");
		for (List<String> line : tab) {
			for (String cell : line.subList(0, line.size() - 1))
				out.print(cell + " & ");
			out.println(Utils.last(line) + " \\\\");
		}
		out.println("\\end{tabular}");
		out.println();
		out.flush();
	}
	
	public static <T> Collector<T, ?, Stream<T>> shuffle() {
        return Collectors.collectingAndThen(Collectors.toList(), collected -> {
            Collections.shuffle(collected);
            return collected.stream();
        });
    }
	
	@FunctionalInterface
	public interface TriConsumer <A, B, C> { 
		public void accept (A a, B b, C c);
	}
	
	@FunctionalInterface
	public interface TriFunction <A, B, C, R> { 
		public R apply (A a, B b, C c);
	}
}
