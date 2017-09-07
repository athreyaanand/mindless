package xyz.tracestudios.mindless;

import android.content.Context;

public class MarketPlaceKeyGen {

    Context context;

    MarketPlaceKeyGen(Context context){
        this.context = context;
    }

    public String getKey(){
        return context.getResources().getString(context.getResources().getIdentifier("LL", "string", "xyz.tracestudios.mindless"));

    }


}
