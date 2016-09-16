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

    // Anvil Weather Underground API key
    private static final String apiKey = "1973b8c26086ac10";

    // All sites
    private static final String [] sitesAll = { "Kmdsilve4", "Kmieastl10", "Kmdgaith12", "Kmdgerma14", "Kmdfrede39",
            "Kmdhager17", "Kmdmonro5", "Kmdedgew1", "Kmdspark2", "Kmdtimon1", "Kmdparkv5",
            "Kmdburto4", "Kmdelkri1", "Kmdodent3", "Kmdessex6", "Kmdhanov3", "Kmdmiddl4",
            "Kmdellic49", "Kmdmorni2", "Kmdwaldo6", "Kmdgreen3", "Kmdarnol6", "Kmdglenb3",
            "Kmdglenb12", "Kmdodent3", "Kdebetha13", "Kdehenlo2", "Kmdannap43", "Kmdclint2" };

    // Sites subset used for testing
    private static final String [] sitesSubset = { "KFLORLAN65" };

    // Number of queries made before resetting
    private int numQueries = 0;

    // Milliseconds from epoch of last query time
    private long lastQueryTime = Long.MAX_VALUE;

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
            List<Site> sites = new ArrayList<Site>();
            for (String site : sitesSubset) {
                // Pause execution for a minute once we reach 10 queries.
                // Weather Underground does not allow more than 10 queries in a minute
                try{
                    long timeDifferenceBetweenQueries = System.currentTimeMillis();
                    if (timeDifferenceBetweenQueries < SECONDS_IN_A_MINUTE && numQueries >= MAX_QUERIES_IN_A_MINUTE){
                        // Pause thread execution for one minute
                        Log.d("BEGINNING SLEEP", Integer.toString(numQueries));
                        Thread.sleep(SECONDS_IN_A_MINUTE);
                        Log.d("FINISHED SLEEPING", Integer.toString(numQueries));

                        // Reset the number of queries
                        numQueries = 0;
                    }
                    else if (timeDifferenceBetweenQueries >= SECONDS_IN_A_MINUTE){
                        // Reset the number of queries
                        numQueries = 0;
                    }
                }
                catch (InterruptedException e){
                    e.printStackTrace();
                    return null;
                }

                // Query the weather underground for site information. Increment counter
                Log.d("NUMQUERIESBEFOREQUERY", Integer.toString(numQueries));
                String qResult = "";
                if (queryDate.equals("Yesterday")) {
                    qResult = QuerySite(MessageFormat.format(YESTERDAY_BASE_QUERY, apiKey, site));
                }
                else {
                    qResult = QuerySite(MessageFormat.format(TODAY_BASE_QUERY, apiKey, site));
                }

                // Update query time. Increment the number of queries
                lastQueryTime = System.currentTimeMillis();
                ++numQueries;
                Log.d("NUMQUERIESAFTERQUERY", Integer.toString(numQueries));

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
