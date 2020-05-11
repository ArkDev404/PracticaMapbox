package com.ray.mapy;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.MapboxDirections;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonOptions;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.mapboxsdk.utils.BitmapUtils;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

import static android.graphics.Color.parseColor;
import static com.mapbox.core.constants.Constants.PRECISION_6;
import static com.mapbox.mapboxsdk.style.expressions.Expression.color;
import static com.mapbox.mapboxsdk.style.expressions.Expression.get;
import static com.mapbox.mapboxsdk.style.expressions.Expression.interpolate;
import static com.mapbox.mapboxsdk.style.expressions.Expression.lineProgress;
import static com.mapbox.mapboxsdk.style.expressions.Expression.linear;
import static com.mapbox.mapboxsdk.style.expressions.Expression.literal;
import static com.mapbox.mapboxsdk.style.expressions.Expression.match;
import static com.mapbox.mapboxsdk.style.expressions.Expression.stop;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconOffset;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineCap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineGradient;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineJoin;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth;

public class EjemploRura extends AppCompatActivity
        implements MapboxMap.OnMapClickListener, MapboxMap.OnMapLongClickListener {

    // Variables que nos sirven como etiquetas para llevar un control de la ruta
    private static final String ORIGIN_ICON_ID = "origin-icon-id";
    private static final String DESTINATION_ICON_ID = "destination-icon-id";
    private static final String ROUTE_LAYER_ID = "route-layer-id";
    private static final String ROUTE_LINE_SOURCE_ID = "route-source-id";
    private static final String ICON_LAYER_ID = "icon-layer-id";
    private static final String ICON_SOURCE_ID = "icon-source-id";

    // Preparamos las variables importantes
    private MapView mapView;
    private MapboxMap mapboxMap;
    private DirectionsRoute currentRoute;
    private MapboxDirections client;
    private FloatingActionButton fab;

    // Variables que nos permiten establecer parametros como Puntos iniciales, color de la linea
    // que traza la ruta y grosor
    private static Point ORIGIN_POINT = Point.fromLngLat(-100.424445, 23.711179);
    private static Point DESTINATION_POINT = Point.fromLngLat(-100.6314134, 23.6683872);
    private static final float LINE_WIDTH = 2f;
    private static final String ORIGIN_COLOR = "#2096F3";
    private static final String DESTINATION_COLOR = "#F84D4D";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Preparamos Mapbox agregando el Token del SDK
        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_ejemplo_rura);

        // Referenciamoos el FAB de esta vista
        fab = findViewById(R.id.fab_help);

        // Le agregamos un evento clic
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // A hacer clic desplegamos un cuadro de dialogo con las instrucciones para
                // trazar las rutas
                AlertDialog.Builder builder = new AlertDialog.Builder(EjemploRura.this)
                        .setTitle("Ayuda")
                        .setMessage(R.string.textoAyuda)
                        .setPositiveButton("Aceptar", null);
                builder.show();
            }
        });

        Toast.makeText(getApplicationContext(),
                "Espere a que cargue la ruta por defecto",
                Toast.LENGTH_LONG).show();

        // Preparamos el MapView y lo refeenciamos a esta vista
        mapView = findViewById(R.id.mapView2);
        mapView.onCreate(savedInstanceState);
        // Sobrecarga del metodo on MapReady que nos permite establecer los valores al cargar el mapa
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull MapboxMap mapboxMap) {
                // Referenciamos el mapa
                EjemploRura.this.mapboxMap = mapboxMap;
                // Le agregamos un estilo en este caso de las calles del mundo
                mapboxMap.setStyle(new Style.Builder().fromUri(Style.MAPBOX_STREETS)
                        // Creamos un icono para el marcador del Origen
                        .withImage(ORIGIN_ICON_ID, BitmapUtils.getBitmapFromDrawable(
                                getResources().getDrawable(R.drawable.blue_marker)))
                        // Creamos un icono para el marcador del Origen
                        .withImage(DESTINATION_ICON_ID, BitmapUtils.getBitmapFromDrawable(
                                getResources().getDrawable(R.drawable.red_marker))), new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {
                        // Metodos que nos serviran para establecer los puntos y poder trazar la ruta
                        // mas optima con Retrofit, servicio que hace consulta a la API de Rutas de
                        // Mapbox
                        initSources(style);
                        initLayers(style);
                        // Cargamos la ruta desde el origen al destino
                        getRoute(mapboxMap, ORIGIN_POINT, DESTINATION_POINT);
                        // Agregamos un evento clic y un evento de clic largo al mapa
                        // nos serviran mas adelante
                        mapboxMap.addOnMapClickListener(EjemploRura.this);
                        mapboxMap.addOnMapLongClickListener(EjemploRura.this);
                    }
                });
            }
        });
    }

    //Este metodo nos permite agregar la ruta y las marcas en el mapa
    private void initSources(@NonNull Style loadedMapStyle) {
        loadedMapStyle.addSource(new GeoJsonSource(ROUTE_LINE_SOURCE_ID, new GeoJsonOptions().withLineMetrics(true)));
        loadedMapStyle.addSource(new GeoJsonSource(ICON_SOURCE_ID, getOriginAndDestinationFeatureCollection()));
    }

    //Este metodo nos permite consultar las modificacion de los puntos origen y destino
    private FeatureCollection getOriginAndDestinationFeatureCollection() {
        Feature originFeature = Feature.fromGeometry(ORIGIN_POINT);
        originFeature.addStringProperty("originDestination", "origin");
        Feature destinationFeature = Feature.fromGeometry(DESTINATION_POINT);
        destinationFeature.addStringProperty("originDestination", "destination");
        return FeatureCollection.fromFeatures(new Feature[] {originFeature, destinationFeature});
    }

// Este metodo prepara los marcadores en el mapa ademas de preparar las lineas
    private void initLayers(@NonNull Style loadedMapStyle) {
//Preparamos el marcador para establecerlo en el mapa y trazar las lineas de ruta
        loadedMapStyle.addLayer(new LineLayer(ROUTE_LAYER_ID, ROUTE_LINE_SOURCE_ID).withProperties(
                lineCap(Property.LINE_CAP_ROUND),
                lineJoin(Property.LINE_JOIN_ROUND),
                lineWidth(LINE_WIDTH),
                lineGradient(interpolate(
                        linear(), lineProgress(),
                        // Establecemos los colores a la linea, en este caso un gradiente
                        stop(0f, color(parseColor(ORIGIN_COLOR))),
                        stop(1f, color(parseColor(DESTINATION_COLOR)))
                ))));

// Especificamos donde comienza y donde termina la linea de trazado de la ruta mas optima
        loadedMapStyle.addLayer(new SymbolLayer(ICON_LAYER_ID, ICON_SOURCE_ID).withProperties(
                iconImage(match(get("originDestination"), literal("origin"),
                        stop("origin", ORIGIN_ICON_ID),
                        stop("destination", DESTINATION_ICON_ID))),
                iconIgnorePlacement(true),
                iconAllowOverlap(true),
                iconOffset(new Float[] {0f, -4f})));
    }

// Este metodo es el metodo que nos permite consultar la ruta mas optima a traves de Retrofit2
    // para poder realizar el calculo de la ruta mas optima desde el origen al destino
    private void getRoute(MapboxMap mapboxMap, Point origin, Point destination) {
        client = MapboxDirections.builder()
                .origin(origin)
                .destination(destination)
                .overview(DirectionsCriteria.OVERVIEW_FULL)
                .profile(DirectionsCriteria.PROFILE_WALKING)
                .accessToken(getString(R.string.access_token))
                .build();
        client.enqueueCall(new Callback<DirectionsResponse>() {
            // Este metodo nos permite hacer la peticion HTTP con retrofit y comprobar nuestra conexion
            // a internet y nos devuelve la respuesta, dependiendo si hay conexion o no, nos dara
            // una respuesta, si hay conexion, se trazara la ruta y si no hay, nos marcara un error
            // de que no encontramos la ruta
            @Override
            public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                Timber.d("Response code: %s", response.code());

                if (response.body() == null) {
                    Timber.e("No routes found, make sure you set the right user and access token.");
                    return;
                } else if (response.body().routes().size() < 1) {
                    Timber.e("No routes found");
                    return;
                }

                // Obtenemos las direcciones de la API Direcciones de Mapbox
                currentRoute = response.body().routes().get(0);

                // Si la peticion no nos devuelve nulo
                if (currentRoute != null) {
                    if (mapboxMap != null) {
                        mapboxMap.getStyle(new Style.OnStyleLoaded() {
                            @Override
                            public void onStyleLoaded(@NonNull Style style) {

                                // Trazamos la linea del GeoJson
                                GeoJsonSource originDestinationPointGeoJsonSource = style.getSourceAs(ICON_SOURCE_ID);

                                if (originDestinationPointGeoJsonSource != null) {
                                    originDestinationPointGeoJsonSource.setGeoJson(getOriginAndDestinationFeatureCollection());
                                }

                                GeoJsonSource lineLayerRouteGeoJsonSource = style.getSourceAs(ROUTE_LINE_SOURCE_ID);

                                if (lineLayerRouteGeoJsonSource != null) {
                                    LineString lineString = LineString.fromPolyline(currentRoute.geometry(), PRECISION_6);
                                    lineLayerRouteGeoJsonSource.setGeoJson(Feature.fromGeometry(lineString));
                                }
                            }
                        });
                    }
                } else {
                    // Si no hay conexion desplegamos que no se pudo encontrar la ruta
                    Timber.d("Directions route is null");
                    Toast.makeText(EjemploRura.this,
                            getString(R.string.route_can_not_be_displayed), Toast.LENGTH_SHORT).show();
                }
            }
            // este metodo nos permite devolver ek error que nos haya devuelto la peticion HTTP
            @Override
            public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
                Toast.makeText(EjemploRura.this,
                        getString(R.string.route_call_failure), Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    public boolean onMapClick(@NonNull LatLng mapClickPoint) {

        Toast.makeText(getApplicationContext(),
                "El punto origen seleccionado es:\n" +
                        "Latitud: "+mapClickPoint.getLatitude()+"\n"+
                        "Longitud: "+mapClickPoint.getLongitude(),
                Toast.LENGTH_SHORT).show();

        //Al hacer clic en un punto especifico del mapa, se tomaran sus coordenadas y se estableceran
        // como nuevo punto origen
        ORIGIN_POINT = Point.fromLngLat(mapClickPoint.getLongitude(), mapClickPoint.getLatitude());
        // Trazamos la ruta desde el punto origen al destino
        getRoute(mapboxMap, ORIGIN_POINT, DESTINATION_POINT);
        return true;
    }

    @Override
    public boolean onMapLongClick(@NonNull LatLng mapClickPoint) {
        Toast.makeText(getApplicationContext(),
                "El punto destino seleccionado es:\n" +
                        "Latitud: "+mapClickPoint.getLatitude()+"\n"+
                        "Longitud: "+mapClickPoint.getLongitude(),
                Toast.LENGTH_SHORT).show();
        //Este metodo es similar solo que aqui al mantener un clic largo establecemos el punto destino
        DESTINATION_POINT = Point.fromLngLat(mapClickPoint.getLongitude(), mapClickPoint.getLatitude());

        // Hacemos la peticion y trazamos la ruta desde el origen al destino
        getRoute(mapboxMap, ORIGIN_POINT, DESTINATION_POINT);
        return true;
    }

    // Sobrecarga de metodos del ciclo de vida para hacer mas optima la app
    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Al destruir el activity  cancelamos las peticiones
        if (client != null) {
            client.cancelCall();
        }
        // Y eliminamos lo que se haya hecho con clos eventos clic
        if (mapboxMap != null) {
            mapboxMap.removeOnMapClickListener(this);
            mapboxMap.removeOnMapLongClickListener(this);
        }
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }


}