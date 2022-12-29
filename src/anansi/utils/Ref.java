package anansi.utils;

/**
 * A reference to an object, allowing to use it in lambda-expressions (omitting effective final requirement).
 * 
 * @author Marcin Waniek
 */
public class Ref<T> {
	
	private T t;

	public Ref() {
		this.t = null;
	}
	
	public Ref(T t) {
		this.t = t;
	}

	public T get() {
		return t;
	}

	public boolean set(T t) {
		boolean res = !same(t);
		this.t = t;
		return res;
	}
	
	public T getAndSet(T newT) {
		T oldT = t;
		t = newT;
		return oldT;
	}
	
	private boolean same(Object t) {
		return (this.t == null && t == null) || (this.t != null && this.t.equals(t));
	}
	
	@Override
	public String toString() {
		return "<" + t + ">";
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		return same(((Ref<?>) obj).t);
	}

	@Override
	public int hashCode() {
		return t == null ? 0 : t.hashCode();
	}
}
