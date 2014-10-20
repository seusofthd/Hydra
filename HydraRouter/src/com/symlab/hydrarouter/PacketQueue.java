package com.symlab.hydrarouter;

import java.util.LinkedList;
import java.util.Observable;

import com.symlab.hydra.network.DataPackage;

public class PacketQueue extends Observable{

	private LinkedList<DataPackage> queue;
	private LinkedList<DataPackage> resultQueue;
	boolean isSmartphoneBusy = false;
	boolean isCloudBusy = false;
	

	public PacketQueue() {
		queue = new LinkedList<DataPackage>();
		resultQueue = new LinkedList<DataPackage>();
	}

	public void enqueue(DataPackage m) {
		synchronized (queue) {
			queue.addLast(m);
			setChanged();
			notifyObservers();
		}
	}

	public DataPackage dequeue() {
		synchronized (queue) {
			DataPackage method = queue.removeFirst();
			synchronized (resultQueue) {
				resultQueue.addLast(method);
			}
			return method;
		}
	}

	public int queueSize() {
		synchronized (queue) {
			return queue.size();
		}
	}

	public void clearQueue() {
		queue.clear();
	}

	public LinkedList<DataPackage> getQueue() {
		return queue;
	}

}
