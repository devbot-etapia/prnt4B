import React from 'react'
import {Text,View} from 'react-native';  
import { Section } from './Section';
//import PrintersContext from './src/context/Printer-context';

const Printers = (props) => {
    //const { printers } = useContext(PrintersContext);
    let printers = <Section title='0 printers found' />

    if(props.list != null ){
        if(typeof(props.list) == "object"){
            const printer = props.list;
            printers = <Section title={printer.DeviceName}>
                <Text>Target: {printer.Target}</Text>{'\n'}
                <Text>BDAddress: {printer.BDAddress}</Text>{'\n'}
                <Text>IpAddress: {printer.IpAddress}</Text>{'\n'}
                <Text>MAC: {printer.MACAddress}</Text>
            </Section>
        }
        else {
            printers = <View>
                {props.list.map(printer => 
                    <Section title={printer.DeviceName}>
                        <Text>Target: {printer.Target}</Text>{'\n'}
                        <Text>BDAddress: {printer.BDAddress}</Text>{'\n'}
                        <Text>IpAddress: {printer.IpAddress}</Text>{'\n'}
                        <Text>MAC: {printer.MACAddress}</Text>
                    </Section>
                )}
            </View>;
        }
    }

    return <View>
        {printers}
    </View>;
}

export default Printers