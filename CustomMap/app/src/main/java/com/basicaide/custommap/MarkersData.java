package com.basicaide.custommap;

public class MarkersData {

        String title = "";
        String snippet = "";
        String city = "";
        double lat,lon;

        public MarkersData(String title,String snippet, double lat, double lon, String city){
            this.title=title;
            this.snippet=snippet;
            this.lat=lat;
            this.lon=lon;
            this.city=city;

    }

}
