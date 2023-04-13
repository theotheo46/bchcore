const { createPublicApi } = require('./cnft-util');

const {Given, When, Then, After} = require('@cucumber/cucumber');
var {setDefaultTimeout} = require('@cucumber/cucumber');
setDefaultTimeout(10 * 1000);
var uuid = require("uuid");
const assert = require('assert');
const {
    BurnRequest,
    CNFT,
    CNFTCrypto,
    CNFTGateApi,
    CNFTStore,
    CryptographyConfiguration,
    DNA,
    DescriptionField,
    FeedType,
    FieldMeta,
    FieldType,
    Gene,
    GeneID,
    HDPathStore,
    HDSignatureOperations,
    HttpExecutorFactory,
    HttpService,
    InMemoryHDPathStore,
    InMemoryKeysStore,
    MemberEvents,
    OwnerEvents,
    RegulatorEvents,
    RegulatorCapabilities,
    SmartContract,
    SmartContractEvents,
    TokenChangeRequest,
    TokenContent,
    TokenId,
    TokenTypeMeta,
    WalletEvents,
    WalletIssueTokenRequest,
    WebCryptoEncryptionOperations,
} = require('wallet-lib');

const elliptic = require('elliptic');
const EC = elliptic.ec
const jsec = new EC('secp256k1')

const crypto = require('crypto').webcrypto;
// export WebCrypto to global scope, so it to be accessible by XXXCryptoOperations
global.crypto = crypto;


global.Buffer = global.Buffer || require('buffer').Buffer;

if (typeof btoa === 'undefined') {
    global.btoa = function (str) {
        return new Buffer.from(str, 'binary').toString('base64');
    };
}

if (typeof atob === 'undefined') {
    global.atob = function (b64Encoded) {
        return new Buffer.from(b64Encoded, 'base64').toString('binary');
    };
}

const XMLHttpRequest = require("xmlhttprequest").XMLHttpRequest;
// export XMLHttpRequest to global scope, so it to be accessible by GateApi:
global.XMLHttpRequest = XMLHttpRequest;

let scenarioContext = {
    "wallets": {},
    "walletIds": {},
    "dataFeeds": {},
    "walletSeeds": {},
    //
    "smartContractTemplateAttributes": {},
    "smartContractTemplateFeeds": {},
    "smartContractTemplateStateModel": {},
    "smartContractTemplates": {},
    //
    "smartContractAddress": {},
    "smartContract": {},
    "smartContractFeeds": {},
    "smartContractRegulators": {},
    "smartContractExtraData": {},
    //
    "tokenAddress": {},
    "tokenIdAddress": {},
    "tokenIssueRequest": {},
    "tokenTypeIdByName": {}
};

log = function (msg) {
    console.log(msg);
}


async function getOperations(wallet) {
    let id = await wallet.getIdentity;
    let operations = await wallet.listOperations;
    let ops = await Promise.all(operations.map(async (operation) => {
        let history = await Promise.all(operation.history.map(async (state) => {
            let details = await wallet.getOperationDetails(state);
            return {state, details};
        }));
        return {operation, history};
    }));

    log('')
    log(`Operations for ${id}`)
//    log(ops);
    ops.forEach(function (o) {
        log(`\t OpID: ${o.operation.operationId}`);
        o.history.forEach(function (history) {
            log(`\t\t ${history.state}`);
            //log(history.details);
            log(`\t\t\t -----------------------------`);
        });
    });

    log(`=============================================================================================================`)
}

Given('there is embeded postgres',
    async function () {
        assert.fail(`Non applicable for JS test`);
    }
)


Given('[ICO] there is a client {string}',
    async function (clientName) {
        log(`Creating new wallet for ${clientName}`)
        let CNFTGateUrl = "http://localhost:8981"

        let context =
            CNFTCrypto.newContext(
                    CNFTCrypto.hdSignatureOperations(crypto, jsec, HDPathStore.inMemory()),
                    CNFTCrypto.webCryptoEncryption(crypto),
                    CNFTCrypto.webCryptoAccessOperations(crypto, jsec),
                    CNFTCrypto.hdSignatureOperations(crypto, jsec, HDPathStore.inMemory()),
                    CNFTCrypto.hash(crypto)
                );
        let walletCrypto = await context.create();

        let adminListener = {}
        let adminStore = CNFTStore.inMemory();

        //
        log("Connecting to Admin wallet ...")
        let adminWallet = await CNFT.createWalletRemote(
            "http://localhost:8983",
            adminListener,
            adminStore,
            1000,
            false
        );
        let member = await walletCrypto.memberInformation();
        log("Registering wallet ...")
        let tx = await adminWallet.registerMember(member);
        log(`Registered wallet [${member.id}] in TX: [${tx.blockNumber} : ${tx.txId}]`)
        //

        let gate = CNFTGateApi.createAuthenticated(walletCrypto, CNFTGateUrl)
        let store = CNFTStore.inMemory();

        let listener = {};
        let walletConfiguration = {
            "gate": gate,
            "store": store,
            "crypto": walletCrypto,
            "listener": listener
        };

        // Example listener call back:
        //  "onTokenTypeRegistered": async function (wallet, types) {
        //      log(`New types registered`);
        //  }

        return await CNFT
            .newWallet(walletConfiguration)
            .then(wallet => {
                wallet.getIdentity.then(walletIdentity => {
                        scenarioContext.wallets[clientName] = wallet;
                        scenarioContext.walletIds[clientName] = walletIdentity;
                        scenarioContext.walletSeeds[clientName] = generatedBufferSeed;
                        wallet["waiterFor"] = function (listenerMethod, predicate) {
                            let holder = {}
                            let result = new Promise(function (resolve, reject) {
                                holder.resolve = resolve;
                                holder.reject = reject;
                            });
                            listener[listenerMethod] = async function (...args) {
                                if (predicate(...args)) {
                                    holder.resolve();
                                    listener[listenerMethod] = undefined;
                                }
                                return Promise.resolve();
                            };
                            return result;
                        };

                        log(`Created new client ${clientName}`)
                    }
                )
            });
    }
);

Given('[ICO] there is a client remote {string}',
    async function (clientName) {
        log(`Creating new wallet for ${clientName}`)

        let listener = {}
        let store = CNFTStore.inMemory();

        let wallet = await CNFT.createWalletRemotelocalhost(
            "http://localhost:8983",
            listener,
            store,
            1000,
            false
        );

        wallet.getIdentity.then(walletIdentity => {
                scenarioContext.wallets[clientName] = wallet;
                scenarioContext.walletIds[clientName] = walletIdentity;
                scenarioContext.walletSeeds[clientName] = generatedBufferSeed;
                wallet["waiterFor"] = function (listenerMethod, predicate) {
                    let holder = {}
                    let result = new Promise(function (resolve, reject) {
                        holder.resolve = resolve;
                        holder.reject = reject;
                    });
                    listener[listenerMethod] = async function (...args) {
                        if (predicate(...args)) {
                            holder.resolve();
                            listener[listenerMethod] = undefined;
                        }
                        return Promise.resolve();
                    };
                    return result;
                };
                log(`Created new client ${clientName}`)
            }
        );
    });

When('{string} registered data feed for {string} with fields:',
    async function (clientName, dataFeedType, data) {
        log(`Registering data feed ${dataFeedType} for ${clientName} ...`)
        if (typeof clientName === undefined) assert.fail(`Unknown client ${clientName}`);
        let wallet = scenarioContext.wallets[clientName];
        if (typeof dataFeedType === undefined) assert.fail(`Unknown data feed ${dataFeedType}`);
        let hashes = data.hashes()
        let fields = hashes.map(hash =>
            new FieldMeta(hash["id"], hash["typeId"], hash["description"])
        )
        let tx = await wallet.registerDataFeed(
            [
                description = new DescriptionField(
                    "Description of the data feed",
                    FieldType.Text,
                    dataFeedType,
                )
            ],
            fields = fields
        );
        scenarioContext.dataFeeds[dataFeedType] = tx.value;
        log(`Registered data feed ${dataFeedType} for ${clientName}.`);
        return tx;
    }
);


When('smart contract template for {string} attributes are:',
    async function (smartContractTemplateName, data) {
        log(`Storing attributes for smartContractTemplate ${smartContractTemplateName} ...`)
        if (typeof smartContractTemplateName === undefined) assert.fail(`Unknown smart contract template ${clientName}`);
        let hashes = data.hashes()
        let fields = hashes.map(hash =>
            new FieldMeta(hash["id"], hash["typeId"], hash["description"])
        )
        scenarioContext.smartContractTemplateAttributes[smartContractTemplateName] = fields;
        log(`Stored smart contract template ${smartContractTemplateName} attributes`);
        return "";
    }
);

When('smart contract template for {string} data feeds are:',
    async function (smartContractTemplateName, data) {
        log(`Storing data feed ids for smartContractTemplate ${smartContractTemplateName}...`)
        if (typeof smartContractTemplateName === undefined) assert.fail(`Unknown smart contract template ${clientName}`);
        let hashes = data.hashes()
        let feeds = hashes.map(hash =>
            new FeedType(scenarioContext.dataFeeds[hash["id"]].fields)
        )
        if (feeds.length !== 0) {
            scenarioContext.smartContractTemplateFeeds[smartContractTemplateName] = feeds;
            log(`Stored smart contract template ${smartContractTemplateName} data feeds`);
        }
        return feeds
    }
);


When('smart contract template for {string} state model is:',
    async function (smartContractTemplateName, data) {
        log(`Storing state model for smartContractTemplate ${smartContractTemplateName}...`)
        if (typeof smartContractTemplateName === undefined) assert.fail(`Unknown smart contract template ${clientName}`);
        let hashes = data.hashes()
        let stateModel = hashes.map(hash =>
            new FieldMeta(hash["id"], hash["typeId"], hash["description"])
        )
        if (stateModel.length !== 0) {
            scenarioContext.smartContractTemplateStateModel[smartContractTemplateName] = stateModel;
            log(`Stored smart contract template ${smartContractTemplateName} state model`);
        }
        return stateModel;
    }
);

When('{string} registered smart contract template for {string}',
    async function (clientName, smartContractTemplateName) {
        log(`Registering smart contract template ${smartContractTemplateName} for ${clientName}...`)
        if (typeof clientName === undefined) assert.fail(`Unknown client ${clientName}`);
        let wallet = scenarioContext.wallets[clientName];
        if (typeof smartContractTemplateName === undefined) assert.fail(`Unknown smart contract template ${clientName}`);
        let feeds = scenarioContext.smartContractTemplateFeeds[smartContractTemplateName] || [];
        let attributes = scenarioContext.smartContractTemplateAttributes[smartContractTemplateName] || [];
        let stateModel = scenarioContext.smartContractTemplateStateModel[smartContractTemplateName] || [];
        let tx = await wallet.registerSmartContractTemplate(
            feeds = feeds,
            description = [
                new DescriptionField(
                    name = "Smart contract template name",
                    typeId = FieldType.Text,
                    value = smartContractTemplateName,
                )
            ],
            attributes = attributes,
            stateModel = stateModel,
            classImplementation = smartContractTemplateName
        );
        scenarioContext.smartContractTemplates[smartContractTemplateName] = tx.value;
        log(`Registered smart contract template ${smartContractTemplateName} for ${clientName} (${tx.value}).`)
        return tx;
    }
);

When('[ICO] {string} registered token type {string}', async function (clientName, tokenTypeName) {
    log(`Registering token type ${tokenTypeName} for ${clientName} ...`);
    if (typeof clientName === undefined) assert.fail(`Unknown client ${clientName}`);
    let wallet = scenarioContext.wallets[clientName];
    log(`wallet ${wallet}`)
    let typeId = await wallet.createAddress;

//    registerTokenType(address: Bytes, meta: TokenTypeMeta, dna: DNA, regulation: Collection[RegulatorCapabilities], burnExtraData: Collection[FieldMeta])

    let result = await wallet.registerTokenType(
        typeId,
        new TokenTypeMeta(
            [new DescriptionField("Value", FieldType.Text, tokenTypeName)],
            [new FieldMeta("amount", FieldType.Text)]
        ),
        DNA(
            [
                Gene(GeneID.EmissionControlledByIssuer, [])
            ],
            [
                Gene(GeneID.RequireOwnerSignatureForTransfer, [])
            ],
            [
                Gene(GeneID.RequireOwnerSignatureForBurn, [])
            ],
            [
                Gene(GeneID.Fungible, [])
            ]
        ),
        [], []
    )
    log(result);

    scenarioContext.tokenTypeIdByName[clientName] = typeId;
    log(`Registered token type ${tokenTypeName} for ${clientName} (${typeId.toB64}).`)
    return result;
});

When('[ICO] {string} registered token type {string} with recipient signature gene', async function (clientName, tokenTypeName) {
    log(`Registering token type ${tokenTypeName} for ${clientName} with recipient signature gene`);
    if (typeof clientName === undefined) assert.fail(`Unknown client ${clientName}`);
    let wallet = scenarioContext.wallets[clientName];
    log(`wallet ${wallet}`)
    let typeId = await wallet.createAddress;
    let result = await wallet.registerTokenType(
        typeId,
        new TokenTypeMeta(
            [new DescriptionField("Value", FieldType.Text, tokenTypeName)],
            [new FieldMeta("amount", FieldType.Text, "")]
        ),
        DNA(
            [
                Gene(GeneID.EmissionControlledByIssuer, [])
            ],
            [
                Gene(GeneID.RequireOwnerSignatureForTransfer, []),
                Gene(GeneID.RequireRecipientSignatureForTransfer, [])
            ],
            [
                Gene(GeneID.RequireOwnerSignatureForBurn, [])
            ],
            [
                Gene(GeneID.Fungible, [])
            ]
        ),
        [], []
    )
    scenarioContext.tokenTypeIdByName[clientName] = typeId;
    log(`Registered token type ${tokenTypeName} for ${clientName} (${typeId.toB64}) with recipient signature gene.`)
    return result;
});

When('{string} registered address for smart contract {string}',
    async function (clientName, smartContractName) {
        log(`Registering smart contract  ${smartContractName} for ${clientName}...`)
        if (typeof clientName === undefined) assert.fail(`Unknown client ${clientName}`);
        let wallet = scenarioContext.wallets[clientName];
        if (typeof smartContractName === undefined) assert.fail(`Unknown smart contract  ${clientName}`);
        let address = await wallet.createSmartContractAddress;
        scenarioContext.smartContractAddress[smartContractName] = address;
        log(`Registered smart contract  ${smartContractName} address for ${clientName}`)
        return address;
    }
);

When('{string} registered address',
    async function (clientName) {
        log(`Registering address for ${clientName}...`)
        if (typeof clientName === undefined) assert.fail(`Unknown client ${clientName}`);
        let wallet = scenarioContext.wallets[clientName];
        let address = await wallet.createSingleOwnerAddress;
        scenarioContext.tokenAddress[clientName] = address;
        log(`Registered address for ${clientName}`)
        return address;
    }
);

When('smart contract {string} data feeds are:',
    async function (smartContractName, data) {
        log(`Storing data feed ids for smartContract ${smartContractName}...`)
        if (typeof smartContractName === undefined) assert.fail(`Unknown smart contract  ${clientName}`);
        let hashes = data.hashes()
        let feeds = hashes.map(hash =>
            scenarioContext.dataFeeds[hash["id"]].address
        )
        if (feeds.length !== 0) {
            scenarioContext.smartContractFeeds[smartContractName] = feeds;
            log(`Stored smart contract template ${smartContractName} data feeds`);
        }
        return feeds
    }
);

When('smart contract {string} regulators are:',
    async function (smartContractName, data) {
        log(`Storing regulators for smartContract ${smartContractName}...`)
        if (typeof smartContractName === undefined) assert.fail(`Unknown smart contract  ${clientName}`);
        let hashes = data.hashes()
        let regulatorCapabilities = hashes.map(hash =>
            new RegulatorCapabilities(
                scenarioContext.walletIds[hash["name"]],
                hash["capabilities"].split(" ")
            )
        )
        if (regulatorCapabilities.length !== 0) {
            scenarioContext.smartContractRegulators[smartContractName] = regulatorCapabilities;
            log(`Stored smart contract  ${smartContractName} regulations`);
        }
        return regulatorCapabilities
    }
);

When('{string} registered address for token {string} with issuer {string}',
    async function (clientName, tokenName, issuerName) {
        log(`Registering address for token ${tokenName} for ${clientName} with issuer ${issuerName}...`)
        if (typeof clientName === undefined) assert.fail(`Unknown client ${clientName}`);
        let wallet = scenarioContext.wallets[clientName];
        let tokenType = scenarioContext.tokenTypeIdByName[issuerName];
        let address = await wallet.createAddress;
        let tokenId = new TokenId(
            tokenType,
            uuid.v4()
        )
        scenarioContext.tokenAddress[clientName] = address;
        scenarioContext.tokenIdAddress[clientName] = tokenId;
        log(`Created address for token ${tokenName} for ${clientName}.`)
        return address;
    }
);


When('smart contract {string} burn extra data:',
    async function (smartContractName, data) {
        log(`Smart contract ${smartContractName} burn extra data...`)
        let hashes = data.hashes()
        let burnExtraData = hashes.map(hash =>
            new FieldMeta(hash["id"], hash["typeId"], hash["description"])
        );
        scenarioContext.smartContractExtraData[smartContractName] = burnExtraData;
        return burnExtraData;

    });


When('{string} registered {string} smart contract with smart contract template {string} with {string} minutes subscriptionEndDate and attributes:',
    async function (clientName, smartContractName, smartContractTemplateName, minutes, data) {
        log(`Registering smart contract template ${smartContractTemplateName} for ${clientName}...`)
        if (typeof clientName === undefined) assert.fail(`Unknown client ${clientName}`);
        let wallet = scenarioContext.wallets[clientName];
        if (typeof smartContractName === undefined) assert.fail(`Unknown smart contract  ${clientName}`);
        if (typeof smartContractTemplateName === undefined) assert.fail(`Unknown smart contract  ${clientName}`);
        let feeds = scenarioContext.smartContractFeeds[smartContractTemplateName] || [];
        let smartContractAddress = scenarioContext.smartContractAddress[smartContractName];
        let smartContractTemplate = scenarioContext.smartContractTemplates[smartContractTemplateName];
        let regulators = scenarioContext.smartContractRegulators[smartContractName] || [];
        let hashes = data.hashes()
        let attr = hashes.map(hash =>
            hash["value"]
        )
        let func = async () => {
            if (smartContractTemplateName === "ICO") {
                let issuerAddress = scenarioContext.tokenAddress[clientName];
                let issuerAddressB64 = btoa(String.fromCharCode.apply(null, new Uint8Array(issuerAddress)));
                let attr_ = attr.concat([issuerAddressB64]);
                let now = new Date();
                let isoNow = now.toISOString();
                let isoSubscriptionEndDate = new Date(now.getTime() + minutes * 60000).toISOString();
                attr_[4] = isoNow;
                attr_[13] = isoNow;
                attr_[14] = isoSubscriptionEndDate;
                attr_[15] = isoNow;
                log(attr_);
                return attr_
            } else {
                let identity = await wallet.getIdentity;
                let identityB64 = btoa(String.fromCharCode.apply(null, new Uint8Array(identity)));
                return attr.concat([identityB64])
            }
        }
        let attributes = await func()
        let smartContract = new SmartContract(
            address = smartContractAddress,
            templateAddress = smartContractTemplate.address,
            dataFeeds = feeds,
            regulators = regulators,
            attributes = attributes
        );
        let tx = await wallet.createSmartContract(
            smartContractAddress, smartContractTemplate.address, feeds, regulators, attributes, [],
            DNA(
                [
                    Gene(GeneID.EmissionControlledByIssuer, [])
                ],
                [
                    Gene(GeneID.RequireOwnerSignatureForTransfer, [])
                ],
                [
                    Gene(GeneID.RequireOwnerSignatureForBurn, [])
                ],
                [
                    Gene(GeneID.Fungible, [])
                ]
            )
        ); //TODO add proper burnExtraData
        scenarioContext.smartContract[smartContractName] = smartContract;
        log(`Registered smart contract ${smartContractName} for ${clientName}.`);
        return tx;
    }
);

When('{string} registered {string} smart contract with smart contract template {string} and attributes:',
    async function (clientName, smartContractName, smartContractTemplateName, data) {
        log(`Registering smart contract template ${smartContractTemplateName} for ${clientName}...`)
        if (typeof clientName === undefined) assert.fail(`Unknown client ${clientName}`);
        let wallet = scenarioContext.wallets[clientName];
        if (typeof smartContractName === undefined) assert.fail(`Unknown smart contract  ${clientName}`);
        if (typeof smartContractTemplateName === undefined) assert.fail(`Unknown smart contract  ${clientName}`);
        let feeds = scenarioContext.smartContractTemplateFeeds[smartContractTemplateName] || [];
        let smartContractAddress = scenarioContext.smartContractAddress[smartContractName];
        let smartContractTemplate = scenarioContext.smartContractTemplates[smartContractTemplateName];
        let regulators = scenarioContext.smartContractRegulators[smartContractName] || [];
        let hashes = data.hashes()
        let attr = hashes.map(hash =>
            hash["value"]
        )
        let func = async () => {
            if (smartContractTemplateName === "ICO") {
                let issuerAddress = scenarioContext.tokenAddress[clientName];
                let issuerAddressB64 = btoa(String.fromCharCode.apply(null, new Uint8Array(issuerAddress)));
                return attr.concat([issuerAddressB64])
            } else {
                let identity = await wallet.getIdentity;
                let identityB64 = btoa(String.fromCharCode.apply(null, new Uint8Array(identity)));
                return attr.concat([identityB64])
            }
        }
        let attributes = await func()
        let smartContract = new SmartContract(
            address = smartContractAddress,
            templateAddress = smartContractTemplate.address,
            dataFeeds = feeds,
            regulators = regulators,
            attributes = attributes
        );
        let dna = new DNA(
            emission = [],
            transfer =
                [
                    new Gene(
                        id = GeneID.RequireOwnerSignatureForTransfer,
                        parameters = []
                    ),
                    new Gene(
                        id = GeneID.RequireRecipientSignatureForTransfer,
                        parameters = []
                    )
                ],
            burn =
                [
                    new Gene(
                        id = GeneID.RequireOwnerSignatureForBurn,
                        parameters = []
                    )
                ],
            change =
                [
                    new Gene(
                        id = GeneID.Fungible,
                        parameters = []
                    )
                ]
        )
        let tx = await wallet.createSmartContract(
            smartContractAddress,
            smartContractTemplate.address,
            feeds,
            regulators,
            attributes,
            [],
            dna); //TODO add proper burnExtraData
        scenarioContext.smartContract[smartContractName] = smartContract;
        console.log(`Registered smart contract ${smartContractName} for ${clientName}.`);
        return tx;
    }
);

When('[ICO] {string} issued token {string} for {string} of type {string} with value {string}',
    async function (issuerName, tokenName, clientName, tokenTypeName, tokenValue) {
        log(`Issuing token for ${clientName}...`)
        let issuerWallet = scenarioContext.wallets[issuerName];
        if (typeof issuerWallet === undefined) assert.fail(`Unknown issuerName  ${issuerName}`);
        let clientWallet = scenarioContext.wallets[clientName];
        if (typeof clientWallet === undefined) assert.fail(`Unknown clientName  ${clientName}`);
        let tokenId = scenarioContext.tokenIdAddress[clientName];
        let owner = await clientWallet.createSingleOwnerAddress;
        let tokenIssued = clientWallet.waiterFor("onWalletEvents", (wallet, events) => {
            log(`Got on tokensReceived: ${events.owner.tokensReceived}`)
            return true;
        });
        if (tokenId !== undefined) {
            issuerWallet
                .issue(
                    new WalletIssueTokenRequest(
                        tokenId,
                        owner,
                        new TokenContent(
                            [tokenValue]
                        ),
                        ""
                    )
                )
            scenarioContext.tokenTypeIdByName[tokenName] = tokenId;
            log(`Issued token of type ${tokenTypeName} for ${clientName}`)
            await tokenIssued;
            await getOperations(issuerWallet)
        }
    });


When('{string} checks his tokens',
    async function (clientName) {
        log(`Checks tokens for ${clientName}...`)
        if (typeof clientName === undefined) assert.fail(`Unknown client ${clientName}`);
        let wallet = scenarioContext.wallets[clientName];
        let tokens = await wallet.listTokens;
        log(`Tokens are: ${tokens}`)
        return tokens;
    }
);


When('{string} sends his {string} token to {string} smart contract',
    async function (clientName, tokenName, smartContractName) {
        log(`${clientName} sends his ${tokenName} token to ${smartContractName}...`)
        let wallet = scenarioContext.wallets[clientName];
        let tokenId = scenarioContext.tokenIdAddress[clientName];
        let tokensDeleted = wallet.waiterFor("onWalletEvents", (wallet, events) => {
            log(`Got onWalletEvents: Tokens added: ${events.owner.tokensReceived}, tokens deleted: ${events.owner.tokensBurn}`)
            return true;
        });
        let destinationAddress = scenarioContext.smartContractAddress[smartContractName];
        await wallet.sendTokenToSmartContract(destinationAddress, uuid.v4(), [tokenId], new ArrayBuffer(0));
        await tokensDeleted;
    }
);


When('[Transfer with accept] {string} sends his {string} token to {string}',
    async function (clientName, tokenName, clientTo) {
        log(`${clientName} sends token to ${clientTo}`)
        let wallet = scenarioContext.wallets[clientName];
        let tokenId = scenarioContext.tokenIdAddress[clientName];
        let destinationAddress = scenarioContext.tokenAddress[clientTo]
        let walletToIdentity = scenarioContext.walletIds[clientTo]
        await wallet.sendTokenToMember(walletToIdentity, destinationAddress, uuid.v4(), [tokenId], new ArrayBuffer(0))
        log(`Token ${tokenName} transfered to ${clientTo}`)
    }
);

When('{string} accepts token',
    async function (clientName) {
        log(`${clientName} accepts`)
        let wallet = scenarioContext.wallets[clientName];
        let transactions = await wallet.listOperations
        let pendings = transactions.filter(tx => {
                let currentState = tx.history.last.state
                return currentState == OperationStatus.IssuePendingRegulation ||
                    currentState == OperationStatus.DealPendingRegulation ||
                    currentState == OperationStatus.BurnPendingRegulation
            }
        )
        pendings.foreEach((async opId => await wallet.acceptToken(opId.operationId)))
        setTimeout(function() {}, 2000);
    }
);

When('{string} sends his {string} token to {string} smart contract - should fail with no regulation',
    async function (clientName, tokenName, smartContractName) {
        log(`${clientName} sends his ${tokenName} token to ${smartContractName}...`)
        let wallet = scenarioContext.wallets[clientName];
        let tokenAddress = scenarioContext.tokenAddress[clientName];
        let destinationAddress = scenarioContext.smartContractAddress[smartContractName];
        try {
            await wallet.sendToken(tokenAddress, destinationAddress);
        } catch (e) {
            assert(e.AT.responseText === "error in simulation: transaction returned with failure: Regulation failed");
        }

    }
);


When('{string} burns his first token in his tokens list',
    async function (clientName) {
        log(`s"${clientName} burns his first token in his list...`)
        let wallet = scenarioContext.wallets[clientName];
        let tokenDeleted = wallet.waiterFor("onWalletEvents", (wallet, events) => {
            log(`Got onWalletEvents: Tokens added: ${events.owner.tokensReceived}, tokens deleted: ${events.owner.tokensBurn}`)
            return true;
        });
        let tokens = await wallet.listTokens;
        await wallet.burnToken([new BurnRequest(tokens[0].id, [], [])]);
        await tokenDeleted;
    }
);

When('{string} burns his first token in his tokens list with extra data:',
    async function (clientName, data) {
        log(`s"${clientName} burns his first token in his list with extra data...`)
        let wallet = scenarioContext.wallets[clientName];
        let tokenDeleted = wallet.waiterFor("onWalletEvents", (wallet, events) => {
            log(`Got onWalletEvents: Tokens added: ${events.owner.tokensReceived}, tokens deleted: ${events.owner.tokensBurn}`)
            return true;
        });
        let hashes = data.hashes();
        let extraFields = hashes.map(hash =>
            hash["value"]
        )
        let tokens = await wallet.listTokens;
        await wallet.burnToken(tokens[0].id, new ArrayBuffer(0), extraFields);
        await tokenDeleted;
    }
);


When('{string} checks his burnt issuer tokens',
    async function (clientName) {
        log(`Checks burnt issued tokens for ${clientName}...`)
        if (typeof clientName === undefined) assert.fail(`Unknown client ${clientName}`);
        let wallet = scenarioContext.wallets[clientName];
        let tokens = await wallet.listBurntIssuedTokens;
        log(`Tokens are: ${tokens}`)

        getOperations(wallet);
        return tokens;
    }
);

When('{string} checks his pending transactions',
    async function (regulatorName) {
        log(`${regulatorName} checks his pending transactions...`)
        if (typeof regulatorName === undefined) assert.fail(`Unknown regulator ${regulatorName}`);
        let wallet = scenarioContext.wallets[regulatorName];
        let transactions = await wallet.listOperations();
        let pendings = transactions.filter(tx => {
                let currentState = tx.history.last.state
                return currentState == OperationStatus.IssuePendingRegulation ||
                    currentState == OperationStatus.DealPendingRegulation ||
                    currentState == OperationStatus.BurnPendingRegulation
            }
        )
        log(`Transactions are ${pendings.join(", ")}`)
    }
);

When('{string} checks {string} smart contract state',
    async function (clientName, smartContractName) {
        log(`Checks smart contract state for ${smartContractName}...`)
        if (typeof clientName === undefined) assert.fail(`Unknown client ${clientName}`);
        if (typeof smartContractName === undefined) assert.fail(`Unknown tokenName ${smartContractName}`);
        let smartContractAddress = scenarioContext.smartContractAddress[smartContractName];

        const publicApi = createPublicApi();

        let state = await publicApi.getSmartContractState(smartContractAddress);
        log(`Smart contract state for ${smartContractName}: ${state}`)
        return state;
    }
);

When('{string} checks {string} smart contract regulation',
    async function (clientName, smartContractName) {
        log(`Checks smart contract regulation for ${smartContractName}...`)
        if (typeof clientName === undefined) assert.fail(`Unknown client ${clientName}`);
        if (typeof smartContractName === undefined) assert.fail(`Unknown tokenName ${smartContractName}`);
        let smartContractAddress = scenarioContext.smartContractAddress[smartContractName];

        const publicApi = createPublicApi();

        let regulation = await publicApi.getSmartContractRegulation(smartContractAddress);
        log(`Smart contract regulation for ${smartContractName}: ${regulation}`)
        return regulation;
    }
);

When('{string} approves {string} smart contract',
    async function (clientName, smartContractName) {
        log(`${clientName} approves ${smartContractName} smart contract...`)
        let wallet = scenarioContext.wallets[clientName];
        let smartContract = scenarioContext.smartContract[smartContractName];
        let smartContractApproved = wallet.waiterFor("onWalletEvents", (wallet, events) => {
            log(`Got on regulationUpdatedSmartContracts: ${events.smartContracts.regulationUpdatedSmartContracts}`)
            return true;
        });
        let result = wallet.approveSmartContract(smartContract.address);
        await smartContractApproved;
        return result
    }
);


When('{string} rejects {string} smart contract',
    async function (clientName, smartContractName) {
        log(`${clientName} rejects ${smartContractName} smart contract...`)
        let wallet = scenarioContext.wallets[clientName];
        let smartContract = scenarioContext.smartContract[smartContractName];
        let result = wallet.rejectSmartContract(smartContract.address, "Not enough information provided");
        return result
    }
);


When('{string} checks smart contract state for {string}',
    async function (clientName, address) {
        log(`Checks smart contract state for ${address}...`)
        if (typeof clientName === undefined) assert.fail(`Unknown client ${clientName}`);
        if (typeof smartContractName === undefined) assert.fail(`Unknown address ${address}`);
        let smartContractAddress = _base64ToArrayBuffer(address);

        const publicApi = createPublicApi();

        let state = await publicApi.getSmartContractState(smartContractAddress);
        log(`Smart contract state for ${address}: ${state}`)
        return state;
    }
);

When('{string} approves {string} endorsement', async function (regulator, client) {
    if (typeof regulator === undefined) assert.fail(`Unknown client ${regulator}`);
    if (typeof client === undefined) assert.fail(`Unknown client ${client}`);
    let regulatorWallet = scenarioContext.wallets[regulator];
    let clientWallet = scenarioContext.wallets[client];
    let walletIdentity = scenarioContext.walletIds[client];

    let approveEndorsementEvent = clientWallet.waiterFor("onWalletEvents", (wallet, events) => {
        log(`Got on endorsed: ${events.member.endorsed}`)
        return true;
    });

    await regulatorWallet.endorseMember(walletIdentity, _base64ToArrayBuffer("certificate"));

    await approveEndorsementEvent;
});

When('{string} rejects {string} endorsement', async function (regulator, client) {
    if (typeof regulator === undefined) assert.fail(`Unknown client ${regulator}`);
    if (typeof client === undefined) assert.fail(`Unknown client ${client}`);
    let regulatorWallet = scenarioContext.wallets[regulator];
    let clientWallet = scenarioContext.wallets[client];
    let walletIdentity = scenarioContext.walletIds[client];
    let rejectEndorsementEvent = clientWallet.waiterFor("onWalletEvents", (wallet, events) => {
        log(`Got on endorsementRejected: ${events.member.endorsementRejected}`)
        return true;
    });
    regulatorWallet.rejectEndorsement(walletIdentity, "No data provided");
    await rejectEndorsementEvent;
});


When('{string} checks his endorsements', async function (clientName) {
    if (typeof clientName === undefined) assert.fail(`Unknown client ${clientName}`);
    let wallet = scenarioContext.wallets[clientName];
    let endorsements = await wallet.listEndorsement;
    log(`Endorsements for ${clientName}: ${endorsements}`)

});

When('{string} checks his endorsements are empty', async function (clientName) {
    if (typeof clientName === undefined) assert.fail(`Unknown client ${clientName}`);
    let wallet = scenarioContext.wallets[clientName];
    let endorsements = await wallet.listEndorsement;
    assert(endorsements.length === 0);
});

When('{string} sees one token in his list', async function (clientName) {
    log('Checks tokens for $clientName...')
    let wallet = scenarioContext.wallets[clientName];
    let tokens = await wallet.listTokens
    log(tokens)
    getOperations(wallet);
    assert(tokens.length === 1)
});

When('{string} changes token {string} with two new tokens with values {string} and {string}',
    async function (clientName, tokenName, newValue1, newValue2) {
        let wallet = scenarioContext.wallets[clientName];
        let tokenId = scenarioContext.tokenTypeIdByName[tokenName]
        let tokensChanged = wallet.waiterFor("onWalletEvents", (wallet, events) => {
            log(`Got onWalletEvents: Tokens added: ${events.owner.tokensReceived}, tokens deleted: ${events.owner.tokensBurn}`)
            return true;
        });

        await wallet.changeToken(
            new TokenChangeRequest(
                tokenId = tokenId,
                amounts = [
                    newValue1,
                    newValue2
                ]
            )
        )

        await tokensChanged;

    });


When('{string} tries to change token {string} with two new tokens with values {string} and {string} but gets errors as the amounts are not even',
    async function (clientName, tokenName, newValue1, newValue2) {
        log(`${clientName} tries to change token $tokenName with two new tokens with values ${newValue1} and ${newValue2} but gets errors as the amounts are not even`)

        let wallet = scenarioContext.wallets[clientName];
        let tokenId = scenarioContext.tokenTypeIdByName[tokenName]

        await wallet.changeToken(
            new TokenChangeRequest(
                tokenId = tokenId,
                amounts = [
                    newValue1,
                    newValue2
                ]
            )
        ).catch(function (err) {
            assert(err.Lorg_scalajs_dom_ext_AjaxException__f_xhr.status === 500)
        })
    });

When('{string} sees no tokens in his list', async function (clientName) {
    log('Checks tokens for $clientName...')
    let wallet = scenarioContext.wallets[clientName];
    let tokens = await wallet.listTokens
    log(tokens)
    assert(tokens.length === 0)
});

When('{string} sees two tokens in his list', async function (clientName) {
    log('Checks tokens for $clientName...')
    let wallet = scenarioContext.wallets[clientName];
    let tokens = await wallet.listTokens
    log(tokens)
    assert(tokens.length === 2)
});

When('{string} sees four tokens in his list', async function (clientName) {
    log('Checks tokens for $clientName...')
    let wallet = scenarioContext.wallets[clientName];
    let tokens = await wallet.listTokens
    log(tokens)
    assert(tokens.length === 4)
});


function _base64ToArrayBuffer(base64) {
    var binary_string = atob(base64);
    var len = binary_string.length;
    var bytes = new Uint8Array(len);
    for (var i = 0; i < len; i++) {
        bytes[i] = binary_string.charCodeAt(i);
    }
    return bytes.buffer;
}

After(
 /*   function () {
        return new Promise((resolve, reject) => {
            log(`Stopping wallets ...`);
            for (const index in scenarioContext.wallets) {
                log(`Stopping wallet ${index} ...`);
            }
            log(`Wallets stopped.`);
            return resolve();
        });
    }*/
);

