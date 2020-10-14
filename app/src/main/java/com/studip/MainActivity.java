package com.studip;
import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.studip.api.API;


public class MainActivity extends AppCompatActivity
{
    

    
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try
        {
            MasterKey.Builder b = new MasterKey.Builder(this);
            b.setKeyScheme(MasterKey.KeyScheme.AES256_GCM);
            MasterKey m = b.build();
            SharedPreferences sharedPreferences = EncryptedSharedPreferences.create(
                    this,
                    "secret_shared_prefs",
                    m,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
            if (Data.settings == null)
            {
                Data.settings = Settings.load(sharedPreferences);
            }
            if (Data.api == null)
            {
                Data.api = API.restore(sharedPreferences,this);
            }
        }
        catch (Exception e) {e.printStackTrace();}
        if (Data.api != null && Data.api.logged_in())
        {
            Intent intent = new Intent(this,HomeActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onStop()
    {
        super.onStop();
    }

    public void onSettingsButton(View v)
    {
        Intent intent = new Intent(this,SettingsActivity.class);
        startActivity(intent);
    }
    
    
    private void toLoginActivity()
    {
        Intent intent = new Intent(this,LoginActivity.class);
        startActivity(intent);
        finish();
    }
    
    public void onSubmitURL(View v)
    {
        if (v.equals(findViewById(R.id.submit_url)))
        {
            TextView t = findViewById(R.id.server_url);
            Data.api = new API(t.getText().toString());
            toLoginActivity();
            return;
        }
        else
        {
            if (v instanceof Button)
            {
                Button b = (Button) v;
                Data.api = new API(b.getText().toString());
                toLoginActivity();
                return;
            }
        }
    }
}