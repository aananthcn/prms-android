package com.nonprofit.aananth.prms;

import android.content.res.Configuration;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import java.lang.String;
import java.util.Arrays;
import java.util.List;

public class LoginActivity extends AppCompatActivity {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    // Aananth added these member variables
    private int currLayout;
    private List doctors = Arrays.asList("Jegadish", "Rama");
    private EditText user, password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        // Example of a call to a native method
        TextView tv = (TextView) findViewById(R.id.message_txt);
        tv.setText(stringFromJNI());

        // Aananth added these lines
        currLayout = R.layout.login;
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    public void onLogin(View view) {
        // get the user inputs
        user = (EditText)findViewById(R.id.user);
        password = (EditText)findViewById(R.id.password);

        // check if the credentials are valid. TODO: Implement a secure and better auth logic
        if (doctors.contains(user.getText().toString())) {
            if (password.getText().toString().contains("saami123")) {
                Log.d("Authentication", "Successful login");
                currLayout = R.layout.patients;
                setContentView(currLayout);
            }
            else {
                Log.d("Authentication", "Failure! Password: " + password.getText().toString());
            }
        }
        else {
            Log.d("Authentication ", "Failure! Username: " + user.getText().toString());
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d("Configuration Changed", "Layout set to current layout");
        setContentView(currLayout);
    }
}
