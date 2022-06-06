/**
 * @format
 */

 import {AppRegistry} from 'react-native';
 import App from './App';
 import {name as appName} from './app.json';
 import { AppPermissions } from './src/components/Services';
 
 AppRegistry.registerComponent(appName, () => {
     AppPermissions();
     return App
 });
 