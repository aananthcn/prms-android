package com.nonprofit.aananth.prms;

import java.io.Serializable;

public class PatStatistics implements Serializable {
    int TotalPatients;
    int MalePatients;
    int FemalePatients;
    String TopPatient;
    int TopPatientVisits;

    public PatStatistics(int total, int males, int females, String topPatient, int top_visits){
        TotalPatients = total;
        MalePatients = males;
        FemalePatients = females;
        TopPatient = topPatient;
        TopPatientVisits = top_visits;
    }
}
