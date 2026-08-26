package com.clipper.android_clipper;

import android.content.ClipboardManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class ClipboardContentProvider extends ContentProvider {

    public static final String AUTHORITY = "com.clipper.android_clipper.provider";
    public static final String PATH_CLIPBOARD = "clipboard";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + PATH_CLIPBOARD);

    private static final int CLIPBOARD = 1;
    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // 使用 /data/data/<package>/files/ 目录下的文件
    private static final String CLIPBOARD_FILE = "clipboard_data.txt";

    static {
        uriMatcher.addURI(AUTHORITY, PATH_CLIPBOARD, CLIPBOARD);
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    private File getClipboardFile() {
        return new File(getContext().getFilesDir(), CLIPBOARD_FILE);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (uri == null) return null;

        if (uriMatcher.match(uri) == CLIPBOARD) {
            MatrixCursor cursor = new MatrixCursor(new String[]{"text", "timestamp"});
            String text = "";

            try {
                // Try to get from system clipboard first
                ClipboardManager cm = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null && cm.hasPrimaryClip()) {
                    android.content.ClipData clip = cm.getPrimaryClip();
                    if (clip != null && clip.getItemCount() > 0) {
                        text = clip.getItemAt(0).coerceToText(getContext()).toString();
                        // Also save to file for future access
                        saveToFile(text);
                    }
                }

                // If no system clipboard content, try reading from file
                if (text.isEmpty()) {
                    File file = getClipboardFile();
                    if (file.exists() && file.length() > 0) {
                        FileInputStream fis = new FileInputStream(file);
                        byte[] data = new byte[4096];
                        int len = fis.read(data);
                        fis.close();
                        if (len > 0) {
                            text = new String(data, 0, len, "UTF-8");
                        }
                    }
                }

                if (!text.isEmpty()) {
                    cursor.addRow(new Object[]{text, System.currentTimeMillis()});
                }
            } catch (Exception e) {
                // 忽略
            }
            return cursor;
        }
        return null;
    }

    @Override
    public String getType(Uri uri) {
        if (uri == null) return null;
        if (uriMatcher.match(uri) == CLIPBOARD) {
            return "vnd.android.cursor.item/vnd." + AUTHORITY + "." + PATH_CLIPBOARD;
        }
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        if (uri == null) return null;
        if (uriMatcher.match(uri) == CLIPBOARD) {
            String text = values.getAsString("text");
            if (text != null) {
                saveToFile(text);
                setSystemClipboard(text);
            }
        }
        return uri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if (uri == null) return 0;
        if (uriMatcher.match(uri) == CLIPBOARD) {
            saveToFile("");
            clearSystemClipboard();
            return 1;
        }
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (uri == null) return 0;
        if (uriMatcher.match(uri) == CLIPBOARD) {
            String text = values.getAsString("text");
            if (text != null) {
                saveToFile(text);
                setSystemClipboard(text);
                return 1;
            }
        }
        return 0;
    }

    private void saveToFile(String text) {
        try {
            File file = getClipboardFile();
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(text.getBytes("UTF-8"));
            fos.close();
        } catch (Exception e) {
            // 忽略
        }
    }

    private void setSystemClipboard(String text) {
        try {
            ClipboardManager cm = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                android.content.ClipData clip = android.content.ClipData.newPlainText("clipper", text);
                cm.setPrimaryClip(clip);
            }
        } catch (Exception e) {
            // 忽略
        }
    }

    private void clearSystemClipboard() {
        try {
            ClipboardManager cm = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.clearPrimaryClip();
            }
        } catch (Exception e) {
            // 忽略
        }
    }
}