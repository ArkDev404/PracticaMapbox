package com.ray.mapy;


import android.annotation.SuppressLint;
import android.location.Location;
import android.os.Bundle;
import android.widget.Toast;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;

import java.lang.ref.WeakReference;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

// Clase que permite la localización en tiempo real
public class RealtimeActivity extends AppCompatActivity implements
        OnMapReadyCallback, PermissionsListener {
    // Variables que establecen el tiempo de la petición al GPS
    private static final long DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L;
    private static final long DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5;

    // Variables importantes para la construcción de la app
    private MapboxMap mapboxMap;
    private MapView mapView;
    private PermissionsManager permissionsManager;
    private LocationEngine locationEngine;
    private LocationChangeListeningActivityLocationCallback callback =
            new LocationChangeListeningActivityLocationCallback(this);
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Token para poder acceder a la API de Mapbox
        Mapbox.getInstance(this, getString(R.string.access_token));
        // Referencia a la vista de esta activity
        setContentView(R.layout.activity_realtime);
        // Referencias al Mapview y preparación del mapa
        mapView = findViewById(R.id.mapView3);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
    }

    // Sobrecarga del metodo onMapReady
    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
// Agregamos el estilo de mapa con calles
        mapboxMap.setStyle(Style.MAPBOX_STREETS,
                new Style.OnStyleLoaded() {
                    @Override public void onStyleLoaded(@NonNull Style style) {
                        // Al iniciar el mapa comenzamos a hacer las peticiones al GPS
                        // con este metodo
                        enableLocationComponent(style);
                    }
                });
    }


    // Con este metodo comenzamos a realizar las peticiones
    @SuppressWarnings( {"MissingPermission"})
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
        // Verificamos si tenemos los permisos de localizacion habilitados
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            // Creamos una instancia para obtener la localización
            LocationComponent locationComponent = mapboxMap.getLocationComponent();
            LocationComponentActivationOptions locationComponentActivationOptions =
                    LocationComponentActivationOptions.builder(this, loadedMapStyle)
                            .useDefaultLocationEngine(false)
                            .build();
            locationComponent.activateLocationComponent(locationComponentActivationOptions);
            //Habilitamos para que sea visible el punto de localización
            locationComponent.setLocationComponentEnabled(true);
            //Seguimos el punto de la localización
            locationComponent.setCameraMode(CameraMode.TRACKING);
            // Movemos con la brujula los sitios a trasladarse
            locationComponent.setRenderMode(RenderMode.COMPASS);
            // Iniciamos la localizacion
            initLocationEngine();
        } else {
            //en caso de no estar habilitados los permisos los solicitamos
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }
    // Este metodo permite establecer el tiempo en que se haran las peticiones de localización
    @SuppressLint("MissingPermission")
    private void initLocationEngine() {
        locationEngine = LocationEngineProvider.getBestLocationEngine(this);
        // Establecemos el tiempo y la prioridad en el que queremos hacer las peticiones
        LocationEngineRequest request = new LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
                .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME).build();
        // Solicitamos las actualizaciones de la ubicacion
        locationEngine.requestLocationUpdates(request, callback, getMainLooper());
        locationEngine.getLastLocation(callback);
    }

    // Este metodo es el que nos permite solicitar los permisos
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    // Este método permite especificar para que queremos solicitar permisos
    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, R.string.user_location_permission_explanation,
                Toast.LENGTH_LONG).show();
    }
    // Este metodo nos refleja el resultado de la solicitud de permisos
    @Override
    public void onPermissionResult(boolean granted) {
        // Si se aceptaron
        if (granted) {
            mapboxMap.getStyle(new Style.OnStyleLoaded() {
                @Override
                // Comenzamos a realizar la localización
                public void onStyleLoaded(@NonNull Style style) {
                    enableLocationComponent(style);
                }
            });
        } else {
            // En caso contrario indicamos que no se aceptaron
            Toast.makeText(this, R.string.user_location_permission_not_granted,
                    Toast.LENGTH_LONG).show();
        }
    }
    // Clase auxiliar que nos permite obtener las actualizaciones de localizacion
    private static class LocationChangeListeningActivityLocationCallback
            implements LocationEngineCallback<LocationEngineResult> {

        // Creamos una peticion a nuestra Activity
        private final WeakReference<RealtimeActivity> activityWeakReference;

        // Constructor de la clase
        LocationChangeListeningActivityLocationCallback(RealtimeActivity activity) {
            this.activityWeakReference = new WeakReference<>(activity);
        }
        // Metodo que nos indica que si las peticiones se realizan
        @Override
        public void onSuccess(LocationEngineResult result) {
            RealtimeActivity activity = activityWeakReference.get();
// Si se realizan obtenemos la ultima ubicacion solicitada
            if (activity != null) {
                Location location = result.getLastLocation();
// Si no se encuentra se sigue solicitando
                if (location == null) {
                    return;
                }
                if (activity.mapboxMap != null && result.getLastLocation() != null) {
                    activity.mapboxMap.getLocationComponent().forceLocationUpdate(result.getLastLocation());
                }
            }
        }
        // Si hubo algun error
        @Override
        public void onFailure(@NonNull Exception exception) {
            RealtimeActivity activity = activityWeakReference.get();
            if (activity != null) {
                // Mandamos el error que sucedio
                Toast.makeText(activity, exception.getLocalizedMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Sobrecarga de los metodos del ciclo de vida para optimizar
    // el uso de los mapas de Mapbox
    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Al matar el activity dejamos de hacer peticiones al GPS
        if (locationEngine != null) {
            locationEngine.removeLocationUpdates(callback);
        }
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}

