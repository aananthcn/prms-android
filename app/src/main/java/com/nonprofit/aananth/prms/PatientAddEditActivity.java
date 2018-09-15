package com.nonprofit.aananth.prms;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;

import static android.provider.AlarmClock.EXTRA_MESSAGE;

public class PatientAddEditActivity extends AppCompatActivity {

    private String TAG = "PRMS-PatientAddEditActivity";
    private PatientDB mPatientDB;
    private Patient mCurrPatient;
    private enum Mode {ADD_PAT, UPDATE_PAT, INVALID_PAT};
    Mode mMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_edit_pat);
        Log.d(TAG, "Entering onCreate()");

        //do basic initialization of objects
        mPatientDB = new PatientDB(this);
        Intent intent = getIntent();
        mCurrPatient = (Patient) intent.getSerializableExtra("patient");
        String message = intent.getStringExtra(EXTRA_MESSAGE);
        if (message.equalsIgnoreCase("add patient")) {
            AddNewPatient();
        }
        else if (message.equalsIgnoreCase("update patient")) {
            EditPatientRecord();
        }
        else {
            update_mode(Mode.INVALID_PAT);
            Log.d(TAG, "onCreate() invalid Mode");
        }
    }

    public void AddNewPatient() {
        setTitle("Add patient");
        Log.d(TAG, "AddNewPatient()");

        Button delbtn = (Button) findViewById(R.id.pat_del);
        delbtn.setVisibility(View.INVISIBLE);
        update_mode(Mode.ADD_PAT);
    }

    public void EditPatientRecord() {
        EditText patname, patphone, patmail;
        update_mode(Mode.UPDATE_PAT);
        setTitle("Update patient");
        Log.d(TAG, "EditPatientRecord()");

        patname = (EditText) findViewById(R.id.fullName);
        patname.setText(mCurrPatient.Name);
        patphone = (EditText) findViewById(R.id.phoneNo);
        patphone.setText(mCurrPatient.Phone);
        patmail = (EditText) findViewById(R.id.emailID);
        patmail.setText(mCurrPatient.Email);
        if (mCurrPatient.Gender.equalsIgnoreCase("Male")) {
            RadioButton gender = (RadioButton) findViewById(R.id.radioMale);
            gender.setChecked(true);
        }
        else {
            RadioButton gender = (RadioButton) findViewById(R.id.radioFemale);
            gender.setChecked(true);
        }

        Button patsav = (Button) findViewById(R.id.pat_save);
        patsav.setText("Update");
        Button patback = (Button) findViewById(R.id.pat_back);
        patback.setText("Cancel");
    }

    public void SavePatientRecord(View view) {
        EditText patname, patphone, patmail;
        String gender;
        RadioButton patgender;

        Log.d(TAG, "SavePatientRecord()");
        patname = (EditText)findViewById(R.id.fullName);
        patphone = (EditText)findViewById(R.id.phoneNo);
        patmail = (EditText)findViewById(R.id.emailID);
        patgender = (RadioButton)findViewById(R.id.radioMale);

        if (patname.getText().toString().length() == 0) {
            //myOnBackPressed();
            this.onBackPressed();
            return;
        }

        if (patgender.isChecked())
            gender = "Male";
        else
            gender = "Female";


        if (mMode == Mode.ADD_PAT) {
            Patient pat = new Patient(patname.getText().toString(), patphone.getText().toString(),
                    patmail.getText().toString(), gender, null, null);
            mPatientDB.AddPatient(pat);
            Log.d(TAG, "Added patient '" + pat.Name + "'");
        }
        else if (mMode == Mode.UPDATE_PAT) {
            Patient pat = new Patient(patname.getText().toString(), patphone.getText().toString(),
                    patmail.getText().toString(), gender, mCurrPatient.Pid, mCurrPatient.Uid);
            mPatientDB.UpdatePatient(pat);
            Log.d(TAG, " Updated patient '" + pat.Name + "'");
        }
        //myOnBackPressed();
        this.onBackPressed();
    }

    public void CancelPatientRecordEdit(View view) {
        Log.d(TAG, "CancelPatientRecordEdit()");
        this.onBackPressed();
    }

    public void DeleteCurrPatient(View view) {
        Log.d(TAG, "DeleteCurrPatient()");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure to delete this patient?");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked ok button
                mPatientDB.DeletePatient(mCurrPatient);
                PatientAddEditActivity.this.onBackPressed();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
                PatientAddEditActivity.this.onBackPressed();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }


    private void update_mode(Mode mode) {
        Log.d(TAG, "mMode (old) = " + mMode + ", mMode (new) = " + mode);
        mMode = mode;
    }
}
