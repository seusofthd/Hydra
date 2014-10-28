package com.symlab.hydra.lib;

import java.util.LinkedList;
import java.util.Observable;

import com.symlab.hydra.OffloadingService;
import com.symlab.hydra.network.DataPackage;

public class TaskQueue extends Observable {
	
	OffloadingService context;

	private LinkedList<OffloadableMethod> queue;
	private LinkedList<OffloadableMethod> waitingQueue;
	private LinkedList<OffloadableMethod> resultQueue;

	public TaskQueue(OffloadingService context) {
		this.context = context;
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

	public synchronized void setResult(ResultContainer resultContainer) {
		for (OffloadableMethod om : resultQueue) {
			if (om.methodPackage.id == resultContainer.id) {
				om.result = resultContainer.result;
				om.execDuration = resultContainer.pureExecutionDuration;
				context.setResults(om);
			}
		}
	}
}
