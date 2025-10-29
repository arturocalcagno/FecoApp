package com.arturocalcagno.fecoapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.CaptureActivity;
import java.util.Calendar;

public class Remitos extends AppCompatActivity {
    //Pantalla
    private boolean online;
    private TextView nrocarga;
    private TextView bocaremito;
    private TextView remito;
    private Integer remitoint;
    private TextView razonsocial;
    private Button agregarremito;
    private TextView resultadoenvio;
    private TextView resultado;
    private Button transferirpendientes;
    private Calendar registrorto;
    private String cargaenviar;
    private Integer bocaint;
    private String bocaenviar;
    private String remitoenviar;
    private String razonsocialenviar;
    private String fotoremitoenviar;
    private String fotoremitoenviar2;
    private String registroenviar;
    private String res;
    private int remitospendientes;
    //Tomar foto
    String imageurl;
    private static final int REQUEST_LOCATION_PERMISSION = 1001;  // Código único para solicitud de permiso de ubicación
    private static final int REQUEST_CAMERA_PERMISSION = 1002;    // Ya lo tienes, pero lo incluyo para referencia
    private static final int REQUEST_CODE_ALL_PERMISSIONS = 1003; // Código único para solicitar múltiples permisos
    private static final int REQUEST_CODE_LOCATION_PERMISSION = 1004;
    private static final int REQUEST_CODE_BACKGROUND_LOCATION_PERMISSION = 2001;
    private ActivityResultLauncher<Intent> unifiedLauncher;
    private Map<Integer, Integer> buttonIdToNumberMap;
    private int currentButtonId = -1;
    private Button tomarfoto1;
    private Button tomarfoto2;
    private String bytesfoto1;
    private String bytesfoto2;
    private Uri fotoUri;
    private Bitmap bm1;
    private Bitmap bm2;
    //posicionamiento
    String longitudeGPS;
    String latitudeGPS;
    private FusedLocationProviderClient fusedLocationClient;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.remitos);
        nrocarga = findViewById(R.id.txtNroCarga);
        bocaremito = findViewById(R.id.txtBocaRemito);
        remito = findViewById(R.id.txtRemito);
        razonsocial = findViewById(R.id.txtRazonSocial);
        agregarremito = findViewById(R.id.cmdAgregarRemito);
        resultadoenvio = findViewById(R.id.txtResultadoEnvio);
        resultado = findViewById(R.id.txtResultadoRemito);
        transferirpendientes = findViewById(R.id.cmdTransferirPendientes);
        transferirpendientes.setVisibility(View.INVISIBLE);
        // Inicializar el mapa de botones de fotos
        initializeButtonMap();
        Button nuevoingreso = findViewById(R.id.cmdNuevoIngreso);
        tomarfoto1 = findViewById(R.id.cmdTomarFoto1);
        tomarfoto2 = findViewById(R.id.cmdTomarFoto2);
        imageurl = "";
        bytesfoto1 = "";
        bytesfoto2 = "";
        // Configurar OnClickListener para todos los botones de fotos
        View.OnClickListener onClickListener = this::handleButtonClick;
        nuevoingreso.setOnClickListener(onClickListener);
        tomarfoto1.setOnClickListener(onClickListener);
        tomarfoto2.setOnClickListener(onClickListener);
        // Inicializar el Fused Location Provider
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        // Inicializar ActivityResultLauncher unificado
        unifiedLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // Obtener el número del botón desde SharedPreferences
                    SharedPreferences sharedPref = getSharedPreferences("MyPref", Context.MODE_PRIVATE);
                    int buttonNumber = sharedPref.getInt("button_id", -1);
                    if (result.getResultCode() == RESULT_OK) {
                        switch (buttonNumber) {
                            case 1: // QR
                                IntentResult qrResult = IntentIntegrator.parseActivityResult(result.getResultCode(), result.getData());
                                if (qrResult.getContents() != null) {
                                    String resultadoqr = qrResult.getContents();
                                    //Tenemos que ver si la lectura es correcta = cantidad de caracteres y guiones
                                    String[] parts = resultadoqr.split("-");
                                    if (parts.length != 5) {
                                        resultado.setText("Código inválido");
                                    } else {
                                        nrocarga.setText(parts[0]);
                                        bocaint = Integer.parseInt(parts[1]);
                                        bocaremito.setText(String.valueOf(bocaint));
                                        remitoint = Integer.parseInt(parts[2]);
                                        remito.setText(String.valueOf(remitoint));
                                        razonsocial.setText(parts[3]);
                                        tomarfoto1.setEnabled(true);
                                        tomarfoto1.getBackground().setAlpha(255);
                                        tomarfoto2.setEnabled(false);
                                        tomarfoto2.getBackground().setAlpha(64);
                                        agregarremito.setEnabled(true);
                                        agregarremito.getBackground().setAlpha(255);
                                        resultado.setText("");
                                    }
                                } else {
                                    resultado.setText("Escaneo cancelado");
                                }
                                break;
                            case 2: // Foto 1
                                // Obtener la URI de la imagen desde la variable `fotoUri` que se usó en `tomarFoto()`
                                if (fotoUri != null) {
                                    try {
                                        // Procesar el bitmap
                                        bm1 = rotateBitmapOrientation(imageurl);  // Método para rotar el bitmap
                                        bm1 = escalar(bm1);                  // Método para escalar el bitmap
                                        bytesfoto1 = bitmapToBase64(bm1);
                                        tomarfoto1.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));
                                        tomarfoto2.setEnabled(true);
                                        tomarfoto2.getBackground().setAlpha(255);
                                        resultado.setText("Foto 1 tomada");
                                    } catch (IOException e) {
                                        resultado.setText("Error al procesar Foto 2");
                                    } finally {
                                        clearCache();
                                        if (bm1 != null) {
                                            bm1.recycle();
                                        }
                                    }
                                } else {
                                    resultado.setText("Error con la imagen");
                                }
                                break;
                            case 3: // Foto 2
                                // Obtener la URI de la imagen desde la variable `fotoUri` que se usó en `tomarFoto()`
                                if (fotoUri != null) {
                                    try {
                                        // Procesar el bitmap
                                        bm2 = rotateBitmapOrientation(imageurl);  // Método para rotar el bitmap
                                        bm2 = escalar(bm2);                  // Método para escalar el bitmap
                                        bytesfoto2 = bitmapToBase64(bm2);
                                        tomarfoto2.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));
                                        resultado.setText("Foto 2 tomada");
                                    } catch (IOException e) {
                                        resultado.setText("Error al procesar Foto 2");
                                    } finally {
                                        clearCache();
                                        if (bm2 != null) {
                                            bm2.recycle();
                                        }
                                    }
                                } else {
                                    resultado.setText("Error con la imagen");
                                }
                                break;
                        }
                    } else {
                        // Mostrar un mensaje de error específico para cada botón
                        switch (buttonNumber) {
                            case 1:
                                resultado.setText("Error lectura código QR");
                                break;
                            case 2:
                                resultado.setText("Error al tomar la Foto 1");
                                // esto cambia los colores y deshabilita la foto 2
                                bytesfoto1 = "";
                                tomarfoto1.setBackgroundColor(0xFFFEAA0C);
                                tomarfoto1.getBackground().setAlpha(255);
                                bytesfoto2 = "";
                                tomarfoto2.setBackgroundColor(0xFFFEAA0C);
                                tomarfoto2.getBackground().setAlpha(64);
                                tomarfoto2.setEnabled(false);
                                break;
                            case 3:
                                resultado.setText("Error al tomar la Foto 2");
                                bytesfoto2 = "";
                                tomarfoto2.setBackgroundColor(0xFFFEAA0C);
                                tomarfoto2.getBackground().setAlpha(255);
                                break;
                        }
                    }
                }
        );
        nuevoingreso.setEnabled(true);
        nuevoingreso.getBackground().setAlpha(255);
        tomarfoto1.setEnabled(false);
        tomarfoto1.getBackground().setAlpha(64);
        tomarfoto2.setEnabled(false);
        tomarfoto2.getBackground().setAlpha(64);
        agregarremito.setEnabled(false);
        agregarremito.getBackground().setAlpha(64);
        //Iniciamos la carga de la pantalla
        pantallainicial();
    }

    private void initializeButtonMap() {
        buttonIdToNumberMap = new HashMap<>();
        buttonIdToNumberMap.put(R.id.cmdNuevoIngreso, 1);
        buttonIdToNumberMap.put(R.id.cmdTomarFoto1, 2);
        buttonIdToNumberMap.put(R.id.cmdTomarFoto2, 3);
    }

    public void lanzarprincipal (View view){
        Intent i = new Intent(this, Principal.class) ;
        startActivity(i);
    }

    public void pantallainicial() {
        final ProgressDialog ringProgressDialog = ProgressDialog.show(Remitos.this, "Por favor espere", "Validando datos", true);
        ringProgressDialog.setCancelable(false);
        new Thread(() -> {
            online = internetDisponible();
            if (online) {
                sincronizarremitospendientes();
            }
            //Agregar sincronización si hay pendientes
            runOnUiThread(this::actualizarremitospendientes);
            ringProgressDialog.dismiss();
        }).start();
    }

    @SuppressLint({"Range", "SetTextI18n"})
    private void actualizarobjetospantalla(){
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
        //Vemos si hay Remitos pendientes
        Cursor c =  DB.getTotalRemitosPendientes();
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()){
            remitospendientes = c.getInt(c.getColumnIndex("total"));
        }
        c.close();
        if (remitospendientes > 0) {
            resultado.setText("Remitos Pendientes: " + remitospendientes);
            transferirpendientes.setVisibility(View.VISIBLE);
        } else {
            resultado.setText("");
            transferirpendientes.setVisibility(View.INVISIBLE);
        }
    }

    @SuppressLint({"Range", "SetTextI18n"})
    private void actualizarremitospendientes(){
        Cursor c = DB.getTotalRemitosPendientes();
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()){
            remitospendientes = c.getInt(c.getColumnIndex("total"));
        }
        c.close();
        if (remitospendientes > 0) {
            resultado.setText("Remitos Pendientes: " + remitospendientes);
            transferirpendientes.setVisibility(View.VISIBLE);
        } else {
            resultado.setText("");
            transferirpendientes.setVisibility(View.INVISIBLE);
        }
    }

    public Bitmap rotateBitmapOrientation(String photoFilePath) {
        // Create and configure BitmapFactory
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(photoFilePath, bounds);
        BitmapFactory.Options opts = new BitmapFactory.Options();
        Bitmap bm = BitmapFactory.decodeFile(photoFilePath, opts);
        // Read EXIF Data
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(photoFilePath);
        } catch (Exception e) {
            Log.e("DB_ERROR", "Error al agregar remito: ", e);  // Usar Log para registrar el error
        }
        assert exif != null;
        String orientString = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
        int orientation = orientString != null ? Integer.parseInt(orientString) : ExifInterface.ORIENTATION_NORMAL;
        int rotationAngle = 0;
        if (orientation == ExifInterface.ORIENTATION_ROTATE_90) rotationAngle = 90;
        if (orientation == ExifInterface.ORIENTATION_ROTATE_180) rotationAngle = 180;
        if (orientation == ExifInterface.ORIENTATION_ROTATE_270) rotationAngle = 270;
        // Rotate Bitmap
        Matrix matrix = new Matrix();
        matrix.setRotate(rotationAngle, (float) bm.getWidth(), (float) bm.getHeight());
        return Bitmap.createBitmap(bm, 0, 0, bounds.outWidth, bounds.outHeight, matrix, true);
    }

    private Bitmap escalar(Bitmap foto){
        //Cambiamos el tamaño
        // original measurements
        int origWidth = foto.getWidth();
        int origHeight = foto.getHeight();
//        final int destWidth = 2000;//or the width you need
        final int destWidth = origWidth / 2;
        if(origWidth > destWidth) {
            // picture is wider than we want it, we calculate its target height
            //int destHeight = origHeight / (origWidth / destWidth);
            int destHeight = origHeight / (origWidth / destWidth);
            // we create an scaled bitmap so it reduces the image, not just trim it
            foto = Bitmap.createScaledBitmap(foto, destWidth, destHeight, false);
        }
        return foto;
    }

    private String bitmapToBase64(Bitmap bitmap) throws IOException{
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    @SuppressLint("SetTextI18n")
    public void agregartransaccion(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Verificar si los permisos de ubicación están otorgados
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // Si no están otorgados, solicitarlos
                requestLocationPermission();
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // Para Android 10+ solicitar permisos de segundo plano si es necesario
                requestBackgroundLocationPermission();
            } else {
                // Los permisos ya se han otorgado, proceder a verificar el GPS y obtener la ubicación
                verificarGPSyObtenerUbicacion();
            }
        } else {
            // Para Android 5 a 5.1, proceder directamente (no se requieren permisos en tiempo de ejecución)
            verificarGPSyObtenerUbicacion();
        }
    }

    private void verificarGPSyObtenerUbicacion() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Verificar si el GPS está activado
        LocationRequest locationRequest = LocationRequest.create();
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = settingsClient.checkLocationSettings(builder.build());

        task.addOnSuccessListener(locationSettingsResponse -> {
            // El GPS está activado, obtener la ubicación
            obtenerUbicacion();
        }).addOnFailureListener(e -> {
            if (e instanceof ResolvableApiException) {
                // El GPS está desactivado, mostrar un diálogo para activarlo
                try {
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(this, 100); // Cambiar por registerForActivityResult si es necesario
                } catch (IntentSender.SendIntentException sendEx) {
                    // Manejar el error
                    resultado.setText("Error al intentar activar el GPS");
                }
            } else {
                // GPS no disponible y no se puede resolver
                resultado.setText("¡Activa el GPS!");
            }
        });
    }

    private void requestLocationPermission() {
        List<String> permissionsNeeded = new ArrayList<>();
        permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);

        // Solicitar los permisos necesarios
        ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), REQUEST_CODE_LOCATION_PERMISSION);
    }

    private void requestBackgroundLocationPermission() {
        // Para Android 10+ (API 29), solicitar permisos de ubicación en segundo plano
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, REQUEST_CODE_BACKGROUND_LOCATION_PERMISSION);
        }
    }

    @SuppressLint("SetTextI18n")
    private void obtenerUbicacion() {
            // Verificar permisos
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
                return;
            }
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            latitudeGPS = String.valueOf(location.getLatitude());
                            longitudeGPS = String.valueOf(location.getLongitude());
                            // Aquí puedes continuar con el envío de datos, incluyendo latitud y longitud
                            procesarEnvioDatos();
                        } else {
                            // Manejar si la ubicación es null
                            resultado.setText("No se pudo obtener la ubicación");
                        }
                    });
        }

    @SuppressLint("SetTextI18n")
    private void procesarEnvioDatos() {
            // Aquí puedes continuar con el envío de datos, incluyendo latitud y longitud
            resultadoenvio.setText("");
            resultado.setText("");
            //Verificamos la carga
            if (nrocarga.getText().toString().isEmpty()) {
                resultado.setText("Ingrese el Remito");
            } else {
                //verificamos si ingresó la fotografía
                if (bytesfoto1.isEmpty()) {
                    resultado.setText("Tome la fotografía del Remito");
                } else {
                    //Enviamos el remito
                    final ProgressDialog ringProgressDialog = ProgressDialog.show(Remitos.this, "Por favor espere", "Enviando Remito", true);
                    ringProgressDialog.setCancelable(false);
                    new Thread(() -> {
                        //Tenemos que crear el indice = año+mes+dia+hora+minutos+segundos+carga+boca+remito
                        registrorto = Calendar.getInstance();
                        int year = registrorto.get(Calendar.YEAR);
                        int month = registrorto.get(Calendar.MONTH) + 1;
                        int day = registrorto.get(Calendar.DAY_OF_MONTH);
                        int hour = registrorto.get(Calendar.HOUR_OF_DAY);
                        int minute = registrorto.get(Calendar.MINUTE);
                        int seconds = registrorto.get(Calendar.SECOND);
                        registroenviar = Integer.toString(year) + (month<10?("0"+month):(month)) + (day<10?("0"+day):(day)) + (hour<10?("0"+hour):(hour)) + (minute<10?("0"+minute):(minute)) + (seconds<10?("0"+seconds):(seconds));
                        cargaenviar = nrocarga.getText().toString();
                        bocaint = Integer.parseInt(bocaremito.getText().toString());
                        bocaenviar = Integer.toString(bocaint);
                        remitoint = Integer.parseInt(remito.getText().toString());
                        remitoenviar = Integer.toString(remitoint);
                        razonsocialenviar = razonsocial.getText().toString();
                        fotoremitoenviar = bytesfoto1;
                        fotoremitoenviar2 = bytesfoto2;
                        online = internetDisponible();
                        if (online) {
                            //Tengo que subir al web-service el Remito, y si falla guardarlo
                            WebService ws = new WebService();
                            res = ws.registrarremito(cargaenviar, bocaenviar, remitoenviar,  razonsocialenviar, registroenviar, fotoremitoenviar, latitudeGPS, longitudeGPS, fotoremitoenviar2);
                            //Ademas de subir al servidor tengo que cambiar el estado
                            if (res.equals("false")) {
                                agregaroperacion();
                            } else {
                                resultadoenvio.setText("¡Remito Recibido!");
                            }
                        } else {
                            agregaroperacion();
                        }
                        runOnUiThread(this::actualizarobjetospantalla);
                        ringProgressDialog.dismiss();
                    }).start();
                }
            }
        }


    public void agregaroperacion(){
        DB.agregarRemito(cargaenviar, bocaenviar, remitoenviar, razonsocialenviar, registroenviar, fotoremitoenviar, latitudeGPS, longitudeGPS, fotoremitoenviar2);
    }

    public void transferirremitospendientes(View view){
        final ProgressDialog ringProgressDialog = ProgressDialog.show(Remitos.this, "Por favor espere", "Enviando Remitos", true);
        ringProgressDialog.setCancelable(false);
        new Thread(() -> {
            //Verificamos si tenemos conexión a Internet
            online = internetDisponible();
            if (online) {
                sincronizarremitospendientes();
            }
            runOnUiThread(this::actualizarremitospendientes);
            ringProgressDialog.dismiss();
        }).start();
    }

    @SuppressLint("Range")
    private void sincronizarremitospendientes() {
        try {
            Cursor e = DB.getRemitosPendientes();
            if (e.getCount() > 0) {
                while (e.moveToNext()) {
                    cargaenviar = e.getString(e.getColumnIndex("carga"));
                    bocaenviar = e.getString(e.getColumnIndex("boca"));
                    remitoenviar = e.getString(e.getColumnIndex("remito"));
                    razonsocialenviar = e.getString(e.getColumnIndex("razonsocial"));
                    registroenviar = e.getString(e.getColumnIndex("registro"));
                    fotoremitoenviar = e.getString(e.getColumnIndex("foto"));
                    fotoremitoenviar2 = e.getString(e.getColumnIndex("foto2"));
                    latitudeGPS = e.getString(e.getColumnIndex("latitud"));
                    longitudeGPS = e.getString(e.getColumnIndex("longitud"));
                    //Tengo que subir al web-service cada uno de los depósitos y cambiar el estado a Enviado
                    WebService ws = new WebService();
                    res = ws.registrarremito(cargaenviar, bocaenviar, remitoenviar, razonsocialenviar, registroenviar, fotoremitoenviar, latitudeGPS, longitudeGPS, fotoremitoenviar2);
                    //Si subió al servidor tengo que eliminarlo
                    if (res.equals("true")) {
                        DB.eliminarRemito(bocaenviar, remitoenviar);
                    }
                }
                e.close();
            }
        } catch (Exception e) {
            Log.e("DB_ERROR", "Error al agregar remito: ", e);  // Usar Log para registrar el error
        }
    }

    private void handleButtonClick(@NonNull View v) {
        Integer buttonNumber = buttonIdToNumberMap.get(v.getId());
        if (buttonNumber != null) {
            // Guardar buttonNumber en SharedPreferences
            SharedPreferences sharedPref = getSharedPreferences("MyPref", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt("button_id", buttonNumber);
            editor.apply(); // Aplicar cambios
            currentButtonId = v.getId();  // Guarda el ID del botón actual
            if (buttonNumber == 1) {
                // QR: No se necesita permiso de cámara, proceder directamente
                escanearQR();
            } else {
                // Fotos: Verificar permisos de cámara
                verificarPermisosCamera(buttonNumber);
            }
        }
    }

    // Método para verificar y solicitar el permiso CAMARA
    private void verificarPermisosCamera(int numeroBoton) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // El permiso CAMERA no está concedido, solicitar permiso
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            // El permiso CAMERA ya está concedido, proceder con la lógica de la cámara
            tomarFoto(numeroBoton);
        }
    }

    // Método para escanear el código QR
    private void escanearQR() {
        //Borramos los datos y habilitamos
        nrocarga.setText("");
        bocaremito.setText("");
        remito.setText("");
        razonsocial.setText("");
        tomarfoto1.setEnabled(false);
        tomarfoto1.setBackgroundColor(0xFFFEAA0C);
        tomarfoto1.getBackground().setAlpha(64);
        tomarfoto2.setEnabled(false);
        tomarfoto2.setBackgroundColor(0xFFFEAA0C);
        tomarfoto2.getBackground().setAlpha(64);
        agregarremito.setEnabled(false);
        agregarremito.getBackground().setAlpha(64);
        resultadoenvio.setText("");
        //Habilitamos la lectura del código QR
        IntentIntegrator integrador = new IntentIntegrator(this);
        integrador.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrador.setPrompt("Leer código QR del Remito");
        integrador.setCameraId(0);  // Selecciona la cámara frontal o trasera (0 para trasera)
        integrador.setBeepEnabled(true);  // Activa sonido al leer el código
        integrador.setBarcodeImageEnabled(true);  // Habilita la captura de imagen del código
        integrador.setCaptureActivity(CaptureActivity.class);
        Intent intent = integrador.createScanIntent();
        unifiedLauncher.launch(intent);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION:
                // Manejo del permiso de cámara
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permiso de cámara concedido, tomar foto
                    Integer buttonNumber = buttonIdToNumberMap.get(currentButtonId);
                    if (buttonNumber != null) {
                        tomarFoto(buttonNumber);
                    }
                } else {
                    // Permiso de cámara denegado
                    if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                        resultado.setText("Habilitar Permiso de Cámara");
                    } else {
                        // Permiso denegado permanentemente, redirigir a configuración
                        resultado.setText("Habilitar Permiso de Cámara");
                        openAppSettings();
                    }
                }
                break;
            case REQUEST_LOCATION_PERMISSION:
                // Manejo del permiso de ubicación
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permiso de ubicación concedido, obtener la ubicación
                    agregartransaccion(null);  // Iniciar el proceso de obtener la ubicación
                } else {
                    // Permiso de ubicación denegado
                    if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                        resultado.setText("Seleccione Permiso de Ubicación: Permitir todo el tiempo");
                    } else {
                        // Permiso denegado permanentemente, redirigir a configuración
                        resultado.setText("Seleccione Permiso de Ubicación: Permitir todo el tiempo");
                        openAppSettings();
                    }
                }
                break;
            case REQUEST_CODE_BACKGROUND_LOCATION_PERMISSION:
                // Manejo del permiso de ubicación en segundo plano (Android 10+)
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permiso de ubicación en segundo plano concedido, obtener la ubicación
                    verificarGPSyObtenerUbicacion();
                } else {
                    // Permiso de ubicación en segundo plano denegado
                    if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                        resultado.setText("Seleccione Permiso de Ubicación: Permitir todo el tiempo");
                    } else {
                        resultado.setText("Seleccione Permiso de Ubicación: Permitir todo el tiempo");
                        openAppSettings();
                    }
                }
                break;
            case REQUEST_CODE_ALL_PERMISSIONS:
                // Manejo de múltiples permisos (cámara, ubicación, almacenamiento, etc.)
                Map<String, Integer> permissionResults = new HashMap<>();
                for (int i = 0; i < permissions.length; i++) {
                    permissionResults.put(permissions[i], grantResults[i]);
                }

                boolean allPermissionsGranted = true;
                boolean locationDeniedPermanently = false;
                boolean cameraDeniedPermanently = false;

                // Verificar si los permisos más importantes fueron concedidos
                if (permissionResults.get(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    locationDeniedPermanently = !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION);
                }

                if (permissionResults.get(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    cameraDeniedPermanently = !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA);
                }

                if (allPermissionsGranted) {
                    // Todos los permisos requeridos fueron concedidos
                    agregartransaccion(null);  // Obtener la ubicación
                } else {
                    if (locationDeniedPermanently || cameraDeniedPermanently) {
                        resultado.setText("Habilite permisos de Cámara y Ubicación todo el tiempo");
                        openAppSettings();
                    } else {
                        resultado.setText("Habilite permisos de Cámara y Ubicación todo el tiempo");
                    }
                }
                break;
            default:
                // Otros casos
                break;
        }
    }

    // Método para abrir la configuración de la app si el permiso fue denegado permanentemente
    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    private void tomarFoto(int numeroBoton) {
        resultado.setText("");
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File imagenArchivo = null;
        try {
            imagenArchivo = crearImagen();
        } catch (IOException ex) {
            Log.e("Error", ex.toString());
        }
        if (imagenArchivo != null) {
            fotoUri = FileProvider.getUriForFile(this, "com.arturocalcagno.fecoapp.FecoApp", imagenArchivo);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, fotoUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            // Lanzar la actividad para tomar la foto
            unifiedLauncher.launch(intent);
            // Guardar el número del botón en SharedPreferences si es necesario
            SharedPreferences sharedPref = getSharedPreferences("MyPref", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt("button_id", numeroBoton);
            editor.apply();
        }
    }

    @NonNull
    private File crearImagen() throws IOException {
        String nombreImagen = "foto_";
        File directorio = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File imagen = File.createTempFile(nombreImagen, ".jpg", directorio);
        imageurl = imagen.getAbsolutePath();
        return imagen;
    }

    private void clearCache() {
        File cacheDir = getCacheDir();
        if (cacheDir != null && cacheDir.isDirectory()) {
            File[] files = cacheDir.listFiles();
            if (files != null) for (File file : files) {
                file.delete();
            }
        }
    }

    private boolean internetDisponible() {
        ConnectivityManager cManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo nInfo = cManager.getActiveNetworkInfo();
        return nInfo != null && nInfo.isConnected();
    }

}
