package anansi.temporal.epidemic;

import java.util.HashMap;
import java.util.Map;

import anansi.core.Coalition;
import anansi.temporal.TemporalGraph;

/**
 * Representation of an epidemic diffusion model for temporal graphs.
 * 
 * @author Marcin Waniek
 */
public abstract class TemporalEpidemicModel {

	protected Coalition infected;
	protected Map<Integer,Integer> infectionTime;
	protected TemporalGraph g;
	protected int t;
	
	public TemporalEpidemicModel() {
		this.infected = null;
		this.infectionTime = null;
		this.g = null;
		this.t = 0;
	}
	
	public TemporalEpidemicModel(TemporalEpidemicModel tem) {
		this.infected = new Coalition(tem.infected);
		this.infectionTime = new HashMap<>(tem.infectionTime);
		this.g = tem.g;
		this.t = tem.t;
	}
	
	public abstract String getName();
	
	public Coalition getInfected() {
		return infected;
	}

	public Coalition getEverInfected() {
		return infected;
	}
	
	public Coalition getEverInfectedAndAlive() {
		return infected;
	}
	
	public Coalition getNowInfected() {
		return infected;
	}
	
	public Coalition getDead() {
		return new Coalition();
	}
	
	public double getAverageInfected() {
		return (double) getEverInfected().size() / (getEverInfected().size() - getNowInfected().size()); 
	}
	
	public Integer getInfectionTime(int i) {
		return infectionTime.get(i);
	}
	
	public Coalition runDiffusion(int source, TemporalGraph g) {
		return runDiffusion(new Coalition(source), g);
	}
	
	public Coalition runDiffusion(Coalition source, TemporalGraph g) {
		return runDiffusion(source, g, g.getT());
	}
	
	public Coalition runDiffusion(int source, TemporalGraph g, int rounds) {
		return runDiffusion(new Coalition(source), g, rounds);
	}
	
	public Coalition runDiffusion(Coalition source, TemporalGraph g, int rounds) {
		startDiffusion(source, g);
		while (t < rounds)
			executeOneStep(t++);
		return infected;
	}
	
	protected void startDiffusion(Coalition source, TemporalGraph g) {
		this.g = g;
		this.infected = new Coalition(source);
		this.infectionTime = new HashMap<>();
		this.t = 0;
		source.forEach(i -> this.infectionTime.put(i, 0));
	}
	
	public Coalition continueDiffusion(int totalRounds) {
		while (t < totalRounds)
			executeOneStep(t++);
		return infected;
	}
	
	protected abstract void executeOneStep(int t);
	
	@Override
	public int hashCode() {
		return getName().hashCode();
	}
}
