package com.symlab.hydraservice;

import java.io.File;
import java.io.PrintStream;
import java.io.Serializable;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

import com.symlab.hydra.DandelionHelper;
import com.symlab.hydra.lib.MethodPackage;
import com.symlab.hydra.lib.OffloadableMethod;
import com.symlab.hydra.lib.ResultTicket;
import com.symlab.testoffloading.NQueens;
import com.symlab.testoffloading.Sorting;
import com.symlab.testoffloading.Sudoku;
import com.symlab.testoffloading.TestFaceDetection;

public class MainActivity extends Activity {

	private transient DandelionHelper dh;
	private TextView tv;
	private Spinner spinner;

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
					dh.startHelping();
				} else {
					dh.stopHelping();
				}

			}

		});
		dh = new DandelionHelper(this);

		spinner = (Spinner) findViewById(R.id.spinner1);

		new Thread(new Runnable() {
			@Override
			public void run() {
				runOnUiThread(new Runnable() {
					public void run() {
						println("Hydra");
					}
				});
			}

		}).start();

		// startService(new View(this));
		// execute(null);

	}

	public void startService(View v) {
		dh.initializeOHelper();
		runOnUiThread(new Runnable() {
			public void run() {
				clear_screen();
				println("service is started");
			}
		});
	}

	public void stopService(View v) {
		dh.stopHelping();
		dh.tearDownOHelper();
		dh.stopOffloadingService();
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
				// OffloadableMethod(MainActivity.this, getPackageName(),
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

	PrintStream printStream;

	public void execute(View v) {
		try {
//			printStream = new PrintStream(new File("/sdcard/res.csv"));
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

	public void execute_nqueen(View v, final int n) throws Exception {
		println(n + "-Queens");
		new Thread() {
			public void run() {

				for (int i = 0; i < 10; i++) {

					final int num = 1;
					System.out.println(num + " tasks should be executed.");
					NQueens subtasks;
					final ResultTicket rt;
					final Boolean result;
					final String classMethodName = Sorting.class.getName() + "#" + "solveNQueens" + "#" + 0 + "#" + 1;
//					dh.startProfiling(classMethodName);
					result = true;
					subtasks = new NQueens();
					final Class<?>[] paramTypes = { int.class, int.class, int.class };
					Object[] paramValues = { n, 0, n };
					final MethodPackage methodPackage = new MethodPackage((int) (Math.random() * 100000), subtasks, "solveNQueens", paramTypes, paramValues);
					final OffloadableMethod offloadableMethod = new OffloadableMethod(MainActivity.this, getPackageName(), methodPackage, Boolean.class);
					dh.postTask(offloadableMethod);
					try {
						synchronized (offloadableMethod) {
							offloadableMethod.wait();
						}
//						Thread.sleep(140000);
					} catch (Exception e) {
						e.printStackTrace();
					}
					new Thread() {
						public void run() {
							try {
								runOnUiThread(new Runnable() {
									public void run() {
										// System.out.println("result is ready for task "
										// + methodPackage.id);
										// println("result is ready for task " +
										// methodPackage.id);
										// println("Total RTT = " +
										// (offloadableMethod.dataPackage.rttDeviceToVM)
										// / 1000f);
										// println("RTT (Router to VM/offloadee) = "
										// +
										// (offloadableMethod.dataPackage.rttRouterToVM)
										// / 1000f);
										// println("RTT (Manager to VM) = " +
										// (offloadableMethod.dataPackage.rttManagerToVM)
										// / 1000f);
										println(offloadableMethod.dataPackage.dest + " \ttime = " + (offloadableMethod.dataPackage.pureExecTime) / 1000f);
//										printStream.println(offloadableMethod.dataPackage.dest + "," + (offloadableMethod.dataPackage.pureExecTime) / 1000f);
									}
								});

							} catch (Exception e) {
								e.printStackTrace();
							}
						};
					}.start();
					// System.out.println("*** Task submitted ***");
				}
			};
		}.start();
	}

	public void execute_sort(View v) throws Exception {
		println("qSort");
		final int num = 1;
		System.out.println(num + " tasks should be executed.");

		new Thread(new Runnable() {

			@Override
			public void run() {

				for (int k = 0; k < 20; k++) {
					final ResultTicket rt;
					final Boolean result;
					final String classMethodName = Sorting.class.getName() + "#" + "qSort" + "#" + 0 + "#" + 1;
					dh.startProfiling(classMethodName);
					result = true;
					Sorting subtasks = new Sorting();
					final Class<?>[] paramTypes = {};
					Object[] paramValues = {};
					final MethodPackage methodPackage = new MethodPackage((int) (Math.random() * 100000), subtasks, "qSort", paramTypes, paramValues);
					final OffloadableMethod offloadableMethod = new OffloadableMethod(MainActivity.this, getPackageName(), methodPackage, void.class);
					dh.postTask(offloadableMethod);
					synchronized (offloadableMethod) {
						try {
							offloadableMethod.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					new Thread() {
						public void run() {
							try {
								runOnUiThread(new Runnable() {
									public void run() {
										// System.out.println("result is ready for task "
										// + methodPackage.id);
										// println("result is ready for task " +
										// methodPackage.id);
										// println("Total RTT = " +
										// (offloadableMethod.dataPackage.rttDeviceToVM)
										// / 1000f);
										// println("RTT (Router to VM/offloadee) = "
										// +
										// (offloadableMethod.dataPackage.rttRouterToVM)
										// / 1000f);
										// println("RTT (Manager to VM) = " +
										// (offloadableMethod.dataPackage.rttManagerToVM)
										// / 1000f);
										println(offloadableMethod.dataPackage.dest + " \ttime = " + (offloadableMethod.dataPackage.pureExecTime) / 1000f);
//										printStream.println(offloadableMethod.dataPackage.dest + "," + (offloadableMethod.dataPackage.pureExecTime) / 1000f);
									}
								});

							} catch (Exception e) {
								e.printStackTrace();
							}
						};
					}.start();
				}
			}
		}).start();

	}

	public void execute_sudoku(View v) throws Exception {
		println("Sudoku");
		final int num = 1;
		System.out.println(num + " tasks should be executed.");

		new Thread(new Runnable() {

			@Override
			public void run() {

				for (int k = 0; k < 20; k++) {

					final ResultTicket rt;
					final Boolean result;
					final String classMethodName = Sorting.class.getName() + "#" + "hasSolution" + "#" + 0 + "#" + 1;
					dh.startProfiling(classMethodName);
					result = true;
					Sudoku subtasks = new Sudoku();
					final Class<?>[] paramTypes = {};
					Object[] paramValues = {};
					final MethodPackage methodPackage = new MethodPackage((int) (Math.random() * 100000), subtasks, "hasSolution", paramTypes, paramValues);
					final OffloadableMethod offloadableMethod = new OffloadableMethod(MainActivity.this, getPackageName(), methodPackage, Boolean.class);
					dh.postTask(offloadableMethod);
					synchronized (offloadableMethod) {
						try {
							offloadableMethod.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					new Thread() {
						public void run() {
							try {
								runOnUiThread(new Runnable() {
									public void run() {
										// System.out.println("result is ready for task "
										// + methodPackage.id);
										// println("result is ready for task " +
										// methodPackage.id);
										// println("Total RTT = " +
										// (offloadableMethod.dataPackage.rttDeviceToVM)
										// / 1000f);
										// println("RTT (Router to VM/offloadee) = "
										// +
										// (offloadableMethod.dataPackage.rttRouterToVM)
										// / 1000f);
										// println("RTT (Manager to VM) = " +
										// (offloadableMethod.dataPackage.rttManagerToVM)
										// / 1000f);
										println(offloadableMethod.dataPackage.dest + " \ttime = " + (offloadableMethod.dataPackage.pureExecTime) / 1000f);
//										printStream.println(offloadableMethod.dataPackage.dest + "," + (offloadableMethod.dataPackage.pureExecTime) / 1000f);
									}
								});

							} catch (Exception e) {
								e.printStackTrace();
							}
						};
					}.start();
				}
			}
		}).start();

	}

	public void execute_face(View v, final int n) throws Exception {
		println("FaceDetection " + n);
		final int num = 1;
		new Thread(new Runnable() {
			@Override
			public void run() {
				for (int k = 0; k < 20; k++) {
					final ResultTicket rt;
					final Boolean result;
					final String classMethodName = Sorting.class.getName() + "#" + "detect_faces" + "#" + 0 + "#" + 1;
					dh.startProfiling(classMethodName);
					result = true;
					TestFaceDetection subtasks = new TestFaceDetection();
					final Class<?>[] paramTypes = { int.class, int.class };
					Object[] paramValues = { 20, n };
					final MethodPackage methodPackage = new MethodPackage((int) (Math.random() * 100000), subtasks, "detect_faces", paramTypes, paramValues);
					final OffloadableMethod offloadableMethod = new OffloadableMethod(MainActivity.this, getPackageName(), methodPackage, Integer.class);
					dh.postTask(offloadableMethod);
					synchronized (offloadableMethod) {
						try {
							offloadableMethod.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					new Thread() {
						public void run() {
							try {
								runOnUiThread(new Runnable() {
									public void run() {
										// System.out.println("result is ready for task "
										// + methodPackage.id);
										// println("result is ready for task " +
										// methodPackage.id);
										// println("Total RTT = " +
										// (offloadableMethod.dataPackage.rttDeviceToVM)
										// / 1000f);
										// println("RTT (Router to VM/offloadee) = "
										// +
										// (offloadableMethod.dataPackage.rttRouterToVM)
										// / 1000f);
										// println("RTT (Manager to VM) = " +
										// (offloadableMethod.dataPackage.rttManagerToVM)
										// / 1000f);
										println(offloadableMethod.dataPackage.dest + " \ttime = " + (offloadableMethod.dataPackage.pureExecTime) / 1000f);
//										printStream.println(offloadableMethod.dataPackage.dest + "," + (offloadableMethod.dataPackage.pureExecTime) / 1000f);
									}
								});

							} catch (Exception e) {
								e.printStackTrace();
							}
						};
					}.start();
				}
			}
		}).start();

	}

	public void println(String s) {
		tv.append(s + "\n");
	}

	public void print(String s) {
		tv.append(s);
	}

	public void clear_screen() {
		tv.setText("");
	}

}
