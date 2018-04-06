package kz.proffix4.wrates;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;

import java.util.Arrays;
import android.widget.RemoteViews;


public class WeatherWidget extends AppWidgetProvider {

    final String LOG_TAG = "myLogs";
    public String temperature, time_temp_update;


    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        Log.d(LOG_TAG, "onEnabled");


    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        Log.d(LOG_TAG, "onUpdate " + Arrays.toString(appWidgetIds));

        // Проверяем есть ли соединения с сетью интернет
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

        if (activeNetworkInfo.isConnected() == true) {
            for (int id : appWidgetIds) {

                updateWidget(context);
            }
        }



    }

    /**
     * Обновление виджета
     * @param context
     */
    private void updateWidget(Context context) {

        WidgetData weather = new WidgetData(context);
        weather.execute("Pavlodar, KZ", "ru");


    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        Log.d(LOG_TAG, "onDeleted " + Arrays.toString(appWidgetIds));
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        Log.d(LOG_TAG, "onDisabled");
    }


    private class WidgetData extends AsyncTask<String, Void, Weather> {

        private Context context;
        PendingIntent pendingIntent;

        public WidgetData(Context context) {
            this.context = context;

        }


        // Тут реализуем фоновую асинхронную загрузку данных, требующих много времени
        @Override
        protected Weather doInBackground(String... params) {
            Log.d(LOG_TAG, "params[0] " + WeatherBuilder.buildWeather(params[0], params[1]).getDescription());
            return WeatherBuilder.buildWeather(params[0], params[1]);
        }
        // ----------------------------------------------------------------------------

        // Тут реализуем что нужно сделать после окончания загрузки данных
        @Override
        protected void onPostExecute(final Weather weather) {
            super.onPostExecute(weather);
            Log.d(LOG_TAG,"test");

            temperature = String.valueOf(weather.getTemperature());
            time_temp_update = String.valueOf(weather.getDt());
            Log.d(LOG_TAG, "temperature1 " + temperature);


            // Создаем Intent, чтобы запустить главное окно MainActivity
            Intent intent = new Intent(context, MainActivity.class);
            pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

            // Передаем данные в виджет
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget);
            ComponentName componentName = new ComponentName(context, WeatherWidget.class);

            // устанавливаем значение погоды в textView на виджете
            remoteViews.setTextViewText(R.id.temperature, temperature);
            // устанавливаем обработчик (по клику) на виджет
            remoteViews.setOnClickPendingIntent(R.id.main_window, pendingIntent);
            remoteViews.setTextViewText(R.id.time_temp_update, time_temp_update);
            remoteViews.setImageViewBitmap(R.id.weather_icon, weather.getIconData());


            // обновляем виджет
            appWidgetManager.updateAppWidget(componentName, remoteViews);


        }


    }




}

