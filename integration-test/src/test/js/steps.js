//const {Given, When, Then, After} = require('@cucumber/cucumber');
//const assert = require('assert');
//const {
//    CNFT,
//    CNFTGateApi,
//    CNFTStore,
//    CNFTCrypto,
//    CryptographyConfiguration,
//    TokenTypeMeta,
//    DescriptionField,
//    FieldMeta,
//    FieldType,
//    RequestIssue,
//    TokenFieldValue,
//    TokenDescription,
//    BurnTokenRequest,
//    HDPathStore
//} = require('wallet-lib');
//
////const { Crypto } = require("@peculiar/webcrypto");
//const {webcrypto} = require('crypto');
//
//const BIP32Factory = require('bip32').default
//
//// export WebCrypto to global scope, so it to be accessible by XXXCryptoOperations
//
//global.crypto = webcrypto;
//
//const XMLHttpRequest = require("xmlhttprequest").XMLHttpRequest;
//// export XMLHttpRequest to global scope, so it to be accessible by GateApi:
//global.XMLHttpRequest = XMLHttpRequest;
//
//let dealId = "deal";
//let scenarioContext = {
//    "wallets": {},
//    "tokenTypeIdByName": {},
//    "tokenIdByName": {},
//    "offerIdByName": {}
//};
//
//log = function (msg) {
//    console.log(msg);
//}
//
//Given('there is a client {string}',
//    async function (clientName) {
//        let bip32Interface = await import('tiny-secp256k1').then(ecc => BIP32Factory(ecc)).then(bip32 => {
//            return bip32
//        })
//
//        const keyPath = new HDPathStore;
//
//        function toBuffer(ab) {
//            const buf = Buffer.alloc(ab.byteLength);
//            const view = new Uint8Array(ab);
//            for (let i = 0; i < buf.length; ++i) {
//                buf[i] = view[i];
//            }
//            return buf;
//        }
//
//        function typedArray(hex) { return new Uint8Array(hex.match(/[\da-f]{2}/gi).map(function (h) {
//            return parseInt(h, 16)
//        }))}
//
//        const seedStr = 'fffcf9f6f3f0edeae7e4e1dedbd8d5d2cfccc9c6c3c0bdbab7b4b1aeaba8a5a29f9c999693908d8a8784817e7b7875726f6c696663605d5a5754514e4b484542'
//        const bufferSeed = Buffer.from(seedStr, "hex");
//        const text = 'An obscure body in the S-K System, your majesty. The inhabitants refer to it as the planet Earth.';
//
//        async function digestMessage(message) {
//            const encoder = new TextEncoder();
//            const data = encoder.encode(message);
//            const hash = await global.crypto.subtle.digest('SHA-256', data);
//            return hash;
//        }
//
//        let cryptoConfig = CNFTCrypto.HDConfig(keyPath,  bufferSeed, bip32Interface, crypto.subtle)
//
//        log(`Creating new wallet for ${clientName}`)
//        let CNFTGateUrl = "http://localhost:8981"
//
//        let gate = CNFTGateApi.create(CNFTGateUrl);
//        let store = CNFTStore.inMemory();
//        // let cryptoConfig = new CryptographyConfiguration(
//        //     CNFTCrypto.webCryptoSign(webcrypto.subtle),        // Issuer operations
//        //     CNFTCrypto.webCryptoSign(webcrypto.subtle),        // Token operations
//        //     CNFTCrypto.webCryptoSign(webcrypto.subtle),        // Identity operations
//        //     CNFTCrypto.webCryptoEncryption(webcrypto.subtle)   // Encryption operations
//        // );
//
//        let listener = {};
//        let walletConfiguration = {
//            "gate": gate,
//            "store": store,
//            "crypto": cryptoConfig,
//            "listener": listener,
//        };
//        // Example listener call back:
//        //  "onTokenTypeRegistered": async function (wallet, types) {
//        //      log(`New types registered`);
//        //  }
//
//        return CNFT
//            .newWallet(walletConfiguration)
//            .then(wallet => {
//                scenarioContext.wallets[clientName] = wallet;
//                wallet["waiterFor"] = function (listenerMethod, predicate) {
//                    let holder = {}
//                    let result = new Promise(function (resolve, reject) {
//                        holder.resolve = resolve;
//                        holder.reject = reject;
//                    });
//                    listener[listenerMethod] = async function (...args) {
//                        if (predicate(...args)) {
//                            holder.resolve();
//                            listener[listenerMethod] = undefined;
//                        }
//                        return Promise.resolve();
//                    };
//                    return result;
//                };
//
//                log(`Created new client ${clientName}`);
//            });
//    }
//);
//
//When('{string} registered token type {string}',
//    async function (clientName, tokenTypeName) {
//        log(`Registering token type ${tokenTypeName} for ${clientName} ...`);
//
//        let wallet = scenarioContext.wallets[clientName];
//        if (typeof wallet === undefined) {
//            assert.fail(`Unknown client ${clientName}`);
//        }
//
//        let typeRegistered = wallet.waiterFor("onTokenTypeRegistered", (wallet, types) => {
//            log(`Got onTokenTypeRegistered: ${types}`)
//            return true;
//        });
//
//        let tx = await wallet.registerTokenType(
//            new TokenTypeMeta(
//                [new DescriptionField("Value", FieldType.Text, tokenTypeName)],
//                [new FieldMeta("Value", FieldType.Text)],
//                false
//            )
//        );
//
//        let typeId = tx.value;
//        scenarioContext.tokenTypeIdByName[tokenTypeName] = typeId;
//        log(`Registered token type ${tokenTypeName} for ${clientName} (${typeId}).`);
//
//        await typeRegistered
//
//        return tx;
//    }
//);
//
//When('{string} issued token {string} for {string} of type {string} with value {string}',
//    async function (issuerName, tokenName, clientName, tokenTypeName, tokenValue) {
//        log(`Issuing token of type ${tokenTypeName} for ${clientName} ...`)
//        let issuerWallet = scenarioContext.wallets[issuerName];
//        if (typeof issuerWallet === undefined) assert.fail(`Unknown client ${issuerName}`);
//
//        let clientWallet = scenarioContext.wallets[clientName];
//        if (typeof clientWallet === undefined) assert.fail(`Unknown client ${clientName}`);
//
//        let typeId = scenarioContext.tokenTypeIdByName[tokenTypeName];
//        if (typeof typeId === undefined) assert.fail(`Unknown token type ${tokenTypeName}`);
//
//        let clientBCHID = await clientWallet.getIdentity;
//        let reservedTokenIdTx = await clientWallet.reserveId;
//        let reservedTokenId = reservedTokenIdTx.value.tokenId;
//
//        let tx = await issuerWallet
//            .issueFor(clientBCHID, [new RequestIssue(reservedTokenId, typeId, [tokenValue], [])]);
//        let token = tx.value[0];
//        scenarioContext.tokenIdByName[tokenName] = token.tokenId;
//        log(`Issued token of type ${tokenTypeName} for ${clientName} (${token.tokenId}).`);
//
//        return tx;
//    }
//);
//
//When('{string} puts an offer {string}: exchange {string} for {string}',
//    async function (clientName, offerName, supplyTypeName, demandTypeName, data) {
//        let wallet = scenarioContext.wallets[clientName];
//        let supplyTypeId = scenarioContext.tokenTypeIdByName[supplyTypeName];
//        log(supplyTypeId);
//        let demandTypeId = scenarioContext.tokenTypeIdByName[demandTypeName];
//        let hashes = data.raw()
//        log(hashes);
//        let supplyContent = new TokenFieldValue(0, hashes[0][2]);
//        let demandContent = new TokenFieldValue(0, hashes[1][2]);
//        let offer = await wallet.putOffer(
//            new TokenDescription(
//                supplyTypeId,
//                Array(supplyContent)
//            ),
//            new TokenDescription(
//                demandTypeId,
//                Array(demandContent)
//            )
//        );
//        scenarioContext.offerIdByName[offerName] = offer.value.id;
//        return offer.value.id;
//    }
//);
//
//
//When('{string} sees offer {string}',
//    async function (clientName, offerName) {
//        let clientWallet = scenarioContext.wallets[clientName];
//        console.log(`${clientName} sees offer ${offerName} ...`);
//        let newOffer = scenarioContext.wallets[clientName].waiterFor("onOffersChanged", (wallet, newOffers, closedOffers) => {
//            log(`Got onOffersChanged: ${newOffers}`)
//            return true;
//        });
//        await newOffer;
//        let offerId = scenarioContext.offerIdByName[offerName];
//        let walletOffer = await clientWallet.getOffer(offerId);
//        assert(walletOffer !== undefined);
//    });
//
//
//When('{string} has {string} as candidate for offer {string}',
//    async function (clientName, tokenName, offerName) {
//        console.log(`${clientName} has ${tokenName} as candidate for offer ${offerName} ...`);
//        let clientWallet = scenarioContext.wallets[clientName];
//        let offerId = scenarioContext.offerIdByName[offerName];
//        let tokenSupplyCandidates = await clientWallet.listTokenSupplyCandidates(offerId);
//        assert(tokenSupplyCandidates.length === 1);
//    });
//
//
//When('{string} apply with {string} for offer {string}',
//    async function (clientName, tokenName, offerName) {
//        console.log(`${clientName} apply with ${tokenName} for offer ${offerName}...`);
//        let clientWallet = scenarioContext.wallets[clientName];
//        let offerId = scenarioContext.offerIdByName[offerName];
//        let tokenSupplyCandidates = await clientWallet.listTokenSupplyCandidates(offerId);
//        let walletToken = tokenSupplyCandidates[0];
//        let result = await clientWallet.applyForOffer(offerId, dealId, walletToken.signedToken);
//        console.log(`${clientName} successfully applied for offer ${offerName}`);
//        return result;
//    });
//
//
//When('{string} approves offer {string}',
//    async function (clientName, offerName) {
//        console.log(`${clientName} approves offer ${offerName}...`);
//        let clientWallet = scenarioContext.wallets[clientName];
//        let offerId = scenarioContext.offerIdByName[offerName];
//        let applyForOffer = clientWallet.waiterFor("onApplyForOffer", (wallet, applyForOfferRequests) => {
//            log(`Got onApplyForOffer: ${applyForOfferRequests}`)
//            return true;
//        });
//        await applyForOffer;
//        let tokenDemandCandidates = await clientWallet.listTokenDemandCandidates(offerId);
//        assert(tokenDemandCandidates.length === 1);
//        let walletToken = tokenDemandCandidates[0];
//        let result = await clientWallet.approveOffer(offerId, dealId, walletToken.signedToken);
//        return result;
//    });
//
//
//When('{string} finalizes offer {string}',
//    async function (clientName, offerName) {
//        console.log(`${clientName} approves offer ${offerName}...`);
//        let clientWallet = scenarioContext.wallets[clientName];
//        let approveForOffer = clientWallet.waiterFor("onApproveOffer", (wallet, approveOfferRequests) => {
//            log(`Got onApplyForOffer: ${approveOfferRequests}`)
//            return true;
//        });
//        await approveForOffer;
//        let offerId = scenarioContext.offerIdByName[offerName];
//        let result = await clientWallet.finalizeOffer(offerId, dealId);
//        return result;
//    });
//
//
//When('{string} closes offer {string}',
//    async function (clientName, offerName) {
//        console.log(`${clientName} closes offer ${offerName}...`);
//        let clientWallet = scenarioContext.wallets[clientName];
//        let offerId = scenarioContext.offerIdByName[offerName];
//        let closedOffer = clientWallet.waiterFor("onOffersChanged", (wallet, newOffers, closedOffers) => {
//            log(`Got onOffersChanged: ${closedOffers}`)
//            return true;
//        });
//        let result = await clientWallet.closeOffer(offerId, dealId);
//        await closedOffer;
//        return result;
//    });
//
//
//When('{string} sees token {string} in his wallet',
//    async function (clientName, tokenName) {
//        console.log(`${clientName} see token ${tokenName} in his wallet...`);
//        let clientWallet = scenarioContext.wallets[clientName];
//        let tokenId = scenarioContext.tokenIdByName[tokenName];
//        let tokens = await clientWallet.listTokens;
//        assert(tokens[0].signedToken.tokenBody.tokenId === tokenId);
//    });
//
//
//When('{string} burns token {string}',
//    async function (clientName, tokenName) {
//        let clientWallet = scenarioContext.wallets[clientName];
//        let tokenId = scenarioContext.tokenIdByName[tokenName];
//        let burntToken = clientWallet.waiterFor("onTokenListChanged", (wallet,tokensAdded , tokenIdsRemoved,tokensFrozen) => {
//            log(`Got onTokenListChanged: ${tokenIdsRemoved}`)
//            return true;
//        });
//        let result = await clientWallet.burnToken([new BurnTokenRequest(tokenId, [], [])])
//        await burntToken;
//        return result;
//    });
//
//
//When('{string} sees his token list is empty',
//    async function (clientName) {
//        console.log(`${clientName} sees his token list is empty in his wallet...`);
//        let clientWallet = scenarioContext.wallets[clientName];
//        let tokens = await clientWallet.listTokens;
//        assert(tokens.length === 0);
//    });
//
//
//After(
//    function () {
//        return new Promise((resolve, reject) => {
//            log(`Stopping wallets ...`);
//            for (const index in scenarioContext.wallets) {
//                log(`Stopping wallet ${index} ...`);
//                scenarioContext.wallets[index].stopListenBlocks();
//            }
//            log(`Wallets stopped.`);
//            return resolve();
//        });
//    }
//);
//
