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
    private List<Patient> mPatientList;
    private PatientDB mPatientDB;
    private Patient mCurrPatient;
    private Doctor mDoctor;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private PatRecyclerAdapter mPatRcAdapter;

    private SearchTypes mSearchType;
    private String mPrescription, mComplaint, mMessage;
    private Boolean initialPatRenderingDone = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.complaint_search);
        Log.d(TAG, "onCreate()");

        //do basic initialization of objects
        mPatientDB = new PatientDB(this);

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        mMessage = intent.getStringExtra(EXTRA_MESSAGE);
        mDoctor = (Doctor) intent.getSerializableExtra("doctor");
        mPatientList = mPatientDB.GetPatientListWith(null, null);
    }


    private void getSearchInputs() {
        final EditText srchBox = (EditText) findViewById(R.id.search_txt);
        srchBox.setHint(findSearchType(mMessage));
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
                                mComplaint = srchstr;
                                Log.d(TAG, "onCreate::mComplaint = " + mComplaint );
                            }
                            else if (mSearchType == SearchTypes.PRESCRIPTION_SEARCH) {
                                SearchPatientsTreatedWith(srchstr);
                                mPrescription = srchstr;
                                Log.d(TAG, "onCreate::mPrescription = " + mPrescription );
                            }
                            else {
                                Log.d(TAG, "SearchTypes.INVALID_SEARCH");
                            }
                            handled = true;
                        default:
                            //Log.d(TAG, " setOnKeyListener search patient: unknown key " + keyCode);
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
        if (mPatientList.size() > 0) {
            mPatientList.clear();
        }
        mPatientList.addAll(mPatientDB.GetPatientListWith(null, prescription));
        Log.d(TAG, "Search patient records for prescription");
        if (mPatientList.size() == 0) {
            Toast.makeText(getBaseContext(), "No patients found", Toast.LENGTH_SHORT).show();
        }

        if (initialPatRenderingDone) {
            refreshPatRecycleView();
        }
        else {
            renderPatRecycleView();
        }
        hideKeyboard();
    }

    private void SearchPatientsWithComplaint(String complaint) {
        if (mPatientList.size() > 0) {
            mPatientList.clear();
        }
        mPatientList.addAll(mPatientDB.GetPatientListWith(complaint, null));
        Log.d(TAG, "Search patient records for complaints");
        if (mPatientList.size() == 0) {
            Toast.makeText(getBaseContext(), "No patients found", Toast.LENGTH_SHORT).show();
        }

        if (initialPatRenderingDone) {
            refreshPatRecycleView();
        }
        else {
            renderPatRecycleView();
        }
        hideKeyboard();
    }


    public void renderPatRecycleView() {
        mRecyclerView = (RecyclerView) findViewById(R.id.pat_rv);
        if (mRecyclerView == null) {
            Log.d(TAG, "renderPatRecycleView: mRecylerView is null!!");
            return;
        }
        mLinearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        mPatRcAdapter = new PatRecyclerAdapter(mPatientList);
        mRecyclerView.addItemDecoration(new DividerItemDecorator(this, LinearLayoutManager.VERTICAL));
        mRecyclerView.setAdapter(mPatRcAdapter);

        mPatRcAdapter.notifyDataSetChanged();
        initialPatRenderingDone = true;

        mRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(this,
                mRecyclerView, new MainActivity.ClickListener() {
            @Override
            public void onClick(View view, final int position) {
                if (mPatientList.isEmpty()) {
                    Log.d(TAG, "mPatientList Empty!");
                    return;
                }

                // show patient history view
                mCurrPatient = mPatientList.get(position);
                if (!mCurrPatient.Name.equals("Empty")) {
                    Intent intent = new Intent(SearchActivity.this, TreatmentViewActivity.class);
                    intent.putExtra("patient", mCurrPatient);
                    intent.putExtra("doctor", mDoctor);
                    startActivity(intent);
                }
            }

            @Override
            public void onLongClick(View view, int position) {
                // do nothing while searching!s
            }
        }));
    }


    public void refreshPatRecycleView() {
        // It is assumed that mPatientList is updated with latest content at this point.
        // Hence, we always call notifyDataSetChanged() here unconditionally !!
        if (mSearchType == SearchTypes.COMPLAINT_SEARCH) {
            setTitle("Patients with Complaints");
            Log.d(TAG, "mComplaint = " + mComplaint);
        }
        else if (mSearchType == SearchTypes.PRESCRIPTION_SEARCH) {
            setTitle("Patients taking Treatments");
            Log.d(TAG, "mPrescription = " + mPrescription );
        }
        mPatRcAdapter.notifyDataSetChanged();
        Log.d(TAG, "refreshPatRecycleView()");
    }


    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "onResume()");

        if (initialPatRenderingDone) {
            refreshPatRecycleView();
        }
        else {
            getSearchInputs();
        }
    }


    private void hideKeyboard() {
        Context context = SearchActivity.this;
        View view = getCurrentFocus().getRootView();

        InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
