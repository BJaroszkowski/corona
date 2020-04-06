package com.pucha.coronavirus.models;

import java.text.DecimalFormat;

public class LocationStats {
    private String country;
    private String countrymin;
    private int casesPer100k;
    private int latestTotalCases;
    private int casesDiff;
    private int deathsDiff;
    private int deaths;
    private int recovered;


    public double getDeathRatio() {
        double ratio = (double) deaths/latestTotalCases * 100;
        return Math.round(ratio*100)/100.0;
    }

    public int getDeathsDiff() {
        return deathsDiff;
    }

    public void setDeathsDiff(int deathsDiff) {
        this.deathsDiff = deathsDiff;
    }

    public void setCountrymin(String countrymin) {
        this.countrymin = countrymin;
    }

    public int getRecovered() {
        return recovered;
    }

    public void setRecovered(int recovered) {
        this.recovered = recovered;
    }

    public String getCountrymin() {
        return countrymin;
    }

    public int getDeaths() {
        return deaths;
    }

    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }


    public int getCasesDiff() {
        return casesDiff;
    }

    public void setCasesDiff(int casesDiff) {
        this.casesDiff = casesDiff;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
        this.countrymin = country.replaceAll("[\\s,*()]","").toLowerCase();
    }

    public int getLatestTotalCases() {
        return latestTotalCases;
    }

    public void setLatestTotalCases(int latestTotalCases) {
        this.latestTotalCases = latestTotalCases;
    }

    public int getCasesPer100k() {
        return casesPer100k;
    }

    public void setCasesPer100k(int casesPer100k) {
        this.casesPer100k = casesPer100k;
    }

    @Override
    public String toString() {
        return "LocationStats{" +
                "country='" + country + '\'' +
                ", latestTotalCases=" + latestTotalCases +
                ", diffFromPrevDay=" + casesDiff +
                '}';
    }
}