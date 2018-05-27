# Android_UseCameraAndGallery
7.0及以上系统(向下兼容6.0)拍照、读取系统相册、裁剪

```java
/**
 * Android拍照、调用系统相册、图片裁剪（兼容6.0权限处理及7.0以上文件管理）
 *
 *         <p>
 *         本文Demo包含以下要点：
 *         1、Android6.0运行时权限封装(避免用户选择不再提示后无法获取权限的问题)
 *         2、Android7.0 出现FileUriExposedException异常的问题（本例使用FileProvider解决）
 *         3、对照片进行裁剪
 *         4、PhotoUtils工具类对拍照和相册获取照片的封装
 *         5、自定义圆形头像CircleImageView
 *         6、BuildConfig.APPLICATION_ID 与 this.getPackageName()都可以获取App包名
 */
 
 <!--注意：
      authorities：app的包名.fileprovider
      exported：必须是false
      grantUriPermissions：必须是true，表示授予 URI 临时访问权限
  -->
  <provider
      android:name="android.support.v4.content.FileProvider"
      android:authorities="com.example.cameragallery.fileprovider"
      android:exported="false"
      android:grantUriPermissions="true">
      <meta-data
          android:name="android.support.FILE_PROVIDER_PATHS"
          android:resource="@xml/file_paths" />
  </provider>
  
  
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <paths>
        <!--
        external-path表示用来指定共享路径的，类似的还有：
        cache-path代表的根目录: getCacheDir()
        files-path代表的根目录： Context.getFilesDir()
        external-path代表的根目录: Environment.getExternalStorageDirectory()

        name：就是你给这个访问路径起个名字
        path：代表要共享的目录，即需要临时授权访问的路径（.代表所有路径）
        示例：path=""表示将整个SD卡进行共享，path="pictures"就只共享SD卡下的pictures文件夹
        -->
        <external-path name="pictures" path="" />
    </paths>
</resources>
```

![](https://github.com/ykmeory/Android_UseCameraAndGallery/blob/master/screenshot/demo.gif "screenshot")
![](https://github.com/ykmeory/Android_UseCameraAndGallery/blob/master/screenshot/img1.jpg "img1")
![](https://github.com/ykmeory/Android_UseCameraAndGallery/blob/master/screenshot/img2.jpg "img2")
