package com.ray.mapy;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class SplashScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        //Creamos un nuevo handler para darle una duracion al splash
        new Handler().postDelayed(new Runnable() {
            //Sobreescribimos el metodo run
            @Override
            public void run() {
                // Abriremos el el Activity Login y lo destruiremos para que al querer regresar
                // ya no se puede accesar al Splash
                Intent intent = new Intent(SplashScreen.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
            // El splash tendra una duracion de 2.5 segundos
        },2500);
    }
}
