package anansi.experiment;

import java.util.List;

import anansi.utils.Utils;

/**
 * Abstraction of an experiment.
 * 
 * @author Marcin Waniek
 */
public abstract class Experiment {
	
	private String resultsDirPath;
	
	public Experiment(String resultsDirPath){
		this.resultsDirPath = resultsDirPath;
	}

	public abstract String getName();
		
	protected abstract List<String> getColumnNames();
	
	protected List<String> getHeader() {
		return Utils.aList(getName());
	}

	public ExperimentResult perform(){
		System.out.println("Beginning experiment " + getName() + " with header " + getHeader());
		long time = System.currentTimeMillis();
		
		ExperimentResult res = new ExperimentResult(getName(), resultsDirPath, getHeader(), getColumnNames());
		perform(res);
		if (!res.getRows().isEmpty())
			res.saveResult();
		
		time = System.currentTimeMillis() - time;
		System.out.println("Experiment finished in " + Utils.timeDesc(time) + "\n");
		return res;
	}
	
	protected abstract void perform(ExperimentResult res);
}