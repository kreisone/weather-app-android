package kz.proffix4.wrates;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.GroundOverlay2;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;


public class MainActivity extends AppCompatActivity implements SensorEventListener {


    private static final String TAG = "data";
    private ImageView imageView; // Иконка погоды
    private TextView textView; // Компонент для данных погоды
    private TextView textView2; // Компонент для данных котировок

    private SensorManager mSensorManager;

    private String compassDegree = "";
    private int degree = 0;


    private MapView mapView;
    private CompassOverlay mCompassOverlay;
    private MyLocationNewOverlay mLocationOverlay;
    private Bitmap bitmapArrows;
    private GroundOverlay2 overlay;
    private Bitmap rotated;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // requesting permissions
        // запрашивание разрешений у пользователя
        int PERSMISSION_ALL = 1;
        String[] PERMISSIONS = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE
        };

        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERSMISSION_ALL);
        }



        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        imageView = (ImageView) findViewById(R.id.imageView);
        textView = (TextView) findViewById(R.id.textView);
        textView2 = (TextView) findViewById(R.id.textView2);

        JSONWeatherTask task = new JSONWeatherTask(); // Создание потока определения погоды
        task.execute(getString(R.string.Город), getString(R.string.Язык)); // Активация потока

        JSOUPRatesTask task2 = new JSOUPRatesTask(); // Создание потока определения котировок
        task2.execute(); // Активация потока







        // OSM

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        // Инициализация карты
        mapView = (MapView) this.findViewById(R.id.mapview);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);
        mapView.setTileSource(TileSourceFactory.MAPNIK);

        IMapController mapController = (MapController) this.mapView.getController();
        mapController.setZoom(11.0);
        mapController.setCenter(new GeoPoint(52.2561941,77.0422887));
        mapController.stopPanning();

        // Zooming
        final DisplayMetrics dm = this.getResources().getDisplayMetrics();
        RotationGestureOverlay mRotationGestureOverlay = new RotationGestureOverlay(mapView);
        mRotationGestureOverlay.setEnabled(true);
        mapView.getOverlays().add(mRotationGestureOverlay);

        // Zooming scale
        // Размерная шкала масштабирования
        ScaleBarOverlay mScaleBarOverlay = new ScaleBarOverlay(mapView);
        mScaleBarOverlay.setScaleBarOffset(0,(int)(40 * dm.density));
        mScaleBarOverlay.setCentred(true);
        mScaleBarOverlay.setScaleBarOffset(dm.widthPixels / 2, 10);
        mapView.getOverlays().add(mScaleBarOverlay);




        // Конвертируем Drawable в Bitmap
        // bitmapArrows - изображение стрелок ветра
        bitmapArrows = BitmapFactory.decodeResource(getResources(), R.drawable.arrows);
        // Инициализируем слой GroundOverlay2
        overlay = new GroundOverlay2();


        // PolyLines
//        List<GeoPoint> geoPoints = new ArrayList<>();
//        //add your points here
//        geoPoints.add(new GeoPoint(52.2561941,77.0422887));
//        geoPoints.add(new GeoPoint(52.3699894,76.9072292));
//        geoPoints.add(new GeoPoint(52.2180349,77.0921818));
//        geoPoints.add(new GeoPoint(52.3309441,76.8895858));
//        Polygon polygon = new Polygon();    //see note below
//        polygon.setFillColor(Color.argb(75, 255,0,0));
//        geoPoints.add(geoPoints.get(0));    //forces the loop to close
//        polygon.setPoints(geoPoints);
//        polygon.setTitle("A sample polygon");
//
//        //polygons supports holes too, points should be in a counter-clockwise order
//        List<List<GeoPoint>> holes = new ArrayList<>();
//        holes.add(geoPoints);
//        polygon.setHoles(holes);
//
//        mapView.getOverlayManager().add(polygon);





        // Location
        this.mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this),mapView);
        this.mLocationOverlay.enableMyLocation();
        mapView.getOverlays().add(this.mLocationOverlay);

        // Compass
        this.mCompassOverlay = new CompassOverlay(this, new InternalCompassOrientationProvider(this), mapView);
        this.mCompassOverlay.setPointerMode(true);
        this.mCompassOverlay.enableCompass();
        mapView.getOverlays().add(this.mCompassOverlay);

        // Markers
        Marker startMarker = new Marker(mapView);
        startMarker.setPosition(new GeoPoint(52.2561941,77.0422887));
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        startMarker.setTitle("Алюминиевый завод");
        mapView.getOverlays().add(startMarker);


//        Marker startMarker1 = new Marker(mapView);
//        startMarker1.setPosition(new GeoPoint(52.2180349,77.0921818));
//        startMarker1.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
//        mapView.getOverlays().add(startMarker1);

        Marker startMarker2 = new Marker(mapView);
        startMarker2.setPosition(new GeoPoint(52.3699894,76.9072292));
        startMarker2.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        startMarker2.setTitle("ПНХЗ");
        mapView.getOverlays().add(startMarker2);


//        Marker startMarker3 = new Marker(mapView);
//        startMarker3.setPosition(new GeoPoint(52.3309441,76.8895858));
//        startMarker3.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
//        mapView.getOverlays().add(startMarker3);
//
//
//        Marker startMarker4 = new Marker(mapView);
//        startMarker4.setPosition(new GeoPoint(52.257447,77.0870702));
//        startMarker4.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
//        mapView.getOverlays().add(startMarker4);




    }

    // check permissions
    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }


    // Кнопка "Обновить"
    public void onClick(View view) {
        new JSONWeatherTask().execute(getString(R.string.Город), getString(R.string.Язык)); // Создание и активация потока определения погоды
        new JSOUPRatesTask().execute(); // Создание и активация потока определения котировок
        Toast.makeText(getApplicationContext(), R.string.обновление, Toast.LENGTH_SHORT).show();
    }






    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    // Обработчик меню
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.newCheck) {
            Intent i = new Intent(android.content.Intent.ACTION_VIEW);
            i.setData(Uri.parse("https://play.google.com/store/apps/details?id=kz.proffix4.wrates"));
            startActivity(i);
            return true;
        }


        if (id == R.id.about) {
            Toast toast = Toast.makeText(getApplicationContext(), R.string.email, Toast.LENGTH_LONG);
            toast.show();
            return true;
        }

        if (id == R.id.exit) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        degree = Math.round(event.values[0]);
        String degreeStr = Weather.getWindDirectionCode(degree);

        if (!degreeStr.equals(compassDegree)) {
            compassDegree = degreeStr;
            new JSONWeatherTask().execute(getString(R.string.Город), getString(R.string.Язык));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onResume() {
        super.onResume();

        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onPause() {
        super.onPause();

        mSensorManager.unregisterListener(this);
    }

    // Класс отдельного асинхронного потока
    private class JSONWeatherTask extends AsyncTask<String, Void, Weather> {

        // Тут реализуем фоновую асинхронную загрузку данных, требующих много времени
        @Override
        protected Weather doInBackground(String... params) {
            return WeatherBuilder.buildWeather(params[0], params[1]);
        }
        // ----------------------------------------------------------------------------

        // Тут реализуем что нужно сделать после окончания загрузки данных
        @Override
        protected void onPostExecute(final Weather weather) {
            super.onPostExecute(weather);



//            mapView = (MapView) findViewById(R.id.mapview);
            Matrix matrix = new Matrix();
            matrix.postRotate((float) weather.getWind_deg());

            rotated = Bitmap.createBitmap(bitmapArrows, 0, 0, bitmapArrows.getWidth(), bitmapArrows.getHeight(), matrix, true);


            overlay.setTransparency(0.2f);
            overlay.setImage(rotated);
            overlay.setPosition(new GeoPoint(52.3699894,76.9072292),
                                new GeoPoint( 52.2561941,77.0422887));
//            overlay.setBearing((float) weather.getWind_deg());

            Log.d(TAG, "overlay: " + overlay.getBearing());

            mapView.getOverlayManager().add(overlay);





            // Устанавливаем картинку погоды
            imageView.post(new Runnable() { //  Используем синхронизацию с UI
                @Override
                public void run() {
                    // Если есть считанная иконка с web
                    if (weather.getIconData() != null) {
                        imageView.setImageBitmap(weather.getIconData()); // Установка иконки
                    } else {
                        imageView.setImageResource(R.mipmap.ic_launcher); // Установка своей иконки при ошибке
                    }
                    imageView.invalidate(); // Принудительная прорисовка картинки на всякий случай
                }
            });

            // Выдаем данные о погоде в компонент
            textView.post(new Runnable() { //  с использованием синхронизации UI
                @Override
                public void run() {
                    textView.setText("");
                    if (weather.getCity() != null) {
                        textView.append(weather.getDt() + "\n");
                        textView.append(String.valueOf(weather.getTemperature()) + " \u2103, " +
                                weather.getDescription() + "\n");
                        textView.append(getString(R.string.ветер) + " " + weather.getWind_speed() + " " + getString(R.string.мс) + "\n");
                        textView.append(getString(R.string.напр) + " " + weather.getWind_deg() + " \u00b0" + " " +
                                Weather.getWindDirectionCode(weather.getWind_deg()));
                        if (!compassDegree.equals("")) {
                            textView.append(", " + getString(R.string.вы) + ": " +
                                    degree + " \u00b0" + " " + compassDegree);
                        }
                    } else {
                        textView.append(getString(R.string.Нет_данных_о_погоде) + "\n");
                        textView.append(getString(R.string.Проверьте_доступность_интернета));
                    }
                }
            });

        }
        // ------------------------------------------------------------------------------------

    }

    // Класс отдельного асинхронного потока
    private class JSOUPRatesTask extends AsyncTask<String, Void, String> {

        // Тут реализуем фоновую асинхронную загрузку данных, требующих много времени
        @Override
        protected String doInBackground(String... params) {
            return RatesReader.getRatesData(); // Получаем данные котировок
        }
        // ----------------------------------------------------------------------------

        // Тут реализуем что нужно сделать после окончания загрузки данных
        @Override
        protected void onPostExecute(final String rates) {
            super.onPostExecute(rates);

            // Выдаем данные о котировках в компонент
            textView.post(new Runnable() { //  с использованием синхронизации UI
                @Override
                public void run() {
                    if (rates != null) {
                        textView2.setText(rates);
                    } else {
                        textView2.setText("");
                        textView2.append(getString(R.string.Нет_данных_о_котировках) + "\n");
                        textView2.append(getString(R.string.Проверьте_доступность_интернета));
                    }
                }
            });

        }
        // ------------------------------------------------------------------------------------

    }





}
