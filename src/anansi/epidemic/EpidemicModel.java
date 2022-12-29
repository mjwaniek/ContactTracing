package anansi.epidemic;

import java.util.HashMap;
import java.util.Map;

import anansi.core.Coalition;
import anansi.core.Graph;

/**
 * Representation of an epidemic diffusion model.
 * 
 * @author Marcin Waniek
 */
public abstract class EpidemicModel {

	protected Coalition infected;
	protected Map<Integer,Integer> infectionTime;
	protected int t;
	protected Graph g;
	protected int diffusionRounds;
	
	public EpidemicModel(int diffusionRounds) {
		this.infected = null;
		this.infectionTime = null;
		this.t = 0;
		this.g = null;
		this.diffusionRounds = diffusionRounds;
	}
	
	public abstract String getName();
	
	public abstract double getBasicProbability();
	
	protected abstract Coalition executeOneStep();
	
	public Coalition getInfected() {
		return infected;
	}
	
	public Integer getInfectionTime(int i) {
		return infectionTime.get(i);
	}
	
	public int getTime() {
		return t;
	}
	
	public int getDiffusionRounds() {
		return diffusionRounds;
	}
	
	public void setDiffusionRounds(int diffusionRounds) {
		this.diffusionRounds = diffusionRounds;
	}
	
	protected boolean diffusionFinished() {
		return t >= diffusionRounds;
	}
	
	protected Coalition diffusionResult() {
		return infected;
	}
	
	public Coalition runDiffusion(Coalition source, Graph g) {
		startDiffusion(source, g);
		while (!diffusionFinished())
			runRound();
		return diffusionResult();
	}
	
	public Coalition runDiffusion(int source, Graph g) {
		return runDiffusion(new Coalition(source), g);
	}
	
	public void startDiffusion(Coalition source, Graph g) {
		this.g = g;
		infected = new Coalition(source);
		infectionTime = new HashMap<>();
		source.forEach(i -> infectionTime.put(i, 0));
		t = 0;
	}
	
	public void startDiffusion(int source, Graph g) {
		startDiffusion(new Coalition(source), g);
	}

	public Coalition runRound() {
		Coalition newlyInfected = executeOneStep();
		++t;
		newlyInfected.forEach(i -> infectionTime.put(i, t));
		return newlyInfected;
	}
	
	@Override
	public int hashCode() {
		return getName().hashCode();
	}
}
