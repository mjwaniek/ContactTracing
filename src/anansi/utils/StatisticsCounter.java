package anansi.utils;

/**
 * Class computing mean, standard deviation and 95% confidence interval
 * of a series of numbers in constant memory.
 * 
 * @author Marcin Waniek
 */
public class StatisticsCounter {
	private double sum;
	private double sumSq;
	private long n;

	public StatisticsCounter(double sum, double sumSq, long n) {
		this.sum = sum;
		this.sumSq = sumSq;
		this.n = n;
	}
	
	public StatisticsCounter() {
		this(0., 0., 0);
	}

	public StatisticsCounter(double v) {
		this();
		addValue(v);
	}
	
	public double getSum() {
		return sum;
	}

	public double getSumSq() {
		return sumSq;
	}

	public long getN() {
		return n;
	}

	public StatisticsCounter addValue(double v) {
		sum += v;
		sumSq += v * v;
		++n;
		return this;
	}
	
	public StatisticsCounter addMultipleValues(double v, int k) {
		sum += v * k;
		sumSq += v * v * k;
		n += k;
		return this;
	}
	
	public StatisticsCounter addCounter(double sum, double sumSq, long n) {
		this.sum += sum;
		this.sumSq += sumSq;
		this.n += n;
		return this;
	}
	
	public long size() {
		return n;
	}
	
	public Double getMean() {
		return sum / n;
	}
	
	public Double getSD() {
		if (n > 0) {
			return Math.sqrt(Math.abs(sumSq / n - (sum * sum)/(n * n)));
		} else
			return 0.;
	}
	
	public Double getConf95() {
		if (n > 0)
			return 1.96 * getSD() / Math.sqrt(n);
		else
			return 0.;
	}
}
