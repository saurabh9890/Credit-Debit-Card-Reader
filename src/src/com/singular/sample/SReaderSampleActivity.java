package com.singular.sample;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.util.concurrent.TimeoutException;

import com.singular.hijack.SReaderApi;

public class SReaderSampleActivity extends Activity {
	public final String TAG = "SReaderSampleActivity";
	Button swipe, detect, stop;
	TextView result_text;
	private TimeCount time = null;
	private AudioManager am = null;

	private boolean mHeadsetPlugged = false;
	private BroadcastReceiver mHeadsetReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(Intent.ACTION_HEADSET_PLUG)) {
				boolean hasHeadset = (intent.getIntExtra("state", 0) == 1);
				boolean hasMicroPhone = (intent.getIntExtra("microphone", 0) == 1);
				if (hasHeadset && hasMicroPhone) {
					mHeadsetPlugged = true;
					handler.post(enable_detect);
				} else {
					mHeadsetPlugged = false;
					if (sreader != null)
						sreader.Stop();
					handler.post(disable_button);
				}
				handler.post(mHeadsetPluginHandler);
			}
		}
	};

	private Handler handler = new Handler();
	SReaderApi sreader = null;

	private String version = null;
	private String ksn = null;
	private String random = null;
	private String workingkey = null;

	private String encryption_data = null;
	private String decryption_data = null;

	private Runnable mHeadsetPluginHandler = new Runnable() {
		public void run() {
			String plug_str = mHeadsetPlugged ? "plugin" : "unplugin";
			Toast.makeText(SReaderSampleActivity.this, "Headset " + plug_str,
					Toast.LENGTH_LONG).show();
			if (sreader != null) {
				CloseSinWave();
				finish();
			}
		}
	};

	private Runnable disable_button = new Runnable() {
		public void run() {
			swipe.setEnabled(false);
			detect.setEnabled(false);
		}
	};

	private Runnable enable_button = new Runnable() {
		public void run() {
			swipe.setClickable(true);
			swipe.setEnabled(true);
			swipe.setText(R.string.swipe);
			detect.setClickable(true);
			detect.setEnabled(true); // false
			stop.setEnabled(true);
			detect.setText(R.string.detect);
		}
	};

	private Runnable enable_detect = new Runnable() {
		public void run() {
			detect.setClickable(true);
			detect.setEnabled(true);
			detect.setText(R.string.detect);
		}
	};

	private Runnable timeout_ack = new Runnable() {
		public void run() {
			Toast.makeText(SReaderSampleActivity.this, "Timeout!!!",
					Toast.LENGTH_LONG).show();
		}
	};

	private Runnable unknown_err = new Runnable() {
		public void run() {
			result_text.setText(R.string.unknown_error);
		}
	};

	private Runnable set_version = new Runnable() {
		public void run() {
			String txt = version + "\n";
			result_text.setText(txt);
		}
	};

	private Runnable display_encryptiondata = new Runnable() {
		public void run() {
			String txt = version + "\n\nEncryption data\n";
			txt += encryption_data + "\n\n\n";
			result_text.setText(txt);
		}
	};

	private Runnable display_decryptiondata = new Runnable() {
		public void run() {
			String txt = version + "\n\nEncryption data\n";
			txt += encryption_data + "\n\n\nDecryption data\n";
			txt += decryption_data + "\n";
			result_text.setText(txt);
		}
	};

	private Runnable clear_all = new Runnable() {
		public void run() {
			version = "";
			encryption_data = "";
			decryption_data = "";
			result_text.setText("");
		}
	};

	private Runnable clear_encryption = new Runnable() {
		public void run() {
			encryption_data = "";
			decryption_data = "";
			String txt = version + "\n";
			result_text.setText(txt);
		}
	};

	private Runnable begin_detect = new Runnable() {
		public void run() {
			myToast = new MyToast(SReaderSampleActivity.this,
					"Card Reader Detecting...");
			myToast.show();
		}
	};

	private Runnable begin_swipe = new Runnable() {
		public void run() {
			myToast = new MyToast(SReaderSampleActivity.this, "Please swipe card...");
			myToast.show();
		}
	};

	private Runnable settext_swpie = new Runnable() {
		public void run() {
			swipe.setClickable(true);
			swipe.setText(R.string.swipe);
		}
	};

	/* 定义一个Toast的内部类 */
	public class MyToast {
		private Context mContext = null;
		private Toast mToast = null;
		private Handler mHandler = null;
		private Runnable mToastThread = new Runnable() {

			public void run() {
				mToast.show();
				mHandler.postDelayed(mToastThread, 3000);
			}
		};

		public MyToast(Context context, String txt) {
			mContext = context;
			mHandler = new Handler(mContext.getMainLooper());
			mToast = Toast.makeText(mContext, txt, Toast.LENGTH_LONG);
		}

		public void setText(String text) {
			mToast.setText(text);
		}

		public void show() {
			mHandler.post(mToastThread);
		}

		public void cancel() {
			mHandler.removeCallbacks(mToastThread);
			mToast.cancel();
		}
	}

	private MyToast myToast = null;

	/* 定义一个倒计时的内部类 */
	class TimeCount extends CountDownTimer {
		int id;

		public TimeCount(int id, long millisInFuture, long countDownInterval) {
			super(millisInFuture, countDownInterval);// 参数依次为总时长,和计时的时间间隔
			this.id = id;
		}

		@Override
		public void onFinish() {// 计时完毕时触发
			if (id == R.id.swipe) {
				swipe.setText(R.string.reswipe);
				swipe.setClickable(true);
			} else if (id == R.id.detect) {
				detect.setText(R.string.detect);
				detect.setClickable(true);
			}
		}

		@Override
		public void onTick(long millisUntilFinished) {// 计时过程显示
			CharSequence str = getString(R.string.second);
			if (id == R.id.swipe) {
				swipe.setClickable(false);
				swipe.setText(millisUntilFinished / 1000 + str.toString());
			} else if (id == R.id.detect) {
				detect.setClickable(false);
				detect.setText(millisUntilFinished / 1000 + str.toString());
			}
		}
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

		IntentFilter iFilter = new IntentFilter();
		iFilter.addAction(Intent.ACTION_HEADSET_PLUG);
		iFilter.addCategory(Intent.CATEGORY_DEFAULT);
		registerReceiver(mHeadsetReceiver, iFilter);

		swipe = (Button) this.findViewById(R.id.swipe);
		swipe.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				onSwipe();
			}
		});

		detect = (Button) this.findViewById(R.id.detect);
		detect.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				onDetect();
			}
		});

		stop = (Button) this.findViewById(R.id.stop);
		stop.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				onStopReader();
			}
		});

		result_text = (TextView) this.findViewById(R.id.result);

		swipe.setEnabled(false);
		detect.setEnabled(true);
		stop.setEnabled(false);
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		int maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		am.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, 0);
	}

	@Override
	public void onDestroy() {
		unregisterReceiver(mHeadsetReceiver);
		super.onDestroy();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			if (sreader != null) {
				sreader.Stop();
				sreader = null;
				if (myToast != null)
					myToast.cancel();
				finish();
				System.exit(0);
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	private void onSwipe() {
		if (sreader == null)
			return;
		time = new TimeCount(R.id.swipe, 15000, 1000);
		time.start();// 开始计时
		swipe.setEnabled(false);
		stop.setEnabled(true);
		new Thread() {
			public void run() {
				String data = null;
				decryption_data = null;
				encryption_data = null;
				handler.post(clear_encryption);
				handler.post(begin_swipe);
				try {
					Log.v(TAG, "getdata");
					data = sreader.ReadCard(15000);

				} catch (Exception ex) {
					if (ex instanceof TimeoutException) {
						time.cancel();
						myToast.cancel();
						sreader.Cancel();
						handler.post(enable_button);
						handler.post(timeout_ack);
						return;
					} else
						handler.post(unknown_err);
					CloseSinWave();
				}

				Log.v(TAG, "toast cancel");
				myToast.cancel();
				time.cancel();

				if (data == null) {
					encryption_data = sreader.GetErrorString();
					if (encryption_data.equalsIgnoreCase("cancel all"))
						return;
					handler.post(display_encryptiondata);
				} else {
					Log.v(TAG, "encryption data=" + data);
					encryption_data = "\n" + data;
					handler.post(display_encryptiondata);

					Log.v(TAG, "decryption...workingkey=" + workingkey);
					String d_str = sreader.TriDesDecryption(workingkey, data);
					Log.v(TAG, "decryption over=" + d_str);

					if (d_str != null) {
						int index2 = d_str.indexOf("A2");
						Log.v(TAG, "A2 index=" + index2);
						int index3 = d_str.indexOf("A3");
						Log.v(TAG, "A3 index=" + index3);
						if (index2 < 2 || index3 < 4)
							return;
						String t1 = d_str.substring(2, index2);
						String t2 = d_str.substring(index2 + 2, index3);
						String t3 = d_str.substring(index3 + 2);

						String ex_msg = "";

						if (t1.equals(""))
							decryption_data = "\nT1=" + "T1 Empty";
						else
							decryption_data = "\nT1="
									+ changeHexString2CharString(t1);
						if (t2.equals(""))
							decryption_data += "\nT2=" + "T2 Empty";
						else {
							String e2 = changeHexString2CharString(t2);
							
							if (e2.length() < 24 || e2.length() > 40)
								ex_msg = "\nTrack2 Length Error = " + e2.length() + "byte";
							decryption_data += "\nT2=" + e2;
						}
						if (t3.equals(""))
							decryption_data += "\nT3=" + "T3 Empty";
						else
							decryption_data += "\nT3="
									+ changeHexString2CharString(t3) + ex_msg;
						handler.post(display_decryptiondata);
					}

					try {
						Log.v(TAG, "re-getrandom");
						random = sreader.GetRandom(10000);
						if (random == null) {
							String err = sreader.GetErrorString();
							if (err.equalsIgnoreCase("cancel all"))
								return;
						}
						workingkey = sreader.GenerateWorkingKey(random, ksn);
					} catch (Exception ex) {
						if (ex instanceof TimeoutException) {
							handler.post(timeout_ack);
							sreader.Cancel();
							handler.post(enable_button);
							handler.post(settext_swpie);
							return;
						} else
							handler.post(unknown_err);
						CloseSinWave();
					}
				}
				handler.post(enable_button);
				handler.post(settext_swpie);
			}
		}.start();
	}

	/**
	 * 将16进制转换成ASCII码
	 * 
	 * @param buf
	 * @return
	 */
	private String changeHexString2CharString(String e) {
		String char_txt = "";
		for (int i = 0; i < e.length(); i = i + 2) {
			String c = e.substring(i, i + 2);
			char j = (char) Integer.parseInt(c, 16);
			char_txt += j;
		}
		return char_txt;
	}

	private boolean Detect_sReader() {
		mHeadsetPlugged = HeadSetUtils.checkHeadset();
		if (!mHeadsetPlugged) {
			result_text.setText(R.string.nodevice);
		}
		return mHeadsetPlugged;
	}

	private boolean GenerateSinWave() {
		sreader = SReaderApi.getSreaderInstance();
		if (sreader.Init() == true) {
			sreader.Start();
			am.setMode(AudioManager.MODE_NORMAL);
			return true;
		}
		return false;
	}

	private void CloseSinWave() {
		if (sreader != null)
			sreader.Stop();
	}

	private void Initialization() {
		time = new TimeCount(R.id.detect, 25000, 1000);
		time.start();// 开始计时
		detect.setEnabled(false);
		swipe.setEnabled(false);
		stop.setEnabled(true);
		handler.post(begin_detect);
		new Thread() {
			public void run() {
				int i = 0;
				try {
					Log.v(TAG, "sleep 5000ms for power on....");
					// Log.e(TAG, "now=" + SystemClock.uptimeMillis());
					sleep(5000);

					Log.v(TAG, "getversion");
					version = sreader.GetVersion(10000);

					i++;
					Log.v(TAG, "getksn");
					ksn = sreader.GetKSN(10000);

					if (ksn == null) {
						String err = sreader.GetErrorString();
						if (err.equalsIgnoreCase("cancel all"))
							return;
						throw new Exception("ksn is null");
					}

					i++;
					Log.v(TAG, "getrandom");
					random = sreader.GetRandom(10000);

					if (random == null) {
						String err = sreader.GetErrorString();
						if (err.equalsIgnoreCase("cancel all"))
							return;
						throw new Exception("random is null");
					}

					workingkey = sreader.GenerateWorkingKey(random, ksn);
					Log.v(TAG, "workingkey=" + workingkey);

					if (workingkey == null) {
						String err = sreader.GetErrorString();
						if (err.equalsIgnoreCase("cancel all"))
							return;
						throw new Exception("workingkey is null");
					}

					time.cancel();
					myToast.cancel();
					handler.post(enable_button);
					handler.post(set_version);
				} catch (Exception ex) {
					Log.e(TAG, "Initialization(" + i + ") ex=" + ex);
					time.cancel();
					myToast.cancel();
					if (ex instanceof TimeoutException) {
						handler.post(timeout_ack);
					} else
						handler.post(unknown_err);
					handler.post(enable_detect);
					CloseSinWave();
				}
			}
		}.start();
	}

	private void onDetect() {
		if (Detect_sReader() == true) {
			handler.post(clear_all);
			if (GenerateSinWave() == true) {
				Initialization();
			}
		}
	}

	private void onCancel() {
		if (sreader == null)
			return;
		new Thread() {
			public void run() {
				sreader.Cancel();
				sreader.Stop();
			}
		}.start();
	}

	private void onStopReader() {
		CloseSinWave();
		if (time != null)
			time.cancel();
		if (myToast != null)
			myToast.cancel();
		swipe.setText(R.string.swipe);
		handler.post(disable_button);
		handler.post(enable_detect);
		handler.post(clear_all);
		stop.setEnabled(false);
	}
}