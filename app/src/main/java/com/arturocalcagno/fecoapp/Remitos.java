package com.arturocalcagno.fecoapp;

import static com.google.zxing.integration.android.IntentIntegrator.parseActivityResult;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import android.widget.Toast;


import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;
import com.google.zxing.integration.android.IntentIntegrator;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Remitos extends AppCompatActivity {

    private String carga, boca, remito, razonsocial, registro, imageurl;
    private Button nuevoingreso, tomarfoto1, tomarfoto2, agregarremito, transferirpendientes;
    private TextView txtResultadoEnvio, txtResultadoRemito;
    private String bytesfoto1 = "", bytesfoto2 = "";
    private int remitospendientes;

    private DB dbHelper;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private String latitudeGPS, longitudeGPS;
    private ProgressBar progressBar, progressBarSinc;

    private static final int REQUEST_CODE_LOCATION_PERMISSION = 1;
    private static final int REQUEST_CHECK_SETTINGS = 100;

    private ActivityResultLauncher<Intent> activityLauncher;
    private Uri fotoUri;
    private int fotoTomada;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.remitos);

        dbHelper = new DB(this);

        nuevoingreso = findViewById(R.id.cmdNuevoIngreso);
        tomarfoto1 = findViewById(R.id.cmdTomarFoto1);
        tomarfoto2 = findViewById(R.id.cmdTomarFoto2);
        agregarremito = findViewById(R.id.cmdAgregarRemito);
        transferirpendientes = findViewById(R.id.cmdTransferirPendientes);
        txtResultadoEnvio = findViewById(R.id.txtResultadoEnvio);
        txtResultadoRemito = findViewById(R.id.txtResultadoRemito);

        progressBar = findViewById(R.id.remitos_progressBar);
        progressBarSinc = findViewById(R.id.remitos_progressBar_sinc);

        setupLauncher();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        crearLocationCallback();

        pantallainicial();
    }

    private void setupLauncher() {
        activityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // Resultado del Escáner QR
                    com.google.zxing.integration.android.IntentResult qrResult = parseActivityResult(result.getResultCode(), result.getData());
                    if (qrResult != null) {
                        if (qrResult.getContents() != null) {
                            String[] parts = qrResult.getContents().split(";");
                            if (parts.length >= 4) {
                                carga = parts[0];
                                boca = parts[1];
                                remito = parts[2];
                                razonsocial = parts[3];
                                registro = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                                txtResultadoRemito.setText("Boca: " + boca + " Remito: " + remito);
                                tomarfoto1.setEnabled(true);
                                tomarfoto1.getBackground().setAlpha(255);
                            } else {
                                txtResultadoEnvio.setText("Error: Código QR con formato incorrecto.");
                            }
                        }
                    }
                    // Resultado de la Cámara
                    else if (result.getResultCode() == Activity.RESULT_OK) {
                        if (fotoTomada == 1 || fotoTomada == 2) {
                            try {
                                Bitmap bm = rotateBitmapOrientation(imageurl);
                                bm = escalar(bm);
                                String base64Image = bitmapToBase64(bm);
                                if (bm != null) bm.recycle();

                                if (fotoTomada == 1) {
                                    bytesfoto1 = base64Image;
                                    tomarfoto1.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
                                    tomarfoto2.setEnabled(true);
                                    tomarfoto2.getBackground().setAlpha(255);
                                    txtResultadoEnvio.setText("Foto 1 tomada");
                                } else if (fotoTomada == 2) {
                                    bytesfoto2 = base64Image;
                                    tomarfoto2.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
                                    agregarremito.setEnabled(true);
                                    agregarremito.getBackground().setAlpha(255);
                                    txtResultadoEnvio.setText("Foto 2 tomada");
                                }
                            } catch (IOException e) {
                                txtResultadoEnvio.setText("Error al procesar la foto");
                                Log.e("FOTO_ERROR", "Error I/O", e);
                            } finally {
                                clearCache();
                            }
                        }
                    }
                }
        );
    }

    public void lanzarprincipal(View view) {
        Intent i = new Intent(this, Principal.class);
        startActivity(i);
        finish();
    }

    public void pantallainicial() {
        nuevoingreso.setEnabled(true);
        nuevoingreso.getBackground().setAlpha(255);
        tomarfoto1.setEnabled(false);
        tomarfoto1.getBackground().setAlpha(64);
        tomarfoto2.setEnabled(false);
        tomarfoto2.getBackground().setAlpha(64);
        agregarremito.setEnabled(false);
        agregarremito.getBackground().setAlpha(64);
        txtResultadoEnvio.setText("");
        txtResultadoRemito.setText("");

        progressBarSinc.setVisibility(View.VISIBLE);
        txtResultadoEnvio.setText("Validando datos...");

        new Thread(() -> {
            if (internetDisponible()) {
                sincronizarremitospendientes();
            }
            runOnUiThread(this::actualizarremitospendientes);
        }).start();
    }

    @SuppressLint({"Range", "SetTextI18n"})
    private void actualizarobjetospantalla() {
        tomarfoto1.setEnabled(false);
        tomarfoto1.setBackgroundColor(0xFFFEAA0C);
        tomarfoto1.getBackground().setAlpha(64);
        tomarfoto2.setEnabled(false);
        tomarfoto2.setBackgroundColor(0xFFFEAA0C);
        tomarfoto2.getBackground().setAlpha(64);
        agregarremito.setEnabled(false);
        agregarremito.getBackground().setAlpha(64);
        bytesfoto1 = "";
        bytesfoto2 = "";
        txtResultadoRemito.setText("");

        actualizarremitospendientes();
    }

    @SuppressLint({"Range", "SetTextI18n"})
    private void actualizarremitospendientes() {
        progressBarSinc.setVisibility(View.GONE);
        remitospendientes = 0;
        try (Cursor c = dbHelper.getTotalRemitosPendientes()) {
            if (c != null && c.moveToFirst()) {
                remitospendientes = c.getInt(c.getColumnIndex("total"));
            }
        }

        if (remitospendientes > 0) {
            txtResultadoEnvio.setText("Remitos Pendientes: " + remitospendientes);
            transferirpendientes.setVisibility(View.VISIBLE);
        } else {
            txtResultadoEnvio.setText("No hay remitos pendientes.");
            transferirpendientes.setVisibility(View.INVISIBLE);
        }
    }

    // --- MÉTODOS DE ACCIÓN DE BOTONES ---

    public void nuevoIngreso(View view) {
        IntentIntegrator integrador = new IntentIntegrator(this);
        integrador.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrador.setPrompt("Lector - QR");
        integrador.setBeepEnabled(true);
        integrador.setTorchEnabled(true);
        activityLauncher.launch(integrador.createScanIntent());
    }

    public void tomarFoto(View view) {
        int id = view.getId();
        if (id == R.id.cmdTomarFoto1) {
            fotoTomada = 1;
        } else if (id == R.id.cmdTomarFoto2) {
            fotoTomada = 2;
        } else {
            return;
        }

        Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (i.resolveActivity(getPackageManager()) != null) {
            File imagenArchivo = null;
            try {
                imagenArchivo = creararchivo();
            } catch (IOException e) {
                Log.e("CAPTURA_FOTO", "Error creando archivo", e);
            }
            if (imagenArchivo != null) {
                fotoUri = FileProvider.getUriForFile(this, "com.arturocalcagno.fecoapp.fileprovider", imagenArchivo);
                i.putExtra(MediaStore.EXTRA_OUTPUT, fotoUri);
                activityLauncher.launch(i);
            }
        }
    }

    public void agregartransaccion(View view) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_LOCATION_PERMISSION);
        } else {
            verificarGPSyObtenerUbicacion();
        }
    }

    public void transferirremitospendientes(View view){
        progressBarSinc.setVisibility(View.VISIBLE);
        txtResultadoEnvio.setText("Sincronizando pendientes...");
        new Thread(() -> {
            if (internetDisponible()){
                sincronizarremitospendientes();
            } else {
                runOnUiThread(()-> txtResultadoEnvio.setText("Sin conexión para sincronizar."));
            }
            runOnUiThread(this::actualizarremitospendientes);
        }).start();
    }


    // --- LÓGICA DE UBICACIÓN Y ENVÍO (sin cambios) ---

    @SuppressLint("SetTextI18n")
    private void verificarGPSyObtenerUbicacion() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(5000)
                .build();
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        Task<LocationSettingsResponse> task = LocationServices.getSettingsClient(this).checkLocationSettings(builder.build());

        task.addOnSuccessListener(locationSettingsResponse -> obtenerUbicacion(locationRequest));
        task.addOnFailureListener(e -> {
            if (e instanceof ResolvableApiException) {
                try {
                    ((ResolvableApiException) e).startResolutionForResult(this, REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException ignored) {}
            } else {
                txtResultadoEnvio.setText("¡Activa el GPS para continuar!");
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void crearLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                fusedLocationClient.removeLocationUpdates(this);
                if (!locationResult.getLocations().isEmpty()) {
                    android.location.Location location = locationResult.getLocations().get(0);
                    latitudeGPS = String.valueOf(location.getLatitude());
                    longitudeGPS = String.valueOf(location.getLongitude());
                    procesarEnvioDatosConUbicacion();
                } else {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        agregarremito.setEnabled(true);
                        txtResultadoEnvio.setText("No se pudo obtener ubicación. Intente de nuevo.");
                    });
                }
            }
        };
    }

    @SuppressLint({"MissingPermission", "SetTextI18n"})
    private void obtenerUbicacion(LocationRequest locationRequest) {
        agregarremito.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        txtResultadoEnvio.setText("Obteniendo ubicación...");

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null && (System.currentTimeMillis() - location.getTime()) < 60000) {
                latitudeGPS = String.valueOf(location.getLatitude());
                longitudeGPS = String.valueOf(location.getLongitude());
                procesarEnvioDatosConUbicacion();
            } else {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
            }
        }).addOnFailureListener(this, e -> {
            progressBar.setVisibility(View.GONE);
            agregarremito.setEnabled(true);
            txtResultadoEnvio.setText("Error al obtener la ubicación.");
        });
    }

    @SuppressLint("SetTextI18n")
    private void procesarEnvioDatosConUbicacion() {
        if (internetDisponible()) {
            txtResultadoEnvio.setText("Enviando Remito...");
            new Thread(() -> {
                WebService ws = new WebService();
                final String res = ws.registrarremito(carga, boca, remito, razonsocial, registro, bytesfoto1, latitudeGPS, longitudeGPS, bytesfoto2);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    if ("true".equals(res)) {
                        txtResultadoEnvio.setText("Remito Enviado");
                        actualizarobjetospantalla();
                    } else {
                        txtResultadoEnvio.setText("Error al enviar. Se guardó para enviar luego.");
                        dbHelper.agregarRemito(carga, boca, remito, razonsocial, registro, bytesfoto1, latitudeGPS, longitudeGPS, bytesfoto2);
                        actualizarobjetospantalla();
                    }
                });
            }).start();
        } else {
            progressBar.setVisibility(View.GONE);
            txtResultadoEnvio.setText("Sin conexión. Se guardó para enviar luego.");
            dbHelper.agregarRemito(carga, boca, remito, razonsocial, registro, bytesfoto1, latitudeGPS, longitudeGPS, bytesfoto2);
            actualizarobjetospantalla();
        }
    }


    // --- SINCRONIZACIÓN Y AYUDANTES (sin cambios) ---

    @SuppressLint("Range")
    private void sincronizarremitospendientes() {
        try (Cursor c = dbHelper.getRemitosPendientes()) {
            if (c != null) {
                while (c.moveToNext()) {
                    WebService ws = new WebService();
                    String res = ws.registrarremito(
                            c.getString(c.getColumnIndex("carga")),
                            c.getString(c.getColumnIndex("boca")),
                            c.getString(c.getColumnIndex("remito")),
                            c.getString(c.getColumnIndex("razonsocial")),
                            c.getString(c.getColumnIndex("registro")),
                            c.getString(c.getColumnIndex("foto")),
                            c.getString(c.getColumnIndex("latitud")),
                            c.getString(c.getColumnIndex("longitud")),
                            c.getString(c.getColumnIndex("foto2"))
                    );
                    if ("true".equals(res)) {
                        dbHelper.eliminarRemito(c.getString(c.getColumnIndex("boca")), c.getString(c.getColumnIndex("remito")));
                    }
                }
            }
        } catch (Exception e) {
            Log.e("SINC_ERROR", "Error sincronizando remitos", e);
        }
    }

    public boolean internetDisponible() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        Network network = cm.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
        return capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    }


    private File creararchivo() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String nombreArchivo = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(nombreArchivo, ".jpg", storageDir);
        imageurl = image.getAbsolutePath();
        return image;
    }

    private Bitmap escalar(Bitmap bitmap) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        int newW = 800;
        int newH = (h * newW) / w;
        return Bitmap.createScaledBitmap(bitmap, newW, newH, false);
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    public Bitmap rotateBitmapOrientation(String photoFilePath) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(photoFilePath, bounds);
        BitmapFactory.Options opts = new BitmapFactory.Options();
        Bitmap bm = BitmapFactory.decodeFile(photoFilePath, opts);
        ExifInterface exif = new ExifInterface(photoFilePath);
        String orientString = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
        int orientation = orientString != null ? Integer.parseInt(orientString) : ExifInterface.ORIENTATION_NORMAL;
        int rotationAngle = 0;
        if (orientation == ExifInterface.ORIENTATION_ROTATE_90) rotationAngle = 90;
        if (orientation == ExifInterface.ORIENTATION_ROTATE_180) rotationAngle = 180;
        // --- CORRECCIÓN FINAL ---
        if (orientation == ExifInterface.ORIENTATION_ROTATE_270) rotationAngle = 270;
        Matrix matrix = new Matrix();
        matrix.setRotate(rotationAngle, (float) bm.getWidth() / 2, (float) bm.getHeight() / 2);
        return Bitmap.createBitmap(bm, 0, 0, bounds.outWidth, bounds.outHeight, matrix, true);
    }

    private void clearCache() {
        try {
            File dir = getBaseContext().getCacheDir();
            if (dir != null && dir.isDirectory()) {
                for (File child : dir.listFiles()) {
                    child.delete();
                }
            }
        } catch (Exception e) {
            Log.e("CLEAR_CACHE", "Error limpiando caché", e);
        }
    }
}



