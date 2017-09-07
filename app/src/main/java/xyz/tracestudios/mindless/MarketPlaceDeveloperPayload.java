package xyz.tracestudios.mindless;

import android.content.Context;

public class MarketPlaceDeveloperPayload {

    Context context;

    MarketPlaceDeveloperPayload(Context context){this.context = context;}

    public String getPayload(){
        return context.getResources().getString(context.getResources().getIdentifier("L", "string", "xyz.tracestudios.mindless"));
    }
}
