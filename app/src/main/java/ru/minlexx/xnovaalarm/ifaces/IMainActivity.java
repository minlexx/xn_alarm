package ru.minlexx.xnovaalarm.ifaces;

import ru.minlexx.xnovaalarm.RefresherService;

public interface IMainActivity {
    void notifyServiceStateChange();
    RefresherService getRefresherService();
}
