package com.arturocalcagno.fecoapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button; // Se necesita para el botón de enviar
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Objects;

// --- CORRECCIÓN: Heredar de Activity directamente ---
public class Cargas extends Activity implements View.OnClickListener {

    private EditText empresa, celular, vehiculos;
    private Spinner zonas;
    private TextView resultado;
    private EditText fechadisponibilidad;
    private Button botonEnviar; // Botón para iniciar la acción

    public String empresazona = "", celularzona = "", zona = "", fechadisponible = "", totalvehiculos = "";
    private final Calendar C = Calendar.getInstance();
    private int mesini, anioini, diaini;
    public Boolean online;
    public String res = "";

    // --- Variables para Ubicación ---
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private String latitudeGPS, longitudeGPS;
    private ProgressBar progressBar;

    // --- Constantes para Permisos ---
    private static final int REQUEST_CODE_LOCATION_PERMISSION = 1;
    private static final int REQUEST_CHECK_SETTINGS = 3;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cargas);

        // --- CORRECCIÓN: Usando los IDs de tu XML y casteando al tipo correcto ---
        empresa = findViewById(R.id.txtEmpresa);
        celular = findViewById(R.id.txtCelContacto);
        zonas = findViewById(R.id.spinnerzonas);
        fechadisponibilidad = findViewById(R.id.txtFechaDisponibilidad);
        vehiculos = findViewById(R.id.txtVehiculos);
        resultado = findViewById(R.id.txtResultadoDisponibilidad);
        botonEnviar = findViewById(R.id.cmdAgregarDisponibilidad); // Asumiendo que este es el ID del botón de enviar
        progressBar = findViewById(R.id.cargas_progressBar); // El ProgressBar que agregaste a tu XML

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        agregarzonas();
        configurarfechadisponibilidad();
        traerultimaempresa();
        crearLocationCallback();
    }

    @SuppressLint("Range")
    private void traerultimaempresa() {
        // --- CORRECCIÓN: Pasar el contexto correcto (this) ---
        DB db = new DB(this);
        Cursor c = db.obtenerCarga();
        if (c != null) {
            if (c.moveToFirst()) {
                empresazona = c.getString(c.getColumnIndex("empresa"));
                celularzona = c.getString(c.getColumnIndex("celular"));
                zona = c.getString(c.getColumnIndex("region"));
                empresa.setText(empresazona);
                celular.setText(celularzona);
                // Aquí podrías preseleccionar la zona en el Spinner si quisieras
            }
            c.close();
        }
        db.close();
    }

    @Override
    public void onClick(View v) {
        // Este método está requerido por la interfaz, puede quedar vacío si no se usa.
    }

    // El método que se llama desde el atributo android:onClick del botón en el XML
    public void agregardisponibilidad(View view) {
        resultado.setText("");
        zona = zonas.getSelectedItem().toString();

        // Validaciones de los campos
        if (empresa.getText().toString().isEmpty()) {
            resultado.setText("Ingrese la Empresa");
            return;
        }
        if (celular.getText().toString().isEmpty()) {
            resultado.setText("Ingrese el Celular");
            return;
        }
        if (Objects.equals(zona, "Seleccione")) {
            resultado.setText("Seleccione una Provincia");
            return;
        }
        if (vehiculos.getText().toString().isEmpty() || "0".equals(vehiculos.getText().toString())) {
            resultado.setText("Indique cantidad de vehículos");
            return;
        }

        empresazona = empresa.getText().toString();
        celularzona = celular.getText().toString();
        totalvehiculos = vehiculos.getText().toString();

        iniciarProcesoDeUbicacionYEnvio();
    }

    // --- El resto de la lógica (listas, calendarios, ubicación, etc.) sigue igual ---

    private void agregarzonas() {
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
        fechadisponibilidad.setFocusable(false); // Para que no se pueda escribir en él
        fechadisponibilidad.setClickable(true);

        String diaFormateado = (diaini < 10) ? "0" + diaini : String.valueOf(diaini);
        String mesFormateado = (mesini + 1 < 10) ? "0" + (mesini + 1) : String.valueOf(mesini + 1);
        fechadisponibilidad.setText(diaFormateado + "/" + mesFormateado + "/" + anioini);
        fechadisponible = anioini + "-" + mesFormateado + "-" + diaFormateado;

        fechadisponibilidad.setOnClickListener(v -> {
            DatePickerDialog recogerFecha = new DatePickerDialog(Cargas.this, (view, year, month, dayOfMonth) -> {
                final int mesActual = month + 1;
                String diaFormateado1 = (dayOfMonth < 10) ? "0" + dayOfMonth : String.valueOf(dayOfMonth);
                String mesFormateado1 = (mesActual < 10) ? "0" + mesActual : String.valueOf(mesActual);
                fechadisponibilidad.setText(diaFormateado1 + "/" + mesFormateado1 + "/" + year);
                fechadisponible = year + "-" + mesFormateado1 + "-" + diaFormateado1;
                anioini = year;
                mesini = month;
                diaini = dayOfMonth;
            }, anioini, mesini, diaini);
            recogerFecha.getDatePicker().setMinDate(System.currentTimeMillis());
            recogerFecha.show();
        });
    }

    public void agregarcarga() {
        DB db = new DB(this);
        db.eliminarCarga();
        db.agregarCarga(empresazona, celularzona, zona, fechadisponible, totalvehiculos, latitudeGPS, longitudeGPS);
        db.close();
    }

    private void iniciarProcesoDeUbicacionYEnvio() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_LOCATION_PERMISSION);
        } else {
            verificarGPSyObtenerUbicacionCargas();
        }
    }

    @SuppressLint("SetTextI18n")
    private void verificarGPSyObtenerUbicacionCargas() {
        // --- CORRECCIÓN: Creación moderna de LocationRequest ---
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(5000)
                .build();

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = settingsClient.checkLocationSettings(builder.build());

        task.addOnSuccessListener(locationSettingsResponse -> {
            Log.d("Cargas_GPS", "Configuración de GPS OK. Obteniendo ubicación...");
            obtenerUbicacionActualCargas(locationRequest);
        });

        task.addOnFailureListener(e -> {
            if (e instanceof ResolvableApiException) {
                try {
                    ((ResolvableApiException) e).startResolutionForResult(Cargas.this, REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException sendEx) {
                    resultado.setText("Error al intentar activar el GPS");
                }
            } else {
                resultado.setText("¡Activa el GPS para continuar!");
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void crearLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                fusedLocationClient.removeLocationUpdates(this); // Detener actualizaciones
                if (!locationResult.getLocations().isEmpty()) {
                    android.location.Location location = locationResult.getLocations().get(0);
                    latitudeGPS = String.valueOf(location.getLatitude());
                    longitudeGPS = String.valueOf(location.getLongitude());
                    Log.d("Cargas_Location", "Ubicación FRESCA obtenida: Lat=" + latitudeGPS + ", Lon=" + longitudeGPS);
                    procesarEnvioDatosConUbicacion();
                } else {
                    Log.w("Cargas_Location", "LocationResult estaba vacío.");
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        botonEnviar.setEnabled(true);
                        resultado.setText("No se pudo obtener ubicación. Intente de nuevo.");
                    });
                }
            }
        };
    }

    @SuppressLint({"MissingPermission", "SetTextI18n"})
    private void obtenerUbicacionActualCargas(LocationRequest locationRequest) {
        // Deshabilitar botón y mostrar progreso ANTES de empezar a buscar ubicación
        botonEnviar.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        resultado.setText("Obteniendo ubicación...");

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    // Comprobar si hay una ubicación reciente y válida
                    if (location != null && (System.currentTimeMillis() - location.getTime()) < 60000) { // Menos de 1 min
                        latitudeGPS = String.valueOf(location.getLatitude());
                        longitudeGPS = String.valueOf(location.getLongitude());
                        Log.d("Cargas_Location", "Ubicación RÁPIDA (last known) obtenida.");
                        procesarEnvioDatosConUbicacion();
                    } else {
                        // Si no hay ubicación rápida, solicitar una nueva
                        Log.d("Cargas_Location", "Solicitando ubicación fresca...");
                        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
                    }
                })
                .addOnFailureListener(this, e -> {
                    Log.e("Cargas_Location", "Error al obtener getLastLocation.", e);
                    progressBar.setVisibility(View.GONE);
                    botonEnviar.setEnabled(true);
                    resultado.setText("Error al obtener la ubicación.");
                });
    }

    @SuppressLint("SetTextI18n")
    private void procesarEnvioDatosConUbicacion() {
        resultado.setText("Enviando disponibilidad...");
        agregarcarga();

        new Thread(() -> {
            online = internetDisponible();
            if (online) {
                WebService ws = new WebService();
                res = ws.registrarcarganew(empresazona, celularzona, zona, fechadisponible, totalvehiculos, latitudeGPS, longitudeGPS);
            } else {
                res = "no_internet";
            }

            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                botonEnviar.setEnabled(true);

                if ("true".equals(res)) {
                    vehiculos.setText("");
                    resultado.setText("¡Disponibilidad Recibida con Ubicación!");
                } else if ("no_internet".equals(res)) {
                    resultado.setText("Sin conexión, vuelva a intentar");
                } else {
                    resultado.setText("Error al enviar disponibilidad. Intente luego.");
                    Log.e("Cargas_WebService", "Respuesta de registrarcarganew: " + res);
                }
            });
        }).start();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_LOCATION_PERMISSION) {
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
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                Log.d("Cargas_GPS", "El usuario activó el GPS. Reintentando.");
                verificarGPSyObtenerUbicacionCargas();
            } else {
                Log.w("Cargas_GPS", "El usuario NO activó el GPS.");
                resultado.setText("La activación del GPS es necesaria para continuar.");
                progressBar.setVisibility(View.GONE);
                botonEnviar.setEnabled(true);
            }
        }
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
        Toast.makeText(this, "Debe habilitar el permiso de ubicación manualmente", Toast.LENGTH_LONG).show();
    }

    // --- CORRECCIÓN: Método modernizado para verificar internet ---
    public boolean internetDisponible() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        Network network = cm.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
        return capabilities != null && (
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    }
}

