package wehack.viacs.com.wehack;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends ActionBarActivity {

    static Handler bluetoothIn;
    final int handlerState = 0;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    InputStream mmInputStream;
    BluetoothDevice mmDevice;

    boolean foundDevice;

    Switch mySwitch;

    SharedPreferences sharedpreferences;
    SharedPreferences.Editor editor;
    public static final String MyPREFERENCES = "MyPrefs";
    public static final String Phone = "phoneKey", Name = "nameKey", StringAll = "allKey";
    StringBuilder allContactNames, sbPhone, sbName, recDataString;

    String phone, msg;
    // Declaring a Location Manager
    protected LocationManager locationManager;

    String help_msg = "I met with an accident at ";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        allContactNames = new StringBuilder(500);
        // `Declaring List Variable`
        sbPhone = new StringBuilder();
        sbName = new StringBuilder();
        recDataString = new StringBuilder();
        locationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)==false)
        {startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));}


        sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        if (sharedpreferences.contains(Phone)) {
            Log.d("Phone", sharedpreferences.getString(Phone, ""));
            sbPhone.append(sharedpreferences.getString(Phone, ""));
        }
        if (sharedpreferences.contains(Name)) {

            Log.d("Name", sharedpreferences.getString(Name, ""));
            sbName.append(sharedpreferences.getString(Name, ""));
        }
        if (sharedpreferences.contains(StringAll)) {

            Log.d("StringAll", sharedpreferences.getString(StringAll, ""));
        }


        bluetoothIn = new Handler() {
            public void handleMessage(android.os.Message hlp_msg) {
                if (hlp_msg.what == handlerState) {                                     //if message is what we want
                    try {
                        help();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        mySwitch = (Switch)

                findViewById(R.id.mySwitch);

        //set the switch to OFF
        mySwitch.setChecked(false);
        //attach a listener to check for changes in state
        mySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()

                                            {

                                                @Override
                                                public void onCheckedChanged(CompoundButton buttonView,
                                                                             boolean isChecked) {

                                                    if (isChecked) {
                                                            startCarSystem();

                                                        mySwitch.setText("Exit C.A.R.S.");
                                                    } else {
                                                        stopCarSystem();
                                                        mySwitch.setText("Enter C.A.R.S.");
                                                    }

                                                }
                                            }

        );


        Button choose = (Button) findViewById(R.id.contacts);
        choose.setOnClickListener(new View.OnClickListener()

                                  {
                                      @Override
                                      public void onClick(View v) {


               /* allContactNames.delete(0, allContactNames.length());

                for (int i = 0; i < listName.size(); i++) {

                    allContactNames.append(listName.get(i) + "\n");
                }*/


                                          AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);

                                          // set title
                                          alertDialogBuilder.setTitle("Emergency Contacts");

                                          if (sharedpreferences.contains(Name)) {

                                              msg = sharedpreferences.getString(Name, "");
                                          }

                                          // set dialog message
                                          alertDialogBuilder
                                                  .setMessage(msg)
                                                  .setCancelable(true)
                                                  .setPositiveButton("Add More", new DialogInterface.OnClickListener() {
                                                      public void onClick(DialogInterface dialog, int id) {
                                                          //Start activity to get contact
                                                          final Uri uriContact = ContactsContract.Contacts.CONTENT_URI;
                                                          Intent intentPickContact = new Intent(Intent.ACTION_PICK, uriContact);
                                                          startActivityForResult(intentPickContact, 1);
                                                      }
                                                  });

                                          // create alert dialog
                                          AlertDialog alertDialog = alertDialogBuilder.create();

                                          // show it
                                          alertDialog.show();
                                      }
                                  }

        );

    }


    private void stopCarSystem() {
        if (mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.disable();
            Toast.makeText(MainActivity.this, "Exiting Car hacko-system", Toast.LENGTH_LONG).show();
        }
    }

    private void startCarSystem() {

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) {
            connectArduino();
        } else if (resultCode == RESULT_OK) {
            if (requestCode == 1) {
                Uri returnUri = data.getData();
                Cursor cursor = getContentResolver().query(returnUri, null, null, null, null);

                if (cursor.moveToNext()) {
                    int columnIndex_ID = cursor.getColumnIndex(ContactsContract.Contacts._ID);
                    String contactID = cursor.getString(columnIndex_ID);

                    int columnIndex_HASPHONENUMBER = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER);
                    String stringHasPhoneNumber = cursor.getString(columnIndex_HASPHONENUMBER);

                    if (stringHasPhoneNumber.equalsIgnoreCase("1")) {
                        Cursor cursorNum = getContentResolver().query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=" + contactID,
                                null,
                                null);

                        //Get the first phone number
                        if (cursorNum.moveToNext()) {

                            int columnIndex_number = cursorNum.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                            String stringNumber = cursorNum.getString(columnIndex_number);
                            int columnIndex_name = cursorNum.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                            String stringName = cursorNum.getString(columnIndex_name);
                            editor = sharedpreferences.edit();
                            //editor.putString(Phone, stringNumber);

                            sbPhone.append(stringNumber);
                            sbPhone.append(";");
                            sbName.append(stringName).append("\n");
                            editor.putString(Phone, sbPhone.toString());
                            editor.putString(Name, sbName.toString());
                            editor.apply();
                            Toast.makeText(getApplicationContext(), "Added " + stringName + " to list", Toast.LENGTH_LONG).show();
                        }

                    } else {
                        Toast.makeText(getApplicationContext(), "No Phone Number!", Toast.LENGTH_LONG).show();
                    }


                } else {
                    Toast.makeText(getApplicationContext(), "No data!", Toast.LENGTH_LONG).show();
                }
            } else {
                Log.d("TAG", "OnActivityResult Error");
            }

        }
    }

    private void connectArduino() {

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().equals("HC-05")) {
                    mmDevice = device;
                    foundDevice = true;
                    break;
                } else {
                    foundDevice = false;
                }
            }
            if (foundDevice == true) {
                Toast.makeText(MainActivity.this, "Entering Car hacko-system", Toast.LENGTH_LONG).show();

                UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard SerialPortService ID
                try {
                    mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);

                    try {
                        mmSocket.connect();
                        Log.d("TAG", "bluetooth socket connected");
                        try {
                            mmInputStream = mmSocket.getInputStream();
                            Log.d("TAG", "getInputStream succeeded");
                        } catch (IOException e_getin) {
                            Log.d("TAG", "getInputStream failed", e_getin);
                        }
                    } catch (IOException e_connect) {
                        e_connect.printStackTrace();
                    }
                    // mmOutputStream = mmSocket.getOutputStream();
                } catch (IOException ecreate) {
                    Log.i("TAG", "create socket failed", ecreate);
                }

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            help();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }, 5000);

            } else {
                Toast.makeText(MainActivity.this, "Please Connect to HC-05 (Password is 1234)", Toast.LENGTH_LONG).show();
            }

        } else {
            Toast.makeText(MainActivity.this, "No paired Devices", Toast.LENGTH_LONG).show();
            mySwitch.setChecked(false);
        }


    }

    private void help() throws IOException {

        //this string contains location

        //Lat and Lan of person
        GPSTracker gps = new GPSTracker(MainActivity.this);
        double latitude = gps.getLatitude();
        double longitude = gps.getLongitude();
        // Toast.makeText(getApplicationContext(), "Your Location is - \nLat: " + latitude + "\nLong: " + longitude, Toast.LENGTH_LONG).show();
        //My Code
        /*LocationAddress locationAddress = new LocationAddress();
        locationAddress.getAddressFromLocation(latitude, longitude,
                getApplicationContext(), new GeocoderHandler());
        Toast.makeText(getApplicationContext(), "Your address: " + locAddress, Toast.LENGTH_LONG).show();*/
        Geocoder geocoder;
        List<Address> addresses;
        geocoder = new Geocoder(this, Locale.getDefault());
        addresses = geocoder.getFromLocation(latitude, longitude, 1);

        String address = addresses.get(0).getAddressLine(0);
        String city = addresses.get(0).getAddressLine(1);
        String country = addresses.get(0).getAddressLine(2);

        // Toast.makeText(getApplicationContext(), "Your address: " + address + city + country, Toast.LENGTH_LONG).show();

        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(sbPhone.toString(), null, help_msg + "Lat: " + latitude + "\nLong: " + longitude + "\n\n" + address + "  " + city + "  " + country, null, null);
            Toast.makeText(MainActivity.this, "Emergency SMS sent.",
                    Toast.LENGTH_LONG).show();
            mBluetoothAdapter.disable();
            setContentView(R.layout.activity_accident);
        } catch (Exception e) {
            Toast.makeText(MainActivity.this,
                    "SMS failed, please try again.",
                    Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_refresh) {
            connectArduino();

            //Log.d("Tag", sbPhone.toString());


            return true;
        } else if (id == R.id.action_reset) {
            sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
            sharedpreferences.edit().clear().apply();
            Toast.makeText(MainActivity.this, "Done Resetting contacts", Toast.LENGTH_SHORT).show();
            sbPhone.delete(0, sbPhone.length());
            sbName.delete(0, sbName.length());
            msg = "";
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


}
