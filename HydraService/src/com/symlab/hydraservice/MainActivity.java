package com.symlab.hydraservice;

import java.io.File;
import java.io.PrintStream;
import java.io.Serializable;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.TextView;

import com.symlab.hydra.HydraHelper;
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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		tv = (TextView) findViewById(R.id.output);
		tv.setMovementMethod(new ScrollingMovementMethod());
		CheckBox cb;
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
		hydraHelper = new HydraHelper(this);

		spinner = (Spinner) findViewById(R.id.spinner1);
		spinner.setSelection(1);
		spinner2 = (Spinner) findViewById(R.id.spinner2);
		println("Hydra");
	}

	public void startService(View v) {
		hydraHelper.initializeOHelper();
		println("service is started");
	}

	public void stopService(View v) {
		hydraHelper.stopHelping();
		hydraHelper.tearDownOHelper();
		hydraHelper.stopOffloadingService();
	}

	public void execute_ocr(View v) {
		// final String classMethodName = CameraListener.class.getName() + "#" +
		// "ocrCall" + "#1";
		// dh.startProfiling(classMethodName);

		System.out.println("*** Task submitted ***");

		// Intent intent = new Intent(this, AndroidOCR.class);
		// startActivity(intent);
		ocrThread.start();
	}

	OCRthread ocrThread = new OCRthread();

	class OCRthread extends Thread implements Serializable {
		public void run() {
			while (true) {
				// try {
				// Thread.sleep(2000);
				// final Class<String> returnTypes = String.class;
				// final Class<?>[] paramTypes = { SerialBitmap.class };
				// Object[] paramValues = { AndroidOCR.sbitMap };
				// final ResultTicket rt;
				// final MethodPackage methodPackage = new MethodPackage((int)
				// (Math.random() * 100000), AndroidOCR.cameraListener,
				// "ocrCall", paramTypes, paramValues);
				// final OffloadableMethod offloadableMethod = new
				// OffloadableMethod(MainActivity.this, apkPath,
				// methodPackage, returnTypes);
				// offloadableMethod.dataPackage = null;
				// if (AndroidOCR.sbitMap != null && AndroidOCR.sbitMap.bitmap
				// != null) {
				// dh.postTask(offloadableMethod);
				// }
				//
				// new Thread() {
				// public void run() {
				// try {
				// synchronized (offloadableMethod) {
				// offloadableMethod.wait();
				// }
				// AndroidOCR.ocrText = (String) (offloadableMethod.result);
				// System.out.println("#RES " + methodPackage.id + " | " +
				// AndroidOCR.ocrText);
				// System.out.println("#Total RTT = " +
				// (offloadableMethod.dataPackage.rttDeviceToVM) / 1000f);
				// System.out.println("#RTT (Router to VM/offloadee) = " +
				// (offloadableMethod.dataPackage.rttRouterToVM) / 1000f);
				// System.out.println("#RTT (Manager to VM) = " +
				// (offloadableMethod.dataPackage.rttManagerToVM) / 1000f);
				// System.out.println("#Pure Exec Time = " +
				// (offloadableMethod.dataPackage.pureExecTime) / 1000f);
				// runOnUiThread(new Runnable() {
				// public void run() {
				// println("result is ready for task " + methodPackage.id);
				// println("Total RTT = " +
				// (offloadableMethod.dataPackage.rttDeviceToVM) / 1000f);
				// println("RTT (Router to VM/offloadee) = " +
				// (offloadableMethod.dataPackage.rttRouterToVM) / 1000f);
				// println("RTT (Manager to VM) = " +
				// (offloadableMethod.dataPackage.rttManagerToVM) / 1000f);
				// println("Pure Exec Time = " +
				// (offloadableMethod.dataPackage.pureExecTime) / 1000f);
				//
				// }
				// });
				//
				// } catch (InterruptedException e) {
				// e.printStackTrace();
				// }
				// };
				// }.start();
				//
				// } catch (Exception e) {
				// e.printStackTrace();
				// }
				//
			}
		}
	}

	public void execute2(final View v) throws Exception {
		final PrintStream batteryPrintStream = new PrintStream(new File("/sdcard/b.csv"));
		new Thread() {
			@Override
			public void run() {
				int sec = 0;
				while (true) {
					Status status = DeviceStatus.newInstance(MainActivity.this).readStatus();
					if (sec % 10 == 0) {
						synchronized (batteryPrintStream) {
							batteryPrintStream.println(sec + "," + status.batteryPercentage);
						}
						println(sec + "," + status.batteryPercentage);
						System.out.println("Battery = " + status.batteryPercentage + " %");
					}
					try {
						sec++;
						sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();
		new Thread() {
			public void run() {
				int count = 1;
				while (true) {
					execute_nqueen(v, 7);
					execute_sort(v);
					execute_sudoku(v);
					execute_face(v, 1);
					execute_nqueen(v, 8);
					execute_face(v, 5);
					synchronized (batteryPrintStream) {
						batteryPrintStream.println("round," + count++);
					}
				}
			};
		}.start();

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
			String apkPath = Environment.getExternalStorageDirectory().getPath() + "/Hydra/hydraApp.apk";
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
			final OffloadableMethod offloadableMethod = new OffloadableMethod(MainActivity.this, apkPath, methodPackage, Boolean.class, getOffloadingMethod());
			hydraHelper.postTask(offloadableMethod);
			try {
				synchronized (offloadableMethod) {
					offloadableMethod.wait();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			println(offloadableMethod.dataPackage.dest + " \ttime = " + (offloadableMethod.execDuration) / 1000f);
			printStream.println(offloadableMethod.dataPackage.dest + "," + (offloadableMethod.execDuration) / 1000f);
		}
	}

	public void execute_sort(View v) {
		println("qSort");
		for (int k = 0; k < 20; k++) {
			Object obj = null;
			String apkPath = Environment.getExternalStorageDirectory().getPath() + "/Hydra/hydraApp.apk";
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
			final OffloadableMethod offloadableMethod = new OffloadableMethod(MainActivity.this, apkPath, methodPackage, void.class, getOffloadingMethod());
			hydraHelper.postTask(offloadableMethod);
			try {
				synchronized (offloadableMethod) {
					offloadableMethod.wait();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			println(offloadableMethod.dataPackage.dest + " \ttime = " + (offloadableMethod.execDuration) / 1000f);
			printStream.println(offloadableMethod.dataPackage.dest + "," + (offloadableMethod.execDuration) / 1000f);
		}
	}

	public void execute_sudoku(View v) {
		println("Sudoku");
		for (int k = 0; k < 20; k++) {
			Object obj = null;
			String apkPath = Environment.getExternalStorageDirectory().getPath() + "/Hydra/hydraApp.apk";
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
			final OffloadableMethod offloadableMethod = new OffloadableMethod(MainActivity.this, apkPath, methodPackage, Boolean.class, getOffloadingMethod());
			hydraHelper.postTask(offloadableMethod);
			try {
				synchronized (offloadableMethod) {
					offloadableMethod.wait();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			println(offloadableMethod.dataPackage.dest + " \ttime = " + (offloadableMethod.execDuration) / 1000f);
			printStream.println(offloadableMethod.dataPackage.dest + "," + (offloadableMethod.execDuration) / 1000f);
		}
	}

	public void execute_face(View v, final int n) {
		println("FaceDetection " + n);
		for (int k = 0; k < 20; k++) {
			Object obj = null;
			String apkPath = Environment.getExternalStorageDirectory().getPath() + "/Hydra/hydraApp.apk";
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
			final OffloadableMethod offloadableMethod = new OffloadableMethod(MainActivity.this, apkPath, methodPackage, Integer.class, getOffloadingMethod());
			hydraHelper.postTask(offloadableMethod);
			try {
				synchronized (offloadableMethod) {
					offloadableMethod.wait();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			println(offloadableMethod.dataPackage.dest + " \ttime = " + (offloadableMethod.execDuration) / 1000f);
			printStream.println(offloadableMethod.dataPackage.dest + "," + (offloadableMethod.execDuration) / 1000f);
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
