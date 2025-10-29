package com.arturocalcagno.fecoapp;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by Arturo on 16/04/2022.
 */
public class DB extends SQLiteOpenHelper {

    //CREACION BASE DE DATOS
    private static final String DB_NAME = "fecoapp";
    private static final int SCHEME_VERSION = 7;
    public static SQLiteDatabase db;

    public DB(FecoApp context) {
        super(context, DB_NAME, null, SCHEME_VERSION);
        // Con esto si no está creada la base de datos la va a crear
        db = this.getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DBTablas.CREATE_DB_TABLE1);
        db.execSQL(DBTablas.CREATE_DB_TABLE2);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 5) {
            db.execSQL(DBTablas.CREATE_DB_TABLE2);
        }
        if (oldVersion < 6) {
            // Verificar si la columna 'foto2' ya existe
            Cursor cursor = db.rawQuery("PRAGMA table_info(remitos)", null);
            boolean columnExists = false;
            if (cursor.moveToFirst()) {
                do {
                    @SuppressLint("Range") String columnName = cursor.getString(cursor.getColumnIndex("name"));
                    if ("foto2".equals(columnName)) {
                        columnExists = true;
                        break;
                    }
                } while (cursor.moveToNext());
            }
            cursor.close();
            // Si la columna no existe, agregarla
            if (!columnExists) {
                db.execSQL("ALTER TABLE remitos ADD COLUMN foto2 TEXT");
            }
        }
        if (oldVersion < 7) {
            // Agregamos las columnas latcargas y loncargas a la tabla de cargas
            db.execSQL("ALTER TABLE cargas ADD COLUMN latcargas TEXT");
            db.execSQL("ALTER TABLE cargas ADD COLUMN loncargas TEXT");
        }
    }

    //Remitos
    public static void agregarRemito(String carga, String boca, String remito, String razonsocial, String registro, String foto, String latitud, String longitud, String foto2){
        ContentValues valores = new ContentValues();
        valores.put(DBTablas.FIELD_CARGA, carga);
        valores.put(DBTablas.FIELD_BOCA, boca );
        valores.put(DBTablas.FIELD_REMITO, remito);
        valores.put(DBTablas.FIELD_RAZONSOCIAL, razonsocial);
        valores.put(DBTablas.FIELD_REGISTRO, registro);
        valores.put(DBTablas.FIELD_FOTO, foto);
        valores.put(DBTablas.FIELD_FOTO2, foto2);
        valores.put(DBTablas.FIELD_LATITUD, latitud);
        valores.put(DBTablas.FIELD_LONGITUD, longitud);
        try
        {
            db.insert(DBTablas.TABLE_REMITOS, null, valores);
        } catch (Exception e) {
            Log.e("DB_ERROR", "Error al agregar remito: ", e);  // Usar Log para registrar el error
        }
    }

    public static Cursor getRemitosPendientes() {
        String selectQuery= "SELECT carga, boca, remito, razonsocial, registro, foto, latitud, longitud, foto2 FROM "+DBTablas.TABLE_REMITOS;
        return db.rawQuery(selectQuery,null);
    }

    public static void eliminarRemito(String boca, String remito) {
        db.delete(DBTablas.TABLE_REMITOS,"boca ='"+boca+"' AND remito ='"+remito+"'",null);
    }

    public static Cursor getTotalRemitosPendientes() {
        String selectQuery= "SELECT COUNT(remito) AS total FROM "+DBTablas.TABLE_REMITOS;
        return db.rawQuery(selectQuery, null);
    }

    //Cargas
    public void eliminarCarga() {
        SQLiteDatabase db=this.getReadableDatabase();
        db.delete(DBTablas.TABLE_CARGAS,null,null);
    }

    public void agregarCarga(String empresa, String celular, String region, String disponibilidad, String vehiculos, String latcargas, String loncargas){
        ContentValues valores = new ContentValues();
        valores.put(DBTablas.FIELD_EMPRESA, empresa);
        valores.put(DBTablas.FIELD_CELULAR, celular );
        valores.put(DBTablas.FIELD_REGION, region);
        valores.put(DBTablas.FIELD_DISPONIBILIDAD, disponibilidad);
        valores.put(DBTablas.FIELD_VEHICULOS, vehiculos);
        valores.put(DBTablas.FIELD_LATCARGAS, latcargas);
        valores.put(DBTablas.FIELD_LONCARGAS, loncargas);
        try
        {
            db.insert(DBTablas.TABLE_CARGAS, null, valores);
        } catch (Exception e) {
            Log.e("DB_ERROR", "Error al agregar remito: ", e);  // Usar Log para registrar el error
        }
    }

    public Cursor obtenerCarga() {
        SQLiteDatabase db=this.getReadableDatabase();
        String selectQuery= "SELECT empresa, celular, region FROM "+DBTablas.TABLE_CARGAS;
        return db.rawQuery(selectQuery,null);
    }

}
