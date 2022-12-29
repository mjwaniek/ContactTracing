package anansi.epidemic;

import anansi.core.Coalition;
import anansi.core.Graph;
import anansi.utils.Utils;

/**
 * Susceptible-Infected-Recovered epidemic diffusion model.
 * 
 * @author Marcin Waniek
 */
public class SIRModel extends SIModel {
	
	private Coalition recovered;
	private double recoveryProb;

	public SIRModel(double infectionProb, double recoveryProb, int diffusionRounds) {
		super(infectionProb, diffusionRounds);
		this.recovered = null;
		this.recoveryProb = recoveryProb;
	}

	@Override
	public String getName() {
		return "SIR-" + (int)(infectionProb * 100) + "-" + (int)(recoveryProb * 100) + "-" + diffusionRounds;
	}
	
	public Coalition getRecovered() {
		return recovered;
	}
	
	@Override
	public void startDiffusion(int source, Graph g) {
		super.startDiffusion(source, g);
		this.recovered = new Coalition();
	}

	@Override
	protected Coalition executeOneStep() {
		Coalition newlyInfected = getNewlyInfected();
		newlyInfected.filter(i -> !recovered.contains(i));
		Coalition newlyResistant = infected.stream().filter(i -> Utils.RAND.nextDouble() <= recoveryProb)
				.boxed().collect(Coalition.getCollector());
		infected.add(newlyInfected);
		infected.remove(newlyResistant);
		recovered.add(newlyResistant);
		return newlyInfected;
	}
}
