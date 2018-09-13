package com.nonprofit.aananth.prms;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import java.util.List;

import static android.provider.AlarmClock.EXTRA_MESSAGE;

public class SearchActivity extends AppCompatActivity {

    public enum SearchTypes {
        COMPLAINT_SEARCH,
        PRESCRIPTION_SEARCH,
        INVALID_SEARCH
    }

    private String TAG = "PRMS-SearchActivity";
    private List<Patient> patientList;
    private PatientDB patientDB;
    private Patient mCurrPatient;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private PatRecyclerAdapter mPatRcAdapter;

    private SearchTypes mSearchType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.complaint_search);

        //do basic initialization of objects
        patientDB = new PatientDB(this);

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        String message = intent.getStringExtra(EXTRA_MESSAGE);

        final EditText srchBox = (EditText) findViewById(R.id.search_txt);
        //srchBox.invalidate();
        srchBox.setHint(findSearchType(message));
        srchBox.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                boolean handled = false;
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    switch (keyCode)
                    {
                        case KeyEvent.KEYCODE_DPAD_CENTER:
                        case KeyEvent.KEYCODE_ENTER:
                        case KeyEvent.KEYCODE_SEARCH:
                            String srchstr = srchBox.getText().toString();
                            if (mSearchType == SearchTypes.COMPLAINT_SEARCH) {
                                SearchPatientsWithComplaint(srchstr);
                            }
                            else if (mSearchType == SearchTypes.PRESCRIPTION_SEARCH) {
                                SearchPatientsTreatedWith(srchstr);
                            }
                            else {
                                Log.d(TAG, "SearchTypes.INVALID_SEARCH");
                            }
                            handled = true;
                        default:
                            Log.d(TAG, " setOnKeyListener search patient: unknown key " + keyCode);
                            break;
                    }
                }
                return handled;

            }
        });
    }

    private String findSearchType(String message) {
        String searchBoxStr;

        if (message.contains("complaint search")) {
            setTitle("Patients with complaint:");
            searchBoxStr = "Type the complaint or symptom";
            mSearchType = SearchTypes.COMPLAINT_SEARCH;
        }
        else if (message.contains("prescription search")) {
            setTitle("Patients treated with:");
            searchBoxStr = "Type the medicine or treatment";
            mSearchType = SearchTypes.PRESCRIPTION_SEARCH;
        }
        else {
            setTitle(message);
            searchBoxStr = "Bug: report this to the author!";
            mSearchType = SearchTypes.INVALID_SEARCH;
        }

        return searchBoxStr;
    }

    private void SearchPatientsTreatedWith(String prescription) {
        Log.d(TAG, "Search patient records for prescription");
        patientList = patientDB.GetPatientListWith(null, prescription);
        renderPatRecycleView(patientList);
        hideKeyboard();
    }

    private void SearchPatientsWithComplaint(String complaint) {
        Log.d(TAG, "Search patient records for complaints");
        patientList = patientDB.GetPatientListWith(complaint, null);
        renderPatRecycleView(patientList);
        hideKeyboard();
    }


    public void renderPatRecycleView(final List<Patient> patlist) {
        mRecyclerView = (RecyclerView) findViewById(R.id.pat_rv);
        if (mRecyclerView == null) {
            Log.d(TAG, "renderPatRecycleView: mRecylerView is null!!");
            return;
        }
        mLinearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        mPatRcAdapter = new PatRecyclerAdapter(patientList);
        mRecyclerView.addItemDecoration(new DividerItemDecorator(this, LinearLayoutManager.VERTICAL));
        mRecyclerView.setAdapter(mPatRcAdapter);

        mPatRcAdapter.notifyDataSetChanged();

        mRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(this,
                mRecyclerView, new MainActivity.ClickListener() {
            @Override
            public void onClick(View view, final int position) {
                // show patient history view
                mCurrPatient = patlist.get(position);
                if (!mCurrPatient.Name.equals("Empty")) {
                    //treatmentList = treatmentDB.GetTreatmentList(mCurrPatient, ListOrder.REVERSE);
                    //renderTreatRecycleView(treatmentList);
                }
            }

            @Override
            public void onLongClick(View view, int position) {
                // edit patient data
                mCurrPatient = patlist.get(position);
                //EditPatientRecord();
            }
        }));
    }

    public void hideKeyboard() {
        Context context = SearchActivity.this;
        View view = getCurrentFocus().getRootView();

        InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
