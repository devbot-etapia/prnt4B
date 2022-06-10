export const ERROR_MESSAGES = {
    "1": "No error",
    "2": "Unable to find the specifyed printer by the printer name",
    "3": "Unable to connect to the specifyed printer by the target",
    "4": "Invalid json payload"
}

export const CONSTANTS = {
    DEFAULT_TIMER_VALUE: '3600',
    URL: 'http://192.168.1.73:8000/api/cloudprnt'
}

export const GetErrorMessage = (value) => ERROR_MESSAGES[value];
