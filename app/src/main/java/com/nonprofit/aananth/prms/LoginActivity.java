package com.nonprofit.aananth.prms;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import static android.provider.AlarmClock.EXTRA_MESSAGE;

public class LoginActivity extends AppCompatActivity {

    private String TAG = "PRMS-LoginActivity";
    private String mLoginMessage;
    private Spinner mSpinner;
    private Doctor mDoctor;
    private List<Doctor> mDocList;
    private DoctorDB doctorDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        Log.d(TAG, "onCreate()");
        doctorDB = new DoctorDB(this);

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        mLoginMessage = intent.getStringExtra(EXTRA_MESSAGE);
        mDoctor = (Doctor) intent.getSerializableExtra("doctor");
    }


    public void onResume() {
        super.onResume();
        setupDoctorLoginSpinner();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.login_menu, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected()");
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.add_doctor:
                AddNewDoctor();
                return true; // say to android that this menu event is handled here...
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setupDoctorLoginSpinner() {
        // Welcome message
        TextView tv = (TextView) findViewById(R.id.message_txt);
        tv.setText(mLoginMessage);

        // Set login options
        mSpinner = (Spinner)findViewById(R.id.doctor);
        List<String> loginList = new ArrayList<String>();
        mDocList = doctorDB.GetDoctorList(ListOrder.ASCENDING);
        Log.d(TAG, "mDocList.size = " + mDocList.size());
        for (Doctor doc : mDocList) {
            Log.d(TAG, "Doctor name = " + doc.name);
            loginList.add(doc.name);
        }

        // Creating adapter for mSpinner
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, loginList);

        // Drop down layout style - list view with radio button
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // attaching data adapter to mSpinner
        mSpinner.setAdapter(dataAdapter);
    }


    public void AddNewDoctor() {
        Log.d(TAG, "AddNewDoctor()");
        Intent intent = new Intent(this, DoctorAddEditActivity.class);
        intent.putExtra("doctor", mDoctor);
        intent.putExtra(EXTRA_MESSAGE, "add doctor");
        startActivity(intent);
    }


    public void onLoginButtonClick(View view) {
        Log.d(TAG, "onLoginButtonClick()");

        if(mDocList.size() > 0) {
            if (!mDocList.get(0).name.equalsIgnoreCase("Empty")) {
                Intent intent = new Intent();
                intent.putExtra("login result", "login successful");
                setResult(RESULT_OK, intent);
                finish();
            }
        }
    }


    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra("login result", "login exit");
        setResult(RESULT_OK, intent);
        finish();
    }
}
