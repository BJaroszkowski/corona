package com.pucha.coronavirus.models;

import java.util.List;

public class TimeSeries {
    private String country;
    private String countrymin;
    private List<Integer> deaths;
    private List<Integer> confirmed;

    public String getCountrymin() {
        return countrymin;
    }


    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
        this.countrymin = country.replaceAll("[\\s,*()]","").toLowerCase();
    }

    public List<Integer> getConfirmed() {
        return confirmed;
    }

    public void setConfirmed(List<Integer> confirmed) {
        this.confirmed = confirmed;
    }

    public List<Integer> getDeaths() {
        return deaths;
    }

    public void setDeaths(List<Integer> deaths) {
        this.deaths = deaths;
    }

}
