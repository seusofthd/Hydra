package com.symlab.hydrarouter;

import java.util.concurrent.atomic.AtomicLong;

public class FIFOEntry<E extends Comparable<? super E>> implements Comparable<FIFOEntry<E>> {
	static final AtomicLong seq = new AtomicLong(0);
	final long seqNum;
	final E entry;

	public FIFOEntry(E entry) {
		seqNum = seq.getAndIncrement();
		this.entry = entry;
	}

	public E getEntry() {
		return entry;
	}

	@Override
	public int compareTo(FIFOEntry<E> another) {
		int res = entry.compareTo(another.entry);

		if (res == 0 && another.entry != this.entry) {
			res = (seqNum < another.seqNum ? -1 : 1);
		}
		return res;
	}

}
