package tecnoinf.proyecto.grupo4.usbusdroid3.Activities.TimeTable;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import tecnoinf.proyecto.grupo4.usbusdroid3.Activities.NewTicket.NTJourneyListActivity;
import tecnoinf.proyecto.grupo4.usbusdroid3.Helpers.DayConverter_ES;
import tecnoinf.proyecto.grupo4.usbusdroid3.Helpers.RestCallAsync;
import tecnoinf.proyecto.grupo4.usbusdroid3.Models.BusStop;
import tecnoinf.proyecto.grupo4.usbusdroid3.R;

public class TimeTable extends AppCompatActivity {

    private static String servicesFromToRest;
    private String dayOfWeek;
    private String origin;
    private String destination;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time_table);
        final Intent father = getIntent();
        SharedPreferences sharedPreferences = getSharedPreferences("USBusData", Context.MODE_PRIVATE);
        final String token = sharedPreferences.getString("token", "");
        //final String token = father.getStringExtra("token");

        try {
            JSONObject intentData = new JSONObject(father.getStringExtra("data"));
            JSONArray busStops = new JSONArray(intentData.get("data").toString().replace("\\", ""));

            List<BusStop> busStopList = BusStop.fromJson(busStops);
            ArrayList<String> busStopsNames = new ArrayList<>();
            for (BusStop bs: busStopList) {
                busStopsNames.add(bs.getName());
            }

            String dayNames[] = {"LUNES", "MARTES", "MIÉRCOLES", "JUEVES", "VIERNES", "SÁBADO", "DOMINGO"};

            final Spinner spinnerDay = (Spinner) findViewById(R.id.spnDOW);
            ArrayAdapter<String> dayAdapter = new ArrayAdapter<>(this, R.layout.simple_usbus_spinner_item, dayNames);
            assert spinnerDay != null;
            spinnerDay.setAdapter(dayAdapter);

            final Spinner spinnerFrom = (Spinner) findViewById(R.id.spnFrom);
            ArrayAdapter<String> fromAdapter = new ArrayAdapter<>(this, R.layout.simple_usbus_spinner_item, busStopsNames);
            assert spinnerFrom != null;
            spinnerFrom.setAdapter(fromAdapter);

            final Spinner spinnerTo = (Spinner) findViewById(R.id.spnTo);
            ArrayAdapter<String> toAdapter = new ArrayAdapter<>(this, R.layout.simple_usbus_spinner_item, busStopsNames);
            assert spinnerTo != null;
            spinnerTo.setAdapter(toAdapter);

            Button submitBtn = (Button) findViewById(R.id.btnSearch);
            assert submitBtn != null;
            submitBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        dayOfWeek = spinnerDay.getSelectedItem().toString();
                        origin = spinnerFrom.getSelectedItem().toString();
                        destination = spinnerTo.getSelectedItem().toString();
                        servicesFromToRest = getString(R.string.URLServicesFromTo,
                                getString(R.string.URL_REST_API),
                                getString(R.string.tenantId),
                                DayConverter_ES.convertEN(dayOfWeek),
                                origin,
                                destination);

                        AsyncTask<Void, Void, JSONObject> servicesResult = new RestCallAsync(getApplicationContext(), servicesFromToRest, "GET", null, token).execute();
                        JSONObject servicesData = servicesResult.get();

                        Intent listServicesFromToIntent = new Intent(v.getContext(), TTServicesListActivity.class);
                        listServicesFromToIntent.putExtra("data", servicesData.toString());
                        startActivity(listServicesFromToIntent);
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
}
