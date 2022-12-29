package anansi.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Distribution of numbers.
 * 
 * @author Marcin Waniek
 */
public class Distribution {
	
	private Map<Integer, Integer> m;
	private Integer count;
	private Double mean;
	private Double median;
	private Double standardDeviation;
	
	public Distribution(IntStream elems) {
		this.m = new HashMap<>();
		elems.boxed().forEach(k -> m.compute(k, (__,x) -> x == null ? 1 : x + 1));
		this.count = null;
		this.mean = null;
		this.median = null;
		this.standardDeviation = null;
	}
	
	public Distribution(IntStream elems, Function<Integer, Integer> count) {
		this.m = elems.boxed().collect(Collectors.toMap(k -> k, k -> count.apply(k)));
		this.count = null;
		this.mean = null;
		this.median = null;
		this.standardDeviation = null;
	}
	
	public Distribution(Collection<Integer> elems) {
		this(elems.stream().mapToInt(k -> k));
	}
	
	public Distribution(Collection<Integer> elems, Function<Integer, Integer> count) {
		this(elems.stream().mapToInt(k -> k), count);
	}
	
	public Set<Integer> keys() {
		return m.keySet();
	}
	
	public int get(Integer k) {
		return m.get(k);
	}
	
	public int count() {
		if (count == null)
			count = m.values().stream().mapToInt(x -> x).sum();
		return count;
	}

	public double mean() {
		if (mean == null)
			mean = m.keySet().stream().mapToDouble(k -> m.get(k) * k).sum() / count();
		return mean;
	}

	public double standardDeviation() {
		if (standardDeviation == null)
			standardDeviation =Math.sqrt(
					m.keySet().stream().mapToDouble(k -> m.get(k) * Math.pow(k - mean(), 2.)).sum() / count());
		return standardDeviation;
	}

	public double coefficientVariation() {
		return mean() == 0. ? 0. : standardDeviation() / Math.abs(mean());
	}
	
	public double median() {
		if (median == null) {
			Ref<Integer> skip = new Ref<>((count() - 1) / 2);
			m.keySet().stream().sorted().forEach(k -> {
				if (median == null && skip.get() < m.get(k))
					median = k.doubleValue();
				else if (count() % 2 == 0 && skip.get() == -1)
					median = (median + k) / 2.;
				skip.set(skip.get() - m.get(k));
			});
		}
		return median;
	}

	public double skewness() {
		return standardDeviation() == 0 ? 0. : (mean() - median()) / standardDeviation();
	}
	
	public void print() {
		m.keySet().stream().sorted().forEach(k -> System.out.println(k + "->\t" + m.get(k)));
	}
	
	@Override
	public String toString() {
		return m.toString();
	}
}
