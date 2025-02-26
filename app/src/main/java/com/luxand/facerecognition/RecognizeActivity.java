package com.luxand.facerecognition;

import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.luxand.FSDK;

public class RecognizeActivity extends AppCompatActivity implements View.OnClickListener {
    private Preview mPreview;
    private ProcessImageAndDrawResults mDraw;
    private final String database = "Memory50.dat";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Camera layer and drawing layer
        mDraw = new ProcessImageAndDrawResults(this);
        mPreview = new Preview(this, mDraw);
        mDraw.mTracker = new FSDK.HTracker();
        
        String templatePath = this.getApplicationInfo().dataDir + "/" + database;
        if (FSDK.FSDKE_OK != FSDK.LoadTrackerMemoryFromFile(mDraw.mTracker, templatePath)) {
            int res = FSDK.CreateTracker(mDraw.mTracker);
            if (FSDK.FSDKE_OK != res) {
                showErrorAndClose("Error creating tracker", res);
            }
        }

        int errpos[] = new int[1];
        FSDK.SetTrackerMultipleParameters(mDraw.mTracker, "ContinuousVideoFeed=true;RecognitionPrecision=0;Threshold=0.997;Threshold2=0.9995;ThresholdFeed=0.97;MemoryLimit=1000;HandleArbitraryRotations=false;DetermineFaceRotationAngle=false;InternalResizeWidth=70;FaceDetectionThreshold=5;", errpos);
        if (errpos[0] != 0) {
            showErrorAndClose("Error setting tracker parameters, position", errpos[0]);
        }

        setContentView(mPreview);
        addContentView(mDraw, new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT));

        // Menu
        View buttons = getLayoutInflater().inflate(R.layout.recognize_menu, null);
        buttons.findViewById(R.id.backButton).setOnClickListener(this);
        addContentView(buttons, new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT));
    }

    public void showErrorAndClose(String error, int code) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(error + ": " + code)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        android.os.Process.killProcess(android.os.Process.myPid());
                    }
                })
                .show();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.backButton) {
            finish();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        pauseProcessingFrames();
        String templatePath = this.getApplicationInfo().dataDir + "/" + database;
        FSDK.SaveTrackerMemoryToFile(mDraw.mTracker, templatePath);
    }

    @Override
    public void onResume() {
        super.onResume();
        resumeProcessingFrames();
    }

    private void pauseProcessingFrames() {
        mDraw.mStopping = 1;
        for (int i=0; i<100; ++i) {
            if (mDraw.mStopped != 0) break;
            try { Thread.sleep(10); }
            catch (Exception ex) {}
        }
    }

    private void resumeProcessingFrames() {
        mDraw.mStopped = 0;
        mDraw.mStopping = 0;
    }
}
