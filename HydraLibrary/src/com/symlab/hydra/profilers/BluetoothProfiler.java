package com.symlab.hydra.profilers;

import java.io.Serializable;
import java.util.ArrayList;

public class BluetoothProfiler extends NetworkProfiler implements Serializable {
	// private static final String TAG = "BluetoothProfiler";

	private static final long serialVersionUID = -5970915753539019261L;
	private ArrayList<Long> startTimes;
	private ArrayList<Long> endTimes;

	public BluetoothProfiler() {
		startTimes = new ArrayList<Long>();
		endTimes = new ArrayList<Long>();
	}

	public void addStartTime(Long start) {
		startTimes.add(start);
	}

	public void addEndTime(Long end) {
		endTimes.add(end);
	}

	public ArrayList<Long> getStartTimes() {
		return startTimes;
	}

	public ArrayList<Long> getEndTimes() {
		return endTimes;
	}

	public int size() {
		return Math.min(startTimes.size(), endTimes.size());
	}

	public Long totalOnTime() {
		Long ret = 0L;
		for (int i = 0; i < size(); i++) {
			ret += endTimes.get(i) - startTimes.get(i);
		}
		return ret;
	}

	public synchronized void merge(BluetoothProfiler btp) {
		for (int i = 0; i < btp.size(); i++) {
			addStartTime(btp.getStartTimes().get(i));
			addEndTime(btp.getEndTimes().get(i));
		}
	}

	public void resetProfiler() {
		startTimes.clear();
		endTimes.clear();
	}

}
