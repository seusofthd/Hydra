package com.symlab.hydra.experiment;

import android.util.Log;

public class CpuJammer {
	private Thread t1;
	// private Thread t2;
	private Object lock = new Object();
	private boolean paused = true;

	private static CpuJammer cj = null;

	private CpuJammer() {
		t1 = new Thread(r);
		t1.setPriority(Thread.MAX_PRIORITY);
		t1.start();
		// t2 = new Thread(r);
		// t2.setPriority(Thread.MAX_PRIORITY);
	}

	public static CpuJammer newInstance() {
		if (cj == null) {
			cj = new CpuJammer();
		}
		return cj;
	}

	public void jamming() {
		synchronized (lock) {
			paused = false;
			lock.notifyAll();
		}
		// cj.t2.start();
		Log.e("CpuJammer", "Start jamming");
	}

	public void stopJam() {
		synchronized (lock) {
			paused = true;
		}
		// cj.t2.interrupt();
		Log.e("CpuJammer", "Stop jamming");
	}

	private Runnable r = new Runnable() {

		@Override
		public void run() {
			while (!Thread.currentThread().isInterrupted()) {
				synchronized (lock) {
					while (paused) {
						try {
							lock.wait();
						} catch (InterruptedException e) {
						}
					}
				}
				double pi = 1f;
				for (int i = 0; i < 100; i++)
					pi *= Math.PI;
			}
		}

	};
}
