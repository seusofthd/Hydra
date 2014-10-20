package com.symlab.hydra.lib;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

import dalvik.system.DexClassLoader;

/**
 * Custom object input stream to also deal with dynamically loaded classes. The
 * classes can be retrieved from Android Dex files, provided in Apk (android
 * application) files.
 * 
 * @author Andrius
 * 
 */
public class DynamicObjectInputStream extends ObjectInputStream {

	private static String TAG = "DynamicObjectInputStream";

	private ClassLoader mCurrent = null;// ClassLoader.getSystemClassLoader();
	private DexClassLoader mCurrentDexLoader = null;

	public DynamicObjectInputStream(InputStream in, ClassLoader cl) throws IOException {
		super(in);
		mCurrent = cl;
	}

	public DynamicObjectInputStream(InputStream in) throws IOException {
		super(in);
		mCurrent = this.getClass().getClassLoader();
	}

	/**
	 * Override the method resolving a class to also look into the constructed
	 * DexClassLoader
	 */
	@Override
	protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
		try {
			try {
				// Log.d(TAG, "Loading: " + desc.getName());
				// Log.i(TAG, "Using default loader");
				// return super.resolveClass(desc);
				return mCurrent.loadClass(desc.getName());
			} catch (ClassNotFoundException e) {
				// Log.wtf(TAG, "Failed, Use dex");
				// Log.d(TAG, "Use Dex Loading: " + desc.getName());
				return mCurrentDexLoader.loadClass(desc.getName());
			}
		} catch (ClassNotFoundException e) {
			// Log.wtf(TAG, "Use dex failed");
			return super.resolveClass(desc);
			// return mCurrent.loadClass(desc.getName());
		} catch (NullPointerException e) { // Thrown when currentDexLoader is
			// not yet set up
			// Log.wtf(TAG, "Didn't load dex file, Use dex failed");
			return super.resolveClass(desc);
		}

	}

	/**
	 * Add a Dex file to the Class Loader for dynamic class loading for clients
	 * 
	 * @param apkFile
	 *            the apk package
	 */
	public void addDex(String apkFile, String optDir) {
		if (mCurrentDexLoader == null)
			mCurrentDexLoader = new DexClassLoader(apkFile, optDir, null, mCurrent);
		else
			mCurrentDexLoader = new DexClassLoader(apkFile, optDir, null, mCurrentDexLoader);
	}

	/**
	 * Add a Dex file to the Class Loader for dynamic class loading for clients
	 * 
	 * @param apkFile
	 *            the apk package
	 */
	public void addDex(final File apkFile) {
		if (mCurrentDexLoader == null)
			mCurrentDexLoader = new DexClassLoader(apkFile.getAbsolutePath(), apkFile.getParentFile().getAbsolutePath(), null, mCurrent);
		else
			mCurrentDexLoader = new DexClassLoader(apkFile.getAbsolutePath(), apkFile.getParentFile().getAbsolutePath(), null, mCurrentDexLoader);

	}

}
