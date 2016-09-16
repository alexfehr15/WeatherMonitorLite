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

        String temperature;
        Bitmap icon = null;
        TextView tempText;
        Spinner spinner;
        Button button;

        ProgressDialog dialog;

        private XPath xPath = XPathFactory.newInstance().newXPath();

        private String apiKey = "1973b8c26086ac10";

        private String [] sitesAll = { "Kmdsilve4", "Kmieastl10", "Kmdgaith12", "Kmdgerma14", "Kmdfrede39",
                "Kmdhager17", "Kmdmonro5", "Kmdedgew1", "Kmdspark2", "Kmdtimon1", "Kmdparkv5",
                "Kmdburto4", "Kmdelkri1", "Kmdodent3", "Kmdessex6", "Kmdhanov3", "Kmdmiddl4",
                "Kmdellic49", "Kmdmorni2", "Kmdwaldo6", "Kmdgreen3", "Kmdarnol6", "Kmdglenb3",
                "Kmdglenb12", "Kmdodent3", "Kdebetha13", "Kdehenlo2", "Kmdannap43", "Kmdclint2" };

        private String [] sitesSubset = { "KFLORLAN65" };

        private int numQueries = 0;

        private long lastQueryTime = Long.MAX_VALUE;

        private static final String TODAY_BASE_QUERY = "http://api.wunderground.com/api/{0}/conditions/q/pws:{1}.xml";
        private static final String YESTERDAY_BASE_QUERY = "http://api.wunderground.com/api/{0}/yesterday/q/pws:{1}.xml";

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            // Set up TextView
            tempText = (TextView) findViewById(R.id.tempText);
            tempText.setMovementMethod(new ScrollingMovementMethod());

            // Set up Spinner
            spinner = (Spinner) findViewById(R.id.day_spinner);
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.spinner_choices, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);

            // Set up Button
            button = (Button) findViewById(R.id.queryButton);
            button.setEnabled(true);
        }

        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
            return true;
        }

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
                // new URL() failed
                // ...
            }
            catch (IOException e) {
                // openConnection() failed
                // ...
            }

            return qResult;
        }

        public void QuerySites(View view){
            // Kickoff task to check sites
            button.setEnabled(false);
            new retrieve_weatherTask(spinner.getSelectedItem().toString()).execute();
        }

        protected class retrieve_weatherTask extends AsyncTask<Void, String, List<Site>> {

            private String queryDate;

            public retrieve_weatherTask(String queryDate){
                this.queryDate = queryDate;
            }

            protected void onPreExecute(){
                dialog = new ProgressDialog(MainActivity.this);
                dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                dialog.setMessage("Loading…");
                dialog.setCancelable(false);
                dialog.show();
            }

            @Override
            protected List<Site> doInBackground(Void... arg0) {
                List<Site> sites = new ArrayList<Site>();
                for (String site : sitesAll) {
                    // Pause execution for a minute once we reach 10 queries. Weather Underground does not allow more than 10 queries in a minute
                    try{
                        if (System.currentTimeMillis() - lastQueryTime < 60000 && numQueries >= 10){
                            Log.d("BEGINNING SLEEP", Integer.toString(numQueries));
                            Thread.sleep(60000);
                            Log.d("FINISHED SLEEPING", Integer.toString(numQueries));

                            numQueries = 0;
                        }
                    }
                    catch (InterruptedException e){
                        e.printStackTrace();
                    }

                    // Query the weather underground for site information. Increment counter
                    Log.d("NUMQUERIESBEFOREQUERY", Integer.toString(numQueries));
                    String qResult = "";
                    if (queryDate.equals("Yesterday")) {
                        qResult = QuerySite(MessageFormat.format("http://api.wunderground.com/api/{0}/yesterday/q/pws:{1}.xml", apiKey, site));
                    }
                    else {
                        qResult = QuerySite(MessageFormat.format("http://api.wunderground.com/api/{0}/conditions/q/pws:{1}.xml", apiKey, site));
                    }

                    lastQueryTime = System.currentTimeMillis();
                    ++numQueries;
                    Log.d("NUMQUERIESAFTERQUERY", Integer.toString(numQueries));

                    // Create document to use XPath on
                    Document dest = null;
                    DocumentBuilderFactory dbFactory = DocumentBuilderFactory
                            .newInstance();
                    DocumentBuilder parser;
                    try {
                        parser = dbFactory.newDocumentBuilder();
                        dest = parser
                                .parse(new ByteArrayInputStream(qResult.getBytes()));
                    } catch (ParserConfigurationException e1) {
                        e1.printStackTrace();
                    } catch (SAXException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    String precipitation;
                    if (queryDate.equals("Yesterday")) {
                        precipitation = sv ("//history/observations/observation[last()]/precip_totali", dest);
                    }
                    else {
                        precipitation = sv ("//current_observation/precip_today_in", dest);
                    }
                    Log.d("PRECIPITATIONBEFORECONC", precipitation);
                    precipitation += " inches";
                    sites.add(new Site(site, precipitation));

                    Log.d("PRECIPITATION", MessageFormat.format("{0}: {1}", site, precipitation));
                }

                return sites;
            }

            public String sv(String query, Node node) {

                String rs = "";

                try {
                    Node n = (Node) xPath.evaluate(query, node, XPathConstants.NODE);
                    if (n != null) {
                        rs = n.getTextContent();
                    }
                } catch (Exception e) {
                    rs = "";
                }
                return rs;
            }

            protected void onPostExecute(List<Site> result) {
                if(dialog.isShowing()){
                    dialog.dismiss();
                    String displayText = "";
                    for (Site site : result) {
                        displayText += MessageFormat.format("{0}: {1}\n", site.Name, site.Precipitation);
                    }
                    tempText.setText(displayText);
                    button.setEnabled(true);
                }
            }
        }
}
