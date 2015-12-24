package be.tim.fajrero;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.github.pwittchen.prefser.library.Prefser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

public class Prefs {

    public static final String JSON_KEY_SSID = "ssid";
    public static final String JSON_KEY_LEVEL = "level";

    private Prefs() {}

    public static final String KEY_SSID = "be.tim.fajrero.Prefs.KEY.SSID";
    public static final String KEY_CLIENT_NAME = "be.tim.fajrero.Prefs.KEY.CLIENT_NAME";
    public static final String KEY_BROKER = "be.tim.fajrero.Prefs.KEY.BROKER";
    public static final String KEY_PASSWORD = "be.tim.fajrero.Prefs.KEY.PASSWORD";
    public static final String KEY_PASSWORD_HIDDEN = "be.tim.fajrero.Prefs.KEY.PASSWORD_HIDDEN";
    public static final String KEY_SCANNED_SSIDS = "be.tim.fajrero.Prefs.KEY.SCANNED_SSIDS";
    public static final String KEY_KNOWN_SSIDS= "be.tim.fajrero.Prefs.KEY.KNOWN_SSIDS";

    private static Prefser getPrefser(Context context) {
        return new Prefser(context);
    }

    public static boolean hasScannedSsids(Context context) {
        Prefser prefser = getPrefser(context);
        return !TextUtils.isEmpty(prefser.get(KEY_SCANNED_SSIDS, String.class, ""));
    }

    public static String getSsid(Context context) {
        Prefser prefser = getPrefser(context);
        return prefser.get(KEY_SSID, String.class, "");
    }

    public static void putSsidsFromScan(Context context, List<ScanResult> results) {
        Prefser prefser = getPrefser(context);
        JSONArray ssidArray = new JSONArray();
        for (ScanResult result : results) {
            JSONObject ssidObject = new JSONObject();
            try {
                ssidObject.put(JSON_KEY_SSID, result.SSID);
                ssidObject.put(JSON_KEY_LEVEL, result.level);
                ssidArray.put(ssidObject);
            } catch (JSONException e) {
                Log.e("Fajrero", "JSONException occured in putSsidsFromScan", e);
            }
        }
        prefser.put(KEY_SCANNED_SSIDS, ssidArray.toString());
    }

    public static void putSsidsFromConfiguredNetorks(Context context, List<WifiConfiguration> configuredNetworks) {
        Prefser prefser = getPrefser(context);
        JSONArray ssidArray = new JSONArray();
        for (WifiConfiguration configuration : configuredNetworks) {
            JSONObject ssidObject = new JSONObject();
            try {
                final String ssid = configuration.SSID.replace("\"", "");
                ssidObject.put(JSON_KEY_SSID, ssid);
                ssidArray.put(ssidObject);
            } catch (JSONException e) {
                Log.e("Fajrero", "JSONException occured in putSsidsFromConfiguredNetorks", e);
            }
        }
        prefser.put(KEY_KNOWN_SSIDS, ssidArray.toString());

    }

    public static List<String> getSsids(Context context) {
        Prefser prefser = getPrefser(context);
        String jsonScan = prefser.get(KEY_SCANNED_SSIDS, String.class, "");
        String jsonKnown = prefser.get(KEY_KNOWN_SSIDS, String.class, "");
        List<KnownSsid> ssidListScan = Collections.EMPTY_LIST;
        List<KnownSsid> ssidListKnown = Collections.EMPTY_LIST;

        try {
            // Get json array from prefs
            JSONArray ssidArray = new JSONArray(jsonScan);
            final int length = ssidArray.length();

            // Convert to ArrayList and strip own AP ssid from list
            ssidListScan = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                JSONObject ssidObject = ssidArray.getJSONObject(i);
                int level = ssidObject.optInt(JSON_KEY_LEVEL);
                String ssid = ssidObject.getString(JSON_KEY_SSID);
                if (!ssid.equals(MainActivity.AP_SSID_NAME)) {
                    final KnownSsid knownSsid = new KnownSsid(ssid, level);
                    ssidListScan.add(knownSsid);
                }
            }

        } catch (JSONException e) {
            Log.e("Fajrero", "JSONException occured in getSsids", e);
        } catch (Exception e) {
            Log.e("Fajrero", "Exception occured in getSsids!", e);
        }

        try {
            // Get json array from prefs
            JSONArray ssidArray = new JSONArray(jsonKnown);
            final int length = ssidArray.length();

            // Convert to ArrayList and strip own AP ssid from list
            ssidListKnown = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                JSONObject ssidObject = ssidArray.getJSONObject(i);
                String ssid = ssidObject.getString(JSON_KEY_SSID);
                if (!ssid.equals(MainActivity.AP_SSID_NAME)) {
                    final KnownSsid knownSsid = new KnownSsid(ssid, 0);
                    ssidListKnown.add(knownSsid);
                }
            }
        } catch (JSONException e) {
            Log.e("Fajrero", "JSONException occured in getSsids", e);
        } catch (Exception e) {
            Log.e("Fajrero", "Exception occured in getSsids!", e);
        }

        if (ssidListKnown.isEmpty() && ssidListScan.isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        if (ssidListKnown.isEmpty()) {
            return convertToStringArrayList(ssidListScan);
        }

        if (ssidListScan.isEmpty()) {
            return  convertToStringArrayList(ssidListKnown);
        }

        // Both lists are filled, merge results
        for (KnownSsid ssid : ssidListKnown) {
            if (!ssidListScan.contains(ssid)) {
                ssidListScan.add(ssid);
            }
        }
        return convertToStringArrayList(ssidListScan);
    }

    @NonNull
    private static List<String> convertToStringArrayList(List<KnownSsid> ssidListScan) {
        // Sort on level
        Collections.sort(ssidListScan, new KnownSsid.SsidComparator());

        // Remove duplicates, but keep the order
        LinkedHashSet<KnownSsid> ssidSet = new LinkedHashSet<>();
        ssidSet.addAll(ssidListScan);

        // Convert to List of strings for adapter
        List<String> stringList = new ArrayList<>(ssidSet.size());
        for (KnownSsid ssid : ssidSet) {
            stringList.add(ssid.getName());
        }

        return stringList;
    }

}
