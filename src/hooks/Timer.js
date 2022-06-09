import React, { useEffect, useState } from "react";

export function useTimer(seconds = 30, callback, delay = true) {
    const [count, setCount] = useState(seconds);

    useEffect(() => {
        if(!delay){
            callback();
        }
        const timer = setInterval(() => {
            setCount(state => {
                const newState = (state == 0) ? seconds : state - 1
                if(newState == 0){
                    callback();
                }
                return newState;
            })
        }, 1000);
        return () => clearInterval(timer);
    }, [seconds, callback]);

    return [count, setCount];
}