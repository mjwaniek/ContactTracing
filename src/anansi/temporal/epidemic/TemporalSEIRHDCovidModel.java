package anansi.temporal.epidemic;

import anansi.core.Coalition;
import anansi.temporal.TemporalGraph;
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
public class TemporalSEIRHDCovidModel extends TemporalEpidemicModel {

	private Coalition susceptible;
	private Coalition exposed;
	private Coalition infectedAsympt;
	private Coalition infectedSympt;
	private Coalition hospitalized;
	private Coalition recovered;
	private Coalition dead;
	
	private double lambdaHD;
	private double lambdaHR;
	private double gamma;
	private double pH;
	private double pA;
	private double muP;
	private double epsilon;
	private double beta;

	private static final double DEF_LAMBDA_HD = .0031;
	private static final double DEF_LAMBDA_HR = .083;
	private static final double DEF_GAMMA = 1./2.3;
	private static final double DEF_P_H = .1;
	private static final double DEF_P_A = .5;
	private static final double DEF_MU_P = 1./1.5;
	private static final double DEF_EPSILON = 1./3.7;
	private static final double DEF_BETA = .0791;
		
	public TemporalSEIRHDCovidModel() {
		this.susceptible = null;
		this.exposed = null;
		this.infectedAsympt = null;
		this.infectedSympt = null;
		this.hospitalized = null;
		this.recovered = null;
		this.dead = null;
		setDefaultParams();
	}
	
	public TemporalSEIRHDCovidModel(TemporalSEIRHDCovidModel tcm) {
		super(tcm);
		this.susceptible = new Coalition(tcm.susceptible);
		this.exposed = new Coalition(tcm.exposed);
		this.infectedAsympt = new Coalition(tcm.infectedAsympt);
		this.infectedSympt = new Coalition(tcm.infectedSympt);
		this.hospitalized = new Coalition(tcm.hospitalized);
		this.recovered = new Coalition(tcm.recovered);
		this.dead = new Coalition(tcm.dead);
		this.lambdaHD = tcm.lambdaHD;
		this.lambdaHR = tcm.lambdaHR;
		this.gamma = tcm.gamma;
		this.pH = tcm.pH;
		this.pA = tcm.pA;
		this.muP = tcm.muP;
		this.epsilon = tcm.epsilon;
		this.beta = tcm.beta;
	}
	
	@Override
	public String getName() {
		return "SEIRHD-Covid";
	}
	
	public TemporalSEIRHDCovidModel setDefaultParams() {
		this.lambdaHD = DEF_LAMBDA_HD;
		this.lambdaHR = DEF_LAMBDA_HR;
		this.gamma = DEF_GAMMA;
		this.pH = DEF_P_H;
		this.pA = DEF_P_A;
		this.muP = DEF_MU_P;
		this.epsilon = DEF_EPSILON;
		this.beta = DEF_BETA;
		return this;
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
	
	@Override
	public Coalition getDead() {
		return dead;
	}
	
	public double getLambdaHD() {
		return lambdaHD;
	}

	public double getLambdaHR() {
		return lambdaHR;
	}

	public double getGamma() {
		return gamma;
	}

	public double getpH() {
		return pH;
	}

	public double getpA() {
		return pA;
	}

	public double getMuP() {
		return muP;
	}

	public double getEpsilon() {
		return epsilon;
	}

	public double getBeta() {
		return beta;
	}

	public TemporalSEIRHDCovidModel setLambdaHD(double lambdaHD) {
		this.lambdaHD = lambdaHD;
		return this;
	}

	public TemporalSEIRHDCovidModel setLambdaHR(double lambdaHR) {
		this.lambdaHR = lambdaHR;
		return this;
	}

	public TemporalSEIRHDCovidModel setGamma(double gamma) {
		this.gamma = gamma;
		return this;
	}

	public TemporalSEIRHDCovidModel setpH(double pH) {
		this.pH = pH;
		return this;
	}

	public TemporalSEIRHDCovidModel setpA(double pA) {
		this.pA = pA;
		return this;
	}

	public TemporalSEIRHDCovidModel setMuP(double muP) {
		this.muP = muP;
		return this;
	}

	public TemporalSEIRHDCovidModel setEpsilon(double epsilon) {
		this.epsilon = epsilon;
		return this;
	}

	public TemporalSEIRHDCovidModel setBeta(double beta) {
		this.beta = beta;
		return this;
	}

	@Override
	public Coalition getEverInfected() {
		return Coalition.add(getEverInfectedAndAlive(), getDead());
	}

	@Override
	public Coalition getEverInfectedAndAlive() {
		return Coalition.add(infected, infectedSympt, infectedAsympt, hospitalized, recovered);
	}
	
	@Override
	public Coalition getNowInfected() {
		return Coalition.add(infected, infectedSympt, infectedAsympt);
	}
	
	public void vaccinate(int i) {
		if (!dead.contains(i) && !hospitalized.contains(i)) {
			susceptible.remove(i);
			exposed.remove(i);
			infected.remove(i);
			infectedAsympt.remove(i);
			infectedSympt.remove(i);
			recovered.add(i);
		}
	}
	
	@Override
	protected void startDiffusion(Coalition source, TemporalGraph g) {
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
	protected void executeOneStep(int t) {
		// H->D and H->R
		hospitalized.filter(i -> {
			double r = Utils.RAND.nextDouble();
			if (r < lambdaHD + lambdaHR) {
				if (r < lambdaHD)
					dead.add(i);
				else
					recovered.add(i);
				return false;
			} else
				return true;
		});
		
		// Is->H and Is->R
		infectedSympt.filter(i -> {
			if (Utils.RAND.nextDouble() < gamma) {
				if (Utils.RAND.nextDouble() < pH)
					hospitalized.add(i);
				else
					recovered.add(i);
				return false;
			} else
				return true;
		});
		
		// Ia->R
		infectedAsympt.filter(i -> {
			if (Utils.RAND.nextDouble() < gamma) {
				recovered.add(i);
				return false;
			} else
				return true;
		});
		
		// Ip -> Ia and Ip -> Is
		infected.filter(i -> {
			if (Utils.RAND.nextDouble() < muP) {
				if (Utils.RAND.nextDouble() < pA)
					infectedAsympt.add(i);
				else
					infectedSympt.add(i);
				return false;
			} else
				return true;
		});
		
		// E->Ip
		exposed.filter(i -> {
			if (Utils.RAND.nextDouble() < epsilon) {
				infected.add(i);
				infectionTime.put(i, t);
				return false;
			} else
				return true;
		});
		
		// S->E
		susceptible.filter(i -> {
			if (Utils.RAND.nextDouble() < beta * g.getTPredsStream(i, t)
					.filter(j -> infected.contains(j) || infectedAsympt.contains(j) || infectedSympt.contains(j)).count()) {
				exposed.add(i);
				return false;
			} else
				return true;
		});
	}
}
