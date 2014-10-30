package com.symlab.hydra.experiment;


public class CpuJammer {
	
	JamTask[] t;
	int num;

	public CpuJammer(int n) {
		num = n;
		t = new JamTask[n];
	}

	public void jam() {
		
		for (int i = 0; i < num; i++) {
			t[i] = new JamTask();
			t[i].start();
		}
	}
	
	public void stopJam() {
		for (int i = 0; i < num; i++) {
			t[i].stopJam();
		}
	}
	
	class JamTask extends Thread {
		
		private Object lock = new Object();
		private boolean paused = false;

		@Override
		public void run() {
			while (!paused) {
				double pi = 1f;
				for (int i = 0; i < 100; i++)
					pi *= Math.PI;
			}
		}

		public void stopJam() {
			synchronized (lock) {
				paused = true;
			}
		}
		
	};
}