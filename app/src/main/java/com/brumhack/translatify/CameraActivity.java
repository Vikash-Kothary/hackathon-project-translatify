package com.brumhack.translatify;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.SensorEvent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.clarifai.api.ClarifaiClient;
import com.clarifai.api.RecognitionRequest;
import com.clarifai.api.RecognitionResult;
import com.clarifai.api.Tag;
import com.clarifai.api.exception.ClarifaiException;
import com.memetix.mst.language.Language;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Timer;
import java.util.TimerTask;


/**
 * A placeholder fragment containing a simple view.
 */
public class CameraActivity extends Activity {

    public final static String TAG = "Translatify";
    public final static String APP_ID = "DqI1mgCUeAXlPPs8fFAq3WV85iPO3K3DJmzaWBxB";
    public final static String APP_SECRET = "tDbjwTRGP6noQ0NlFH2j7FugR3iN_xPvxjqrocRo";
    private final static String APP_TOKEN = "JbD6XHtC8AX28WeChBAt5rK2lZKeIU";

    private Button mBtnShot;
    private CountDownTimer countdown;
    private Camera camera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_camera);

        TextureView mTextureView = (TextureView) findViewById(R.id.texture);
        camera = new Camera(this, mTextureView);

//        Timer timer = new Timer()

        mBtnShot = (Button) findViewById(R.id.btn_takepicture);
        mBtnShot.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                camera.takeAnother();
                getTags(null);
            }
        });

        Accelerometer acc = new Accelerometer(this) {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                onAccelerometerChange(sensorEvent);
            }
        };
    }


    @Override
    protected void onResume() {
        super.onResume();
        camera.onResume();
    }


    @Override
    protected void onPause() {
        super.onPause();
        camera.onPause();
    }

    public void onAccelerometerChange(SensorEvent sensorEvent) {
        Float accX = sensorEvent.values[0];
        if (accX <= 0.2) {
            ((ImageView) findViewById(R.id.countdown)).setBackgroundColor(Color.GREEN);
            if (countdown == null) {
                countdown = new CountDownTimer(1000, 500) {
                    public void onTick(long millisUntilFinished) {
//                        Log.e(TAG, "seconds remaining: " + millisUntilFinished / 1000);
                    }

                    public void onFinish() {
                        Log.e(TAG, ":)");
                        camera.takePicture();
                        getTags(null);

                    }
                }.start();
            }
        } else {
            ((ImageView) findViewById(R.id.countdown)).setBackgroundColor(Color.RED);
            if (countdown != null) {
                countdown.cancel();
                countdown = null;
            }
        }

    }

    public void getTags(View view) {
        // The user picked an image. Send it to Clarifai for recognition.
        Bitmap bitmap = null;
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            try {
                bitmap = BitmapFactory.decodeStream(new FileInputStream(camera.getPicture()), null, options);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            if (bitmap != null) {

                Log.e(TAG, "Recognizing...");

                // Run recognition on a background thread since it makes a network call.
                new AsyncTask<Bitmap, Void, RecognitionResult>() {
                    @Override
                    protected RecognitionResult doInBackground(Bitmap... bitmaps) {
                        return recognizeBitmap(bitmaps[0]);
                    }

                    @Override
                    protected void onPostExecute(RecognitionResult result) {
                        updateUIForResult(result);
                    }
                }.execute(bitmap);
            } else {
                Log.e(TAG, "Unable to load selected image.");
            }

    }

    /**
     * Sends the given bitmap to Clarifai for recognition and returns the result.
     */
    private RecognitionResult recognizeBitmap(Bitmap bitmap) {

        Log.e(TAG, "Clarifai error");
        try {
            // Scale down the image. This step is optional. However, sending large images over the
            // network is slow and  does not significantly improve recognition performance.
            Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 320,
                    320 * bitmap.getHeight() / bitmap.getWidth(), true);

            // Compress the image as a JPEG.
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            byte[] jpeg = out.toByteArray();

            // Send the JPEG to Clarifai and return the result.

            ClarifaiClient client = new ClarifaiClient(APP_ID, APP_SECRET);
            return client.recognize(new RecognitionRequest(jpeg)).get(0);
        } catch (ClarifaiException e) {
            Log.e(TAG, "Clarifai error", e);
            return null;
        }
    }

    /**
     * Updates the UI by displaying tags for the given result.
     */
    private void updateUIForResult(RecognitionResult result) {
        if (result != null) {
            if (result.getStatusCode() == RecognitionResult.StatusCode.OK) {
                // Display the list of tags in the UI.
                StringBuilder b = new StringBuilder();
                for (Tag tag : result.getTags()) {
                    if (!tag.getName().toString().contains("nobody") && !tag.getName().toString().contains("politics") && !tag.getName().toString().contains("blur")) {
                        b.append(b.length() > 0 ? ", " : "").append(tag.getName());
                    }
                }
                String str = b.toString().split(",")[0] + " : ";
                try {
                    new Translator(this).translate(b.toString().split(",")[0], Language.FRENCH, str);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Log.e("Translator", "Tags:\n" + b);
            } else {
                Log.e(TAG, "Clarifai: " + result.getStatusMessage());
                Log.e(TAG, "Sorry, there was an error recognizing your image.");
            }
        } else {
            Log.e(TAG, "Sorry, there was an error recognizing your image.");
        }
    }
}