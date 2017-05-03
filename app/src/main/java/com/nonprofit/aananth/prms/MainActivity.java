package com.nonprofit.aananth.prms;

import android.content.res.Configuration;
import android.database.Cursor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.String;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.os.SystemClock.sleep;


public class MainActivity extends AppCompatActivity {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    // Aananth added these member variables
    private int currLayout;
    private List doctors = Arrays.asList("Jegadish", "Rama");
    private EditText user, password, patname, patphone, patmail;
    private PatientDB patientdb;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private RecyclerAdapter mAdapter;
    List<Patient> patientList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        // Example of a call to a native method
        TextView tv = (TextView) findViewById(R.id.message_txt);
        tv.setText(stringFromJNI());

        // Aananth added these lines
        currLayout = R.layout.login;
        patientdb = new PatientDB(this);
        patientList = patientdb.GetPatientList(null);
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
        currLayout = R.layout.patients;
        setContentView(currLayout);

        // Aananth moved these lines from OnCreate()
        mRecyclerView = (RecyclerView) findViewById(R.id.pl_rv);
        if (mRecyclerView == null) {
            Log.d("Main Activity", "mRecylerView is null!!");
            return;
        }
        mLinearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        mAdapter = new RecyclerAdapter(patientList); // patientList is updated in OnCreate()
        mRecyclerView.addItemDecoration(new DividerItemDecorator(this, LinearLayoutManager.VERTICAL));
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.notifyDataSetChanged();

        mRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(this,
                mRecyclerView, new ClickListener() {
            @Override
            public void onClick(View view, final int position) {
                //Values are passing to activity & to fragment as well
                Toast.makeText(MainActivity.this, "Single Click on position        :"+position,
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLongClick(View view, int position) {
                Toast.makeText(MainActivity.this, "Long press on position :"+position,
                        Toast.LENGTH_LONG).show();
            }
        }));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d("Main Activity", "Layout set to current layout");
        setContentView(currLayout);
    }

    // Added by Aananth: Add button handler
    public void AddNewPatient(View view) {
        currLayout = R.layout.add_edit;
        setContentView(currLayout);
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

        patientdb.AddPatient(patname.getText().toString(), gender, patphone.getText().toString(),
                patmail.getText().toString());
        Log.d("Main Activity", "Saved patient records");
        currLayout = R.layout.patients;
        setContentView(currLayout);

        mRecyclerView = (RecyclerView) findViewById(R.id.pl_rv);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        patientList = patientdb.GetPatientList(null);
        mAdapter = new RecyclerAdapter(patientList);
        mRecyclerView.addItemDecoration(new DividerItemDecorator(this, LinearLayoutManager.VERTICAL));
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.notifyDataSetChanged();

        mRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(this,
                mRecyclerView, new ClickListener() {
            @Override
            public void onClick(View view, final int position) {
                //Values are passing to activity & to fragment as well
                Toast.makeText(MainActivity.this, "Single Click on position        :"+position,
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLongClick(View view, int position) {
                Toast.makeText(MainActivity.this, "Long press on position :"+position,
                        Toast.LENGTH_LONG).show();
            }
        }));
    }

    public void SearchPatNamePhone(View view) {
        EditText srchtxt;

        srchtxt = (EditText) findViewById(R.id.search_txt);

        Log.d("Main Activity", "Search patient records");
        currLayout = R.layout.patients;
        setContentView(currLayout);

        mRecyclerView = (RecyclerView) findViewById(R.id.pl_rv);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        patientList = patientdb.GetPatientList(srchtxt.getText().toString());
        mAdapter = new RecyclerAdapter(patientList);
        mRecyclerView.addItemDecoration(new DividerItemDecorator(this, LinearLayoutManager.VERTICAL));
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.notifyDataSetChanged();

        mRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(this,
                mRecyclerView, new ClickListener() {
            @Override
            public void onClick(View view, final int position) {
                //Values are passing to activity & to fragment as well
                Toast.makeText(MainActivity.this, "Single Click on position        :"+position,
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLongClick(View view, int position) {
                Toast.makeText(MainActivity.this, "Long press on position :"+position,
                        Toast.LENGTH_LONG).show();
            }
        }));
    }

    public static interface ClickListener{
        public void onClick(View view,int position);
        public void onLongClick(View view,int position);
    }



    public void RefreshPatientList() {
        String columns[] = {"name", "phone"};
        ArrayList<ArrayList<String>> retList = new ArrayList<ArrayList<String>>();
        ArrayList<String> list = new ArrayList<String>();
        Cursor cursor = patientdb.getReadableDatabase().query(patientdb.TABLE_PATIENTS,
                 columns, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                list = new ArrayList<String>();
                for(int i=0; i<cursor.getColumnCount(); i++) {
                    list.add( cursor.getString(i) );
                }
                retList.add(list);
            } while (cursor.moveToNext());
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        //return retList;
    }
}
