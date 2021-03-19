package com.pucha.coronavirus.services;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

public class NationalDataFetcher {

    private String POPULATION_DATA_URL;
    private HttpClient client;

    public NationalDataFetcher(String URL, HttpClient client) {
        this.POPULATION_DATA_URL = URL;
        this.client = client;
    }

    public Map<String, Double> fetchPopulations() throws IOException, InterruptedException {
        HttpRequest requestData = HttpRequest.newBuilder()
                .uri(URI.create(POPULATION_DATA_URL))
                .build();
        HttpResponse<String> httpResponseData = client.send(requestData, HttpResponse.BodyHandlers.ofString());
        StringReader stringreader = new StringReader(httpResponseData.body());
        BufferedReader bufferedReader = new BufferedReader(stringreader);
        for (int i = 0; i < 3; i++) {
            bufferedReader.readLine();
        }
        CSVParser parser = CSVParser.parse(bufferedReader, CSVFormat.DEFAULT.withFirstRecordAsHeader());

        Map<String,Double> countryPopulations = new HashMap<>();
        for (CSVRecord record: parser) {
            if (record.get("Type").equals("Country")) {
                String country = record.get("Name");
                switch(country) {
                    case "United States":
                        country = "US";
                        break;
                    case "Bosnia-Herzegovina":
                        country = "Bosnia and Herzegovina";
                        break;
                    case "Taiwan":
                        country = "Taiwan*";
                        break;
                    case "Palestinian Territory":
                        country = "West Bank and Gaza";
                        break;
                    case "Congo":
                        country = "Congo (Brazzaville)";
                        break;
                    case "Congo, Dem. Rep.":
                        country = "Congo (Kinshasa)";
                        break;
                    case "Myanmar":
                        country = "Burma";
                        break;
                    case "St. Kitts-Nevis":
                        country = "Saint Kitts and Nevis";
                        break;
                    case "eSwatini":
                        country = "Eswatini";
                        break;
                    case "Cape Verde":
                        country = "Cabo Verde";
                        break;
                    case "St. Vincent and the Grenadines":
                        country = "Saint Vincent and the Grenadines";
                        break;
                    case "Federated States of Micronesia":
                        country = "Micronesia";
                        break;
                }
                countryPopulations.put(country, Double.parseDouble(record.get("Data")));
            }else if (record.get("Type").equals("World")) {
                countryPopulations.put("World", Double.parseDouble(record.get("Data")));
            }
            countryPopulations.put("Diamond Princess", 0.003711);
            countryPopulations.put("MS Zaandam", 0.001052);
            countryPopulations.put("Holy See", 0.000618);
        }
        return countryPopulations;
    }
}
