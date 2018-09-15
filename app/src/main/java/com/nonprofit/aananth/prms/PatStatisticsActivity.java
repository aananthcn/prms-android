package com.nonprofit.aananth.prms;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import java.util.Locale;

public class PatStatisticsActivity extends AppCompatActivity {

    private PatStatistics mPatStatistics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.statistics_pat);
        setTitle("Patient Statistics");


        TextView no_of_pat_v = (TextView) findViewById(R.id.total_pat);
        TextView male_pat_v = (TextView) findViewById(R.id.male_pat);
        TextView female_pat_v = (TextView) findViewById(R.id.female_pat);
        TextView top_pat_v = (TextView) findViewById(R.id.top_pat);


        Intent intent = getIntent();
        mPatStatistics = (PatStatistics) intent.getSerializableExtra("patient statistics");

        no_of_pat_v.setText(String.format(Locale.US, "%d", mPatStatistics.TotalPatients));

        String males = String.format(Locale.US, "%.2f", (100.0 * mPatStatistics.MalePatients)/mPatStatistics.TotalPatients) + "% Males  | ";
        male_pat_v.setText(males);

        String females = String.format(Locale.US, "%.2f", (100.0 * mPatStatistics.FemalePatients)/mPatStatistics.TotalPatients) + "% Females";
        female_pat_v.setText(females);

        String top_pat = mPatStatistics.TopPatient + " is the Top Patient with " +  mPatStatistics.TopPatientVisits + " visits";
        top_pat_v.setText(top_pat);
    }
}
