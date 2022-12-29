package anansi.temporal;

import java.util.List;
import java.util.Stack;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import anansi.core.Coalition;
import anansi.core.Edge;
import anansi.core.Graph;
import anansi.core.LWGraph;
import anansi.temporal.Latency.PathRec;
import anansi.utils.Utils;

/**
 * Representation of a temporal graph.
 * 
 * @author Marcin Waniek
 */
public class TemporalGraph extends LWGraph<Void, Timeline> {

	private int maxTime;
	private Stack<TemporalChange> history;
	private Latency latency;
	
	public TemporalGraph(String name, int n, boolean directed){
		super(name, n, directed);
		this.maxTime = 0;
		this.latency = null;
	}
	
	public TemporalGraph(String name, int n){
		this(name, n, false);
	}
	
	/**
	 * Get the number of time moments in the interval.
	 */
	public int getT() {
		return maxTime + 1;
	}
	
	public int contactsCount() {
		return edgesStream().mapToInt(e -> w(e).size()).sum();
	}
	
	public void addTEdge(int i, int j, int t) {
		addTEdge(i, j, t, t);
	}
	
	public void addTEdge(int i, int j, int begin, int end) {
		if (i != j){
			performAddTEdge(i, j, begin, end);
			if (history != null)
				history.push(new TemporalAddition(i, j, begin, end));
			notifyListenersAdd(new Edge(i, j, isDirected()));
		}
	}
	
	public void removeTEdge(int i, int j, int t) {
		removeTEdge(i, j, t, t);
	}
	
	public void removeTEdge(int i, int j, int begin, int end) {
		if (i != j && containsEdge(i, j)){
			performRemoveTEdge(i, j, begin, end, true);
			if (history != null)
				history.push(new TemporalRemoval(i, j, begin, end));
			notifyListenersRemove(new Edge(i, j, isDirected()));
		}
	}
	
	protected void performAddTEdge(int i, int j, int begin, int end){
		if (containsEdge(i, j)) {
			if (w(i, j) == null)
				setWeight(i, j, new Timeline(begin, end));
			else
				w(i, j).add(begin, end);
		} else
			addEdge(i, j, new Timeline(begin, end));
		maxTime = Math.max(maxTime, end);
	}
	
	protected void performRemoveTEdge(int i, int j, int begin, int end, boolean recount){
		w(i, j).remove(begin, end);
		if (w(i, j).isEmpty())
			removeEdge(i, j);
		recountMaxTimeAfterRemoval();
	}
	
	@Override
	protected void performAddEdge(int i, int j, Timeline w) {
		super.performAddEdge(i, j, w);
		if (w != null)
			maxTime = Math.max(maxTime, w.getMaxTime());
	}
	
	@Override
	protected void performRemoveEdge(int i, int j) {
		super.performRemoveEdge(i, j);
		recountMaxTimeAfterRemoval();
	}
	
	private void recountMaxTimeAfterRemoval() {
		int newMax = 0;
		for (Edge e : edges())
			if (w(e).getMaxTime() == maxTime)
				return;
			else
				newMax = Math.max(newMax, w(e).getMaxTime());
		maxTime = newMax;
	}
	
	@Override
	public void forceConnectivity() {
		forceConnectivity((c, rest) -> e(c.getAny().intValue(), rest.findAny().getAsInt()),
				e -> addTEdge(e.i(), e.j(), Utils.RAND.nextInt(getT())));
	}
	
	public boolean containsTEdge(int i, int j, int t){
		return containsEdge(i, j) && w(i, j).contains(t);
	}
	
	public IntStream getTSuccsStream(int i, int t) {
		return getSuccs(i).stream().filter(j -> w(i, j).contains(t));
	}
	
	public IntStream getTPredsStream(int i, int t) {
		return getPreds(i).stream().filter(j -> w(j, i).contains(t));
	}
	
	public IntStream getTNeighsStream(int i, int t) {
		if (isDirected())
			return IntStream.concat(getTSuccsStream(i, t), getTPredsStream(i, t)).distinct();
		else
			return getTSuccsStream(i, t);
	}
	
	public Coalition getTSuccs(int i, int t) {
		return getTSuccsStream(i, t).boxed().collect(Coalition.getCollector());
	}
	
	public Coalition getTPreds(int i, int t) {
		return getTPredsStream(i, t).boxed().collect(Coalition.getCollector());
	}
	
	public Coalition getTNeighs(int i, int t) {
		return getTNeighsStream(i, t).boxed().collect(Coalition.getCollector());
	}
	
	public Stream<Edge> tEdgesStream(int t){
		return nodesStream()
				.mapToObj(i -> getTSuccsStream(i, t).filter(j -> i < j || isDirected()).mapToObj(j -> e(i, j)))
				.flatMap(s -> s);
	}
	
	public int getContactsNumber(int i) {
		return getNeighs(i).stream().map(j -> w(i, j).size()).sum();
	}
	
	public int getOutContactsNumber(int i) {
		return getSuccs(i).stream().map(j -> w(i, j).size()).sum();
	}
	
	public int getInContactsNumber(int i) {
		return getPreds(i).stream().map(j -> w(j, i).size()).sum();
	}
	
	public double getAverageContactsNumber() {
		return nodesStream().map(i -> getContactsNumber(i)).average().getAsDouble();
	}
	
	public Graph getGraphAtTime(int t) {
		Graph res = new Graph(getName() + "[t=" + t + "]", size(), isDirected());
		nodesStream().forEach(i -> getTSuccsStream(i, t).forEach(j -> res.addEdge(i, j)));
		return res;
	}
	
	public double getAverageLatency(int i, int j) {
		if (latency == null)
			latency = Latency.construct(this);
		return latency.getAverageLatency(i, j);
	}
	
	public int getLatency(int i, int j, int t) {
		if (latency == null)
			latency = Latency.construct(this);
		return latency.getLatency(i, j, t);
	}
	
	public List<PathRec> getShortestPathsRecords(int i, int j) {
		if (latency == null)
			latency = Latency.construct(this);
		return latency.getShortestPathsRecords(i, j);
	}
	
	public PathRec getShortestPathsRecord(int i, int j, int t) {
		if (latency == null)
			latency = Latency.construct(this);
		return latency.getShortestPathsRecord(i, j, t);
	}
	
	@Override
	public void startRecordingHistory(){
		history = new Stack<>();
	}
	
	@Override
	public void stopRecordingHistory(){
		history = null;
	}
	
	@Override
	public void resetGraph(){
		if (history != null){
			while (!history.empty())
				history.pop().revert();
			recountMaxTimeAfterRemoval();
			notifyListenersReset();
		}
	}
	
	@Override
	public void revertChanges(int k){
		if (history != null){
			for (int i = 0; i < k; ++i)
				if (!history.empty())
					history.pop().revert();
			recountMaxTimeAfterRemoval();
		}
	}
	
	@Override
	public int historySize(){
		return history == null ? 0 : history.size();
	}
	
	@Override
	public void printDescription(boolean ifConnected) {
		super.printDescription(ifConnected);
		System.out.println("Contacts:\t" + contactsCount());
		System.out.println("Time moments:\t" + getT());
	}
	
	private abstract class TemporalChange {
		
		protected int i;
		protected int j;
		protected int begin;
		protected int end;
		
		public TemporalChange(int i, int j, int begin, int end) {
			this.i = i;
			this.j = j;
			this.begin = begin;
			this.end = end;
		}
		
		public abstract void revert();
	}
	
	private class TemporalAddition extends TemporalChange {
		
		public TemporalAddition(int i, int j, int begin, int end) {
			super(i, j, begin, end);
		}
		
		@Override
		public void revert() {
			performRemoveTEdge(i, j, begin, end, false);
			notifyListenersRemove(new Edge(i, j, isDirected()));
		}
	}
	
	private class TemporalRemoval extends TemporalChange {
		
		public TemporalRemoval(int i, int j, int begin, int end) {
			super(i, j, begin, end);
		}
		
		@Override
		public void revert() {
			performAddTEdge(i, j, begin, end);
			notifyListenersAdd(new Edge(i, j, isDirected()));
		}
	}
}