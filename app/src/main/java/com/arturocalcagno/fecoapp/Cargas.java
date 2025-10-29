package com.arturocalcagno.fecoapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Objects;

public class Cargas extends FecoApp implements View.OnClickListener{

    private EditText empresa;
    private EditText celular;
    private Spinner zonas;
    private EditText fechadisponibilidad;
    private EditText vehiculos;
    private TextView resultado;
    private boolean online;
    private int anioini, mesini, diaini;
    private String fechadisponible;
    Calendar C = Calendar.getInstance();
    private String zona;
    private String empresazona;
    private String celularzona;
    private String totalvehiculos;
    private String res;

    // --- NUEVAS VARIABLES PARA UBICACIÓN ---
    private FusedLocationProviderClient fusedLocationClient;
    private String latitudeGPS;
    private String longitudeGPS;

    // --- NUEVAS CONSTANTES PARA PERMISOS ---
    private static final int REQUEST_CODE_LOCATION_PERMISSION = 1004; 
    private static final int REQUEST_CODE_BACKGROUND_LOCATION_PERMISSION = 2001; 
    private static final int REQUEST_CHECK_SETTINGS = 100; 


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cargas);
        empresa = findViewById(R.id.txtEmpresa);
        celular = findViewById(R.id.txtCelContacto);
        zonas = findViewById(R.id.spinnerzonas);
        fechadisponibilidad = findViewById(R.id.txtFechaDisponibilidad);
        vehiculos = findViewById(R.id.txtVehiculos);
        resultado = findViewById(R.id.txtResultadoDisponibilidad);

        // --- INICIALIZAR CLIENTE DE UBICACIÓN ---
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        agregarzonas();
        configurarfechadisponibilidad();
        pantallainicial();
    }

    public void lanzarprincipal (View view){
        Intent i = new Intent(this, Principal.class) ;
        startActivity(i);
    }

    @SuppressLint("Range")
    public void pantallainicial() {
        DB db=new DB(this);
        Cursor c = db.obtenerCarga();
        if (c != null) {
            if (c.moveToFirst()){
                empresazona = c.getString(c.getColumnIndex("empresa"));
                celularzona = c.getString( c.getColumnIndex( "celular" ) );
                zona = c.getString( c.getColumnIndex( "region" ) );
                empresa.setText(empresazona);
                celular.setText(celularzona);
            }
            c.close();
        }
        db.close();
    }

    @Override
    public void onClick(View v) {
    }

    private void agregarzonas(){
        final ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add("Seleccione");
        arrayList.add("CABA");
        arrayList.add("Buenos Aires");
        arrayList.add("Catamarca");
        arrayList.add("Chaco");
        arrayList.add("Chubut");
        arrayList.add("Córdoba");
        arrayList.add("Corrientes");
        arrayList.add("Entre Ríos");
        arrayList.add("Formosa");
        arrayList.add("Jujuy");
        arrayList.add("La Pampa");
        arrayList.add("La Rioja");
        arrayList.add("Mendoza");
        arrayList.add("Misiones");
        arrayList.add("Neuquén");
        arrayList.add("Río Negro");
        arrayList.add("Salta");
        arrayList.add("San Juan");
        arrayList.add("San Luis");
        arrayList.add("Santa Cruz");
        arrayList.add("Santa Fé");
        arrayList.add("Santiago del Estero");
        arrayList.add("Tierra del Fuego");
        arrayList.add("Tucumán");
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, R.layout.spinner_items, arrayList);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        zonas.setAdapter(arrayAdapter);
    }

    @SuppressLint("SetTextI18n")
    private void configurarfechadisponibilidad() {
        anioini = C.get(Calendar.YEAR);
        mesini = C.get(Calendar.MONTH);
        diaini = C.get(Calendar.DAY_OF_MONTH) + 1;
        fechadisponibilidad.setEnabled(true);
        String diaFormateado = (diaini < 10)? "0" + String.valueOf(diaini):String.valueOf(diaini);
        String mesFormateado = (mesini + 1 < 10)? "0" + String.valueOf(mesini + 1):String.valueOf(mesini + 1);
        fechadisponibilidad.setText(diaFormateado + "/" + mesFormateado + "/" + anioini);
        fechadisponible = anioini + "-" + mesFormateado + "-" + diaFormateado;
        fechadisponibilidad.setOnClickListener(v -> {
            final Calendar calendar = Calendar.getInstance();
            DatePickerDialog recogerFecha = new DatePickerDialog(Cargas.this, (view, year, month, dayOfMonth) -> {
                final int mesActual = month + 1;
                String diaFormateado1 = (dayOfMonth < 10)? "0" + String.valueOf(dayOfMonth):String.valueOf(dayOfMonth);
                String mesFormateado1 = (mesActual < 10)? "0" + String.valueOf(mesActual):String.valueOf(mesActual);
                fechadisponibilidad.setText(diaFormateado1 + "/" + mesFormateado1 + "/" + year);
                fechadisponible = year + "-" + mesFormateado1 + "-" + diaFormateado1;
                anioini = year;
                mesini = month;
                diaini = dayOfMonth;
            },anioini, mesini, diaini);
            recogerFecha.getDatePicker().setMinDate(calendar.getTimeInMillis());
            recogerFecha.show();
        });
    }

//    @SuppressLint("SetTextI18n")
//    public void agregardisponibilidad (View view) {
//        resultado.setText("");
//        zona = zonas.getSelectedItem().toString();
//        if (empresa.getText().toString().isEmpty()) {
//            resultado.setText("Ingrese la Empresa");
//        } else {
//            if (celular.getText().toString().isEmpty()) {
//                resultado.setText("Ingrese el Celular");
//            } else {
//                if(Objects.equals(zona, "") || Objects.equals(zona, "Seleccione") || zona.isEmpty()) {
//                    resultado.setText("Seleccione una Provincia");
//                } else {
//                    if(Objects.equals(fechadisponible, "")) {
//                        resultado.setText("Seleccione una Fecha");
//                    } else {
//                        totalvehiculos = vehiculos.getText().toString();
//                        if(totalvehiculos.isEmpty() || totalvehiculos.equals("0")) {
//                            resultado.setText("Indique cantidad de vehículos");
//                        } else {
//                            empresazona = empresa.getText().toString();
//                            celularzona = celular.getText().toString();
//                            // NO se llama a agregarcarga() aquí porque este flujo no obtiene la ubicación.
//
//                            final ProgressDialog ringProgressDialog = ProgressDialog.show(Cargas.this, "Por favor espere", "Enviando Disponibilidad", true);
//                            ringProgressDialog.setCancelable(false);
//                            new Thread(() -> {
//                                online = internetDisponible();
//                                if (online) {
//                                    WebService ws = new WebService();
//                                    res = ws.registrarcarga(empresazona, celularzona, zona,  fechadisponible, totalvehiculos);
//                                } else {
//                                    runOnUiThread(() -> resultado.setText("Sin conexión, vuelva a intentar"));
//                                    res = "false";
//                                }
//                                runOnUiThread(() -> {
//                                    if ("true".equals(res)) {
//                                        fechadisponibilidad.setText("");
//                                        fechadisponible = "";
//                                        vehiculos.setText("");
//                                        totalvehiculos = "";
//                                        resultado.setText("¡Disponibilidad Recibida!");
//                                    } else if (!online && "false".equals(res)) {
//                                        // Mensaje de sin conexión ya mostrado
//                                    } else {
//                                        resultado.setText("Error al enviar disponibilidad.");
//                                    }
//                                });
//                                ringProgressDialog.dismiss();
//                            }).start();
//                        }
//                    }
//                }
//            }
//        }
//    }

    public void agregarcarga(){
        DB db=new DB(this);
        db.eliminarCarga();
        db.agregarCarga(empresazona, celularzona, zona, fechadisponible, totalvehiculos, latitudeGPS, longitudeGPS);
        db.close();
    }

    @SuppressLint("SetTextI18n")
    public void agregardisponibilidad(View view) {
        resultado.setText(""); 
        zona = zonas.getSelectedItem().toString();
        if (empresa.getText().toString().isEmpty()) {
            resultado.setText("Ingrese la Empresa");
            return;
        }
        if (celular.getText().toString().isEmpty()) {
            resultado.setText("Ingrese el Celular");
            return;
        }
        if (Objects.equals(zona, "") || Objects.equals(zona, "Seleccione") || zona.isEmpty()) {
            resultado.setText("Seleccione una Provincia");
            return;
        }
        if (Objects.equals(fechadisponible, "")) {
            resultado.setText("Seleccione una Fecha");
            return;
        }
        totalvehiculos = vehiculos.getText().toString();
        if (totalvehiculos.isEmpty() || totalvehiculos.equals("0")) {
            resultado.setText("Indique cantidad de vehículos");
            return;
        }
        empresazona = empresa.getText().toString();
        celularzona = celular.getText().toString();
        iniciarProcesoDeUbicacionYEnvio();
    }

    @SuppressLint("SetTextI18n")
    private void iniciarProcesoDeUbicacionYEnvio() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestLocationPermissionCargas();
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                       ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestBackgroundLocationPermissionCargas();
            } else {
                verificarGPSyObtenerUbicacionCargas();
            }
        } else {
            verificarGPSyObtenerUbicacionCargas();
        }
    }

    private void requestLocationPermissionCargas() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_LOCATION_PERMISSION);
    }

    private void requestBackgroundLocationPermissionCargas() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, REQUEST_CODE_BACKGROUND_LOCATION_PERMISSION);
        }
    }

    @SuppressLint("SetTextI18n")
    private void verificarGPSyObtenerUbicacionCargas() {
        LocationRequest locationRequest = LocationRequest.create();
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = settingsClient.checkLocationSettings(builder.build());

        task.addOnSuccessListener(locationSettingsResponse -> {
            Log.d("Cargas_GPS", "Configuración de GPS OK. Obteniendo ubicación...");
            obtenerUbicacionActualCargas();
        });

        task.addOnFailureListener(e -> {
            Log.e("Cargas_GPS", "Error en configuración de GPS.", e);
            if (e instanceof ResolvableApiException) {
                try {
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(Cargas.this, REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException sendEx) {
                    Log.e("Cargas_GPS", "Error al intentar activar GPS.", sendEx);
                    resultado.setText("Error al intentar activar el GPS");
                }
            } else {
                resultado.setText("¡Activa el GPS para continuar!");
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void obtenerUbicacionActualCargas() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w("Cargas_Location", "Permiso de ubicación no concedido al intentar obtener ubicación.");
            resultado.setText("Permiso de ubicación no concedido.");
            requestLocationPermissionCargas();
            return;
        }

        fusedLocationClient.getLastLocation()
            .addOnSuccessListener(this, location -> {
                if (location != null) {
                    latitudeGPS = String.valueOf(location.getLatitude());
                    longitudeGPS = String.valueOf(location.getLongitude());
                    Log.d("Cargas_Location", "Ubicación obtenida: Lat=" + latitudeGPS + ", Lon=" + longitudeGPS);
                    procesarEnvioDatosConUbicacion();
                } else {
                    Log.w("Cargas_Location", "No se pudo obtener la última ubicación conocida (puede ser null).");
                    resultado.setText("No se pudo obtener la ubicación. Intenta de nuevo.");
                }
            })
            .addOnFailureListener(this, e -> {
                Log.e("Cargas_Location", "Error al obtener ubicación.", e);
                resultado.setText("Error al obtener ubicación.");
            });
    }

    @SuppressLint("SetTextI18n")
    private void procesarEnvioDatosConUbicacion() {
        agregarcarga();

        final ProgressDialog ringProgressDialog = ProgressDialog.show(Cargas.this, "Por favor espere", "Enviando Disponibilidad", true);
        ringProgressDialog.setCancelable(false);
        new Thread(() -> {
            online = internetDisponible(); 
            if (online) {
                WebService ws = new WebService();
                Log.d("Cargas_WebService", "Enviando a registrarcarganew: " + empresazona + ", " + celularzona + ", " + zona + ", " + fechadisponible + ", " + totalvehiculos + ", Lat: " + latitudeGPS + ", Lon: " + longitudeGPS);
                res = ws.registrarcarganew(empresazona, celularzona, zona, fechadisponible, totalvehiculos, latitudeGPS, longitudeGPS);
            } else {
                runOnUiThread(() -> resultado.setText("Sin conexión, vuelva a intentar"));
                res = "false"; 
            }

            runOnUiThread(() -> {
                if ("true".equals(res)) { 
                    fechadisponibilidad.setText("");
                    fechadisponible = "";
                    vehiculos.setText("");
                    totalvehiculos = "";
                    resultado.setText("¡Disponibilidad Recibida con Ubicación!");
                } else if (!online && "false".equals(res)) {
                    // Ya se manejó el mensaje de "Sin conexión"
                }
                else {
                    resultado.setText("Error al enviar disponibilidad. Intente luego.");
                    Log.e("Cargas_WebService", "Respuesta de registrarcarganew: " + res);
                }
            });
            ringProgressDialog.dismiss();
        }).start();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CODE_LOCATION_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Cargas_Permission", "Permiso ACCESS_FINE_LOCATION concedido.");
                    verificarGPSyObtenerUbicacionCargas();
                } else {
                    Log.w("Cargas_Permission", "Permiso ACCESS_FINE_LOCATION denegado.");
                    resultado.setText("Permiso de ubicación necesario para continuar.");
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                       openAppSettings(); 
                    }
                }
                break;
            case REQUEST_CODE_BACKGROUND_LOCATION_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Cargas_Permission", "Permiso ACCESS_BACKGROUND_LOCATION concedido.");
                    verificarGPSyObtenerUbicacionCargas();
                } else {
                    Log.w("Cargas_Permission", "Permiso ACCESS_BACKGROUND_LOCATION denegado.");
                }
                break;
        }
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }
    
//    private boolean internetDisponible() {
//        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
//        if (cm == null) {
//            Log.e("Cargas_Network", "ConnectivityManager es null.");
//            return false;
//        }
//        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
//        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
//    }
}
