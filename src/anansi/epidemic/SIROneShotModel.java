package anansi.epidemic;

import anansi.core.Coalition;
import anansi.core.Graph;

/**
 * Variation of the SIR model where infected have only one chance to infect before they become resistant.
 * 
@article{goel2016structural,
  title={The structural virality of online diffusion},
  author={Goel, Sharad and Anderson, Ashton and Hofman, Jake and Watts, Duncan J},
  journal={Management Science},
  volume={62},
  number={1},
  pages={180--196},
  year={2016},
  publisher={INFORMS}
}
 * 
 * @author Marcin Waniek
 */
public class SIROneShotModel extends SIModel {

	private Coalition recovered;

	public SIROneShotModel(double infectionProb) {
		super(infectionProb, 0);
		this.recovered = null;
	}

	@Override
	public String getName() {
		return "SIR-OneShot-" + (int)(infectionProb * 100);
	}
	
	public Coalition getRecovered() {
		return recovered;
	}
	
	@Override
	public void startDiffusion(Coalition source, Graph g) {
		super.startDiffusion(source, g);
		this.recovered = new Coalition();
	}
	
	@Override
	protected boolean diffusionFinished() {
		return infected.isEmpty();
	}
	
	@Override
	protected Coalition diffusionResult() {
		return recovered;
	}

	@Override
	protected Coalition executeOneStep() {
		Coalition newlyInfected = getNewlyInfected();
		newlyInfected.filter(i -> !recovered.contains(i));
		recovered.add(infected);
		infected = newlyInfected;
		diffusionRounds = t + 1;
		return newlyInfected;
	}
}
