package in.consistentservices.home_statpotentiostat;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.PointsGraphSeries;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class plottingScreen extends AppCompatActivity
{
    private int voltage = 0;
    private int current = 0;
    private int choice = 1;

    private Button start;
    private Button stop;
    private Button refresh;
    private Button save;
    private TextView status;

    //Permission Management Variables
    private static final int REQUEST_ENABLE_BT = 1;

    //Bluetooth Adapter
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice mmDevice;
    private BluetoothSocket mmSocket;
    private OutputStream mmOutputStream;
    private InputStream mmInputStream;
    private Thread workerThread;
    private byte[] readBuffer;
    private int readBufferPosition;
    private int counter;
    private volatile boolean stopWorker;

    private GraphView chart;
    private PointsGraphSeries<DataPoint> xySeries;
    private ArrayList<XYValue> xyValueArray;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plotting_screen);

        //Hiding Action Bar
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.hide();

        Intent intent = getIntent();
        voltage = intent.getIntExtra("Volt", 4);
        current = intent.getIntExtra("Current", 500);
        choice = intent.getIntExtra("Choice", 1);

        start = (Button) findViewById(R.id.start);
        stop = (Button) findViewById(R.id.stop);
        refresh = (Button) findViewById(R.id.refresh);
        save = (Button) findViewById(R.id.save);
        status = (TextView) findViewById(R.id.status);

        chart = (GraphView) findViewById(R.id.chart);
        xySeries = new PointsGraphSeries<>();
        xyValueArray = new ArrayList<>();

        chart.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.BOTH);
        chart.getGridLabelRenderer().setNumHorizontalLabels(7);
        chart.getGridLabelRenderer().setNumVerticalLabels(7);

        chart.getViewport().setYAxisBoundsManual(true);
        chart.getViewport().setMaxY(current);
        chart.getViewport().setMinY(0);

        chart.getViewport().setXAxisBoundsManual(true);
        chart.getViewport().setMaxX(voltage);
        chart.getViewport().setMinX(0);

        chart.getGridLabelRenderer().setGridColor(Color.WHITE);
        chart.getGridLabelRenderer().setVerticalLabelsColor(Color.WHITE);
        chart.getGridLabelRenderer().setHorizontalLabelsColor(Color.WHITE);
        chart.getGridLabelRenderer().reloadStyles();

        disableBluetooth();
        enableBluetooth();

        start.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if(!enableBluetooth())
                {
                    return;
                }
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        status.setText(R.string.scanning);
                    }
                });
                findBT();
            }
        });

        stop.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                closeBT();
            }
        });

        refresh.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
            }
        });

        save.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
            }
        });

        disableBluetooth();
        enableBluetooth();
    }

    @Override
    public void onBackPressed()
    {
        closeBT();
        super.onBackPressed();
        Intent go = new Intent( plottingScreen.this, parameterScreen.class);
        startActivity(go);
        finish();
    }

    private void disableBluetooth()
    {
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        assert bluetoothManager != null;
        bluetoothAdapter = bluetoothManager.getAdapter();

        if(bluetoothAdapter.isEnabled())
        {
            bluetoothAdapter.disable();
        }
    }

    private boolean enableBluetooth()
    {
        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        assert bluetoothManager != null;
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled())
        {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return false;
        }
        else
        {
            return true;
        }
    }

    private void findBT()
    {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter == null)
        {
            Toast.makeText(plottingScreen.this, "No Bluetooth Adapter Available!", Toast.LENGTH_LONG).show();
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice device : pairedDevices)
            {
                if(device.getName().equals("HOME-Stat:Potentiostat"))
                {
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            status.setText(R.string.device_found);
                        }
                    });
                    mmDevice = device;
                    openBT();
                    break;
                }
            }
        }
    }

    private void openBT()
    {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
        try
        {
            mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
            mmSocket.connect();
            mmOutputStream = mmSocket.getOutputStream();
            mmInputStream = mmSocket.getInputStream();
            beginListenForData();
            sendData();
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    status.setText(R.string.connection_opened);
                }
            });
        }

        catch (Exception e)
        {
            e.printStackTrace();
            Log.d("BT_Deb_###", e.toString());
            Toast.makeText(plottingScreen.this, "Bluetooth Connection Error!", Toast.LENGTH_LONG).show();
        }
    }

    private void beginListenForData()
    {
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    try
                    {
                        int bytesAvailable = mmInputStream.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, StandardCharsets.US_ASCII);
                                    readBufferPosition = 0;

                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {
                                            Log.d("BT_Deb_###", data.split(",")[0]);
                                            Log.d("BT_Deb_###", data.split(",")[1]);
                                            plotGraph(Double.parseDouble(data.split(",")[0]),
                                                     Double.parseDouble(data.split(",")[1]));
                                        }
                                    });
                                }

                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        stopWorker = true;
                        e.printStackTrace();
                        Log.d("BT_Deb_###", e.toString());
                    }
                }
            }
        });
        workerThread.start();
    }

    private void sendData()
    {
        String msg = choice + "\n";
        try
        {
            mmOutputStream.write(msg.getBytes());
            Toast.makeText(plottingScreen.this, "Data Sent!", Toast.LENGTH_LONG).show();
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    status.setText(R.string.settings_sent);
                }
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Log.d("BT_Deb_###", e.toString());
            Toast.makeText(plottingScreen.this, "Unable to Send Data!", Toast.LENGTH_LONG).show();
        }
    }

    private void closeBT()
    {
        stopWorker = true;
        try
        {
            mmOutputStream.close();
            mmInputStream.close();
            mmSocket.close();
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    status.setText(R.string.not_connected);
                }
            });
        }

        catch (Exception e)
        {
            e.printStackTrace();
            Log.d("BT_Deb_###", e.toString());
        }
    }

    private void plotGraph(double i, double v)
    {
        xySeries = new PointsGraphSeries<>();
        xyValueArray.add(new XYValue(map(v, voltage), map(i, current)));

        Log.d("BT_Deb_V_###", String.valueOf(map(v, voltage)));

        if(xyValueArray.size() == 0)
        {
            return;
        }

        try
        {
            xySeries.appendData(new DataPoint(map(v, voltage), map(i, current)),true, 6000);
            xySeries.setShape(PointsGraphSeries.Shape.POINT);
            xySeries.setColor(Color.YELLOW);
            xySeries.setSize(10f);

            chart.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.BOTH);
            chart.getGridLabelRenderer().setNumHorizontalLabels(7);
            chart.getGridLabelRenderer().setNumVerticalLabels(7);

            chart.getViewport().setYAxisBoundsManual(true);
            chart.getViewport().setMaxY(current);
            chart.getViewport().setMinY(0);

            chart.getViewport().setXAxisBoundsManual(true);
            chart.getViewport().setMaxX(voltage);
            chart.getViewport().setMinX(0);

            chart.getGridLabelRenderer().setGridColor(Color.WHITE);
            chart.getGridLabelRenderer().setVerticalLabelsColor(Color.WHITE);
            chart.getGridLabelRenderer().setHorizontalLabelsColor(Color.WHITE);
            chart.getGridLabelRenderer().reloadStyles();

            chart.addSeries(xySeries);
        }

        catch (Exception e)
        {
            e.printStackTrace();
            Log.d("BT_Deb_###", e.toString());
        }
    }

    private ArrayList<XYValue> sortArray(ArrayList<XYValue> array)
    {
        int factor = Integer.parseInt(String.valueOf(Math.round(Math.pow(array.size(),2))));
        int m = array.size() - 1;
        int count = 0;
        Log.d("BT_Deb_###", "sortArray: Sorting the XYArray.");


        while (true)
        {
            m--;
            if (m <= 0)
            {
                m = array.size() - 1;
            }
            Log.d("BT_Deb_###", "sortArray: m = " + m);
            try
            {
                double tempY = array.get(m - 1).getY();
                double tempX = array.get(m - 1).getX();
                if (tempX > array.get(m).getX())
                {
                    array.get(m - 1).setY(array.get(m).getY());
                    array.get(m).setY(tempY);
                    array.get(m - 1).setX(array.get(m).getX());
                    array.get(m).setX(tempX);
                }

                else if (tempX == array.get(m).getX())
                {
                    count++;
                    Log.d("BT_Deb_###", "sortArray: count = " + count);
                }
                else if (array.get(m).getX() > array.get(m - 1).getX())
                {
                    count++;
                    Log.d("BT_Deb_###", "sortArray: count = " + count);
                }
                //break when factorial is done
                if (count == factor)
                {
                    break;
                }
            }
            catch (Exception e)
            {
                Log.e("BT_Deb_###", "sortArray: ArrayIndexOutOfBoundsException. Need more than 1 data point to create Plot." +
                        e.getMessage());
                break;
            }
        }
        return array;
    }

    private double map(double x, double out_max)
    {
        return (x * out_max) / 4095.0;
    }
}