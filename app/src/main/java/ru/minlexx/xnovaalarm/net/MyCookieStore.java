package ru.minlexx.xnovaalarm.net;

import android.content.SharedPreferences;
import android.util.Log;

import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MyCookieStore implements CookieStore {

    private static final String TAG = MyCookieStore.class.getName();

    private static final String PREFS_COOKIE_U5_FULL = "u5_full";
    private static final String PREFS_COOKIE_U5_ID = "u5_id";
    private static final String PREFS_COOKIE_U5_SECRET = "u5_secret";

    // for each URI there is a cookie list
    protected Map<URI, List<HttpCookie>> _memCookiesMap;

    public MyCookieStore() {
        this._memCookiesMap = new HashMap<>(5);
    }

    public void storeCookiesTo(SharedPreferences.Editor prefs_editor) {
        // store cookies values
        HttpCookie cook = getCookieByName(PREFS_COOKIE_U5_ID);
        if (cook != null) {
            prefs_editor.putString(PREFS_COOKIE_U5_ID, cook.getValue());
        }
        cook = getCookieByName(PREFS_COOKIE_U5_SECRET);
        if (cook != null) {
            prefs_editor.putString(PREFS_COOKIE_U5_SECRET, cook.getValue());
        }
        cook = getCookieByName(PREFS_COOKIE_U5_FULL);
        if (cook != null) {
            prefs_editor.putString(PREFS_COOKIE_U5_FULL, cook.getValue());
        }
    }

    public void loadCookiesFrom(SharedPreferences prefs) {
        final String u5_full = prefs.getString(PREFS_COOKIE_U5_FULL, "N");
        final String u5_id = prefs.getString(PREFS_COOKIE_U5_ID, "0");
        final String u5_secret = prefs.getString(PREFS_COOKIE_U5_SECRET, "");
        //
        // check if we actually loaded them
        if (u5_id.equals("0") || u5_secret.isEmpty()) {
            Log.d(TAG, "loadCookiesFrom(): no cookies were restored.");
            return;
        }
        //
        final HttpCookie cfull = new HttpCookie(PREFS_COOKIE_U5_FULL, u5_full);
        final HttpCookie cid = new HttpCookie(PREFS_COOKIE_U5_ID, u5_id);
        final HttpCookie csecret = new HttpCookie(PREFS_COOKIE_U5_SECRET, u5_secret);
        //
        final List<HttpCookie> cooks = new ArrayList<>(3);
        cooks.add(cfull);
        cooks.add(cid);
        cooks.add(csecret);
        //
        for(HttpCookie cook: cooks) {
            cook.setDomain("uni5.xnova.su");
            cook.setPath("/");
            cook.setVersion(0);
            cook.setMaxAge(-1); // forever until browser shutdown
            //
            this.add(null, cook);
        }
    }

    public synchronized HttpCookie getCookieByName(String name) {
        final List<HttpCookie> all = getCookies();
        for(HttpCookie cook: all) {
            if (cook.getName().equals(name))
                return cook;
        }
        return null;
    }


    @Override
    public synchronized void add(URI uri, HttpCookie cookie) {
        if (cookie == null) {
            throw new NullPointerException("cookie == null");
        }
        if (uri != null) {
            Log.d(TAG, String.format("CookieStore::add(\"%s\", \"%s\")",
                    uri.toString(), cookie.toString()));
        } else {
            Log.d(TAG, String.format("CookieStore::add(null, \"%s\")",
                    cookie.toString()));
        }

        //if (uri == null) {
        //    // actually, HashMap allows null keys! this is OK
        //    Log.e(TAG, "CookieStore::add(): sorry I dont support null URIs atm");
        //    return;
        //}

        if (!_memCookiesMap.containsKey(uri)) {
            _memCookiesMap.put(uri, new ArrayList<HttpCookie>(5));
        }
        final List<HttpCookie> cl = _memCookiesMap.get(uri);
        // check if there already exists a cookie with such name in a list
        for (HttpCookie cook: cl) {
            if (cookie.equals(cook)) {
                Log.d(TAG, String.format(
                        "CookieStore::add(): removing already existing cookie \"%s\"",
                        cookie.getName()));
                cl.remove(cook);
                break;
            }
        }
        cl.add(cookie);
    }

    /************************************************************************************
     CookieStore::get("http://uni5.xnova.su:80/login/")
     CookieStore::get("http://uni5.xnova.su:80/login/"): returning 0 items
     CookieStore::add("http://uni5.xnova.su:80/login/", "PHPSESSID=tpvph...7pn86")
     CookieStore::add("http://uni5.xnova.su:80/login/", "u5_id=87")
     CookieStore::add("http://uni5.xnova.su:80/login/", "u5_secret=c01aa...0b87")
     CookieStore::add("http://uni5.xnova.su:80/login/", "u5_full=N")
     CookieStore::add("http://uni5.xnova.su:80/login/", "u5_id=87")
     CookieStore::add(): removing already existing cookie "u5_id"
     CookieStore::add("http://uni5.xnova.su:80/login/", "u5_secret=c01aa...0b87")
     CookieStore::add(): removing already existing cookie "u5_secret"
     CookieStore::add("http://uni5.xnova.su:80/login/", "u5_full=N")
     CookieStore::add(): removing already existing cookie "u5_full"
     CookieStore::add("http://uni5.xnova.su:80/login/", "u5_id=87")
     CookieStore::add(): removing already existing cookie "u5_id"
     CookieStore::add("http://uni5.xnova.su:80/login/", "u5_secret=c01aa...0b87")
     CookieStore::add(): removing already existing cookie "u5_secret"
     CookieStore::add("http://uni5.xnova.su:80/login/", "u5_full=N")
     CookieStore::add(): removing already existing cookie "u5_full"
     ************************************************************************************/

    @Override
    public synchronized List<HttpCookie> get(URI uri) {
        if (uri == null) {
            throw new NullPointerException("uri == null");
        }
        //
        Log.d(TAG, String.format("CookieStore::get(\"%s\")", uri.toString()));
        //
        // this should never return null, return at least empty list
        final List<HttpCookie> ret = new ArrayList<>();
        //
        // 1. Find all cookies for requested url
        final List<HttpCookie> cookiesForUrl = _memCookiesMap.get(uri);
        if (cookiesForUrl != null) {
            ret.addAll(cookiesForUrl);
        }
        //
        // 2. Find all cookies that match URL's domain name (host)
        final String domain = uri.getHost();
        for(List<HttpCookie> list: _memCookiesMap.values()) {
            for(HttpCookie candidate: list) {
                final String domain2 = candidate.getDomain();
                if (HttpCookie.domainMatches(domain, domain2) /*&& !candidate.hasExpired()*/)
                    ret.add(candidate);
                //if (candidate.hasExpired())
                // we do not currently check expiration dates :D
            }
        }
        //
        Log.d(TAG, String.format("CookieStore::get(\"%s\"): returning %d items",
                uri.toString(), ret.size()));
        //
        return ret;
    }

    @Override
    public synchronized List<HttpCookie> getCookies() {
        Log.i(TAG, "CookieStore::getCookies() called");
        // all cookies for all URLs
        final List<HttpCookie> ret = new ArrayList<>();
        for(List<HttpCookie> list: _memCookiesMap.values()) {
            ret.addAll(list);
        }
        return ret;
    }

    @Override
    public synchronized List<URI> getURIs() {
        Log.i(TAG, "CookieStore::getURIs() called");
        final List<URI> uris = new ArrayList<>();
        for (URI uri: _memCookiesMap.keySet()) {
            uris.add(uri);
        }
        return uris;
    }

    @Override
    public synchronized boolean remove(URI uri, HttpCookie cookie) {
        if (cookie == null) return false;
        if (uri != null) {
            Log.i(TAG, String.format("CookieStore::remove(\"%s\", \"%s\")",
                    uri.toString(), cookie.toString()));
        } else {
            Log.i(TAG, String.format("CookieStore::remove(null, \"%s\")",
                    cookie.toString()));
        }
        final List<HttpCookie> list = _memCookiesMap.get(uri);
        if (list != null) {
            list.remove(cookie);
            return true;
        }
        return false;
    }

    @Override
    public synchronized boolean removeAll() {
        Log.i(TAG, "CookieStore::removeAll() called");
        _memCookiesMap = new HashMap<>(5); // haha
        return true;
    }
}
