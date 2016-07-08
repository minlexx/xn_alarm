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
    // for each URI there is a cookie list
    protected Map<URI, List<HttpCookie>> _memCookiesMap;

    public MyCookieStore() {
        this._memCookiesMap = new HashMap<>(5);
    }

    public void storeCookiesTo(SharedPreferences.Editor prefs_editor) {
        prefs_editor.putString("cookie_u5_full", "N");
        // u5_id, u5_secret
    }

    public void loadCookiesFrom(SharedPreferences prefs) {
        prefs.getString("cookie_u5_full", "N");
        // u5_id, u5_secret
    }

    @Override
    public void add(URI uri, HttpCookie cookie) {
        if (cookie == null) return; // invalid arg!
        if (uri != null) {
            Log.d(TAG, String.format("CookieStore::add(\"%s\", \"%s\")",
                    uri.toString(), cookie.toString()));
        } else {
            Log.d(TAG, String.format("CookieStore::add(null, \"%s\")",
                    cookie.toString()));
        }

        if (uri == null) {
            // sorry I dont support null URIs atm
            Log.e(TAG, "CookieStore::add(): sorry I dont support null URIs atm");
            return;
        }

        if (!_memCookiesMap.containsKey(uri)) {
            _memCookiesMap.put(uri, new ArrayList<HttpCookie>(3));
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

    @Override
    public List<HttpCookie> get(URI uri) {
        if (uri == null) {
            throw new NullPointerException("uri == null");
        }
        //
        Log.d(TAG, String.format("CookieStore::get(\"%s\")", uri.toString()));
        //
        // this should never return null
        List<HttpCookie> ret = new ArrayList<HttpCookie>();
        List<HttpCookie> cookiesForUrl = _memCookiesMap.get(uri);
        if (cookiesForUrl != null)
            ret.addAll(cookiesForUrl);
        //
        Log.d(TAG, String.format("CookieStore::get(\"%s\"): returning %d items",
                uri.toString(), ret.size()));
        //
        return ret;
    }

    @Override
    public List<HttpCookie> getCookies() {
        Log.i(TAG, "CookieStore::getCookies() called");
        // all cookies for all URLs
        final List<HttpCookie> ret = new ArrayList<>();
        for(List<HttpCookie> list: _memCookiesMap.values()) {
            ret.addAll(list);
        }
        return ret;
    }

    @Override
    public List<URI> getURIs() {
        Log.i(TAG, "CookieStore::getURIs() called");
        final List<URI> uris = new ArrayList<>();
        for (URI uri: _memCookiesMap.keySet()) {
            uris.add(uri);
        }
        return uris;
    }

    @Override
    public boolean remove(URI uri, HttpCookie cookie) {
        if (cookie == null) return false;
        if (uri != null) {
            Log.i(TAG, String.format("CookieStore::remove(\"%s\", \"%s\")",
                    uri.toString(), cookie.toString()));
        } else {
            Log.i(TAG, String.format("CookieStore::remove(null, \"%s\")",
                    cookie.toString()));
        }
        return false;
    }

    @Override
    public boolean removeAll() {
        Log.i(TAG, "CookieStore::removeAll() called");
        _memCookiesMap = new HashMap<>(5); // haha
        return true;
    }
}
