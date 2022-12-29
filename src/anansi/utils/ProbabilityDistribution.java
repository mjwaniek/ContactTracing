package anansi.utils;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;

/**
 * Probability distribution over a set of integers.
 * 
 * @author Marcin Waniek
 */
public class ProbabilityDistribution<T> {

	private List<T> values;
	private Function<T,Double> probability;
	private double[] acc;
	
	public ProbabilityDistribution(List<T> values, Function<T,Double> probability) {
		this.values = values;
		this.probability = probability;
		this.acc = null;
	}
	
	public ProbabilityDistribution(List<T> values, Map<T,Double> probability) {
		this(values, t -> probability.get(t));
	}
	
	public T draw() {
		computeAccIfNotReady();
		return drawFromAcc(acc);
	}
	
	public T drawFiltered(Predicate<T> filter) {
		T test = draw();
		if (test == null || filter.test(test))
			return test;
		double[] a = new double[values.size()];
		int i = 0;
		double deltaAcc = 0.;
		for (T t : values) {
			if (!filter.test(t))
				deltaAcc -= acc[i] - (i > 0 ? acc[i-1] : 0.);
			a[i] = acc[i] + deltaAcc;
			++i;
		}
		return drawFromAcc(a);
	}
	
	public T drawUpdated(BiFunction<T,Double,Double> update) {
		computeAccIfNotReady();
		double[] a = new double[values.size()];
		int i = 0;
		for (T t : values) {
			a[i] = update.apply(t, acc[i] - (i > 0 ? acc[i-1] : 0.)) + (i > 0 ? a[i-1] : 0.);
			++i;
		}
		return drawFromAcc(a);
	}
	
	public void update(T t, double delta) {
		update(elem -> elem.equals(t) , delta);
	}
	
	public void update(Predicate<T> filter, double delta) {
		computeAccIfNotReady();
		double deltaAcc = 0.;
		for (int i = 0; i < acc.length; ++i) {
			if (filter.test(values.get(i)))
				deltaAcc += delta;
			acc[i] += deltaAcc;
		}
	}
	
	public void reset() {
		acc = null;
		computeAccIfNotReady();
	}
	
	public void filter(Predicate<T> filter) {
		values = Utils.asList(values.stream().filter(filter));
		acc = null;
	}
	
	private void computeAccIfNotReady() {
		if (acc == null) {
			acc = new double[values.size()];
			int i = 0;
			for (T t : values)
				acc[i++] = probability.apply(t);
			for (int j = 1; j < acc.length; ++j)
				acc[j] += acc[j - 1];
		}
	}
	
	private T drawFromAcc(double[] a) {
		if (a[a.length - 1] <= 0.)
			return null;
		double v = Utils.RAND.nextDouble() * a[a.length - 1];
		if (v <= a[0])
			return values.get(0);
		int l = 0;
		int r = a.length - 1;
		while (l <= r) {
			int m = (l + r) / 2;
			if (m > 0 && a[m - 1] <= v && v <= a[m])
				return values.get(m);
			else if (m > 0 && v < a[m-1])
				r  = m - 1;
			else
				l = m + 1;
		}
		return null;
	}
	
	public static ProbabilityDistribution<Integer> constructInt(int startInclusive, int endExclusive,
			Function<Integer,Double> probability) {
		 return new ProbabilityDistribution<>(
				Utils.asList(IntStream.range(startInclusive, endExclusive).boxed()), probability);
	}
}
