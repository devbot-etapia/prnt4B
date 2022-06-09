import React, { useEffect, useState, useContext } from 'react';
import { ScrollView, StatusBar, StyleSheet, Text, useColorScheme, View, Button, DeviceEventEmitter } from 'react-native';

import { Colors } from 'react-native/Libraries/NewAppScreen';
import { SubmitMACs } from './src/components/Services';
import { TransferDataToSDK, StartDiscovery } from './src/components/EpsonNative';
import Printers from './src/components/Printers';
import { Section } from './src/components/Section';
import { TextInput } from 'react-native-paper';
import { PrintersContexProvider } from './src/context/Printer-context';
import { useTimer } from './src/hooks/Timer'
import { CONSTANTS } from './src/context/constants';

const App = () => {
  const isDarkMode = useColorScheme() === 'dark';
  const [periodicity, setPeriodicity] = useState(CONSTANTS.DEFAULT_TIMER_VALUE);
  const [isDiscoverEnabled , setIsDiscoverEnabled] = useState(false);

  const [printer , setPrinter] = useState(null);
  
  useEffect(() => {
    restartDiscovery()
    DeviceEventEmitter.addListener("printers", (printerListened) => {
      setPrinter(printerListened)
    });
  }, []);

  const fetchToFindPrintings = async () => {
    if(!printer)
      return;

    const data = await SubmitMACs(printer.Target);
    if(data.queue)
      await TransferDataToSDK(JSON.stringify(data), data.id_ticket_queue, printer.Target, printer.DeviceName);
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

  const [timer, setTimer] = useTimer(parseInt(periodicity), fetchToFindPrintings);

  const handlePeriodicity = () => {
    let newSecondsValue = parseInt(periodicity);
    if(isNaN(newSecondsValue) || newSecondsValue <= 0){
      setPeriodicity(CONSTANTS.DEFAULT_TIMER_VALUE);
      newSecondsValue = parseInt(CONSTANTS.DEFAULT_TIMER_VALUE);
    }  
    
    setTimer(newSecondsValue)
  }

  return (
    <PrintersContexProvider>
      <View style={styles.container}>
        <StatusBar barStyle={isDarkMode ? Colors.darker : Colors.lighter} />
        <ScrollView
          style={{ backgroundColor: isDarkMode ? '#000' : Colors.lighter, width: '100%' }} contentContainerStyle={styles.container}>
          <Section title="Step One" centered={false}>
            Edit <Text style={styles.highlight}>the Timer</Text> to change the
            requests periodicity to search for new printings
          </Section>
          <TextInput
            style={styles.inputContainerStyle}
            label="Timer"
            value={periodicity}
            placeholder="Set the timer value measured in seconds"
            keyboardType='numeric'
            onBlur={handlePeriodicity}
            onChangeText={text => setPeriodicity(text)}
          />
          <Section title="Printers" centered={true}>
          </Section>
          <Printers list={printer} />
          <View style={styles.button}>
            <Button title="Search printers" onPress={restartDiscovery} disabled={isDiscoverEnabled} />
          </View>
          {/* <View style={styles.button}>
            <Button title="Search new printings" onPress={fetchToFindPrintings} disabled={!printer} />
          </View> */}
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
  inputContainerStyle: {
    marginTop: 8,
    marginHorizontal: 24
}
});

export default () => {
  return <App />;
};
