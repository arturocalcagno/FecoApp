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
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.android.gms.tasks.Task;
import com.google.zxing.integration.android.IntentIntegrator;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Remitos extends AppCompatActivity {

    // --- Constantes ---
    private static final int REQUEST_CODE_LOCATION_PERMISSION = 1;
    private static final int REQUEST_CHECK_SETTINGS = 100;
    private static final int FOTO_NUMERO_1 = 1;
    private static final int FOTO_NUMERO_2 = 2;

    // --- Vistas ---
    private Button nuevoIngresoButton, tomarFoto1Button, tomarFoto2Button, agregarRemitoButton, transferirPendientesButton;
    private TextView resultadoEnvio, resultadoRemito;
    private ProgressBar progressBar, progressBarSinc;

    // --- Datos del Remito ---
    private TextView nrocarga;
    private TextView bocaremito;
    private TextView remito;
    private TextView razonsocial;
    private String registro;
    private String bytesFoto1 = "", bytesFoto2 = "";
    private String imageUrl;

    // --- Base de Datos ---
    private DB dbHelper;

    // --- Ubicación ---
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private String latitudeGPS, longitudeGPS;

    // --- Activity Launchers ---
    private ActivityResultLauncher<Intent> activityLauncher;
    private int fotoTomadaNumero;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.remitos);

        dbHelper = new DB(this);

        inicializarVistas();
        setupActivityLauncher();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        crearLocationCallback();

        configurarEstadoInicial();
    }

    private void inicializarVistas() {
        nuevoIngresoButton = findViewById(R.id.cmdNuevoIngreso);
        nrocarga = findViewById(R.id.txtNroCarga);
        bocaremito = findViewById(R.id.txtBocaRemito);
        remito = findViewById(R.id.txtRemito);
        razonsocial = findViewById(R.id.txtRazonSocial);
        tomarFoto1Button = findViewById(R.id.cmdTomarFoto1);
        tomarFoto2Button = findViewById(R.id.cmdTomarFoto2);
        agregarRemitoButton = findViewById(R.id.cmdAgregarRemito);
        transferirPendientesButton = findViewById(R.id.cmdTransferirPendientes);
        resultadoEnvio = findViewById(R.id.txtResultadoEnvio);
        resultadoRemito = findViewById(R.id.txtResultadoRemito);
        progressBar = findViewById(R.id.remitos_progressBar);
        progressBarSinc = findViewById(R.id.remitos_progressBar_sinc);
    }

    private void setupActivityLauncher() {
        activityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (fotoTomadaNumero > 0) {
                        procesarResultadoFoto(result);
                    } else {
                        procesarResultadoQR(result);
                    }
                }
        );
    }

    private void procesarResultadoQR(androidx.activity.result.ActivityResult result) {
        com.google.zxing.integration.android.IntentResult qrResult = parseActivityResult(result.getResultCode(), result.getData());
        if (qrResult.getContents() != null) {
            String[] parts = qrResult.getContents().split("-");
            if (parts.length >= 4) {
                try {
                    nrocarga.setText(parts[0]);
                    int bocaint = Integer.parseInt(parts[1]);
                    bocaremito.setText(String.valueOf(bocaint));
                    int remitoint = Integer.parseInt(parts[2]);
                    remito.setText(String.valueOf(remitoint));
                    razonsocial.setText(parts[3]);
                    registro = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

                    resultadoRemito.setText("");
                    habilitarBoton(tomarFoto1Button, true);
                } catch (NumberFormatException e) {
                    Log.e("QR_FORMAT_ERROR", "El QR no contiene números válidos en las partes esperadas.", e);
                    resultadoRemito.setText(R.string.remitos_qr_error_formato);
                }
            } else {
                resultadoRemito.setText(R.string.remitos_qr_error_formato);
            }
        }
    }

    private void procesarResultadoFoto(androidx.activity.result.ActivityResult result) {
        if (result.getResultCode() == Activity.RESULT_OK) {
            if (fotoTomadaNumero == FOTO_NUMERO_1 || fotoTomadaNumero == FOTO_NUMERO_2) {
                try {
                    Bitmap bm = rotateBitmapOrientation(imageUrl);
                    bm = escalar(bm);
                    String base64Image = bitmapToBase64(bm);
                    bm.recycle();

                    if (fotoTomadaNumero == FOTO_NUMERO_1) {
                        bytesFoto1 = base64Image;
                        marcarBotonComoCompleto(tomarFoto1Button);
                        habilitarBoton(tomarFoto2Button, true);
                        habilitarBoton(agregarRemitoButton, true);
                        resultadoRemito.setText(R.string.remitos_foto1_tomada);
                    } else if (fotoTomadaNumero == FOTO_NUMERO_2) {
                        bytesFoto2 = base64Image;
                        marcarBotonComoCompleto(tomarFoto2Button);
                        resultadoRemito.setText(R.string.remitos_foto2_tomada);
                    }
                } catch (IOException e) {
                    resultadoRemito.setText(R.string.remitos_error_procesar_foto);
                    Log.e("FOTO_ERROR", "Error I/O procesando la foto", e);
                } finally {
                    clearCache();
                    fotoTomadaNumero = 0; // Resetear
                }
            }
        }
    }

    // --- MÉTODOS DE ACCIÓN DE BOTONES (llamados desde XML) ---

    public void lanzarPrincipal(View view) {
        finish(); // Cierra esta actividad y vuelve a la anterior
    }

    public void nuevoIngreso(View view) {
        // MEJORA: Actualizar el contador de pendientes al iniciar un nuevo ingreso.
        actualizarContadorRemitosPendientes();

        // 1. Limpiar campos de texto de la UI
        nrocarga.setText("");
        bocaremito.setText("");
        remito.setText("");
        razonsocial.setText("");
        resultadoRemito.setText("");

        // 2. Limpiar variables de datos de fotos para evitar envíos incorrectos
        bytesFoto1 = "";
        bytesFoto2 = "";

        // 3. Deshabilitar y resetear visualmente los botones de los siguientes pasos
        habilitarBoton(tomarFoto1Button, false);
        restaurarColorOriginalBoton(tomarFoto1Button);
        habilitarBoton(tomarFoto2Button, false);
        restaurarColorOriginalBoton(tomarFoto2Button);
        habilitarBoton(agregarRemitoButton, false);

        // 4. Iniciar el escáner QR
        IntentIntegrator integrador = new IntentIntegrator(this);
        integrador.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrador.setPrompt(getString(R.string.remitos_lector_qr_prompt));
        integrador.setBeepEnabled(true);
        activityLauncher.launch(integrador.createScanIntent());
    }

    public void tomarFoto(View view) {
        int id = view.getId();
        if (id == R.id.cmdTomarFoto1) {
            fotoTomadaNumero = FOTO_NUMERO_1;
        } else if (id == R.id.cmdTomarFoto2) {
            fotoTomadaNumero = FOTO_NUMERO_2;
        } else {
            return;
        }

        Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File imagenArchivo;
        try {
            imagenArchivo = crearArchivoImagen();
        } catch (IOException e) {
            Log.e("CAPTURA_FOTO", "Error creando archivo de imagen", e);
            return; // No continuar si no se puede crear el archivo
        }

        Uri fotoUri = FileProvider.getUriForFile(this, "com.arturocalcagno.fecoapp.FecoApp", imagenArchivo);
        i.putExtra(MediaStore.EXTRA_OUTPUT, fotoUri);
        activityLauncher.launch(i);
    }

    public void agregarTransaccion(View view) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_LOCATION_PERMISSION);
        } else {
            verificarGPSyObtenerUbicacion();
        }
    }

    public void transferirRemitosPendientes(View view){
        mostrarProgresoSinc(true);
        resultadoEnvio.setText(R.string.remitos_sincronizando);
        iniciarSincronizacionEnSegundoPlano(true);
    }

    // --- LÓGICA DE ESTADO DE LA UI ---

    private void configurarEstadoInicial() {
        habilitarBoton(nuevoIngresoButton, true);
        habilitarBoton(tomarFoto1Button, false);
        habilitarBoton(tomarFoto2Button, false);
        habilitarBoton(agregarRemitoButton, false);

        resultadoRemito.setText("");
        resultadoEnvio.setText("");

        mostrarProgresoSinc(true);
        resultadoEnvio.setText(R.string.remitos_validando_datos);

        iniciarSincronizacionEnSegundoPlano(false);
    }

    private void iniciarSincronizacionEnSegundoPlano(boolean mostrarErrorConexion) {
        new Thread(() -> {
            if (internetDisponible()) {
                sincronizarRemitosPendientes();
                runOnUiThread(this::actualizarContadorRemitosPendientes);
            } else {
                if (mostrarErrorConexion) {
                    runOnUiThread(() -> {
                        mostrarProgresoSinc(false);
                        resultadoEnvio.setText(R.string.remitos_sin_conexion_sinc);
                    });
                } else {
                    runOnUiThread(this::actualizarContadorRemitosPendientes);
                }
            }
        }).start();
    }

    @SuppressLint("Range")
    private void actualizarContadorRemitosPendientes() {
        mostrarProgresoSinc(false);
        int remitosPendientes = 0;
        try (Cursor c = dbHelper.getTotalRemitosPendientes()) {
            if (c != null && c.moveToFirst()) {
                remitosPendientes = c.getInt(c.getColumnIndex("total"));
            }
        }

        if (remitosPendientes > 0) {
            resultadoEnvio.setText(getString(R.string.remitos_pendientes, remitosPendientes));
            transferirPendientesButton.setVisibility(View.VISIBLE);
        } else {
            resultadoEnvio.setText("");
            transferirPendientesButton.setVisibility(View.INVISIBLE);
        }
    }

    private void habilitarBoton(Button button, boolean habilitar) {
        button.setEnabled(habilitar);
        button.getBackground().setAlpha(habilitar ? 255 : 64);
    }

    private void marcarBotonComoCompleto(Button button) {
        button.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
    }

    private void restaurarColorOriginalBoton(Button button) {
        button.setBackgroundColor(0xFFFEAA0C); // Color original de los botones
    }

    private void mostrarProgreso(boolean mostrar) {
        progressBar.setVisibility(mostrar ? View.VISIBLE : View.GONE);
        habilitarBoton(agregarRemitoButton, !mostrar);
        habilitarBoton(nuevoIngresoButton, !mostrar);
        habilitarBoton(transferirPendientesButton, !mostrar);
        habilitarBoton(tomarFoto1Button, false);
        habilitarBoton(tomarFoto2Button, false);
    }

    private void mostrarProgresoSinc(boolean mostrar) {
        progressBarSinc.setVisibility(mostrar ? View.VISIBLE : View.GONE);
        habilitarBoton(transferirPendientesButton, !mostrar);
    }

    // --- LÓGICA DE UBICACIÓN Y ENVÍO ---

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                verificarGPSyObtenerUbicacion();
            } else {
                Toast.makeText(this, R.string.remitos_permiso_ubicacion_necesario, Toast.LENGTH_LONG).show();
            }
        }
    }

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
                resultadoRemito.setText(R.string.remitos_activar_gps);
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
                        mostrarProgreso(false);
                        resultadoRemito.setText(R.string.remitos_error_obtener_ubicacion);
                    });
                }
            }
        };
    }

    @SuppressLint({"MissingPermission"})
    private void obtenerUbicacion(LocationRequest locationRequest) {
        mostrarProgreso(true);
        resultadoRemito.setText(R.string.remitos_obteniendo_ubicacion);

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null && (System.currentTimeMillis() - location.getTime()) < 60000) {
                latitudeGPS = String.valueOf(location.getLatitude());
                longitudeGPS = String.valueOf(location.getLongitude());
                procesarEnvioDatosConUbicacion();
            } else {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
            }
        }).addOnFailureListener(this, e -> {
            mostrarProgreso(false);
            resultadoRemito.setText(R.string.remitos_error_al_obtener_ubicacion);
        });
    }

    private void procesarEnvioDatosConUbicacion() {
        if (internetDisponible()) {
            resultadoEnvio.setText(R.string.remitos_enviando);
            new Thread(() -> {
                WebService ws = new WebService();
                final String res = ws.registrarremito(nrocarga.getText().toString(), bocaremito.getText().toString(), remito.getText().toString(), razonsocial.getText().toString(), registro, bytesFoto1, latitudeGPS, longitudeGPS, bytesFoto2);
                runOnUiThread(() -> {
                    mostrarProgreso(false);
                    if ("true".equals(res)) {
                        resultadoEnvio.setText(R.string.remitos_enviado_exito);
                    } else {
                        resultadoEnvio.setText(R.string.remitos_error_envio);
                        dbHelper.agregarRemito(nrocarga.getText().toString(), bocaremito.getText().toString(), remito.getText().toString(), razonsocial.getText().toString(), registro, bytesFoto1, latitudeGPS, longitudeGPS, bytesFoto2);
                    }
                    // Congelar la UI después del envío, mostrando el resultado.
                    habilitarBoton(tomarFoto1Button, false);
                    habilitarBoton(tomarFoto2Button, false);
                    habilitarBoton(agregarRemitoButton, false);
                    resultadoRemito.setText("");
                });
            }).start();
        } else {
            mostrarProgreso(false);
            resultadoEnvio.setText(R.string.remitos_guardado_sin_conexion);
            dbHelper.agregarRemito(nrocarga.getText().toString(), bocaremito.getText().toString(), remito.getText().toString(), razonsocial.getText().toString(), registro, bytesFoto1, latitudeGPS, longitudeGPS, bytesFoto2);
            // Congelar la UI después del envío, mostrando el resultado.
            habilitarBoton(tomarFoto1Button, false);
            habilitarBoton(tomarFoto2Button, false);
            habilitarBoton(agregarRemitoButton, false);
            resultadoRemito.setText("");
        }
    }


    // --- SINCRONIZACIÓN Y AYUDANTES ---

    @SuppressLint("Range")
    private void sincronizarRemitosPendientes() {
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
            Log.e("SINC_ERROR", "Error sincronizando remitos pendientes", e);
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


    private File crearArchivoImagen() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String nombreArchivo = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        // MEJORA: Añadir una comprobación para evitar un NullPointerException si el almacenamiento no está disponible.
        if (storageDir == null) {
            throw new IOException("No se puede acceder al directorio de imágenes externo.");
        }
        File image = File.createTempFile(nombreArchivo, ".jpg", storageDir);
        imageUrl = image.getAbsolutePath();
        return image;
    }

    private Bitmap escalar(Bitmap bitmap) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        int newW = 800; // Ancho fijo para la escala
        int newH = (h * newW) / w;
        return Bitmap.createScaledBitmap(bitmap, newW, newH, false);
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private int getRotationAngle(String photoFilePath) throws IOException {
        ExifInterface exif = new ExifInterface(photoFilePath);
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        return switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90 -> 90;
            case ExifInterface.ORIENTATION_ROTATE_180 -> 180;
            case ExifInterface.ORIENTATION_ROTATE_270 -> 270;
            default -> 0;
        };
    }

    public Bitmap rotateBitmapOrientation(String photoFilePath) throws IOException {
        Bitmap sourceBitmap = BitmapFactory.decodeFile(photoFilePath, new BitmapFactory.Options());
        if (sourceBitmap == null) {
            throw new IOException("No se pudo decodificar el archivo de imagen: " + photoFilePath);
        }

        int rotationAngle = getRotationAngle(photoFilePath);

        if (rotationAngle == 0) {
            return sourceBitmap;
        }

        Matrix matrix = new Matrix();
        matrix.setRotate(rotationAngle, (float) sourceBitmap.getWidth() / 2, (float) sourceBitmap.getHeight() / 2);

        try {
            Bitmap rotatedBitmap = Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.getWidth(), sourceBitmap.getHeight(), matrix, true);
            sourceBitmap.recycle();
            return rotatedBitmap;
        } catch (OutOfMemoryError e) {
            Log.e("ROTATE_BITMAP", "Error de memoria al rotar el bitmap", e);
            return sourceBitmap;
        }
    }

    private void clearCache() {
        try {
            File dir = getBaseContext().getCacheDir();
            if (dir != null && dir.isDirectory()) {
                File[] children = dir.listFiles();
                if (children != null) { // Evita NullPointerException
                    for (File child : children) {
                        if (!child.delete()) {
                            Log.w("CLEAR_CACHE", "No se pudo eliminar el archivo de caché: " + child.getName());
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e("CLEAR_CACHE", "Error limpiando el caché", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}
