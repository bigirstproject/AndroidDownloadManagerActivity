package com.duowan.AndroidDownloadManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.duowan.download.DownloadManager;
import com.duowan.download.PicAddressUtil;

public class AndroidDownloadManagerActivity extends Activity {
	final String strPref_Download_ID = "PREF_DOWNLOAD_ID";
	SharedPreferences preferenceManager;
	DownloadManager downloadManager;
	ImageView image;
	Button btnStartDownloadOne;
	Button btnStartDownloadTwo;
	Button btnStartDownloadThree;
	Button btnStartDownloadFour;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);  

		preferenceManager = PreferenceManager.getDefaultSharedPreferences(this);    
		// downloadManager = (DownloadManager)
		// getSystemService(DOWNLOAD_SERVICE);
		downloadManager = new DownloadManager(getContentResolver(),
				"com.duowan.AndroidDownloadManager");
		btnStartDownloadOne = (Button) findViewById(R.id.startdownload_one);
		btnStartDownloadTwo = (Button) findViewById(R.id.startdownload_two);
		btnStartDownloadThree = (Button) findViewById(R.id.startdownload_three);
		btnStartDownloadFour = (Button) findViewById(R.id.startdownload_four);
		btnStartDownloadOne.setOnClickListener(new customButtonOnClickListener(
				PicAddressUtil.DOWNLOAD_FILE_ONE));
		btnStartDownloadTwo.setOnClickListener(new customButtonOnClickListener(
				PicAddressUtil.DOWNLOAD_FILE_TWO));
		btnStartDownloadThree
				.setOnClickListener(new customButtonOnClickListener(
						PicAddressUtil.DOWNLOAD_FILE_THREE));
		btnStartDownloadFour
				.setOnClickListener(new customButtonOnClickListener(
						PicAddressUtil.DOWNLOAD_FILE_FOUR));
		image = (ImageView) findViewById(R.id.image_one);
	}

	class customButtonOnClickListener implements OnClickListener {
		String url;

		public customButtonOnClickListener(String url) {
			this.url = url;
		}

		@Override
		public void onClick(View v) {
			Uri downloadUri = Uri.parse(url);
			DownloadManager.Request request = new DownloadManager.Request(
					downloadUri);
			request.setDestinationUri(Uri.fromFile(new File(Environment
					.getExternalStorageDirectory(), url.substring(
					url.lastIndexOf("/") + 1, url.length()-1))));
			long id = downloadManager.enqueue(request);

			// Save the request id
			Editor PrefEdit = preferenceManager.edit();
			PrefEdit.putLong(strPref_Download_ID, id);
			PrefEdit.commit();
		}

	}

	@Override
	protected void onResume() {
		super.onResume();
		IntentFilter intentFilter = new IntentFilter(
				DownloadManager.ACTION_DOWNLOAD_COMPLETE);
		registerReceiver(downloadReceiver, intentFilter);
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(downloadReceiver);
	}

	private BroadcastReceiver downloadReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context arg0, Intent arg1) {
			CheckDwnloadStatus();
			DownloadManager.Query query = new DownloadManager.Query();
			query.setFilterById(preferenceManager.getLong(strPref_Download_ID,
					0));
			Cursor cursor = downloadManager.query(query);
			if (cursor.moveToFirst()) {
				int columnIndex = cursor
						.getColumnIndex(DownloadManager.COLUMN_STATUS);
				int status = cursor.getInt(columnIndex);
				if (status == DownloadManager.STATUS_SUCCESSFUL) {

					// Retrieve the saved request id
					long downloadID = preferenceManager.getLong(
							strPref_Download_ID, 0);

					ParcelFileDescriptor file;
					try {
						file = downloadManager.openDownloadedFile(downloadID);
						FileInputStream fileInputStream = new ParcelFileDescriptor.AutoCloseInputStream(
								file);
						Bitmap bm = BitmapFactory.decodeStream(fileInputStream);
						image.setImageBitmap(bm);
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}

				}
			}
		}
	};

	private void CheckDwnloadStatus() {
		DownloadManager.Query query = new DownloadManager.Query();
		long id = preferenceManager.getLong(strPref_Download_ID, 0);
		query.setFilterById(id);
		Cursor cursor = downloadManager.query(query);
		if (cursor.moveToFirst()) {
			int columnIndex = cursor
					.getColumnIndex(DownloadManager.COLUMN_STATUS);
			int status = cursor.getInt(columnIndex);
			int columnReason = cursor
					.getColumnIndex(DownloadManager.COLUMN_REASON);
			int reason = cursor.getInt(columnReason);

			switch (status) {
			case DownloadManager.STATUS_FAILED:
				String failedReason = "";
				switch (reason) {
				case DownloadManager.ERROR_CANNOT_RESUME:
					failedReason = "ERROR_CANNOT_RESUME";
					break;
				case DownloadManager.ERROR_DEVICE_NOT_FOUND:
					failedReason = "ERROR_DEVICE_NOT_FOUND";
					break;
				case DownloadManager.ERROR_FILE_ALREADY_EXISTS:
					failedReason = "ERROR_FILE_ALREADY_EXISTS";
					break;
				case DownloadManager.ERROR_FILE_ERROR:
					failedReason = "ERROR_FILE_ERROR";
					break;
				case DownloadManager.ERROR_HTTP_DATA_ERROR:
					failedReason = "ERROR_HTTP_DATA_ERROR";
					break;
				case DownloadManager.ERROR_INSUFFICIENT_SPACE:
					failedReason = "ERROR_INSUFFICIENT_SPACE";
					break;
				case DownloadManager.ERROR_TOO_MANY_REDIRECTS:
					failedReason = "ERROR_TOO_MANY_REDIRECTS";
					break;
				case DownloadManager.ERROR_UNHANDLED_HTTP_CODE:
					failedReason = "ERROR_UNHANDLED_HTTP_CODE";
					break;
				case DownloadManager.ERROR_UNKNOWN:
					failedReason = "ERROR_UNKNOWN";
					break;
				}

				Toast.makeText(AndroidDownloadManagerActivity.this,
						"FAILED: " + failedReason, Toast.LENGTH_LONG).show();
				break;
			case DownloadManager.STATUS_PAUSED:
				String pausedReason = "";

				switch (reason) {
				case DownloadManager.PAUSED_QUEUED_FOR_WIFI:
					pausedReason = "PAUSED_QUEUED_FOR_WIFI";
					break;
				case DownloadManager.PAUSED_UNKNOWN:
					pausedReason = "PAUSED_UNKNOWN";
					break;
				case DownloadManager.PAUSED_WAITING_FOR_NETWORK:
					pausedReason = "PAUSED_WAITING_FOR_NETWORK";
					break;
				case DownloadManager.PAUSED_WAITING_TO_RETRY:
					pausedReason = "PAUSED_WAITING_TO_RETRY";
					break;
				}

				Toast.makeText(AndroidDownloadManagerActivity.this,
						"PAUSED: " + pausedReason, Toast.LENGTH_LONG).show();
				break;
			case DownloadManager.STATUS_PENDING:
				Toast.makeText(AndroidDownloadManagerActivity.this, "PENDING",
						Toast.LENGTH_LONG).show();
				break;
			case DownloadManager.STATUS_RUNNING:
				Toast.makeText(AndroidDownloadManagerActivity.this, "RUNNING",
						Toast.LENGTH_LONG).show();
				break;
			case DownloadManager.STATUS_SUCCESSFUL:

				Toast.makeText(AndroidDownloadManagerActivity.this,
						"SUCCESSFUL", Toast.LENGTH_LONG).show();
				// GetFile();
				break;
			}
		}
	}

}