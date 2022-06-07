package com.prnt4b;

import static com.facebook.react.bridge.UiThreadUtil.runOnUiThread;

import android.annotation.SuppressLint;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

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

import org.jetbrains.annotations.Contract;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class EpsonModule extends ReactContextBaseJavaModule implements ReceiveListener {
    private static ReactApplicationContext reactContext;
    private static Printer mPrinterSelected;
    private static int columnWidth;
    private DeviceEventManagerModule.RCTDeviceEventEmitter mEmitter = null;

    EpsonModule(ReactApplicationContext context) {
        super(context);
        reactContext = context;
        columnWidth = 42;
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
        if (mPrinterSelected == null) {
            return;
        }

        JSONArray restaurants = order.getJSONArray("restaurants");
        JSONArray items = order.getJSONArray("items");
        JSONObject firstRestaurant = restaurants.getJSONObject(0);
        JSONObject ghostKitchen = order.getJSONObject("ghostKitchen");
        JSONObject account = ghostKitchen.getJSONObject("account");

        String orderType = order.getString("orderType");
        String timezone = ghostKitchen.getString("timezone");
        String currency = getCurrency(account.getString("currencyCode"));
        boolean isExternalDelivery = order.getBoolean("isExternalDelivery");

        try {
            mPrinterSelected.addPulse(Printer.PARAM_DEFAULT, Printer.PARAM_DEFAULT);

            PrinterUtils.alignCenter(mPrinterSelected);

            mPrinterSelected.addTextSize(2, 2);

            mPrinterSelected.addText(firstRestaurant.getString("name") + "\n");

            mPrinterSelected.addTextSize(1, 1);

            mPrinterSelected.addText("Order " + order.getString("id") + "\n");

            mPrinterSelected.addText(orderType.toUpperCase() + "\n");

            if(orderType.equals("delivery")){
                PrinterUtils.addBold(mPrinterSelected);
                String delivery = isExternalDelivery ? "External" : "Internal";
                mPrinterSelected.addText(delivery + " Delivery" + "\n");
                PrinterUtils.removeStyles(mPrinterSelected);
            }
            String created_at = convertDate(order.getString("created_at"));
            mPrinterSelected.addText("Placed: " + created_at + "\n");

            String scheduledAt = order.getString("scheduledAt");
            if(!scheduledAt.equals("") && !scheduledAt.equals("null")){
                 mPrinterSelected.addTextSmooth(Printer.FALSE);
                 mPrinterSelected.addTextSize(2, 2);
                 mPrinterSelected.addText("Scheduled Order " + convertDate(scheduledAt) + "\n");
                 mPrinterSelected.addTextSize(1, 1);
                 mPrinterSelected.addTextSmooth(Printer.TRUE);
             }

            if(orderType.equals("pickup")){
                int pickupEstimatedDuration = (order.getString("pickupEstimatedDuration").equals("null") || order.getString("pickupEstimatedDuration").equals("")) ? 0 : order.getInt("pickupEstimatedDuration");
                String deliveryEstimatedDurationDate = addSecondsToDate(created_at, pickupEstimatedDuration);
                mPrinterSelected.addText("Pickup By: " + deliveryEstimatedDurationDate + "\n");
            }
            else if (orderType.equals("delivery")){
                int deliveryEstimatedDuration = (order.getString("deliveryEstimatedDuration").equals("null") || order.getString("deliveryEstimatedDuration").equals("")) ? 0 : order.getInt("deliveryEstimatedDuration");
                String deliveryEstimatedDurationDate = addSecondsToDate(created_at, deliveryEstimatedDuration);

                mPrinterSelected.addText("Deliver By: " + deliveryEstimatedDurationDate + "\n");
                String deliveryInstructions = order.getString("deliveryInstructions");
                PrinterUtils.addUnderLine(mPrinterSelected);
                mPrinterSelected.addText("Delivery Instructions \n");
                PrinterUtils.removeStyles(mPrinterSelected);
                mPrinterSelected.addText(deliveryInstructions + "\n");
            }

            PrinterUtils.addUnderLine(mPrinterSelected);
            mPrinterSelected.addText("Customer Details \n");
            PrinterUtils.removeStyles(mPrinterSelected);

            boolean isCustomer = !order.getString("customer").equals("null") && !order.getString("customer").equals("");
            if(isCustomer){
                JSONObject customer = order.getJSONObject("customer");
                String firstName = customer.getString("firstName");
                String lastName = customer.getString("lastName");
                String phone = customer.getString("phone");
                String addressLine1 = customer.getString("addressLine1");
                String addressLine2 = customer.getString("addressLine2");
                String addressCity = customer.getString("addressCity");
                String addressState = customer.getString("addressState");
                String addressZIP = customer.getString("addressZIP");
                mPrinterSelected.addText(firstName + " " + lastName + "\n");
                mPrinterSelected.addText(phone + "\n");
                if (!addressLine1.equals("") && !addressLine1.equals("null")){
                    mPrinterSelected.addText(addressLine1 + "\n");
                }
                if (!addressLine2.equals("") && !addressLine2.equals("null")){
                    mPrinterSelected.addText(addressLine2 + "\n");
                }
                String address = "";
                if(!addressCity.equals("") && !addressCity.equals("null")){
                    address = addressCity;
                }
                if(!addressState.equals("") && !addressState.equals("null")){
                    if(address.equals("")){
                        address = addressState;
                    }
                    else {
                        address += ", " + addressState;
                    }
                }
                if(!addressZIP.equals("") && !addressZIP.equals("null")){
                    address += " " + addressState;
                }
                if(!address.equals("")){
                    mPrinterSelected.addText(address + "\n");
                }
            }
            else {
                JSONObject guest = order.getJSONObject("guest");
                String firstName = guest.getString("firstName");
                String lastName = guest.getString("lastName");
                String phone = guest.getString("phone");
                String addressLine1 = guest.getString("addressLine1");
                String addressLine2 = guest.getString("addressLine2");
                String city = guest.getString("city");
                String state = guest.getString("state");
                String ZIP = guest.getString("ZIP");
                mPrinterSelected.addText(firstName + " " + lastName + "\n");
                mPrinterSelected.addText(phone + "\n");
                if (!addressLine1.equals("") && !addressLine1.equals("null")){
                    mPrinterSelected.addText(addressLine1 + "\n");
                }
                if (!addressLine2.equals("") && !addressLine2.equals("null")){
                    mPrinterSelected.addText(addressLine2 + "\n");
                }
                String address = "";
                if(!city.equals("") && !city.equals("null")){
                    address = city;
                }
                if(!state.equals("") && !state.equals("null")){
                    if(address.equals("")){
                        address = state;
                    }
                    else {
                        address += ", " + state;
                    }
                }
                if(!ZIP.equals("") && !ZIP.equals("null")){
                    address += " " + ZIP;
                }
                if(!address.equals("")){
                    mPrinterSelected.addText(address + "\n");
                }
            }

            PrinterUtils.alignLeft(mPrinterSelected);
            PrinterUtils.addBold(mPrinterSelected);
            mPrinterSelected.addText("Order Details \n");
            PrinterUtils.removeStyles(mPrinterSelected);

            for (int r = 0; r < restaurants.length(); r++){
                JSONObject restaurant = restaurants.getJSONObject(r);
                String restaurantName = restaurant.getString("name");
                mPrinterSelected.addText(restaurantName + "\n");

                int restaurantId = Integer.parseInt(restaurant.getString("id"));
                for (int i = 0; i < items.length(); i++){
                    JSONObject item = items.getJSONObject(i);
                    int itemRestaurantId = Integer.parseInt(item.getString("restaurantId"));
                    if(itemRestaurantId == restaurantId){
                        JSONObject menuItem = item.getJSONObject("menuItem");
                        String menuItemName = menuItem.getString("name");
                        int itemQuantity = (item.getString("quantity").equals("") || item.getString("quantity").equals("null")) ? 0 : Integer.parseInt(item.getString("quantity"));
                        double cost = ((menuItem.getString("cost").equals("") || menuItem.getString("cost").equals("null")) ? 0 : Double.parseDouble(menuItem.getString("cost"))) / 100 * itemQuantity;
                        String costFormatted = numberFormat(cost);

                        String quantityNName = "(" + itemQuantity + ")" + menuItemName;
                        String currencyNCost = currency + costFormatted;
                        mPrinterSelected.addText(padLine(quantityNName, currencyNCost) + "\n");

                        String specialInstructions = item.getString("specialInstructions");
                        if(!specialInstructions.equals("") && !specialInstructions.equals("null")){
                            PrinterUtils.addUnderLineAndBold(mPrinterSelected);
                            mPrinterSelected.addText("Special Instructions:");
                            PrinterUtils.removeStyles(mPrinterSelected);
                            mPrinterSelected.addText(" " + specialInstructions + "\n");
                        }

                        JSONArray modifiers = item.getJSONArray("modifiers");
                        for (int m = 0; m < modifiers.length(); m++){
                            JSONObject modifier = modifiers.getJSONObject(m);
                            JSONArray selections = modifier.getJSONArray("selections");
                            for (int s = 0; s < selections.length(); s++){
                                JSONObject selection = selections.getJSONObject(s);
                                JSONObject menuItemModifierSelection = selection.getJSONObject("menuItemModifierSelection");
                                double costSel = ((menuItemModifierSelection.getString("cost").equals("") || menuItemModifierSelection.getString("cost").equals("null")) ? 0 : Double.parseDouble(menuItemModifierSelection.getString("cost"))) / 100 * itemQuantity;
                                String costSelFormatted = numberFormat(costSel);
                                int selQuantity = (selection.getString("quantity").equals("") || selection.getString("quantity").equals("null")) ? 0 : Integer.parseInt(selection.getString("quantity"));
                                String menuItemModifierSelectionName = menuItemModifierSelection.getString("name");
                                formatModSelections(selQuantity, menuItemModifierSelectionName, false, currency, costSelFormatted, itemQuantity, false);
                            }
                        }
                    }
                }
            }

            mPrinterSelected.addFeedLine(2);
            mPrinterSelected.addCut(Printer.CUT_FEED);
        }
        catch (Exception e) {
            mPrinterSelected.clearCommandBuffer();
            e.printStackTrace();
            Toast.makeText(reactContext, "Print transformed json failed", Toast.LENGTH_LONG).show();
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

    @NonNull
    @Contract(pure = true)
    private String numberFormat(double number){
        DecimalFormat df = new DecimalFormat("0.00");
        return df.format(number);
    }

    private String getCurrency(@NonNull String currency){
        if(currency.equals("ZAR"))
            return "R";

        Currency cur = Currency.getInstance(currency);
        return cur.getSymbol();
    }

    @NonNull
    private String addSecondsToDate(String dateString, int seconds) throws ParseException {
        String result = "";
        try
        {
            @SuppressLint("SimpleDateFormat") Date inputDate= new SimpleDateFormat("dd, MMMM yyyy HH:mm").parse(dateString);

            if (inputDate != null) {
                long allSeconds = inputDate.getTime();
                allSeconds = allSeconds + (seconds * 1000L);
                inputDate = new Date(allSeconds);
            }
            SimpleDateFormat outputDateFormat = new SimpleDateFormat("dd, MMMM yyyy HH:mm", Locale.ENGLISH);
            if (inputDate != null) {
                result = outputDateFormat.format(inputDate);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return result;
    }

    private String padLine(@Nullable String partOne, @Nullable String partTwo){
        if(partOne == null) {partOne = "";}
        if(partTwo == null) {partTwo = "";}
        String concat;
        if((partOne.length() + partTwo.length()) > columnWidth) {
            concat = partOne + " " + partTwo;
        } else {
            int padding = columnWidth - (partOne.length() + partTwo.length());
            concat = partOne + repeat(" ", padding) + partTwo;
        }
        return concat;
    }

    private void formatModSelections(int quantity, String name, boolean includeCost, String currencySymbol, String costFormatted, int itemQuantity, boolean enableItemQuantity) throws Epos2Exception {
        double cost = includeCost ? Double.parseDouble(costFormatted) : 0.0;

        String prefix = "";
        if(Pattern.matches("'/^(Add|Extra)(.*)$/i'", name) || cost > 0){
            prefix = "+ ";
        }
        if(Pattern.matches("'/^(Remove|No)(.*)$/i'", name)){
            prefix = "- ";
        }
        String left, right = "";
        if(includeCost){
            if(!enableItemQuantity){
                if(cost > 0){
                    if(quantity > 1){
                        left = MessageFormat.format("{0}({1}){2}", prefix, quantity, name);
                    }
                    else{
                        left = MessageFormat.format("{0}{1}", prefix, name);
                    }
                    right = MessageFormat.format("{0}{1}", currencySymbol, costFormatted);
                    mPrinterSelected.addText(padLine(left, right) + "\n");
                }
                else{
                    if(quantity > 1){
                        left = MessageFormat.format("{0}({1}){2}", prefix, quantity, name);
                    }
                    else{
                        left = MessageFormat.format("{0}{1}", prefix, name);
                    }
                    mPrinterSelected.addText(left + "\n");
                }
            }
            else{
                if(cost > 0){
                    if(quantity > 0){
                        left = MessageFormat.format("{0}({1}) {2}{3}", prefix, itemQuantity, quantity, name);
                    }
                    else{
                        left = MessageFormat.format("{0}({1}) {2}", prefix, itemQuantity, name);
                    }
                    right = MessageFormat.format("{0}{1}", currencySymbol, costFormatted);
                    mPrinterSelected.addText(padLine(left, right) + "\n");
                }
                else{
                    if(quantity > 0){
                        left = MessageFormat.format("{0}({1}) {2}{3}", prefix, itemQuantity, quantity, name);
                    }
                    else{
                        left = MessageFormat.format("{0}({1}) {2}", prefix, itemQuantity, name);
                    }
                    mPrinterSelected.addText(left + "\n");
                }
            }
        }
        else{
            if(quantity > 1){
                left = MessageFormat.format("{0}({1}){2}", prefix, quantity, name);
            }
            else{
                left = MessageFormat.format("{0}{1}", prefix, name);
            }
            mPrinterSelected.addText(left + "\n");
        }
    }


    /** utility: string repeat */
    protected String repeat(String str, int i){
        return new String(new char[i]).replace("\0", str);
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

class PrinterUtils {
    public static void addBold(@NonNull Printer mPrinterSelected){
        try {
            mPrinterSelected.addTextStyle(Printer.FALSE, Printer.FALSE, Printer.TRUE, Printer.FALSE);
        } catch (Epos2Exception e) {
            e.printStackTrace();
        }
    }
    public static void removeStyles(@NonNull Printer mPrinterSelected){
        try {
            mPrinterSelected.addTextStyle(Printer.FALSE, Printer.FALSE, Printer.FALSE, Printer.FALSE);
        } catch (Epos2Exception e) {
            e.printStackTrace();
        }
    }
    public static void addUnderLine(@NonNull Printer mPrinterSelected){
        try {
            mPrinterSelected.addTextStyle(Printer.FALSE, Printer.TRUE, Printer.FALSE, Printer.FALSE);
        } catch (Epos2Exception e) {
            e.printStackTrace();
        }
    }
    public static void addUnderLineAndBold(@NonNull Printer mPrinterSelected){
        try {
            mPrinterSelected.addTextStyle(Printer.FALSE, Printer.TRUE, Printer.TRUE, Printer.FALSE);
        } catch (Epos2Exception e) {
            e.printStackTrace();
        }
    }
    public static void alignLeft(@NonNull Printer mPrinterSelected){
        try {
            mPrinterSelected.addTextAlign(Printer.ALIGN_LEFT);
        } catch (Epos2Exception e) {
            e.printStackTrace();
        }
    }
    public static void alignCenter(@NonNull Printer mPrinterSelected){
        try {
            mPrinterSelected.addTextAlign(Printer.ALIGN_CENTER);
        } catch (Epos2Exception e) {
            e.printStackTrace();
        }
    }
}
