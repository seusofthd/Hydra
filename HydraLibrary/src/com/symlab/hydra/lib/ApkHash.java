package com.symlab.hydra.lib;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ApkHash {

	public static String hash(byte[] data) {
		String ret = "";
		try {
			MessageDigest digester = MessageDigest.getInstance("MD5");
			digester.update(data);
			ret = toHexString(digester.digest());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return ret;
	}

	private static String toHexString(byte[] ba) {
		String ret = "";
		for (int i = 0; i < ba.length; i++) {
			ret += Integer.toHexString((ba[i] >> 4) & 15);
			ret += Integer.toHexString(ba[i] & 15);
		}
		return ret;
	}

}
