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
import android.widget.Button;
import android.widget.EditText;

import static android.provider.AlarmClock.EXTRA_MESSAGE;

public class TreatmentAddEditActivity extends AppCompatActivity {

    private String TAG = "PRMS-TreatmentAddEditActivity";
    private enum Mode {
        ADD_TREAT, EDIT_TREAT, MAX_TREAT_MODE
    }
    private Mode mMode;
    private Treatment mCurrTreatment;
    private TreatmentDB mTreatmentDB;
    private Patient mCurrPatient;
    private Doctor mDoctor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_edit_treat);
        Log.d(TAG, "Entering onCreate()");

        //do basic initialization of objects
        mTreatmentDB = new TreatmentDB(this);
        Intent intent = getIntent();
        String message = intent.getStringExtra(EXTRA_MESSAGE);
        mCurrPatient = (Patient) intent.getSerializableExtra("patient");
        mDoctor = (Doctor) intent.getSerializableExtra("doctor");
        findTreatmentEditType(message, intent);
    }

    private void findTreatmentEditType(String message, Intent intent) {

        if (message.contains("edit treatment")) {
            setTitle("Edit Treatment");
            mCurrTreatment = (Treatment) intent.getSerializableExtra("treatment");
            EditTreatmentRecord();
        }
        else if (message.contains("add treatment")) {
            setTitle("Add New Treatment");
            AddNewTreatment();
        }
        else {
            setTitle(message);
        }
    }

    public void AddNewTreatment() {
        // Add date according to the format defined in Treatment class
        Treatment treat = new Treatment(null, null, null, null, null);
        EditText date = (EditText) findViewById(R.id.treat_date);
        date.setText(treat.date);
        EditText compl = (EditText) findViewById(R.id.complaint);
        compl.requestFocus();

        update_mode(Mode.ADD_TREAT);
    }


    public void EditTreatmentRecord() {
        EditText compl = (EditText) findViewById(R.id.complaint);
        compl.setText(mCurrTreatment.complaint);
        EditText pres = (EditText) findViewById(R.id.prescription);
        pres.setText(mCurrTreatment.prescription);
        EditText date = (EditText) findViewById(R.id.treat_date);
        date.setText(mCurrTreatment.date);

        Button savupd = (Button) findViewById(R.id.treat_sav_upd);
        savupd.setText("Update");
        Button  cancdel = (Button) findViewById(R.id.treat_canc_del);
        cancdel.setText("Delete");
        update_mode(Mode.EDIT_TREAT);
    }


    public void SaveTreatmentRecord(View view) {
        EditText comp, pres, date;

        comp = (EditText)findViewById(R.id.complaint);
        pres = (EditText)findViewById(R.id.prescription);
        date = (EditText)findViewById(R.id.treat_date);

        if (comp.getText().toString().length() == 0 && pres.getText().toString().length() == 0) {
            return;
        }

        if (mMode == Mode.ADD_TREAT) {
            Treatment treat = new Treatment(mCurrPatient, "", comp.getText().toString(),
                    pres.getText().toString(), mDoctor.name );
            treat.date = date.getText().toString();
            mTreatmentDB.AddTreatment(treat);
            Log.d(TAG, "Added treatment for " + treat.patient.Name);
        }
        else if (mMode == Mode.EDIT_TREAT) {
            Treatment treat = new Treatment(mCurrPatient, mCurrTreatment.tid,
                    comp.getText().toString(), pres.getText().toString(), mDoctor.name);
            treat.date = date.getText().toString();
            mTreatmentDB.UpdateTreatment(treat);
            Log.d(TAG, "Updated treatment for " + treat.patient.Name);
        }

        this.onBackPressed();
    }

    public void CancDelTreatmentEdit(View view) {

        if (mMode == Mode.ADD_TREAT) {
            this.onBackPressed();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure to delete this treatment?");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                // User clicked ok button
                mTreatmentDB.DeleteTreatment(mCurrTreatment);
                TreatmentAddEditActivity.this.onBackPressed();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog - so wait (do nothing)
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void update_mode(Mode mode) {
        Log.d(TAG, "mMode (old) = " + mMode + ", mMode (new) = " + mode);
        mMode = mode;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.titlebar_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_save:
                SaveTreatmentRecord(findViewById(android.R.id.content));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
