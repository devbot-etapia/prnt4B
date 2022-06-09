import React, { useState, useEffect } from "react";

const PrintersContext = React.createContext({
    printers: {},
    onDiscovery: () => { }
});

export const PrintersContexProvider = (props) => {
    const [printers, setPrinters] = useState([]);

    const onDiscover = (printer) => {
        console.log(printer);
        setPrinters([].push(printer));
    };

    return <PrintersContext.Provider value={{
        printers: printers,
        onDiscovery: onDiscover
    }}>
        {props.children}
    </PrintersContext.Provider>
}

export default PrintersContext;