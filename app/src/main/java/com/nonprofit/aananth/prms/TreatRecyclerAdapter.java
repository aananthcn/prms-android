package com.nonprofit.aananth.prms;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

/**
 * Created by aananth on 06/05/17.
 */


public class TreatRecyclerAdapter extends RecyclerView.Adapter<TreatRecyclerAdapter.ViewHolder> {
    private List<Treatment> treatmentList;

    // http://www.androidhive.info/2016/01/android-working-with-recycler-view/
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView compl, date, treat;

        public ViewHolder(View view) {
            super(view);
            compl = (TextView) view.findViewById(R.id.item_compl);
            date = (TextView) view.findViewById(R.id.item_date);
            treat = (TextView) view.findViewById(R.id.item_treat);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public TreatRecyclerAdapter(List<Treatment> treatList) {
        this.treatmentList = treatList;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public TreatRecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                              int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.treat_item_row,
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
        Treatment treatment = treatmentList.get(position);
        holder.compl.setText(treatment.complaint);
        holder.date.setText(treatment.date);
        holder.treat.setText(treatment.prescription);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return treatmentList.size();
    }
}
