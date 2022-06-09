import React, {useState} from 'react'
import { StyleSheet } from 'react-native';
import { TextInput } from 'react-native-paper';

function InputText(props) {
    const [timer, setTimer] = useState(props.value)

    const setNewTime = () => {
        props.setPeriodicity(timer);
    }

    return (
        <TextInput
            style={styles.inputContainerStyle}
            label="Timer"
            value={timer}
            placeholder="Set the timer value measured in seconds"
            keyboardType='numeric'
            onBlur={setNewTime}
            onChangeText={text => setTimer(text)}
        />
    )
}

const styles = StyleSheet.create({
    inputContainerStyle: {
        marginTop: 8,
        marginHorizontal: 24
    }
});

export default InputText