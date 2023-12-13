package com.example.SIFT;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.ViewGroup;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    public int maxRes = 1360; // We scale our image to the resolution of maximum 1000 pixels
    public double [][] greyC = new double[maxRes][maxRes]; // Current grey image
    public double [][] maskS0 = new double[5][5]; // Mask with the Gaussian blur function's values
    public double [][] maskS1 = new double[5][5]; // Mask with the Gaussian blur function's values
    public double [][] maskS2 = new double[7][7]; // Mask with the Gaussian blur function's values
    public double [][] maskS3 = new double[9][9]; // Mask with the Gaussian blur function's values
    public double [][] maskS4 = new double[11][11]; // Mask with the Gaussian blur function's values
    public double [][] maskS = new double[137][137]; // Mask with the Gaussian blur function's values
    public double [][] octave1000First = new double[maxRes][maxRes]; // Here, the figure will have the white border
    public double [][] octave1000Second = new double[maxRes][maxRes]; // Here, the figure will have the white border
    public double [][] octave1000Third = new double[maxRes][maxRes]; // Here, the figure will have the white border
    public double [][] octave1000Fourth = new double[maxRes][maxRes]; // Here, the figure will have the white border
    public double [][] octave1000Fifth = new double[maxRes][maxRes]; // Here, the figure will have the white border
    public double [][] DoG1000First = new double[maxRes][maxRes];
    public double [][] DoG1000Second = new double[maxRes][maxRes];
    public double [][] DoG1000Third = new double[maxRes][maxRes];
    public double [][] DoG1000Fourth = new double[maxRes][maxRes];
    public double [][] Hessian = new double [2][2]; // 2x2 Hessian matrix
    public int [][] keypoints1000 = new int [3000][2]; // Info about keypoints
    public int radius0, radius1, radius2, radius3, radius4, MatrixBorder, flagMax, flagMin, nk, maxNoKeyPoints=100; // maxNoKeyPoints - maximum number of keypoints
    public double maxFirst, maxSecond, minThird, maxThird, minFourth, maxFourth;
    public double minFirst, minSecond, sigma0, sigma1, sigma2, sigma3, sigma4, max, min, trace, det, threshold = 7.65; // 7.65 = 255 * 0.03;
    public double [] xk = new double [83]; // Coordinates of keypoints' net: 25 keypoints (1st level) + 58 keypoints (2nd level; 4 points, border, is included on the 1st level)
    public double [] yk = new double [83]; // Coordinates of keypoints' net
    public double [] IC = new double [83]; // Average intensities of in the circles around keypoints in the descriptor
    public int [][] ICdif = new int [83][83]; // Array with number of the point(s); differences are in the following array
    public double [][] ICdifDouble = new double [83][83]; // Array with number of the point(s); differences are in the following array

    public int x, y, i, j, width, height, pixel, k, i1, i2;
    public String fileSeparator = System.getProperty("file.separator");
    File file;
    Bitmap bmOut, bmOut1, bmOut2, bmOut3, bmOut4;
    OutputStream out;

    private static final String TAG = MainActivity.class.getSimpleName();
    private int REQUEST_CODE_PERMISSIONS = 101;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.READ_EXTERNAL_STORAGE"};
    TextureView textureView;
    MediaPlayer mp=new MediaPlayer(); // MediaPlayer

    ThreadOctave0 t0 = new ThreadOctave0();
    ThreadOctave1 t1 = new ThreadOctave1();
    ThreadOctave2 t2 = new ThreadOctave2();
    ThreadOctave3 t3 = new ThreadOctave3();
    DoGFirst t5 = new DoGFirst();
    DoGSecond t6 = new DoGSecond();
    DoGThird t7 = new DoGThird();
    SIFTkeypointsSecond t8 = new SIFTkeypointsSecond();
    public int [][] keypoints1000Second = new int [1500][2]; // Info about keypoints: Second, i.e. thread, part
    public int nkSecond;

//    ThreadX1 t0 = new ThreadX1(0, greyCInt); // We create a variable t0 of type ThreadX1
//    ThreadX1 t1 = new ThreadX1(1, greyCInt); // We create a variable t1 of type ThreadX1
//    ThreadX1 t2 = new ThreadX1(2, greyCInt); // We create a variable t2 of type ThreadX1
//    ThreadX1 t3 = new ThreadX1(3, greyCInt); // We create a variable t3 of type ThreadX1
//    ThreadX1 t4 = new ThreadX1(4, greyCInt); // We create a variable t4 of type ThreadX1
//    ThreadX1 t5 = new ThreadX1(5, greyCInt); // We create a variable t5 of type ThreadX1
//    ThreadX1 t6 = new ThreadX1(6, greyCInt); // We create a variable t6 of type ThreadX1
//    ThreadX1 t7 = new ThreadX1(7, greyCInt); // We create a variable t7 of type ThreadX1

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textureView = findViewById(R.id.view_finder);

//        MediaPlayer mp=new MediaPlayer(); // We play mp3 here
        try{
            mp.setDataSource(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+fileSeparator+"DeviceIsStarting.mp3");//Write your location here
            mp.prepare(); mp.start();
        }catch(Exception e){e.printStackTrace();}

        maxRes = 180;
        sigma0=0.707107; radius0=2; // radius0 is the radius of the matrix for the Gaussian blur for the current scale
        sigma1=1; radius1=3; // radius1 is the radius of the matrix for the Gaussian blur for the current scale
        sigma2=1.414214; radius2=5; // radius2 is the radius of the matrix for the Gaussian blur for the current scale
        sigma3=2; radius3=6; // radius3 is the radius of the matrix for the Gaussian blur for the current scale
        sigma4=2.828427; radius4=9; // radius4 is the radius of the matrix for the Gaussian blur for the maximum scale
        startFREAK();

        maxRes = 340;
        sigma0=1.414214; radius0=5; // radius0 is the radius of the matrix for the Gaussian blur for the current scale
        sigma1=2; radius1=6; // radius1 is the radius of the matrix for the Gaussian blur for the current scale
        sigma2=2.828427; radius2=9; // radius2 is the radius of the matrix for the Gaussian blur for the current scale
        sigma3=4; radius3=12; // radius3 is the radius of the matrix for the Gaussian blur for the current scale
        sigma4=5.656854; radius4=17; // radius4 is the radius of the matrix for the Gaussian blur for the maximum scale
        startFREAK();

        maxRes = 680;
        sigma0=2.828427; radius0=9; // radius0 is the radius of the matrix for the Gaussian blur for the current scale
        sigma1=4; radius1=12; // radius1 is the radius of the matrix for the Gaussian blur for the current scale
        sigma2=5.656854; radius2=17; // radius2 is the radius of the matrix for the Gaussian blur for the current scale
        sigma3=8; radius3=24; // radius3 is the radius of the matrix for the Gaussian blur for the current scale
        sigma4=11.313708; radius4=34; // radius4 is the radius of the matrix for the Gaussian blur for the maximum scale
        startFREAK();

        maxRes = 1360;
        sigma0=5.636854; radius0=17; // radius0 is the radius of the matrix for the Gaussian blur for the current scale
        sigma1=8; radius1=24; // radius1 is the radius of the matrix for the Gaussian blur for the current scale
        sigma2=11.313708; radius2=34; // radius2 is the radius of the matrix for the Gaussian blur for the current scale
        sigma3=16; radius3=48; // radius3 is the radius of the matrix for the Gaussian blur for the current scale
        sigma4=22.627417; radius4=68; // radius4 is the radius of the matrix for the Gaussian blur for the maximum scale
        startFREAK();

        // We calculate matrices maskS... to speed up the program and avoid repetitions
        sigma0=0.353553; radius0=2; // radius0 is the radius of the matrix for the Gaussian blur for the current scale
        sigma1=0.5; radius1=2; // radius1 is the radius of the matrix for the Gaussian blur for the current scale
        sigma2=0.707107; radius2=3; // radius2 is the radius of the matrix for the Gaussian blur for the current scale
        sigma3=1; radius3=4; // radius3 is the radius of the matrix for the Gaussian blur for the current scale
        sigma4=1.414214; radius4=5; // radius4 is the radius of the matrix for the Gaussian blur for the maximum scale
        for (x=-radius0;x<=radius0;x++)
            for (y=-radius0;y<=radius0;y++) {
                maskS0[x + radius0][y + radius0] = Math.exp(-(x * x + y * y) / (2.0 * sigma0 * sigma0)) / (2.0 * Math.PI * sigma0 * sigma0);
                //Log.i(TAG, "Gaussian function for bluring [" +x + "," + y + "]  " + maskS1[x + radius0][y + radius0]);
            }
        for (x=-radius1;x<=radius1;x++)
            for (y=-radius1;y<=radius1;y++) {
                maskS1[x + radius1][y + radius1] = Math.exp(-(x * x + y * y) / (2.0 * sigma1 * sigma1)) / (2.0 * Math.PI * sigma1 * sigma1);
                //Log.i(TAG, "Gaussian function for bluring [" +x + "," + y + "]  " + maskS1[x + radius0][y + radius0]);
            }
        for (x=-radius2;x<=radius2;x++)
            for (y=-radius2;y<=radius2;y++) {
                maskS2[x + radius2][y + radius2] = Math.exp(-(x * x + y * y) / (2.0 * sigma2 * sigma2)) / (2.0 * Math.PI * sigma2 * sigma2);
                //Log.i(TAG, "Gaussian function for bluring [" +x + "," + y + "]  " + maskS2[x + radius0][y + radius0]);
            }
        for (x=-radius3;x<=radius3;x++)
            for (y=-radius3;y<=radius3;y++) {
                maskS3[x + radius3][y + radius3] = Math.exp(-(x * x + y * y) / (2.0 * sigma3 * sigma3)) / (2.0 * Math.PI * sigma3 * sigma3);
                //Log.i(TAG, "Gaussian function for bluring [" +x + "," + y + "]  " + maskS3[x + radius0][y + radius0]);
            }
        for (x=-radius4;x<=radius4;x++)
            for (y=-radius4;y<=radius4;y++) {
                maskS4[x + radius4][y + radius4] = Math.exp(-(x * x + y * y) / (2.0 * sigma4 * sigma4)) / (2.0 * Math.PI * sigma4 * sigma4);
                //Log.i(TAG, "Gaussian function for bluring [" +x + "," + y + "]  " + maskS4[x + radius0][y + radius0]);
            }
//        DisplayKeyPoints();

        if(allPermissionsGranted()){
//            startCamera(); //start camera if permission has been granted by user
        } else{
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        mp.reset(); // After reset(), the object is like being just created
        try{
            mp.setDataSource(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+fileSeparator+"DeviceIsReady.mp3");//Write your location here
            mp.prepare(); mp.start();
        }catch(Exception e){e.printStackTrace();}
    }

    private void startCamera() {

        CameraX.unbindAll();

        Rational aspectRatio = new Rational (textureView.getWidth(), textureView.getHeight()); // This is original line
        Size screen = new Size(textureView.getWidth(), textureView.getHeight()); //size of the screen

        PreviewConfig pConfig = new PreviewConfig.Builder()
                .setTargetAspectRatio(aspectRatio)
                .setTargetResolution(screen)
                //.setLensFacing(CameraX.LensFacing.FRONT)
                .build();
        Preview preview = new Preview(pConfig);

        preview.setOnPreviewOutputUpdateListener(
                new Preview.OnPreviewOutputUpdateListener() {
                    @Override
                    public void onUpdated(Preview.PreviewOutput output){
                        ViewGroup parent = (ViewGroup) textureView.getParent();
                        parent.removeView(textureView);
                        parent.addView(textureView, 0);

                        textureView.setSurfaceTexture(output.getSurfaceTexture());
                        updateTransform();
                    }
                });


        ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig.Builder().setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
                .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation()).build();
        final ImageCapture imgCap = new ImageCapture(imageCaptureConfig);

        findViewById(R.id.imgCapture).setOnClickListener(v -> {
            imgCap.takePicture(new ImageCapture.OnImageCapturedListener() {
                @Override
                public void onCaptureSuccess(ImageProxy image, int rotationDegrees) {
                    try {
                      // These are original lines
/*                        bb = image.getPlanes()[0].getBuffer();
                        buf = new byte[bb.remaining()];
                        bb.get(buf);
                        bitmap = BitmapFactory.decodeByteArray(buf, 0, buf.length, null);
*/
                        // Now, we open current image to find the pattern
/*                        file = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+fileSeparator + "OInput.jpg");
                        bmOut = BitmapFactory.decodeFile(file.getPath());

                        Log.i(TAG, "We have bitmap :)");
                        // Now, we save the bitmap onto disk to see what we have from camera :)
                        width = bmOut.getWidth(); height = bmOut.getHeight();
                        Log.i(TAG, "Width =  " + width + "   Height = " + height);
                        // Now, we scale an image to the maxRes :)
                        if (height>width) { // Here, we make the smallest size equals maxRes=271 pixels
                            height=Math.round(maxRes * height / width); width=maxRes;
                        } else {
                            width=Math.round(maxRes * width / height); height=maxRes;
                        }
                        Log.i(TAG, "New width =  " + width + "   New height = " + height);
                        bitmap = Bitmap.createScaledBitmap(bitmap, width*2+3, height*2+3, true); // Here, we scale bitmap to maxRes pixels; true means that we use bilinear filtering for better image
                        for (x = 0; x < width*2+3; x++)
                            for (y = 0; y < height*2+3; y++) {
                                pixel = bitmap.getPixel(x, y);
                                greyImg[x][y] = 0.21f * Color.red(pixel) + 0.72f * Color.green(pixel) + 0.07f * Color.blue(pixel); // We're more sensitive to green than other colors, so green is weighted most heavily
                            }
                        // create output bitmap
                        if (bmOut==null) bmOut = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888); // Here, we use 4 bytes to store ARGB info for every pixel
                        xAxis=0;
                        for (x = 2; x < (width*2+1); x = x + 2) {
                            yAxis=0;
                            for (y = 2; y < (height*2+1); y = y + 2) {
                                greyC[xAxis][yAxis]=0.16f*greyImg[x][y]+0.0025f*(greyImg[x-2][y-2]+greyImg[x+2][y-2]+greyImg[x-2][y+2]+greyImg[x+2][y+2]);
                                greyC[xAxis][yAxis]=greyC[xAxis][yAxis]+0.0125f*(greyImg[x-1][y-2]+greyImg[x+1][y-2]+greyImg[x-1][y+2]+greyImg[x+1][y+2] +   greyImg[x-2][y-1]+greyImg[x+2][y-1]+greyImg[x-2][y+1]+greyImg[x+2][y+1]);
                                greyC[xAxis][yAxis]=greyC[xAxis][yAxis]+0.02f*(greyImg[x][y-2]+greyImg[x-2][y]+greyImg[x+2][y]+greyImg[x][y+2]);
                                greyC[xAxis][yAxis]=greyC[xAxis][yAxis]+0.0625f*(greyImg[x-1][y-1]+greyImg[x+1][y-1]+greyImg[x-1][y+1]+greyImg[x+1][y+1]);
                                greyC[xAxis][yAxis]=greyC[xAxis][yAxis]+0.1f*(greyImg[x][y-1]+greyImg[x-1][y]+greyImg[x+1][y]+greyImg[x][y+1]);
                                i = Math.round(greyC[xAxis][yAxis]);
                                bmOut.setPixel(xAxis, yAxis, Color.argb(255, i, i, i));
                                yAxis++;
                            }
                            xAxis++;
                        }
*/

/*                        greyCInt[0][0] = greyC[0][0];
                        for (y = 1; y < height; y++) greyCInt[0][y] = greyC[0][y] + greyCInt[0][y - 1];
                        for (x = 1; x < width; x++) {
                            greyCInt[x][0] = greyC[x][0] + greyCInt[x - 1][0];
                            for (y = 1; y < height; y++) greyCInt[x][y] = greyC[x][y] + greyCInt[x - 1][y] + greyCInt[x][y - 1] - greyCInt[x - 1][y - 1];
                        }
                        // Now, we have an INTEGRAL image in greyCInt

                        try { // wait for threads to end, other threads are checked only
/*                            t0.join(); // Here, we wait for t0 to successfully finish its work
                            t0 = new ThreadX1(0, greyCInt); // We create a variable t3 of type ThreadX1
                            t0.setPriority(Thread.MAX_PRIORITY);
                            t0.setName("t0");
                            t0.start(); // Here, we start thread with x1=3
                            t1.join(); // Here, we wait for t1 to successfully finish its work
                            t1 = new ThreadX1(1, greyCInt); // We create a variable t1 of type ThreadX1
                            t1.setPriority(Thread.MAX_PRIORITY);
                            t1.setName("t1");
                            t1.start(); // Here, we start thread with x1=1
                            if (!t2.isAlive()) {
                                t2 = new ThreadX1(2, greyCInt); // We create a variable t2 of type ThreadX1
                                t2.setName("t2");
                                t2.start(); // Here, we start thread with x1=2
                            }

                            t2.join(); // Here, we wait for t2 to successfully finish its work
                            t2 = new ThreadX1(2, greyCInt); // We create a variable t2 of type ThreadX1
                            t2.setPriority(Thread.MAX_PRIORITY);
                            t2.setName("t2");
                            t2.start(); // Here, we start thread with x1=2
                            if (!t3.isAlive()) {
                                t3 = new ThreadX1(3, greyCInt); // We create a variable t3 of type ThreadX1
                                t3.setName("t3");
                                t3.start(); // Here, we start thread with x1=3
                            }

*/
/*                            t3.join(); // Here, we wait for t3 to successfully finish its work
                            t3 = new ThreadX1(3, greyCInt); // We create a variable t3 of type ThreadX1
                            t3.setPriority(Thread.MAX_PRIORITY);
                            t3.setName("t3");
                            t3.start(); // Here, we start thread with x1=3
                            if (!t4.isAlive()) {
                                t4 = new ThreadX1(4, greyCInt); // We create a variable t4 of type ThreadX1
                                t4.setName("t4");
                                t4.start(); // Here, we start thread with x1=4
                            }
                            if (!t5.isAlive()) {
                                t5 = new ThreadX1(5, greyCInt); // We create a variable t5 of type ThreadX1
                                t5.setName("t5");
                                t5.start(); // Here, we start thread with x1=5
                            }
                            if (!t6.isAlive()) {
                                t6 = new ThreadX1(6, greyCInt); // We create a variable t6 of type ThreadX1
                                t6.setName("t6");
                                t6.start(); // Here, we start thread with x1=6
                            }
                            if (!t7.isAlive()) {
                                t7 = new ThreadX1(7, greyCInt); // We create a variable t7 of type ThreadX1
                                t7.setName("t7");
                                t7.start(); // Here, we start thread with x1=7
                            }
                        } catch (Exception e) {
                            Log.i(TAG, "Thread was interrupted :(");
                        }
                        startCamera();
                        findViewById(R.id.imgCapture).callOnClick(); // Directly call any attached OnClickListener. Unlike performClick(), this only calls the listener, and does not do any associated clicking actions like reporting an accessibility event.
//                    findViewById(R.id.imgCapture).performClick(); // Call this view's OnClickListener, if it is defined. Performs all normal actions associated with clicking: reporting accessibility event, playing a sound, etc.
//                        startCamera();
*/                    } catch (Exception e) {
                        Log.i(TAG, "Exception  " + e);
                    }
                }

                @Override
                public void onError(ImageCapture.UseCaseError error, String message, @Nullable Throwable cause) {
                    Log.i(TAG, "We have NOT got bitmap :(");
//                    findViewById(R.id.imgCapture).performClick(); // Call this view's OnClickListener, if it is defined. Performs all normal actions associated with clicking: reporting accessibility event, playing a sound, etc.
                }
            });
        });

        //bind to lifecycle:
        CameraX.bindToLifecycle((LifecycleOwner)this, preview, imgCap);
    }

    private void updateTransform(){
        Matrix mx = new Matrix();
        float w = textureView.getMeasuredWidth();
        float h = textureView.getMeasuredHeight();

        float cX = w / 2f;
        float cY = h / 2f;

        int rotationDgr;
        int rotation = (int)textureView.getRotation();

        switch(rotation){
            case Surface.ROTATION_0:
                rotationDgr = 0;
                break;
            case Surface.ROTATION_90:
                rotationDgr = 90;
                break;
            case Surface.ROTATION_180:
                rotationDgr = 180;
                break;
            case Surface.ROTATION_270:
                rotationDgr = 270;
                break;
            default:
                return;
        }

        mx.postRotate((float)rotationDgr, cX, cY);
        textureView.setTransform(mx);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if(requestCode == REQUEST_CODE_PERMISSIONS){
            if(allPermissionsGranted()){
                startCamera();
            } else{
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private boolean allPermissionsGranted(){

        for(String permission : REQUIRED_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }

    public void startFREAK(){
        Log.i(TAG, "Time at the beginning = " + Calendar.getInstance().getTime());
        // Dmytro0.jpg: Time at the beginning = Mon Jan 24 14:55:05 GMT+02:00 2022
        // Time at the end = Mon Jan 24 15:00:27 GMT+02:00 2022
        // Dali20.jpg: Time at the beginning = Mon Jan 24 15:32:22 GMT+02:00 2022       Time at the beginning = Mon Jan 24 16:00:41 GMT+02:00 2022
        // Time at the end = Mon Jan 24 15:42:04 GMT+02:00 2022         Time at the end = Mon Jan 24 16:06:04 GMT+02:00 2022
        try {
            nk=0; // Number of keypoints equals 0 initially
            // Now, we open current image to find the pattern
            file = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+fileSeparator+"MyFaces"+fileSeparator+"OInput.jpg");
            bmOut = BitmapFactory.decodeFile(file.getPath());

            Log.i(TAG, "We have bitmap :)");
            // Now, we save the bitmap onto disk to see what we have from camera :)
            width = bmOut.getWidth(); height = bmOut.getHeight();
            Log.i(TAG, "Width =  " + width + "   Height = " + height);
            // Now, we scale an image to the maxRes :)
            if (height<width) { // Here, we make the smallest size equals maxRes=271+4 pixels
                height=Math.round(maxRes * height / width); width=maxRes;
            } else {
                width=Math.round(maxRes * width / height); height=maxRes;
            }
            Log.i(TAG, "New width =  " + width + "   New height = " + height);
            bmOut = Bitmap.createScaledBitmap(bmOut, width, height, true); // Here, we scale bitmap to maxRes pixels; true means that we use bilinear filtering for better image
            for (x = 0; x < width; x++)
                for (y = 0; y < height; y++) {
                    // get pixel color
                    pixel = bmOut.getPixel(x, y);
                    greyC[x][y] = 0.21 * Color.red(pixel) + 0.72 * Color.green(pixel) + 0.07 * Color.blue(pixel); // We're more sensitive to green than other colors, so green is weighted most heavily
                }
            // create output bitmap
            bmOut1 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888); // Here, we use 4 bytes to store ARGB info for every pixel
            bmOut2 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bmOut3 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bmOut4 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Log.i(TAG, "bmOut was created :)");
//-----------------------------------------------------------------------------------------------------------------------------------
            // We start calculation of scales for the first octave
            for (x=-radius0;x<=radius0;x++)
                for (y=-radius0;y<=radius0;y++) {
                    maskS[x + radius0][y + radius0] = Math.exp(-(x * x + y * y) / (2.0 * sigma0 * sigma0)) / (2.0 * Math.PI * sigma0 * sigma0);
                    //Log.i(TAG, "Gaussian function for bluring [" +x + "," + y + "]  " + maskS[x + radius0][y + radius0]);
                }
            for (i=radius4;i<width-radius4;i++) { // we use radius4 to boost the performance
                for (j = radius4; j < height - radius4; j++) {
                    octave1000First[i][j] = 0;
                    for (x = -radius0; x <= radius0; x++)
                        for (y = -radius0; y <= radius0; y++)
                            octave1000First[i][j] = octave1000First[i][j] + maskS[x + radius0][y + radius0] * greyC[i + x][j + y];
                    x=(int)Math.round(octave1000First[i][j]);
                    bmOut1.setPixel(i, j, Color.argb(255, x, x, x));
                }
            }
            file = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+fileSeparator+"Temp"+fileSeparator+"Octave"+maxRes+"First.jpg");
            out = new FileOutputStream(file);
            bmOut1.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush(); out.close();

            for (x=-radius1;x<=radius1;x++)
                for (y=-radius1;y<=radius1;y++) {
                    maskS[x + radius1][y + radius1] = Math.exp(-(x * x + y * y) / (2.0 * sigma1 * sigma1)) / (2.0 * Math.PI * sigma1 * sigma1);
                    //Log.i(TAG, "Gaussian function for bluring [" +x + "," + y + "]  " + maskS[x + radius0][y + radius0]);
                }
            for (i=radius4;i<width-radius4;i++) { // we use radius4 to boost the performance
                for (j = radius4; j < height - radius4; j++) {
                    octave1000Second[i][j] = 0;
                    for (x = -radius1; x <= radius1; x++)
                        for (y = -radius1; y <= radius1; y++)
                            octave1000Second[i][j] = octave1000Second[i][j] + maskS[x + radius1][y + radius1] * greyC[i + x][j + y];
                    x=(int)Math.round(octave1000Second[i][j]);
                    bmOut1.setPixel(i, j, Color.argb(255, x, x, x));
                }
            }
            file = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+fileSeparator+"Temp"+fileSeparator+"Octave"+maxRes+"Second.jpg");
            out = new FileOutputStream(file);
            bmOut1.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush(); out.close();

            for (x=-radius2;x<=radius2;x++)
                for (y=-radius2;y<=radius2;y++) {
                    maskS[x + radius2][y + radius2] = Math.exp(-(x * x + y * y) / (2.0 * sigma2 * sigma2)) / (2.0 * Math.PI * sigma2 * sigma2);
                    //Log.i(TAG, "Gaussian function for bluring [" +x + "," + y + "]  " + maskS[x + radius0][y + radius0]);
                }
            for (i=radius4;i<width-radius4;i++) { // we use radius4 to boost the performance
                for (j = radius4; j < height - radius4; j++) {
                    octave1000Third[i][j] = 0;
                    for (x = -radius2; x <= radius2; x++)
                        for (y = -radius2; y <= radius2; y++)
                            octave1000Third[i][j] = octave1000Third[i][j] + maskS[x + radius2][y + radius2] * greyC[i + x][j + y];
                    x=(int)Math.round(octave1000Third[i][j]);
                    bmOut1.setPixel(i, j, Color.argb(255, x, x, x));
                }
            }
            file = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+fileSeparator+"Temp"+fileSeparator+"Octave"+maxRes+"Third.jpg");
            out = new FileOutputStream(file);
            bmOut1.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush(); out.close();

            for (x=-radius3;x<=radius3;x++)
                for (y=-radius3;y<=radius3;y++) {
                    maskS[x + radius3][y + radius3] = Math.exp(-(x * x + y * y) / (2.0 * sigma3 * sigma3)) / (2.0 * Math.PI * sigma3 * sigma3);
                    //Log.i(TAG, "Gaussian function for bluring [" +x + "," + y + "]  " + maskS[x + radius0][y + radius0]);
                }
            for (i=radius4;i<width-radius4;i++) { // we use radius4 to boost the performance
                for (j = radius4; j < height - radius4; j++) {
                    octave1000Fourth[i][j] = 0;
                    for (x = -radius3; x <= radius3; x++)
                        for (y = -radius3; y <= radius3; y++)
                            octave1000Fourth[i][j] = octave1000Fourth[i][j] + maskS[x + radius3][y + radius3] * greyC[i + x][j + y];
                    x=(int)Math.round(octave1000Fourth[i][j]);
                    bmOut1.setPixel(i, j, Color.argb(255, x, x, x));
                }
            }
            file = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+fileSeparator+"Temp"+fileSeparator+"Octave"+maxRes+"Fourth.jpg");
            out = new FileOutputStream(file);
            bmOut1.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush(); out.close();

            for (x=-radius4;x<=radius4;x++)
                for (y=-radius4;y<=radius4;y++) {
                    maskS[x + radius4][y + radius4] = Math.exp(-(x * x + y * y) / (2.0 * sigma4 * sigma4)) / (2.0 * Math.PI * sigma4 * sigma4);
                    //Log.i(TAG, "Gaussian function for bluring [" +x + "," + y + "]  " + maskS[x + radius0][y + radius0]);
                }
            for (i=radius4;i<width-radius4;i++) { // we use radius4 to boost the performance
                for (j = radius4; j < height - radius4; j++) {
                    octave1000Fifth[i][j] = 0;
                    for (x = -radius4; x <= radius4; x++)
                        for (y = -radius4; y <= radius4; y++)
                            octave1000Fifth[i][j] = octave1000Fifth[i][j] + maskS[x + radius4][y + radius4] * greyC[i + x][j + y];
                    x=(int)Math.round(octave1000Fifth[i][j]);
                    bmOut1.setPixel(i, j, Color.argb(255, x, x, x));
                }
            }
            file = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+fileSeparator+"Temp"+fileSeparator+"Octave"+maxRes+"Fifth.jpg");
            out = new FileOutputStream(file);
            bmOut1.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush(); out.close();
            // We finished calculation of scales for the first octave
//-----------------------------------------------------------------------------------------------------------------------------------
            // We start calculation of DoG
            MatrixBorder=radius4; // The maximum border equals maximum radius, i.e. it is 68 for the 512x512 pixel picture
//            for (i=0;i<width;i++)
//                for (j=0;j<height;j++) {
//                    bmOut.setPixel(i, j, Color.argb(A, 255, 255, 255)); // RGB=255 means white color, i.e. image has white border of 17 pixels
//                    bmOut1.setPixel(i, j, Color.argb(A, 255, 255, 255)); // RGB=255 means white color, i.e. image has white border of 17 pixels
//                    bmOut2.setPixel(i, j, Color.argb(A, 255, 255, 255)); // RGB=255 means white color, i.e. image has white border of 17 pixels
//                    bmOut3.setPixel(i, j, Color.argb(A, 255, 255, 255)); // RGB=255 means white color, i.e. image has white border of 17 pixels
//                }
            minFirst=1000; maxFirst=-1000; minSecond=1000; maxSecond=-1000; minThird=1000; maxThird=-1000; minFourth=1000; maxFourth=-1000;
            for (i=MatrixBorder;i<width-MatrixBorder;i++)
                for (j=MatrixBorder;j<height-MatrixBorder;j++) {
                    DoG1000First[i][j]= octave1000First[i][j] - octave1000Second[i][j];
                    if (DoG1000First[i][j]>maxFirst) maxFirst=DoG1000First[i][j];
                    else if (DoG1000First[i][j]<minFirst) minFirst=DoG1000First[i][j];
                    x=(int)Math.round(DoG1000First[i][j]);
                    bmOut1.setPixel(i, j, Color.argb(255, x, x, x));
                    DoG1000Second[i][j]= octave1000Second[i][j] - octave1000Third[i][j];
                    if (DoG1000Second[i][j]>maxSecond) maxSecond=DoG1000Second[i][j];
                    else if (DoG1000Second[i][j]<minSecond) minSecond=DoG1000Second[i][j];
                    x=(int)Math.round(DoG1000Second[i][j]);
                    bmOut2.setPixel(i, j, Color.argb(255, x, x, x));
                    DoG1000Third[i][j]= octave1000Third[i][j] - octave1000Fourth[i][j];
                    if (DoG1000Third[i][j]>maxThird) maxThird=DoG1000Third[i][j];
                    else if (DoG1000Third[i][j]<minThird) minThird=DoG1000Third[i][j];
                    x=(int)Math.round(DoG1000Third[i][j]);
                    bmOut3.setPixel(i, j, Color.argb(255, x, x, x));
                    DoG1000Fourth[i][j]= octave1000Fourth[i][j] - octave1000Fifth[i][j];
                    if (DoG1000Fourth[i][j]>maxFourth) maxFourth=DoG1000Fourth[i][j];
                    else if (DoG1000Fourth[i][j]<minFourth) minFourth=DoG1000Fourth[i][j];
                    x=(int)Math.round(DoG1000Fourth[i][j]);
                    bmOut4.setPixel(i, j, Color.argb(255, x, x, x));
                }
            //Log.i(TAG, "DoG" + maxRes + "First: MAX =  " + maxFirst + "   MIN = " + minFirst);
            //Log.i(TAG, "DoG" + maxRes + "Second: MAX =  " + maxSecond + "   MIN = " + minSecond);
            //Log.i(TAG, "DoG" + maxRes + "Third: MAX =  " + maxThird + "   MIN = " + minThird);
            //Log.i(TAG, "DoG" + maxRes + "Fourth: MAX =  " + maxFourth + "   MIN = " + minFourth);
            file = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+fileSeparator+"Temp"+fileSeparator+"DoG1000First.jpg");
            out = new FileOutputStream(file);
            bmOut1.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush(); out.close();
            file = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+fileSeparator+"Temp"+fileSeparator+"DoG1000Second.jpg");
            out = new FileOutputStream(file);
            bmOut2.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush(); out.close();
            file = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+fileSeparator+"Temp"+fileSeparator+"DoG1000Third.jpg");
            out = new FileOutputStream(file);
            bmOut3.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush(); out.close();
            file = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+fileSeparator+"Temp"+fileSeparator+"DoG1000Fourth.jpg");
            out = new FileOutputStream(file);
            bmOut4.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush(); out.close();
//-----------------------------------------------------------------------------------------------------------------------------------
            // We look for SIFT keypoints
            // The following loop is for the SECOND DoG
            for (i=MatrixBorder+1;i<width-MatrixBorder-1;i++)
                for (j=MatrixBorder+1;j<height-MatrixBorder-1;j++) {
                    if (Math.abs(DoG1000Second[i][j]) >= threshold) {// We exclude extrema of low contrast: |D(X)| must be gt 0.03, here pixels are between 0 and 1
                        // We check for maximum/minimum
                        flagMax = 1; flagMin = 1; max = -1000; min = 1000;
                        for (x = -1; x <= 1; x++)
                            for (y = -1; y <= 1; y++) {
                                if (DoG1000Second[i][j] <= DoG1000First[i + x][j + y]) flagMax = 0;
                                if (DoG1000Second[i][j] >= DoG1000First[i + x][j + y]) flagMin = 0;
                                if (DoG1000First[i + x][j + y] > max) max = DoG1000First[i + x][j + y];
                                else if (DoG1000First[i + x][j + y] < min) min = DoG1000First[i + x][j + y];
                            }
                        if ((flagMax == 1 && DoG1000Second[i][j] > max) || (flagMin == 1 && DoG1000Second[i][j] < min)) {
                            for (x = -1; x <= 1; x++)
                                for (y = -1; y <= 1; y++) {
                                    if (DoG1000Second[i][j] <= DoG1000Third[i + x][j + y]) flagMax = 0;
                                    if (DoG1000Second[i][j] >= DoG1000Third[i + x][j + y]) flagMin = 0;
                                    if (DoG1000Third[i + x][j + y] > max) max = DoG1000Third[i + x][j + y];
                                    else if (DoG1000Third[i + x][j + y] < min) min = DoG1000Third[i + x][j + y];
                                }
                            if ((flagMax == 1 && DoG1000Second[i][j] > max) || (flagMin == 1 && DoG1000Second[i][j] < min)) {
                                for (x = -1; x <= 1; x++)
                                    for (y = -1; y <= 1; y++)
                                        if (x != 0 && y != 0) {
                                            if (DoG1000Second[i][j] <= DoG1000Second[i + x][j + y])
                                                flagMax = 0;
                                            if (DoG1000Second[i][j] >= DoG1000Second[i + x][j + y])
                                                flagMin = 0;
                                            if (DoG1000Second[i + x][j + y] > max)
                                                max = DoG1000Second[i + x][j + y];
                                            else if (DoG1000Second[i + x][j + y] < min)
                                                min = DoG1000Second[i + x][j + y];
                                        }
                                if ((flagMax == 1 && DoG1000Second[i][j] > max) || (flagMin == 1 && DoG1000Second[i][j] < min)) {
                                    // Now, we eliminate the edges
                                    Hessian[0][0] = DoG1000Second[i + 1][j] + DoG1000Second[i - 1][j] - 2.0 * DoG1000Second[i][j];
                                    Hessian[1][1] = DoG1000Second[i][j + 1] + DoG1000Second[i][j - 1] - 2.0 * DoG1000Second[i][j];
                                    Hessian[0][1] = (DoG1000Second[i + 1][j + 1] - DoG1000Second[i + 1][j - 1] - DoG1000Second[i - 1][j + 1] + DoG1000Second[i - 1][j - 1]) * 0.25;
                                    trace = Hessian[0][0] + Hessian[1][1]; //Trace of a matrix
                                    det = Hessian[0][0] * Hessian[1][1] - Hessian[0][1] * Hessian[0][1]; // Determinant of a matrix
                                    trace = trace * trace / det;
                                    // It was demonstrated that r = 10 is a good ratio, i.e. sqr(r+1)/r=121/10=12.1
                                    if (trace < 12.1 && trace > 0) {// r=10 here
                                        keypoints1000[nk][0]=i; keypoints1000[nk][1]=j;
                                        nk++;
                                        for (x = -10; x <= 10; x++) {
                                            bmOut.setPixel(i + x, j - 1, Color.argb(255, 255, 0, 255)); // fuchsia: rgb(255,0,255)
                                            bmOut.setPixel(i + x, j, Color.argb(255, 255, 0, 255)); // fuchsia: rgb(255,0,255)
                                            bmOut.setPixel(i + x, j + 1, Color.argb(255, 255, 0, 255)); // fuchsia: rgb(255,0,255)
                                            bmOut.setPixel(i - 1, j + x, Color.argb(255, 255, 0, 255)); // fuchsia: rgb(255,0,255)
                                            bmOut.setPixel(i, j + x, Color.argb(255, 255, 0, 255)); // fuchsia: rgb(255,0,255)
                                            bmOut.setPixel(i + 1, j + x, Color.argb(255, 255, 0, 255)); // fuchsia: rgb(255,0,255)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            // The following loop is for the THIRD DoG
            for (i=MatrixBorder+1;i<width-MatrixBorder-1;i++)
                for (j=MatrixBorder+1;j<height-MatrixBorder-1;j++) {
                    if (Math.abs(DoG1000Third[i][j]) >= threshold) {// We exclude extrema of low contrast: |D(X)| must be gt 0.03, here pixels are between 0 and 1
                        // First, we check for maximum/minimum
                        flagMax = 1; flagMin = 1;
                        max = -1000; min = 1000;
                        for (x = -1; x <= 1; x++)
                            for (y = -1; y <= 1; y++) {
                                if (DoG1000Third[i][j] <= DoG1000Second[i + x][j + y]) flagMax = 0;
                                if (DoG1000Third[i][j] >= DoG1000Second[i + x][j + y]) flagMin = 0;
                                if (DoG1000Second[i + x][j + y] > max) max = DoG1000Second[i + x][j + y];
                                else if (DoG1000Second[i + x][j + y] < min) min = DoG1000Second[i + x][j + y];
                            }
                        if ((flagMax == 1 && DoG1000Third[i][j] > max) || (flagMin == 1 && DoG1000Third[i][j] < min)) {
                            for (x = -1; x <= 1; x++)
                                for (y = -1; y <= 1; y++) {
                                    if (DoG1000Third[i][j] <= DoG1000Fourth[i + x][j + y]) flagMax = 0;
                                    if (DoG1000Third[i][j] >= DoG1000Fourth[i + x][j + y]) flagMin = 0;
                                    if (DoG1000Fourth[i + x][j + y] > max) max = DoG1000Fourth[i + x][j + y];
                                    else if (DoG1000Fourth[i + x][j + y] < min) min = DoG1000Fourth[i + x][j + y];
                                }
                            if ((flagMax == 1 && DoG1000Third[i][j] > max) || (flagMin == 1 && DoG1000Third[i][j] < min)) {
                                for (x = -1; x <= 1; x++)
                                    for (y = -1; y <= 1; y++)
                                        if (x != 0 && y != 0) {
                                            if (DoG1000Third[i][j] <= DoG1000Third[i + x][j + y]) flagMax = 0;
                                            if (DoG1000Third[i][j] >= DoG1000Third[i + x][j + y]) flagMin = 0;
                                            if (DoG1000Third[i + x][j + y] > max) max = DoG1000Third[i + x][j + y];
                                            else if (DoG1000Third[i + x][j + y] < min) min = DoG1000Third[i + x][j + y];
                                        }
                                if ((flagMax == 1 && DoG1000Third[i][j] > max) || (flagMin == 1 && DoG1000Third[i][j] < min)) {
                                    // Now, we eliminate the edges
                                    Hessian[0][0] = DoG1000Third[i + 1][j] + DoG1000Third[i - 1][j] - 2.0 * DoG1000Third[i][j];
                                    Hessian[1][1] = DoG1000Third[i][j + 1] + DoG1000Third[i][j - 1] - 2.0 * DoG1000Third[i][j];
                                    Hessian[0][1] = (DoG1000Third[i + 1][j + 1] - DoG1000Third[i + 1][j - 1] - DoG1000Third[i - 1][j + 1] + DoG1000Third[i - 1][j - 1]) * 0.25;
                                    trace = Hessian[0][0] + Hessian[1][1]; //Trace of a matrix
                                    det = Hessian[0][0] * Hessian[1][1] - Hessian[0][1] * Hessian[0][1]; // Determinant of a matrix
                                    trace = trace * trace / det;
                                    // It was demonstrated that r = 10 is a good ratio, i.e. sqr(r+1)/r=121/10=12.1
                                    if (trace < 12.1 && trace > 0) {// r=10 here
                                        keypoints1000[nk][0]=i; keypoints1000[nk][1]=j;
                                        nk++;
                                        for (x = -10; x <= 10; x++) {
                                            bmOut.setPixel(i + x, j - 1, Color.argb(255, 64, 224, 208)); // turquoise color: rgb(64,224,208)
                                            bmOut.setPixel(i + x, j, Color.argb(255, 64, 224, 208)); // turquoise color: rgb(64,224,208)
                                            bmOut.setPixel(i + x, j + 1, Color.argb(255, 64, 224, 208)); // turquoise color: rgb(64,224,208)
                                            bmOut.setPixel(i - 1, j + x, Color.argb(255, 64, 224, 208)); // turquoise color: rgb(64,224,208)
                                            bmOut.setPixel(i, j + x, Color.argb(255, 64, 224, 208)); // turquoise color: rgb(64,224,208)
                                            bmOut.setPixel(i + 1, j + x, Color.argb(255, 64, 224, 208)); // turquoise color: rgb(64,224,208)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            file = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+fileSeparator+"Temp"+fileSeparator+"KeyPoints"+ maxRes + ".jpg");
            out = new FileOutputStream(file);
            bmOut.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush(); out.close();

            // We write info about keypoints into the text file
            file = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+fileSeparator+"Temp"+fileSeparator+"KeyPoints"+maxRes+".txt");
            file.createNewFile();
            //second argument of FileOutputStream constructor indicates whether to append or create new file if one exists
            FileOutputStream outputStream = new FileOutputStream(file, false);
            PrintWriter writer = new PrintWriter(outputStream);
            writer.println(Integer.toString(nk)); writer.println("");
            for (i=0;i<nk;i++) { // nk is the number of keypoints
                writer.println(keypoints1000[i][0]); // X coordinate
                writer.println(keypoints1000[i][1]); // Y coordinate
                writer.println("");
            }
            writer.flush(); writer.close();

            Log.i(TAG, "Five scales of octave done. Resolution : " + maxRes);
        } catch (Exception e) {
            Log.i(TAG, "Exception  " + e);
        }
        Log.i(TAG, "Time at the end = " + Calendar.getInstance().getTime());
    }

    public void DisplayKeyPoints()
    {
        try {
            // Now, we open current image to put marks for keypoints
            file = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+fileSeparator+"MyFaces"+fileSeparator+"OInput.jpg");
            bmOut = BitmapFactory.decodeFile(file.getPath());
            Log.i(TAG, "We have bitmap :)");
            // Now, we save the bitmap onto disk to see what we have from camera :)
            width = bmOut.getWidth(); height = bmOut.getHeight();
            Log.i(TAG, "Width =  " + width + "   Height = " + height);
            // Now, we scale an image to the maxRes :)
            maxRes = 1360;
            if (height<width) { // Here, we make the smallest size equals maxRes=271+4 pixels
                height=Math.round(maxRes * height / width); width=maxRes;
            } else {
                width=Math.round(maxRes * width / height); height=maxRes;
            }
            Log.i(TAG, "New width =  " + width + "   New height = " + height);
            bmOut = Bitmap.createScaledBitmap(bmOut, width, height, true); // Here, we scale bitmap to maxRes pixels; true means that we use bilinear filtering for better image
            for (x = 0; x < width; x++) // We don't use additional Gaussina bluring-filtering since we scale an image (in fact, this is kinda filtering), as well as scaling has bilinear filtering
                for (y = 0; y < height; y++) {
                    // get pixel color
                    pixel = bmOut.getPixel(x, y);
                    greyC[x][y] = 0.21 * Color.red(pixel) + 0.72 * Color.green(pixel) + 0.07 * Color.blue(pixel); // We're more sensitive to green than other colors, so green is weighted most heavily
                    i = (int)Math.round(greyC[x][y]);
                    bmOut.setPixel(x, y, Color.argb(255, i, i, i));
                }
            file = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+fileSeparator+"Temp"+fileSeparator+"CameraBlured.jpg");
            out = new FileOutputStream(file);
            bmOut.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush(); out.close();
            Log.i(TAG, "Temporary file was saved :)");

            Log.i(TAG, "Final width =  " + width + "   Final height = " + height);
            i=470; j=561; // x7
//            i=470; j=561;
            for (x = -10; x <= 10; x++) {
                bmOut.setPixel(i + x, j - 1, Color.argb(255, 64, 224, 208)); // turquoise color: rgb(64,224,208)
                bmOut.setPixel(i + x, j, Color.argb(255, 64, 224, 208)); // turquoise color: rgb(64,224,208)
                bmOut.setPixel(i + x, j + 1, Color.argb(255, 64, 224, 208)); // turquoise color: rgb(64,224,208)
                bmOut.setPixel(i - 1, j + x, Color.argb(255, 64, 224, 208)); // turquoise color: rgb(64,224,208)
                bmOut.setPixel(i, j + x, Color.argb(255, 64, 224, 208)); // turquoise color: rgb(64,224,208)
                bmOut.setPixel(i + 1, j + x, Color.argb(255, 64, 224, 208)); // turquoise color: rgb(64,224,208)
            }
            i=469; j=312; // x10
//            i=400; j=312;
            for (x = -10; x <= 10; x++) {
                bmOut.setPixel(i + x, j - 1, Color.argb(255, 64, 224, 208)); // turquoise color: rgb(64,224,208)
                bmOut.setPixel(i + x, j, Color.argb(255, 64, 224, 208)); // turquoise color: rgb(64,224,208)
                bmOut.setPixel(i + x, j + 1, Color.argb(255, 64, 224, 208)); // turquoise color: rgb(64,224,208)
                bmOut.setPixel(i - 1, j + x, Color.argb(255, 64, 224, 208)); // turquoise color: rgb(64,224,208)
                bmOut.setPixel(i, j + x, Color.argb(255, 64, 224, 208)); // turquoise color: rgb(64,224,208)
                bmOut.setPixel(i + 1, j + x, Color.argb(255, 64, 224, 208)); // turquoise color: rgb(64,224,208)
            }

            xk[7]=470; yk[7]=561; xk[10]=469; yk[10]=312;
//            xk[7]=470; yk[7]=561; xk[10]=420; yk[10]=312;
            sigma0=(xk[7]-xk[10])/3; sigma1=(yk[7]-yk[10])/3;
            xk[6]=xk[7]+sigma0; yk[6]=yk[7]+sigma1;
            xk[0]=xk[6]-sigma1; yk[0]=yk[6]+sigma0;
// First, we have to check the borders: MUST be positive and less than width and height
// Top left corner
            xk[25]=xk[0]-sigma1+sigma0; yk[25]=yk[0]+sigma0+sigma1;
            if (xk[25]>0 && yk[25]>0 && xk[25]<=width && yk[25]<=height) {
                xk[11] = xk[10] - sigma0; yk[11] = yk[10] - sigma1;
                xk[5] = xk[11] - sigma1; yk[5] = yk[11] + sigma0;
// Top right corner
                xk[26] = xk[5] - sigma1 - sigma0; yk[26] = yk[5] + sigma0 - sigma1;
                if (xk[26]>0 && yk[26]>0 && xk[26]<=width && yk[26]<=height) {
// Bottom left corner
                    xk[27] = xk[6] + 5.0*sigma1 + sigma0; yk[27] = yk[6] - 5.0*sigma0 + sigma1;
                    if (xk[27] > 0 && yk[27] > 0 && xk[27] <= width && yk[27] <= height) {
// Bottom right corner
                        xk[28] = xk[11] + 5.0*sigma1 - sigma0; yk[28] = yk[11] - 5.0*sigma0 - sigma1;
                        if (xk[28] > 0 && yk[28] > 0 && xk[28] <= width && yk[28] <= height) {
                            XkYk1st();
                            for (pixel=0;pixel<18;pixel++) { // We calculate average intensities and draw circles here
                                radius0 = (int) Math.round(xk[pixel] - sigma4); // The top coordinate X within the circle, 0 keypoint
                                if (radius0 < Math.round(xk[pixel] - sigma4)) radius0++; // We adjust the coordinate X to be inside the circle
                                radius1 = (int) Math.round(yk[pixel] - sigma4); // The most left coordinate Y within the circle, 0 keypoint
                                if (radius1 < Math.round(yk[pixel] - sigma4)) radius1++; // We adjust the coordinate Y to be inside the circle
                                radius2 = (int) Math.round(xk[pixel] + sigma4); // The bottom coordinate X within the circle, 0 keypoint
                                if (radius2 < Math.round(xk[pixel] + sigma4)) radius2++; // We adjust the coordinate X to be outside the circle - nearest largest integer: to speed the following loop
                                radius3 = (int) Math.round(yk[pixel] + sigma4); // The most right Y coordinate within the circle, 0 keypoint
                                if (radius3 < Math.round(yk[pixel] + sigma4)) radius3++; // We adjust the coordinate Y to be outside the circle - nearest largest integer: to speed the following loop
                                IC[pixel] = 0; // Average intensity of the circle around keypoint 0
                                k = 0; // Number of the pixels inside the circle around keypoint
                                for (i = radius0; i < radius2; i++)
                                    for (j = radius1; j < radius3; j++)
                                        if (Math.sqrt((i - xk[pixel]) * (i - xk[pixel]) + (j - yk[pixel]) * (j - yk[pixel])) <= sigma4) {
                                            k++;
                                            IC[pixel] = IC[pixel] + greyC[i][j];
                                            bmOut.setPixel(i, j, Color.argb(255, 64, 224, 208)); // turquoise color: rgb(64,224,208)
                                        }
                                if (k != 0) IC[pixel] = IC[pixel] / k;
                                Log.i(TAG, "Average intensity for keypoint " + pixel +" : " + IC[pixel] + " ;  Number of pixels: " + k);
                            }
                            sigma4=sigma4*1.5; // radius of the circle around the point, 1st and 2nd levels
                            for (pixel=18;pixel<22;pixel++) { // We calculate average intensities and draw circles here
                                radius0 = (int) Math.round(xk[pixel] - sigma4); // The top coordinate X within the circle, 0 keypoint
                                if (radius0 < Math.round(xk[pixel] - sigma4)) radius0++; // We adjust the coordinate X to be inside the circle
                                radius1 = (int) Math.round(yk[pixel] - sigma4); // The most left coordinate Y within the circle, 0 keypoint
                                if (radius1 < Math.round(yk[pixel] - sigma4)) radius1++; // We adjust the coordinate Y to be inside the circle
                                radius2 = (int) Math.round(xk[pixel] + sigma4); // The bottom coordinate X within the circle, 0 keypoint
                                if (radius2 < Math.round(xk[pixel] + sigma4)) radius2++; // We adjust the coordinate X to be outside the circle - nearest largest integer: to speed the following loop
                                radius3 = (int) Math.round(yk[pixel] + sigma4); // The most right Y coordinate within the circle, 0 keypoint
                                if (radius3 < Math.round(yk[pixel] + sigma4)) radius3++; // We adjust the coordinate Y to be outside the circle - nearest largest integer: to speed the following loop
                                IC[pixel] = 0; // Average intensity of the circle around keypoint 0
                                k = 0; // Number of the pixels inside the circle around keypoint
                                for (i = radius0; i < radius2; i++)
                                    for (j = radius1; j < radius3; j++)
                                        if (Math.sqrt((i - xk[pixel]) * (i - xk[pixel]) + (j - yk[pixel]) * (j - yk[pixel])) <= sigma4) {
                                            k++;
                                            IC[pixel] = IC[pixel] + greyC[i][j];
                                            bmOut.setPixel(i, j, Color.argb(255, 64, 224, 208)); // turquoise color: rgb(64,224,208)
                                        }
                                if (k != 0) IC[pixel] = IC[pixel] / k;
                                Log.i(TAG, "Average intensity for keypoint " + pixel +" : " + IC[pixel] + " ;  Number of pixels: " + k);
                            }
                            sigma4=sigma4*1.33333333333333333; // radius of the circle around the point, 1st and 2nd levels
                            for (pixel=22;pixel<25;pixel++) { // We calculate average intensities and draw circles here
                                radius0 = (int) Math.round(xk[pixel] - sigma4); // The top coordinate X within the circle, 0 keypoint
                                if (radius0 < Math.round(xk[pixel] - sigma4)) radius0++; // We adjust the coordinate X to be inside the circle
                                radius1 = (int) Math.round(yk[pixel] - sigma4); // The most left coordinate Y within the circle, 0 keypoint
                                if (radius1 < Math.round(yk[pixel] - sigma4)) radius1++; // We adjust the coordinate Y to be inside the circle
                                radius2 = (int) Math.round(xk[pixel] + sigma4); // The bottom coordinate X within the circle, 0 keypoint
                                if (radius2 < Math.round(xk[pixel] + sigma4)) radius2++; // We adjust the coordinate X to be outside the circle - nearest largest integer: to speed the following loop
                                radius3 = (int) Math.round(yk[pixel] + sigma4); // The most right Y coordinate within the circle, 0 keypoint
                                if (radius3 < Math.round(yk[pixel] + sigma4)) radius3++; // We adjust the coordinate Y to be outside the circle - nearest largest integer: to speed the following loop
                                IC[pixel] = 0; // Average intensity of the circle around keypoint 0
                                k = 0; // Number of the pixels inside the circle around keypoint
                                for (i = radius0; i < radius2; i++)
                                    for (j = radius1; j < radius3; j++)
                                        if (Math.sqrt((i - xk[pixel]) * (i - xk[pixel]) + (j - yk[pixel]) * (j - yk[pixel])) <= sigma4) {
                                            k++;
                                            IC[pixel] = IC[pixel] + greyC[i][j];
                                            bmOut.setPixel(i, j, Color.argb(255, 64, 224, 208)); // turquoise color: rgb(64,224,208)
                                        }
                                if (k != 0) IC[pixel] = IC[pixel] / k;
                                Log.i(TAG, "Average intensity for keypoint " + pixel +" : " + IC[pixel] + " ;  Number of pixels: " + k);
                            }
                            // We start 2nd level here -----------------------------------------------------------------
                            XkYk2nd();
                            for (pixel=79;pixel<83;pixel++) { // We calculate average intensities and draw circles here
                                radius0 = (int) Math.round(xk[pixel] - sigma4); // The top coordinate X within the circle, 0 keypoint
                                if (radius0 < Math.round(xk[pixel] - sigma4)) radius0++; // We adjust the coordinate X to be inside the circle
                                radius1 = (int) Math.round(yk[pixel] - sigma4); // The most left coordinate Y within the circle, 0 keypoint
                                if (radius1 < Math.round(yk[pixel] - sigma4)) radius1++; // We adjust the coordinate Y to be inside the circle
                                radius2 = (int) Math.round(xk[pixel] + sigma4); // The bottom coordinate X within the circle, 0 keypoint
                                if (radius2 < Math.round(xk[pixel] + sigma4)) radius2++; // We adjust the coordinate X to be outside the circle - nearest largest integer: to speed the following loop
                                radius3 = (int) Math.round(yk[pixel] + sigma4); // The most right Y coordinate within the circle, 0 keypoint
                                if (radius3 < Math.round(yk[pixel] + sigma4)) radius3++; // We adjust the coordinate Y to be outside the circle - nearest largest integer: to speed the following loop
                                IC[pixel] = 0; // Average intensity of the circle around keypoint 0
                                k = 0; // Number of the pixels inside the circle around keypoint
                                for (i = radius0; i < radius2; i++)
                                    for (j = radius1; j < radius3; j++)
                                        if (Math.sqrt((i - xk[pixel]) * (i - xk[pixel]) + (j - yk[pixel]) * (j - yk[pixel])) <= sigma4) {
                                            k++;
                                            IC[pixel] = IC[pixel] + greyC[i][j];
                                            bmOut.setPixel(i, j, Color.argb(255, 64, 224, 208)); // turquoise color: rgb(64,224,208)
                                        }
                                if (k != 0) IC[pixel] = IC[pixel] / k;
                                Log.i(TAG, "Average intensity for keypoint " + pixel +" : " + IC[pixel] + " ;  Number of pixels: " + k);
                            }
                            sigma4=sigma4*0.75; // radius of the circle around the point, 1st and 2nd levels
                            for (pixel=62;pixel<79;pixel++) { // We calculate average intensities and draw circles here
                                radius0 = (int) Math.round(xk[pixel] - sigma4); // The top coordinate X within the circle, 0 keypoint
                                if (radius0 < Math.round(xk[pixel] - sigma4)) radius0++; // We adjust the coordinate X to be inside the circle
                                radius1 = (int) Math.round(yk[pixel] - sigma4); // The most left coordinate Y within the circle, 0 keypoint
                                if (radius1 < Math.round(yk[pixel] - sigma4)) radius1++; // We adjust the coordinate Y to be inside the circle
                                radius2 = (int) Math.round(xk[pixel] + sigma4); // The bottom coordinate X within the circle, 0 keypoint
                                if (radius2 < Math.round(xk[pixel] + sigma4)) radius2++; // We adjust the coordinate X to be outside the circle - nearest largest integer: to speed the following loop
                                radius3 = (int) Math.round(yk[pixel] + sigma4); // The most right Y coordinate within the circle, 0 keypoint
                                if (radius3 < Math.round(yk[pixel] + sigma4)) radius3++; // We adjust the coordinate Y to be outside the circle - nearest largest integer: to speed the following loop
                                IC[pixel] = 0; // Average intensity of the circle around keypoint 0
                                k = 0; // Number of the pixels inside the circle around keypoint
                                for (i = radius0; i < radius2; i++)
                                    for (j = radius1; j < radius3; j++)
                                        if (Math.sqrt((i - xk[pixel]) * (i - xk[pixel]) + (j - yk[pixel]) * (j - yk[pixel])) <= sigma4) {
                                            k++;
                                            IC[pixel] = IC[pixel] + greyC[i][j];
                                            bmOut.setPixel(i, j, Color.argb(255, 64, 224, 208)); // turquoise color: rgb(64,224,208)
                                        }
                                if (k != 0) IC[pixel] = IC[pixel] / k;
                                Log.i(TAG, "Average intensity for keypoint " + pixel +" : " + IC[pixel] + " ;  Number of pixels: " + k);
                            }
                            sigma4=sigma4*0.6666666666666666666; // radius of the circle around the point, 1st and 2nd levels
                            for (pixel=25;pixel<62;pixel++) { // We calculate average intensities and draw circles here
                                radius0 = (int) Math.round(xk[pixel] - sigma4); // The top coordinate X within the circle, 0 keypoint
                                if (radius0 < Math.round(xk[pixel] - sigma4)) radius0++; // We adjust the coordinate X to be inside the circle
                                radius1 = (int) Math.round(yk[pixel] - sigma4); // The most left coordinate Y within the circle, 0 keypoint
                                if (radius1 < Math.round(yk[pixel] - sigma4)) radius1++; // We adjust the coordinate Y to be inside the circle
                                radius2 = (int) Math.round(xk[pixel] + sigma4); // The bottom coordinate X within the circle, 0 keypoint
                                if (radius2 < Math.round(xk[pixel] + sigma4)) radius2++; // We adjust the coordinate X to be outside the circle - nearest largest integer: to speed the following loop
                                radius3 = (int) Math.round(yk[pixel] + sigma4); // The most right Y coordinate within the circle, 0 keypoint
                                if (radius3 < Math.round(yk[pixel] + sigma4)) radius3++; // We adjust the coordinate Y to be outside the circle - nearest largest integer: to speed the following loop
                                IC[pixel] = 0; // Average intensity of the circle around keypoint 0
                                k = 0; // Number of the pixels inside the circle around keypoint
                                for (i = radius0; i < radius2; i++)
                                    for (j = radius1; j < radius3; j++)
                                        if (Math.sqrt((i - xk[pixel]) * (i - xk[pixel]) + (j - yk[pixel]) * (j - yk[pixel])) <= sigma4) {
                                            k++;
                                            IC[pixel] = IC[pixel] + greyC[i][j];
                                            bmOut.setPixel(i, j, Color.argb(255, 64, 224, 208)); // turquoise color: rgb(64,224,208)
                                        }
                                if (k != 0) IC[pixel] = IC[pixel] / k;
                                Log.i(TAG, "Average intensity for keypoint " + pixel +" : " + IC[pixel] + " ;  Number of pixels: " + k);
                            }
                            for (k = 0; k < 83; k++) { // We draw crosses here
                                i = (int)Math.round(xk[k]);
                                j = (int)Math.round(yk[k]);
                                for (x = -10; x <= 10; x++) {
                                    bmOut.setPixel(i + x, j - 1, Color.argb(255, 255, 0, 255)); // fuchsia: rgb(255,0,255)
                                    bmOut.setPixel(i + x, j, Color.argb(255, 255, 0, 255)); // fuchsia: rgb(255,0,255)
                                    bmOut.setPixel(i + x, j + 1, Color.argb(255, 255, 0, 255)); // fuchsia: rgb(255,0,255)
                                    bmOut.setPixel(i - 1, j + x, Color.argb(255, 255, 0, 255)); // fuchsia: rgb(255,0,255)
                                    bmOut.setPixel(i, j + x, Color.argb(255, 255, 0, 255)); // fuchsia: rgb(255,0,255)
                                    bmOut.setPixel(i + 1, j + x, Color.argb(255, 255, 0, 255)); // fuchsia: rgb(255,0,255)
                                }
                            }
// We sort all differences here:
                            Log.i(TAG, "1st level:");
                            for (i=0;i<25;i++) { // In this loop, we gonna sort the differences, 1st level
                                for (j=0;j<25;j++) { // In this loop, we calculate differences, 1st level
                                    ICdif[i][j] = j;
                                    ICdifDouble[i][j] = IC[i] - IC[j];
                                }
                                flagMin=1;
                                while (flagMin==1){
                                    flagMin=0;
                                    for (j=0; j<24; j++)
                                        if (Math.abs(ICdifDouble[i][j])<Math.abs(ICdifDouble[i][j+1])) {
                                            det = ICdifDouble[i][j]; ICdifDouble[i][j] = ICdifDouble[i][j+1]; ICdifDouble[i][j+1]=det;
                                            k=ICdif[i][j]; ICdif[i][j]=ICdif[i][j+1]; ICdif[i][j+1]=k;
                                            flagMin=1;
                                        }
                                }
                                Log.i(TAG, i + " : " + ICdif[i][0] + "  " + ICdifDouble[i][0] + "  " + ICdif[i][1] + "  " + ICdifDouble[i][1] + "  " + ICdif[i][2] + "  " + ICdifDouble[i][2] + "  " + ICdif[i][3] + "  " + ICdifDouble[i][3] + "  " + ICdif[i][4] + "  " + ICdifDouble[i][4] + "  " + ICdif[i][5] + "  " + ICdifDouble[i][5] + "  " + ICdif[i][6] + "  " + ICdifDouble[i][6] + "  " + ICdif[i][7] + "  " + ICdifDouble[i][7] + "  " + ICdif[i][8] + "  " + ICdifDouble[i][8] + "  " + ICdif[i][9] + "  " + ICdifDouble[i][9] + ICdif[i][10] + "  " + ICdifDouble[i][10] + "  " + ICdif[i][11] + "  " + ICdifDouble[i][11] + "  " + ICdif[i][12] + "  " + ICdifDouble[i][12] + "  " + ICdif[i][13] + "  " + ICdifDouble[i][13] + "  " + ICdif[i][14] + "  " + ICdifDouble[i][14] + "  " + ICdif[i][15] + "  " + ICdifDouble[i][15] + "  " + ICdif[i][16] + "  " + ICdifDouble[i][16] + "  " + ICdif[i][17] + "  " + ICdifDouble[i][17] + "  " + ICdif[i][18] + "  " + ICdifDouble[i][18] + "  " + ICdif[i][19] + "  " + ICdifDouble[i][19]);
                            }
                            Log.i(TAG, "2nd level:");
                            for (i=25;i<83;i++) { // In this loop, we gonna sort the differences, 2nd level
                                for (j = 25; j < 83; j++) { // In this loop, we calculate differences, 2nd level
                                    ICdif[i][j] = j;
                                    ICdifDouble[i][j] = IC[i] - IC[j];
                                }
                                flagMin = 1;
                                while (flagMin == 1) {
                                    flagMin = 0;
                                    for (j = 25; j < 82; j++)
                                        if (Math.abs(ICdifDouble[i][j]) < Math.abs(ICdifDouble[i][j + 1])) {
                                            det = ICdifDouble[i][j]; ICdifDouble[i][j] = ICdifDouble[i][j + 1]; ICdifDouble[i][j + 1] = det;
                                            k = ICdif[i][j]; ICdif[i][j] = ICdif[i][j + 1]; ICdif[i][j + 1] = k;
                                            flagMin = 1;
                                        }
                                }
                                Log.i(TAG, i + " : " + ICdif[i][25] + "  " + ICdifDouble[i][25] + "  " + ICdif[i][26] + "  " + ICdifDouble[i][26] + "  " + ICdif[i][27] + "  " + ICdifDouble[i][27] + "  " + ICdif[i][28] + "  " + ICdifDouble[i][28] + "  " + ICdif[i][29] + "  " + ICdifDouble[i][29] + "  " + ICdif[i][30] + "  " + ICdifDouble[i][30] + "  " + ICdif[i][31] + "  " + ICdifDouble[i][31] + "  " + ICdif[i][32] + "  " + ICdifDouble[i][32] + "  " + ICdif[i][33] + "  " + ICdifDouble[i][33] + "  " + ICdif[i][34] + "  " + ICdifDouble[i][34] + "  " + ICdif[i][35] + "  " + ICdifDouble[i][35] + "  " + ICdif[i][36] + "  " + ICdifDouble[i][36] + "  " + ICdif[i][37] + "  " + ICdifDouble[i][37] + "  " + ICdif[i][38] + "  " + ICdifDouble[i][38] + "  " + ICdif[i][39] + "  " + ICdifDouble[i][39] + "  " + ICdif[i][40] + "  " + ICdifDouble[i][40] + "  " + ICdif[i][41] + "  " + ICdifDouble[i][41] + "  " + ICdif[i][42] + "  " + ICdifDouble[i][42] + "  " + ICdif[i][43] + "  " + ICdifDouble[i][43] + "  " + ICdif[i][44] + "  " + ICdifDouble[i][44]);
                            }
                        } else Log.i(TAG, "Descriptor size is out of the image size");
                    } else Log.i(TAG, "Descriptor size is out of the image size");
                } else Log.i(TAG, "Descriptor size is out of the image size");
            } else Log.i(TAG, "Descriptor size is out of the image size");
            file = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+fileSeparator+"Temp"+fileSeparator+"ImageWithKeyPoints"+ maxRes + ".jpg");
            out = new FileOutputStream(file);
            bmOut.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush(); out.close();

            Log.i(TAG, "Marks were added into the image. Resolution : " + maxRes);
// ------------------------------------------------------------------------------------------------------------------------------------------------------
// We start analysis of new file here with SIFT
        maxRes = 180;
        sigma0=0.353553; radius0=2; // radius0 is the radius of the matrix for the Gaussian blur for the current scale
        sigma1=0.5; radius1=2; // radius1 is the radius of the matrix for the Gaussian blur for the current scale
        sigma2=0.707107; radius2=3; // radius2 is the radius of the matrix for the Gaussian blur for the current scale
        sigma3=1; radius3=4; // radius3 is the radius of the matrix for the Gaussian blur for the current scale
        sigma4=1.414214; radius4=5; // radius4 is the radius of the matrix for the Gaussian blur for the maximum scale
        // Now, we open current image to find the pattern
        file = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+fileSeparator + "Dmytro20.jpg");
        bmOut = BitmapFactory.decodeFile(file.getPath());

        Log.i(TAG, "We have bitmap :)");
        // Now, we save the bitmap onto disk to see what we have from camera :)
        width = bmOut.getWidth(); height = bmOut.getHeight();
        Log.i(TAG, "Width =  " + width + "   Height = " + height);
        // Now, we scale an image to the maxRes :)
        if (height<width) { // Here, we make the smallest size equals maxRes=271+4 pixels
            height=Math.round(maxRes * height / width); width=maxRes;
        } else {
            width=Math.round(maxRes * width / height); height=maxRes;
        }
        Log.i(TAG, "New width =  " + width + "   New height = " + height);
        bmOut = Bitmap.createScaledBitmap(bmOut, width, height, true); // Here, we scale bitmap to maxRes pixels; true means that we use bilinear filtering for better image
        for (x = 0; x < width; x++)
            for (y = 0; y < height; y++) {
                // get pixel color
                pixel = bmOut.getPixel(x, y);
                greyC[x][y] = 0.21 * Color.red(pixel) + 0.72 * Color.green(pixel) + 0.07 * Color.blue(pixel); // We're more sensitive to green than other colors, so green is weighted most heavily
            }
        // create output bitmap
//        bmOut1 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888); // Here, we use 4 bytes to store ARGB info for every pixel
//        bmOut2 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
//        bmOut3 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
//        bmOut4 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Log.i(TAG, "bmOut was created :)");
//-----------------------------------------------------------------------------------------------------------------------------------
        // We start calculation of octaves
            t0.start(); t1.start(); t2.start(); t3.start();
            // t4:
            // we use radius4 to boost the performance
            for (i=radius4;i<width-radius4;i++)
                for (j = radius4; j < height - radius4; j++) {
                    octave1000Fifth[i][j] = 0;
                    for (x = -radius4; x <= radius4; x++)
                        for (y = -radius4; y <= radius4; y++)
                            octave1000Fifth[i][j] = octave1000Fifth[i][j] + maskS4[x + radius4][y + radius4] * greyC[i + x][j + y];
//                    x=(int)Math.round(octave1000Fifth[i][j]);
//                bmOut1.setPixel(i, j, Color.argb(255, x, x, x));
                }
//        file = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+fileSeparator+"Temp"+fileSeparator+"Octave"+maxRes+"Fifth.jpg");
//        out = new FileOutputStream(file);
//        bmOut1.compress(Bitmap.CompressFormat.JPEG, 100, out);
//        out.flush(); out.close();
            t0.join(); t1.join(); t2.join(); t3.join();
        // We finished calculation of octaves
//-----------------------------------------------------------------------------------------------------------------------------------
        // We start calculation of DoG
            MatrixBorder=radius4; // The maximum border equals maximum radius
            if (MatrixBorder<20) MatrixBorder=20; // We take into consideration the descriptor - the border cannot be less than 20 pixels to top, bottom, left, and right
//            for (i=0;i<width;i++)
//                for (j=0;j<height;j++) {
//                    bmOut.setPixel(i, j, Color.argb(A, 255, 255, 255)); // RGB=255 means white color, i.e. image has white border of 17 pixels
//                    bmOut1.setPixel(i, j, Color.argb(A, 255, 255, 255)); // RGB=255 means white color, i.e. image has white border of 17 pixels
//                    bmOut2.setPixel(i, j, Color.argb(A, 255, 255, 255)); // RGB=255 means white color, i.e. image has white border of 17 pixels
//                    bmOut3.setPixel(i, j, Color.argb(A, 255, 255, 255)); // RGB=255 means white color, i.e. image has white border of 17 pixels
//                }
//        minFirst=1000; maxFirst=-1000; minSecond=1000; maxSecond=-1000; minThird=1000; maxThird=-1000; minFourth=1000; maxFourth=-1000;
            t5.start(); t6.start(); t7.start();
            // t8:
            for (i=MatrixBorder;i<width-MatrixBorder;i++)
                for (j=MatrixBorder;j<height-MatrixBorder;j++) {
                    DoG1000Fourth[i][j]= octave1000Fourth[i][j] - octave1000Fifth[i][j];
//                if (DoG1000Fourth[i][j]>maxFourth) maxFourth=DoG1000Fourth[i][j];
//                else if (DoG1000Fourth[i][j]<minFourth) minFourth=DoG1000Fourth[i][j];
//                x=(int)Math.round(DoG1000Fourth[i][j]);
//                bmOut4.setPixel(i, j, Color.argb(255, x, x, x));
                }
            t5.join(); t6.join(); t7.join();
        //Log.i(TAG, "DoG" + maxRes + "First: MAX =  " + maxFirst + "   MIN = " + minFirst);
        //Log.i(TAG, "DoG" + maxRes + "Second: MAX =  " + maxSecond + "   MIN = " + minSecond);
        //Log.i(TAG, "DoG" + maxRes + "Third: MAX =  " + maxThird + "   MIN = " + minThird);
        //Log.i(TAG, "DoG" + maxRes + "Fourth: MAX =  " + maxFourth + "   MIN = " + minFourth);
/*        file = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+fileSeparator+"Temp"+fileSeparator+"DoG1000First.jpg");
        out = new FileOutputStream(file);
        bmOut1.compress(Bitmap.CompressFormat.JPEG, 100, out);
        out.flush(); out.close();
        file = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+fileSeparator+"Temp"+fileSeparator+"DoG1000Second.jpg");
        out = new FileOutputStream(file);
        bmOut2.compress(Bitmap.CompressFormat.JPEG, 100, out);
        out.flush(); out.close();
        file = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+fileSeparator+"Temp"+fileSeparator+"DoG1000Third.jpg");
        out = new FileOutputStream(file);
        bmOut3.compress(Bitmap.CompressFormat.JPEG, 100, out);
        out.flush(); out.close();
        file = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+fileSeparator+"Temp"+fileSeparator+"DoG1000Fourth.jpg");
        out = new FileOutputStream(file);
        bmOut4.compress(Bitmap.CompressFormat.JPEG, 100, out);
        out.flush(); out.close();
*/
//-----------------------------------------------------------------------------------------------------------------------------------
        // We look for SIFT keypoints
        t8.start(); // Here, we start 2nd, i.e. thread, part
        // Here, we start 1st part
        nk=0; // Number of keypoints equals 0 initially
        for (i=MatrixBorder+1;i<width-MatrixBorder-1;i++)
            for (j=MatrixBorder+1;j<height-MatrixBorder-1;j++) {
                // The following condition is for the SECOND DoG
                if (Math.abs(DoG1000Second[i][j]) >= threshold) {// We exclude extrema of low contrast: |D(X)| must be gt 0.03, here pixels are between 0 and 1
                    // We check for maximum/minimum
                    flagMax = 1; flagMin = 1;
                    max = -1000; min = 1000;
                    for (x = -1; x <= 1; x++) {
                        for (y = -1; y <= 1; y++) {
                            if (DoG1000Second[i][j] <= DoG1000First[i + x][j + y]) flagMax = 0;
                            if (DoG1000Second[i][j] >= DoG1000First[i + x][j + y]) flagMin = 0;
                            if (DoG1000First[i + x][j + y] > max) max = DoG1000First[i + x][j + y];
                            else if (DoG1000First[i + x][j + y] < min)
                                min = DoG1000First[i + x][j + y];
                        }
                        if (flagMax==0 && flagMin==0) break;
                    }
                    if ((flagMax == 1 && DoG1000Second[i][j] > max) || (flagMin == 1 && DoG1000Second[i][j] < min)) {
                        for (x = -1; x <= 1; x++) {
                            for (y = -1; y <= 1; y++) {
                                if (DoG1000Second[i][j] <= DoG1000Third[i + x][j + y]) flagMax = 0;
                                if (DoG1000Second[i][j] >= DoG1000Third[i + x][j + y]) flagMin = 0;
                                if (DoG1000Third[i + x][j + y] > max) max = DoG1000Third[i + x][j + y];
                                else if (DoG1000Third[i + x][j + y] < min) min = DoG1000Third[i + x][j + y];
                            }
                            if (flagMax==0 && flagMin==0) break;
                        }
                        if ((flagMax == 1 && DoG1000Second[i][j] > max) || (flagMin == 1 && DoG1000Second[i][j] < min)) {
                            for (x = -1; x <= 1; x++) {
                                for (y = -1; y <= 1; y++)
                                    if (x != 0 && y != 0) {
                                        if (DoG1000Second[i][j] <= DoG1000Second[i + x][j + y]) flagMax = 0;
                                        if (DoG1000Second[i][j] >= DoG1000Second[i + x][j + y]) flagMin = 0;
                                        if (DoG1000Second[i + x][j + y] > max) max = DoG1000Second[i + x][j + y];
                                        else if (DoG1000Second[i + x][j + y] < min) min = DoG1000Second[i + x][j + y];
                                    }
                                if (flagMax==0 && flagMin==0) break;
                            }
                            if ((flagMax == 1 && DoG1000Second[i][j] > max) || (flagMin == 1 && DoG1000Second[i][j] < min)) {
                                // Now, we eliminate the edges
                                Hessian[0][0] = DoG1000Second[i + 1][j] + DoG1000Second[i - 1][j] - 2.0 * DoG1000Second[i][j];
                                Hessian[1][1] = DoG1000Second[i][j + 1] + DoG1000Second[i][j - 1] - 2.0 * DoG1000Second[i][j];
                                Hessian[0][1] = (DoG1000Second[i + 1][j + 1] - DoG1000Second[i + 1][j - 1] - DoG1000Second[i - 1][j + 1] + DoG1000Second[i - 1][j - 1]) * 0.25;
                                trace = Hessian[0][0] + Hessian[1][1]; //Trace of a matrix
                                det = Hessian[0][0] * Hessian[1][1] - Hessian[0][1] * Hessian[0][1]; // Determinant of a matrix
                                // It was demonstrated that r = 10 is a good ratio, i.e. sqr(r+1)/r=121/10=12.1
                                if (trace * trace / det < 12.1) {// r=10 here
                                    keypoints1000[nk][0] = i;
                                    keypoints1000[nk][1] = j;
                                    nk++;
                                    for (x = -10; x <= 10; x++) {
                                        if ((i + x) >= 0 && (i + x) < width && (j - 1) >= 0 && (j - 1) < height)
                                            bmOut.setPixel(i + x, j - 1, Color.argb(255, 255, 0, 255)); // fuchsia: rgb(255,0,255)
                                        if ((i + x) >= 0 && (i + x) < width && j >= 0 && j < height)
                                            bmOut.setPixel(i + x, j, Color.argb(255, 255, 0, 255)); // fuchsia: rgb(255,0,255)
                                        if ((i + x) >= 0 && (i + x) < width && (j + 1) >= 0 && (j + 1) < height)
                                            bmOut.setPixel(i + x, j + 1, Color.argb(255, 255, 0, 255)); // fuchsia: rgb(255,0,255)
                                        if ((i + 1) >= 0 && (i + 1) < width && (j + x) >= 0 && (j + x) < height)
                                            bmOut.setPixel(i - 1, j + x, Color.argb(255, 255, 0, 255)); // fuchsia: rgb(255,0,255)
                                        if (i >= 0 && i < width && (j + x) >= 0 && (j + x) < height)
                                            bmOut.setPixel(i, j + x, Color.argb(255, 255, 0, 255)); // fuchsia: rgb(255,0,255)
                                        if ((i + 1) >= 0 && (i + 1) < width && (j + x) >= 0 && (j + x) < height)
                                            bmOut.setPixel(i + 1, j + x, Color.argb(255, 255, 0, 255)); // fuchsia: rgb(255,0,255)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        t8.join(); // Here, we wait for 2nd, i.e. thread, part to be finished and joint the main thread

        nk = nk + nkSecond;
        for (k=0;k<nkSecond;k++) {
            keypoints1000[k+nk][0] = keypoints1000Second[k][0];
            keypoints1000[k+nk][1] = keypoints1000Second[k][1];
            i = keypoints1000Second[k][0]; j = keypoints1000Second[k][1];
            for (x = -10; x <= 10; x++) {
                if ((i + x) >= 0 && (i + x) < width && (j - 1) >= 0 && (j - 1) < height)
                    bmOut.setPixel(i + x, j - 1, Color.argb(255, 64, 224, 208)); // turquoise color: rgb(64,224,208)
                if ((i + x) >= 0 && (i + x) < width && j >= 0 && j < height)
                    bmOut.setPixel(i + x, j, Color.argb(255, 64, 224, 208)); // turquoise color: rgb(64,224,208)
                if ((i + x) >= 0 && (i + x) < width && (j + 1) >= 0 && (j + 1) < height)
                    bmOut.setPixel(i + x, j + 1, Color.argb(255, 64, 224, 208)); // turquoise color: rgb(64,224,208)
                if ((i - 1) >= 0 && (i - 1) < width && (j + x) >= 0 && (j + x) < height)
                    bmOut.setPixel(i - 1, j + x, Color.argb(255, 64, 224, 208)); // turquoise color: rgb(64,224,208)
                if (i >= 0 && i < width && (j + x) >= 0 && (j + x) < height)
                    bmOut.setPixel(i, j + x, Color.argb(255, 64, 224, 208)); // turquoise color: rgb(64,224,208)
                if ((i + 1) >= 0 && (i + 1) < width && (j + x) >= 0 && (j + x) < height)
                    bmOut.setPixel(i + 1, j + x, Color.argb(255, 64, 224, 208)); // turquoise color: rgb(64,224,208)
            }
        }

        file = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+fileSeparator+"Temp"+fileSeparator+"KeyPoints"+ maxRes + ".jpg");
        out = new FileOutputStream(file);
        bmOut.compress(Bitmap.CompressFormat.JPEG, 100, out);
        out.flush(); out.close();

        // We write info about keypoints into the text file
        file = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+fileSeparator+"Temp"+fileSeparator+"KeyPoints"+maxRes+".txt");
        file.createNewFile();
        //second argument of FileOutputStream constructor indicates whether to append or create new file if one exists
        FileOutputStream outputStream = new FileOutputStream(file, false);
        PrintWriter writer = new PrintWriter(outputStream);
        writer.println(Integer.toString(nk)); writer.println("");
        for (i=0;i<nk;i++) { // nk is the number of keypoints
            writer.println(keypoints1000[i][0]); // X coordinate
            writer.println(keypoints1000[i][1]); // Y coordinate
            writer.println("");
        }
        writer.flush(); writer.close();
//-------------------------------------------------------------------------------------------------------------------------------
// We exclude keypoints that cannot belong to our TWO KEYPOINTS:
// 1st simple rule: we exclude the keypoint if it is less than 20 mm from the border, i.e. x<20, y<20, x>(width-20), y>(height-20)
// We do that because distance between 2 main keypoints must be greater than 30 - Ok we've done it with MatrixBorder
        Log.i(TAG, "Number of keypoints before optomization: " + nk);
// 2nd simple rule: we analyze maxNoKeyPoints keypoints from the center of the image
// We sort keypoints in accord to the distance from the center. Then, we simply take maxNoKeyPoints keypoints
        if (nk>maxNoKeyPoints){
            det=width/2; // X coordinate: center
            trace=height/2; // Y coorinate: center
            for (i=0;i<nk;i++) { // We calculate differences here
                minFirst=keypoints1000[i][0]-det;
                minSecond=keypoints1000[i][1]-trace;
                octave1000First[i][0]=Math.sqrt(minFirst*minFirst+minSecond*minSecond);
            }
            // We sort differences here from smallest to largest
            flagMin=1;
            while (flagMin==1){
                flagMin=0;
                for (i=0; i<(nk-1); i++)
                    if (octave1000First[i][0]>octave1000First[i+1][0]) {
                        det = octave1000First[i][0]; octave1000First[i][0] = octave1000First[i+1][0]; octave1000First[i+1][0]=det;
                        x = keypoints1000[i][0]; keypoints1000[i][0] = keypoints1000[i+1][0]; keypoints1000[i+1][0] = x;
                        y = keypoints1000[i][1]; keypoints1000[i][1] = keypoints1000[i+1][1]; keypoints1000[i+1][1] = y;
                        flagMin=1;
                    }
            }
            nk=maxNoKeyPoints; // We analyze only maxNoKeyPoints keypoints
        }
//-------------------------------------------------------------------------------------------------------------------------------
// We start analysis of keypoints here: 1st level
        for (i1=0;i1<nk;i1++)
            for (i2 = 0; i2 < nk; i2++) {
                det = keypoints1000[i1][0] - keypoints1000[i2][0];
                trace = keypoints1000[i1][1] - keypoints1000[i2][1];
                if ((Math.sqrt(det * det + trace * trace) > 30)) {
                    // We use distance 30 pixels cause we need some pixels for intermediate points
//                    Log.i(TAG, "We start analysis of the file: width =  " + width + " ; height = " + height);
                    xk[7] = keypoints1000[i1][0];
                    yk[7] = keypoints1000[i1][1];
                    xk[10] = keypoints1000[i2][0];
                    yk[10] = keypoints1000[i2][1];
                    sigma0 = (xk[7] - xk[10]) / 3;
                    sigma1 = (yk[7] - yk[10]) / 3;
                    xk[6] = xk[7] + sigma0;
                    yk[6] = yk[7] + sigma1;
                    xk[0] = xk[6] - sigma1;
                    yk[0] = yk[6] + sigma0;
// First, we have to check the borders: MUST be positive and less than width and height
// Top left corner
                    xk[25] = xk[0] - sigma1 + sigma0;
                    yk[25] = yk[0] + sigma0 + sigma1;
                    if (xk[25] > 0 && yk[25] > 0 && xk[25] <= width && yk[25] <= height) {
                        xk[11] = xk[10] - sigma0;
                        yk[11] = yk[10] - sigma1;
                        xk[5] = xk[11] - sigma1;
                        yk[5] = yk[11] + sigma0;
// Top right corner
                        xk[26] = xk[5] - sigma1 - sigma0;
                        yk[26] = yk[5] + sigma0 - sigma1;
                        if (xk[26] > 0 && yk[26] > 0 && xk[26] <= width && yk[26] <= height) {
// Bottom left corner
                            xk[27] = xk[6] + 5.0 * sigma1 + sigma0;
                            yk[27] = yk[6] - 5.0 * sigma0 + sigma1;
                            if (xk[27] > 0 && yk[27] > 0 && xk[27] <= width && yk[27] <= height) {
// Bottom right corner
                                xk[28] = xk[11] + 5.0 * sigma1 - sigma0;
                                yk[28] = yk[11] - 5.0 * sigma0 - sigma1;
                                if (xk[28] > 0 && yk[28] > 0 && xk[28] <= width && yk[28] <= height) {
                                    XkYk1st();
                                    for (pixel = 0; pixel < 18; pixel++) { // We calculate average intensities and draw circles here
                                        radius0 = (int) Math.round(xk[pixel] - sigma4); // The top coordinate X within the circle, 0 keypoint
                                        if (radius0 < Math.round(xk[pixel] - sigma4))
                                            radius0++; // We adjust the coordinate X to be inside the circle
                                        radius1 = (int) Math.round(yk[pixel] - sigma4); // The most left coordinate Y within the circle, 0 keypoint
                                        if (radius1 < Math.round(yk[pixel] - sigma4))
                                            radius1++; // We adjust the coordinate Y to be inside the circle
                                        radius2 = (int) Math.round(xk[pixel] + sigma4); // The bottom coordinate X within the circle, 0 keypoint
                                        if (radius2 < Math.round(xk[pixel] + sigma4))
                                            radius2++; // We adjust the coordinate X to be outside the circle - nearest largest integer: to speed the following loop
                                        radius3 = (int) Math.round(yk[pixel] + sigma4); // The most right Y coordinate within the circle, 0 keypoint
                                        if (radius3 < Math.round(yk[pixel] + sigma4))
                                            radius3++; // We adjust the coordinate Y to be outside the circle - nearest largest integer: to speed the following loop
                                        IC[pixel] = 0; // Average intensity of the circle around keypoint 0
                                        k = 0; // Number of the pixels inside the circle around keypoint
                                        for (i = radius0; i < radius2; i++)
                                            for (j = radius1; j < radius3; j++) {
                                                det = i - xk[pixel]; trace = j - yk[pixel];
                                                if (Math.sqrt(det*det + trace*trace) <= sigma4) {
                                                    k++;
                                                    IC[pixel] = IC[pixel] + greyC[i][j];
//                                                    bmOut.setPixel(i, j, Color.argb(255, 64, 224, 208)); // turquoise color: rgb(64,224,208)
                                                }
                                            }
                                        IC[pixel] = IC[pixel] / k;
//                                        Log.i(TAG, "Average intensity for keypoint " + pixel +" : " + IC[pixel] + " ;  Number of pixels: " + k);
                                    }
                                    sigma4 = sigma4 * 1.5; // radius of the circle around the point, 1st and 2nd levels
                                    for (pixel = 18; pixel < 22; pixel++) { // We calculate average intensities and draw circles here
                                        radius0 = (int) Math.round(xk[pixel] - sigma4); // The top coordinate X within the circle, 0 keypoint
                                        if (radius0 < Math.round(xk[pixel] - sigma4))
                                            radius0++; // We adjust the coordinate X to be inside the circle
                                        radius1 = (int) Math.round(yk[pixel] - sigma4); // The most left coordinate Y within the circle, 0 keypoint
                                        if (radius1 < Math.round(yk[pixel] - sigma4))
                                            radius1++; // We adjust the coordinate Y to be inside the circle
                                        radius2 = (int) Math.round(xk[pixel] + sigma4); // The bottom coordinate X within the circle, 0 keypoint
                                        if (radius2 < Math.round(xk[pixel] + sigma4))
                                            radius2++; // We adjust the coordinate X to be outside the circle - nearest largest integer: to speed the following loop
                                        radius3 = (int) Math.round(yk[pixel] + sigma4); // The most right Y coordinate within the circle, 0 keypoint
                                        if (radius3 < Math.round(yk[pixel] + sigma4))
                                            radius3++; // We adjust the coordinate Y to be outside the circle - nearest largest integer: to speed the following loop
                                        IC[pixel] = 0; // Average intensity of the circle around keypoint 0
                                        k = 0; // Number of the pixels inside the circle around keypoint
                                        for (i = radius0; i < radius2; i++)
                                            for (j = radius1; j < radius3; j++) {
                                                det = i - xk[pixel]; trace = j - yk[pixel];
                                                if (Math.sqrt(det*det + trace*trace) <= sigma4) {
                                                    k++;
                                                    IC[pixel] = IC[pixel] + greyC[i][j];
//                                                    bmOut.setPixel(i, j, Color.argb(255, 64, 224, 208)); // turquoise color: rgb(64,224,208)
                                                }
                                            }
                                        IC[pixel] = IC[pixel] / k;
//                                        Log.i(TAG, "Average intensity for keypoint " + pixel +" : " + IC[pixel] + " ;  Number of pixels: " + k);
                                    }
                                    sigma4 = sigma4 * 1.333333333333333333; // radius of the circle around the point, 1st and 2nd levels
                                    for (pixel = 22; pixel < 25; pixel++) { // We calculate average intensities and draw circles here
                                        radius0 = (int) Math.round(xk[pixel] - sigma4); // The top coordinate X within the circle, 0 keypoint
                                        if (radius0 < Math.round(xk[pixel] - sigma4))
                                            radius0++; // We adjust the coordinate X to be inside the circle
                                        radius1 = (int) Math.round(yk[pixel] - sigma4); // The most left coordinate Y within the circle, 0 keypoint
                                        if (radius1 < Math.round(yk[pixel] - sigma4))
                                            radius1++; // We adjust the coordinate Y to be inside the circle
                                        radius2 = (int) Math.round(xk[pixel] + sigma4); // The bottom coordinate X within the circle, 0 keypoint
                                        if (radius2 < Math.round(xk[pixel] + sigma4))
                                            radius2++; // We adjust the coordinate X to be outside the circle - nearest largest integer: to speed the following loop
                                        radius3 = (int) Math.round(yk[pixel] + sigma4); // The most right Y coordinate within the circle, 0 keypoint
                                        if (radius3 < Math.round(yk[pixel] + sigma4))
                                            radius3++; // We adjust the coordinate Y to be outside the circle - nearest largest integer: to speed the following loop
                                        IC[pixel] = 0; // Average intensity of the circle around keypoint 0
                                        k = 0; // Number of the pixels inside the circle around keypoint
                                        for (i = radius0; i < radius2; i++)
                                            for (j = radius1; j < radius3; j++){
                                                det = i - xk[pixel]; trace = j - yk[pixel];
                                                if (Math.sqrt(det*det + trace*trace) <= sigma4) {
                                                    k++;
                                                    IC[pixel] = IC[pixel] + greyC[i][j];
//                                                    bmOut.setPixel(i, j, Color.argb(255, 64, 224, 208)); // turquoise color: rgb(64,224,208)
                                                }
                                            }
                                        IC[pixel] = IC[pixel] / k;
//                                        Log.i(TAG, "Average intensity for keypoint " + pixel +" : " + IC[pixel] + " ;  Number of pixels: " + k);
                                    }
// We find 20 points with maximum difference now:
//                                    Log.i(TAG, "1st level:");
                                    k = 0; // Number of correct comparisons
                                    for (i = 0; i < 25; i++) { // We calculate coincidence between template and current image here, 1st level
                                        // We consider only 1st and 2nd maximum differences, i.e. 10% of all comparisions
                                        trace = IC[i] - IC[ICdif[i][0]];
                                        if ((trace > 0 && ICdifDouble[i][0] > 0) || (trace < 0 && ICdifDouble[i][0] < 0))
                                            k++;
                                        trace = IC[i] - IC[ICdif[i][1]];
                                        if ((trace > 0 && ICdifDouble[i][1] > 0) || (trace < 0 && ICdifDouble[i][1] < 0))
                                            k++;
                                    }
                                    if (k > 45) {
                                        // 50 is a maximum correct comparisions: 50*0.4=20 ; 0.4 - accuracy1; accuracy - % of correct comparisons
                                        // 50 is a maximum correct comparisions: 50*0.35=17.5 ; 0.35 - accuracy1; accuracy - % of correct comparisons
                                        // 50 is a maximum correct comparisions: 50*0.9=45 ; 0.9 - accuracy1; accuracy - % of correct comparisons
                                        // 50 is a maximum correct comparisions: 50*0.8=40 ; 0.8 - accuracy1; accuracy - % of correct comparisons
                                        // 50 is a maximum correct comparisions: 50*0.7=35 ; 0.7 - accuracy1; accuracy - % of correct comparisons
                                        // 50 is a maximum correct comparisions: 50*0.5=25 ; 0.5 - accuracy1; accuracy - % of correct comparisons
                                        // Log.i(TAG, "1st level : Point 11 - x=" + xk[11] + " ; y=" + yk[11] + " ; Point 14 - x=" + xk[14] + " ; y=" + yk[14]); // 10*77*0.95=731.5, i.e. 95% is correct
                                        for (i = 0; i < 25; i++) // We calculate coincidence between template and current image here, 1st level
                                            for (j = 2; j < 20; j++) { // We start with 3rd maximum difference cause 1st and 2nd were discussed above
                                                // We analyze first 20 differences
                                                trace = IC[i] - IC[ICdif[i][j]];
                                                if ((trace > 0 && ICdifDouble[i][j] > 0) || (trace < 0 && ICdifDouble[i][j] < 0))
                                                    k++;
                                            }
//                                        Log.i(TAG, "5%: "+k);
                                        if (k > 425) {
                                            // 500 is a maximum correct comparisions: 500*0.4=200 ; 0.4 - accuracy1
                                            // 500 is a maximum correct comparisions: 500*0.35=175 ; 0.35 - accuracy1
                                            // 500 is a maximum correct comparisions: 500*0.9=450 ; 0.9 - accuracy1
                                            // 500 is a maximum correct comparisions: 500*0.8=400 ; 0.8 - accuracy1
                                            // 500 is a maximum correct comparisions: 500*0.85=425 ; 0.85 - accuracy1
                                            // 500 is a maximum correct comparisions: 500*0.7=350 ; 0.7 - accuracy1
                                            // 500 is a maximum correct comparisions: 500*0.5=250 ; 0.5 - accuracy1
                                            // We start 2nd level here -----------------------------------------------------------------
                                            XkYk2nd();
                                            for (pixel = 79; pixel < 83; pixel++) { // We calculate average intensities and draw circles here
                                                radius0 = (int) Math.round(xk[pixel] - sigma4); // The top coordinate X within the circle, 0 keypoint
                                                if (radius0 < Math.round(xk[pixel] - sigma4))
                                                    radius0++; // We adjust the coordinate X to be inside the circle
                                                radius1 = (int) Math.round(yk[pixel] - sigma4); // The most left coordinate Y within the circle, 0 keypoint
                                                if (radius1 < Math.round(yk[pixel] - sigma4))
                                                    radius1++; // We adjust the coordinate Y to be inside the circle
                                                radius2 = (int) Math.round(xk[pixel] + sigma4); // The bottom coordinate X within the circle, 0 keypoint
                                                if (radius2 < Math.round(xk[pixel] + sigma4))
                                                    radius2++; // We adjust the coordinate X to be outside the circle - nearest largest integer: to speed the following loop
                                                radius3 = (int) Math.round(yk[pixel] + sigma4); // The most right Y coordinate within the circle, 0 keypoint
                                                if (radius3 < Math.round(yk[pixel] + sigma4))
                                                    radius3++; // We adjust the coordinate Y to be outside the circle - nearest largest integer: to speed the following loop
                                                IC[pixel] = 0; // Average intensity of the circle around keypoint 0
                                                k = 0; // Number of the pixels inside the circle around keypoint
                                                for (i = radius0; i < radius2; i++)
                                                    for (j = radius1; j < radius3; j++) {
                                                        det = i - xk[pixel]; trace = j - yk[pixel];
                                                        if (Math.sqrt(det*det + trace*trace) <= sigma4) {
                                                            k++;
                                                            IC[pixel] = IC[pixel] + greyC[i][j];
//                                                            bmOut.setPixel(i, j, Color.argb(255, 64, 224, 208)); // turquoise color: rgb(64,224,208)
                                                        }
                                                    }
                                                IC[pixel] = IC[pixel] / k;
//                                                Log.i(TAG, "Average intensity for keypoint " + pixel +" : " + IC[pixel] + " ;  Number of pixels: " + k);
                                            }
                                            sigma4 = sigma4 * 0.75; // radius of the circle around the point, 1st and 2nd levels
                                            for (pixel = 62; pixel < 79; pixel++) { // We calculate average intensities and draw circles here
                                                radius0 = (int) Math.round(xk[pixel] - sigma4); // The top coordinate X within the circle, 0 keypoint
                                                if (radius0 < Math.round(xk[pixel] - sigma4))
                                                    radius0++; // We adjust the coordinate X to be inside the circle
                                                radius1 = (int) Math.round(yk[pixel] - sigma4); // The most left coordinate Y within the circle, 0 keypoint
                                                if (radius1 < Math.round(yk[pixel] - sigma4))
                                                    radius1++; // We adjust the coordinate Y to be inside the circle
                                                radius2 = (int) Math.round(xk[pixel] + sigma4); // The bottom coordinate X within the circle, 0 keypoint
                                                if (radius2 < Math.round(xk[pixel] + sigma4))
                                                    radius2++; // We adjust the coordinate X to be outside the circle - nearest largest integer: to speed the following loop
                                                radius3 = (int) Math.round(yk[pixel] + sigma4); // The most right Y coordinate within the circle, 0 keypoint
                                                if (radius3 < Math.round(yk[pixel] + sigma4))
                                                    radius3++; // We adjust the coordinate Y to be outside the circle - nearest largest integer: to speed the following loop
                                                IC[pixel] = 0; // Average intensity of the circle around keypoint 0
                                                k = 0; // Number of the pixels inside the circle around keypoint
                                                for (i = radius0; i < radius2; i++)
                                                    for (j = radius1; j < radius3; j++) {
                                                        det = i - xk[pixel]; trace = j - yk[pixel];
                                                        if (Math.sqrt(det*det + trace*trace) <= sigma4) {
                                                            k++;
                                                            IC[pixel] = IC[pixel] + greyC[i][j];
//                                                            bmOut.setPixel(i, j, Color.argb(255, 64, 224, 208)); // turquoise color: rgb(64,224,208)
                                                        }
                                                    }
                                                IC[pixel] = IC[pixel] / k;
//                                                Log.i(TAG, "Average intensity for keypoint " + pixel +" : " + IC[pixel] + " ;  Number of pixels: " + k);
                                            }
                                            sigma4 = sigma4 * 0.6666666666666666; // radius of the circle around the point, 1st and 2nd levels
                                            for (pixel = 25; pixel < 62; pixel++) { // We calculate average intensities and draw circles here
                                                radius0 = (int) Math.round(xk[pixel] - sigma4); // The top coordinate X within the circle, 0 keypoint
                                                if (radius0 < Math.round(xk[pixel] - sigma4))
                                                    radius0++; // We adjust the coordinate X to be inside the circle
                                                radius1 = (int) Math.round(yk[pixel] - sigma4); // The most left coordinate Y within the circle, 0 keypoint
                                                if (radius1 < Math.round(yk[pixel] - sigma4))
                                                    radius1++; // We adjust the coordinate Y to be inside the circle
                                                radius2 = (int) Math.round(xk[pixel] + sigma4); // The bottom coordinate X within the circle, 0 keypoint
                                                if (radius2 < Math.round(xk[pixel] + sigma4))
                                                    radius2++; // We adjust the coordinate X to be outside the circle - nearest largest integer: to speed the following loop
                                                radius3 = (int) Math.round(yk[pixel] + sigma4); // The most right Y coordinate within the circle, 0 keypoint
                                                if (radius3 < Math.round(yk[pixel] + sigma4))
                                                    radius3++; // We adjust the coordinate Y to be outside the circle - nearest largest integer: to speed the following loop
                                                IC[pixel] = 0; // Average intensity of the circle around keypoint 0
                                                k = 0; // Number of the pixels inside the circle around keypoint
                                                for (i = radius0; i < radius2; i++)
                                                    for (j = radius1; j < radius3; j++) {
                                                        det = i - xk[pixel]; trace = j - yk[pixel];
                                                        if (Math.sqrt(det*det + trace*trace) <= sigma4) {
                                                            k++;
                                                            IC[pixel] = IC[pixel] + greyC[i][j];
//                                                            bmOut.setPixel(i, j, Color.argb(255, 64, 224, 208)); // turquoise color: rgb(64,224,208)
                                                        }
                                                    }
                                                IC[pixel] = IC[pixel] / k;
//                                                Log.i(TAG, "Average intensity for keypoint " + pixel +" : " + IC[pixel] + " ;  Number of pixels: " + k);
                                            }
                                            k = 0; // Number of correct comparisons
                                            for (i = 25; i < 83; i++) {// We calculate coincidence between template and current image here, 2nd level
                                                // We consider only 1st and 2nd maximum differences, i.e. 5% of all comparisions
                                                trace = IC[i] - IC[ICdif[i][25]];
                                                if ((trace > 0 && ICdifDouble[i][25] > 0) || (trace < 0 && ICdifDouble[i][25] < 0)) k++;
                                                trace = IC[i] - IC[ICdif[i][26]];
                                                if ((trace > 0 && ICdifDouble[i][26] > 0) || (trace < 0 && ICdifDouble[i][26] < 0)) k++;
                                            }
                                            Log.i(TAG, "5%: " + k);
                                            if (k > 104) {
                                                // 116 is a maximum two correct comparisions for 58 points: 116*0.35=40.6 ; 0.35 - accuracy2
                                                // 116 is a maximum two correct comparisions for 58 points: 116*0.55=63.8 ; 0.55 - accuracy2
                                                // 116 is a maximum two correct comparisions for 58 points: 116*0.9=104.4 ; 0.9 - accuracy2
                                                // 116 is a maximum two correct comparisions for 58 points: 116*0.95=110.2 ; 0.95 - accuracy2
                                                // 116 is a maximum two correct comparisions for 58 points: 116*0.8=92.8 ; 0.8 - accuracy2
                                                // 116 is a maximum two correct comparisions for 58 points: 116*0.7=81.2 ; 0.7 - accuracy2
                                                // 116 is a maximum two correct comparisions for 58 points: 116*0.5=58 ; 0.5 - accuracy2
                                                for (i = 25; i < 83; i++) // We calculate coincidence between template and current image here
                                                    for (j = 27; j < 64; j++) { // We start with 3rd maximum difference cause 1st and 2nd were discussed above
                                                        trace = IC[i] - IC[ICdif[i][j]];
                                                        if ((trace > 0 && ICdifDouble[i][j] > 0) || (trace < 0 && ICdifDouble[i][j] < 0)) k++;
                                                    }
                                                //Log.i(TAG, "100%: "+k);
                                                // 58*40=2320 is a maximum correct comparisions: 2320*0.35=812 ; 0.35 - accuracy2
                                                // 58*40=2320 is a maximum correct comparisions: 2320*0.55=1276 ; 0.55 - accuracy2
                                                // 58*40=2320 is a maximum correct comparisions: 2320*0.9=2088 ; 0.9 - accuracy2
                                                // 58*40=2320 is a maximum correct comparisions: 2320*0.95=2204 ; 0.95 - accuracy2
                                                // 58*40=2320 is a maximum correct comparisions: 2320*0.8=1856 ; 0.8 - accuracy2
                                                // 58*40=2320 is a maximum correct comparisions: 2320*0.85=1972 ; 0.85 - accuracy2
                                                // 58*40=2320 is a maximum correct comparisions: 2320*0.7=1624 ; 0.7 - accuracy2
                                                // 58*40=2320 is a maximum correct comparisions: 2320*0.5=1160 ; 0.5 - accuracy2
                                                Log.i(TAG, "10%: " + k);
                                                if (k > 1972) {
                                                    Log.i(TAG, "Point 7 - x=" + xk[7] + " ; y=" + yk[7] + " ; Point 10 - x=" + xk[10] + " ; y=" + yk[10]);
/*                                                    for (k=0;k<25;k++) {
                                                        i = (int)Math.round(xk[k]); j = (int)Math.round(yk[k]);
                                                        for (x = -1; x <= 1; x++) {
                                                            bmOut.setPixel(i + x, j - 1, Color.argb(255, 255, 0, 0)); // red color: rgb(255,0,0)
                                                            bmOut.setPixel(i + x, j, Color.argb(255, 255, 0, 0)); // red color: rgb(255,0,0)
                                                            bmOut.setPixel(i + x, j + 1, Color.argb(255, 255, 0, 0)); // red color: rgb(255,0,0)
                                                            bmOut.setPixel(i - 1, j + x, Color.argb(255, 255, 0, 0)); // red color: rgb(255,0,0)
                                                            bmOut.setPixel(i, j + x, Color.argb(255, 255, 0, 0)); // red color: rgb(255,0,0)
                                                            bmOut.setPixel(i + 1, j + x, Color.argb(255, 255, 0, 0)); // red color: rgb(255,0,0)
                                                        }
                                                    }
*/
                                                }
                                            }
                                        }
                                    }
                                } // else Log.i(TAG, "Descriptor size is out of the image size: xk[11]=" + keypoints1000[i1][0] + " ; yk[11]=" + keypoints1000[i1][1] + " ; xk[14]=" + keypoints1000[i2][0] + " ; yk[14]=" + keypoints1000[i2][1]);
                            } // else Log.i(TAG, "Descriptor size is out of the image size: xk[11]=" + keypoints1000[i1][0] + " ; yk[11]=" + keypoints1000[i1][1] + " ; xk[14]=" + keypoints1000[i2][0] + " ; yk[14]=" + keypoints1000[i2][1]);
                        } // else Log.i(TAG, "Descriptor size is out of the image size: xk[11]=" + keypoints1000[i1][0] + " ; yk[11]=" + keypoints1000[i1][1] + " ; xk[14]=" + keypoints1000[i2][0] + " ; yk[14]=" + keypoints1000[i2][1]);
                    } // else Log.i(TAG, "Descriptor size is out of the image size: xk[11]=" + keypoints1000[i1][0] + " ; yk[11]=" + keypoints1000[i1][1] + " ; xk[14]=" + keypoints1000[i2][0] + " ; yk[14]=" + keypoints1000[i2][1]);

                    file = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath() + fileSeparator + "Temp" + fileSeparator + "ImageWithKeyPoints" + maxRes + ".jpg");
                    out = new FileOutputStream(file);
                    bmOut.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    out.flush();
                    out.close();
//                    Log.i(TAG, "Marks were added into the image. Resolution : " + maxRes);
                }
            }
        Log.i(TAG, "Analysis finished. Resolution : " + maxRes);
    } catch (Exception e) {
        Log.i(TAG, "Exception  " + e);
    }
    }

    public void XkYk1st(){
        xk[13] = xk[7] + sigma1; yk[13] = yk[7] - sigma0;
        xk[12] = xk[13] + sigma0; yk[12] = yk[13] + sigma1;
        xk[14] = xk[13] - sigma0; yk[14] = yk[13] - sigma1;
        xk[15] = xk[14] - sigma0; yk[15] = yk[14] - sigma1;
        xk[16] = xk[15] - sigma0; yk[16] = yk[15] - sigma1;
        xk[17] = xk[16] - sigma0; yk[17] = yk[16] - sigma1;
        xk[1] = xk[0] - sigma0; yk[1] = yk[0] - sigma1;
        xk[2] = xk[1] - sigma0; yk[2] = yk[1] - sigma1;
        xk[3] = xk[2] - sigma0; yk[3] = yk[2] - sigma1;
        xk[4] = xk[3] - sigma0; yk[4] = yk[3] - sigma1;
        xk[8] = xk[7] - sigma0; yk[8] = yk[7] - sigma1;
        xk[9] = xk[8] - sigma0; yk[9] = yk[8] - sigma1;
        xk[18] = xk[13] + sigma1; yk[18] = yk[13] - sigma0;
        xk[19] = xk[14] + sigma1; yk[19] = yk[14] - sigma0;
        xk[20] = xk[15] + sigma1; yk[20] = yk[15] - sigma0;
        xk[21] = xk[16] + sigma1; yk[21] = yk[16] - sigma0;
        xk[22] = xk[19] + sigma1; yk[22] = yk[19] - sigma0;
        xk[23] = xk[20] + sigma1; yk[23] = yk[20] - sigma0;
        xk[24] = xk[22] + sigma1 - sigma0*0.5; yk[24] = yk[22] - sigma0 - sigma1*0.5;
        sigma4=Math.sqrt((xk[10]-xk[7])*(xk[10]-xk[7])+(yk[10]-yk[7])*(yk[10]-yk[7]))/24; // radius of the circle around the point, 1st and 2nd levels
    }

    public void XkYk2nd(){
        sigma0=sigma0*0.5; sigma1=sigma1*0.5;
        xk[25] = xk[0] - sigma0; yk[25] = yk[0] - sigma1;
        xk[26] = xk[1] - sigma0; yk[26] = yk[1] - sigma1;
        xk[27] = xk[2] - sigma0; yk[27] = yk[2] - sigma1;
        xk[28] = xk[3] - sigma0; yk[28] = yk[3] - sigma1;
        xk[29] = xk[4] - sigma0; yk[29] = yk[4] - sigma1;
        xk[30] = xk[6] - sigma1; yk[30] = yk[6] + sigma0;
        xk[31] = xk[30] - sigma0; yk[31] = yk[30] - sigma1;
        xk[32] = xk[31] - sigma0; yk[32] = yk[31] - sigma1;
        xk[33] = xk[32] - sigma0; yk[33] = yk[32] - sigma1;
        xk[34] = xk[33] - sigma0; yk[34] = yk[33] - sigma1;
        xk[35] = xk[34] - sigma0; yk[35] = yk[34] - sigma1;
        xk[36] = xk[35] - sigma0; yk[36] = yk[35] - sigma1;
        xk[37] = xk[36] - sigma0; yk[37] = yk[36] - sigma1;
        xk[38] = xk[37] - sigma0; yk[38] = yk[37] - sigma1;
        xk[39] = xk[38] - sigma0; yk[39] = yk[38] - sigma1;
        xk[40] = xk[39] - sigma0; yk[40] = yk[39] - sigma1;
        xk[41] = xk[6] - sigma0; yk[41] = yk[6] - sigma1;
        xk[42] = xk[7] - sigma0; yk[42] = yk[7] - sigma1;
        xk[43] = xk[8] - sigma0; yk[43] = yk[8] - sigma1;
        xk[44] = xk[9] - sigma0; yk[44] = yk[9] - sigma1;
        xk[45] = xk[10] - sigma0; yk[45] = yk[10] - sigma1;
        xk[46] = xk[12] - sigma1; yk[46] = yk[12] + sigma0;
        xk[47] = xk[46] - sigma0; yk[47] = yk[46] - sigma1;
        xk[48] = xk[47] - sigma0; yk[48] = yk[47] - sigma1;
        xk[49] = xk[48] - sigma0; yk[49] = yk[48] - sigma1;
        xk[50] = xk[49] - sigma0; yk[50] = yk[49] - sigma1;
        xk[51] = xk[50] - sigma0; yk[51] = yk[50] - sigma1;
        xk[52] = xk[51] - sigma0; yk[52] = yk[51] - sigma1;
        xk[53] = xk[52] - sigma0; yk[53] = yk[52] - sigma1;
        xk[54] = xk[53] - sigma0; yk[54] = yk[53] - sigma1;
        xk[55] = xk[54] - sigma0; yk[55] = yk[54] - sigma1;
        xk[56] = xk[55] - sigma0; yk[56] = yk[55] - sigma1;
        xk[57] = xk[12] - sigma0; yk[57] = yk[12] - sigma1;
        xk[58] = xk[13] - sigma0; yk[58] = yk[13] - sigma1;
        xk[59] = xk[14] - sigma0; yk[59] = yk[14] - sigma1;
        xk[60] = xk[15] - sigma0; yk[60] = yk[15] - sigma1;
        xk[61] = xk[16] - sigma0; yk[61] = yk[16] - sigma1;
        xk[62] = xk[57] + sigma1; yk[62] = yk[57] - sigma0;
        xk[63] = xk[62] - sigma0; yk[63] = yk[62] - sigma1;
        xk[64] = xk[63] - sigma0; yk[64] = yk[63] - sigma1;
        xk[65] = xk[64] - sigma0; yk[65] = yk[64] - sigma1;
        xk[66] = xk[65] - sigma0; yk[66] = yk[65] - sigma1;
        xk[67] = xk[66] - sigma0; yk[67] = yk[66] - sigma1;
        xk[68] = xk[67] - sigma0; yk[68] = yk[67] - sigma1;
        xk[69] = xk[68] - sigma0; yk[69] = yk[68] - sigma1;
        xk[70] = xk[69] - sigma0; yk[70] = yk[69] - sigma1;
        xk[71] = xk[18] - sigma0; yk[71] = yk[18] - sigma1;
        xk[72] = xk[19] - sigma0; yk[72] = yk[19] - sigma1;
        xk[73] = xk[20] - sigma0; yk[73] = yk[20] - sigma1;
        xk[74] = xk[71] + sigma1; yk[74] = yk[71] - sigma0;
        xk[75] = xk[74] - sigma0; yk[75] = yk[74] - sigma1;
        xk[76] = xk[75] - sigma0; yk[76] = yk[75] - sigma1;
        xk[77] = xk[76] - sigma0; yk[77] = yk[76] - sigma1;
        xk[78] = xk[77] - sigma0; yk[78] = yk[77] - sigma1;
        xk[79] = xk[22] - sigma0; yk[79] = yk[22] - sigma1;
        xk[80] = xk[22] + sigma1; yk[80] = yk[22] - sigma0;
        xk[81] = xk[80] - sigma0; yk[81] = yk[80] - sigma1;
        xk[82] = xk[81] - sigma0; yk[82] = yk[81] - sigma1;
    }

//    public class ThreadX1 extends Thread { // This thread is used to find the object
//    }

    class ThreadOctave0 extends Thread { // This thread is used to speed up SIFT
        private int i, j, x, y;
        public void run (){
            // we use radius4 to boost the performance
            for (i=radius4;i<width-radius4;i++)
                for (j = radius4; j < height - radius4; j++) {
                    octave1000First[i][j] = 0;
                    for (x = -radius0; x <= radius0; x++)
                        for (y = -radius0; y <= radius0; y++)
                            octave1000First[i][j] = octave1000First[i][j] + maskS0[x + radius0][y + radius0] * greyC[i + x][j + y];
//                    x=(int)Math.round(octave1000First[i][j]);
//                  bmOut1.setPixel(i, j, Color.argb(255, x, x, x));
                }
//        file = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+fileSeparator+"Temp"+fileSeparator+"Octave"+maxRes+"First.jpg");
//        out = new FileOutputStream(file);
//        bmOut1.compress(Bitmap.CompressFormat.JPEG, 100, out);
//        out.flush(); out.close();
        }
    }

    class ThreadOctave1 extends Thread { // This thread is used to speed up SIFT
        private int i, j, x, y;
        public void run (){
            // we use radius4 to boost the performance
            for (i=radius4;i<width-radius4;i++)
                for (j = radius4; j < height - radius4; j++) {
                    octave1000Second[i][j] = 0;
                    for (x = -radius1; x <= radius1; x++)
                        for (y = -radius1; y <= radius1; y++)
                            octave1000Second[i][j] = octave1000Second[i][j] + maskS1[x + radius1][y + radius1] * greyC[i + x][j + y];
//                    x=(int)Math.round(octave1000Second[i][j]);
//                bmOut1.setPixel(i, j, Color.argb(255, x, x, x));
                }
//        file = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+fileSeparator+"Temp"+fileSeparator+"Octave"+maxRes+"Second.jpg");
//        out = new FileOutputStream(file);
//        bmOut1.compress(Bitmap.CompressFormat.JPEG, 100, out);
//        out.flush(); out.close();
        }
    }

    class ThreadOctave2 extends Thread { // This thread is used to speed up SIFT
        private int i, j, x, y;
        public void run (){
            // we use radius4 to boost the performance
            for (i=radius4;i<width-radius4;i++)
                for (j = radius4; j < height - radius4; j++) {
                    octave1000Third[i][j] = 0;
                    for (x = -radius2; x <= radius2; x++)
                        for (y = -radius2; y <= radius2; y++)
                            octave1000Third[i][j] = octave1000Third[i][j] + maskS2[x + radius2][y + radius2] * greyC[i + x][j + y];
//                    x=(int)Math.round(octave1000Third[i][j]);
//                bmOut1.setPixel(i, j, Color.argb(255, x, x, x));
                }
//        file = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+fileSeparator+"Temp"+fileSeparator+"Octave"+maxRes+"Third.jpg");
//        out = new FileOutputStream(file);
//        bmOut1.compress(Bitmap.CompressFormat.JPEG, 100, out);
//        out.flush(); out.close();
        }
    }

    class ThreadOctave3 extends Thread { // This thread is used to speed up SIFT
        private int i, j, x, y;
        public void run (){
            // we use radius4 to boost the performance
            for (i=radius4;i<width-radius4;i++)
                for (j = radius4; j < height - radius4; j++) {
                    octave1000Fourth[i][j] = 0;
                    for (x = -radius3; x <= radius3; x++)
                        for (y = -radius3; y <= radius3; y++)
                            octave1000Fourth[i][j] = octave1000Fourth[i][j] + maskS3[x + radius3][y + radius3] * greyC[i + x][j + y];
//                    x=(int)Math.round(octave1000Fourth[i][j]);
//                bmOut1.setPixel(i, j, Color.argb(255, x, x, x));
                }
//        file = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+fileSeparator+"Temp"+fileSeparator+"Octave"+maxRes+"Fourth.jpg");
//        out = new FileOutputStream(file);
//        bmOut1.compress(Bitmap.CompressFormat.JPEG, 100, out);
//        out.flush(); out.close();
        }
    }

    class DoGFirst extends Thread { // This thread is used to speed up SIFT
        private int i, j;
        public void run (){
            for (i=MatrixBorder;i<width-MatrixBorder;i++)
                for (j=MatrixBorder;j<height-MatrixBorder;j++) {
                    DoG1000First[i][j]= octave1000First[i][j] - octave1000Second[i][j];
//                if (DoG1000First[i][j]>maxFirst) maxFirst=DoG1000First[i][j];
//                else if (DoG1000First[i][j]<minFirst) minFirst=DoG1000First[i][j];
//                x=(int)Math.round(DoG1000First[i][j]);
//                bmOut1.setPixel(i, j, Color.argb(255, x, x, x));
                }
        }
    }

    class DoGSecond extends Thread { // This thread is used to speed up SIFT
        private int i, j;
        public void run (){
            for (i=MatrixBorder;i<width-MatrixBorder;i++)
                for (j=MatrixBorder;j<height-MatrixBorder;j++) {
                    DoG1000Second[i][j]= octave1000Second[i][j] - octave1000Third[i][j];
//                if (DoG1000Second[i][j]>maxSecond) maxSecond=DoG1000Second[i][j];
//                else if (DoG1000Second[i][j]<minSecond) minSecond=DoG1000Second[i][j];
//                x=(int)Math.round(DoG1000Second[i][j]);
//                bmOut2.setPixel(i, j, Color.argb(255, x, x, x));
                }
        }
    }

    class DoGThird extends Thread { // This thread is used to speed up SIFT
        private int i, j;
        public void run (){
            for (i=MatrixBorder;i<width-MatrixBorder;i++)
                for (j=MatrixBorder;j<height-MatrixBorder;j++) {
                    DoG1000Third[i][j]= octave1000Third[i][j] - octave1000Fourth[i][j];
//                if (DoG1000Third[i][j]>maxThird) maxThird=DoG1000Third[i][j];
//                else if (DoG1000Third[i][j]<minThird) minThird=DoG1000Third[i][j];
//                x=(int)Math.round(DoG1000Third[i][j]);
//                bmOut3.setPixel(i, j, Color.argb(255, x, x, x));
                }
        }
    }

    class SIFTkeypointsSecond extends Thread { // This thread is used to speed up SIFT
        private int i, j, flagMax, flagMin, x, y;
        private double min, max, trace, det;
        private double [][] Hessian = new double [2][2]; // 2x2 Hessian matrix
        public void run (){
            nkSecond=0; // Number of keypoints in the 2nd part equals 0 initially
            for (i=MatrixBorder+1;i<width-MatrixBorder-1;i++)
                for (j=MatrixBorder+1;j<height-MatrixBorder-1;j++) {
                    // The following condition is for the THIRD DoG
                    if (Math.abs(DoG1000Third[i][j]) >= threshold) {// We exclude extrema of low contrast: |D(X)| must be gt 0.03, here pixels are between 0 and 1
                        // First, we check for maximum/minimum
                        flagMax = 1; flagMin = 1;
                        max = -1000; min = 1000;
                        for (x = -1; x <= 1; x++) {
                            for (y = -1; y <= 1; y++) {
                                if (DoG1000Third[i][j] <= DoG1000Second[i + x][j + y]) flagMax = 0;
                                if (DoG1000Third[i][j] >= DoG1000Second[i + x][j + y]) flagMin = 0;
                                if (DoG1000Second[i + x][j + y] > max) max = DoG1000Second[i + x][j + y];
                                else if (DoG1000Second[i + x][j + y] < min) min = DoG1000Second[i + x][j + y];
                            }
                            if (flagMax==0 && flagMin==0) break;
                        }
                        if ((flagMax == 1 && DoG1000Third[i][j] > max) || (flagMin == 1 && DoG1000Third[i][j] < min)) {
                            for (x = -1; x <= 1; x++) {
                                for (y = -1; y <= 1; y++) {
                                    if (DoG1000Third[i][j] <= DoG1000Fourth[i + x][j + y]) flagMax = 0;
                                    if (DoG1000Third[i][j] >= DoG1000Fourth[i + x][j + y]) flagMin = 0;
                                    if (DoG1000Fourth[i + x][j + y] > max) max = DoG1000Fourth[i + x][j + y];
                                    else if (DoG1000Fourth[i + x][j + y] < min) min = DoG1000Fourth[i + x][j + y];
                                }
                                if (flagMax==0 && flagMin==0) break;
                            }
                            if ((flagMax == 1 && DoG1000Third[i][j] > max) || (flagMin == 1 && DoG1000Third[i][j] < min)) {
                                for (x = -1; x <= 1; x++){
                                    for (y = -1; y <= 1; y++)
                                        if (x != 0 && y != 0) {
                                            if (DoG1000Third[i][j] <= DoG1000Third[i + x][j + y]) flagMax = 0;
                                            if (DoG1000Third[i][j] >= DoG1000Third[i + x][j + y]) flagMin = 0;
                                            if (DoG1000Third[i + x][j + y] > max) max = DoG1000Third[i + x][j + y];
                                            else if (DoG1000Third[i + x][j + y] < min) min = DoG1000Third[i + x][j + y];
                                        }
                                    if (flagMax==0 && flagMin==0) break;
                                }
                                if ((flagMax == 1 && DoG1000Third[i][j] > max) || (flagMin == 1 && DoG1000Third[i][j] < min)) {
                                    // Now, we eliminate the edges
                                    Hessian[0][0] = DoG1000Third[i + 1][j] + DoG1000Third[i - 1][j] - 2.0 * DoG1000Third[i][j];
                                    Hessian[1][1] = DoG1000Third[i][j + 1] + DoG1000Third[i][j - 1] - 2.0 * DoG1000Third[i][j];
                                    Hessian[0][1] = (DoG1000Third[i + 1][j + 1] - DoG1000Third[i + 1][j - 1] - DoG1000Third[i - 1][j + 1] + DoG1000Third[i - 1][j - 1]) * 0.25;
                                    trace = Hessian[0][0] + Hessian[1][1]; //Trace of a matrix
                                    det = Hessian[0][0] * Hessian[1][1] - Hessian[0][1] * Hessian[0][1]; // Determinant of a matrix
                                    // It was demonstrated that r = 10 is a good ratio, i.e. sqr(r+1)/r=121/10=12.1
                                    if (trace * trace / det < 12.1) {// r=10 here
                                        keypoints1000Second[nkSecond][0]=i; keypoints1000Second[nkSecond][1]=j;
                                        nkSecond++;
                                    }
                                }
                            }
                        }
                    }
                }
        }
    }
}