package com.example.alex.helloworld;

// a weather station's metadata
public class Site {
    // The name of the site
    String Name;

    // The precipitation total for the site
    String Precipitation;

    // Constructor.
    public Site(String name, String precipitation){
        Name = name;
        Precipitation = precipitation;
    }
}
