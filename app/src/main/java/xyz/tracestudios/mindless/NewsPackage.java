package xyz.tracestudios.mindless;

import java.text.SimpleDateFormat;
import java.util.Date;

public class NewsPackage extends Package{

    String title, domain, url, thumbnail;
    long utc;

    public NewsPackage(String title, String domain, String url, String thumbnail, long utc) {
        this.title = title;
        this.domain = domain;
        this.url = url;
        this.thumbnail = thumbnail;
        this.utc = utc;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public long getUtc() {
        return utc;
    }

    public String getPrettyUtc(){
        Date date = new Date(getUtc() * 1000L);
        String datestr = new SimpleDateFormat("MMM d").format(date);
        return datestr;
    }

    public void setUtc(long utc) {
        this.utc = utc;
    }
}
