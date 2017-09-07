package xyz.tracestudios.mindless;

/**
 * Created by athreya on 3/20/2017.
 */

import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.squareup.picasso.Picasso;

import com.google.android.gms.ads.NativeExpressAdView;

import java.text.ParseException;
import java.util.List;

import static android.R.attr.path;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.MyViewHolder> {

    private List<Package> newsList;
    private Context mContext;
    private CardView cv;
    private int cellCount = 0;

    final private int AD_TYPE = 127;
    final private int NORMAL_TYPE = 203;
    final private int FEATURE_TYPE = 347;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView title, domain, time;
        public ImageView thumbnail;
        private NativeExpressAdView adView;

        public MyViewHolder(View view) {
            super(view);

            cv = (CardView) view.findViewById(R.id.cv);
            title = (TextView) view.findViewById(R.id.title);
            domain = (TextView) view.findViewById(R.id.domain);
            time = (TextView) view.findViewById(R.id.relativeTime);
            thumbnail = (ImageView) view.findViewById(R.id.featureImage);
            adView = (NativeExpressAdView) view.findViewById(R.id.adView);
        }
    }

    public NewsAdapter(List<Package> newsList, Context context) {
        this.newsList = newsList;
        this.mContext = context;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View itemView;

        switch (viewType){
            case FEATURE_TYPE: {
                itemView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.first_list_item, parent, false);
                System.out.println("NEWSADAPTER: Returned FEATURE @index("+cellCount+")");
                break;
            }
            case NORMAL_TYPE: {
                itemView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.front_page_list_item, parent, false);
                System.out.println("NEWSADAPTER: Returned NORMAL @index("+cellCount+")");
                break;
            }
            case AD_TYPE: {
                itemView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.ad_list_item, parent, false);
                System.out.println("NEWSADAPTER: Returned AD @index("+cellCount+")");
                break;
            }
            default: {
                itemView = null;
                System.out.println("NEWSADAPTER: Could not return any type @index("+cellCount+")");
            }
        }

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {

        int viewType = getItemViewType(position);

        switch (viewType){
            case FEATURE_TYPE: {
                System.out.println("NEWSADAPTER: Loading FEATURE");
                //DOES NOT BREAK INTENTINLY
            }
            case NORMAL_TYPE: {
                System.out.println("NEWSADAPTER: Loading NORMAL");
                NewsPackage news = (NewsPackage) newsList.get(position);
                    holder.title.setText(news.getTitle());
                        holder.title.setEllipsize(TextUtils.TruncateAt.END);
                        holder.title.setMaxLines(2);
                    holder.domain.setText(news.getDomain());
                    holder.time.setText(news.getPrettyUtc());
                if (news.getThumbnail() != null)
                    Picasso.with(mContext).load(news.getThumbnail()).fit().centerCrop().into(holder.thumbnail);

                break;
            }
            case AD_TYPE: {
                System.out.println("NEWSADAPTER: Loading AD");
                holder.adView.loadAd(new AdRequest.Builder().build());
                break;
            }
            default: {
            }
        }
    }

    @Override
    public int getItemCount() {
        return newsList.size(); // + (newsList.size() / 5);
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0)
            return FEATURE_TYPE;
        else if(newsList.get(position) instanceof AdPackage)
            return AD_TYPE;
        else
            return NORMAL_TYPE;
    }
}
