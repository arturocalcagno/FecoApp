package com.arturocalcagno.fecoapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class Principal extends FecoApp {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.principal);
    }

    public void pantallaremitos (View view){
        Intent i = new Intent(this, Remitos.class) ;
        startActivity(i);
    }

    public void pantallacargas (View view){
        Intent i = new Intent(this, Cargas.class) ;
        startActivity(i);
    }

    public void lanzarprincipal (View view){
        Intent i = new Intent(this, FecoApp.class) ;
        startActivity(i);
    }

}
