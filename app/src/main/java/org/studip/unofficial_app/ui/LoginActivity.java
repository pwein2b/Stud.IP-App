package org.studip.unofficial_app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.security.crypto.EncryptedSharedPreferences;

import org.studip.unofficial_app.R;
import org.studip.unofficial_app.api.API;
import org.studip.unofficial_app.model.APIProvider;
import org.studip.unofficial_app.model.SettingsProvider;


public class LoginActivity extends AppCompatActivity
{
    private boolean login_running = false;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        API api = APIProvider.getAPI(this);
        if (APIProvider.getAPI(this) == null) {
            Intent intent = new Intent(this, ServerSelectActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        
        
        
        /*
        if (api != null && api.logged_in(this) == 200)
        {
            Intent intent = new Intent(this,HomeActivity.class);
            startActivity(intent);
            finish();
            return;
        }
         */
        
        
        
        setContentView(R.layout.activity_login);
    }
    
    
    @Override
    public void onStop()
    {
        super.onStop();
        API api = APIProvider.getAPI(this);
        if (api != null) {
            EncryptedSharedPreferences prefs = APIProvider.getPrefs(this);
            if (prefs != null)
            {
                api.save(prefs);
            }
        }
    }
    
    @Override
    public void onBackPressed()
    {
        finish();
    }
    
    public void onLogin(View v)
    {
        TextView username = findViewById(R.id.username);
        TextView password = findViewById(R.id.password);
        if (! login_running)
        {
            login_running = true;
            final AppCompatActivity c = this;
            LiveData<Integer> l = APIProvider.getAPI(this).login(this, username.getText().toString(), password.getText().toString(), SettingsProvider.getSettings(this).authentication_method);
            l.observe(this, code ->
            {
                if (code != -1)
                {
                    if (code == 200)
                    {
                        Intent intent = new Intent(c, HomeActivity.class);
                        startActivity(intent);
                        finish();
                        APIProvider.getAPI(c).save(APIProvider.getPrefs(c));
                        login_running = false;
                        //System.out.println("logged in");
                        l.removeObservers(c);
                    }
                    else
                    {
                        l.removeObservers(c);
                        login_running = false;
                        int msg;
                        switch (code) {
                            case 401:
                                msg = R.string.login_error_auth;
                                break;
                            case 403:
                            case 404:
                            case 405:
                                msg = R.string.login_error_no_api;
                                break;
                            default:
                                msg = R.string.login_error_message;
                        }
                        AlertDialog d = new AlertDialog.Builder(c).setTitle(R.string.login_error_title).setMessage(HelpActivity.fromHTML(getString(msg))).show();
                        try {
                            TextView t = d.findViewById(android.R.id.message);
                            if (t != null) {
                                t.setMovementMethod(LinkMovementMethod.getInstance());
                            }
                        } catch (Exception ignored) {}
                    }
                }
            });
        }
    }
    
    
    
    
    
    
}