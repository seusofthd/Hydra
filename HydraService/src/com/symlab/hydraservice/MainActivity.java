package com.symlab.hydraservice;

import java.io.File;
import java.io.PrintStream;
import java.io.Serializable;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.TextView;

import com.symlab.hydra.HydraHelper;
import com.symlab.hydra.OffloadingService;
import com.symlab.hydra.lib.MethodPackage;
import com.symlab.hydra.lib.OffloadableMethod;
import com.symlab.hydra.network.Msg;
import com.symlab.hydra.status.DeviceStatus;
import com.symlab.hydra.status.Status;

import dalvik.system.DexClassLoader;

public class MainActivity extends Activity {

	private transient HydraHelper hydraHelper;
	private TextView tv;
	private Spinner spinner;
	private Spinner spinner2;
	CheckBox cb;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		tv = (TextView) findViewById(R.id.output);
		tv.setMovementMethod(new ScrollingMovementMethod());
		cb = (CheckBox) findViewById(R.id.helping);
		cb.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					hydraHelper.startHelping();
				} else {
					hydraHelper.stopHelping();
				}

			}

		});

		spinner2 = (Spinner) findViewById(R.id.spinner2);
		spinner2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				// OffloadingService offloadingService =
				// (OffloadingService)hydraHelper.service;
				// System.out.println("changed " + offloadingService);
				// if (offloadingService != null) {
				//
				// Msg selected = Msg.LOCAL;
				// switch (position) {
				// case 0:
				// selected = Msg.LOCAL;
				// case 1:
				// selected = Msg.CLOUD;
				// case 2:
				// selected = Msg.SMARTPHONE;
				// case 3:
				// selected = Msg.GREEDY;
				// }
				// offloadingService.offloadingMethod = selected;
				// }
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// TODO Auto-generated method stub

			}

		});

		hydraHelper = new HydraHelper(this);

		spinner = (Spinner) findViewById(R.id.spinner1);
		spinner.setSelection(1);
		spinner2 = (Spinner) findViewById(R.id.spinner2);
		intent = new Intent(this, OffloadingService.class);
		println("Hydra");
	}

	Intent intent;

	public void startService(View v) {
		if (!hydraHelper.serviceIsStart) {
			hydraHelper.startService(intent);
			println("service is started");
			if (!hydraHelper.mIsBound) {
				hydraHelper.bindService();
				println("service is binded");
			} else {
				println("service is already binded");
			}
		} else {
			println("service is already started");

		}
	}

	public void stopService(View v) {
		if (hydraHelper.serviceIsStart) {
			hydraHelper.stopHelping();
			cb.setSelected(false);
			hydraHelper.stopService(intent);
			println("service is stopped");
			if (hydraHelper.mIsBound) {
				hydraHelper.unbindService();
				println("service is unbinded");
			} else {
				println("service is already unbinded");
			}
		} else {
			println("service is already stopped");
		}
	}

	class OCRthread extends Thread implements Serializable {
	}

	PrintStream printStream;

	public void execute(final View v) {
		new Thread() {
			@Override
			public void run() {
				try {
					printStream = new PrintStream(new File("/sdcard/res.csv"));
					switch (spinner.getSelectedItemPosition()) {
					case 0:
						execute_nqueen(v, 8);
						break;
					case 1:
						execute_nqueen(v, 7);
						break;
					case 2:
						execute_sort(v);
						break;
					case 3:
						execute_sudoku(v);
						break;
					case 4:
						execute_face(v, 1);
						break;
					case 5:
						execute_face(v, 5);
						break;
					default:
						break;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

	public void execute_nqueen(View v, final int n) {
		println(n + "-Queens");
		for (int i = 0; i < 10; i++) {
			Object obj = null;
			String apkPath = "/sdcard/Hydra/HydraApp.apk";
			try {
				DexClassLoader dexClassLoader = new DexClassLoader(apkPath, getDir("dex", Context.MODE_PRIVATE).getAbsolutePath(), null, getClassLoader());
				Class<?> classToLoad = dexClassLoader.loadClass("com.symlab.hydraapp.NQueens");
				obj = classToLoad.newInstance();
				final String classMethodName = classToLoad.getName() + "#" + "solveNQueens" + "#" + 0 + "#" + 1;
				// dh.startProfiling(classMethodName);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			final Boolean result;
			result = true;
			final Class<?>[] paramTypes = { int.class, int.class, int.class };
			Object[] paramValues = { n, 0, n };
			final MethodPackage methodPackage = new MethodPackage((int) (Math.random() * 100000), obj, "solveNQueens", paramTypes, paramValues);
			final OffloadableMethod offloadableMethod = new OffloadableMethod(hydraHelper.getPackageName(), apkPath, methodPackage, Boolean.class);
			hydraHelper.postTask(offloadableMethod, apkPath);
			try {
				synchronized (offloadableMethod) {
					offloadableMethod.wait();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			println("time = " + offloadableMethod.execDuration / 1000f);
		}
	}

	public void execute_sort(View v) {
		println("qSort");
		for (int k = 0; k < 20; k++) {
			Object obj = null;
			String apkPath = "/sdcard/Hydra/HydraApp.apk";
			try {
				DexClassLoader dexClassLoader = new DexClassLoader(apkPath, getDir("dex", Context.MODE_PRIVATE).getAbsolutePath(), null, getClassLoader());
				Class<?> classToLoad = dexClassLoader.loadClass("com.symlab.hydraapp.Sorting");
				obj = classToLoad.newInstance();
				final String classMethodName = classToLoad.getName() + "#" + "qSort" + "#" + 0 + "#" + 1;
				// dh.startProfiling(classMethodName);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			final Boolean result;
			result = true;
			final Class<?>[] paramTypes = {};
			Object[] paramValues = {};
			final MethodPackage methodPackage = new MethodPackage((int) (Math.random() * 100000), obj, "qSort", paramTypes, paramValues);
			final OffloadableMethod offloadableMethod = new OffloadableMethod(hydraHelper.getPackageName(), apkPath, methodPackage, void.class);
			hydraHelper.postTask(offloadableMethod, apkPath);
			try {
				synchronized (offloadableMethod) {
					offloadableMethod.wait();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			println("time = " + offloadableMethod.execDuration / 1000f);
		}
	}

	public void execute_sudoku(View v) {
		println("Sudoku");
		for (int k = 0; k < 20; k++) {
			Object obj = null;
			String apkPath = "/sdcard/Hydra/HydraApp.apk";
			try {
				DexClassLoader dexClassLoader = new DexClassLoader(apkPath, getDir("dex", Context.MODE_PRIVATE).getAbsolutePath(), null, getClassLoader());
				Class<?> classToLoad = dexClassLoader.loadClass("com.symlab.hydraapp.Sudoku");
				obj = classToLoad.newInstance();
				final String classMethodName = classToLoad.getName() + "#" + "hasSolution" + "#" + 0 + "#" + 1;
				// dh.startProfiling(classMethodName);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			final Boolean result;
			result = true;
			final Class<?>[] paramTypes = {};
			Object[] paramValues = {};
			final MethodPackage methodPackage = new MethodPackage((int) (Math.random() * 100000), obj, "hasSolution", paramTypes, paramValues);
			final OffloadableMethod offloadableMethod = new OffloadableMethod(hydraHelper.getPackageName(), apkPath, methodPackage, Boolean.class);
			hydraHelper.postTask(offloadableMethod, apkPath);
			try {
				synchronized (offloadableMethod) {
					offloadableMethod.wait();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			println("time = " + offloadableMethod.execDuration / 1000f);
		}
	}

	public void execute_face(View v, final int n) {
		println("FaceDetection " + n);
		for (int k = 0; k < 20; k++) {
			Object obj = null;
			String apkPath = "/sdcard/Hydra/HydraApp.apk";
			try {
				DexClassLoader dexClassLoader = new DexClassLoader(apkPath, getDir("dex", Context.MODE_PRIVATE).getAbsolutePath(), null, getClassLoader());
				Class<?> classToLoad = dexClassLoader.loadClass("com.symlab.hydraapp.FaceDetection");
				obj = classToLoad.newInstance();
				final String classMethodName = classToLoad.getName() + "#" + "detect_faces" + "#" + 0 + "#" + 1;
				// dh.startProfiling(classMethodName);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			final Boolean result;
			result = true;
			final Class<?>[] paramTypes = { int.class, int.class };
			Object[] paramValues = { 20, n };
			final MethodPackage methodPackage = new MethodPackage((int) (Math.random() * 100000), obj, "detect_faces", paramTypes, paramValues);
			final OffloadableMethod offloadableMethod = new OffloadableMethod(hydraHelper.getPackageName(), apkPath, methodPackage, Integer.class);
			hydraHelper.postTask(offloadableMethod, apkPath);
			try {
				synchronized (offloadableMethod) {
					offloadableMethod.wait();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			println("time = " + offloadableMethod.execDuration / 1000f);
		}
	}

	private Msg getOffloadingMethod() {
		switch (spinner2.getSelectedItemPosition()) {
		case 0:
			return Msg.LOCAL;
		case 1:
			return Msg.CLOUD;
		case 2:
			return Msg.SMARTPHONE;
		case 3:
			return Msg.GREEDY;
		}
		return null;
	}

	public void println(final String s) {
		new Thread() {
			@Override
			public void run() {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						tv.setText(s + "\n" + tv.getText());
					}
				});
			}
		}.start();

	}

	public void clear_screen() {
		new Thread() {
			@Override
			public void run() {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						tv.setText("");
					}
				});
			}
		}.start();
	}

}
