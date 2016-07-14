package ru.minlexx.xnovaalarm.pojo;

import java.util.Locale;


public class XNFlight {
    String mission;
    String coordFrom;
    String coordTo;
    int timeLeft;
    boolean isReturn;

    public XNFlight() {
        mission = "";
        coordFrom = "";
        coordTo = "";
        timeLeft = -1;
        isReturn = false;
    }

    @Override
    public int hashCode() {
        int ret = timeLeft;
        if (mission != null) ret += mission.hashCode();
        if (coordFrom != null) ret += coordFrom.hashCode();
        if (coordTo != null) ret += coordTo.hashCode();
        return ret;
    }

    @Override
    public boolean equals(Object other) {
        return (this == other) || (
                (other instanceof XNFlight) &&
                        (this.hashCode() == other.hashCode())
        );
    }

    @Override
    public String toString() {
        final String retStr = isReturn ? " return" : "";
        return String.format(Locale.getDefault(), "Flight (%s, %d sec%s)",
                mission, timeLeft, retStr);
    }
}
