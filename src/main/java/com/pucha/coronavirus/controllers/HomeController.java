package com.pucha.coronavirus.controllers;

import com.pucha.coronavirus.models.LocationStats;
import com.pucha.coronavirus.models.TimeSeries;
import com.pucha.coronavirus.services.CoronaVirusDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    @Autowired
    CoronaVirusDataService coronaVirusDataService;

    @GetMapping("/")
    public String home(Model model) {

        model.addAttribute("locationStats", coronaVirusDataService.getAllStats());
        model.addAttribute("totalConfirmedCases", coronaVirusDataService.getTotalConfirmedCases());
        model.addAttribute("newConfirmedCases", coronaVirusDataService.getNewConfirmedCases());
        model.addAttribute("fetchDate", coronaVirusDataService.getFetchDate());

        return "home";
    }

    @GetMapping("/plot")
    public String plot(Model model, @RequestParam String country){

        TimeSeries countryTimeSeries = coronaVirusDataService.getTimeSeries(country);
        model.addAttribute("country", countryTimeSeries.getCountry());
        model.addAttribute("lineChartData", coronaVirusDataService.getLineChartData(countryTimeSeries));
        model.addAttribute("columnChartConfirmed", coronaVirusDataService
                .getColumnChartData(countryTimeSeries, "confirmed"));
        model.addAttribute("columnChartDeaths", coronaVirusDataService
                .getColumnChartData(countryTimeSeries, "deaths"));

        return "plot";
    }
}
