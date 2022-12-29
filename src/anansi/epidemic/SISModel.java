package anansi.epidemic;

import anansi.core.Coalition;
import anansi.utils.Utils;

/**
 * Susceptible-Infected-Susceptible epidemic diffusion model.
 * 
 * @author Marcin Waniek
 */
public class SISModel extends SIModel {

	private double resetProb;
	
	public SISModel(double infectionProb, double resetProb, int diffusionRounds) {
		super(infectionProb, diffusionRounds);
		this.resetProb = resetProb;
	}

	@Override
	public String getName() {
		return "SIS-" + (int)(infectionProb * 100) + "-" + (int)(resetProb * 100) + "-" + diffusionRounds;
	}

	@Override
	protected Coalition executeOneStep() {
		Coalition newlyInfected = getNewlyInfected();
		infected.filter(i -> Utils.RAND.nextDouble() <= 1. - resetProb);
		infected.add(newlyInfected);
		return newlyInfected;
	}
}
