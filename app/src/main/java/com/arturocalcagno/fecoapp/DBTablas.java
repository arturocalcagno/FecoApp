package com.arturocalcagno.fecoapp;

public class DBTablas {

    // Creamos la tabla de remitos
    public static final String TABLE_REMITOS = "remitos";
    public static final String FIELD_ID1 = "idremito";
    public static final String FIELD_CARGA = "carga";
    public static final String FIELD_BOCA = "boca";
    public static final String FIELD_REMITO = "remito";
    public static final String FIELD_FOTO = "foto";
    public static final String FIELD_FOTO2 = "foto2";
    public static final String FIELD_REGISTRO = "registro";
    public static final String FIELD_ESTADO = "estado";
    public static final String FIELD_INDICE = "indice";
    public static final String FIELD_RAZONSOCIAL = "razonsocial";
    public static final String FIELD_LATITUD = "latitud";
    public static final String FIELD_LONGITUD = "longitud";
    public static final String CREATE_DB_TABLE1 = "create table " + TABLE_REMITOS + "( " +
            FIELD_ID1 + " integer primary key autoincrement," +
            FIELD_CARGA + " text," +
            FIELD_BOCA + " text," +
            FIELD_REMITO + " text," +
            FIELD_FOTO + " text," +
            FIELD_FOTO2 + " text," +
            FIELD_REGISTRO + " text," +
            FIELD_ESTADO + " text," +
            FIELD_INDICE + " text," +
            FIELD_RAZONSOCIAL + " text," +
            FIELD_LATITUD + " text," +
            FIELD_LONGITUD + " text" + ");";

    // Creamos la tabla de cargas
    public static final String TABLE_CARGAS = "cargas";
    public static final String FIELD_ID2 = "idcarga";
    public static final String FIELD_EMPRESA = "empresa";
    public static final String FIELD_CELULAR = "celular";
    public static final String FIELD_REGION = "region";
    public static final String FIELD_DISPONIBILIDAD = "disponibilidad";
    public static final String FIELD_VEHICULOS = "vehiculos";
    public static final String FIELD_LATCARGAS = "latcargas";
    public static final String FIELD_LONCARGAS = "loncargas";
    public static final String CREATE_DB_TABLE2 = "create table " + TABLE_CARGAS + "( " +
            FIELD_ID2 + " integer primary key autoincrement," +
            FIELD_EMPRESA + " text," +
            FIELD_CELULAR + " text," +
            FIELD_REGION + " text," +
            FIELD_DISPONIBILIDAD + " text," +
            FIELD_VEHICULOS + " text," +
            FIELD_LATCARGAS + " text," +
            FIELD_LONCARGAS + " text" + ");";

}
