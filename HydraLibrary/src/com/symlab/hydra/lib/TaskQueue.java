package com.symlab.hydra.lib;

import java.util.LinkedList;

import com.symlab.hydra.network.DataPackage;

public class TaskQueue {

	private LinkedList<OffloadableMethod> queue;
	private LinkedList<OffloadableMethod> resultQueue;

	public TaskQueue() {
		queue = new LinkedList<OffloadableMethod>();
		resultQueue = new LinkedList<OffloadableMethod>();
	}

	public void enqueue(OffloadableMethod m) {
		synchronized (queue) {
			queue.addLast(m);
		}
	}

	public OffloadableMethod dequeue() {
		synchronized (queue) {
			OffloadableMethod method = queue.removeFirst();
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

	public LinkedList<OffloadableMethod> getQueue() {
		return queue;
	}

	public void setResult(DataPackage dataPackage) {
		ResultContainer resultContainer = (ResultContainer) dataPackage.deserialize();
		for (OffloadableMethod offloadableMethod : resultQueue) {
			if (offloadableMethod.id==resultContainer.id) {
				offloadableMethod.result = resultContainer.result;
				offloadableMethod.dataPackage = dataPackage;
				if (offloadableMethod.dataPackage.finish) {
					synchronized (offloadableMethod) {
						offloadableMethod.notifyAll();
					}
				}
			}
		}
	}
}
