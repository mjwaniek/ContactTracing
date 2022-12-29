package anansi.epidemic;

import anansi.core.Coalition;
import anansi.core.Graph;
import anansi.utils.Utils;

/**
 * Linear threshold epidemic cascade model. 
 * 
@article{kleinberg2007cascading,
  title={Cascading behavior in networks: Algorithmic and economic issues},
  author={Kleinberg, Jon},
  journal={Algorithmic game theory},
  volume={24},
  pages={613--632},
  year={2007}
} 
 * @author Marcin Waniek
 */
public class LinearThresholdModel extends EpidemicModel {

	private double[] thresholds;
	private int[] infectedNeighs;
	private Coalition halo;
	private boolean finished;
	
	public LinearThresholdModel() {
		super(0);
		this.thresholds = null;
		this.infectedNeighs = null;
		this.halo = null;
		this.finished = false;
	}

	@Override
	public String getName() {
		return "linthreshold";
	}

	@Override
	public double getBasicProbability() {
		return 0;
	}
	
	@Override
	public void startDiffusion(Coalition source, Graph g) {
		super.startDiffusion(source, g);
		thresholds = new double[g.size()];
		for (int i : g.nodes())
			thresholds[i] = Utils.RAND.nextDouble();
		infectedNeighs = new int[g.size()];
		halo = new Coalition();
		for (int i : source)
			for (int j : g.getSuccs(i)) {
				++infectedNeighs[j];
				if (!source.contains(j))
					halo.add(j);
			}
		this.finished = false;
	}
	
	@Override
	protected boolean diffusionFinished() {
		return finished;
	}

	@Override
	protected Coalition executeOneStep() {
		Coalition newlyInfected = new Coalition();
		Coalition newlyInHalo = new Coalition();
		for (int i : halo)
			if ((double)infectedNeighs[i] / g.getInDegree(i) > thresholds[i]) {
				newlyInfected.add(i);
				for (int j : g.getSuccs(i)) {
					++infectedNeighs[j];
					newlyInHalo.add(j);
				}
			}
		if (newlyInfected.isEmpty())
			finished = true;
		infected.add(newlyInfected);
		halo.add(newlyInHalo);
		halo.remove(infected);
		diffusionRounds = t + 1;
		return newlyInfected;
	}
}
