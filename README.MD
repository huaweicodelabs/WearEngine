# Wear Engine Demo

## Table of Contents

 * [Introduction](#Introduction)
 * [Getting Started](#Getting Started)
 * [Supported Environments](#Supported Environments)
 * [License](#License)


## Introduction

This project is a sample code of the Wear Engine service. The sample code calls the Android API of the Wear Engine service provided by the Huawei Health app to implement message communication and data transmission between third-party apps on your phone and watch. This sample code is for reference only or restricted use.

Include these functions of Wear Engine：  
1) 	Authorization  
Obtains user authorization for device capability exposure. 
  Code location: \app\src\main\java\com\huawei\wearengine\app\WearEngineMainActivity.java  initData()

2) 	Device Connection Management  
Allows users to obtain the list of devices bound to the Huawei Health app.  
  Code location: \app\src\main\java\com\huawei\wearengine\app\WearEngineMainActivity.java  getBoundDevices(View view)
    
3)	P2P Message Management  
  Code location: \app\src\main\java\com\huawei\wearengine\app\WearEngineMainActivity.java  pingBoundDevices(View view)
  Code location: \app\src\main\java\com\huawei\wearengine\app\WearEngineMainActivity.java  sendMessage(View view)  sendFile(String sendFilePath)
  Code location: \app\src\main\java\com\huawei\wearengine\app\WearEngineMainActivity.java  receiveMessage(View view)
   
   

## Getting Started

Before using Wear Engine demo code, check whether the IDE environment has been installed. 
1. Decompress the demo code package.    
2. Copy the package into the IDE directory and import it into the IDE Tool.
3. Press Sync Project with Gradle Files to ensure the development environment builds completed.

For more development details, please refer to the following link:
https://developer.huawei.com/consumer/cn/codelab/WearEngine/index.html (CN)
https://developer.huawei.com/consumer/en/codelab/WearEngine/index.html (EN)


## Supported Environments
* Android Studio V3.3.2 or later
* Java SDK 1.8 or later

	
## License
   Wear Engine demo is licensed under the [Apache License, version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

