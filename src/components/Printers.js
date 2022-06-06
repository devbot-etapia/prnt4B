// import React from 'react'
// import { Card } from 'react-native-paper';
// import {Text} from 'react-native';  

// const Printers = (props) => {
//     const printers = <Text>0 printers found</Text>;

//     if(props.list != null ){
//         if(typeof(props.list) == "object"){
//             const printer = props.list;
//             printers =  <Card.Title
//                 title={printer.DeviceName}
//                 subtitle={printer.Target}
//             />;
//         }
//         else {
//             printers = props.list.map(printer => 
//                 <Card.Title
//                     title={printer.DeviceName}
//                     subtitle={printer.Target}
//                 />
//             );
//         }
//     }

//     return printers
// }

// export default Printers