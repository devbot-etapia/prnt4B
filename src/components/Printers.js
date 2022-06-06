import React from 'react'
import { Card } from 'react-native-paper';
import {Text,View} from 'react-native';  
import { Section } from './Section';

const Printers = (props) => {
    let printers = <Section title='0 printers found' />

    if(props.list != null ){
        if(typeof(props.list) == "object"){
            const printer = props.list;
            printers = <Section title={printer.DeviceName}>
                <Text>Target: {printer.Target}</Text>{'\n'}
                <Text>MAC: {printer.MACAddress}</Text>
            </Section>
        }
        else {
            printers = props.list.map(printer => 
                <Card.Title
                    title={printer.DeviceName}
                    subtitle={printer.Target}
                />
            );
        }
    }

    return <View>
        {printers}
    </View>;
}

export default Printers