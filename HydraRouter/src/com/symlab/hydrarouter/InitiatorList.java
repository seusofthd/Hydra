package com.symlab.hydrarouter;

import java.util.ArrayList;

public class InitiatorList {

	private ArrayList<String> ID;
	private ArrayList<Integer> resource;

	public InitiatorList() {
		ID = new ArrayList<String>();
		resource = new ArrayList<Integer>();
	}

	public int numOfInitiators() {
		return ID.size();
	}

	public int hasResource(String id) {
		int i = ID.indexOf(id);
		if (i != -1)
			return resource.get(i);
		else
			return 0;
	}

	public void updateResource(String id, int r) {
		int i = ID.indexOf(id);
		if (i == -1) {
			ID.add(id);
			resource.add(r);
		} else {
			resource.set(i, resource.get(i) + r);
		}
	}
}
