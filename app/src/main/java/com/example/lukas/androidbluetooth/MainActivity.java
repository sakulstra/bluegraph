package com.example.lukas.androidbluetooth;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.widget.Toast;

import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;


public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks,
        SettingsFragment.OnFragmentInteractionListener,
        DevicesFragment.OnFragmentInteractionListener,
        GraphFragment.OnFragmentInteractionListener,
        TerminalFragment.OnFragmentInteractionListener{

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;
    private SettingsFragment mSettingsFragment;
    private DevicesFragment mDevicesFragment;
    private GraphFragment mGraphFragment;
    private TerminalFragment mTerminalFragment;
    private FragmentManager fragmentManager;
    private BluetoothSPP mBluetoothSPP;

    SharedPreferences sharedPref;
    private boolean axisInitialized = false;
    private boolean firstDump = true;

    public int axis;
    public int currentFragment;
    private LineGraphSeries<DataPoint>[] mSeries;
    private int graphLastXValue;

    private boolean connected = false;


    FileOutputStream stream = null;
    boolean streamOpen = false;

    public void disconnect(){
        if(connected){
            mBluetoothSPP.disconnect();
            connected = false;
        }
    }

    public boolean isConnected(){
        return connected;
    }

    public void sendMessage(String message, boolean lineEndings){
        if(isConnected()){
            mBluetoothSPP.send(message,lineEndings);
        }
    }
    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState == null) {
            setContentView(R.layout.activity_main);

            fragmentManager = getFragmentManager();
            sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            mDevicesFragment = new DevicesFragment();
            mSettingsFragment = new SettingsFragment();
            mNavigationDrawerFragment = (NavigationDrawerFragment)
                    getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
            mTitle = getTitle();
            mGraphFragment = new GraphFragment();
            mTerminalFragment = new TerminalFragment();
            // Set up the drawer.
            mNavigationDrawerFragment.setUp(
                    R.id.navigation_drawer,
                    (DrawerLayout) findViewById(R.id.drawer_layout));

            mBluetoothSPP = new BluetoothSPP(getApplicationContext());
            if (!mBluetoothSPP.isBluetoothAvailable()) {
                //oops, don't have bluetooth
                return;
            }
            if (!mBluetoothSPP.isBluetoothEnabled()) {
                //bluetooth is turned off, let's try to turn it on
                mBluetoothSPP.enable();
            }
            while (!mBluetoothSPP.isBluetoothEnabled()) ;
            mBluetoothSPP.setupService(); // setup bluetooth service
            mBluetoothSPP.startService(BluetoothState.DEVICE_OTHER); // start bluetooth service
            mBluetoothSPP.setBluetoothConnectionListener(new BluetoothSPP.BluetoothConnectionListener() {
                public void onDeviceConnected(String name, String address) {
                    connected = true;
                    Toast toast = Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_LONG);
                    toast.show();
                    mNavigationDrawerFragment.selectItem(2);
                }

                public void onDeviceDisconnected() {
                    connected = false;
                    Toast toast = Toast.makeText(getApplicationContext(), "Disconnected", Toast.LENGTH_LONG);
                    toast.show();
                }

                public void onDeviceConnectionFailed() {
                    Toast toast = Toast.makeText(getApplicationContext(), "Couldn't connect to device", Toast.LENGTH_LONG);
                    toast.show();
                }
            });
        }

    }

    @Override
    public void onResume(){
        super.onResume();
        if(mNavigationDrawerFragment!=null)
            mNavigationDrawerFragment.selectItem(currentFragment);
    }

    @Override
    public void onBackPressed() {
        if (fragmentManager.getBackStackEntryCount()>1) {
            fragmentManager.popBackStack();
        } else {
            super.onBackPressed();
            return;
        }

    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        fragmentManager = getFragmentManager();
        if(mDevicesFragment == null){
            mDevicesFragment = new DevicesFragment();
        }
        currentFragment = position;
        switch(position){
            case 0:
                fragmentManager.beginTransaction()
                    .replace(R.id.container, mDevicesFragment, "DEVICES").addToBackStack("devices")
                    .commit();
                break;
            case 1:
                fragmentManager.beginTransaction()
                    .replace(R.id.container, mSettingsFragment, "SETTINGS").addToBackStack("settings")
                    .commit();
                break;
            case 2:
                fragmentManager.beginTransaction()
                    .replace(R.id.container, mGraphFragment, "GRAPH").addToBackStack("graph")
                    .commit();
                break;
            case 3:
                fragmentManager.beginTransaction()
                        .replace(R.id.container, mTerminalFragment, "TERMINAL").addToBackStack("terminal")
                        .commit();
                break;
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == BluetoothState.REQUEST_CONNECT_DEVICE) {
            if(resultCode == Activity.RESULT_OK) {
                mBluetoothSPP.connect(data);
                mBluetoothSPP.setOnDataReceivedListener(new BluetoothSPP.OnDataReceivedListener() {

                    public void onDataReceived(byte[] data, String message) {
                        if (streamOpen) {
                            writeToFile(message);
                        }
                        if (firstDump) {
                            firstDump = false;
                            return;
                        }
                        TerminalFragment terminal = (TerminalFragment) fragmentManager.findFragmentByTag("TERMINAL");
                        GraphFragment graph = (GraphFragment) fragmentManager.findFragmentByTag("GRAPH");
                        if (graph != null && graph.isGraphOpen()) {
                            if (!axisInitialized) {
                                if(sharedPref.getBoolean("pref_startTagOn", false)){
                                    String tmp = sharedPref.getString("pref_startTag","");
                                    message.replace(tmp,"");
                                }
                                axis = (message.split(sharedPref.getString("pref_delimiter", ","))).length;
                                if (mSeries == null || mSeries.length != axis) {
                                    mSeries = new LineGraphSeries[axis];
                                    for (int i = 0; i < axis; i++) {
                                        mSeries[i] = new LineGraphSeries<DataPoint>();
                                    }
                                }
                                graph.initialize(mSeries);
                                axisInitialized = true;
                            } else {
                                String[] parts = message.split(sharedPref.getString("pref_delimiter", ","));
                                int tmp = Integer.parseInt(sharedPref.getString("pref_windowSize", "200"));
                                for (int i = 0; i < axis; i++) {
                                    try {
                                        mSeries[i].appendData(new DataPoint(graphLastXValue, Float.parseFloat(parts[i])), true, tmp);
                                        graphLastXValue++;
                                    } catch (NumberFormatException e) {
                                        Toast toast = Toast.makeText(getApplicationContext(), "Make sure the delimiter is correct. Got: " + parts[i] + " after parsing", Toast.LENGTH_LONG);
                                        toast.show();
                                        mNavigationDrawerFragment.selectItem(1);
                                        return;
                                    }
                                }
                            }
                        } else {
                            axisInitialized = false;
                            if (terminal != null) {
                                terminal.updateText(message);
                            }
                        }
                    }

                });
            }
        } else if(requestCode == BluetoothState.REQUEST_ENABLE_BT) {
            if(resultCode == Activity.RESULT_OK) {
                mBluetoothSPP.setupService();
                mBluetoothSPP.startService(BluetoothState.DEVICE_ANDROID);
            }
        }
    }


    public boolean initFileWrite(String filename){
        streamOpen = true;
        if(!isExternalStorageWritable()){
            return false;
        }

        File file = createFile(filename);
        try {
            stream = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void stopFileWrite(){
        streamOpen = false;
        closeFileStream(stream);
    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    private File createFile(String filename){
        File file = new File(Environment.getExternalStorageDirectory(), filename);
        try {
            if(!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }else{
                //file exists
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }

    public void writeToFile(String data){
        try {
            stream.write(data.getBytes());
            stream.write('\r');
            stream.write('\n');
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeFileStream(FileOutputStream stream){
        if(stream == null)return;
        try {
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = getString(R.string.title_section1);
                break;
            case 2:
                mTitle = getString(R.string.title_section2);
                break;
            case 3:
                mTitle = getString(R.string.title_section3);
                break;
            case 4:
                mTitle = getString(R.string.title_section4);
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(mTitle);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

   /* @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }*/

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

}
