package anansi.temporal.epidemic;

import java.util.List;
import java.util.stream.Collectors;

import anansi.core.Coalition;
import anansi.temporal.TemporalGraph;
import anansi.utils.Utils;

/**
 * Susceptible-Infected-Recovered epidemic diffusion model for temporal graphs.
 * 
 * @author Marcin Waniek
 */
public class TemporalSIRModel extends TemporalSIModel {
	
	private Coalition recovered;
	private double recoveryProb;

	public TemporalSIRModel(double infectionProb, double recoveryProb) {
		super(infectionProb);
		this.recovered = null;
		this.recoveryProb = recoveryProb;
	}

	@Override
	public String getName() {
		return "TSIR-" + (int)(infectionProb * 100) + "-" + (int)(recoveryProb * 100);
	}
	
	public Coalition getRecovered() {
		return recovered;
	}
	
	@Override
	public Coalition getEverInfectedAndAlive() {
		return Coalition.add(infected, recovered);
	}
	
	@Override
	public void startDiffusion(Coalition source, TemporalGraph g) {
		super.startDiffusion(source, g);
		this.recovered = new Coalition();
	}

	@Override
	protected void executeOneStep(int t) {
		List<Integer> newlyInfected = getNewlyInfected(t);
		newlyInfected.removeIf(i -> recovered.contains(i));
		List<Integer> newlyResistant = infected.stream().filter(i -> Utils.RAND.nextDouble() <= recoveryProb)
				.boxed().collect(Collectors.toList());
		for (int i : newlyInfected) {
			infected.add(i);
			infectionTime.put(i, t);
		}
		infected.removeAll(newlyResistant);
		recovered.addAll(newlyResistant);
	}
}
