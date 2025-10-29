package com.arturocalcagno.fecoapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;

public class FecoApp extends Activity{

    private TextView resultado;
    public String versioninstalada;
    public String versionmatch;
    private Button ingresar;
    public Boolean internet;
    private Boolean fin;
    private static final String APK_FILE_NAME = "FecoApp3.apk";

    private Handler statusCheckHandler;
    private Runnable statusCheckRunnable;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        resultado = findViewById(R.id.txtResultadoPermisos);
        DB db = new DB(this);
        ingresar = findViewById(R.id.cmd_ingresar);
        //Mostramos la versión instalada
        TextView version = findViewById(R.id.txtVersion);
        versioninstalada = getVersionName();
        version.setText("Versión " + versioninstalada);

        // Inicializamos el verificador de estado
        statusCheckHandler = new Handler(Looper.getMainLooper());
        statusCheckRunnable = new Runnable() {
            @Override
            public void run() {
                // Solo actuar si el botón está en modo "Descargando"
                if (ingresar != null && "DESCARGANDO...".equals(ingresar.getText().toString())) {
                    if (isApkAlreadyDownloaded()) {
                        // Si el APK ya existe, la descarga terminó. Actualizamos la UI.
                        ingresar.setText("INSTALAR");
                        ingresar.setEnabled(true);
                        // No volvemos a programar el verificador, el trabajo está hecho.
                    } else {
                        // Si no, volvemos a comprobar en 2 segundos.
                        statusCheckHandler.postDelayed(this, 2000);
                    }
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Si volvemos a la app y estaba descargando, reactivamos el verificador inmediatamente.
        if (ingresar != null && "DESCARGANDO...".equals(ingresar.getText().toString())) {
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
            return "N/A"; // En caso de no poder obtener la versión, mostrar un valor predeterminado
        }
    }

    @SuppressLint("SetTextI18n")
    public void lanzarloginnew (View view){
        if (internetDisponible()) {
            String buttonText = ingresar.getText().toString();
            if (buttonText.equals("ACTUALIZAR")) {
                iniciaractualizacion();
            } else if (buttonText.equals("INSTALAR")) {
                installDownloadedApk();
            } else { // INGRESAR
                iniciarapp();
            }
        } else {
            resultado.setText("Sin conexión");
        }
    }

    @SuppressLint("SetTextI18n")
    private void iniciaractualizacion() {
        File apkFile = getApkFile();
        if (apkFile != null && apkFile.exists()) {
            apkFile.delete();
        }

        String apkUrl = "https://github.com/arturocalcagno/vendedores.fecoapp/releases/download/V2.0/FecoApp3.apk";

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(apkUrl));
        request.setTitle("Actualización de FecoApp");
        request.setDescription("Descargando la nueva versión...");
        request.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, APK_FILE_NAME);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.allowScanningByMediaScanner();

        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        downloadManager.enqueue(request);

        if (ingresar != null) {
            ingresar.setEnabled(false);
            ingresar.setText("DESCARGANDO...");
        }

        // Iniciamos el verificador periódico
        statusCheckHandler.post(statusCheckRunnable);

        Toast.makeText(this, "Descargando", Toast.LENGTH_LONG).show();
    }

    @SuppressLint("SetTextI18n")
    private void iniciarapp() {
        final Intent[] i = new Intent[1];
        final ProgressDialog ringProgressDialog = ProgressDialog.show(FecoApp.this, "Por favor espere", "Validando datos", true);
        ringProgressDialog.setCancelable(false);
        new Thread(() -> {
                internet = internetDisponible();
                if (internet) {
                    WebService ws = new WebService();
                    versionmatch = ws.validarversion(versioninstalada);
                }
            runOnUiThread(() -> {
                if (!internet){
                    resultado.setText("Sin conexión. No se puede ingresar");
                    fin = true;
                } else {
                    fin = false;
                }
            });
            ringProgressDialog.dismiss();

            runOnUiThread(() -> {
                if (!fin){
                    if (!versioninstalada.equals(versionmatch)) {
                        resultado.setText("Nueva Versión Disponible");
                        // Comprueba si la actualización ya está descargada para mostrar el botón correcto
                        if (isApkAlreadyDownloaded()) {
                            ingresar.setText("INSTALAR");
                        } else {
                            ingresar.setText("ACTUALIZAR");
                        }
                        if (ingresar != null) {
                            ingresar.setEnabled(true);
                        }
                    } else {
                        i[0] = new Intent(FecoApp.this, Principal.class);
                        startActivity(i[0]);
                        finish();
                    }
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
            Toast.makeText(this, "Archivo de actualización no encontrado. Reiniciando estado...", Toast.LENGTH_LONG).show();
            ingresar.setText("ACTUALIZAR");
            return;
        }

        Uri apkUri = FileProvider.getUriForFile(this, "com.arturocalcagno.fecoapp.FecoApp", apkFile);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No se encontró un instalador de paquetes válido.", Toast.LENGTH_SHORT).show();
        }
    }

    public boolean internetDisponible() {
        ConnectivityManager cManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo nInfo = cManager.getActiveNetworkInfo();
        return nInfo != null && nInfo.isConnected();
    }
}
