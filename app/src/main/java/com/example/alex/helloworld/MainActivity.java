package com.example.alex.helloworld;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    // TextView used to display site query results
    private TextView textView;

    // Spinner used to select whether query should be for today or the day prior
    private Spinner spinner;

    // Button used to kick off the task that queries the sites
    private Button button;

    // Loading dialog shown while sites are queried
    private ProgressDialog dialog;

    // XPath instance
    private XPath xPath = XPathFactory.newInstance().newXPath();

    // Anvil Weather Underground API key (Alex)
    private static final String apiKeyOne = "1973b8c26086ac10";

    // Anvil Weather Underground API key (Jess)
    private static final String apiKeyTwo = "578e3c16e28c043a";

    // Anvil Weather Underground API Key (Alex - bucket)
    private static final String apiKeyThree = "50ad2fd8d9651127";

    // List of API keys
    private List<APIKey> apiKeys = new ArrayList<APIKey>();

    // Current API key index
    private int currentKeyIndex = 0;

    // All sites
    private static final String [] sitesAll = { "Kmdsilve4", "Kmieastl10", "Kmdgaith12", "Kmdgerma14", "Kmdfrede39",
            "Kmdhager17", "Kmdmonro5", "Kmdedgew1", "Kmdspark2", "Kmdtimon1", "Kmdparkv5",
            "Kmdburto4", "Kmdelkri1", "Kmdodent3", "Kmdessex6", "Kmdhanov3", "Kmdmiddl4",
            "Kmdellic49", "Kmdmorni2", "Kmdwaldo6", "Kmdgreen3", "Kmdarnol6", "Kmdglenb3",
            "Kmdglenb12", "Kmdodent3", "Kdebetha13", "Kdehenlo2", "Kmdannap43", "Kmdclint2" };

    // Sites subset used for testing
    private static final String [] sitesSubset = { "KFLORLAN65" };

    // Base query used when query is about today's conditions
    private static final String TODAY_BASE_QUERY = "http://api.wunderground.com/api/{0}/conditions/q/pws:{1}.xml";

    private static final String TODAY_XPATH_QUERY = "//current_observation/precip_today_in";

    // Base query used when query is about yesterday's conditions
    private static final String YESTERDAY_BASE_QUERY = "http://api.wunderground.com/api/{0}/yesterday/q/pws:{1}.xml";

    // Base XPath query used when query is about yesterday's conditions
    private static final String YESTERDAY_XPATH_QUERY = "//history/observations/observation[last()]/precip_totali";

    // Number of seconds in a minute
    private static final long SECONDS_IN_A_MINUTE = 60000;

    // Maximum number of queries that can be made in a minute
    private static final int MAX_QUERIES_IN_A_MINUTE = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Display the main UI when created
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up API keys
        apiKeys.add(0, new APIKey(apiKeyOne));
        apiKeys.add(1, new APIKey(apiKeyTwo));
        apiKeys.add(2, new APIKey(apiKeyThree));
        Log.d("APIKEYSLOADED", Integer.toString(apiKeys.size()));

        // Set up Spinner
        spinner = (Spinner) findViewById(R.id.day_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.spinner_choices, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        // Set up Button
        button = (Button) findViewById(R.id.queryButton);
        button.setEnabled(true);

        // Set up TextView
        textView = (TextView) findViewById(R.id.textView);
        textView.setMovementMethod(new ScrollingMovementMethod());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        return true;
    }

    // Kickoff task to query site information
    public void QuerySites(View view){
        //Disable site query button while sites are being queried
        button.setEnabled(false);

        // Start task
        new WeatherTask(spinner.getSelectedItem().toString()).execute();
    }

    // Get data from URL based on provided query
    private String QuerySite(String siteQuery){
        String qResult = "";
        try {
            URL myURL = new URL(siteQuery);
            URLConnection myURLConnection = myURL.openConnection();
            myURLConnection.connect();
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    myURLConnection.getInputStream()));

            StringBuilder stringBuilder = new StringBuilder();
            String stringReadLine = null;
            while ((stringReadLine = in.readLine()) != null) {
                stringBuilder.append(stringReadLine + "\n");
            }
            qResult = stringBuilder.toString();
            in.close();
        }
        catch (MalformedURLException e) {
            // new URL() failed. Return empty string
            e.printStackTrace();
            qResult = "";
        }
        catch (IOException e) {
            // openConnection() failed. Return empty string
            e.printStackTrace();
            qResult = "";
        }

        return qResult;
    }

    // Search node using query string
    public String Search(String query, Node node) {
        String rs = "";

        try {
            Node n = (Node) xPath.evaluate(query, node, XPathConstants.NODE);
            if (n != null) {
                rs = n.getTextContent();
            }
        } catch (Exception e) {
            e.printStackTrace();
            rs = "";
        }

        return rs;
    }

    // Create document to use XPath on from query result string
    private Document CreateDocument(String queryResult){
        // Create document to use XPath on
        Document dest = null;
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory
                .newInstance();
        DocumentBuilder parser;
        try {
            parser = dbFactory.newDocumentBuilder();
            dest = parser
                    .parse(new ByteArrayInputStream(queryResult.getBytes()));
        } catch (ParserConfigurationException e1) {
            e1.printStackTrace();
            dest = null;
        } catch (SAXException e) {
            e.printStackTrace();
            dest = null;
        } catch (IOException e) {
            e.printStackTrace();
            dest = null;
        }

        return dest;
    }

    // Async task used to retrieve site weather data
    protected class WeatherTask extends AsyncTask<Void, String, List<Site>> {
        // Whether the site should be queried for today's conditions or yesterday's conditions
        private String queryDate;

        // Constructor
        public WeatherTask(String queryDate){
            this.queryDate = queryDate;
        }

        // Method called before the task is executed. Show loading dialog
        protected void onPreExecute(){
            dialog = new ProgressDialog(MainActivity.this);
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setMessage("Loading…");
            dialog.setCancelable(false);
            dialog.show();
        }

        // Background method. Query site information
        @Override
        protected List<Site> doInBackground(Void... arg0) {
            // Reset number of queries where applicable
            for (APIKey key : apiKeys){
                if (System.currentTimeMillis() - key.LastQueryTimeMilliseconds >= SECONDS_IN_A_MINUTE){
                    key.NumQueries = 0;
                    Log.d("RESETQUERIESBACKGROUND", key.APIKey);
                }
            }

            List<Site> sites = new ArrayList<Site>();
            for (String site : sitesAll) {
                // Pause execution for a minute once we reach 10 queries.
                // Weather Underground does not allow more than 10 queries in a minute
                try{
                    long timeDifferenceBetweenQueries = System.currentTimeMillis() - apiKeys.get(currentKeyIndex).LastQueryTimeMilliseconds;
                    if (timeDifferenceBetweenQueries < SECONDS_IN_A_MINUTE &&
                            apiKeys.get(currentKeyIndex).NumQueries >= MAX_QUERIES_IN_A_MINUTE){
                        Boolean sleep = true;
                        for (int i = 0; i < apiKeys.size(); ++i){
                            if (System.currentTimeMillis() - apiKeys.get(i).LastQueryTimeMilliseconds >= SECONDS_IN_A_MINUTE ||
                                    apiKeys.get(i).NumQueries < MAX_QUERIES_IN_A_MINUTE){
                                sleep = false;
                                currentKeyIndex = i;
                                Log.d("NOSLEEPFOR", apiKeys.get(i).APIKey);
                                break;
                            }
                        }

                        if (sleep){
                            // Pause thread execution for one minute
                            Log.d("BEGINNING SLEEP", apiKeys.get(currentKeyIndex).APIKey);
                            Thread.sleep(SECONDS_IN_A_MINUTE);
                            Log.d("FINISHED SLEEPING", apiKeys.get(currentKeyIndex).APIKey);

                            // Reset the number of queries for each API key
                            for (APIKey key : apiKeys) {
                                key.NumQueries = 0;
                            }
                        }
                    }
                    else if (timeDifferenceBetweenQueries >= SECONDS_IN_A_MINUTE){
                        // Reset the number of queries for the current API key
                        apiKeys.get(currentKeyIndex).NumQueries = 0;
                        Log.d("ELSEIFRESETQUERIES", apiKeys.get(currentKeyIndex).APIKey);
                    }
                }
                catch (InterruptedException e){
                    e.printStackTrace();
                    return null;
                }

                Log.d("QUERYKEY", apiKeys.get(currentKeyIndex).APIKey);

                // Query the weather underground for site information. Increment counter
                Log.d("NUMQUERIESBEFOREQUERY", Integer.toString(apiKeys.get(currentKeyIndex).NumQueries));
                String qResult = "";
                if (queryDate.equals("Yesterday")) {
                    qResult = QuerySite(MessageFormat.format(YESTERDAY_BASE_QUERY, apiKeyOne, site));
                }
                else {
                    qResult = QuerySite(MessageFormat.format(TODAY_BASE_QUERY, apiKeyOne, site));
                }

                // Update query time. Increment the number of queries
                apiKeys.get(currentKeyIndex).LastQueryTimeMilliseconds = System.currentTimeMillis();
                ++apiKeys.get(currentKeyIndex).NumQueries;
                Log.d("NUMQUERIESAFTERQUERY", Integer.toString(apiKeys.get(currentKeyIndex).NumQueries));

                // Only proceed if site query is valid
                String precipitation = "unknown";
                if (qResult != null && !qResult.isEmpty()) {
                    // Create document from query result
                    Document dest = CreateDocument(qResult);

                    // Use XPath to extract precipitation
                    if (queryDate.equals("Yesterday")) {
                        precipitation = Search(YESTERDAY_XPATH_QUERY, dest);
                    } else {
                        precipitation = Search(TODAY_XPATH_QUERY, dest);
                    }

                    // Add units to precipitation
                    precipitation += " in";
                }

                // Add site
                sites.add(new Site(site, precipitation));
                Log.d("PRECIPITATION", MessageFormat.format("{0}: {1}", site, precipitation));
            }

            return sites;
        }

        // Method called when task execution has finished
        protected void onPostExecute(List<Site> result) {
            // Dismiss loading dialog if it is showing
            if(dialog.isShowing()) {
                dialog.dismiss();
            }

            // Populate TextView with site information
            String displayText = "";
            for (Site site : result) {
                displayText += MessageFormat.format("{0}: {1}\n", site.Name, site.Precipitation);
            }
            textView.setText(displayText);

            // Re-enable check sites button
            button.setEnabled(true);
        }
    }
}
