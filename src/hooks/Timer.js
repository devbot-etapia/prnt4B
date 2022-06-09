import React, { useEffect, useState } from "react";

export function useTimer(seconds = 30, callback, delay = true) {
    const [count, setCount] = useState(seconds);
    const [isStopped, setIsStopped] = useState(false);

    useEffect(() => {
        if(!delay){
            callback();
        }
        const timer = setInterval(() => {
            setCount(state => {
                // if(isStopped){
                //     return state;
                // }
                const newState = (state == 0) ? seconds : state - 1
                if(newState == 0){
                    callback();
                }
                return newState;
            })
        }, 1000);
        return () => clearInterval(timer);
    }, []);

    return [count, isStopped, setIsStopped];
}