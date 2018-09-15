package com.nonprofit.aananth.prms;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import java.io.Serializable;
import java.util.List;

import static android.provider.AlarmClock.EXTRA_MESSAGE;

public class TreatmentViewActivity extends AppCompatActivity implements Serializable{

    private String TAG = "PRMS-TreatmentViewActivity";

    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private TreatRecyclerAdapter mTreatRcAdapter;
    private Boolean mRcVwInitialized = false;
    private Boolean mRcVwUpdateNeeded = false;

    private Patient mCurrPatient;
    private Doctor mDoctor;
    private List<Treatment> mTreatmentList;
    private Treatment mCurrTreatment;
    private TreatmentDB mTreatmentDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.treatments);
        Log.d(TAG, "Entering onCreate()");

        //do basic initialization of objects
        mTreatmentDB = new TreatmentDB(this);
        Intent intent = getIntent();
        mCurrPatient = (Patient) intent.getSerializableExtra("patient");
        mDoctor = (Doctor) intent.getSerializableExtra("doctor");

        setTitle("Treatments - " + mCurrPatient.Name);
        mTreatmentList = mTreatmentDB.GetTreatmentList(mCurrPatient, ListOrder.REVERSE);
        renderTreatRecycleView();
        mRcVwInitialized = true;
    }


    public void renderTreatRecycleView() {
        //update_mode(Mode.VIEW_TREAT);
        mRecyclerView = (RecyclerView) findViewById(R.id.treat_rv);
        if (mRecyclerView == null) {
            Log.d(TAG, "renderTreatRecycleView: mRecylerView is null!!");
            return;
        }
        mLinearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        mTreatRcAdapter = new TreatRecyclerAdapter(mTreatmentList);
        mRecyclerView.addItemDecoration(new DividerItemDecorator(this, LinearLayoutManager.VERTICAL));
        mRecyclerView.setAdapter(mTreatRcAdapter);

        mTreatRcAdapter.notifyDataSetChanged();

        mRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(this,
                mRecyclerView, new MainActivity.ClickListener() {
            @Override
            public void onClick(View view, final int position) {
                Log.d(TAG, "onClick()");
                // share treatment
                mCurrTreatment = mTreatmentList.get(position);
                registerForContextMenu(view);
                openContextMenu(view);
                view.showContextMenu();
                unregisterForContextMenu(view);
            }

            @Override
            public void onLongClick(View view, int position) {
                // edit treatment
                mCurrTreatment = mTreatmentList.get(position);
                EditTreatmentRecord();
            }
        }));
    }


    public void AddNewTreatment(View view) {
        Intent intent = new Intent(TreatmentViewActivity.this, TreatmentAddEditActivity.class);
        intent.putExtra("patient", mCurrPatient);
        intent.putExtra("doctor", mDoctor);
        intent.putExtra(EXTRA_MESSAGE, "add treatment");
        Log.d(TAG, "Starting TreatmentAdd/EditActivity from AddNewTreatment()");
        startActivity(intent);
        mRcVwUpdateNeeded = true;
    }


    public void EditTreatmentRecord() {
        Intent intent = new Intent(TreatmentViewActivity.this, TreatmentAddEditActivity.class);
        intent.putExtra("patient", mCurrPatient);
        intent.putExtra("doctor", mDoctor);
        intent.putExtra("treatment", mCurrTreatment);
        intent.putExtra(EXTRA_MESSAGE, "edit treatment");
        Log.d(TAG, "Starting TreatmentAdd/EditActivity from EditTreatmentRecord()");
        startActivity(intent);
        mRcVwUpdateNeeded = true;
    }


    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "onResume()");
        if ((mRcVwInitialized) && (mRcVwUpdateNeeded)) {
            Log.d(TAG, "Notifying Treatment Recycle View Adapter");
            mTreatmentList.clear();
            mTreatmentList.addAll(mTreatmentDB.GetTreatmentList(mCurrPatient, ListOrder.REVERSE));
            mTreatRcAdapter.notifyDataSetChanged();
            mRcVwUpdateNeeded = false;
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.share_menu, menu);
        menu.setHeaderTitle("Share Treatment");
    }


    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch(item.getItemId()) {
            case R.id.share_whatsapp:
                Intent wtspIntent = new Intent(Intent.ACTION_SEND);
                wtspIntent.setType("text/plain");
                wtspIntent.setPackage("com.whatsapp");
                wtspIntent.putExtra(Intent.EXTRA_TEXT, "*Date*: "+ mCurrTreatment.date
                        + "\n\n*Patient:* "+ mCurrTreatment.patient.Name
                        + "\n\n*Complaint*: "+ mCurrTreatment.complaint
                        + "\n\n*Prescription*: "+ mCurrTreatment.prescription);
                try {
                    this.startActivity(wtspIntent);
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(getBaseContext(), "WhatsApp have not been installed.",
                            Toast.LENGTH_SHORT).show();
                }
                return true;
            case R.id.share_sms:
                Intent smsIntent = new Intent(Intent.ACTION_VIEW);
                smsIntent.setType("vnd.android-dir/mms-sms");
                smsIntent.putExtra("address", mCurrTreatment.patient.Phone);
                smsIntent.putExtra("sms_body", mCurrTreatment.prescription);
                try {
                    this.startActivity(smsIntent);
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(getBaseContext(), "Couldn't send SMS",
                            Toast.LENGTH_SHORT).show();
                }
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }
}
