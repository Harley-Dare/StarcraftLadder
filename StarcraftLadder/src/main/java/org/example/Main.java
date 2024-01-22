package org.example;

public class Main {
    public static void main(String[] args) throws Exception {
        String cfgPath = "/home/harley/Documents/programming/StarcraftLadder/config.properties";
        int regionID = 1;
        String region = "us";

        DataFetcher dataFetcher = new DataFetcher(cfgPath, region, regionID);
        System.out.println(dataFetcher.getOAuthToken());
    }
}