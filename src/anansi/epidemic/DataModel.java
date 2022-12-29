package anansi.epidemic;

import anansi.core.Coalition;

public class DataModel extends EpidemicModel {

	private String name;
	private Coalition dataActive;

	public DataModel(String name, Coalition dataActive) {
		super(1);
		this.name = name;
		this.dataActive = dataActive;
	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public double getBasicProbability() {
		return 0;
	}

	@Override
	protected Coalition executeOneStep() {
		dataActive.stream().filter(i -> i < g.size()).forEach(i -> infected.add(i));
		return dataActive;
	}
}
