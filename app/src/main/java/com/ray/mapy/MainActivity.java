package com.ray.mapy;

import android.content.Intent;
import android.os.Bundle;

import com.getbase.floatingactionbutton.FloatingActionButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;

import androidx.annotation.NonNull;

import java.util.List;


// Clase Principal
public class MainActivity extends AppCompatActivity implements
        OnMapReadyCallback {

    // Declaración de Variables importantes
    private PermissionsManager permissionsManager;
    private MapboxMap mapboxMap;
    private MapView mapView;
    private FloatingActionButton fabCamara,fabCasa,fabRuta,fabRealtime;
    private FloatingActionsMenu fabMenu;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inicializamos Mapbox con nuestro Token Unico
        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_main);
        // Establecemos nuestro toolbar personalizado en la activity
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Referenciamos el Mapa
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        // Preparamos mapbox para trabajar con sus metodos
        mapView.getMapAsync(this);

        /// Referenciamos el FAB Item - Mover Camara
        fabCamara = findViewById(R.id.fabCamara);
        fabCasa = findViewById(R.id.fabCasa);
        fabRuta = findViewById(R.id.fabRuta);
        fabRealtime = findViewById(R.id.fabRealtime);
        fabMenu = findViewById(R.id.fabMenu);

        // Establecemos un evento de clic al FAB
        fabCamara.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Creamos una nueva posición de Camara
                CameraPosition position = new CameraPosition.Builder()
                        // Le asignamos coordenadas, zoom, rotacion, etc
                        .target(new LatLng(23.6683872, -100.6314134))
                        .zoom(14)
                        .bearing(180)
                        .tilt(30)
                        .build();
                // Le agregamos una animación hasta el tiempo de llegada a la posición
                mapboxMap.animateCamera(CameraUpdateFactory
                        .newCameraPosition(position), 4000);

                fabMenu.collapse();
            }
        });

        fabCasa.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Creamos una nueva posición de Camara
                CameraPosition position = new CameraPosition.Builder()
                        // Le asignamos coordenadas, zoom, rotacion, etc
                        .target(new LatLng(23.711179, -100.424445))
                        .zoom(17)
                        .bearing(180)
                        .tilt(30)
                        .build();
                // Le agregamos una animación hasta el tiempo de llegada a la posición
                mapboxMap.animateCamera(CameraUpdateFactory
                        .newCameraPosition(position), 4000);
                fabMenu.collapse();
            }
        });

        fabRealtime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), RealtimeActivity.class);
                startActivity(intent);
            }
        });

        // Establecemos el evento clic para el FAB - Ruta
        fabRuta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Nos mandara a otra activity donde se trazan rutas
                Intent intent = new Intent(getApplicationContext(), EjemploRura.class);
                startActivity(intent);
            }
        });

    }

    // Sobrecarga del metodo onMapReady que nos permite asignar eventos cuando se carga el mapa
    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        MainActivity.this.mapboxMap = mapboxMap;

        // Agregamos un tipo de mapa, en este caso el Mapa Satelital de Calles
        mapboxMap.setStyle(Style.MAPBOX_STREETS);

        mapboxMap.addMarker(new MarkerOptions()
                // Agregamos una posición y un titulo y subtitulo cuando se hace clic en el Marker
                .position(new LatLng(23.711179, -100.424445))
                .title("Casa Ray")
                .setSnippet("Ejido: El Consuelo")
        );
    }

    // Para usar Mapbox, debemos de sobrecargar los Mapas del Ciclo de Vida de un Activity
    // Esto para una mejor gestión de recursos
    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    // Sobrecarga para poder inflar nuestro menu personalizdo
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    // Sobrecarga del metodo de opciones de menu que nos permite dar un evento a cada item del
    // menu que se ha seleccionado
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            // Por cada item seleccionado agregamos un estilo diferente de mapa (Tema)
            case R.id.thm_1:
                mapboxMap.setStyle(Style.LIGHT);
                return true;
            case R.id.thm_2:
                mapboxMap.setStyle(Style.DARK);
                return true;
            case R.id.thm_3:
                mapboxMap.setStyle(Style.TRAFFIC_DAY);
                return true;
            case R.id.thm_4:
                mapboxMap.setStyle(Style.TRAFFIC_NIGHT);
                return true;
            case R.id.thm_6:
                mapboxMap.setStyle(Style.SATELLITE);
                return true;
            case R.id.thm_7:
                mapboxMap.setStyle(Style.SATELLITE_STREETS);
                return true;
            case R.id.thm_8:
                mapboxMap.setStyle(Style.MAPBOX_STREETS);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
