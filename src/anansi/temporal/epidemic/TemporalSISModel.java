package anansi.temporal.epidemic;

import java.util.List;

import anansi.core.Coalition;
import anansi.temporal.TemporalGraph;
import anansi.utils.Utils;

/**
 * Susceptible-Infected-Susceptible epidemic diffusion model for temporal graphs.
 * 
 * @author Marcin Waniek
 */
public class TemporalSISModel extends TemporalSIModel {

	private Coalition everInfected;
	private double resetProb;
	
	public TemporalSISModel(double infectionProb, double resetProb) {
		super(infectionProb);
		this.everInfected = null;
		this.resetProb = resetProb;
	}

	@Override
	public String getName() {
		return "TSIS-" + (int)(infectionProb * 100) + "-" + (int)(resetProb * 100);
	}
	
	@Override
	public Coalition getEverInfectedAndAlive() {
		return everInfected;
	}
	
	@Override
	protected void startDiffusion(Coalition source, TemporalGraph g) {
		super.startDiffusion(source, g);
		everInfected = new Coalition(source);
	}

	@Override
	protected void executeOneStep(int t) {
		List<Integer> newlyInfected = getNewlyInfected(t);
		everInfected.addAll(newlyInfected);
		infected.filter(i -> Utils.RAND.nextDouble() <= 1. - resetProb);
		for (int i : newlyInfected) {
			infected.add(i);
			infectionTime.put(i, t);
		}
	}
}
