package in.consistentservices.home_statpotentiostat;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;

public class parameterScreen extends AppCompatActivity
{

    private Button next;

    private RadioGroup voltageControl;
    private RadioButton oneVolt;
    private RadioButton twoVolt;
    private RadioButton threeVolt;
    private RadioButton fourVolt;

    private RadioGroup currentControl;
    private RadioButton hundredMa;
    private RadioButton fiveHundredMa;
    private RadioButton twoMa;
    private RadioButton fiveMa;

    private int voltage = 0;
    private int current = 0;
    private int choice = 0;

    private boolean voltFlag = false;
    private boolean currentFlag = false;

    //Variables for Permission
    protected static final int REQUEST_CHECK_SETTINGS = 0x1;
    private static final int INITIAL_REQUEST = 1337;
    private String[] Permissions = {android.Manifest.permission.WRITE_EXTERNAL_STORAGE};

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parameter_screen);

        //Hiding Action Bar
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.hide();
        checkingPermission();

        next = (Button) findViewById(R.id.next);

        voltageControl = (RadioGroup) findViewById(R.id.voltageControl);
        oneVolt = (RadioButton) findViewById(R.id.oneVolt);
        twoVolt = (RadioButton) findViewById(R.id.twoVolt);
        threeVolt = (RadioButton) findViewById(R.id.threeVolt);
        fourVolt = (RadioButton) findViewById(R.id.fourVolt);

        currentControl = (RadioGroup) findViewById(R.id.currentControl);
        hundredMa = (RadioButton) findViewById(R.id.hundredMa);
        fiveHundredMa = (RadioButton) findViewById(R.id.fiveHundredMa);
        twoMa = (RadioButton) findViewById(R.id.twoMa);
        fiveMa = (RadioButton) findViewById(R.id.fiveMa);

        next.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {

                if(!currentFlag )
                {
                    Toast.makeText(parameterScreen.this, "Please Select Current", Toast.LENGTH_SHORT).show();
                    return;
                }

                if(!voltFlag )
                {
                    Toast.makeText(parameterScreen.this, "Please Select Voltage", Toast.LENGTH_SHORT).show();
                    return;
                }

                if(!checkingPermission() )
                {
                    Toast.makeText(parameterScreen.this, "Please Grant Necessary Permission", Toast.LENGTH_SHORT).show();
                    return;
                }
                next();
            }
        });

        voltageControl.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, final int i)
            {
                if (i == R.id.oneVolt)
                {
                    voltFlag = true;
                    voltage = 1;
                }

                else if (i == R.id.twoVolt)
                {
                    voltFlag = true;
                    voltage = 2;
                }

                else if (i == R.id.threeVolt)
                {
                    voltFlag = true;
                    voltage = 3;
                }

                else if (i == R.id.fourVolt)
                {
                    voltFlag = true;
                    voltage = 4;
                }
            }
        });

        currentControl.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, final int i)
            {
                if (i == R.id.hundredMa)
                {
                    currentFlag = true;
                    current = 100;
                    choice = 1;
                    //Toast.makeText(parameterScreen.this, String.valueOf(choice), Toast.LENGTH_SHORT).show();
                }

                else if (i == R.id.fiveHundredMa)
                {
                    currentFlag = true;
                    current = 500;
                    choice = 2;
                    //Toast.makeText(parameterScreen.this, String.valueOf(choice), Toast.LENGTH_SHORT).show();
                }

                else if (i == R.id.twoMa)
                {
                    currentFlag = true;
                    current = 2;
                    choice = 3;
                    //Toast.makeText(parameterScreen.this, String.valueOf(choice), Toast.LENGTH_SHORT).show();
                }

                else if (i == R.id.fiveMa)
                {
                    currentFlag = true;
                    current = 5;
                    choice = 4;
                    //Toast.makeText(parameterScreen.this, String.valueOf(choice), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void next()
    {
        Intent go = new Intent(parameterScreen.this, plottingScreen.class);
        go.putExtra("Volt", voltage);
        go.putExtra("Current", current);
        go.putExtra("Choice", choice);
        startActivity(go);
        finish();
    }

    @Override
    public void onBackPressed()
    {
        super.onBackPressed();
        finishAffinity();
        System.exit(0);
    }

    //Checking Permission
    private boolean checkingPermission()
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if(!canWriteExternalStorage())
            {
                requestPermissions(Permissions, INITIAL_REQUEST);
                return false;
            }

            else
            {
                return true;
            }
        }

        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean canWriteExternalStorage()
    {
        return(hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE));
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean hasPermission(String perm)
    {
        return(PackageManager.PERMISSION_GRANTED == checkSelfPermission(perm));
    }
}