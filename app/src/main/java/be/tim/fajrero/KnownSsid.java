package be.tim.fajrero;

public class KnownSsid {

    private String name;
    private int level; //The detected signal level in dBm, also known as the RSSI.

    public KnownSsid(String name, int level) {
        this.name = name;
        this.level = level;
    }

    public String getName() {
        return name;
    }

    public int getLevel() {
        return level;
    }
}
