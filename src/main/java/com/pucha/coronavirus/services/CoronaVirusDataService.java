package com.pucha.coronavirus.services;

import com.pucha.coronavirus.models.LocationStats;
import com.pucha.coronavirus.models.TimeSeries;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Time;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CoronaVirusDataService {
    private DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM-dd-yyyy");
    private String fetchDate;
    private List<String> allDates;
    private String VIRUS_DATA_DAILY_URL = "https://raw.githubusercontent.com/CSSEGISandData/COVID-19/master/csse_covid_19_data/csse_covid_19_daily_reports/";
    private String VIRUS_DATA_CONFIRMED_URL = "https://raw.githubusercontent.com/CSSEGISandData/COVID-19/master/csse_covid_19_data/csse_covid_19_time_series/time_series_covid19_confirmed_global.csv";
    private String VIRUS_DATA_DEATHS_URL = "https://raw.githubusercontent.com/CSSEGISandData/COVID-19/master/csse_covid_19_data/csse_covid_19_time_series/time_series_covid19_deaths_global.csv";
    private String POPULATION_DATA_URL = "https://datacenter.prb.org/download/international/indicator/population/csv";
    private HttpClient client = HttpClient.newHttpClient();

    private List<LocationStats> allStats = new ArrayList<>();
    private List<TimeSeries> allTimeSeries = new ArrayList<>();

    @PostConstruct
    @Scheduled(cron = "0 0 * * * *")
    public void fetchVirusData() throws IOException, InterruptedException {
        System.out.println("Fetching new virus data");
        fetchTimeSeries();
        fetchLocationStats();
    }

    private void fetchLocationStats() throws IOException, InterruptedException {
        LocalDateTime now = LocalDateTime.now();
        fetchDate = dtf.format(now.minusDays(1));
        HttpRequest requestDaily = getDailyRequest(fetchDate);
        HttpResponse<String> httpResponseDaily = client.send(requestDaily, HttpResponse.BodyHandlers.ofString());
        if (httpResponseDaily.statusCode()==404){
            fetchDate = dtf.format(now.minusDays(2));
            requestDaily = getDailyRequest(fetchDate);
            httpResponseDaily = client.send(requestDaily, HttpResponse.BodyHandlers.ofString());
        }
        StringReader inDaily = new StringReader(httpResponseDaily.body());
        Iterable<CSVRecord> recordsDaily = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(inDaily);

        List<LocationStats> newStats = new ArrayList<>();
        for(CSVRecord record: recordsDaily) {
            String country = record.get("Country_Region");
            int latestTotalCases = Integer.parseInt(record.get("Confirmed"));
            int recovered = Integer.parseInt(record.get("Recovered"));
            int deaths = Integer.parseInt(record.get("Deaths"));
            LocationStats existing = newStats.stream()
                    .filter(stat -> stat.getCountry().equals(country))
                    .findFirst().orElse(null);

            LocationStats locationStat;
            if (existing != null){
                locationStat = existing;
                latestTotalCases += locationStat.getLatestTotalCases();
                deaths += locationStat.getDeaths();
                recovered += locationStat.getRecovered();

            }else {
                locationStat = new LocationStats();
                locationStat.setCountry(country);
            }

            locationStat.setLatestTotalCases(latestTotalCases);
            locationStat.setDeaths(deaths);
            locationStat.setRecovered(recovered);
            if (existing==null) newStats.add(locationStat);
        }
        for(TimeSeries timeSeries: allTimeSeries){
            String country = timeSeries.getCountry();
            List<Integer> casesSeries = timeSeries.getConfirmed();
            List<Integer> deathsSeries = timeSeries.getDeaths();
            int casesDiff = casesSeries.get(casesSeries.size() - 1) - casesSeries.get(casesSeries.size() - 2);
            int deathsDiff = deathsSeries.get(deathsSeries.size() - 1) - deathsSeries.get(deathsSeries.size() - 2);
            LocationStats locationStat  = newStats.stream()
                    .filter(stat -> stat.getCountry().equals(country))
                    .findFirst().orElse(null);
            if (locationStat!=null){
                locationStat.setCasesDiff(casesDiff);
                locationStat.setDeathsDiff(deathsDiff);
            }
        }
        NationalDataFetcher nationalDataFetcher = new NationalDataFetcher(POPULATION_DATA_URL, client);
        Map<String, Double> countryPopulations = nationalDataFetcher.fetchPopulations();
        for (LocationStats locationStats: newStats) {
            Double population = countryPopulations.get(locationStats.getCountry());
            locationStats.setCasesPer100k((int) (locationStats.getLatestTotalCases()/(population*10)));
        }

        LocationStats world = new LocationStats();
        world.setCountry("World");
        world.setCountrymin("world");
        world.setLatestTotalCases(newStats.stream().mapToInt(LocationStats::getLatestTotalCases).sum());
        world.setCasesDiff(newStats.stream().mapToInt(LocationStats::getCasesDiff).sum());
        world.setDeaths(newStats.stream().mapToInt(LocationStats::getDeaths).sum());
        world.setDeathsDiff(newStats.stream().mapToInt(LocationStats::getDeathsDiff).sum());
        world.setCasesPer100k((int) (world.getLatestTotalCases()/(countryPopulations.get("World")*10)));
        world.setRecovered(newStats.stream().mapToInt(LocationStats::getRecovered).sum());
        newStats.add(world);

        newStats.sort(Comparator.comparingInt(LocationStats::getLatestTotalCases).reversed());
        this.allStats = newStats;
    }

    private StringReader getContent(String URL) throws IOException, InterruptedException {
        HttpRequest requestData = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .build();
        HttpResponse<String> httpResponseData = client.send(requestData, HttpResponse.BodyHandlers.ofString());
        return new StringReader(httpResponseData.body());
    }

    private void fetchTimeSeries() throws IOException, InterruptedException {
        List<TimeSeries> newTimeSeries = new ArrayList<>();
        List<String> newDates = new ArrayList<>();
        CSVParser parser = CSVParser.parse(getContent(VIRUS_DATA_CONFIRMED_URL), CSVFormat.DEFAULT.withFirstRecordAsHeader());
        List<String> headers = parser.getHeaderNames();
        //        TODO:Consider only fetching data if new data is available (maybe only fetch new data in time series)
        newDates = headers.subList(4,headers.size());


        TimeSeries timeSeries;
        for (CSVRecord record: parser) {
            String recordCountry = record.get("Country/Region");
            timeSeries = newTimeSeries.stream().
                    filter(x -> x.getCountry().equals(recordCountry)).
                    findFirst().orElse(null);
            if (timeSeries == null) {
                timeSeries = new TimeSeries();
                timeSeries.setCountry(recordCountry);
                newTimeSeries.add(timeSeries);
            }
            List<Integer> newConfirmed = record.toMap().values().stream().
                    skip(4).mapToInt(Integer::parseInt).boxed().collect(Collectors.toList());
            List<Integer> oldConfirmed = timeSeries.getConfirmed();
            List<Integer> totalConfirmed;
            if (oldConfirmed == null) {
                totalConfirmed = newConfirmed;
            } else {
                totalConfirmed = sumLists(newConfirmed, oldConfirmed);
            }
            timeSeries.setConfirmed(totalConfirmed);
        }


        parser = CSVParser.parse(getContent(VIRUS_DATA_DEATHS_URL), CSVFormat.DEFAULT.withFirstRecordAsHeader());
        for (CSVRecord record: parser) {
            timeSeries = newTimeSeries.stream().
                    filter(x -> x.getCountry().equals(record.get("Country/Region"))).
                    findFirst().orElse(null);
            assert timeSeries != null;
            List<Integer> newDeaths = record.toMap().values().stream().skip(4).
                    mapToInt(Integer::parseInt).boxed().
                    collect(Collectors.toList());
            List<Integer> oldDeaths = timeSeries.getDeaths();
            List<Integer> totalDeaths;
            if (oldDeaths == null){
                totalDeaths = newDeaths;
            } else {
                totalDeaths = sumLists(newDeaths, oldDeaths);
            }
            timeSeries.setDeaths(totalDeaths);
        }

        int seriesLength = newTimeSeries.get(0).getConfirmed().size();
        TimeSeries worldTimeSeries = new TimeSeries();
        worldTimeSeries.setCountry("World");
        List<Integer> worldConfirmed = Arrays.stream(new int[seriesLength]).boxed().collect(Collectors.toList());
        List<Integer> worldDeaths = Arrays.stream(new int[seriesLength]).boxed().collect(Collectors.toList());
        for (TimeSeries ts: newTimeSeries) {
            worldConfirmed = sumLists(worldConfirmed, ts.getConfirmed());
            worldDeaths = sumLists(worldDeaths, ts.getDeaths());
        }
        worldTimeSeries.setConfirmed(worldConfirmed);
        worldTimeSeries.setDeaths(worldDeaths);
        newTimeSeries.add(worldTimeSeries);
        
        allTimeSeries = newTimeSeries;
        allDates = newDates;
    }

    private List<Integer> sumLists(List<Integer> oneList, List<Integer> otherList) {
        List<Integer> newList = new ArrayList<>();
        for (int i = 0; i < oneList.size(); i++) {
            newList.add(oneList.get(i) + otherList.get(i));
        }
        return newList;
    }

    private HttpRequest getDailyRequest(String date) {
        String link = VIRUS_DATA_DAILY_URL + date + ".csv";
        return HttpRequest.newBuilder()
                .uri(URI.create(link))
                .build();
    }

    public List<LocationStats> getAllStats() {
        return allStats;
    }
    public List<TimeSeries> getAllTimeSeries() {return allTimeSeries;}
    public List<String> getAllDates() {return allDates;}

    public String getFetchDate() {
        return fetchDate;
    }

    public TimeSeries getTimeSeries(String countrymin){
        TimeSeries countryTimeSeries = allTimeSeries.stream().
                filter(x -> x.getCountrymin().equals(countrymin)).
                findFirst().orElse(null);
        if (countryTimeSeries==null) throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Country not found"
        );
        return countryTimeSeries;
    }

    public Object[][] getLineChartData(TimeSeries countryTimeSeries){
        List<Integer> cases = countryTimeSeries.getConfirmed();
        List<Integer> deaths = countryTimeSeries.getDeaths();
        Object[][] lineChartData = new Object[allDates.size()+1][3];
        lineChartData[0] = new Object[]{"Date", "Cases", "Deaths"};
        for (int i = 0; i < allDates.size(); i++) {
            lineChartData[i+1] = new Object[]{allDates.get(i), cases.get(i), deaths.get(i)};
        }
        return lineChartData;
    }

    public Object[][] getColumnChartData(TimeSeries countryTimeSeries, String category) {
        List<Integer> ydata;
        String ylabel;
        if (category.equals("deaths")) {
            ydata = countryTimeSeries.getDeaths();
            ylabel = "New Deaths";
        } else {
            ydata = countryTimeSeries.getConfirmed();
            ylabel = "New Cases";
        }

        List<Integer> dataDiffs = new ArrayList<>();
        for (int i = 1; i < ydata.size(); i++) {
            dataDiffs.add(ydata.get(i)-ydata.get(i-1));
        }
        Object[][] columnChartData = new Object[allDates.size()][2];
        columnChartData[0] = new Object[]{"Date", ylabel};
        for (int i = 0; i < dataDiffs.size(); i++) {
            columnChartData[i+1] = new Object[]{allDates.get(i+1), dataDiffs.get(i)};
        }
        return columnChartData;
    }

    public int getTotalConfirmedCases() {
        return allStats.stream().filter(x->!x.getCountry().equals("World"))
                .mapToInt(LocationStats::getLatestTotalCases).sum();
    }

    public int getNewConfirmedCases() {
        return allStats.stream().filter(x->!x.getCountry().equals("World"))
                .mapToInt(LocationStats::getCasesDiff).sum();
    }

    public int getTotalDeaths() {
        return allStats.stream().filter(x->!x.getCountry().equals("World"))
                .mapToInt(LocationStats::getDeaths).sum();
    }

    public int getNewDeaths() {
        return allStats.stream().filter(x->!x.getCountry().equals("World"))
                .mapToInt(LocationStats::getDeathsDiff).sum();
    }

    public int getTotalRecovered() {
        return allStats.stream().filter(x->!x.getCountry().equals("World"))
                .mapToInt(LocationStats::getRecovered).sum();
    }
}
