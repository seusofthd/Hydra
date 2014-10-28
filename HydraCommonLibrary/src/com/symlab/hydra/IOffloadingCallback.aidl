package com.symlab.hydra;

import com.symlab.hydra.lib.OffloadableMethod;

interface IOffloadingCallback {
	void setResult(in byte[] offloadableMethodBytes);
}