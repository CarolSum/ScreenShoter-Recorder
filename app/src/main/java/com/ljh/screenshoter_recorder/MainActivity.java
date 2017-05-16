package com.ljh.screenshoter_recorder;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {

    private static final int SCREEN_SHOT = 0;
    private Button screenShotBt;

    private ImageView screenShotPic;

    private MediaProjectionManager projectionManager;

    private MediaProjection mediaProjection;

    private ImageReader imageReader;

    String imageName;
    Bitmap bitmap;

    int width;
    int height;
    int dpi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        screenShotBt = (Button) findViewById(R.id.screen_shot_button);
        screenShotPic = (ImageView) findViewById(R.id.screen_shot_pic);
        //通过DisplayMetrics获取显示的一些相关信息,width, height, api 等
        DisplayMetrics metric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metric);
        width = metric.widthPixels;
        height = metric.heightPixels;
        dpi = metric.densityDpi;
        //获取MediaProjectionManager
        projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

        screenShotBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    //通过 MediaProjectionManager.createScreenCaptureIntent() 获取 Intent
                    startActivityForResult(projectionManager.createScreenCaptureIntent(),SCREEN_SHOT);
                }
            }
        });
    }

    //处理返回逻辑
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == SCREEN_SHOT){
            if(resultCode == RESULT_OK){
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    //通过 MediaProjectionManager.getMediaProjection(resultCode,data) 获取 MediaProjection
                    mediaProjection = projectionManager.getMediaProjection(resultCode, data);
                    //创建 ImageReader,构建 VirtualDisplay
                    /**
                     * @param：width/height: 指定生成图像的宽和高
                     * @param: format 是图像的格式，这个格式必须是 ImageFormat或 PixelFormat 中的一个
                     * @param: maxImages 这个参数指的是你想同时在 ImageReader 里获取到的Image对象的个数
                     */
                    imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 1);

                    /**
                     * @param: 前4个分别为VirtualDisplay 的名字，宽，高和dpi。
                     * @param: 第5个参数为DisplayManager 的flag
                     * @param: 第六个参数是一个 Surface。当createVirtualDisplay 调用后，在真实屏幕上的每一帧都会输入到 Surface参数 里。
                     * @param: 第7个参数是一个回调函数，第8个参数是一个Handler
                     */
                    mediaProjection.createVirtualDisplay("ScreenShot", width, height, dpi,
                            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, imageReader.getSurface(), null, null);
                    SystemClock.sleep(100);//这里如果不sleep的话会崩？
                    imageName = System.currentTimeMillis() + ".png";
                    //获取image对象并转化为Bitmap对象
                    Image image = imageReader.acquireNextImage();
                    //这里得到的width, height 是像素格式
                    int width = image.getWidth();
                    int height = image.getHeight();
                    final Image.Plane[] planes = image.getPlanes();
                    //获取 ByteBuffer，里面存放的就是图片的字节流，是字节格式的
                    final ByteBuffer buffer = planes[0].getBuffer();
                    int pixelStride = planes[0].getPixelStride();//两个像素的距离(就是一个像素头部到相邻像素的头部)，字节格式。
                    int rowStride = planes[0].getRowStride();//一行占用的距离(就是一行像素头部到相邻行像素的头部)，这个大小和 width 有关，这里需要注意，因为内存对齐的原因，所以每行会有一些空余。这个值也是字节格式的。
                    int rowPadding = rowStride - pixelStride * width;//用整行的距离减去了一行里像素及空隙占用的距离，剩下的就是空余部分。但是这个是字节格式的
                    //rowPadding需转为像素格式
                    bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
                    //创建出合适大小的Bitmap后把Image的buffer传给它就可以得道Bitmap
                    bitmap.copyPixelsFromBuffer(buffer);
                    image.close();
                    if(bitmap != null){
                        screenShotPic.setImageBitmap(bitmap);
                    }
                }
            }
        }
    }
}
