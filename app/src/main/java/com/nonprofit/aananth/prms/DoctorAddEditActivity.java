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

import static android.provider.AlarmClock.EXTRA_MESSAGE;

public class DoctorAddEditActivity extends AppCompatActivity {

    private String TAG = "PRMS-DoctorAdd/EditActivity";
    private DoctorDB doctorDB;
    private Doctor mDoctor;
    private String mFunctionMessage;
    //private enum DoctorFunctions {ADD, EDIT, INVALID};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_edit_doc);

        Log.d(TAG, "onCreate()");
        doctorDB = new DoctorDB(this);
        // Get the Intent that started this activity and extract the string

        Intent intent = getIntent();
        mFunctionMessage = intent.getStringExtra(EXTRA_MESSAGE);
        mDoctor = (Doctor) intent.getSerializableExtra("doctor");
        if (mFunctionMessage.equalsIgnoreCase("add doctor")) {
            AddNewDoctor();
        }
    }

    public void AddNewDoctor() {
        Log.d(TAG, "AddNewDoctor()");
        Button del = (Button) findViewById(R.id.doc_del);
        del.setVisibility(View.INVISIBLE);
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

        this.onBackPressed();
        finish();
    }


    public void DeleteCurrDoc(View view) {
        Log.d(TAG, "DeleteCurrDoc()");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure to delete this Doctor?");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked ok button
                doctorDB.DeleteDoctor(mDoctor);
                DoctorAddEditActivity.this.onBackPressed();
                finish();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
                DoctorAddEditActivity.this.onBackPressed();
                finish();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }


    public void CancelDocRecordEdit(View view) {
        Log.d(TAG, "DeleteCurrDoc()");
        this.onBackPressed();
        finish();
    }

}
