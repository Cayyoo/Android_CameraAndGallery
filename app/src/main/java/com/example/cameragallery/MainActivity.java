package com.example.cameragallery;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;

/**
 * Android拍照、调用系统相册、图片裁剪（兼容6.0权限处理及7.0以上文件管理）
 *
 * @author Administrator
 *         <p>
 *         本文Demo包含以下要点：
 *         1、Android6.0运行时权限封装(避免用户选择不再提示后无法获取权限的问题)
 *         2、Android7.0 出现FileUriExposedException异常的问题（本例使用FileProvider解决）
 *         3、对照片进行裁剪
 *         4、PhotoUtils工具类对拍照和相册获取照片的封装
 *         5、自定义圆形头像CircleImageView
 *         6、BuildConfig.APPLICATION_ID 与 this.getPackageName()都可以获取App包名
 */
public class MainActivity extends BaseActivity implements View.OnClickListener {
    private ImageView photo;


    /**
     * 图库请求码
     */
    private static final int REQUEST_CODE_GALLERY = 0xa0;
    /**
     * 相机请求码
     */
    private static final int REQUEST_CODE_CAMERA = 0xa1;
    /**
     * 裁剪请求码
     */
    private static final int REQUEST_CODE_CROP = 0xa2;


    /**
     * 拍照所得原图
     */
    private File photographedFile = new File(Environment.getExternalStorageDirectory().getPath() + "/IMG_" + System.currentTimeMillis() + ".jpg");
    /**
     * 修剪后的图片
     */
    private File cropFile = new File(Environment.getExternalStorageDirectory().getPath() + "/IMG_" + System.currentTimeMillis() + "_crop.jpg");


    /**
     * 拍照所得原图Uri
     */
    private Uri imageUri;
    /**
     * 拍照后回调，即修剪后的Uri
     */
    private Uri cropImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnTakePhoto = (Button) findViewById(R.id.take_photo);
        Button btnTakeGallery = (Button) findViewById(R.id.take_gallery);
        photo = (ImageView) findViewById(R.id.photo);

        btnTakePhoto.setOnClickListener(this);
        btnTakeGallery.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            //拍照
            case R.id.take_photo:
                requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE}, new RequestPermissionCallBack() {
                    @Override
                    public void granted() {
                        if (hasSdcard()) {
                            imageUri = Uri.fromFile(photographedFile);

                            //API 24(7.0)以上
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                //通过FileProvider创建一个content类型的Uri
                                imageUri = FileProvider.getUriForFile(
                                        MainActivity.this,
                                        MainActivity.this.getPackageName() + ".fileprovider",
                                        photographedFile
                                );
                            }

                            PhotoUtils.takePicture(MainActivity.this, imageUri, REQUEST_CODE_CAMERA);
                        } else {
                            Toast.makeText(MainActivity.this, "设备没有SD卡", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void denied() {
                        Toast.makeText(MainActivity.this, "您未授权", Toast.LENGTH_LONG).show();
                    }
                });
                break;
            //打开图库
            case R.id.take_gallery:
                requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, new RequestPermissionCallBack() {
                    @Override
                    public void granted() {
                        PhotoUtils.openPic(MainActivity.this, REQUEST_CODE_GALLERY);
                    }

                    @Override
                    public void denied() {
                        Toast.makeText(MainActivity.this, "您未授权", Toast.LENGTH_LONG).show();
                    }
                });
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //裁剪图片宽高
        int outputX = 480, outputY = 480;

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                //拍照完成回调
                case REQUEST_CODE_CAMERA:
                    cropImageUri = Uri.fromFile(cropFile);
                    PhotoUtils.cropImageUri(this, imageUri, cropImageUri, 1, 1, outputX, outputY, REQUEST_CODE_CROP);
                    break;
                //访问相册完成回调
                case REQUEST_CODE_GALLERY:
                    if (hasSdcard()) {
                        cropImageUri = Uri.fromFile(cropFile);

                        Uri newUri = Uri.parse(PhotoUtils.getPath(this, data.getData()));

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            newUri = FileProvider.getUriForFile(
                                    this,
                                    BuildConfig.APPLICATION_ID + ".fileprovider",
                                    new File(newUri.getPath())
                            );
                        }

                        PhotoUtils.cropImageUri(this, newUri, cropImageUri, 1, 1, outputX, outputY, REQUEST_CODE_CROP);
                    } else {
                        Toast.makeText(MainActivity.this, "设备没有SD卡", Toast.LENGTH_SHORT).show();
                    }
                    break;
                //裁剪回调
                case REQUEST_CODE_CROP:
                    Bitmap bitmap = PhotoUtils.getBitmapFromUri(cropImageUri, this);
                    if (bitmap != null) {
                        showImages(bitmap);
                    }

                    /*
                    拍照图片、拍照后裁剪所得图片，都已被自动保存在SDCard上，但是打开系统相册时却无法显示。
                    此处发送广播让MediaScanner扫描制定的文件，在系统相册中就可以找到拍摄的照片了，

                    注意：
                        打开图库，能看到的只是拍照后裁剪所得照片，拍照原图还是看不到。
                        且，保存的是最后一次裁剪过的图片，之前裁剪过的图片会被替换。
                        若要保留每次裁剪后的图片，请在拍照、图库回调后分别调用以下代码。
                     */
                    Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    intent.setData(cropImageUri);
                    sendBroadcast(intent);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 将图片显示ImageView组件
     */
    private void showImages(Bitmap bitmap) {
        photo.setImageBitmap(bitmap);
    }

    /**
     * 检查设备是否存在SDCard的工具方法
     */
    public static boolean hasSdcard() {
        String state = Environment.getExternalStorageState();
        return state.equals(Environment.MEDIA_MOUNTED);
    }

}

