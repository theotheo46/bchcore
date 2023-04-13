### Quick start guide

1. `./download-fabric-binaries.sh`
    The command above downloads and executes a bash script that will download and extract all of the platform-specific binaries you will need to set up your network. It retrieves the following platform-specific binaries:
        configtxgen,
        configtxlator,
        cryptogen,
        discover,
        idemixgen
        orderer,
        peer,
        fabric-ca-client,
        fabric-ca-server
    and places them in the hlf/bin directory. More than that this script download file with default network config:
        configtx.yaml,
        core.yaml,
        orderer.yaml
    and places them in the hlf/config directory.
    More information you can find on https://hyperledger-fabric.readthedocs.io/en/release-2.2/install.html
2. `./patch-core-yaml.sh`
    Script changes the default hlf/config/core.yaml file for external builder: cnft-external, which located in buildpack
    More information you can find on https://hyperledger-fabric.readthedocs.io/en/release-2.0/cc_launcher.html
3. `./patch-configtx-yaml.sh`
    Script changes the default hlf/config/configtx.yaml file: the parameter BatchTimeout from 2s to 1s. Decreasing this value improve latency. 
    More information you cat find on https://hyperledger-fabric.readthedocs.io/en/release-2.2/config_update.html
4. `./prepare.sh local.csv`
    This script prepare crypto-material neede to run the network. It generates:
        certificates - which are located in directory crypto-config,
        orderer genesis block - first block on a chain,
        anchor peer -  peers in different organizations to know about each other organizations
        More information you can find on https://hyperledger-fabric.readthedocs.io/en/release-2.2/glossary.html#anchor-peer
5. `./build-distr.sh`
    Copy chanicode, gate and frontend to distribution directory
6. `./bootstrap.sh`
    Start network:
        orderer service,
        peers,
        create channel,
        chaincode,
        start gate
   - use option `-p` to start postgres docker container
     `./bootstrap.sh -p`
7. `curl localhost:8981/block-number; echo`
    To make sure everything is fine. The answer should be 2.

##To stop all process
1. `./cleanup.sh`
    Stop process, orderers, peers,  and delete logs, persistence

P.S. `tarball-prepare.sh` makes possible to create tarball-distributive, which can be deployed in a Linux virtual machine.
One liner start:
`
    ./download-fabric-binaries.sh;
    ./patch-core-yaml.sh;
    ./patch-configtx-yaml.sh;
    ./prepare.sh local.csv;
    ./build-distr.sh;
    ./bootstrap.sh;
`
