import React from 'react';
import { StyleSheet, View, Button } from 'react-native';
import { SubmitCompletedQueue } from './Services'

import { NativeModules } from 'react-native';
const EpsonModule = NativeModules.EpsonModule;

export const TransferDataToSDK = async (payload, id_ticket_queue) => {
  console.log(`Init transfer to sdk, ticket ${id_ticket_queue}`)
  const prntResult = await EpsonModule.rnReceiveData(payload);
  if (prntResult == true) {
    SubmitCompletedQueue(id_ticket_queue)
  }
}

export const StartDiscovery = async () => {
  console.log("Running discover ...");
  return await EpsonModule.rnDiscover();
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

  return (
    <View>
      <View style={styles.button}>
        <Button title="try bluetooth connect" onPress={bluetoothFromEpsonNative} />
      </View>
      <View style={styles.button}>
        <Button title="try usb connect" onPress={usbFromEpsonNative} />
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
