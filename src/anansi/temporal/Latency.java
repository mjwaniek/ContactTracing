package anansi.temporal;

import java.util.ArrayList;
import java.util.List;

import anansi.core.Coalition;
import anansi.core.Edge;
import anansi.core.Graph;
import anansi.core.GraphChangeListener;
import anansi.utils.Utils;

/**
 * Computing latency in a temporal graph using algorithm from:
 * 
@article{pan2011path,
  title={Path lengths, correlations, and centrality in temporal networks},
  author={Pan, Raj Kumar and Saram{\"a}ki, Jari},
  journal={Physical Review E},
  volume={84},
  number={1},
  pages={016105},
  year={2011},
  publisher={APS}
}
 * 
 * @author Marcin Waniek
 */
public class Latency implements GraphChangeListener {

	private TemporalGraph g;
	private List<PathRec>[][] path;
	private double[][] averageLatency;
	
	private Latency(TemporalGraph g) {
		this.g = g;
	}
	
	public static Latency construct(TemporalGraph g){
		Latency res = new Latency(g);
		g.subscribe(res);
		return res;
	}
	
	public double getAverageLatency(int i, int j) {
		if (path == null)
			recountLatency();
		return averageLatency[i][j];
	}
	
	public int getLatency(int i, int j, int t) {
		if (path == null)
			recountLatency();
		if (path[i][j] != null) {
			PathRec rec = getShortestPathsRecord(i, j, t);
			if (!rec.isDummy())
				return rec.duration + rec.start - t;
		}
		return Integer.MAX_VALUE;
	}
	
	public List<PathRec> getShortestPathsRecords(int i, int j) {
		if (path == null)
			recountLatency();
		return path[i][j];
	}
	
	public PathRec getShortestPathsRecord(int i, int j, int t) {
		if (path == null)
			recountLatency();
		if (path[i][j] != null) {
		    int l = 0;
		    int r = path[i][j].size() - 1; 
	        while (l <= r) { 
	            int m = l + (r - l) / 2;
	            if (path[i][j].get(m).start >= t && (m == 0 || path[i][j].get(m-1).start < t)) 
	                return path[i][j].get(m); 
	            if (path[i][j].get(m).start < t) 
	                l = m + 1; 
	            else
	                r = m - 1; 
	        }
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	private void recountLatency() {
		path = new List[g.size()][g.size()]; // null indicates the lack of path
		averageLatency = new double[g.size()][g.size()];
		for (int t = g.getT() - 1; t >= 0 ; --t) {
			for (int i : g.nodes()) {
				for (int j : g.getTSuccs(i, t)) {
					if (path[i][j] == null)
						path[i][j] = new ArrayList<>();
					path[i][j].add(0, new PathRec(t, 0, 1, false, j));
					for (int k : g.nodes())
						if (k != i && k != j && path[j][k] != null){
							PathRec jk = path[j][k].get(0);
							if (jk.start == t)
								jk = path[j][k].size() > 1 ? path[j][k].get(1) : null;
							if (jk != null) {
								if (path[i][k] == null || jk.arrival() < path[i][k].get(0).arrival()){
									if (path[i][k] == null)
										path[i][k] = new ArrayList<>();
									path[i][k].add(0, new PathRec(t, jk.arrival()-t, jk.number, false, j));
								} else if (jk.arrival() == path[i][k].get(0).arrival() && path[i][k].get(0).start == t){
									path[i][k].get(0).number += jk.number;
									path[i][k].get(0).succs.add(j);
								}
							}
						}
				}
			}
		}
		for (int i : g.nodes())
			for (int j : g.nodes())
				if (i != j) {
					if (path[i][j] == null)
						averageLatency[i][j] = Double.POSITIVE_INFINITY;
					else {
						if (Utils.last(path[i][j]).start < g.getT() - 1)
							path[i][j].add(new PathRec(g.getT() - 1,
								path[i][j].get(0).start + path[i][j].get(0).duration, path[i][j].get(0).number, true,
								new Coalition(path[i][j].get(0).succs)));
						int prevTime = 0;
						for (PathRec rec : path[i][j]) {
							averageLatency[i][j] += (rec.start-prevTime)* ((rec.start-prevTime) / 2. + rec.duration);
							prevTime = rec.start;
						}
						averageLatency[i][j] /= g.getT() - 1;
					}
				}
	}
	
	@Override
	public void notifyAdd(Graph g, Edge e) {
		reactNotify();
	}

	@Override
	public void notifyRemove(Graph g, Edge e) {
		reactNotify();
	}

	@Override
	public void notifyReset(Graph g) {
		reactNotify();	
	}

	protected void reactNotify(){
		path = null;
		averageLatency = null;
	}
	
	public static class PathRec {
		private int start;
		private int duration;
		private int number;
		private boolean dummy;
		private Coalition succs;
		
		public PathRec(int start, int duration, int number, boolean dummy, int succ) {
			this.start = start;
			this.duration = duration;
			this.number = number;
			this.dummy = dummy;
			this.succs = new Coalition(succ);
		}
		
		public PathRec(int start, int duration, int number, boolean dummy, Coalition succs) {
			this.start = start;
			this.duration = duration;
			this.number = number;
			this.dummy = dummy;
			this.succs = succs;
		}
		
		public int getStart() {
			return start;
		}

		public int getDuration() {
			return duration;
		}

		public int getNumber() {
			return number;
		}
		
		public boolean isDummy() {
			return dummy;
		}
		
		public Coalition getSuccs() {
			return succs;
		}

		public int arrival() {
			return start + duration;
		}
		
		@Override
		public String toString() {
			return "(t=" + start + ", d=" + duration + ", n=" + number + ", s=" + succs + (dummy ? ", dummy)" : ")");
		}
	}
}
