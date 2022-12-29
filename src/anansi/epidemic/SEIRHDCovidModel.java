package anansi.epidemic;

import anansi.core.Coalition;
import anansi.core.Graph;
import anansi.utils.Utils;

/**\
 * The SERIHD model of COVID-19 based on Rusu et al. (without the tracing component).
 * 
@article{rusu2021modelling,
  title={Modelling digital and manual contact tracing for COVID-19 Are low uptakes and missed contacts deal-breakers?},
  author={Rusu, Andrei and Farrahi, Katayoun and Emonet, R{\'e}mi},
  journal={medRxiv},
  year={2021},
  publisher={Cold Spring Harbor Laboratory Press}
}
 * 
 * @author Marcin Waniek
 */
public class SEIRHDCovidModel extends EpidemicModel {

	private Coalition susceptible;
	private Coalition exposed;
	private Coalition infectedAsympt;
	private Coalition infectedSympt;
	private Coalition hospitalized;
	private Coalition recovered;
	private Coalition dead;

	private static final double LAMBDA_HD = .0031;
	private static final double LAMBDA_HR = .083;
	private static final double GAMMA = 1./2.3;
	private static final double P_H = .1;
	private static final double P_A = .5;
	private static final double MU_P = 1./1.5;
	private static final double EPSILON = 1./3.7;
	private static final double BETA = .0791;
	
	public SEIRHDCovidModel(int diffusionRounds) {
		super(diffusionRounds);
		this.susceptible = null;
		this.exposed = null;
		this.infectedAsympt = null;
		this.infectedSympt = null;
		this.hospitalized = null;
		this.recovered = null;
		this.dead = null;
	}
	
	@Override
	public String getName() {
		return "SEIRHD-Covid";
	}
	
	public Coalition getSusceptible() {
		return susceptible;
	}
	
	public Coalition getExposed() {
		return exposed;
	}
	
	public Coalition getInfectedAsympt() {
		return infectedAsympt;
	}
	
	public Coalition getInfectedSympt() {
		return infectedSympt;
	}
	
	public Coalition getRecovered() {
		return recovered;
	}
	
	public Coalition getHospitalized() {
		return hospitalized;
	}
	
	public Coalition getDead() {
		return dead;
	}

	@Override
	public double getBasicProbability() {
		return 0.;
	}
	
	@Override
	public void startDiffusion(Coalition source, Graph g) {
		super.startDiffusion(source, g);
		susceptible = g.nodesCoalition().remove(source);
		exposed = new Coalition();
		infectedAsympt = new Coalition();
		infectedSympt = new Coalition();
		hospitalized = new Coalition();
		recovered = new Coalition();
		dead = new Coalition(); 
	}

	@Override
	protected Coalition executeOneStep() {
		// H->D and H->R
		hospitalized.filter(i -> {
			double r = Utils.RAND.nextDouble();
			if (r < LAMBDA_HD + LAMBDA_HR) {
				if (r < LAMBDA_HD)
					dead.add(i);
				else
					recovered.add(i);
				return false;
			} else
				return true;
		});
		
		// Is->H and Is->R
		infectedSympt.filter(i -> {
			if (Utils.RAND.nextDouble() < GAMMA) {
				if (Utils.RAND.nextDouble() < P_H)
					hospitalized.add(i);
				else
					recovered.add(i);
				return false;
			} else
				return true;
		});
		
		// Ia->R
		infectedAsympt.filter(i -> {
			if (Utils.RAND.nextDouble() < GAMMA) {
				recovered.add(i);
				return false;
			} else
				return true;
		});
		
		// Ip -> Ia and Ip -> Is
		infected.filter(i -> {
			if (Utils.RAND.nextDouble() < MU_P) {
				if (Utils.RAND.nextDouble() < P_A)
					infectedAsympt.add(i);
				else
					infectedSympt.add(i);
				return false;
			} else
				return true;
		});
		
		// E->Ip
		Coalition newlyInfected = new Coalition();
		exposed.filter(i -> {
			if (Utils.RAND.nextDouble() < EPSILON) {
				newlyInfected.add(i);
				return false;
			} else
				return true;
		});
		infected.add(newlyInfected);
		
		// S->E
		susceptible.filter(i -> {
			if (Utils.RAND.nextDouble() < BETA * g.getPreds(i).stream()
					.filter(j -> infected.contains(j) || infectedAsympt.contains(j) || infectedSympt.contains(j)).count()) {
				exposed.add(i);
				return false;
			} else
				return true;
		});
		return newlyInfected;
	}
}
