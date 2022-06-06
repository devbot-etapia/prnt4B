import React from 'react';
import {StyleSheet, View, Button, DeviceEventEmitter} from 'react-native';

import {NativeModules} from 'react-native';
const EpsonModule = NativeModules.EpsonModule;

export const TransferDataToSDK = async (payload) => {
  const result = await EpsonModule.rnReceiveData(payload);
}

export const StartDiscovery = async () => {
  console.log("Running discover ...");
  await EpsonModule.rnDiscover();
}

export const GetConstants = async () => {
  const constants = EpsonModule.getConstants();
  console.log(constants);
}

const EpsonNative = (props) => {
  const bluetoothFromEpsonNative = () => {
    EpsonModule.rnBluetooth();
  };

  const usbFromEpsonNative = () => {
    EpsonModule.rnUSB();
  };

  const constantsFromEpsonNative = () => {
    const constants = EpsonModule.getConstants();
    console.log(constants);
  };

  const discoverFromEpsonNative = async () => {
    const result = await EpsonModule.rnDiscover();
    console.log(result);
    if(result){
      console.log('discover ok');
    }
    else{
      console.log('devices not found');
    }
  };

  return (
    <View>
      {/* <View style={styles.button}>
        <Button title="try string" onPress={stringFromEpsonNative} />
      </View> */}
      <View style={styles.button}>
        <Button title="try bluetooth connect" onPress={bluetoothFromEpsonNative} />
      </View>
      <View style={styles.button}>
        <Button title="try usb connect" onPress={usbFromEpsonNative} />
      </View>
      <View style={styles.button}>
        <Button title="try discover" onPress={discoverFromEpsonNative} />
      </View>
      <View style={styles.button}>
        <Button title="get constants" color='#C00' onPress={constantsFromEpsonNative} />
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  button: {
    marginTop: 15,
    marginHorizontal: 24,
    paddingVertical: 5,
  }
});

export default EpsonNative;
