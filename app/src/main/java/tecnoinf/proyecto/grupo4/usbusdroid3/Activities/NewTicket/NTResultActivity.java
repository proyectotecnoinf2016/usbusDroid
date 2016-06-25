package tecnoinf.proyecto.grupo4.usbusdroid3.Activities.NewTicket;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;

import tecnoinf.proyecto.grupo4.usbusdroid3.Activities.MainClient;
import tecnoinf.proyecto.grupo4.usbusdroid3.Helpers.RestCallAsync;
import tecnoinf.proyecto.grupo4.usbusdroid3.Models.TicketStatus;
import tecnoinf.proyecto.grupo4.usbusdroid3.R;

public class NTResultActivity extends AppCompatActivity {

    private String token;
    private String updateTicketRest;
    private String username;
    private JSONObject tempTicket;
    private JSONObject updatedTicket;
    private JSONObject journey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ntresult);


        Intent father = getIntent();
        //token = father.getStringExtra("token");
        SharedPreferences sharedPreferences = getSharedPreferences("USBusData", Context.MODE_PRIVATE);
        username = sharedPreferences.getString("username", "");
        token = sharedPreferences.getString("token", "");

        try {
            tempTicket = new JSONObject(father.getStringExtra("ticket"));

            JSONObject paymentDetails = new JSONObject(father.getStringExtra("PaymentDetails"));

            System.out.println("8181818181818 paymentDetails: "+paymentDetails);

            //TODO: if response.state == approved
            updatedTicket = new JSONObject();
            updatedTicket.put("tenantId", getString(R.string.tenantId));
            updatedTicket.put("id", tempTicket.get("id"));
            updatedTicket.put("paymentToken", paymentDetails.getJSONObject("response").get("id"));
            updatedTicket.put("username", username);
            updatedTicket.put("status", TicketStatus.CONFIRMED);

            System.out.println("lololololololo  updatedTicket armado: " + updatedTicket);

            updateTicketRest = getString(R.string.URLbuyTicket, getString(R.string.URL_REST_API), getString(R.string.tenantId)) + "/" + tempTicket.get("id").toString();
            AsyncTask<Void, Void, JSONObject> updTicketResult = new RestCallAsync(getApplicationContext(), updateTicketRest, "PUT", updatedTicket, token).execute();
            JSONObject updTicketData = updTicketResult.get();
            System.out.println("_=_=_=_=_=_=_=_= updTicketData: "+updTicketData);
            //Displaying payment details
            showDetails(paymentDetails.getJSONObject("response"), father.getStringExtra("PaymentAmount"));
        } catch (JSONException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showDetails(JSONObject jsonDetails, String paymentAmount) throws JSONException {
        //Views
        TextView textViewId = (TextView) findViewById(R.id.paymentId);
        TextView textViewStatus= (TextView) findViewById(R.id.paymentStatus);
        TextView textViewAmount = (TextView) findViewById(R.id.paymentAmount);

        //Showing the details from json object
        textViewId.setText(jsonDetails.getString("id"));
        textViewStatus.setText(jsonDetails.getString("state"));
        textViewAmount.setText("USD " + paymentAmount);
    }

    @Override
    public void onBackPressed() {
        Intent homeIntent = new Intent(this, MainClient.class);
        //homeIntent.putExtra("token", token);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(homeIntent);
    }
}
