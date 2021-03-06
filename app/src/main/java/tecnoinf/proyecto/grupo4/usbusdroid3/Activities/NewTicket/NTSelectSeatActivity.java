package tecnoinf.proyecto.grupo4.usbusdroid3.Activities.NewTicket;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import tecnoinf.proyecto.grupo4.usbusdroid3.Helpers.RestCallAsync;
import tecnoinf.proyecto.grupo4.usbusdroid3.R;

public class NTSelectSeatActivity extends AppCompatActivity {

    private int selectedSeat = 0;
    private int lastSelectedPosition = -1;
    private int lastSelectedSeat = -1;
    private static ArrayList<Integer> occupied;
    private static ArrayList<Integer> booked;

    public class MyAdapter extends BaseAdapter {

        final int numberOfItems = nbrOfSeats + nbrOfSeats/4;
        private Bitmap[] bitmap = new Bitmap[numberOfItems];

        private Context context;
        private LayoutInflater layoutInflater;

        MyAdapter(Context c){
            context = c;
            layoutInflater = LayoutInflater.from(context);

            for(int i = 0; i < numberOfItems; i++){
                if((i + 3) % 5 == 0) {
                    bitmap[i] = BitmapFactory.decodeResource(context.getResources(), R.drawable.bus_aisle_dotted);
                } else {
                    bitmap[i] = BitmapFactory.decodeResource(context.getResources(), R.drawable.seat_black);
                }
            }
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            return (occupied == null || (((position + 3) % 5) != 0) && !occupied.contains(position));
        }

        @Override
        public int getCount() {
            return bitmap.length;
        }

        @Override
        public Object getItem(int position) {
            return bitmap[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Integer positionI = position;

            View grid;
            if(true){
                layoutInflater = getLayoutInflater();
                grid = layoutInflater.inflate(R.layout.gridview_seat, null);
            }else{
                grid = (View)convertView;
            }

            ImageView imageView = (ImageView)grid.findViewById(R.id.seatImage);
            imageView.setImageBitmap(bitmap[position]);
            TextView textView = (TextView)grid.findViewById(R.id.seatNumber);

            int seatNbr;
            if(positionI < 3) {
                seatNbr = (positionI + 1);
                grid.setId(seatNbr);
            } else {
                seatNbr = (positionI + 1) - (((positionI-2) / 5) + 1);
                grid.setId(seatNbr);
            }

            textView.setText(String.valueOf(seatNbr));

            if(occupied != null && !occupied.isEmpty() && occupied.indexOf(positionI) != -1) {
                imageView.setColorFilter(Color.RED);
                textView.setTextColor(Color.WHITE);
                grid.setEnabled(false);
                grid.setClickable(false);
            } else if (booked != null && !booked.isEmpty() && booked.indexOf(positionI) != -1) {
                imageView.setColorFilter(Color.BLUE);
                textView.setTextColor(Color.WHITE);
                grid.setEnabled(false);
                grid.setClickable(false);
            } else if(seatNbr == selectedSeat && positionIsEnabled(positionI)) {
                imageView.setColorFilter(Color.rgb(0, 100, 0));
                textView.setTextColor(Color.WHITE);
            } else {
                imageView.clearColorFilter();
                textView.setTextColor(Color.WHITE);
            }

            if(!positionIsEnabled(positionI) &&
                    !(occupied != null && !occupied.isEmpty() && occupied.indexOf(positionI) != -1) &&
                    !(booked != null && !booked.isEmpty() && booked.indexOf(positionI) != -1)) {
                textView.setTextColor(Color.TRANSPARENT);
            }
            return grid;
        }
    }

    GridView gridView;
    Button confirmButton;
    private String token;
    private Intent father;
    private JSONObject journeyJSON;
    private JSONArray bookingsJSONArray;
    public int standing;
    public int nbrOfSeats;
    private String ticketPriceRest;
    private String origin, destination;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_ntselect_seat);
            gridView = (GridView)findViewById(R.id.seatsGV);
            confirmButton = (Button) findViewById(R.id.confirmSeatBtn);
            father = getIntent();
            SharedPreferences sharedPreferences = getSharedPreferences("USBusData", Context.MODE_PRIVATE);
            token = sharedPreferences.getString("token", "");
            origin = father.getStringExtra("origin");
            destination = father.getStringExtra("destination");


            JSONArray occupiedJSONArray;
            journeyJSON = new JSONObject(father.getStringExtra("journey"));
            bookingsJSONArray = new JSONArray(father.getStringExtra("bookedSeats"));

            standing = journeyJSON.getJSONObject("bus").getInt("standingPassengers");
            nbrOfSeats = journeyJSON.getJSONObject("bus").getInt("seats");

            occupiedJSONArray = new JSONArray(father.getStringExtra("soldSeats"));
//            if (!journeyJSON.isNull("seatsState") && journeyJSON.getJSONArray("seatsState").length() > 0) {
//                occupiedJSONArray = journeyJSON.getJSONArray("seatsState");
//            } else {
//                occupiedJSONArray = new JSONArray();
//            }

            occupied = new ArrayList<>();
            Integer occupiedSeat;
            Integer occupiedPosition;
            for (int i = 0; i < occupiedJSONArray.length(); i++) {
                if (!occupiedJSONArray.getJSONObject(i).getBoolean("free")) {
                    occupiedSeat = occupiedJSONArray.getJSONObject(i).getInt("number");
                    occupiedPosition = seat2Position(occupiedSeat);
//                    System.out.println("Seat: "+occupiedSeat + "  Position: " + occupiedPosition);
//
//                    System.out.println("adding to occupied: " + occupiedPosition);
                    occupied.add(occupiedPosition);
                }
            }

            booked = new ArrayList<>();
            Integer bookedSeat;
            Integer bookedPosition;
            for (int k = 0; k < bookingsJSONArray.length(); k++) {
                bookedSeat = bookingsJSONArray.getJSONObject(k).getInt("number");
                bookedPosition = seat2Position(bookedSeat);
                booked.add(bookedPosition);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        MyAdapter adapter = new MyAdapter(this);
        gridView.setAdapter(adapter);

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(lastSelectedSeat != -1) {
                    lastSelectedSeat = selectedSeat;
                }
                if(positionIsEnabled(position)) {
                    selectedSeat = position2Seat(position);

                    ImageView selectedSeatImage = (ImageView) view.findViewById(R.id.seatImage);
                    if ((occupied == null || occupied.isEmpty() || (occupied != null && !occupied.isEmpty() && !occupied.contains(position))) &&
                            (booked == null || booked.isEmpty() || (booked != null && !booked.isEmpty() && !booked.contains(position)))) {
                        selectedSeatImage.setColorFilter(Color.rgb(0, 100, 0));
                    }

                    if (lastSelectedPosition > -1 &&
                            lastSelectedPosition != position &&
                            (occupied == null || occupied.isEmpty() || (!occupied.contains(lastSelectedPosition) && !occupied.contains(position))) &&
                            (booked == null || booked.isEmpty() || (!booked.contains(lastSelectedPosition) && !booked.contains(position)))) {
                        View lastView = parent.getChildAt(lastSelectedPosition - parent.getFirstVisiblePosition());
                        if (lastView != null) {
                            ImageView lastImage = (ImageView) lastView.findViewById(R.id.seatImage);
                            lastImage.clearColorFilter();
                        }
                    }

                    if ((occupied == null || occupied.isEmpty() || !occupied.contains(position)) &&
                            (booked == null || booked.isEmpty() || !booked.contains(position))) {
                        lastSelectedPosition = position;
                    }
                }
            }
        });

        assert confirmButton != null;
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                        if (selectedSeat > 0) {

                            if(father.getStringExtra("closeOption").equalsIgnoreCase("buy")) {
                                ticketPriceRest = getString(R.string.URLticketPrice,
                                        getString(R.string.URL_REST_API),
                                        getString(R.string.tenantId),
                                        journeyJSON.get("id").toString(),
                                        origin.replace(" ", "+"),
                                        destination.replace(" ", "+"));

                                AsyncTask<Void, Void, JSONObject> priceResult = new RestCallAsync(getApplicationContext(), ticketPriceRest, "GET", null, token).execute();
                                JSONObject priceData = priceResult.get();
                                Double ticketPriceDouble = new JSONObject(priceData.getString("data")).getDouble("price");
                                String ticketPrice = String.format("%.2f", ticketPriceDouble);

                                Intent confirmationIntent = new Intent(v.getContext(), NTConfirmationActivity.class);
                                //confirmationIntent.putExtra("token", token);
                                confirmationIntent.putExtra("journey", journeyJSON.toString());
                                confirmationIntent.putExtra("ticketPrice", ticketPrice);
                                confirmationIntent.putExtra("origin", origin);
                                confirmationIntent.putExtra("destination", destination);
                                confirmationIntent.putExtra("seat", selectedSeat);
                                confirmationIntent.putExtra("originKm", father.getDoubleExtra("originKm", 0.0));
                                confirmationIntent.putExtra("destinationKm", father.getDoubleExtra("destinationKm", 0.0));
                                startActivity(confirmationIntent);
                            } else if (father.getStringExtra("closeOption").equalsIgnoreCase("booking")) {

                                ticketPriceRest = getString(R.string.URLticketPrice,
                                        getString(R.string.URL_REST_API),
                                        getString(R.string.tenantId),
                                        journeyJSON.get("id").toString(),
                                        origin.replace(" ", "+"),
                                        destination.replace(" ", "+"));

                                AsyncTask<Void, Void, JSONObject> priceResult = new RestCallAsync(getApplicationContext(), ticketPriceRest, "GET", null, token).execute();
                                JSONObject priceData = priceResult.get();
                                Double ticketPriceDouble = new JSONObject(priceData.getString("data")).getDouble("price");
                                String ticketPrice = String.format("%.2f", ticketPriceDouble);

                                Intent confirmationIntent = new Intent(v.getContext(), NTBookingActivity.class);
                                //confirmationIntent.putExtra("token", token);
                                confirmationIntent.putExtra("journey", journeyJSON.toString());
                                confirmationIntent.putExtra("ticketPrice", ticketPrice);
                                confirmationIntent.putExtra("origin", origin);
                                confirmationIntent.putExtra("destination", destination);
                                confirmationIntent.putExtra("seat", selectedSeat);
                                confirmationIntent.putExtra("originKm", father.getDoubleExtra("originKm", 0.0));
                                confirmationIntent.putExtra("destinationKm", father.getDoubleExtra("destinationKm", 0.0));
                                startActivity(confirmationIntent);
                            }
    //                    Intent busStopSelectionIntent = new Intent(getBaseContext(), NTBusStopSelectionActivity.class);
    //                    busStopSelectionIntent.putExtra("seat", String.valueOf(selectedSeat));
    //                    busStopSelectionIntent.putExtra("journey", father.getStringExtra("journey"));
    //                    startActivity(busStopSelectionIntent);
                    } else {
                        Toast.makeText(getApplicationContext(), "Debe seleccionar un asiento", Toast.LENGTH_LONG).show();
                        //Nota: La compra de standing passenger solo se permite mediante app de Guarda (USBusDroid Trip)
                    }
                }catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public boolean positionIsEnabled(int position) {
        //System.out.println("positionIsEnabled position:"+position);
        Integer positionI = position;
        if(occupied != null && !occupied.isEmpty() && booked != null && !booked.isEmpty()) {
            return (((position+3) % 5) != 0) && (occupied.indexOf(positionI) == -1) && (booked.indexOf(positionI) == -1);
        } else if(occupied != null && !occupied.isEmpty()) {
            return (((position+3) % 5) != 0) && (occupied.indexOf(positionI) == -1);
        } else if(booked != null && !booked.isEmpty()) {
            return (((position+3) % 5) != 0) && (booked.indexOf(positionI) == -1);
        } else {
            Integer occupiedPosition = occupied == null? -1 : occupied.indexOf(positionI);
            Integer bookedPosition = booked == null? -1 : booked.indexOf(positionI);
            //System.out.println("ocupado: " + occupiedPosition);
            Boolean result;
            result = ((((positionI + 3) % 5) != 0) && occupiedPosition.intValue() == -1 && bookedPosition.intValue() == -1);
            //System.out.println("result: "+result);
            return result;
        }
    }

    private Integer position2Seat (Integer position) {
        Integer seat;
        if(position < 3) {
            seat = (position + 1);
        } else {
            seat = (position + 1) - (((position-2) / 5) + 1);
        }
        return seat;
    }

    private Integer seat2Position (Integer seat) {
        Integer position;
        if(seat < 3) {
            position = (seat - 1);
        } else if ((seat-2)%4 == 0) {
            position = (seat - 1) + ((seat-2) / 4);
        } else {
            position = (seat - 1) + (((seat-2) / 4) + 1);
        }
        return position;
    }
}
