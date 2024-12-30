metadata {
   definition (name: "Aqara Double Rocker (WS-USC02)", namespace: "landek.com", author: "Ryan Landek", filename: "zigbee-aqara-ws-usc02", importUrl: "https://raw.githubusercontent.com/ryanlandek/Hubitat/zigbee-aqara-ws-usc02.groovy") {
       capability "Configuration"

       command "topButtonOn"
       command "topButtonOff"
       command "topButtonToggle"
       command "bottomButtonOn"
       command "bottomButtonOff"
       command "bottomButtonToggle"
              
       attribute 'Button1', 'enum', ['Unknown', 'On', 'Off']
       attribute 'Button2', 'enum', ['Unknown', 'On', 'Off']
       attribute 'InternalTemperature', 'NUMBER'

       fingerprint model:"lumi.switch.b2laus01", manufacturer:"LUMI", profileId:"0104", endpointId:"01", inClusters:"0000,0002,0003,0004,0005,0006,0009", outClusters:"000A,0019", application:"20"     
   }

   preferences {
      input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
      input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
   }
}

def logsOff(){
   log.warn "debug logging disabled..."
   device.updateSetting("logEnable", [value:"false", type:"bool"])
}

def parse(String description) {
    if (logEnable) log.debug "parse() description: ${description}"
    def descMap = zigbee.parseDescriptionAsMap(description) // Parses hex (base 16) string data to Integer -- perhaps easier to work with:

   def rawValue
    if (logEnable) 
    {
        log.debug "--- Start Message ----"
        log.debug "Device: ${device.displayName}"
        log.debug "Endpoint: ${descMap.endpoint}"
        log.debug "Cluster: ${descMap.clusterInt}"
        log.debug "Attribute: ${descMap.attrInt}"
        if (descMap.value != null) log.debug "Value: ${Integer.parseInt(descMap.value, 16)}"
        log.debug "--- End Message ----"
    }
   switch (descMap.clusterInt) {
     
       case 0x0000:
       if (logEnable) log.debug "parse() 0x0000 - Info - Not Implemented"
       break
       case 0x0002:
       if (logEnable) log.debug "parse() 0x0002 - Int. Temp"
       if (descMap.attrInt == 0) {
           rawValue = Integer.parseInt(descMap.value, 16)
           sendEvent(name: "InternalTemperature", value: rawValue, descriptionText: "${device.displayName} temp is ${rawValue}")
       }
       break
       case 0x0004:
       if (logEnable) log.debug "parse() 0x0004 - Groups - Not Implemented"
       break
       case 0x0005:
       if (logEnable) log.debug "parse() 0x0005 - Scenes - Not Implemented"
       break
       case 0x0006:
       if (logEnable) log.debug "parse() 0x0006 - On/Off"
       if (descMap.attrInt == 0) {
           rawValue = Integer.parseInt(descMap.value, 16)

           String switchValue // attribute value of 0 means off, 1 (only other valid value) means on
           switchValue = (rawValue == 0) ? "Off" : "On"

           String buttonName // value of 1 means first button, 2 (only other valid value) means the second one
           buttonName = (descMap.endpoint == "01") ? "Button1" : "Button2"

           sendEvent(name: buttonName, value: switchValue, descriptionText: "${device.displayName}, ${buttonName} is ${switchValue}")

           if (txtEnable) log.info "${device.displayName}, ${buttonName} switch is ${switchValue}"

       }
       else {
           if (logEnable) log.debug "0x0006:${descMap.attrId}"
       }
       break
       default:
           if (logEnable) log.debug "ignoring ${descMap.clusterId}:${descMap.attrId}"
           break
   }
}

def configure() {
    if (logEnable) log.debug "configure()"

    sendEvent(name: "Button1", value: "Unknown", descriptionText: "Button 1 set up")
    sendEvent(name: "Button2", value: "Unknown", descriptionText: "Button 2 set up")
    sendEvent(name: "InternalTemperature", value: 0)
    state.lastTempTime = "0"
    schedule('0 */30 * ? * *', updateTemp)
    
    return [
        "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0006 {${device.zigbeeId}} {}",
        "delay 200",
        "zdo bind 0x${device.deviceNetworkId} 0x02 0x01 0x0006 {${device.zigbeeId}} {}"
    ]
}

def topButtonOn() {
    if (logEnable) log.debug "topButtonOn()"
    return "he cmd 0x${device.deviceNetworkId} 0x01 0x0006 1 {}"
}

def topButtonOff() {
    if (logEnable) log.debug "topButtonOff()"
    return "he cmd 0x${device.deviceNetworkId} 0x01 0x0006 0 {}"
}

def topButtonToggle() {
    if (logEnable) log.debug "topButtonToggle()"
    return "he cmd 0x${device.deviceNetworkId} 0x01 0x0006 2 {}"
}

def bottomButtonOn() {
    if (logEnable) log.debug "topButtonOn()"
    return "he cmd 0x${device.deviceNetworkId} 0x02 0x0006 1 {}"
}

def bottomButtonOff() {
    if (logEnable) log.debug "topButtonOff()"
    return "he cmd 0x${device.deviceNetworkId} 0x02 0x0006 0 {}"
}

def bottomButtonToggle() {
    if (logEnable) log.debug "topButtonToggle()"
    return "he cmd 0x${device.deviceNetworkId} 0x02 0x0006 2 {}"
}

def checkPresence() {
    if (logEnable) log.debug "checkPresence() - Not Implemented"
}

def getInfo() {
    if (logEnable) log.debug "getInfo() - Not Implemented"
}

def checkEventInterval() {
    if (logEnable) log.debug "checkEventInterval() - Not Implemented"
}

def updateTemp() {
    if (logEnable) log.debug "updateTemp()"

    def dat = new Date()
    def result = null
    state.lastTempTime = dat
    return zigbee.readAttribute(0x0002,0)
}
