package edu.pmdm.mortahil_fatimaimdbapp.api;

import java.util.ArrayList;
import java.util.List;

public class RapidApiKeyManager {
    private List<String> apiKeys=new ArrayList<>();
    private int currentKeyIndex =0;



    public RapidApiKeyManager(){
        //Añadimos nuestras claves de RapidAPI
        apiKeys.add("b2f9469579msha213c07b8130c1ap1e4793jsn5b9c97c7b358");

        apiKeys.add("9b3f010a5cmsh314da0c0807bc6ep1c4cadjsn48c27286ae89"); //esta no funciona peticiones gastadas

    }

    public String getCurrentKey() {
        return apiKeys.get(currentKeyIndex);
    }

    public void switchToNextKey() {
        currentKeyIndex = (currentKeyIndex + 1) % apiKeys.size();
    }
}
