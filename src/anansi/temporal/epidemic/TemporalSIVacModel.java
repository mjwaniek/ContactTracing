package anansi.temporal.epidemic;

import java.util.List;
import java.util.stream.Collectors;

import anansi.core.Coalition;
import anansi.temporal.TemporalGraph;
import anansi.utils.Utils;

/**
 * Susceptible-Infected epidemic diffusion model with vaccinated state for temporal graphs.
 * 
 * @author Marcin Waniek
 */
public class TemporalSIVacModel extends TemporalSIModel {

	protected Coalition vaccinated;

	public TemporalSIVacModel(double infectionProb) {
		super(infectionProb);
		this.vaccinated = null;
	}

	@Override
	public String getName() {
		return "TSIV-" + (int)(infectionProb * 100);
	}
	
	public void vaccinate(int i) {
		this.vaccinated.add(i);
	}
	
	@Override
	protected void startDiffusion(Coalition source, TemporalGraph g) {
		super.startDiffusion(source, g);
		this.vaccinated = new Coalition();
	}
	
	@Override
	protected List<Integer> getNewlyInfected(int t) {
		return infected.stream().filter(i -> !vaccinated.contains(i)).flatMap(i -> g.getTSuccsStream(i, t))
			.filter(j -> !infected.contains(j) && !vaccinated.contains(j) && Utils.RAND.nextDouble() <= infectionProb)
			.boxed().collect(Collectors.toList());
	}
}
