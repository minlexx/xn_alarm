package ru.minlexx.xnovaalarm.ifaces;

import java.net.HttpCookie;
import java.util.List;


public interface IMainActivity {
    void notifyServiceStateChange();

    void onXNovaLoginOK(List<HttpCookie> cookies);
    void onXNovaLoginFail(String errorStr);
}
