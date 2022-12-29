package anansi.temporal;

import anansi.core.Edge;
import anansi.core.Graph;
import anansi.core.GraphGenerator;
import anansi.utils.ProbabilityDistribution;
import anansi.utils.Utils;

/**
 * Generating random temporal networks.
 * 
 * @author Marcin Waniek
 */
public class TemporalGraphGenerator {
	
	private static final int DEF_INTERVAL_MIN = 1;
	private static final int DEF_INTERVAL_MAX = 10000;
	private static final double DEF_INTERVAL_COEFF = 2.;

/*
@article{barabasi1999emergence,
	title={Emergence of scaling in random networks},
	author={Barab{\'a}si, Albert-L{\'a}szl{\'o} and Albert, R{\'e}ka},
	journal={science},
	volume={286},
	number={5439},
	pages={509--512},
	year={1999},
	publisher={American Association for the Advancement of Science}
}
*/
	public static TemporalGraph generateBarabasiAlbertTGraph(int n, int avgDegree, int maxTime, int avgContactsPerEdge,
			double overlap) {
		return generateBarabasiAlbertTGraph(n, avgDegree, maxTime, avgContactsPerEdge, overlap,
				DEF_INTERVAL_MIN, DEF_INTERVAL_MAX, DEF_INTERVAL_COEFF);
	}
	
	public static TemporalGraph generateBarabasiAlbertTGraph(int n, int avgDegree, int maxTime, int avgContactsPerEdge,
			double overlap, int intervalMin, int intervalMax, double intervalCoeff){
		return generateContacts("tba-" + n + "-" + avgDegree
						+ nameSuffix(maxTime, avgContactsPerEdge, overlap, intervalMin, intervalMax, intervalCoeff),
				GraphGenerator.generateBarabasiAlbertGraph(n, avgDegree), maxTime, overlap, avgContactsPerEdge,
				intervalMin, intervalMax, intervalCoeff);
	}
	
/*
@article{erdds1959random,
	title={On random graphs I.},
	author={Erd{\H{o}}s, Paul and R{\'e}nyi, Alfr{\'e}d},
	journal={Publ. Math. Debrecen},
	volume={6},
	pages={290--297},
	year={1959}
}
*/
	public static TemporalGraph generateErdosRenyiTGraph(int n, int avgDegree, int maxTime, int avgContactsPerEdge,
			double overlap){
		return generateErdosRenyiTGraph(n, avgDegree, maxTime, avgContactsPerEdge, overlap,
				DEF_INTERVAL_MIN, DEF_INTERVAL_MAX, DEF_INTERVAL_COEFF);
	}
	
	public static TemporalGraph generateErdosRenyiTGraph(int n, int avgDegree, int maxTime, int avgContactsPerEdge,
			double overlap, int intervalMin, int intervalMax, double intervalCoeff){
		return generateContacts("ter-" + n + "-" + avgDegree
						+ nameSuffix(maxTime, avgContactsPerEdge, overlap, intervalMin, intervalMax, intervalCoeff),
				GraphGenerator.generateErdosRenyiGraph(n, avgDegree), maxTime, overlap, avgContactsPerEdge,
				intervalMin, intervalMax, intervalCoeff);
	}

/*
@article{watts1998collective,
	title={Collective dynamics of	small-world networks},
	author={Watts, Duncan J and Strogatz, Steven H},
	journal={nature},
	volume={393},
	number={6684},
	pages={440--442},
	year={1998},
	publisher={Nature Publishing Group}
}
*/
	public static TemporalGraph generateSmallWorldTGraph(int n, int avgDegree, double beta,int maxTime,
			int avgContactsPerEdge, double overlap){
		return generateSmallWorldTGraph(n, avgDegree, beta, maxTime, avgContactsPerEdge, overlap,
				DEF_INTERVAL_MIN, DEF_INTERVAL_MAX, DEF_INTERVAL_COEFF);
	}
	
	public static TemporalGraph generateSmallWorldTGraph(int n, int avgDegree, double beta,int maxTime,
			int avgContactsPerEdge, double overlap, int intervalMin, int intervalMax, double intervalCoeff){
		return generateContacts("tws-" + n + "-" + avgDegree
						+ nameSuffix(maxTime, avgContactsPerEdge, overlap, intervalMin, intervalMax, intervalCoeff),
				GraphGenerator.generateSmallWorldGraph(n, avgDegree, beta), maxTime, overlap, avgContactsPerEdge,
				intervalMin, intervalMax, intervalCoeff);
	}
	
	private static String nameSuffix(int maxTime, int avgContactsPerEdge, double overlap, int intervalMin,
			int intervalMax, double intervalCoeff) {
		return "-" + maxTime + "-" + avgContactsPerEdge + "-" + (int)(overlap * 100)
				+ ((intervalMin==DEF_INTERVAL_MIN && intervalMax==DEF_INTERVAL_MAX && intervalCoeff==DEF_INTERVAL_COEFF)
					? "" : "-" + intervalMin + "-" + intervalMax + "-" + intervalCoeff);
	}
	
/*
@article{holme2013epidemiologically,
  title={Epidemiologically optimal static networks from temporal network data},
  author={Holme, Petter},
  journal={PLoS computational biology},
  volume={9},
  number={7},
  pages={e1003142},
  year={2013},
  publisher={Public Library of Science}
}
 */
	private static TemporalGraph generateContacts(String name, Graph g, int maxTime, double overlap,
			int avgContactsPerEdge, int intervalMin, int intervalMax, double intervalCoeff){
		TemporalGraph res = new TemporalGraph(name, g.size());
		ProbabilityDistribution<Integer> pd = ProbabilityDistribution.constructInt(intervalMin, intervalMax + 1,
				x -> Math.pow(x, -intervalCoeff));
		long[] contacts = new long[avgContactsPerEdge * g.edgesCount()];
		for (int i = 0; i < contacts.length; ++i)
			contacts[i] = pd.draw() + (i == 0 ? 0 : contacts[i-1]);
		double period = (double)(contacts[contacts.length - 1] + pd.draw()) / g.edgesCount();
		int i = 0;
		double edgeLowerBound = 0.;
		for (Edge e : g.edges()) {
			double edgeBeginTime = Utils.RAND.nextDouble() * (1. - overlap) * maxTime;
			while (i < contacts.length && contacts[i] < edgeLowerBound + period)
				res.addTEdge(e.i(), e.j(), (int)(edgeBeginTime + overlap * maxTime
						* ((contacts[i++] - edgeLowerBound) / period)));
			edgeLowerBound += period;
		}
		return res;
	}
}
