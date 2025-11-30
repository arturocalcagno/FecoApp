package com.arturocalcagno.fecoapp;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;

public class FecoApp extends Activity {

    private static final String APK_FILE_NAME = "FecoApp.apk";
    private TextView resultado;
    private Button ingresar;
    private ProgressBar progressBar;

    private String versionInstalada;
    private String versionMatch;

    private Handler statusCheckHandler;
    private Runnable statusCheckRunnable;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        resultado = findViewById(R.id.txtResultadoPermisos);
        ingresar = findViewById(R.id.cmd_ingresar);
        progressBar = findViewById(R.id.progressBar);

        //Mostramos la versión instalada
        TextView version = findViewById(R.id.txtVersion);
        versionInstalada = getVersionName();
        version.setText(getString(R.string.version_format, versionInstalada));

        // Inicializamos el verificador de estado de la descarga
        statusCheckHandler = new Handler(Looper.getMainLooper());
        statusCheckRunnable = () -> {
            // Solo actuar si el botón está en modo "Descargando"
            if (ingresar != null && getString(R.string.button_downloading).equals(ingresar.getText().toString())) {
                if (isApkAlreadyDownloaded()) {
                    // Si el APK ya existe, la descarga terminó. Actualizamos la UI.
                    ingresar.setText(R.string.button_install);
                    ingresar.setEnabled(true);
                } else {
                    // Si no, volvemos a comprobar en 2 segundos.
                    statusCheckHandler.postDelayed(this.statusCheckRunnable, 2000);
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Si volvemos a la app y estaba descargando, reactivamos el verificador.
        if (ingresar != null && getString(R.string.button_downloading).equals(ingresar.getText().toString())) {
            statusCheckHandler.post(statusCheckRunnable);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Para ahorrar batería, detenemos el verificador cuando la app no está visible.
        statusCheckHandler.removeCallbacks(statusCheckRunnable);
    }

    private String getVersionName() {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("AppVersion", "Error al obtener el nombre de la versión", e);
            return "N/A";
        }
    }

    public void lanzarLogin(View view) {
        if (!internetDisponible()) {
            resultado.setText(R.string.status_no_connection);
            return;
        }

        String buttonText = ingresar.getText().toString();
        if (buttonText.equals(getString(R.string.button_update))) {
            iniciarActualizacion();
        } else if (buttonText.equals(getString(R.string.button_install))) {
            installDownloadedApk();
        } else { // INGRESAR
            iniciarApp();
        }
    }

    private void iniciarActualizacion() {
        File apkFile = getApkFile();
        if (apkFile != null && apkFile.exists()) {
            if (!apkFile.delete()) {
                Log.w("Update", "No se pudo eliminar el APK antiguo.");
            }
        }

        String apkUrl = "https://github.com/arturocalcagno/vendedores.fecoapp/releases/download/V2.0/FecoApp.apk";

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(apkUrl));
        request.setTitle(getString(R.string.update_title));
        request.setDescription(getString(R.string.update_downloading_notification));
        request.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, APK_FILE_NAME);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        downloadManager.enqueue(request);

        if (ingresar != null) {
            ingresar.setEnabled(false);
            ingresar.setText(R.string.button_downloading);
        }

        statusCheckHandler.post(statusCheckRunnable);

        Toast.makeText(this, R.string.download_started_notification, Toast.LENGTH_LONG).show();
    }

    private void iniciarApp() {
        progressBar.setVisibility(View.VISIBLE);
        ingresar.setEnabled(false);

        new Thread(() -> {
            boolean tieneInternet = internetDisponible();
            if (tieneInternet) {
                WebService ws = new WebService();
                versionMatch = ws.validarversion(versionInstalada);
                // versionMatch = ws.validarversionPrueba(versionInstalada);
            }

            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);

                if (!tieneInternet) {
                    resultado.setText(R.string.status_no_connection_cant_login);
                    ingresar.setEnabled(true);
                    return;
                }

                if (!versionInstalada.equals(versionMatch)) {
                    resultado.setText(R.string.status_new_version_available);
                    if (isApkAlreadyDownloaded()) {
                        ingresar.setText(R.string.button_install);
                    } else {
                        ingresar.setText(R.string.button_update);
                    }
                    ingresar.setEnabled(true);
                } else {
                    Intent i = new Intent(FecoApp.this, Principal.class);
                    startActivity(i);
                    finish();
                }
            });
        }).start();
    }

    private File getApkFile() {
        File downloadDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (downloadDir == null) {
            return null;
        }
        return new File(downloadDir, APK_FILE_NAME);
    }

    private boolean isApkAlreadyDownloaded() {
        File apkFile = getApkFile();
        return apkFile != null && apkFile.exists() && apkFile.length() > 0;
    }

    private void installDownloadedApk() {
        File apkFile = getApkFile();
        if (apkFile == null || !apkFile.exists()) {
            Toast.makeText(this, R.string.error_update_file_not_found, Toast.LENGTH_LONG).show();
            ingresar.setText(R.string.button_update);
            return;
        }

        // CORRECCIÓN: Se utiliza la autoridad correcta del FileProvider, que coincide con la del AndroidManifest.xml.
        Uri apkUri = FileProvider.getUriForFile(this, "com.arturocalcagno.fecoapp.FecoApp", apkFile);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.error_package_installer_not_found, Toast.LENGTH_SHORT).show();
        }
    }

    public boolean internetDisponible() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return false;
        }

        Network network = connectivityManager.getActiveNetwork();
        if (network == null) {
            return false;
        }

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        return capabilities != null && (
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    }
}
