import React, { useState } from 'react'
import { StyleSheet } from 'react-native';
import { TextInput, HelperText, useTheme } from 'react-native-paper';

function InputText() {
    const [Timer, setTimer] = useState("")
    return (
        <TextInput
            style={styles.inputContainerStyle}
            label="Timer"
            value={Timer}
            placeholder="Set the timer value measured in seconds"
            onChangeText={time => setTimer(time)}
            keyboardType='numeric'
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