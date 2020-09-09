/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.huawei.wearengine.app.utils;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.os.EnvironmentCompat;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * File Manager
 *
 * @since 2020-08-05
 */
public class FileManager {
    private static final String TAG = "SelectFileManager";

    private static final String FILE_DOCUMNET_ID_RAW = "raw:";

    private static final String FILE_DOWNLOAD_CONTENT = "content://downloads/public_downloads";

    private static final String FILE_SCHEME_CONTENT = "content";

    private static final String FILE_SCHEME_FILE = "file";

    private static final String FILE_DOCUMENT_TYPE = "primary";

    private static final String FILE_CONTENT_TYLE_AUDIO = "audio";

    private static final String FILE_CONTENT_TYLE_IMAGE = "image";

    private static final String FILE_CONTENT_TYLE_VIDEO = "video";

    private static final String FILE_TYPE_STORAGE = "com.android.externalstorage.documents";

    private static final String FILE_TYPE_DOWNLOAD = "com.android.providers.downloads.documents";

    private static final String FILE_TYPE_MEDIA = "com.android.providers.media.documents";

    private static final String FILE_QUERY_ID = "_id=?";

    private static final String FILE_QUERY_DATA = "_data";

    private static final String FILE_SPLIT = ":";

    private static final String STRING_NULL_CONTENT = "";

    private static final String SCHEME_FILE = "/root";

    private static final int INDEX_ONE = 0;

    private static final int INDEX_TWO = 1;

    private FileManager() {
    }

    /**
     * 从文件数据库中查询获取真正的文件路径
     *
     * @param context context上下文
     * @param contentUri 文件Uri地址
     * @return 返回文件的路径
     */
    public static String getFilePath(Context context, Uri contentUri) {
        String selectFilePath = null;
        int sdkInit = Build.VERSION.SDK_INT;
        int kitkat = Build.VERSION_CODES.KITKAT;
        boolean isKitKat = sdkInit >= kitkat;
        if (isKitKat) {
            selectFilePath = getContentPath(context, contentUri);
        } else {
            selectFilePath = getRealPathFromUri(context, contentUri);
        }
        return selectFilePath;
    }

    /**
     * 获取文件路径
     *
     * @param context context上下文
     * @param contentUri 文件Uri地址
     * @return 返回文件的路径
     */
    private static String getRealPathFromUri(Context context, Uri contentUri) {
        String result = null;
        if ((context == null) || (contentUri == null)) {
            Log.w(TAG, "context or contentUri is null");
            return result;
        }
        String[] data = {MediaStore.Images.Media.DATA};
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(contentUri, data, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                result = cursor.getString(columnIndex);
                return result;
            }
        } catch (IllegalArgumentException exception) {
            Log.e(TAG, "getRealPathFromUri IllegalArgumentException");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return result;
    }

    /**
     * 获取文件路径为content://开头时文件路径
     *
     * @param context context上下文
     * @param uri 文件Uri地址
     * @return 返回文件路径
     */
    private static String getContentPath(Context context, Uri uri) {
        String result = null;
        if ((context == null) || (uri == null)) {
            Log.w(TAG, "context or uri is null");
            return result;
        }
        if (DocumentsContract.isDocumentUri(context, uri)) {
            result = dealDocument(context, uri);
        } else if (FILE_SCHEME_CONTENT.equalsIgnoreCase(uri.getScheme())) {
            Log.i(TAG, "content");
            result = getColumn(context, uri, null, null);
            return result;
        } else if (FILE_SCHEME_FILE.equalsIgnoreCase(uri.getScheme())) {
            result = uri.getPath();
            return result;
        } else {
            Log.w(TAG, "the uri is other type");
        }
        return result;
    }

    /**
     * 处理Document的Uri
     *
     * @param context context上下文
     * @param uri 文件Uri地址
     * @return 返回文件路径
     */
    private static String dealDocument(Context context, Uri uri) {
        String result = null;
        if (isExternalStorage(uri)) {
            result = getExternalStorage(uri);
        } else if (isDownloads(uri)) {
            Log.i(TAG, "download");
            try {
                final String id = DocumentsContract.getDocumentId(uri);
                if (id.startsWith(FILE_DOCUMNET_ID_RAW)) {
                    result = id.replaceFirst(FILE_DOCUMNET_ID_RAW, STRING_NULL_CONTENT);
                    return result;
                }
                Uri contentUri = ContentUris.withAppendedId(Uri.parse(FILE_DOWNLOAD_CONTENT), Long.parseLong(id));
                result = getColumn(context, contentUri, null, null);
                return result;
            } catch (NumberFormatException exception) {
                Log.e(TAG, "getContentPath NumberFormatException");
            }
        } else if (isMedia(uri)) {
            String documentId = DocumentsContract.getDocumentId(uri);
            if (documentId != null) {
                result = dealWithDocumentId(context, documentId);
                return result;
            }
        } else {
            Log.w(TAG, "other type");
        }
        return result;
    }

    /**
     * 根据Document的id返回文件路径
     *
     * @param context context上下文
     * @param documentId 文件documentId
     * @return 返回文件路径
     */
    private static String dealWithDocumentId(Context context, String documentId) {
        String[] splits = documentId.split(FILE_SPLIT);
        String type = splits[INDEX_ONE];
        String result = null;
        if (type != null) {
            Uri contentUri = getContentUri(type);
            if (contentUri == null) {
                Log.w(TAG, "contentUri is null");
            } else {
                result = getColumn(context, contentUri, FILE_QUERY_ID, new String[] {splits[INDEX_TWO]});
                return result;
            }
        }
        return result;
    }

    /**
     * 确定所选的文件类型
     *
     * @param uri 文件Uri地址
     * @return 文件是否为ExternalStorageDocument类型
     */
    private static boolean isExternalStorage(Uri uri) {
        return FILE_TYPE_STORAGE.equals(uri.getAuthority());
    }

    /**
     * 确定所选的文件类型
     *
     * @param uri 文件Uri地址
     * @return 文件是否为Download类型
     */
    private static boolean isDownloads(Uri uri) {
        return FILE_TYPE_DOWNLOAD.equals(uri.getAuthority());
    }

    /**
     * 确定所选的文件类型
     *
     * @param uri 文件Uri地址
     * @return 文件是否为Media类型
     */
    private static boolean isMedia(Uri uri) {
        return FILE_TYPE_MEDIA.equals(uri.getAuthority());
    }

    /**
     * 获取文件类型所对应的Uri
     *
     * @param type 文件类型
     * @return 文件类型所对应的Uri
     */
    private static Uri getContentUri(String type) {
        Uri contentUri = null;
        if (FILE_CONTENT_TYLE_IMAGE.equals(type)) {
            contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        } else if (FILE_CONTENT_TYLE_VIDEO.equals(type)) {
            contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        } else if (FILE_CONTENT_TYLE_AUDIO.equals(type)) {
            contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        } else {
            Log.w(TAG, "the contentUri is null");
        }
        return contentUri;
    }

    /**
     * 根据uri获取获取外部存储路径
     *
     * @param uri 文件Uri地址
     * @return 文件获取外部存储路径
     */
    private static String getExternalStorage(Uri uri) {
        String documentId = DocumentsContract.getDocumentId(uri);
        String result = null;
        if (documentId != null) {
            String[] splits = documentId.split(FILE_SPLIT);
            String type = splits[INDEX_ONE];
            if (type != null && FILE_DOCUMENT_TYPE.equalsIgnoreCase(type)) {
                result = Environment.getExternalStorageDirectory() + File.separator + splits[INDEX_TWO];
            }
        }
        return result;
    }

    /**
     * 获取此Uri的data列的值。
     *
     * @param context context上下文
     * @param uri 要查询的Uri
     * @param selection 查询中使用的过滤器
     * @param selectionArgs 查询中使用的选择参数
     * @return 返回文件路径
     */
    private static String getColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        String columnName = FILE_QUERY_DATA;
        String[] projections = {columnName};
        String result = null;
        try {
            cursor = context.getContentResolver().query(uri, projections, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                result = cursor.getString(cursor.getColumnIndexOrThrow(columnName));
                return result;
            }
        } catch (IllegalArgumentException exception) {
            Log.e(TAG, "getColumn IllegalArgumentException");
            if (!TextUtils.isEmpty(uri.getPath())) {
                result = uri.getPath().replace(SCHEME_FILE, "");
                return result;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return result;
    }

    /**
     * 创建图片地址uri,用于保存拍照后的照片 Android 10以后使用这种方法
     *
     * @param context 应用上下文
     * @return 图片的uri
     */
    public static Uri createImageUri(Context context) {
        String status = Environment.getExternalStorageState();

        // 判断是否有SD卡,优先使用SD卡存储,当没有SD卡时使用手机存储
        if (status.equals(Environment.MEDIA_MOUNTED)) {
            return context.getContentResolver()
                .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new ContentValues());
        } else {
            return context.getContentResolver()
                .insert(MediaStore.Images.Media.INTERNAL_CONTENT_URI, new ContentValues());
        }
    }

    /**
     * 创建保存图片的文件
     *
     * @param context 应用上下文
     * @return File 文件信息
     * @throws IOException IO异常
     */
    public static File createImageFile(Context context) throws IOException {
        String imageName = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (!storageDir.exists()) {
            storageDir.mkdir();
        }
        File tempFile = new File(storageDir, imageName);
        if (!Environment.MEDIA_MOUNTED.equals(EnvironmentCompat.getStorageState(tempFile))) {
            return null;
        }
        return tempFile;
    }

    /**
     * 获取压缩后的文件路径
     *
     * @param context 上下文
     * @param fileUri 原始图片路径
     * @return String 压缩后的文件路径
     */
    public static String getPathAfterCompressed(Context context, Uri fileUri) {
        String savePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        File filePic = new File(savePath);
        try {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                savePath = Environment.getExternalStorageDirectory().getAbsolutePath();
            } else {
                savePath = context.getApplicationContext().getFilesDir().getAbsolutePath();
            }
            filePic = new File(savePath + "/Pictures/"
                + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".bin");
            Log.d(TAG, "image path is " + filePic);

            if (!filePic.exists()) {
                filePic.createNewFile();
            }
            Bitmap bitmap = getBitmapFormUri(context, fileUri);
            saveBitMap(bitmap, context);
            imgToBin(bitmap, filePic);
        } catch (IOException e) {
            Log.e(TAG, "Compressed Picture error", e);
        }
        return filePic.getAbsolutePath();
    }

    private static void saveBitMap(Bitmap bitmap, Context context) throws IOException {
        String savePath;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            savePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        } else {
            savePath = context.getApplicationContext().getFilesDir().getAbsolutePath();
        }

        File filePic;
        filePic = new File(savePath + "/Pictures/"
            + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".jpeg");
        if (!filePic.exists()) {
            filePic.createNewFile();
        }

        try {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePic));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bos.flush();
            bos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "bmp path is " + filePic);
    }

    // 图片大小压缩
    private static Bitmap getBitmapFormUri(Context context, Uri uri) throws FileNotFoundException, IOException {
        BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
        onlyBoundsOptions.inJustDecodeBounds = true;
        onlyBoundsOptions.inDither = true;
        onlyBoundsOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
        InputStream input = context.getContentResolver().openInputStream(uri);
        Bitmap bitmap1 = BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
        input.close();
        int originalWidth = onlyBoundsOptions.outWidth;
        int originalHeight = onlyBoundsOptions.outHeight;
        if ((originalWidth == -1) || (originalHeight == -1)) {
            return null;
        }
        // 比例压缩
        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inSampleSize = 1; // 设置缩放比例
        bitmapOptions.inDither = true;
        bitmapOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
        bitmapOptions.inJustDecodeBounds = false;
        input = context.getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions);
        input.close();
        return zoomBitmap(bitmap, 454, 454);
    }

    // 将图片文件转换为bin格式
    private static void imgToBin(Bitmap bitmap, File file) {

        try {
            byte[] bytes = getPicturePixel(bitmap);
            FileOutputStream out = new FileOutputStream(file);
            out.write(bytes);
            out.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static byte[] getPicturePixel(Bitmap bitmap) {

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int pixelSize = 4;
        int headSize = 8;

        int colorMode = 1 << 8 + 0;
        int widthBitOffset = 0;
        int heightBitOffset = 16;
        int header = (bitmap.getWidth() << widthBitOffset) + (bitmap.getHeight() << heightBitOffset);
        // 所有的像素的数组，图片宽×高
        int[] pixels = new int[width * height];
        byte[] result = new byte[width * height * pixelSize + headSize];

        int index = 0;

        result[index++] = (byte)(colorMode & 0xFF);
        result[index++] = (byte)((colorMode >> 8) & 0xFF);
        result[index++] = (byte)((colorMode >> 16) & 0xFF);
        result[index++] = (byte)((colorMode >> 24) & 0xFF);

        result[index++] = (byte)(header & 0xFF);
        result[index++] = (byte)((header >> 8) & 0xFF);
        result[index++] = (byte)((header >> 16) & 0xFF);
        result[index++] = (byte)((header >> 24) & 0xFF);

        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int i = 0; i < pixels.length; i++) {
            int clr = pixels[i];
            int alpha = (clr & 0xff000000) >> 24;
            int red = (clr & 0x00ff0000) >> 16;
            int green = (clr & 0x0000ff00) >> 8;
            int blue = clr & 0x000000ff;
            // Log.d("tag", "r=" + red + ",g=" + green + ",b=" + blue);
            result[index++] = (byte)blue;
            result[index++] = (byte)green;
            result[index++] = (byte)red;
            result[index++] = (byte)alpha;
        }
        return result;
    }

    public static Bitmap zoomBitmap(Bitmap bitmap, float vw, float vh) {
        float width = bitmap.getWidth();// 获得图片宽高
        float height = bitmap.getHeight();
        float resultWidth = 0;
        float resultHeight = 0;
        float scaleWidht, scaleHeight, xBegin, yBegin;// 图片缩放倍数以及x，y轴平移位置
        Bitmap newbmp = null; // 新的图片
        Matrix matrix = new Matrix();// 变换矩阵
        if ((width / height) <= vw / vh) {
            // 当宽高比大于所需要尺寸的宽高比时以宽的倍数为缩放倍数
            scaleWidht = vw / width;
            scaleHeight = scaleWidht;
            // 获取bitmap源文件中y做表需要偏移的像数大小
            yBegin = (height - width) / 2;
            xBegin = 0;
            resultWidth = width - xBegin;
            resultHeight = height - 2 * yBegin;
        } else {
            scaleWidht = vh / height;
            scaleHeight = scaleWidht;
            // 获取bitmap源文件中x做表需要偏移的像数大小
            xBegin = (width - height) / 2;
            yBegin = 0;
            resultWidth = width - 2 * xBegin;
            resultHeight = height - yBegin;
        }
        matrix.postScale(scaleWidht / 1f, scaleHeight / 1f);
        try {
            if (width - xBegin > 0 && height - yBegin > 0 && bitmap != null)
                // （原图，x轴起始位置，y轴起始位置，x轴结束位置，Y轴结束位置，缩放矩阵，是否过滤原图）为防止报错取绝对值
                newbmp = Bitmap.createBitmap(bitmap, (int)Math.abs(xBegin), (int)Math.abs(yBegin),
                    (int)Math.abs(resultWidth), (int)Math.abs(resultHeight), matrix, true);
        } catch (Exception e) {
            // 如果报错则返回原图，不至于为空白
            e.printStackTrace();
            return bitmap;
        }
        return newbmp;
    }
}
