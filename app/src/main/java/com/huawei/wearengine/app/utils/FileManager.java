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
     * Query the real file path from the database
     *
     * @param context context
     * @param contentUri Uri address of the file 
     * @return Return the file path
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
     * Get the file path 
     *
     * @param context context
     * @param contentUri Uri address of the file
     * @return Return the file path
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
     * File path: content://File path at the beginning
     *
     * @param context context
     * @param uri Uri address of the file
     * @return Return the file path
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
     * Process Document Uri
     *
     * @param context context
     * @param uri Uri address of the file
     * @return Return the file path
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
     * Return file path according to Document id 
     *
     * @param context context
     * @param documentId documentId of the file
     * @return Return the file path
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
     * Confirm the selected file type
     *
     * @param uri Uri address of the file 
     * @return if the file is ExternalStorageDocument type
     */
    private static boolean isExternalStorage(Uri uri) {
        return FILE_TYPE_STORAGE.equals(uri.getAuthority());
    }

    /**
     * Confirm the selected file type
     *
     * @param uri Uri address of the file 
     * @return if the file is Download type
     */
    private static boolean isDownloads(Uri uri) {
        return FILE_TYPE_DOWNLOAD.equals(uri.getAuthority());
    }

    /**
     * Confirm the selected file type
     *
     * @param uri Uri address of the file
     * @return if the file is Media type
     */
    private static boolean isMedia(Uri uri) {
        return FILE_TYPE_MEDIA.equals(uri.getAuthority());
    }

    /**
     * Get the corresponding Uri to the file type
     *
     * @param type File type
     * @return Return the corresponding Uri to the file type
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
     * Get external storage path according to uri
     *
     * @param uri Uri address of the file 
     * @return The file gets external storage path
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
     * Get value of the Uri data
     *
     * @param context context
     * @param uri the Uri needs query
     * @param selection The filter used in the query
     * @param selectionArgs The selected parameter used in the query
     * @return Return the file path
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
     * Create uri of image address to save photos after shooting in Android 10 or later 
     *
     * @param context context
     * @return Image uri
     */
    public static Uri createImageUri(Context context) {
        String status = Environment.getExternalStorageState();

        // If there is an SD card, SD card will be precedently used; otherwise phone storage will be used.
        if (status.equals(Environment.MEDIA_MOUNTED)) {
            return context.getContentResolver()
                .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new ContentValues());
        } else {
            return context.getContentResolver()
                .insert(MediaStore.Images.Media.INTERNAL_CONTENT_URI, new ContentValues());
        }
    }

    /**
     * Create file of saving images
     *
     * @param context Context
     * @return File File information
     * @throws IOException IO exception
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
     * Get the path of the compressed file
     *
     * @param context Context
     * @param fileUri Original image path
     * @return String File path of the compressed file
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

    // Compress image size
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
        // Proportional compression
        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inSampleSize = 1; 
        bitmapOptions.inDither = true;
        bitmapOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
        bitmapOptions.inJustDecodeBounds = false;
        input = context.getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions);
        input.close();
        return zoomBitmap(bitmap, 454, 454);
    }

    // Transfer the image file into bin format
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
        // The arrays of all the pixels, weight by height of image
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
        float width = bitmap.getWidth();// Get width and height of the image
        float height = bitmap.getHeight();
        float resultWidth = 0;
        float resultHeight = 0;
        float scaleWidht, scaleHeight, xBegin, yBegin;// Zoom ratio and X, Y axis translation position
        Bitmap newbmp = null; // New images
        Matrix matrix = new Matrix();// Transform matrix
        if ((width / height) <= vw / vh) {
            // When the aspect ratio is greater than the requirement, the multiple of the width is taken as the zoom factor
            scaleWidht = vw / width;
            scaleHeight = scaleWidht;
            // To get Y coordinate in the bitmap source file,it needs the translation pixels
            yBegin = (height - width) / 2;
            xBegin = 0;
            resultWidth = width - xBegin;
            resultHeight = height - 2 * yBegin;
        } else {
            scaleWidht = vh / height;
            scaleHeight = scaleWidht;
            // To get Y coordinate in the bitmap source file,it needs the translation pixels
            xBegin = (width - height) / 2;
            yBegin = 0;
            resultWidth = width - 2 * xBegin;
            resultHeight = height - yBegin;
        }
        matrix.postScale(scaleWidht / 1f, scaleHeight / 1f);
        try {
            if (width - xBegin > 0 && height - yBegin > 0 && bitmap != null)
                // （Original image, X-axis start position, Y-axis start-up position, X-axis end position, Y-axis end position, zoom matrix, whether to filter the original image)Take absolute value to prevent error reporting
                newbmp = Bitmap.createBitmap(bitmap, (int)Math.abs(xBegin), (int)Math.abs(yBegin),
                    (int)Math.abs(resultWidth), (int)Math.abs(resultHeight), matrix, true);
        } catch (Exception e) {
            // If an error is reported, it will return to the original image, not blank
            e.printStackTrace();
            return bitmap;
        }
        return newbmp;
    }
}
