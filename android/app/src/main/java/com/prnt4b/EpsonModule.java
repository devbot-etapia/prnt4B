package com.prnt4b;

import static com.facebook.react.bridge.UiThreadUtil.runOnUiThread;

import android.annotation.SuppressLint;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.epson.epos2.printer.PrinterStatusInfo;
import com.epson.epos2.printer.ReceiveListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import com.epson.epos2.discovery.Discovery;
import com.epson.epos2.discovery.DiscoveryListener;
import com.epson.epos2.discovery.FilterOption;
import com.epson.epos2.Epos2Exception;
import com.epson.epos2.printer.Printer;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class EpsonModule extends ReactContextBaseJavaModule implements ReceiveListener {
    private static ReactApplicationContext reactContext;
    private static Printer mPrinterSelected;
    private DeviceEventManagerModule.RCTDeviceEventEmitter mEmitter = null;

    EpsonModule(ReactApplicationContext context) {
        super(context);
        reactContext = context;
    }

    @Override
    public void onPtrReceive(Printer printer, int i, PrinterStatusInfo printerStatusInfo, String s) {
    }

    @ReactMethod(isBlockingSynchronousMethod = true)
    public boolean rnReceiveData(String jsonPayload) {
        try {
            if(jsonPayload == null){
                return false;
            }

            JSONObject jsonObject = new JSONObject(jsonPayload);
            String locationType = jsonObject.getJSONObject("data").getString("locationType");
            String mac = jsonObject.getString("mac");
            JSONObject data = jsonObject.getJSONObject("data");

            mPrinterSelected = findPrinter();

            if(locationType.equals("Restaurant")){
                createRestaurantReceipt(data);
            }
            else if (locationType.equals("Ghost Kitchen")){
                createGhostKitchenReceipt(data);
            }

            connectToPrinter();
            printReceipt();
            disconnectToPrinter();
            return true;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    @ReactMethod(isBlockingSynchronousMethod = true)
    public boolean rnDiscover() {
        stopDiscovery();
        try {
            runDiscover();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //stopDiscovery();
        return true;
    }

    @ReactMethod
    public void rnBluetooth() {
        Log.i(null, "testConnect: initial");
        try {
            //BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

            //if (ActivityCompat.checkSelfPermission(reactContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            //return;
            //}
            //Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();

            mPrinterSelected = findPrinter();
            if (mPrinterSelected != null) {
                mPrinterSelected.connect("BT:00:01:90:76:E1:67", 300000);
                createTestReceipt("BLUETOOTH");
                printReceipt();
                mPrinterSelected.disconnect();
                Log.d(null, "testConnect: after disconnect");
            }
        } catch (Epos2Exception e) {
            e.printStackTrace();
            Toast.makeText(reactContext, "testBluetooth: error, " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @ReactMethod
    public void rnUSB() {
        Toast.makeText(reactContext, "testUSB: initial", Toast.LENGTH_SHORT).show();
        try {
            Printer mPrinterSelected = findPrinter();
            if (mPrinterSelected != null) {
                mPrinterSelected.connect("USB:", 300000);
                createTestReceipt("USB");
                printReceipt();
                Toast.makeText(reactContext, "testUSB: after connect", Toast.LENGTH_LONG).show();
                mPrinterSelected.disconnect();
                Toast.makeText(reactContext, "testUSB: after disconnect", Toast.LENGTH_LONG).show();
            }
        } catch (Epos2Exception e) {
            e.printStackTrace();
            Toast.makeText(reactContext, "testUSB: error, " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @NonNull
    @Override
    public String getName() {
        return "EpsonModule";
    }

    private void runDiscover(){
        FilterOption filterOption = new FilterOption();
        filterOption.setPortType(Discovery.PORTTYPE_ALL);
        filterOption.setDeviceModel(Discovery.MODEL_ALL);
        filterOption.setDeviceType(Discovery.TYPE_PRINTER);
        filterOption.setEpsonFilter(Discovery.FILTER_NAME);
        filterOption.setUsbDeviceName(Discovery.TRUE);

        try {
            Discovery.start(reactContext.getBaseContext(), filterOption, mDiscoveryListener);
        } catch (Exception e) {
            //ShowMsg.showException(e, "start", mContext);
            Toast.makeText(reactContext, "testDiscover: error, " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void SendPrintersEvent(String EventName, String DeviceName, String MACAddress, String Target, String BDAddress, String IpAddress){
        WritableMap params = Arguments.createMap();
        params.putString("DeviceName", DeviceName);
        params.putString("MACAddress", MACAddress);
        params.putString("Target", Target);
        params.putString("BDAddress", BDAddress);
        params.putString("IpAddress", IpAddress);

        if(mEmitter == null){
            mEmitter = getReactApplicationContext().getJSModule((DeviceEventManagerModule.RCTDeviceEventEmitter.class));
        }
        if(mEmitter != null){
            mEmitter.emit(EventName, params);
        }
    }

    private final DiscoveryListener mDiscoveryListener = deviceInfo -> runOnUiThread(new Runnable() {
        @Override
        public synchronized void run() {
            Log.i(null, "DiscoveryListener: listening to Target, " + deviceInfo.getTarget());
            Log.i(null, "DiscoveryListener: listening to Device, " + deviceInfo.getDeviceName());
            Log.i(null, "DiscoveryListener: listening to Bluetooth, " + deviceInfo.getBdAddress());

            String DeviceName = deviceInfo.getDeviceName();
            String MACAddress = deviceInfo.getMacAddress();
            String Target = deviceInfo.getTarget();
            String BDAddress = deviceInfo.getBdAddress();
            String IpAddress = deviceInfo.getIpAddress();

            SendPrintersEvent("printers", DeviceName, MACAddress, Target, BDAddress, IpAddress);
        }
    });

    private void stopDiscovery() {
        try {
            Discovery.stop();
        }
        catch (Epos2Exception e) {
            e.printStackTrace();
        }
    }

    @Nullable
    private Printer findPrinter (){
        // Converts the printer name into PrinterSeries
        int printerSeries = convertPrinterNameToPrinterSeries("TM_m10_008721");
        try {
            //return new Printer(Printer.TM_M10, Printer.MODEL_ANK, reactContext);
            return new Printer(printerSeries, Printer.MODEL_ANK, reactContext);
        } catch (Epos2Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private int convertPrinterNameToPrinterSeries(@NonNull String printerName) {
        int printerSeries = Printer.TM_T88;
        if(printerName.contains("TM-T88V")){
            printerSeries = Printer.TM_T88;
        }else if(printerName.contains("TM-m10")){
            printerSeries = Printer.TM_M10;
        }else if(printerName.contains("TM-m30")){
            printerSeries = Printer.TM_M30;
        }else if(printerName.contains("TM-P20")){
            printerSeries = Printer.TM_P20;
        }else if(printerName.contains("TM-P60II")){
            printerSeries = Printer.TM_P60II;
        }else if(printerName.contains("TM-P80")){
            printerSeries = Printer.TM_P80;
        }
        // else{
        // Depending on the printer, add conversion processes
        // }
        return printerSeries;
    }

    private void createTestReceipt(String printingMethod) {
        StringBuilder textData = new StringBuilder();
        final int barcodeWidth = 2;
        final int barcodeHeight = 100;

        if (mPrinterSelected == null) {
            return;
        }

        try {
            mPrinterSelected.addPulse(Printer.PARAM_DEFAULT, Printer.PARAM_DEFAULT);

            mPrinterSelected.addTextAlign(Printer.ALIGN_CENTER);

            mPrinterSelected.addTextSize(2, 2);

            mPrinterSelected.addText(printingMethod + "\n");

            mPrinterSelected.addTextSize(1, 1);

            textData.append("DEVELOPER – OMAR MARTINEZ\n");

            mPrinterSelected.addText(textData.toString());
            textData.delete(0, textData.length());

            textData.append("400 MOUSEGAMER         30.00 R\n");
            textData.append("410 HDMI CABLE          9.00 R\n");
            textData.append("------------------------------\n");

            mPrinterSelected.addText(textData.toString());
            textData.delete(0, textData.length());

            textData.append("SUBTOTAL                39.00\n");
            textData.append("IMPOSTS                6.24\n");

            mPrinterSelected.addText(textData.toString());
            textData.delete(0, textData.length());

            mPrinterSelected.addTextSize(2, 2);

            mPrinterSelected.addText("TOTAL    45.24\n");

            mPrinterSelected.addTextSize(1, 1);

            mPrinterSelected.addFeedLine(1);

            mPrinterSelected.addBarcode("01209457",
                    Printer.BARCODE_CODE39,
                    Printer.HRI_BELOW,
                    Printer.FONT_A,
                    barcodeWidth,
                    barcodeHeight);

            mPrinterSelected.addCut(Printer.CUT_FEED);
        }
        catch (Exception e) {
            mPrinterSelected.clearCommandBuffer();
            e.printStackTrace();
        }
    }

    private void createRestaurantReceipt(JSONObject jsonObject) throws JSONException {
        StringBuilder textData = new StringBuilder();

        if (mPrinterSelected == null) {
            return;
        }

        JSONArray restaurants = jsonObject.getJSONArray("restaurants");
        //Object restaurant = restaurants.get(0);
        String restaurantName = "Chanchito Feliz";
        try {
            mPrinterSelected.addPulse(Printer.PARAM_DEFAULT, Printer.PARAM_DEFAULT);

            mPrinterSelected.addTextAlign(Printer.ALIGN_CENTER);

            mPrinterSelected.addTextSize(2, 2);

            mPrinterSelected.addText(restaurantName + "\n");

            mPrinterSelected.addTextSize(1, 1);

            textData.append("DEVELOPER – OMAR MARTINEZ\n");

            mPrinterSelected.addText(textData.toString());
            textData.delete(0, textData.length());

            mPrinterSelected.addTextSize(2, 2);

            mPrinterSelected.addText("TOTAL    45.24\n");

            mPrinterSelected.addFeedLine(1);
            mPrinterSelected.addCut(Printer.CUT_FEED);
        }
        catch (Exception e) {
            mPrinterSelected.clearCommandBuffer();
            e.printStackTrace();
        }
    }

    private void createGhostKitchenReceipt(JSONObject order) throws JSONException {
        StringBuilder textData = new StringBuilder();

        if (mPrinterSelected == null) {
            return;
        }

        JSONArray restaurants = order.getJSONArray("restaurants");
        JSONObject restaurant = restaurants.getJSONObject(0);
        String orderType = order.getString("orderType");
        boolean isExternalDelivery = order.getBoolean("isExternalDelivery");

        try {
            mPrinterSelected.addPulse(Printer.PARAM_DEFAULT, Printer.PARAM_DEFAULT);

            mPrinterSelected.addTextAlign(Printer.ALIGN_CENTER);

            mPrinterSelected.addTextSize(2, 2);

            mPrinterSelected.addText(restaurant.getString("name") + "\n");

            mPrinterSelected.addTextSize(1, 1);

            textData.append("Order ").append(order.getString("id")).append("\n");

            mPrinterSelected.addFeedLine(1);
            mPrinterSelected.addText(orderType + "\n");

            if(orderType.equals("delivery")){
                mPrinterSelected.addTextStyle(Printer.FALSE, Printer.FALSE, Printer.TRUE, Printer.FALSE);
                String delivery = isExternalDelivery ? "External" : "Internal";
                textData.append(delivery).append(" Delivery").append("\n");
                mPrinterSelected.addTextStyle(Printer.FALSE, Printer.FALSE, Printer.FALSE, Printer.FALSE);
            }

            textData.append("Placed ").append(convertDate(order.getString("created_at"))).append("\n");

            Object scheduledAt = order.get("scheduledAt");
            // if(scheduledAt != null){
            // Do negative text
            // }

            mPrinterSelected.addText(textData.toString());
            textData.delete(0, textData.length());

            mPrinterSelected.addTextSize(2, 2);

            mPrinterSelected.addTextAlign(Printer.ALIGN_LEFT);
            mPrinterSelected.addText("TOTAL");
            mPrinterSelected.addTextAlign(Printer.ALIGN_RIGHT);
            mPrinterSelected.addText("0.00");

            mPrinterSelected.addFeedLine(1);
            mPrinterSelected.addCut(Printer.CUT_FEED);
        }
        catch (Exception e) {
            mPrinterSelected.clearCommandBuffer();
            e.printStackTrace();
        }
    }

    @NonNull
    private String convertDate(String dateString) throws ParseException {
        String result = "";
        try
        {
            String inputDateString = "2022-05-25T09:18:25.758Z";

            // Input Date String Format
            @SuppressLint("SimpleDateFormat") SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

            Date inputDate = inputDateFormat.parse(inputDateString);

            //Required output date string Format
            SimpleDateFormat outputDateFormat = new SimpleDateFormat("dd, MMMM yyyy HH:mm", Locale.ENGLISH);

            result = outputDateFormat.format(inputDate);

        } catch (ParseException e) {
            e.printStackTrace();
        }

        return result;
    }

    private void printReceipt() {
        if (mPrinterSelected == null) {
            return;
        }

        try {
            mPrinterSelected.sendData(Printer.PARAM_DEFAULT);
        }
        catch (Exception e) {
            mPrinterSelected.clearCommandBuffer();
            try {
                mPrinterSelected.disconnect();
            }
            catch (Exception ex) {
                // Do nothing
            }
        }
    }

    private void connectToPrinter(){
        if (mPrinterSelected != null) {
            try {
                mPrinterSelected.connect("BT:00:01:90:76:E1:67", 300000);
            } catch (Epos2Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void disconnectToPrinter(){
        if (mPrinterSelected != null) {
            try {
                mPrinterSelected.disconnect();
            } catch (Epos2Exception e) {
                e.printStackTrace();
            }
        }
    }
}
