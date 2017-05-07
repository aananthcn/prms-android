package com.nonprofit.aananth.prms;

import android.content.DialogInterface;
import android.content.res.Configuration;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.String;
import java.util.Arrays;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    // Aananth added this
    public enum Mode {NORMAL, ADD_PAT, UPDATE_PAT, VIEW_TREAT, ADD_TREAT, UPDATE_TREAT}

    // Aananth added these member variables
    private int currLayout;
    private List doctors = Arrays.asList("Jegadish", "Rama");
    private EditText user, password, patname, patphone, patmail;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private PatRecyclerAdapter mPatRcAdapter;
    private List<Patient> patientList;
    private Patient mCurrPatient;
    private PatientDB patientDB;
    private TreatRecyclerAdapter mTreatRcAdapter;
    private List<Treatment> treatmentList;
    private Treatment mCurrTreatment;
    private TreatmentDB treatmentDB;
    private Mode mMode;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        // Example of a call to a native method
        TextView tv = (TextView) findViewById(R.id.message_txt);
        tv.setText(stringFromJNI());

        // Aananth added these lines
        currLayout = R.layout.login;
        patientDB = new PatientDB(this);
        treatmentDB = new TreatmentDB(this);
        mMode = Mode.NORMAL;
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
                Log.d("Main Activity", "Successful login");
                currLayout = R.layout.patients;
                setContentView(currLayout);
            }
            else {
                Log.d("Main Activity", "Failure! Password: " + password.getText().toString());
            }
        }
        else {
            Log.d("Main Activity", "Failure! Username: " + user.getText().toString());
        }

        // remove these lines
        Log.d("Main Activity", "Successful login");
        patientList = patientDB.GetPatientList(null);
        renderPatRecycleView(patientList);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d("Main Activity", "Layout set to current layout");
        setContentView(currLayout);
    }

    // Added by Aananth: button handlers
    public void SearchPatNamePhone(View view) {
        EditText srchtxt;
        String srchstr;

        srchtxt = (EditText) findViewById(R.id.search_txt);

        Log.d("Main Activity", "Search patient records");
        srchstr = srchtxt.getText().toString();
        patientList = patientDB.GetPatientList(srchstr);
        renderPatRecycleView(patientList);

        Button srchbtn = (Button) findViewById(R.id.search_btn);
        if (srchstr.length() > 0) {
            srchbtn.setText("Refresh");
        } else {
            srchbtn.setText("Search");
        }
    }

    public void AddNewPatient(View view) {
        currLayout = R.layout.add_edit_pat;
        setContentView(currLayout);
        setTitle("Add patient");

        Button delbtn = (Button) findViewById(R.id.pat_del);
        delbtn.setVisibility(View.INVISIBLE);
        mMode = Mode.ADD_PAT;
    }

    public void SavePatientRecord(View view) {
        String gender;
        RadioButton patgender;

        patname = (EditText)findViewById(R.id.fullName);
        patphone = (EditText)findViewById(R.id.phoneNo);
        patmail = (EditText)findViewById(R.id.emailID);
        patgender = (RadioButton)findViewById(R.id.radioMale);

        if (patgender.isChecked())
            gender = "Male";
        else
            gender = "Female";

        if (mMode == Mode.ADD_PAT) {
            Patient pat = new Patient(patname.getText().toString(), patphone.getText().toString(),
                    patmail.getText().toString(), gender, "", "");
            patientDB.AddPatient(pat);
            Log.d("Main Activity", "Added patient '" + pat.Name + "'");
        }
        else if (mMode == Mode.UPDATE_PAT) {
            Patient pat = new Patient(patname.getText().toString(), patphone.getText().toString(),
                    patmail.getText().toString(), gender, mCurrPatient.Pid, mCurrPatient.Uid);
            patientDB.UpdatePatient(pat);
            Log.d("Main Activity", " Updated patient '" + pat.Name + "'");
        }
        patientList = patientDB.GetPatientList(null);
        renderPatRecycleView(patientList);
        mMode = Mode.NORMAL;
    }

    public void CancelPatientRecordEdit(View view) {
        patientList = patientDB.GetPatientList(null);
        renderPatRecycleView(patientList);
        mMode = Mode.NORMAL;
    }

    public void DeleteCurrPatient(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure to delete this patient?");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked ok button
                        patientDB.DeletePatient(mCurrPatient);
                        patientList = patientDB.GetPatientList(null);
                        renderPatRecycleView(patientList);
                    }
                });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                        patientList = patientDB.GetPatientList(null);
                        renderPatRecycleView(patientList);
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
        mMode = Mode.NORMAL;
    }

    // interface class
    public static abstract class ClickListener{
        public abstract void onClick(View view, int position);
        public abstract void onLongClick(View view, int position);
    }

    public void renderPatRecycleView(final List<Patient> patlist) {
        // set patient list layout
        currLayout = R.layout.patients;
        setContentView(currLayout);
        setTitle("Patient List");

        mRecyclerView = (RecyclerView) findViewById(R.id.pat_rv);
        if (mRecyclerView == null) {
            Log.d("Main Activity", "renderPatRecycleView: mRecylerView is null!!");
            return;
        }
        mLinearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        mPatRcAdapter = new PatRecyclerAdapter(patientList);
        mRecyclerView.addItemDecoration(new DividerItemDecorator(this, LinearLayoutManager.VERTICAL));
        mRecyclerView.setAdapter(mPatRcAdapter);

        mPatRcAdapter.notifyDataSetChanged();

        mRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(this,
                mRecyclerView, new ClickListener() {
            @Override
            public void onClick(View view, final int position) {
                // show patient history view
                Toast.makeText(MainActivity.this, "Pos :"+position+" - "+patlist.get(position).Name,
                        Toast.LENGTH_LONG).show();
                mCurrPatient = patlist.get(position);
                treatmentList = treatmentDB.GetTreatmentList(mCurrPatient);
                renderTreatRecycleView(treatmentList);
                mMode = Mode.VIEW_TREAT;
            }

            @Override
            public void onLongClick(View view, int position) {
                // edit patient data
                mMode = Mode.UPDATE_PAT;
                currLayout = R.layout.add_edit_pat;
                setContentView(currLayout);
                setTitle("Update patient");

                mCurrPatient = patlist.get(position);
                patname = (EditText) findViewById(R.id.fullName);
                patname.setText(mCurrPatient.Name);
                patphone = (EditText) findViewById(R.id.phoneNo);
                patphone.setText(mCurrPatient.Phone);
                patmail = (EditText) findViewById(R.id.emailID);
                patmail.setText(mCurrPatient.Email);
                if (mCurrPatient.Gender == "Male") {
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
        }));
    }


    public void renderTreatRecycleView(final List<Treatment> treatlist) {
        // set treatment list layout
        currLayout = R.layout.treatments;
        setContentView(currLayout);
        setTitle(treatlist.get(0).patient.Name+"'s history");

        mRecyclerView = (RecyclerView) findViewById(R.id.treat_rv);
        if (mRecyclerView == null) {
            Log.d("Main Activity", "renderTreatRecycleView: mRecylerView is null!!");
            return;
        }
        mLinearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        mTreatRcAdapter = new TreatRecyclerAdapter(treatlist);
        mRecyclerView.addItemDecoration(new DividerItemDecorator(this, LinearLayoutManager.VERTICAL));
        mRecyclerView.setAdapter(mTreatRcAdapter);

        mTreatRcAdapter.notifyDataSetChanged();

        mRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(this,
                mRecyclerView, new ClickListener() {
            @Override
            public void onClick(View view, final int position) {
                // edit treatment
                mCurrTreatment = treatlist.get(position);
                EditTreatmentRecord();
            }

            @Override
            public void onLongClick(View view, int position) {
                // edit patient data
                Toast.makeText(MainActivity.this, "onLongClick - Pos :"+ position,
                        Toast.LENGTH_LONG).show();
            }
        }));
    }

    public void onBackPressed() {
        if (mMode == Mode.UPDATE_TREAT || mMode == Mode.ADD_TREAT) {
            mMode = Mode.VIEW_TREAT;
            treatmentList = treatmentDB.GetTreatmentList(mCurrPatient);
            renderTreatRecycleView(treatmentList);
        }
        else {
            patientList = patientDB.GetPatientList(null);
            renderPatRecycleView(patientList);
            mMode = Mode.NORMAL;
        }
    }

    public void AddNewTreatment(View view) {
        currLayout = R.layout.add_edit_treat;
        setContentView(currLayout);

        String title = "New treatment for " + mCurrPatient.Name;
        setTitle(title);
        mMode = Mode.ADD_TREAT;
    }

    public void SaveTreatmentRecord(View view) {
        EditText comp, pres;

        comp = (EditText)findViewById(R.id.complaint);
        pres = (EditText)findViewById(R.id.prescription);

        if (mMode == Mode.ADD_TREAT) {
            Treatment treat = new Treatment(mCurrPatient, "", comp.getText().toString(),
                    pres.getText().toString());
            treatmentDB.AddTreatment(treat);
            Log.d("Main Activity", "Added treatment for " + treat.patient.Name);
        }
        else if (mMode == Mode.UPDATE_TREAT) {
            Treatment treat = new Treatment(mCurrPatient, mCurrTreatment.tid,
                    comp.getText().toString(), pres.getText().toString());
            treat.date = mCurrTreatment.date; //retain old date
            treatmentDB.UpdateTreatment(treat);
            Log.d("Main Activity", "Updated treatment for " + treat.patient.Name);
        }

        treatmentList = treatmentDB.GetTreatmentList(mCurrPatient);
        renderTreatRecycleView(treatmentList);
        mMode = Mode.VIEW_TREAT;
    }

    public void EditTreatmentRecord() {
        currLayout = R.layout.add_edit_treat;
        setContentView(currLayout);

        String title = "Update treatment for " + mCurrPatient.Name;
        setTitle(title);
        mMode = Mode.UPDATE_TREAT;

        EditText compl = (EditText) findViewById(R.id.complaint);
        compl.setText(mCurrTreatment.complaint);
        EditText pres = (EditText) findViewById(R.id.prescription);
        pres.setText(mCurrTreatment.prescription);

        Button  savupd = (Button) findViewById(R.id.treat_sav_upd);
        savupd.setText("Update");
        Button  cancdel = (Button) findViewById(R.id.treat_canc_del);
        cancdel.setText("Delete");
    }

    public void CancelTreatmentEdit(View view) {
        // print delete option if in update treatment mode!!
        if (mMode == Mode.UPDATE_TREAT) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Are you sure to delete this treatment?");
            builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User clicked ok button
                    treatmentDB.DeleteTreatment(mCurrTreatment);
                    mMode = Mode.VIEW_TREAT;
                    treatmentList = treatmentDB.GetTreatmentList(mCurrPatient);
                    renderTreatRecycleView(treatmentList);
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User cancelled the dialog
                }
            });

            AlertDialog dialog = builder.create();
            dialog.show();
        }

        onBackPressed();
    }
}
