package anansi.temporal.epidemic;

import java.util.List;
import java.util.stream.Collectors;

import anansi.utils.Utils;

/**
 * Susceptible-Infected epidemic diffusion model for temporal graphs.
 * 
 * @author Marcin Waniek
 */
public class TemporalSIModel extends TemporalEpidemicModel {

	protected double infectionProb;
	
	public TemporalSIModel(double infectionProb) {
		this.infectionProb = infectionProb;
	}

	@Override
	public String getName() {
		return "TSI-" + (int)(infectionProb * 100);
	}

	@Override
	protected void executeOneStep(int t) {
		for (int i : getNewlyInfected(t)) {
			infected.add(i);
			infectionTime.put(i, t);
		}
	}
	
	protected List<Integer> getNewlyInfected(int t) {
		return infected.stream().flatMap(i -> g.getTSuccsStream(i, t))
			.filter(j -> !infected.contains(j) && Utils.RAND.nextDouble() <= infectionProb).boxed()
			.collect(Collectors.toList());
	}
}
