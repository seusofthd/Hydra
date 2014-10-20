package com.symlab.hydra.lib;

import java.util.LinkedList;
import java.util.Observable;

import com.symlab.hydra.network.DataPackage;

public class TaskQueue extends Observable {

	private LinkedList<OffloadableMethod> queue;
	private LinkedList<OffloadableMethod> waitingQueue;
	private LinkedList<OffloadableMethod> resultQueue;

	public TaskQueue() {
		queue = new LinkedList<OffloadableMethod>();
		waitingQueue = new LinkedList<OffloadableMethod>();
		resultQueue = new LinkedList<OffloadableMethod>();
	}

	public synchronized void enqueue(OffloadableMethod m) {
		synchronized (queue) {
			queue.addLast(m);
			setChanged();
			notifyObservers();
		}
	}

	public synchronized OffloadableMethod dequeue() {
		synchronized (queue) {
			OffloadableMethod method = queue.removeFirst();
			synchronized (waitingQueue) {
				waitingQueue.addLast(method);
			}
			return method;
		}
	}

	public synchronized OffloadableMethod dequeue(int id) {
		synchronized (waitingQueue) {
			for (OffloadableMethod om : waitingQueue) {
				if (om.methodPackage.id == id) {
					waitingQueue.remove(om);
					synchronized (resultQueue) {
						resultQueue.addLast(om);
						return om;
					}
				}
			}
		}
		return null;
	}

	public synchronized OffloadableMethod getOffloadableMethod(int id) {
		synchronized (waitingQueue) {
			for (OffloadableMethod om : waitingQueue) {
				if (om.methodPackage.id == id) {
					return om;
				}
			}
		}
		return null;
	}

	public synchronized int queueSize() {
		synchronized (queue) {
			return queue.size();
		}
	}

	public synchronized void setResult(DataPackage dataPackage) {
		ResultContainer resultContainer = (ResultContainer) dataPackage.deserialize();
		for (OffloadableMethod offloadableMethod : resultQueue) {
			if (offloadableMethod.methodPackage.id == resultContainer.id) {
				offloadableMethod.result = resultContainer.result;
				offloadableMethod.dataPackage = dataPackage;
				if (offloadableMethod.dataPackage.finish) {

					synchronized (lock) {
						lock.notifyAll();
					}
				}
			}
		}
	}

	public static Object lock = new Object();
}
