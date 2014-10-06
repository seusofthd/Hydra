package com.symlab.hydrarouter;

import android.util.Log;

public class ConnectionLostException extends Exception {

	private static final long serialVersionUID = -3854324662675118935L;

	public ConnectionLostException(String detailMessage, Throwable cause) {
		super(detailMessage, cause);
		Log.e("ConnectionLostException", detailMessage);
	}

}
