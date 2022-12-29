package anansi.epidemic;

import anansi.core.Coalition;
import anansi.core.Graph;
import anansi.utils.Utils;

/**
 * Susceptible-Infected epidemic diffusion model with vaccinated state.
 * 
 * @author Marcin Waniek
 */
public class SIVacModel extends SIModel {

	protected Coalition vaccinated;
	
	public SIVacModel(double infectionProb, int diffusionRounds) {
		super(infectionProb, diffusionRounds);
		this.vaccinated = null;
	}

	@Override
	public String getName() {
		return "SIV-" + (int)(infectionProb * 100) + "-" + diffusionRounds;
	}
	
	public void vaccinate(int i) {
		this.vaccinated.add(i);
	}

	@Override
	public void startDiffusion(int source, Graph g) {
		super.startDiffusion(source, g);
		this.vaccinated = new Coalition();
	}
	
	@Override
	protected Coalition getNewlyInfected() {
		Coalition res = new Coalition();
		for (int i : infected)
			if (!vaccinated.contains(i))
				for (int j : g.getSuccs(i))
					if (!infected.contains(j) && !vaccinated.contains(j) && Utils.RAND.nextDouble() <= infectionProb)
						res.add(j);
		return res;
	}
}
