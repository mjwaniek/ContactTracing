package anansi.core;

/**
 * Object that is notified about every change in graph's structure.
 * 
 * @author Marcin Waniek
 */
public interface GraphChangeListener {

	public void notifyAdd(Graph g, Edge e);
	public void notifyRemove(Graph g, Edge e);
	public default void notifyOther(Graph g, Edge e) {}
	public void notifyReset(Graph g);
}
