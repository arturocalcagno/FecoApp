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
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar; // Importar ProgressBar
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;

public class FecoApp extends Activity {

    private TextView resultado;
    public String versioninstalada;
    public String versionmatch;
    private Button ingresar;
    public Boolean internet;
    private Boolean fin;
    private static final String APK_FILE_NAME = "FecoApp.apk";

    private Handler statusCheckHandler;
    private Runnable statusCheckRunnable;
    private ProgressBar progressBar; // <-- NUEVO: Añadir ProgressBar

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        resultado = findViewById(R.id.txtResultadoPermisos);
        DB db = new DB(this);
        ingresar = findViewById(R.id.cmd_ingresar);
        progressBar = findViewById(R.id.progressBar); // <-- NUEVO: Inicializar el ProgressBar

        //Mostramos la versión instalada
        TextView version = findViewById(R.id.txtVersion);
        versioninstalada = getVersionName();
        version.setText(getString(R.string.version_format, versioninstalada));

        // Inicializamos el verificador de estado
        statusCheckHandler = new Handler(Looper.getMainLooper());
        statusCheckRunnable = new Runnable() {
            @Override
            public void run() {
                // Solo actuar si el botón está en modo "Descargando"
                if (ingresar != null && getString(R.string.button_downloading).equals(ingresar.getText().toString())) {
                    if (isApkAlreadyDownloaded()) {
                        // Si el APK ya existe, la descarga terminó. Actualizamos la UI.
                        ingresar.setText(R.string.button_install);
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
            return "N/A"; // En caso de no poder obtener la versión, mostrar un valor predeterminado
        }
    }

    public void lanzarloginnew(View view) {
        if (internetDisponible()) {
            String buttonText = ingresar.getText().toString();
            if (buttonText.equals(getString(R.string.button_update))) {
                iniciaractualizacion();
            } else if (buttonText.equals(getString(R.string.button_install))) {
                installDownloadedApk();
            } else { // INGRESAR
                iniciarapp();
            }
        } else {
            resultado.setText(R.string.status_no_connection);
        }
    }

    private void iniciaractualizacion() {
        File apkFile = getApkFile();
        if (apkFile != null && apkFile.exists()) {
            apkFile.delete();
        }

        String apkUrl = "https://github.com/arturocalcagno/vendedores.fecoapp/releases/download/V2.0/FecoApp.apk";

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(apkUrl));
        request.setTitle(getString(R.string.update_title));
        request.setDescription(getString(R.string.update_downloading_notification));
        request.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, APK_FILE_NAME);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        // request.allowScanningByMediaScanner(); // <-- CORREGIDO: Método obsoleto eliminado.

        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        downloadManager.enqueue(request);

        if (ingresar != null) {
            ingresar.setEnabled(false);
            ingresar.setText(R.string.button_downloading);
        }

        // Iniciamos el verificador periódico
        statusCheckHandler.post(statusCheckRunnable);

        Toast.makeText(this, R.string.download_started_notification, Toast.LENGTH_LONG).show();
    }

    private void iniciarapp() {
        final Intent[] i = new Intent[1];

        // --- CORRECCIÓN: Reemplazo de ProgressDialog por ProgressBar ---
        progressBar.setVisibility(View.VISIBLE); // Mostrar ProgressBar
        ingresar.setEnabled(false); // Deshabilitar botón durante la carga

        new Thread(() -> {
            internet = internetDisponible();
            if (internet) {
                WebService ws = new WebService();
                versionmatch = ws.validarversion(versioninstalada);
            }

            // Volver al hilo principal para actualizar la UI
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE); // Ocultar ProgressBar

                if (!internet) {
                    resultado.setText(R.string.status_no_connection_cant_login);
                    fin = true;
                } else {
                    fin = false;
                }

                if (!fin) {
                    if (!versioninstalada.equals(versionmatch)) {
                        resultado.setText(R.string.status_new_version_available);
                        if (isApkAlreadyDownloaded()) {
                            ingresar.setText(R.string.button_install);
                        } else {
                            ingresar.setText(R.string.button_update);
                        }
                    } else {
                        i[0] = new Intent(FecoApp.this, Principal.class);
                        startActivity(i[0]);
                        finish();
                        return; // Salir para no re-habilitar el botón innecesariamente
                    }
                }
                // Si no se inició la nueva actividad, re-habilitar el botón
                ingresar.setEnabled(true);
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

        // --- CORRECCIÓN: Usar el authority definido en el Manifest ---
        // Asegúrate que "com.arturocalcagno.fecoapp.fileprovider" es el authority correcto.
        Uri apkUri = FileProvider.getUriForFile(this, "com.arturocalcagno.fecoapp.fileprovider", apkFile);

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

    // --- CORRECCIÓN: Método modernizado para verificar la conexión a internet ---
    public boolean internetDisponible() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return false;
        }

        Network network = connectivityManager.getActiveNetwork();
        if (network == null) {
            return false; // No hay red activa
        }

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        return capabilities != null && (
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    }
}

