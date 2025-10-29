package com.arturocalcagno.fecoapp;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

public class DownloadReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        long downloadId = -1;

        if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
            downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
        } else if (DownloadManager.ACTION_NOTIFICATION_CLICKED.equals(action)) {
            long[] ids = intent.getLongArrayExtra(DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS);
            if (ids != null && ids.length > 0) {
                downloadId = ids[0];
            }
        }

        if (downloadId == -1) {
            return;
        }

        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        Uri apkUri = downloadManager.getUriForDownloadedFile(downloadId);

        if (apkUri != null) {
            installAPK(context, apkUri);
        } else {
            Log.e("FecoAppUpdate", "No se pudo obtener la URI del archivo para el id de descarga: " + downloadId);
        }
    }

    private void installAPK(Context context, Uri uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e("FecoAppUpdate", "Error al iniciar la instalación del paquete.", e);
            Toast.makeText(context, "No se pudo iniciar la instalación del paquete.", Toast.LENGTH_LONG).show();
        }
    }
}
