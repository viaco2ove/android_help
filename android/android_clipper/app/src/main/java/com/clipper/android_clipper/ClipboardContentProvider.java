package com.clipper.android_clipper;

import android.content.ClipboardManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
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
    public static final String PATH_EDIT_TEXT = "edit_text";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + PATH_CLIPBOARD);
    public static final Uri CONTENT_URI_EDIT_TEXT = Uri.parse("content://" + AUTHORITY + "/" + PATH_EDIT_TEXT);

    private static final int CLIPBOARD = 1;
    private static final int EDIT_TEXT = 2;
    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // 文件名
    private static final String CLIPBOARD_FILE = "clipboard_data.txt";
    private static final String EDIT_TEXT_FILE = "edit_text_data.txt";

    static {
        uriMatcher.addURI(AUTHORITY, PATH_CLIPBOARD, CLIPBOARD);
        uriMatcher.addURI(AUTHORITY, PATH_EDIT_TEXT, EDIT_TEXT);
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (uri == null) return null;

        int match = uriMatcher.match(uri);
        if (match == CLIPBOARD) {
            return queryClipboard(uri);
        } else if (match == EDIT_TEXT) {
            return queryEditText(uri);
        }
        return null;
    }

    private Cursor queryClipboard(Uri uri) {
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
                    saveToFile(CLIPBOARD_FILE, text);
                }
            }

            // If no system clipboard content, try reading from file
            if (text.isEmpty()) {
                text = readFromFile(CLIPBOARD_FILE);
            }

            if (!text.isEmpty()) {
                cursor.addRow(new Object[]{text, System.currentTimeMillis()});
            }
        } catch (Exception e) {
            // ignore
        }
        return cursor;
    }

    private Cursor queryEditText(Uri uri) {
        MatrixCursor cursor = new MatrixCursor(new String[]{"text", "timestamp"});
        String text = readFromFile(EDIT_TEXT_FILE);

        if (!text.isEmpty()) {
            cursor.addRow(new Object[]{text, System.currentTimeMillis()});
        }
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        if (uri == null) return null;
        int match = uriMatcher.match(uri);
        if (match == CLIPBOARD) {
            return "vnd.android.cursor.item/vnd." + AUTHORITY + "." + PATH_CLIPBOARD;
        } else if (match == EDIT_TEXT) {
            return "vnd.android.cursor.item/vnd." + AUTHORITY + "." + PATH_EDIT_TEXT;
        }
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        if (uri == null) return null;

        int match = uriMatcher.match(uri);
        if (match == CLIPBOARD || match == EDIT_TEXT) {
            String text = values.getAsString("text");
            if (text != null) {
                String filename = (match == CLIPBOARD) ? CLIPBOARD_FILE : EDIT_TEXT_FILE;
                saveToFile(filename, text);
                // Also set system clipboard for clipboard path
                if (match == CLIPBOARD) {
                    setSystemClipboard(text);
                }
            }
        }
        return uri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if (uri == null) return 0;

        int match = uriMatcher.match(uri);
        if (match == CLIPBOARD || match == EDIT_TEXT) {
            String filename = (match == CLIPBOARD) ? CLIPBOARD_FILE : EDIT_TEXT_FILE;
            saveToFile(filename, "");
            if (match == CLIPBOARD) {
                clearSystemClipboard();
            }
            return 1;
        }
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (uri == null) return 0;

        int match = uriMatcher.match(uri);
        if (match == CLIPBOARD || match == EDIT_TEXT) {
            String text = values.getAsString("text");
            if (text != null) {
                String filename = (match == CLIPBOARD) ? CLIPBOARD_FILE : EDIT_TEXT_FILE;
                saveToFile(filename, text);
                if (match == CLIPBOARD) {
                    setSystemClipboard(text);
                }
                return 1;
            }
        }
        return 0;
    }

    private String readFromFile(String filename) {
        try {
            File file = new File(getContext().getFilesDir(), filename);
            if (file.exists() && file.length() > 0) {
                FileInputStream fis = new FileInputStream(file);
                byte[] data = new byte[4096];
                int len = fis.read(data);
                fis.close();
                if (len > 0) {
                    return new String(data, 0, len, "UTF-8");
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return "";
    }

    private void saveToFile(String filename, String text) {
        try {
            File file = new File(getContext().getFilesDir(), filename);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(text.getBytes("UTF-8"));
            fos.close();
        } catch (Exception e) {
            // ignore
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
            // ignore
        }
    }

    private void clearSystemClipboard() {
        try {
            ClipboardManager cm = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.clearPrimaryClip();
            }
        } catch (Exception e) {
            // ignore
        }
    }
}