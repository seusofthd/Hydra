package com.symlab.hydraapp;

import java.io.File;
import java.io.PrintStream;

import android.app.Activity;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Spinner;
import android.widget.TextView;

import com.symlab.hydra.HydraHelper;
import com.symlab.hydra.lib.MethodPackage;
import com.symlab.hydra.lib.OffloadableMethod;

public class MainActivity extends Activity {

	private TextView tv;
	private Spinner spinner;
	private HydraHelper hydraHelper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		tv = (TextView) findViewById(R.id.output);
		tv.setMovementMethod(new ScrollingMovementMethod());
		hydraHelper = new HydraHelper(this);
		spinner = (Spinner) findViewById(R.id.spinner1);
		spinner.setSelection(1);
		println("Hydra App");
	}

	public void bindService(View v) {
		hydraHelper.bindService();
		clear_screen();
		println("service is binded");
	}

	public void unbindService(View v) {
		hydraHelper.unbindService();
		clear_screen();
		println("service is UNbinded");
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

	public void execute_nqueen(View v, final Integer n) {
		println(n + "-Queens");
		for (int i = 0; i < 1; i++) {
			long time = System.currentTimeMillis();
			String classMethodName = Sorting.class.getName() + "#" + "solveNQueens" + "#" + 0 + "#" + 1;
			// dh.startProfiling(classMethodName);
			NQueens obj = new NQueens();
			Class<?>[] paramTypes = { int.class, int.class, int.class };
			Object[] paramValues = { n, 0, n };

			MethodPackage methodPackage = new MethodPackage((int) (Math.random() * 1000000000), obj, "solveNQueens", paramTypes, paramValues);
			String appName = getPackageName();
			String apkPath = null;
			try {
				apkPath = getPackageManager().getApplicationInfo(appName, 0).sourceDir;
			} catch (NameNotFoundException e1) {
				e1.printStackTrace();
			}
			OffloadableMethod offloadableMethod = new OffloadableMethod(MainActivity.this, appName, apkPath, methodPackage, Boolean.class);
			hydraHelper.postTask(offloadableMethod, apkPath);

			try {
				synchronized (offloadableMethod) {
					offloadableMethod.wait();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			time = System.currentTimeMillis() - time;
			System.out.println("total time = " + time / 1000f);
		}
	}

	public void execute_sort(View v) {
		println("qSort");
		for (int k = 0; k < 20; k++) {
			long time = System.currentTimeMillis();
			String classMethodName = Sorting.class.getName() + "#" + "qSort" + "#" + 0 + "#" + 1;
			// dh.startProfiling(classMethodName);
			Sorting obj = new Sorting();
			Class<?>[] paramTypes = {};
			Object[] paramValues = {};

			MethodPackage methodPackage = new MethodPackage((int) (Math.random() * 1000000000), obj, "qSort", paramTypes, paramValues);
			String appName = getPackageName();
			String apkPath = null;
			try {
				apkPath = getPackageManager().getApplicationInfo(appName, 0).sourceDir;
			} catch (NameNotFoundException e1) {
				e1.printStackTrace();
			}
			final OffloadableMethod offloadableMethod = new OffloadableMethod(MainActivity.this, appName, apkPath, methodPackage, void.class);
			hydraHelper.postTask(offloadableMethod, apkPath);

			try {
				synchronized (offloadableMethod) {
					offloadableMethod.wait();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			time = System.currentTimeMillis() - time;
			System.out.println("total time = " + time / 1000f);
		}
	}

	public void execute_sudoku(View v) {
		println("Sudoku");
		for (int k = 0; k < 20; k++) {
			long time = System.currentTimeMillis();
			String classMethodName = Sorting.class.getName() + "#" + "hasSolution" + "#" + 0 + "#" + 1;
			// dh.startProfiling(classMethodName);
			Sudoku obj = new Sudoku();
			Class<?>[] paramTypes = {};
			Object[] paramValues = {};

			MethodPackage methodPackage = new MethodPackage((int) (Math.random() * 1000000000), obj, "hasSolution", paramTypes, paramValues);
			String appName = getPackageName();
			String apkPath = null;
			try {
				apkPath = getPackageManager().getApplicationInfo(appName, 0).sourceDir;
			} catch (NameNotFoundException e1) {
				e1.printStackTrace();
			}
			OffloadableMethod offloadableMethod = new OffloadableMethod(MainActivity.this, appName, apkPath, methodPackage, Boolean.class);
			hydraHelper.postTask(offloadableMethod, apkPath);

			try {
				synchronized (offloadableMethod) {
					offloadableMethod.wait();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			time = System.currentTimeMillis() - time;
			System.out.println("total time = " + time / 1000f);
		}

	}

	public void execute_face(View v, final int n) {
		println("FaceDetection " + n);
		for (int k = 0; k < 20; k++) {
			long time = System.currentTimeMillis();
			String classMethodName = Sorting.class.getName() + "#" + "detect_faces" + "#" + 0 + "#" + 1;
			// dh.startProfiling(classMethodName);
			FaceDetection obj = new FaceDetection();
			Class<?>[] paramTypes = { int.class, int.class };
			Object[] paramValues = { 20, n };
			MethodPackage methodPackage = new MethodPackage((int) (Math.random() * 1000000000), obj, "detect_faces", paramTypes, paramValues);
			String appName = getPackageName();
			String apkPath = null;
			try {
				apkPath = getPackageManager().getApplicationInfo(appName, 0).sourceDir;
			} catch (NameNotFoundException e1) {
				e1.printStackTrace();
			}
			OffloadableMethod offloadableMethod = new OffloadableMethod(MainActivity.this, appName, apkPath, methodPackage, Integer.class);
			hydraHelper.postTask(offloadableMethod, apkPath);

			try {
				synchronized (offloadableMethod) {
					offloadableMethod.wait();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			time = System.currentTimeMillis() - time;
			System.out.println("total time = " + time / 1000f);
		}
		
	}
	public void println(final String s) {
		new Thread() {
			public void run() {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						tv.setText(s + "\n" + tv.getText());
					}
				});
			};
		}.start();
	}

	public void clear_screen() {
		new Thread() {
			public void run() {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						tv.setText("");
					}
				});
			};
		}.start();
	}

}
