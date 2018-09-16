package com.nonprofit.aananth.prms;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
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
        setupDoctorLoginSpinner();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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
        for (Doctor doc : mDocList) {
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
        //currLayout = R.layout.add_edit_doc;
        //setContentView(currLayout);
        Button del = (Button) findViewById(R.id.doc_del);
        del.setVisibility(View.INVISIBLE);
        //update_mode(Mode.ADD_DOCT);
    }


    public void SaveDocRecord(View view) {
        Log.d(TAG, "SaveDocRecord()");
        EditText docName, docPhone, docEmail;
        String name, ph, email;

        docName = (EditText)findViewById(R.id.docName);
        docPhone = (EditText)findViewById(R.id.docPhone);
        docEmail = (EditText)findViewById(R.id.docEmail);

        name = docName.getText().toString();
        ph = docPhone.getText().toString();
        email = docEmail.getText().toString();

        Doctor doc = new Doctor(name, ph, email);
        doctorDB.AddDoctor(doc);

        //myOnBackPressed();
        this.onBackPressed();
    }


    public void DeleteCurrDoc(View view) {
        Log.d(TAG, "DeleteCurrDoc()");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure to delete this Doctor?");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked ok button
                doctorDB.DeleteDoctor(mDoctor);
                //myOnBackPressed();
                LoginActivity.this.onBackPressed();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
                //myOnBackPressed();
                LoginActivity.this.onBackPressed();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }


    public void CancelDocRecordEdit(View view) {
        Log.d(TAG, "DeleteCurrDoc()");
        //myOnBackPressed();
        this.onBackPressed();
    }


    public void onLoginButtonClick(View view) {
        Log.d(TAG, "onLoginButtonClick()");
        Intent intent = new Intent();
        intent.putExtra("login result", "login successful");
        setResult(RESULT_OK, intent);
        finish();
    }
}
