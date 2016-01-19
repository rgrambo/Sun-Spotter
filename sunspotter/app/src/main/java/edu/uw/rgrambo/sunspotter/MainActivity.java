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

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onTheButtonClick(View view) {
        // Read the user's input.
        String input = ((EditText)findViewById(R.id.textInput)).getText().toString();

        // Grab the error text reference.
        TextView errorText = ((TextView)findViewById(R.id.textInputError));

        // Use regular expressions to check that the input is a Zipcode
        // and set the error text. Execute if it's a valid Zipcode.
        if (input.matches("\\d{5}?")) {
            errorText.setText("");
            new Request(this).execute(input);
        } else {
            errorText.setText(R.string.invalid_zip);
        }
    }

    public void PostExecute(ForecastData forecastData) {
        // Grab reference to the ListView
        ListView listView = (ListView)findViewById(R.id.listView);

        // Create the ListView's adapter
        ArrayAdapter arrayAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, forecastData.list);

        // Set the adapter (Changes the ListView)
        listView.setAdapter(arrayAdapter);

        // Grab references to the main text areas and image
        ImageView imageView = (ImageView)findViewById(R.id.mainImage);
        TextView upperMainText = (TextView)findViewById(R.id.upperMainText);
        TextView lowerMainText = (TextView)findViewById(R.id.lowerMainText);

        if (forecastData.firstSunnyDay == null) {
            imageView.setImageResource(R.drawable.sunsad);
            upperMainText.setText("No Sunshine! :(");
            lowerMainText.setText("There's no sun in the near future.");
        } else {
            imageView.setImageResource(R.drawable.sunhappy);
            upperMainText.setText("YAY! SUNSHINE! :)");
            lowerMainText.setText("It will be sunny on " + forecastData.firstSunnyDay);
        }
    }

    // Async class used to make requests to the openweathermap url.
    private class Request extends AsyncTask<String, Void, Void>
    {
        public ForecastData forecastData;
        public MainActivity activity;

        // Take an activity in order to call its PostExecute method
        public Request(MainActivity activity) {
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

            String result = null;
            try {
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()));
                StringBuffer buffer = new StringBuffer();

                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                }

                result = buffer.toString();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Read the JSON and get the list element
            JSONArray list = null;
            try {
                JSONObject obj = new JSONObject(result);

                list = obj.getJSONArray("list");
            } catch (JSONException e) {
                e.printStackTrace();
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

    public class ForecastData {
        public String firstSunnyDay = null;
        public final List<String> list;

        private ForecastData() {
            this.list = new ArrayList<String>();
        }

        public void AddToList(String date, boolean sun, double temp) {
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
