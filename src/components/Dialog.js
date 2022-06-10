import * as React from 'react';
import { Paragraph, Button, Portal, Dialog, Colors } from 'react-native-paper';

const BtnDialog = ({visible,close,message,title,dismissable = true}) => (
//   <Portal>
    <Dialog onDismiss={close} visible={visible} dismissable={dismissable}>
      <Dialog.Title>{title}</Dialog.Title>
      <Dialog.Content>
        <Paragraph>{message}</Paragraph>
      </Dialog.Content>
      <Dialog.Actions>
        {/* <Button color={Colors.teal500} disabled>
          Disagree
        </Button> */}
        <Button onPress={close}>Ok</Button>
      </Dialog.Actions>
    </Dialog>
//   </Portal>
);

export default BtnDialog;
