package com.symlab.hydra.lib;

import android.util.Log;

public class OffloadingNetworkException extends Exception {

	private static final long serialVersionUID = 7616352574228263924L;

	public OffloadingNetworkException(String detailMessage, Throwable cause) {
		super(((detailMessage == null) ? "" : detailMessage), cause);
		Log.e("OffloadingNetworkException", detailMessage);
	}

}
