/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

const exec = cordova.require('cordova/exec'); // eslint-disable-line no-undef
const utils = require('cordova/utils');
const PositionError = require('./PositionError');

// Native watchPosition method is called async after permissions prompt.
// So we use additional map and own ids to return watch id synchronously.
const pluginToNativeWatchMap = {};

module.exports = {
    getCurrentPosition: function (success, error, args) {
        const permissionWin = function (deviceApiLevel) {
            // Workaround for bug specific to API 31 where requesting `enableHighAccuracy: false` results in TIMEOUT error.
            if (deviceApiLevel === 31) {
                if (typeof args === 'undefined') args = {};
                args.enableHighAccuracy = true;
            }
            exec(positionWin, positionFail, 'Geolocation', 'getCurrentPosition', [])
        };
        const permissionFail = function (e) {
            console.log(e)
            if (error) {
                error(new PositionError(PositionError.PERMISSION_DENIED, 'Illegal Access'));
            }
        };
        const enableHighAccuracy = typeof args === 'object' && !!args.enableHighAccuracy;
        exec(permissionWin, permissionFail, 'Geolocation', 'getPermission', [enableHighAccuracy]);



        const positionWin = function (position) {
            console.log(position)
            console.log(JSON.parse(position))
            success(JSON.parse(position))
        };
        const positionFail = function (e) {
            console.error(e)
            if (error) {
                error(new PositionError(PositionError.PERMISSION_DENIED, 'Illegal Access'));
            }
        }

    },

    watchPosition: function (success, error, args) {
        const pluginWatchId = utils.createUUID();

        const win = function (position) {
            console.log(position)
            if (position === 'OK') {
                return
            }
            try {
                let positionJSON = JSON.parse(position)
                console.log(JSON.parse(position))
            } catch (e) {
                console.log(e)
                return
            }
            success(JSON.parse(position))
        };

        const fail = function (e) {
            console.log(e)
            error(e)
        };
        const enableHighAccuracy = typeof args === 'object' && !!args.enableHighAccuracy;
        console.log('checking permissions')
        exec((r) => {console.log(r)}, (e)=>{console.log(e)}, 'Geolocation', 'getPermission', [enableHighAccuracy]);
        console.log('watchPosition')
        exec(win, fail, 'Geolocation', 'watchPosition', [])
        return pluginWatchId;
    },

    clearWatch: function (pluginWatchId) {
        const nativeWatchId = pluginToNativeWatchMap[pluginWatchId];
        const geo = cordova.require('cordova/modulemapper').getOriginalSymbol(window, 'navigator.geolocation'); // eslint-disable-line no-undef
        geo.clearWatch(nativeWatchId);
    }
};
