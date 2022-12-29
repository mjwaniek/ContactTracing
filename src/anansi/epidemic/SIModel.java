package anansi.epidemic;

import anansi.core.Coalition;
import anansi.utils.Utils;

/**
 * Susceptible-Infected epidemic diffusion model.
 * 
 * @author Marcin Waniek
 */
public class SIModel extends EpidemicModel {

	protected double infectionProb;
	
	public SIModel(double infectionProb, int diffusionRounds) {
		super(diffusionRounds);
		this.infectionProb = infectionProb;
	}

	@Override
	public String getName() {
		return "SI-" + (int)(infectionProb * 100) + "-" + diffusionRounds;
	}

	@Override
	public double getBasicProbability() {
		return infectionProb;
	}

	@Override
	protected Coalition executeOneStep() {
		Coalition newlyInfected = getNewlyInfected();
		newlyInfected.forEach(i -> infected.add(i));
		return newlyInfected;
	}
	
	protected Coalition getNewlyInfected() {
		Coalition res = new Coalition();
		for (int i : infected)
			for (int j : g.getSuccs(i))
				if (!infected.contains(j) && Utils.RAND.nextDouble() <= infectionProb)
					res.add(j);
		return res;
	}
}
