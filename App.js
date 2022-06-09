import React, { useEffect, useState, useContext } from 'react';
import { ScrollView, StatusBar, StyleSheet, Text, useColorScheme, View, Button, DeviceEventEmitter } from 'react-native';

import { Colors } from 'react-native/Libraries/NewAppScreen';
import { SubmitMACs } from './src/components/Services';
import InputText from './src/components/Input';
import EpsonNative, { TransferDataToSDK, StartDiscovery } from './src/components/EpsonNative';
import Printers from './src/components/Printers';
import { Section } from './src/components/Section';
import PrintersContext, { PrintersContexProvider } from './src/context/Printer-context';
import { useTimer } from './src/hooks/Timer'

const App = () => {
  const isDarkMode = useColorScheme() === 'dark';
  const [periodicity, setPeriodicity] = useState("30");
  const [isDiscoverEnabled , setIsDiscoverEnabled] = useState(false);

  const [printer , setPrinter] = useState(null);
  
  useEffect(() => {
    restartDiscovery()
    DeviceEventEmitter.addListener("printers", (printerListened) => {
      setPrinter(printerListened)
    });
  }, []);

  const fetchToFindPrintings = async () => {
    const data = await SubmitMACs();
    if(data.data)
      await TransferDataToSDK(JSON.stringify(data), data.id_ticket_queue);
    else
      console.log("Nothing to print")
  };

  const restartDiscovery = () => {
    setIsDiscoverEnabled(true);
    setPrinter(null);
    StartDiscovery().then(data => {
      setTimeout(() => {
        setIsDiscoverEnabled(false);
      }, 3000);
    });
  }

  //useTimer(parseInt(periodicity), fetchToFindPrintings);

  return (
    <PrintersContexProvider>
      <View style={styles.container}>
        <StatusBar barStyle={isDarkMode ? Colors.darker : Colors.lighter} />
        <ScrollView
          style={{ backgroundColor: isDarkMode ? '#000' : Colors.lighter, width: '100%' }} contentContainerStyle={styles.container}>
          <Section title="Step One">
            Edit <Text style={styles.highlight}>the Timer</Text> to change the
            requests periodicity to search for new printings
          </Section>
          <InputText value={periodicity.toString()} setPeriodicity={setPeriodicity}/>
          <Printers list={printer} />
          <View style={styles.button}>
            <Button title="Search printers" onPress={restartDiscovery} disabled={isDiscoverEnabled} />
          </View>
          <View style={styles.button}>
            <Button title="Search new printings" onPress={fetchToFindPrintings} disabled={!printer} />
          </View>
        </ScrollView>
      </View>
    </PrintersContexProvider>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignContent: 'center',
    justifyContent: 'center'
  },
  highlight: {
    fontWeight: '700',
  },
  button: {
    marginTop: 15,
    marginHorizontal: 24,
    paddingVertical: 5,
  },
});

export default () => {
  return <App />;
};
