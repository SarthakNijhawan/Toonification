package com.example.toonify2;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ToonifyActivity extends Activity {

	private static final String TAG = "Toonify";

	private static final int SELECT_PICTURE = 1;
	private static final int GOTO_CAMERA = 2;

	private Bitmap mBitmap;
	private ProgressBar mProgBar;
	private TextView mInfoText;
	private boolean fProcess;

	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			if(status == LoaderCallbackInterface.SUCCESS) {
				Log.i(TAG, "OpenCV loaded successfully.");
			} else {
				super.onManagerConnected(status);
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_toonify);
		mProgBar = (ProgressBar) findViewById(R.id.prog_bar);
		mProgBar.setVisibility(View.INVISIBLE);
		mInfoText = (TextView) findViewById(R.id.info_message);
	}

	@Override
	protected void onResume() {
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
		if(mBitmap != null && fProcess) {
			mInfoText.setText(R.string.info_processing);
			ImageView imview = (ImageView) findViewById(R.id.image_view);
			imview.setImageBitmap(mBitmap);
			mProgBar.setVisibility(View.VISIBLE);
			BitmapProcessTask bpt = new BitmapProcessTask(imview, mProgBar, mInfoText);
			bpt.execute(mBitmap);
			fProcess = false;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.toonify, menu);
		return true;
	}

	public void dispatchGalleryIntent(View view) {
		Intent intent = new Intent();
		intent.setType("image/*");
		intent.setAction(Intent.ACTION_GET_CONTENT);
		startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PICTURE);
	}
	
	public void dispatchCameraIntent(View view) {
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		startActivityForResult(intent, GOTO_CAMERA);
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data ) {
		if(resultCode == RESULT_OK) {
			if(requestCode == SELECT_PICTURE) {
				Uri selectedImageUri = data.getData();
				mBitmap = null;
				try {
					mBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if(mBitmap != null) {
					fProcess = true;
				}
			} else if(requestCode == GOTO_CAMERA) {
				
			}
		}
	}
	
	public void saveImage(View view) {
		ImageView imview = (ImageView) findViewById(R.id.image_view);
		if(imview != null) {
			
		}
		
		if(mBitmap != null) {
			saveImageToGallery(mBitmap);
		}
	}
	
	/*
	 * Used to save bitmaps and put them in the photo gallery.
	 */
	private void saveImageToGallery(Bitmap finalBitmap) {

	    String root = Environment.getExternalStorageDirectory().getAbsolutePath();
	    File myDir = new File(root + "/Toonify_Saved");    
	    myDir.mkdirs();
	    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
	    String timestamp = sdf.format(new Date());
	    String fname = "toonify-"+ timestamp +".jpg";
	    File file = new File (myDir, fname);
	    if (file.exists ()) file.delete (); 
	    try {
	           FileOutputStream out = new FileOutputStream(file);
	           finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
	           out.flush();
	           out.close();
	           mInfoText.setText(R.string.info_saved + " " + fname);
	    } catch (Exception e) {
	           e.printStackTrace();
	    }
	    sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory())));
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////
	/*
	 * Responsible for image processing task.
	 */
	private class BitmapProcessTask extends AsyncTask<Bitmap, Void, Bitmap> {
		private final WeakReference<ImageView> imviewRef;
		private final WeakReference<ProgressBar> progBarRef;
		private final WeakReference<TextView> textViewRef;

		public BitmapProcessTask(ImageView imview, ProgressBar progBar, TextView textView) {
			imviewRef = new WeakReference<ImageView>(imview);
			progBarRef = new WeakReference<ProgressBar>(progBar);
			textViewRef = new WeakReference<TextView>(textView);
		}

		@Override
		protected Bitmap doInBackground(Bitmap... params) {
			// Time to process the image...
			Bitmap bmp = params[0];
			// Initialize all required mats.
			// Inputs
			Mat rgba = new Mat();
			Mat rgb = new Mat();
			Mat gray = new Mat();
			// Intermediary
			Mat smallrgb = new Mat();
			Mat edges = new Mat();
			Mat tempA1 = new Mat();
			Mat tempA2 = new Mat();
			Mat tempB1 = new Mat();
			// Result
			Mat result = new Mat();
			// Fill input mats, rgba, rgb, and gray.
			Utils.bitmapToMat(bmp, rgba);
			Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY);
			Imgproc.cvtColor(rgba, rgb, Imgproc.COLOR_RGBA2RGB);
			//////////////////////////////////////////////////////////
			// Algorithm
			//////////////////////////////////////////////////////////
			Log.i(TAG, "size: " + gray.size().toString());
			// A1. Noise Reduction: median filter input
			Imgproc.medianBlur(gray, tempA1, 7);
			// A2. Edge Detection: canny edge detector?
			Imgproc.Canny(tempA1, edges, 50, 90);
			// A3. Morphological: connect lines, smooth contours.
			Size dilateSize = new Size(2,2);
			Imgproc.dilate(edges, edges, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, dilateSize));
			// A4. Edge Filter: remove short edges
			List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
			Imgproc.findContours(edges, contours, tempA2, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
			Log.i(TAG, "num contours: " + contours.size());
			List<MatOfPoint> keepContours = new ArrayList<MatOfPoint>();
			int cThresh = 10;
			for(MatOfPoint contour : contours) {
				if(Imgproc.contourArea(contour) > cThresh)
					keepContours.add(contour);
			}
			// B1. Resize: need small image for expensive bilateral filter.
			double scale = 4;
			Size smallSize = new Size(rgb.size().width/scale, rgb.size().height/scale);
			Imgproc.resize(rgb, smallrgb, smallSize);
			// B2. Bilateral Filter: where the magic happens :)
			int numReps = 7; // Number of times - affects speed.
			int kSize = 9; // Kernel size - affects speed.
			double sigmaC = 15; // Color strength.
			double sigmaS = 7; // Spatial strength - affects speed.
			for(int i = 0; i < numReps; i++) {
				Imgproc.bilateralFilter(smallrgb, tempB1, kSize, sigmaC, sigmaS);
				Imgproc.bilateralFilter(tempB1, smallrgb, kSize, sigmaC, sigmaS);
			}
			// B3. Resize: resize back up
			Imgproc.resize(smallrgb, rgb, rgba.size(), 0, 0, Imgproc.INTER_LINEAR);
			// B4. Smooth: smooth the colors.
			Imgproc.medianBlur(rgb, rgb, 7);
			// B5. Quantize Colors: change colors to stricter palette.
			Size size = rgb.size();
			double colorScale = 24; // Reduce each channel by this factor.
			byte[] pixels = new byte[3];
			Log.i(TAG, CvType.typeToString(rgb.type()));	
			for(int row = 0; row < size.height; row++) {
				for(int col = 0; col < size.width; col++) {
					rgb.get(row, col, pixels);
					for(int i = 0; i < 3; i++) {
						int pix = (int)pixels[i];
						pix = (int)(Math.floor((pix) / colorScale) * colorScale);
						if(pix < -128) pix = -128;
						if(pix > 127) pix = 127;
						pixels[i] = (byte)pix;
					}
					rgb.put(row, col, pixels);
				}
			}
			// B6. Smooth: final smoothing of colors
			Imgproc.medianBlur(rgb, rgb, 11);
			// C1. Combine: overlay edges on color image.
			Imgproc.drawContours(rgb, keepContours, -1, new Scalar(30,30,30), -1);
			// C2. Convert: back to RGBA
			Imgproc.cvtColor(rgb, result, Imgproc.COLOR_RGB2RGBA);
			//////////////////////////////////////////////////////////
			// end Algorithm
			//////////////////////////////////////////////////////////
			// Convert result back to bitmap.
			Utils.matToBitmap(result, bmp);
			// Release all mats.
			rgba.release();
			rgb.release();
			gray.release();
			smallrgb.release();
			edges.release();
			tempA1.release();
			tempA2.release();
			tempB1.release();
			result.release();
			return bmp;
		}

		@Override
		protected void onPostExecute(Bitmap bitmap) {
			if(isCancelled()) {
				bitmap = null;
			}
			if(imviewRef != null) {
				ImageView imview = imviewRef.get();
				if(imview != null) {
					Log.i(TAG, "setting the bitmap now...");
					imview.setImageBitmap(bitmap);
				}
			}
			if(progBarRef != null) {
				ProgressBar progBar = progBarRef.get();
				if(progBar != null) {
					progBar.setVisibility(View.INVISIBLE);
				}
			}
			if(textViewRef != null) {
				TextView textView = textViewRef.get();
				if(textView != null) {
					textView.setText(R.string.info_process_complete);
				}
			}
		}

	}
}
