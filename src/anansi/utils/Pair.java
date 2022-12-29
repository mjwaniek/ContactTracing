package anansi.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A generic pair of elements of the same type.
 * 
 * @author Marcin Waniek
 */
public class Pair<T1,T2> {

	private T1 a;
	private T2 b;
	
	public Pair(T1 a, T2 b) {
		this.a = a;
		this.b = b;
	}

	public T1 a() {
		return a;
	}

	public T2 b() {
		return b;
	}
	
	public static <T> Stream<Pair<T,T>> allUnordered(Iterable<T> it){
		List<T> elems = new ArrayList<>();
		it.forEach(t -> elems.add(t));
		Iterator<Pair<T,T>> iter = new Iterator<Pair<T,T>>() {
			private int i = 0;
			private int j = 1;

			@Override
			public boolean hasNext() {
				return i < elems.size() - 1;
			}

			@Override
			public Pair<T,T> next() {
				Pair<T,T> res = new Pair<T,T>(elems.get(i), elems.get(j));
				j = (j + 1) % elems.size();
				if (j == 0){
					++i;
					j = i + 1;
				}
				return res;
			}
		};
		return StreamSupport.stream(((Iterable<Pair<T,T>>)() -> iter).spliterator(), false);
	}
	
	public static <T1,T2> Stream<Pair<T1,T2>> allPairs(Iterable<T1> it1, Iterable<T2> it2){
		Iterator<Pair<T1,T2>> iter = new Iterator<Pair<T1,T2>>() {
			private Iterator<T1> i1 = it1.iterator();
			private Iterator<T2> i2 = it2.iterator();
			private T1 t1 = i1.next();

			@Override
			public boolean hasNext() {
				return i1 != null;
			}

			@Override
			public Pair<T1,T2> next() {
				Pair<T1,T2> res = new Pair<T1,T2>(t1, i2.next());
				if (!i2.hasNext()){
					if (i1.hasNext()){
						t1 = i1.next();
						i2 = it2.iterator();
					} else
						i1 = null;
				}
				return res;
			}
		};
		return StreamSupport.stream(((Iterable<Pair<T1,T2>>)() -> iter).spliterator(), false);
	}
	
	@Override
	public String toString() {
		return "(" + a + "," + b + ")";
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		Pair<T1,T2> p = (Pair<T1,T2>) o;
		return ((a == null && p.a == null) || (a != null && a.equals(p.a)))
				&& ((b == null && p.b == null) || (b != null && b.equals(p.b)));
	}

	@Override
	public int hashCode() {
		return 31 * (31 + ((a == null) ? 0 : a.hashCode())) + ((b == null) ? 0 : b.hashCode());
	}
}
