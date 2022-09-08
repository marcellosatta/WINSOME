#!/bin/bash

jar cvmf configFile/jar/MANIFEST.MF configFile/jar/configFile.jar configFile/out/ConfigFileMain.class
jar cvmf jar/server/MANIFEST.MF jar/server/server.jar out/server/*.class out/common/*.class
jar cvmf jar/client/MANIFEST.MF jar/client/client.jar out/client/*.class out/common/*.class