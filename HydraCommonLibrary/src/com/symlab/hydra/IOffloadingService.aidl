package com.symlab.hydra;

import com.symlab.hydra.lib.OffloadableMethod;
import com.symlab.hydra.IOffloadingCallback;

interface IOffloadingService {
	void addTaskToQueue(in byte[] offloadableMethod, in String apkPath);
	void registerCallback(in IOffloadingCallback offloadingCallback);
	void unregisterCallback();
	String getDeviceId();
	void startHelping();
	void stopHelping();
}