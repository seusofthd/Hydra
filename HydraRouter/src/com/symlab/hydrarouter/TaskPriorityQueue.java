package com.symlab.hydrarouter;

import java.util.concurrent.PriorityBlockingQueue;

public class TaskPriorityQueue {

	static final int PRIORITY_NORMAL = 0;
	static final int PRIORITY_HIGH = 1;
	static final int PRIORITY_RESUBMIT = 2;

	static final int STATE_PENDING = 0;
	static final int STATE_ASSIGNED = 1;
	static final int STATE_ = 2;

	private PriorityBlockingQueue<FIFOEntry<TaskEntry>> taskQueue;

	public TaskPriorityQueue() {
		taskQueue = new PriorityBlockingQueue<FIFOEntry<TaskEntry>>();
	}

	public void add(String taskId, int priority) {
		taskQueue.put(new FIFOEntry<TaskEntry>(new TaskEntry(taskId, priority, 0)));
	}

	public String getTask() {
		TaskEntry te = taskQueue.poll().entry;
		if (te != null)
			return te.taskId;
		else
			return "";
	}

	class TaskEntry implements Comparable<TaskEntry> {

		public String taskId;
		public int priority;
		public int state;

		public TaskEntry(String id, int p, int s) {
			taskId = id;
			priority = p;
			state = s;
		}

		@Override
		public int compareTo(TaskEntry another) {
			// return this.priority-another.priority;
			return another.priority - this.priority;
		}
	}

}
