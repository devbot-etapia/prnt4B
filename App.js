import React, {useEffect, useState} from 'react';
import { ScrollView, StatusBar, StyleSheet, Text, useColorScheme, View, Button, DeviceEventEmitter } from 'react-native';

import { Colors } from 'react-native/Libraries/NewAppScreen';
import { SubmitMACs } from './src/components/Services';
import InputText from './src/components/Input';
import EpsonNative, { TransferDataToSDK, StartDiscovery } from './src/components/EpsonNative';
import Printers from './src/components/Printers';
import { Section } from './src/components/Section';

const App = () => {
  const isDarkMode = useColorScheme() === 'dark';
  //useTimer(30, StartDiscovery, false);
  const [printers , setPrinters] = useState(null);
  const [isDiscoverEnabled , setIsDiscoverEnabled] = useState(false);
  
  useEffect(async () => {
    await restartDiscovery()
    DeviceEventEmitter.addListener("printers", (printersListened) => {
      console.log(printersListened);
      setPrinters(printersListened)
    });
  }, []);

  const fetchTest = async () => {
    const data = await SubmitMACs();
    await TransferDataToSDK(JSON.stringify(data));
  };

  const restartDiscovery = async () => {
    setIsDiscoverEnabled(true);
    await StartDiscovery().then(data => {
      setTimeout(() => {
        setIsDiscoverEnabled(false);
      }, 3000);
    });
  }

  return (
    <View style={styles.container}>
      <StatusBar barStyle={isDarkMode ? Colors.darker : Colors.lighter} />
      <ScrollView
        style={{ backgroundColor: isDarkMode ? '#000' : Colors.lighter, width: '100%' }} contentContainerStyle={styles.container}>
        <Section title="Step One">
          Edit <Text style={styles.highlight}>the Timer</Text> to change the
          requests periodicity to search for new printings
        </Section>
        <InputText />
        <Printers list={printers}/>
        <View style={styles.button}>
          <Button title="Search printers" onPress={restartDiscovery} disabled={isDiscoverEnabled} />
        </View>
        <View style={styles.button}>
          <Button title="Fetch Microservice" onPress={fetchTest} />
        </View>
        <EpsonNative />
      </ScrollView>
    </View>
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
