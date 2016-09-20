package com.example.alex.helloworld;

// Weather Underground API key.
public class APIKey {
    // Actual API key
    public String APIKey;

    // Number of queries made with this API key without a reset
    public int NumQueries;

    // Milliseconds since epoch that a query was made with this key
    public long LastQueryTimeMilliseconds;

    // Constructor
    public APIKey(String apiKey){
        APIKey = apiKey;
        NumQueries = 0;
        LastQueryTimeMilliseconds = Long.MIN_VALUE;
    }
}
