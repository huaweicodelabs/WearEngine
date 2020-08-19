/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.huawei.wearengine.app.utils;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;

/**
 * File Manager
 *
 * @since 2020-08-05
 */
public class SelectFileManager {
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

    private SelectFileManager() {
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
}
