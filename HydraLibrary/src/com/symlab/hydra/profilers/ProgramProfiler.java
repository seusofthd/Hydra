package com.symlab.hydra.profilers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import android.os.SystemClock;

public class ProgramProfiler {
	public String methodName;
	public long execTime;

	// public int instructionCount;
	// public int methodInvocationCount;
	private long mStartTime;

	// private Debug.InstructionCount mICount;

	public ProgramProfiler() {
		methodName = "";

	}

	public ProgramProfiler(String mName) {
		methodName = mName;

	}
	
	public void startExecutionInfoTracking() {
		mStartTime = SystemClock.elapsedRealtimeNanos();

		// mICount.resetAndStart();
	}

	public void stopAndCollectExecutionInfoTracking() {
		// mICount.collect();
		// instructionCount = mICount.globalTotal();
		// methodInvocationCount = mICount.globalMethodInvocations();

		execTime = SystemClock.elapsedRealtimeNanos() - mStartTime;

	}

}
