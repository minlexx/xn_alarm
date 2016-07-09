package ru.minlexx.xnovaalarm.ifaces;

public interface IMainActivity {
    void notifyServiceStateChange();

    void onXNovaLoginOK();
    void onXNovaLoginFail(String errorStr);
}
