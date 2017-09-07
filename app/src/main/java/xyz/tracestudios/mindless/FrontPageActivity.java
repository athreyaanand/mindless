package xyz.tracestudios.mindless;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import xyz.klinker.android.article.ArticleIntent;
import xyz.tracestudios.mindless.util.IabBroadcastReceiver;
import xyz.tracestudios.mindless.util.IabHelper;
import xyz.tracestudios.mindless.util.IabResult;
import xyz.tracestudios.mindless.util.Inventory;
import xyz.tracestudios.mindless.util.Purchase;

public class FrontPageActivity extends AppCompatActivity implements IabBroadcastReceiver.IabBroadcastListener {

    private String TAG = FrontPageActivity.class.getSimpleName();

    private RecyclerView rv;

    Button tryAgain;

    NewsAdapter newsAdapter;

    LinearLayoutManager mLayoutManager;

    private static int AD_FREQUENCY = 5;

    private NavigationView navigationView;
    private DrawerLayout drawer;
    private Toolbar toolbar;
    public static int navItemIndex = 0;


    private List<Package> newsList;

    View contentUnavailable;

    FetchTask fetchTask;

    SwipeRefreshLayout refreshLayout;
    InterstitialAd mInterstitialAd;

    //IAP STUFF --- START

    // Provides purchase notification while this app is running
    IabBroadcastReceiver mBroadcastReceiver;

    // (arbitrary) request code for the purchase flow
    static final int RC_REQUEST = 1664;

    // The helper object
    private IabHelper mHelper;

    //SKU Ownership
    HashMap<String, Boolean> SKUOwnershipHashMap;

    //ITEM SKUs
    static final String SKU_NOADS = "noads";
    static final List<String> SKUList = Arrays.asList(
            SKU_NOADS
    );

    //IAP STUFF --- END


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_front_page);

        toolbar = (Toolbar) findViewById(R.id.toolbar);

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        navigationView = (NavigationView) findViewById(R.id.nav_view);
        // initializing navigation menu
        setUpNavigationView();

        newsList = new ArrayList<>();

        mHelper = new IabHelper(getApplicationContext(), getKey());
        mHelper.enableDebugLogging(true);

        SKUOwnershipHashMap = new HashMap<>();

        if (!(SKUOwnershipHashMap.containsKey(SKU_NOADS) ? SKUOwnershipHashMap.get(SKU_NOADS) : false)) {
            mInterstitialAd = new InterstitialAd(FrontPageActivity.this);
            mInterstitialAd.setAdUnitId("ca-app-pub-9416624096434395/1496578261");
            mInterstitialAd.loadAd(new AdRequest.Builder().build());

            mInterstitialAd.setAdListener(new AdListener() {
                @Override
                public void onAdClosed() {
                    // Load the next interstitial.
                    mInterstitialAd.loadAd(new AdRequest.Builder().build());
                }

            });
        }

        contentUnavailable = findViewById(R.id.content_unavailable);

        rv = (RecyclerView) findViewById(R.id.stories);
        rv.setHasFixedSize(true);
        //rv.addItemDecoration(new DividerItemDecoration(this));

        refreshLayout = (SwipeRefreshLayout) findViewById(R.id.refresh);
        refreshLayout.setRefreshing(true);

        /**
         * Updating parsed JSON data into ListView
         * */
        newsAdapter = new NewsAdapter(newsList, getApplicationContext());
        mLayoutManager = new LinearLayoutManager(getApplicationContext());
        rv.setLayoutManager(mLayoutManager);
        rv.setItemAnimator(new DefaultItemAnimator());
        rv.setAdapter(newsAdapter);
        rv.addOnItemTouchListener(
                new RecyclerItemClickListener(getApplicationContext(), rv, new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        if (position % AD_FREQUENCY != 0 || position == 0) {
                            String apiKey = "ce0452149a894e322d1f9153f0d5ed4a";

                            NewsPackage newsPackage = (NewsPackage) newsList.get(position);
                            ArticleIntent intent = new ArticleIntent.Builder(FrontPageActivity.this, apiKey)
                                    .setToolbarColor(getResources().getColor(R.color.colorPrimary))
                                    .build();

                            intent.launchUrl(FrontPageActivity.this, Uri.parse(newsPackage.getUrl()));

                            Random rand = new Random();
                            int chance = rand.nextInt(4);

                            System.out.println("BEGINNING OF CLICK METHOD");

                            if(chance==0 && !(SKUOwnershipHashMap.containsKey(SKU_NOADS) ? SKUOwnershipHashMap.get(SKU_NOADS) : false)) {
                                if (mInterstitialAd.isLoaded()) {
                                    mInterstitialAd.show();
                                } else {
                                    Log.d("TAG", "The interstitial wasn't loaded yet.");
                                }

                                System.out.println("INTERSTITIAL SHOWING: "+mInterstitialAd.isLoaded());
                            }

                            System.out.println("END OF CLICK METHOD");

                        }
                    }

                    @Override
                    public void onLongItemClick(View view, int position) {
                        // do whatever
                    }
                })
        );

        rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) //check for scroll down
                {
                    int visibleItemCount = mLayoutManager.getChildCount();
                    int totalItemCount = mLayoutManager.getItemCount();
                    int pastVisiblesItems = mLayoutManager.findFirstVisibleItemPosition();
                    if (visibleItemCount + pastVisiblesItems >= totalItemCount) {
                        fetchTask.fetch();
                    }
                }
            }
        });

        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshLayout.setRefreshing(true);
                fetchTask.refresh();
            }
        });


        tryAgain = (Button) findViewById(R.id.try_again);
        tryAgain.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                refreshLayout.setRefreshing(true);
                fetchTask.refresh();
            }
        });

        //START MARKET STUFF

        // Start setup. This is asynchronous and the specified listener
        // will be called once setup completes.
        Log.d(TAG, "Starting setup.");
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                Log.d(TAG, "Setup finished.");

                if (!result.isSuccess()) {
                    // Oh noes, there was a problem.
                    complain("Problem setting up in-app billing: " + result);
                    return;
                }

                // Have we been disposed of in the meantime? If so, quit.
                if (mHelper == null) return;

                // Important: Dynamically register for broadcast messages about updated purchases.
                // We register the receiver here instead of as a <receiver> in the Manifest
                // because we always call getPurchases() at startup, so therefore we can ignore
                // any broadcasts sent while the app isn't running.
                // Note: registering this listener in an Activity is a bad idea, but is done here
                // because this is a SAMPLE. Regardless, the receiver must be registered after
                // IabHelper is setup, but before first call to getPurchases().
                mBroadcastReceiver = new IabBroadcastReceiver(FrontPageActivity.this);
                IntentFilter broadcastFilter = new IntentFilter(IabBroadcastReceiver.ACTION);
                registerReceiver(mBroadcastReceiver, broadcastFilter);

                // IAB is fully set up. Now, let's get an inventory of stuff we own.
                Log.d(TAG, "Setup successful. Querying inventory.");
                try {
                    mHelper.queryInventoryAsync(true, SKUList, null, mGotInventoryListener);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    complain("Error querying inventory. Another async operation in progress.");
                }
            }
        });

        fetchTask = new FetchTask();
        fetchTask.execute();
    }

    private void showContentUnavailable() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                System.out.println("in hide contentU");
                contentUnavailable.setVisibility(View.VISIBLE);
                rv.setVisibility(View.GONE);
            }
        });
    }

    private void hideContentUnavailable() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                System.out.println("in hide contentU");
                contentUnavailable.setVisibility(View.GONE);
                rv.setVisibility(View.VISIBLE);
            }
        });
    }

    private class FetchTask extends AsyncTask<Void, Void, Void> {

        private List<Package> newsPackageList;
        private List<String> lastFetchedIDs;

        private OkHttpClient client;

        private SortType sortType;

        boolean isFirstFetch;
        boolean wasFetchSuccessful;

        boolean isUserAdFree;

        private String lastThingID;

        //URLs to get JSON
        private final String REDDIT_STATIC_URL = "https://static.reddit.com/";
        private final String MINDLESS_ROOT = "https://www.reddit.com/user/TraceStudios/m/mindlessapp/";
        private final String _HOT = MINDLESS_ROOT + "hot/";
        private final String _NEW = MINDLESS_ROOT + "new/";
        private final String _TOP = MINDLESS_ROOT + "top/";
        private final String _ITERATOR = "?count=25&after=";
        private final String _JSON = ".json";

        private int page = 0;
        private final int ARTICLES_PER_PAGE_COUNT = 25;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            isFirstFetch = true;
            isUserAdFree = findUsersAdStatus();
            newsPackageList = new ArrayList<>();
            lastFetchedIDs = new ArrayList<>();
            client = new OkHttpClient();
            sortType = SortType.HOT;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            if (canConnectToSource()) {
                hideContentUnavailable();
                fetch();
            } else
                showContentUnavailable();
            return null;
        }

        private boolean canConnectToSource() {
            try {
                OkHttpClient client = new OkHttpClient();

                Request request = new Request.Builder()
                        .url(REDDIT_STATIC_URL)
                        .build();

                Response response = client.newCall(request).execute();

                return (response.body().string().equals("404 Not Found"));
            } catch (IOException e1) {
                e1.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            updateList();
            updateView();
        }

        private void updateList() {
            newsList.clear();
            for (Package p : newsPackageList)
                newsList.add(p);
        }

        private void updateView() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    refreshLayout.setRefreshing(false);
                    newsAdapter.notifyDataSetChanged();
                }
            });
        }

        public void refresh() {
            isFirstFetch = true;
            lastFetchedIDs.clear();
            destroyStories();
            fetch();
            rv.scrollToPosition(0);
        }

        public void fetch() {

            if (isFirstFetch) {
                isFirstFetch = false;
                page++;
                fetchStories(buildURL(sortType));
            } else {
                //only fetch if not fetched already
                if (!lastFetchedIDs.contains(lastThingID)) {
                    page++;
                    lastFetchedIDs.add(lastThingID);
                    fetchStories(buildURL(sortType, lastThingID));
                } else
                    return;
            }
        }

        private void fetchStories(String url) {
            clientCall(url);
        }

        private void buildStories(JSONObject json) {

            List<NewsPackage> newNewsPackages = new ArrayList<>();

            try {
                JSONObject data = json.getJSONObject("data");
                JSONArray children = data.getJSONArray("children");

                System.out.println("# OF STORIES FOUND: " + children.length());
                // looping through All stories
                for (int i = 0; i < children.length(); i++) {
                    JSONObject childData = children.getJSONObject(i);

                    JSONObject elData = childData.getJSONObject("data");

                    String title = elData.getString("title");
                    String domain = elData.getString("domain");
                    String url = elData.getString("url");
                    long utc = elData.getLong("created_utc");

                    String thumbnail;
                    if (elData.has("preview")) {
                        JSONObject preview = elData.getJSONObject("preview");
                        JSONArray images = preview.getJSONArray("images");
                        JSONObject d = images.getJSONObject(0);
                        JSONObject source = d.getJSONObject("source");
                        thumbnail = source.getString("url");
                    } else {
                        thumbnail = "http://placehold.it/100x100";
                    }

                    //grab last id
                    if (i == 24) {
                        lastThingID = elData.getString("name");
                    }

                    // tmp hash map for single contact
                    NewsPackage cell = new NewsPackage(title, domain, url, thumbnail, utc);
                    System.out.println((i + newsList.size()) + ":" + cell.toString());

                    // adding to cache list
                    newNewsPackages.add(cell);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            newsPackageList.addAll(newNewsPackages);

            processStories();
        }

        private void processStories() {
            removeDups();
            if (!findUsersAdStatus())
                insertAds();
        }

        private void removeDups() {
            List<String> urls = new ArrayList<>();
            for (int i = (page * ARTICLES_PER_PAGE_COUNT); i < newsPackageList.size(); i++) {
                NewsPackage n = (NewsPackage) newsPackageList.get(i);
                if (urls.contains(n.getUrl())) {
                    System.out.println("DUPE ARTICLE FOUND (" + n.getTitle().substring(0,7) + "...) and removed");
                    //remove
                    newsPackageList.remove(i);
                    i--;
                } else
                    urls.add(n.getUrl());
            }
        }

        private boolean findUsersAdStatus() {
            return SKUOwnershipHashMap.containsKey(SKU_NOADS) ? SKUOwnershipHashMap.get(SKU_NOADS) : false;
        }

        private void insertAds() {

            if (!(newsPackageList.size() > 0))
                return;

            for (int i = 1; i < newsPackageList.size(); i++) {
                if (i % AD_FREQUENCY == 0) {
                    AdPackage adPackage = new AdPackage();
                    newsPackageList.add(i, adPackage);
                    System.out.println("FRONTPAGE: AD inserted @i=" + i);
                }
            }


        }

        private void clientCall(String url) {
            System.out.println("GEETIN INFO FROM URL: " + url);
            Request request = new Request.Builder().url(url).build();
            Callback callback = new Callback() {


                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                    wasFetchSuccessful = false;
                    showContentUnavailable();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    hideContentUnavailable();
                    if (!response.isSuccessful())
                        throw new IOException("Unexpected code " + response);

                    hideContentUnavailable();

                    JSONObject json = null;
                    try {
                        json = new JSONObject(response.body().string());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    buildStories(json);
                    updateList();
                    updateView();
                }
            };

            client.newCall(request).enqueue(callback);
        }

        public void destroyStories() {
            newsList.clear();
            newsPackageList.clear();
        }

        private String buildURL(SortType type) {
            switch (type) {
                case NEW:
                    return _NEW + _JSON;
                case TOP:
                    return _TOP + _JSON;
                case HOT: //HOT is Default
                default:
                    return _HOT + _JSON;
            }
        }

        private String buildURL(SortType type, String tid) {
            switch (type) {
                case NEW:
                    return _NEW + _JSON + _ITERATOR + tid;
                case TOP:
                    return _TOP + _JSON + _ITERATOR + tid;
                case HOT: //HOT is Default
                default:
                    return _HOT + _JSON + _ITERATOR + tid;
            }
        }

        public void setSortType(SortType sortType) {
            this.sortType = sortType;
        }
    }

    private void setUpNavigationView() {
        //Setting Navigation View Item Selected Listener to handle the item click of the navigation menu
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                //Check to see which item was being clicked and perform appropriate action
                switch (item.getItemId()) {
                    //Replacing the main content with ContentFragment Which is our Inbox View;
                    case R.id.nav_hot:
                        navItemIndex = 0;
                        fetchTask.setSortType(SortType.HOT);
                        break;
                    case R.id.nav_new:
                        navItemIndex = 1;
                        fetchTask.setSortType(SortType.NEW);
                        break;
                    case R.id.nav_top:
                        navItemIndex = 2;
                        fetchTask.setSortType(SortType.TOP);
                        break;
                    case R.id.nav_noads:
                        navItemIndex = 3;
                        break;
                    case R.id.nav_about_us: navItemIndex = 4; break;
                    default: navItemIndex = 4;
                }

                fetchTask.refresh();
                drawer.closeDrawers();
                invalidateOptionsMenu();

                //Checking if the item is in checked state or not, if not make it in checked state
                if (item.isChecked()) {
                    item.setChecked(false);
                } else {
                    item.setChecked(true);
                }
                item.setChecked(true);

                if (navItemIndex==3){
                    PurchaseItem(SKU_NOADS);
                }else if (navItemIndex==4){
                    Uri uri = Uri.parse("market://details?id=" + getPackageName());
                    Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
                    // To count with Play market backstack, After pressing back button,
                    // to taken back to our application, we need to add following flags to intent.
                    goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                            Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                            Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                    try {
                        startActivity(goToMarket);
                    } catch (ActivityNotFoundException e) {
                        startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse("http://play.google.com/store/apps/details?id=" + getPackageName())));
                    }
                }

                return true;
            }
        });


        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.openDrawer, R.string.closeDrawer) {

            @Override
            public void onDrawerClosed(View drawerView) {
                if (SKUOwnershipHashMap.containsKey(SKU_NOADS) ? SKUOwnershipHashMap.get(SKU_NOADS) : false){
                    Menu nav_Menu = navigationView.getMenu();
                    nav_Menu.findItem(R.id.nav_noads).setVisible(false);
                }
                // Code here will be triggered once the drawer closes as we dont want anything to happen so we leave this blank
                super.onDrawerClosed(drawerView);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                if (SKUOwnershipHashMap.containsKey(SKU_NOADS) ? SKUOwnershipHashMap.get(SKU_NOADS) : false){
                    Menu nav_Menu = navigationView.getMenu();
                    nav_Menu.findItem(R.id.nav_noads).setVisible(false);
                }
                // Code here will be triggered once the drawer open as we dont want anything to happen so we leave this blank
                super.onDrawerOpened(drawerView);
            }
        };

        //Setting the actionbarToggle to drawer layout
        drawer.setDrawerListener(actionBarDrawerToggle);

        //calling sync state is necessary or else your hamburger icon wont show up
        actionBarDrawerToggle.syncState();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawers();
            return;
        }

        super.onBackPressed();
    }

    /* --- MARKET PLACE STUFF --- */

    private String getKey() {
        return new MarketPlaceKeyGen(this).getKey();
    }

    private String getDeveloperPayload() {
        return new MarketPlaceDeveloperPayload(this).getPayload();
    }

    private void PurchaseItem(String sku) {
        Log.d(TAG, "Buy item clicked");

        if (SKUOwnershipHashMap.get(sku)) {
            complain("No need! You already bought NO ADS");
            return;
        }

        // launch the purchase UI flow.
        // We will be notified of completion via mPurchaseFinishedListener
        Log.d(TAG, "Launching purchase flow for gas.");

        // TODO: for security, generate your payload here for verification. See the comments on verifyDeveloperPayload()
        String payload = getDeveloperPayload();

        try {
            mHelper.launchPurchaseFlow(this, sku, RC_REQUEST, mPurchaseFinishedListener, payload);
        } catch (IabHelper.IabAsyncInProgressException e) {
            complain("Error launching purchase flow. Another async operation in progress.");
        }
    }

    // Listener that's called when we finish querying the items and subscriptions we own
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            Log.d(TAG, "Query inventory finished.");

            // Have we been disposed of in the meantime? If so, quit.
            if (mHelper == null) return;

            // Is it a failure?
            if (result.isFailure()) {
                complain("Failed to query inventory: " + result);
                return;
            }

            Log.d(TAG, "Query inventory was successful.");

            //Find if ad-free
            Purchase adFreePurchase = inventory.getPurchase(SKU_NOADS);
            boolean hasAdFreePurchase = adFreePurchase != null && verifyDeveloperPayload(adFreePurchase);
            SKUOwnershipHashMap.put(SKU_NOADS, hasAdFreePurchase);

            Log.d(TAG, "Initial inventory query finished; enabling main UI.");
        }
    };

    @Override
    public void receivedBroadcast() {
        // Received a broadcast notification that the inventory of items has changed
        Log.d(TAG, "Received broadcast notification. Querying inventory.");
        try {
            mHelper.queryInventoryAsync(mGotInventoryListener);
        } catch (IabHelper.IabAsyncInProgressException e) {
            complain("Error querying inventory. Another async operation in progress.");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);
        if (mHelper == null) return;

        // Pass on the activity result to the helper for handling
        if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
            super.onActivityResult(requestCode, resultCode, data);
        } else {
            Log.d(TAG, "onActivityResult handled by IABUtil.");
        }
    }

    /**
     * Verifies the developer payload of a purchase.
     */
    boolean verifyDeveloperPayload(Purchase p) {
        return getDeveloperPayload().equals(p.getDeveloperPayload());
    }

    // Callback for when a purchase is finished
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);

            // if we were disposed of in the meantime, quit.
            if (mHelper == null) return;

            if (result.isFailure()) {
                complain("Error purchasing: " + result);
                return;
            }
            if (!verifyDeveloperPayload(purchase)) {
                complain("Error purchasing. Authenticity verification failed.");
                return;
            }

            Log.d(TAG, "Purchase successful.");

            alert("Thank you for removing ads");

            //TODO: restart intent

            //add ad-free
            if (purchase.getSku().equals(SKU_NOADS))
                SKUOwnershipHashMap.put(SKU_NOADS, true);

        }
    };

    // We're being destroyed. It's important to dispose of the helper here!
    @Override
    public void onDestroy() {
        super.onDestroy();

        // very important:
        if (mBroadcastReceiver != null) {
            unregisterReceiver(mBroadcastReceiver);
        }

        // very important:
        Log.d(TAG, "Destroying helper.");
        if (mHelper != null) {
            mHelper.disposeWhenFinished();
            mHelper = null;
        }
    }

    void complain(String message) {
        Log.e(TAG, "**** Insider Error: " + message);
        alert(message);
    }

    void alert(String message) {
        AlertDialog.Builder bld = new AlertDialog.Builder(this);
        bld.setMessage(message);
        bld.setNeutralButton("OK", null);
        Log.d(TAG, "Showing alert dialog: " + message);
        bld.create().show();
    }

}
