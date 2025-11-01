package com.arturocalcagno.fecoapp;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context; // <-- CORRECCIÓN: Importar Context
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DB extends SQLiteOpenHelper {

    private static final String DB_NAME = "fecoapp";
    private static final int SCHEME_VERSION = 7;
    // --- BUENA PRÁCTICA: Quitar la variable 'db' estática ---
    // La base de datos debe ser gestionada por instancia para evitar fugas de memoria.
    // private static SQLiteDatabase db;

    // --- CORRECCIÓN CLAVE: El constructor debe aceptar un Context genérico ---
    public DB(Context context) {
        super(context, DB_NAME, null, SCHEME_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DBTablas.CREATE_DB_TABLE1);
        db.execSQL(DBTablas.CREATE_DB_TABLE2);
    }

    @SuppressLint("Range") // Para simplificar el código dentro del método
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 5) {
            db.execSQL(DBTablas.CREATE_DB_TABLE2);
        }
        if (oldVersion < 6) {
            Cursor cursor = db.rawQuery("PRAGMA table_info(remitos)", null);
            boolean columnExists = false;
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String columnName = cursor.getString(cursor.getColumnIndex("name"));
                    if ("foto2".equals(columnName)) {
                        columnExists = true;
                        break;
                    }
                }
                cursor.close();
            }
            if (!columnExists) {
                db.execSQL("ALTER TABLE remitos ADD COLUMN foto2 TEXT");
            }
        }
        if (oldVersion < 7) {
            db.execSQL("ALTER TABLE cargas ADD COLUMN latcargas TEXT");
            db.execSQL("ALTER TABLE cargas ADD COLUMN loncargas TEXT");
        }
    }

    // --- OPTIMIZACIÓN: Los métodos no deben ser estáticos si la DB no lo es ---
    public void agregarRemito(String carga, String boca, String remito, String razonsocial, String registro, String foto, String latitud, String longitud, String foto2) {
        SQLiteDatabase db = this.getWritableDatabase(); // Obtener la instancia de DB
        ContentValues valores = new ContentValues();
        valores.put(DBTablas.FIELD_CARGA, carga);
        valores.put(DBTablas.FIELD_BOCA, boca);
        valores.put(DBTablas.FIELD_REMITO, remito);
        valores.put(DBTablas.FIELD_RAZONSOCIAL, razonsocial);
        valores.put(DBTablas.FIELD_REGISTRO, registro);
        valores.put(DBTablas.FIELD_FOTO, foto);
        valores.put(DBTablas.FIELD_FOTO2, foto2);
        valores.put(DBTablas.FIELD_LATITUD, latitud);
        valores.put(DBTablas.FIELD_LONGITUD, longitud);
        try {
            db.insert(DBTablas.TABLE_REMITOS, null, valores);
        } catch (Exception e) {
            Log.e("DB_ERROR", "Error al agregar remito: ", e);
        }
        // No es necesario cerrar la db aquí si se va a usar de nuevo, el sistema lo gestiona.
    }

    public Cursor getRemitosPendientes() {
        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT carga, boca, remito, razonsocial, registro, foto, latitud, longitud, foto2 FROM " + DBTablas.TABLE_REMITOS;
        return db.rawQuery(selectQuery, null);
    }

    public void eliminarRemito(String boca, String remito) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(DBTablas.TABLE_REMITOS, "boca =? AND remito =?", new String[]{boca, remito});
    }

    public Cursor getTotalRemitosPendientes() {
        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT COUNT(remito) AS total FROM " + DBTablas.TABLE_REMITOS;
        return db.rawQuery(selectQuery, null);
    }

    //Cargas
    public void eliminarCarga() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(DBTablas.TABLE_CARGAS, null, null);
    }

    public void agregarCarga(String empresa, String celular, String region, String disponibilidad, String vehiculos, String latcargas, String loncargas) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues valores = new ContentValues();
        valores.put(DBTablas.FIELD_EMPRESA, empresa);
        valores.put(DBTablas.FIELD_CELULAR, celular);
        valores.put(DBTablas.FIELD_REGION, region);
        valores.put(DBTablas.FIELD_DISPONIBILIDAD, disponibilidad);
        valores.put(DBTablas.FIELD_VEHICULOS, vehiculos);
        valores.put(DBTablas.FIELD_LATCARGAS, latcargas);
        valores.put(DBTablas.FIELD_LONCARGAS, loncargas);
        try {
            db.insert(DBTablas.TABLE_CARGAS, null, valores);
        } catch (Exception e) {
            Log.e("DB_ERROR", "Error al agregar carga: ", e);
        }
    }

    public Cursor obtenerCarga() {
        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT empresa, celular, region FROM " + DBTablas.TABLE_CARGAS;
        return db.rawQuery(selectQuery, null);
    }
}

