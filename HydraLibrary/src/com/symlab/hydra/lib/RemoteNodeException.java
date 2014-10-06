package com.symlab.hydra.lib;


public class RemoteNodeException extends Exception {

	private static final long serialVersionUID = 6723087067727386079L;

	public RemoteNodeException(String detailMessage, Throwable cause) {
		super(((detailMessage == null) ? "" : detailMessage), cause);
	}

}
