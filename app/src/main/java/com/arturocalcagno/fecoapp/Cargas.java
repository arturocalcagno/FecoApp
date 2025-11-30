package com.arturocalcagno.fecoapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
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
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Objects;

public class Cargas extends Activity {

    // --- Vistas ---
    private EditText empresaEditText, celularEditText, vehiculosEditText, fechaDisponibilidadEditText;
    private Spinner zonasSpinner;
    private TextView resultadoTextView;
    private Button botonEnviar;
    private ProgressBar progressBar;

    // --- Datos ---
    private String empresaZona, celularZona, zona, fechaDisponible, totalVehiculos;
    private DB dbHelper; // MEJORA: Instancia única para el helper de la DB.

    // --- Ubicación ---
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private String latitudeGPS, longitudeGPS;

    // --- Constantes ---
    private static final int REQUEST_CODE_LOCATION_PERMISSION = 1;
    private static final int REQUEST_CHECK_SETTINGS = 3;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cargas);

        // MEJORA: Inicializar el DB Helper una sola vez.
        dbHelper = new DB(this);

        inicializarVistas();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        crearLocationCallback();

        configurarSpinnerZonas();
        configurarSelectorFecha();
        cargarUltimosDatos();
    }

    private void inicializarVistas() {
        empresaEditText = findViewById(R.id.txtEmpresa);
        celularEditText = findViewById(R.id.txtCelContacto);
        zonasSpinner = findViewById(R.id.spinnerzonas);
        fechaDisponibilidadEditText = findViewById(R.id.txtFechaDisponibilidad);
        vehiculosEditText = findViewById(R.id.txtVehiculos);
        resultadoTextView = findViewById(R.id.txtResultadoDisponibilidad);
        botonEnviar = findViewById(R.id.cmdAgregarDisponibilidad);
        progressBar = findViewById(R.id.cargas_progressBar);
    }

    @SuppressLint("Range")
    private void cargarUltimosDatos() {
        // MEJORA: Usar la instancia única de dbHelper y try-with-resources para el cursor.
        try (Cursor c = dbHelper.obtenerCarga()) {
            if (c != null && c.moveToFirst()) {
                empresaZona = c.getString(c.getColumnIndex("empresa"));
                celularZona = c.getString(c.getColumnIndex("celular"));
                // zona = c.getString(c.getColumnIndex("region")); // Opcional: preseleccionar en Spinner
                empresaEditText.setText(empresaZona);
                celularEditText.setText(celularZona);
            }
        } catch (Exception e) {
            Log.e("CargasDB", "Error al cargar últimos datos", e);
        }
    }

    // Método llamado desde el XML (android:onClick="agregarDisponibilidad")
    public void agregarDisponibilidad(View view) {
        if (!validarCampos()) {
            return;
        }

        // Guardar datos de los campos
        empresaZona = empresaEditText.getText().toString();
        celularZona = celularEditText.getText().toString();
        zona = zonasSpinner.getSelectedItem().toString();
        totalVehiculos = vehiculosEditText.getText().toString();

        iniciarProcesoDeUbicacionYEnvio();
    }

    private boolean validarCampos() {
        resultadoTextView.setText(""); // Limpiar resultado anterior

        if (empresaEditText.getText().toString().trim().isEmpty()) {
            resultadoTextView.setText(R.string.cargas_error_empresa);
            return false;
        }
        if (celularEditText.getText().toString().trim().isEmpty()) {
            resultadoTextView.setText(R.string.cargas_error_celular);
            return false;
        }
        if (Objects.equals(zonasSpinner.getSelectedItem().toString(), "Seleccione")) {
            resultadoTextView.setText(R.string.cargas_error_provincia);
            return false;
        }
        if (vehiculosEditText.getText().toString().trim().isEmpty() || "0".equals(vehiculosEditText.getText().toString())) {
            resultadoTextView.setText(R.string.cargas_error_vehiculos);
            return false;
        }
        return true;
    }

    private void configurarSpinnerZonas() {
        // MEJORA: Puedes considerar mover esta lista a un array en res/values/arrays.xml
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
        zonasSpinner.setAdapter(arrayAdapter);
    }

    private void configurarSelectorFecha() {
        Calendar calendar = Calendar.getInstance();
        fechaDisponibilidadEditText.setFocusable(false);
        fechaDisponibilidadEditText.setClickable(true);

        // Fecha por defecto: mañana
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        actualizarCampoFecha(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        fechaDisponibilidadEditText.setOnClickListener(v -> {
            // Usa el calendario actual como base para el diálogo
            DatePickerDialog recogerFecha = new DatePickerDialog(Cargas.this, (view, year, month, dayOfMonth) -> {
                actualizarCampoFecha(year, month, dayOfMonth);
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

            recogerFecha.getDatePicker().setMinDate(System.currentTimeMillis());
            recogerFecha.show();
        });
    }

    @SuppressLint("DefaultLocale")
    private void actualizarCampoFecha(int year, int month, int dayOfMonth) {
        // Formato para mostrar en el EditText (dd/MM/yyyy)
        fechaDisponibilidadEditText.setText(String.format("%02d/%02d/%d", dayOfMonth, month + 1, year));
        // Formato para guardar o enviar (yyyy-MM-dd)
        fechaDisponible = String.format("%d-%02d-%02d", year, month + 1, dayOfMonth);
    }

    private void guardarCargaLocalmente() {
        // MEJORA: Usar la instancia única de dbHelper y no cerrarla.
        dbHelper.eliminarCarga();
        dbHelper.agregarCarga(empresaZona, celularZona, zona, fechaDisponible, totalVehiculos, latitudeGPS, longitudeGPS);
    }

    // --- Lógica de Ubicación y Envío ---

    private void iniciarProcesoDeUbicacionYEnvio() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_LOCATION_PERMISSION);
        } else {
            verificarGPSyObtenerUbicacion();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                verificarGPSyObtenerUbicacion();
            } else {
                Toast.makeText(this, R.string.cargas_permiso_ubicacion_necesario, Toast.LENGTH_LONG).show();
            }
        }
    }


    private void verificarGPSyObtenerUbicacion() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(5000)
                .build();

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        Task<LocationSettingsResponse> task = LocationServices.getSettingsClient(this).checkLocationSettings(builder.build());

        task.addOnSuccessListener(locationSettingsResponse -> obtenerUbicacionActual(locationRequest));
        task.addOnFailureListener(e -> {
            if (e instanceof ResolvableApiException) {
                try {
                    ((ResolvableApiException) e).startResolutionForResult(Cargas.this, REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException sendEx) {
                    resultadoTextView.setText(R.string.cargas_error_activar_gps);
                }
            } else {
                resultadoTextView.setText(R.string.cargas_necesita_gps);
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
                    procesarEnvioConUbicacion();
                } else {
                    runOnUiThread(() -> {
                        mostrarProgreso(false);
                        resultadoTextView.setText(R.string.cargas_error_ubicacion);
                    });
                }
            }
        };
    }

    @SuppressLint("MissingPermission")
    private void obtenerUbicacionActual(LocationRequest locationRequest) {
        mostrarProgreso(true);
        resultadoTextView.setText(R.string.cargas_obteniendo_ubicacion);

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null && (System.currentTimeMillis() - location.getTime()) < 60000) { // Ubicación de hace < 1 min
                latitudeGPS = String.valueOf(location.getLatitude());
                longitudeGPS = String.valueOf(location.getLongitude());
                procesarEnvioConUbicacion();
            } else {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
            }
        }).addOnFailureListener(this, e -> {
            mostrarProgreso(false);
            resultadoTextView.setText(R.string.cargas_error_ubicacion);
        });
    }

    private void procesarEnvioConUbicacion() {
        if (!internetDisponible()) {
            manejarRespuestaEnvio(false, true);
            return;
        }

        resultadoTextView.setText(R.string.cargas_enviando);

        new Thread(() -> {
            WebService ws = new WebService();
            final String res = ws.agregarcarga(empresaZona, celularZona, zona, fechaDisponible, totalVehiculos, latitudeGPS, longitudeGPS);
            final boolean exito = "true".equals(res);

            runOnUiThread(() -> manejarRespuestaEnvio(exito, false));
        }).start();
    }

    private void manejarRespuestaEnvio(boolean exito, boolean sinConexion) {
        mostrarProgreso(false);
        guardarCargaLocalmente(); // Guardar siempre la última carga

        if (sinConexion) {
            resultadoTextView.setText(R.string.cargas_sin_conexion);
            Toast.makeText(this, R.string.cargas_sin_conexion, Toast.LENGTH_LONG).show();
            return;
        }

        if (exito) {
            resultadoTextView.setText(R.string.cargas_envio_exitoso);
            Toast.makeText(this, R.string.cargas_envio_exitoso, Toast.LENGTH_LONG).show();
            // Opcional: Limpiar campos o cerrar la actividad
            // finish();
        } else {
            resultadoTextView.setText(R.string.cargas_envio_fallido);
            Toast.makeText(this, R.string.cargas_envio_fallido, Toast.LENGTH_LONG).show();
        }
    }

    private void mostrarProgreso(boolean mostrar) {
        progressBar.setVisibility(mostrar ? View.VISIBLE : View.GONE);
        botonEnviar.setEnabled(!mostrar);
    }

    public boolean internetDisponible() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        Network network = cm.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
        return capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // MEJORA: Cerrar la conexión a la DB cuando la actividad se destruye.
        if (dbHelper != null) {
            dbHelper.close();
        }
        // Detener actualizaciones de ubicación si aún están activas
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}
