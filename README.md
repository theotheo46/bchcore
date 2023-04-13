# Installation steps

## 1. Install sdkman

```
apt-get update
apt-get -q -y install curl zip unzip git
curl -s https://get.sdkman.io | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
```
## 2. Install java 11
```
sdk install java 11.0.10.hs-adpt
```
## 3. Install sbt
```
sdk install sbt
```

## 4. Check everything works fine
```
sbt clean compile test
```

 - protocol
        - engine (model)
        - model
   
- chaincode
        - chaincode-impl (chaincode-spec, engine)
        - chaincode-spec (model)
  
- gate
        - gate-impl (gate-spec, chaincode-spec)
        - gate-spec

- wallet-lib (gate-spec)
        - wallet-lib-jvm
        - wallet-lib-js
  
- frontend (wallet-lib-js)

- tools
        - http-service-client
        - http-service-meta
        - http-service-server
        - utility
        - cryptography
  
        