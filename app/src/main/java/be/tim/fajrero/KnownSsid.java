package be.tim.fajrero;

import java.util.Comparator;

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

    @Override
    public int hashCode() {
        if (this.getName() == null) {
            return  super.hashCode();
        }
        return this.getName().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof KnownSsid)) {
            return false;
        }

        KnownSsid otherSsid = (KnownSsid) other;
        if (this.getName() == null) {
            return  otherSsid.getName() == null;
        } else {
            return this.getName().equals(otherSsid.getName());
        }
    }

    public static class SsidComparator implements Comparator<KnownSsid> {
        @Override
        public int compare(KnownSsid o1, KnownSsid o2) {
            return Integer.compare(o2.getLevel(), o1.getLevel());
        }
    }

}
