---
title: "SberChain platform overview"
---

![](img/sber-chain.png)

<div style="page-break-after: always;"></div>

## SberChain node

<div style="page-break-after: always;"></div>

### 3N Control

<img style="float:left;width:45%" src="img/sber-chain.png"/>
<div style="float:left;width:50%;padding-left:5%">

Node and network control service provides you API for node and network management. The service enables you to:

  - initialize and create new platform node
  - configure and scale the node
  - create new network, or
  - join existing

</div>

<div style="page-break-after: always;"></div>

### Membership & regulation

<img style="float:left;width:45%" src="img/sber-chain.png"/>
<div style="float:left;width:50%;padding-left:5%">

Membership API:

 - register new blockchain identity
 - do regulation for existing identities

</div> 
<div style="page-break-after: always;"></div>

### NFT Engine

<img style="float:left;width:45%" src="img/sber-chain.png"/>
<div style="float:left;width:50%;padding-left:5%">

SberChain platform, internally, at blockchain level uses model similar to classic Bitcoin UTXO to represent assets. However, 
we extended the model to enable custom behavior for tokens,
which could be deployed as smart contract. The API allows you:

 - define and deploy parameterizable smart contract
 - create an instance of smart contract
 - issue tokens (by interacting with smart contracts)
 - exchange tokens with other participants
 - burn tokens to trigger execution of issuer obligations

</div> 
<div style="page-break-after: always;"></div>

### Data feeds

<img style="float:left;width:45%" src="img/sber-chain.png"/>
<div style="float:left;width:50%;padding-left:5%">

Concept of the oracles (some party that feeds external data onto the blockchain)
is supported natively on the platform. The API provided to:

 - define and register data feed
 - feed values to blockchain

</div> 
<div style="page-break-after: always;"></div>

### Private messaging

<img style="float:left;width:45%" src="img/sber-chain.png"/>
<div style="float:left;width:50%;padding-left:5%">

One of extended features of platform is private messaging. Any registered participant can send a message to some other. 
The message is stored on blockchain ledger encrypted, so only targeted participant(s) can see the content, 
however the fact is present to any party that has access to platform.

</div> 
<div style="page-break-after: always;"></div>

### Exchange service

<img style="float:left;width:45%" src="img/sber-chain.png"/>
<div style="float:left;width:50%;padding-left:5%">

We also provide an Offering board functionality where any participant can put an offer for exchange one token for another

</div> 
<div style="page-break-after: always;"></div>

### Blobs Streaming

<img style="float:left;width:45%" src="img/sber-chain.png"/>
<div style="float:left;width:50%;padding-left:5%">

Another feature of platform is blobs streaming - any participant can send blobs (binary files) to others

</div> 
<div style="page-break-after: always;"></div>

## SberChain wallet

<img style="float:left;width:45%" src="img/sber-chain.png"/>
<div style="float:left;width:50%;padding-left:5%">

Provided as:
 
 - Java Script ES6 module (suitable for use with Node.js and React native) 
 - Browser plugin (with appropriate ES6 wrapper module for embedding in web pages)
 - Java VM library (has Java, Scala and Kotlin facades)
 - Native binary (exports C ABI ?)

</div> 
<div style="page-break-after: always;"></div>

## SberChain applications

<img style="float:left;width:45%" src="img/sber-chain.png"/>
<div style="float:left;width:50%;padding-left:5%">

Some third layer applications are also provided:
 
 - Mobile wallet
 - Web portal

</div> 
<div style="page-break-after: always;"></div>
