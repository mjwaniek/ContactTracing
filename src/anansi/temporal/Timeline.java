package anansi.temporal;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import anansi.core.Coalition;

/**
 * Representation of a sequence of time intervals.
 * 
 * @author Marcin Waniek
 */
public class Timeline implements Iterable<Integer> {

	private Set<Integer> contacts;
	private int maxTime;
	
	public Timeline() {
		this.contacts = new HashSet<>();
		this.maxTime = Integer.MIN_VALUE;
	}
	
	public Timeline(int t) {
		this();
		add(t);
	}
	
	public Timeline(int begin, int end) {
		this();
		add(begin, end);
	}

	public int getMaxTime() {
		return maxTime;
	}
	
	public void add(int t) {
		add(t, t);
	}
	
	public void add(int begin, int end) {
		assert(begin <= end);
		for (int t = begin; t <= end; ++t)
			contacts.add(t);
		maxTime = Math.max(maxTime, end);
	}
	
	public void remove(int t) {
		remove(t, t);
	}
	
	public void remove(int begin, int end) {
		assert(begin <= end);
		for (int t = begin; t <= end; ++t)
			contacts.remove(t);
		if (begin <= maxTime && maxTime <= end)
			maxTime = contacts.isEmpty() ? Integer.MIN_VALUE : contacts.stream().mapToInt(t -> t).max().getAsInt();
	}
	
	public boolean contains(int t) {
		return contacts.contains(t);
	}
	
	public Integer first() {
		return contacts.isEmpty() ? null : contacts.stream().mapToInt(t -> t).min().getAsInt();
	}

	public Integer last() {
		return contacts.isEmpty() ? null : contacts.stream().mapToInt(t -> t).max().getAsInt();
	}
	
	public int size() {
		return contacts.size();
	}
	
	public boolean isEmpty() {
		return contacts.isEmpty();
	}
	
	public IntStream stream(){
		return contacts.stream().mapToInt(t -> t);
	}
	
	public Coalition asCoalition() {
		return new Coalition(contacts);
	}
	
	@Override
	public Iterator<Integer> iterator() {
		return contacts.iterator();
	}
	
	@Override
	public String toString() {
		return "[" + stream().sorted().mapToObj(t -> Integer.toString(t)).collect(Collectors.joining(",")) + "]";
	}
}
