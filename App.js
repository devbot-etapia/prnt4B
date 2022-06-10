import React, { useEffect, useState, useContext } from 'react';
import { ScrollView, StatusBar, StyleSheet, Text, useColorScheme, View, DeviceEventEmitter } from 'react-native';
import { TextInput, ActivityIndicator, Colors, Button } from 'react-native-paper';
import BtnDialog from './src/components/Dialog';
import Printers from './src/components/Printers';

import { SubmitMACs } from './src/components/Services';
import { TransferDataToSDK, StartDiscovery } from './src/components/EpsonNative';
import { Section } from './src/components/Section';
import { PrintersContexProvider } from './src/context/Printer-context';
import { useTimer } from './src/hooks/Timer'
import { CONSTANTS, GetErrorMessage } from './src/context/constants';

const App = () => {
  const isDarkMode = useColorScheme() === 'dark';
  const [periodicity, setPeriodicity] = useState(CONSTANTS.DEFAULT_TIMER_VALUE);
  const [isDiscoverEnabled , setIsDiscoverEnabled] = useState(false);
  const [dialogMessage , setDialogMessage] = useState("");
  const [dialogTitle , setDialogTitle] = useState("");
  const [showDialog , setShowDialog] = useState(false);

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
    if(data.queue){
      const prntingResult = await TransferDataToSDK(JSON.stringify(data), data.id_ticket_queue, printer.Target, printer.DeviceName);
      handlePrinting(prntingResult);
    }
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

  const handlePrinting = (prntStatus) => {
    if(prntStatus == '1'){
      setDialogTitle("Success!");
      setDialogMessage('We found a new ticket for you! it will be ready soon.');
    }
    else{
      const errorMessage = GetErrorMessage(prntStatus);
      setDialogTitle("Alert");
      setDialogMessage(errorMessage + '\n\n' + 'We are going to mark the ticket as incomplete, and you are allowed to manually request it');
    }
    
    setShowDialog(true);
  }

  const dimissDialog = () => setShowDialog(false);

  const printersSection = printer ? 
  <Printers list={printer} /> : 
  <>
    <ActivityIndicator style={styles.indicator} size={'large'} animating={true} color={Colors.deepPurple400} />
    <Text style={{textAlign:'center', marginTop:8}}>We are looking for printers</Text>
    <Text style={{textAlign:'center'}}>Please be patient</Text>
  </>;

  return (
    <PrintersContexProvider>
      <View style={styles.container}>
        <StatusBar barStyle={isDarkMode ? Colors.darker : Colors.lighter} />
        <ScrollView
          style={{ backgroundColor: isDarkMode ? '#000' : Colors.lighter, width: '100%' }} 
          contentContainerStyle={styles.container}>

          <Section title="Welcome!" centered={false}>
            Edit the <Text style={styles.highlight}>Timer</Text> to automatically to search for new printings.
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
          
          <Button 
            style={styles.button} 
            mode="contained" 
            onPress={fetchToFindPrintings} 
            color={Colors.deepPurple500} 
            disabled={!printer}>Search now</Button>
          
          <Section title="Printers" centered={true} />

          <Button 
            style={styles.button} 
            mode="contained" 
            onPress={restartDiscovery} 
            color={Colors.deepPurple500} 
            disabled={isDiscoverEnabled}>Restart discovery</Button>

          {printersSection}

          <BtnDialog
            visible={showDialog}
            message={dialogMessage}
            title={dialogTitle}
            close={dimissDialog}
          />

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
    marginHorizontal: 24
  },
  inputContainerStyle: {
    marginTop: 8,
    marginHorizontal: 24
  },
  indicator:{
    marginTop: 25
  }
});

export default () => {
  return <App />;
};
