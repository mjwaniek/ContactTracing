package anansi.core;

import java.util.stream.IntStream;

/**
 * Abstraction of a Monte Carlo method.
 * 
 * @author Marcin Waniek
 */
public abstract class MonteCarloAlgorithm {

	protected void preProcess(){}
	
	protected abstract void singleMCIteration();
	
	protected abstract double getControlSum(int iter);
	
	protected void postProcess(){}
	
	public int getPortion(){
		return 1000;
	}
	
	public int getMinIterations(){
		return 1000;
	}
	
	public int getMaxIterations(){
		return 1000000;
	}
	
	public double getPrecision(){
		return 0.00001;
	}

	public Double runProcess(){
		return runAll(() -> IntStream.range(0, getPortion()).forEach(i -> singleMCIteration()));
	}
	
	public Double runParallelProcess(){
		return runAll(() -> IntStream.range(0, getPortion()).parallel().forEach(i -> singleMCIteration()));
	}
	
	private Double runAll(Runnable runPortion){
		Double controlSum = null;
		preProcess();
		int iter = 0;
		while (iter < getMaxIterations()) {
			runPortion.run();
			iter += getPortion();
			Double newControlSum = getControlSum(iter);
			if (controlSum != null && Math.abs(controlSum - newControlSum) < getPrecision() && iter > getMinIterations())
				break;
			controlSum = newControlSum;
		}
		postProcess();
		return controlSum;
	}
}
