import React from 'react';
import { StyleSheet, View, Button } from 'react-native';
import { SubmitCompletedQueue, SubmitInCompletedQueue } from '../Services'

import { NativeModules } from 'react-native';
const EpsonModule = NativeModules.EpsonModule;

export const TransferDataToSDK = async (payload, id_ticket_queue, target, priterName) => {
  console.info(`Init transfer to sdk, ticket ${id_ticket_queue}, target ${target}, priterName ${priterName}`)
  const prntResult = await EpsonModule.rnReceiveData(payload, target, priterName);
  if (prntResult == "1") {
    console.info(`completed ticket ${id_ticket_queue}, target ${target}, priterName ${priterName}`)
    SubmitCompletedQueue(id_ticket_queue)
  }
  else{
    SubmitInCompletedQueue(id_ticket_queue)
    console.warn(`failed ticket ${id_ticket_queue}, target ${target}, priterName ${priterName}`)
  }
  return prntResult;
}

export const StartDiscovery = async () => {
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
