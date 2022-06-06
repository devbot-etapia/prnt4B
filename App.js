import React, {useEffect, useState} from 'react';
import { ScrollView, StatusBar, StyleSheet, Text, useColorScheme, View, Button, DeviceEventEmitter } from 'react-native';

import { Colors } from 'react-native/Libraries/NewAppScreen';
import { SubmitMACs } from './src/components/Services';
import InputText from './src/components/Input';
import EpsonNative, { TransferDataToSDK, StartDiscovery } from './src/components/EpsonNative';
import Printers from './src/components/Printers';

const Section = ({ children, title }) => {
  const isDarkMode = useColorScheme() === 'dark';
  return (
    <View style={styles.sectionContainer}>
      <Text
        style={[
          styles.sectionTitle,
          {
            color: isDarkMode ? Colors.white : Colors.black,
          },
        ]}>
        {title}
      </Text>
      <Text
        style={[
          styles.sectionDescription,
          {
            color: isDarkMode ? Colors.light : Colors.dark,
          },
        ]}>
        {children}
      </Text>
    </View>
  );
};

const App = () => {
  const isDarkMode = useColorScheme() === 'dark';
  //useTimer(30, StartDiscovery, false);
  //const [printers , setPrinters] = useState(null);
  
  useEffect(() => {
    StartDiscovery()
    DeviceEventEmitter.addListener("printers", (printersListened) => {
      console.log(printersListened);
      //setPrinters(printersListened)
    });
  }, []);

  const fetchTest = async () => {
    const data = await SubmitMACs();
    await TransferDataToSDK(JSON.stringify(data));
  };

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
        <View style={styles.button}>
          <Button title="Fetch Microservice" onPress={fetchTest} />
        </View>
        <EpsonNative />
        {/* <Printers list={printers}/> */}
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
  sectionContainer: {
    marginTop: 32,
    paddingHorizontal: 24,
  },
  sectionTitle: {
    fontSize: 24,
    fontWeight: '600',
  },
  sectionDescription: {
    marginTop: 8,
    fontSize: 18,
    fontWeight: '400',
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
