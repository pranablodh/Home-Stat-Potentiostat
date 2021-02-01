package in.consistentservices.home_statpotentiostat;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class splashScreen extends AppCompatActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);

        //Hiding Action Bar
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.hide();

        new Handler().postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                goToLanding();
            }
        },3000);

    }

    private void goToLanding()
    {
        Intent go = new Intent(splashScreen.this, parameterScreen.class);
        startActivity(go);
        finish();
    }
}