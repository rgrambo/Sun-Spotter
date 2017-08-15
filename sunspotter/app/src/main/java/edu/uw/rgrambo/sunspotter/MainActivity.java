package edu.uw.rgrambo.sunspotter;

import android.app.Activity;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import static edu.uw.rgrambo.sunspotter.R.*;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(layout.activity_main);
    }

    public void onTheButtonClick(View view) {
        // Read the user's input.
        String input = ((EditText)findViewById(id.textInput)).getText().toString();

        // Grab the error text reference.
        TextView errorText = ((TextView)findViewById(id.textInputError));

        // Use regular expressions to check that the input is a Zipcode
        // and set the error text. Execute if it's a valid Zipcode.
        if (input.matches("\\d{5}?")) {
            errorText.setText("");
            new Request(this).execute(input);
        } else {
            errorText.setText(string.invalid_zip);
        }
    }

    public void PostExecute(ForecastData forecastData) {
        // Grab reference to the ListView
        ListView listView = (ListView)findViewById(id.listView);

        // Create the ListView's adapter
        ArrayAdapter arrayAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, forecastData.list);

        // Set the adapter (Changes the ListView)
        listView.setAdapter(arrayAdapter);

        // Grab references to the main text areas and image
        ImageView imageView = (ImageView)findViewById(id.mainImage);
        TextView upperMainText = (TextView)findViewById(id.upperMainText);
        TextView lowerMainText = (TextView)findViewById(id.lowerMainText);

        if (forecastData.firstSunnyDay == null) {
            imageView.setImageResource(drawable.sunsad);
            upperMainText.setText(string.no_sunshine);
            lowerMainText.setText(string.no_sunshine_lower);
        } else {
            imageView.setImageResource(drawable.sunhappy);
            upperMainText.setText(string.yes_sunshine);
            lowerMainText.setText(getString(string.yes_sunshine_lower, forecastData.firstSunnyDay));
        }
    }

    // Async class used to make requests to the openweathermap url.
    private class Request extends AsyncTask<String, Void, Void>
    {
        ForecastData forecastData;
        MainActivity activity;

        // Take an activity in order to call its PostExecute method
        Request(MainActivity activity) {
            this.activity = activity;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(String... s) {
            URL url = null;
            try {
                Uri uri = new Uri.Builder().scheme("http")
                        .authority("api.openweathermap.org")
                        .appendPath("data")
                        .appendPath("2.5")
                        .appendPath("forecast")
                        .appendQueryParameter("q", s[0])
                        .appendQueryParameter("appid", BuildConfig.OPEN_WEATHER_MAP_API_KEY)
                        .build();

                url = new URL(uri.toString());
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            if (url == null) {
                return null;
            }

            String result = null;
            try {
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()));
                StringBuilder builder = new StringBuilder();

                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }

                result = builder.toString();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (result == null) {
                return null;
            }

            // Read the JSON and get the list element
            JSONArray list = null;
            try {
                JSONObject obj = new JSONObject(result);

                list = obj.getJSONArray("list");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (list == null) {
                return null;
            }

            forecastData = new ForecastData();
            try {
                // Check each forecast in the list and take out the desired information
                for (int i = 0; i < list.length(); i++) {
                    JSONObject obj = list.getJSONObject(i);

                    // Create a boolean that is true if the sun will be out (false otherwise)
                    String clouds = obj.getJSONArray("weather").getJSONObject(0).getString("main");
                    boolean sun = clouds.equals("Clear");

                    // Declare the format of our date
                    SimpleDateFormat sdf = new SimpleDateFormat("EEE hh:mm");

                    // Add all of the data into the forecast data object.
                    // Have to multiply by 1000 to get milliseconds rather than seconds
                    // Have to convert the degrees from kelvin to fahrenheit
                    forecastData.AddToList(sdf.format(Long.parseLong(obj.getString("dt")) * 1000),
                            sun,
                            kelvinToFahrenheit(Double.parseDouble(
                                    obj.getJSONObject("main").getString("temp"))));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            activity.PostExecute(forecastData);
        }

        private double kelvinToFahrenheit (double K) {
            return ((K - 273.15) * 1.8) + 32;
        }
    }

    private class ForecastData {
        String firstSunnyDay = null;
        final List<String> list;

        private ForecastData() {
            this.list = new ArrayList<String>();
        }

        void AddToList(String date, boolean sun, double temp) {
            String sunString = "No Sun";
            if (sun) {
                if (firstSunnyDay == null) {
                    firstSunnyDay = date;
                }
                sunString = "SUN!";
            }

            list.add(date + ": " + sunString + " at " + String.format("%.2f", temp) + '\u00B0');
        }
    }
}
