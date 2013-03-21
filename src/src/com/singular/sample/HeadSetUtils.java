package com.singular.sample;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class HeadSetUtils {
	private static final String HEADSET_STATE_PATH = "/sys/class/switch/h2w/state";
	private static final String HEADSET_NAME_PATH = "/sys/class/switch/h2w/name";

	static boolean checkHeadset() {
		String name = getHeadsetName();
		if (name != null && (0 != name.length())) {
			if (false == name.trim().equalsIgnoreCase("headset"))
				return false;
			String state = getHeadsetState();
			if (state == null || 0 == state.length())
				return false;
			int i = Integer.parseInt(state);
			return i != 0 ? true : false;
		}
		return false;
	}

	private static String getHeadsetName() {
		return readLineEx(HEADSET_NAME_PATH);
	}

	private static String getHeadsetState() {
		return readLineEx(HEADSET_STATE_PATH);
	}

	private static String readLineEx(String paramString) {
		File localFile = new File(paramString);
		if (false == localFile.exists())
			return null;

		try {
			String localFilePath = localFile.getAbsolutePath();
			FileInputStream localFileInputStream = new FileInputStream(
					localFilePath);
			InputStreamReader localInputStreamReader = new InputStreamReader(
					localFileInputStream);
			BufferedReader localBufferedReader = new BufferedReader(
					localInputStreamReader);
			String str = localBufferedReader.readLine();
			localBufferedReader.close();
			localInputStreamReader.close();
			localFileInputStream.close();
			return str;
		} catch (IOException localIOException) {
			localIOException.printStackTrace();
			return null;
		}

	}
}