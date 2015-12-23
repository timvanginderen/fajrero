package be.tim.fajrero;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.util.Log;

import com.github.pwittchen.prefser.library.Prefser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
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
    public static final String KEY_KNOWN_SSIDS= "be.tim.fajrero.Prefs.KEY.KNOWN_SSIDS";


    private static Prefser getPrefser(Context context) {
        return new Prefser(context);
    }

    public static void putSsids(Context context, List<ScanResult> results) {
        Prefser prefser = getPrefser(context);
        JSONArray ssidArray = new JSONArray();
        for (ScanResult result : results) {
            JSONObject ssidObject = new JSONObject();
            try {
                ssidObject.put(JSON_KEY_SSID, result.SSID);
                ssidObject.put(JSON_KEY_LEVEL, result.level);
                ssidArray.put(ssidObject);
            } catch (JSONException e) {
                Log.e("Fajrero", "JSONException occured in putSsids", e);
            }
        }
        prefser.put(KEY_KNOWN_SSIDS, ssidArray.toString());
    }

    public static List<KnownSsid> getSsids(Context context) {
        Prefser prefser = getPrefser(context);
        String json = prefser.get(KEY_KNOWN_SSIDS, String.class, "");
        try {
            JSONArray ssidArray = new JSONArray(json);
            final int length = ssidArray.length();
            List<KnownSsid> ssidList = new ArrayList<KnownSsid>(length);

            for (int i = 0; i < length; i++) {
                JSONObject ssidObject = ssidArray.getJSONObject(i);
                int level = ssidObject.getInt(JSON_KEY_LEVEL);
                String ssid = ssidObject.getString(JSON_KEY_SSID);
                final KnownSsid knownSsid = new KnownSsid(ssid, level);
                ssidList.add(knownSsid);
            }

            return ssidList;
        } catch (JSONException e) {
            Log.e("Fajrero", "JSONException occured in getSsids", e);
        } catch (Exception e) {
            Log.e("Fajrero", "Exception occured in getSsids!", e);
        }
        return Collections.EMPTY_LIST;
    }

}
