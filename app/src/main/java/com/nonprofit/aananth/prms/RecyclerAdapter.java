package com.nonprofit.aananth.prms;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

/**
 * Created by aananth on 09/04/17.
 */


public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {
    private List<Patient> patientList;

    // http://www.androidhive.info/2016/01/android-working-with-recycler-view/
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView name, phone, email;

        public ViewHolder(View view) {
            super(view);
            name = (TextView) view.findViewById(R.id.item_name);
            phone = (TextView) view.findViewById(R.id.item_phone);
            email = (TextView) view.findViewById(R.id.item_email);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public RecyclerAdapter(List<Patient> patList) {
        this.patientList = patList;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public RecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                   int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.pat_item_row,
                parent, false);
        // set the view's size, margins, paddings and layout parameters
        //...
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // http://www.androidhive.info/2016/01/android-working-with-recycler-view/
        Patient patient = patientList.get(position);
        holder.name.setText(patient.Name);
        holder.email.setText(patient.Email);
        holder.phone.setText(patient.Phone);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return patientList.size();
    }
}
