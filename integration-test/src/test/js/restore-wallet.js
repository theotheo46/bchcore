const { profilesContextMixin, clientCreatesProfile, walletContextMixin } = require('./cnft-common-steps');
const { log } = require('./cnft-util');
const { Given, When, After, setDefaultTimeout } = require('@cucumber/cucumber');
const { fail, strictEqual } = require('assert');
const {
    CNFT,
    CNFTCrypto,
    CreateProfileInfo,
    DNA,
    DescriptionField,
    FieldMeta,
    FieldType,
    Gene,
    GeneID,
    HDPathStore,
    TokenContent,
    TokenTypeMeta,
    WalletIssueTokenRequest,
} = require('wallet-lib');

setDefaultTimeout(10 * 1000);

global.window = {}
window.localStorage = global.localStorage

const elliptic = require('elliptic');
const EC = elliptic.ec
const jsec = new EC('secp256k1')

const crypto = require('crypto').webcrypto;
// export WebCrypto to global scope, so it to be accessible by XXXCryptoOperations
global.crypto = crypto;

global.Buffer = global.Buffer || require('buffer').Buffer;
const XMLHttpRequest = require("xmlhttprequest").XMLHttpRequest;
// export XMLHttpRequest to global scope, so it to be accessible by GateApi:
global.XMLHttpRequest = XMLHttpRequest;
let scenarioContext = {
    "walletSeeds": {},
    "walletIds": {},
    "walletInfo": {},
    "tokenTypeIdByName": {},
};
Object.assign(scenarioContext, walletContextMixin);
Object.assign(scenarioContext, profilesContextMixin);

Given('there is embeded postgres',
    async function () {
        fail(`Non applicable for JS test`);
    }
)

async function awaitWalletEvents(wallet, f) {
    var running = true
    var lastSeenBlock = await wallet.chain.getLatestBlockNumber
    log(`Starting to wait event, block: [${lastSeenBlock}]`);
    while (running) {
        let height = await wallet.chain.getLatestBlockNumber
        while (lastSeenBlock <= height) {
            let events = await wallet.events(lastSeenBlock, false)
            let result = f(wallet, events)
            log(`Processed block [${lastSeenBlock}], found: ${result}`);
            if(result)
                return events
            lastSeenBlock = (parseInt(lastSeenBlock) + 1)
        }
        await new Promise((resolve) => {
            setTimeout(resolve, 1000)
        })
    }
}

Given('Create wallet for {string}',
    async function (clientName) {
        log(`Creating new wallet for ${clientName}`)
        let CNFTGateUrl = "http://localhost:8981"

        let context =
            CNFTCrypto.newContext(
                CNFTCrypto.hdSignatureOperations(crypto, jsec, HDPathStore.inMemory()),
                CNFTCrypto.webCryptoEncryption(crypto),
                CNFTCrypto.webCryptoAccessOperations(crypto, jsec),
                CNFTCrypto.hdSignatureOperations(crypto, jsec, HDPathStore.inMemory()),
                CNFTCrypto.hash(crypto),
                CNFTCrypto.secureRandomGenerator(crypto)
            );
        let walletCrypto = await context.create();

        // save wallet bytes to context:
        let walletBytes = await walletCrypto.exportData();
        scenarioContext.walletSeeds[clientName] = walletBytes;

        //
        log("Connecting to Admin wallet ...")
        let chain = await CNFT.connect(CNFTGateUrl)
        let adminWallet = await chain.connectWallet("http://localhost:8983");
        let member = await walletCrypto.memberInformation();
        log("Registering wallet ...")
        let tx = await adminWallet.registerMember(member);
        log(`Registered wallet [${member.id}] in TX: [${tx.blockNumber} : ${tx.txId}]`)
        //
        await chain
            .newWallet(walletCrypto)
            .then(async wallet => {
                await wallet.getWalletInformation.then(async walletIdentity => {
                    scenarioContext.walletIds[clientName] = walletIdentity.id;
                    scenarioContext.wallets[clientName] = wallet;
                    scenarioContext.walletInfo[clientName] = walletIdentity;
                });
            });
    }
);

When('{string} registered new token type',
    async function (clientName) {
        let tokenTypeName = "T1"
        log(`Registering token type ${tokenTypeName} for ${clientName} ...`);
        let wallet = scenarioContext.wallets[clientName];
        if (typeof wallet === 'undefined') fail(`Unknown client ${clientName}`);
        let typeId = await wallet.createId;

        let result = await wallet.registerTokenType(
            typeId,
            new TokenTypeMeta(
                [
                    new DescriptionField("value", FieldType.Text, tokenTypeName),
                    new DescriptionField("other", FieldType.Text, "О-ГО-ГО!")
                ],
                [new FieldMeta("Value", FieldType.Text)]
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
        scenarioContext.tokenTypeIdByName[clientName] = typeId;
        log(`Registered token type ${tokenTypeName} for ${clientName} (${typeId}).`)
        return result;
    }
);

When('{string} issue new token', { timeout: 200000 }, async function (issuerName) {
    log(`Issuing token for ${issuerName}...`)
    let tokenTypeName = "T1"
    let clientWallet = scenarioContext.wallets[issuerName];
    if (typeof clientWallet === 'undefined') fail(`Unknown issuerName  ${issuerName}`);
    let tokenValue = "100";
    let owner = await clientWallet.createSingleOwnerAddress;
    let tokenType = scenarioContext.tokenTypeIdByName[issuerName];
    let tokenId = await clientWallet.createTokenId(tokenType)
    let myId = await clientWallet.getIdentity;
    let requests = [
        new WalletIssueTokenRequest(
            tokenId,
            owner,
            new TokenContent(
                [tokenValue]
            ),
            myId
        )
    ]
    if (tokenId !== undefined) {
        let waitIssuedTokens = awaitWalletEvents(clientWallet, (wallet, events) => {
            if (
                events.owner.tokensReceived.length > 0 &&
                events.owner.tokensReceived.includes(tokenId)
            ) {
                log(`Tokens Received [${events.owner.tokensReceived.toString()}]`);
                return true;
            }
            return false;
        })

        clientWallet
            .issue(
                requests
            ).catch(function (err) {
                log(err)
        })
        scenarioContext.tokenTypeIdByName["T1"] = tokenId;
        await waitIssuedTokens;
        log(`Issued token of type ${tokenTypeName} for ${issuerName}`);
    }
});

When('{string} create new profile', async function (issuerName) {
    var random_num = new Uint8Array(10); // 2048 = number length in bits
    crypto.getRandomValues(random_num);
    let profileName = random_num.toString(36).substring(7);
    const created = clientCreatesProfile(
        scenarioContext,
        issuerName,
        CreateProfileInfo(
            profileName,
            'test_description',
            'test_avatar',
            'test_background'
        )
    );
    await created;
});

When('Restore wallet for client {string}',
    async function (clientName) {
        log(`Creating new wallet for ${clientName}`)
        let CNFTGateUrl = "http://localhost:8981"

        let context =
            CNFTCrypto.newContext(
                CNFTCrypto.hdSignatureOperations(crypto, jsec, HDPathStore.inMemory()),
                CNFTCrypto.webCryptoEncryption(crypto),
                CNFTCrypto.webCryptoAccessOperations(crypto, jsec),
                CNFTCrypto.hdSignatureOperations(crypto, jsec, HDPathStore.inMemory()),
                CNFTCrypto.hash(crypto),
                CNFTCrypto.secureRandomGenerator(crypto)
            );
        let walletBytes = scenarioContext.walletSeeds[clientName];
        let walletCrypto = await context.importFrom(walletBytes);

        //
        let chain = await CNFT.connect(CNFTGateUrl)
        let wallet = await chain.newWallet(walletCrypto)

        const walletInformation = await wallet.getWalletInformation;

        scenarioContext.walletIds[clientName] = walletInformation.id;
        scenarioContext.wallets[clientName] = wallet;
        scenarioContext.walletInfo[clientName] = walletInformation;

        log(`Restored wallet for client ${clientName}`);
    }
);

When('Check token list in wallet for client {string}', async function (clientName) {
    log(`Checks tokens for ${clientName} ...`)

    function sleep(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }

    await sleep(2000)
    let wallet = scenarioContext.wallets[clientName];
    let tokens = await wallet.listTokens
    log(tokens)
});

When('Check {int} tokens in wallet for client {string}',
    async function (expectedCount, clientName) {
        log(`Checks tokens for ${clientName} ...`);
        let wallet = scenarioContext.wallets[clientName];
        let tokens = await wallet.listTokens;
        strictEqual(tokens.length, expectedCount);
    });

When('Check {int} profiles in wallet for client {string}',
    async function (expectedCount, clientName) {
        log(`Checks profiles for ${clientName} ...`);
        let wallet = scenarioContext.wallets[clientName];
        let profiles = await wallet.listProfiles;
        strictEqual(profiles.length, expectedCount);
    });

When('{string} checks operations list',
    async function (clientName) {
        let wallet = scenarioContext.wallets[clientName];
        let walletIdentity = await wallet.getIdentity;
        log('');
        log(`Operations for ${walletIdentity}`);

        log('_______')
        let operations = await wallet.listOperations;
        operations.forEach(op => {
            log(`id: ${op.operationId}`);
            log(`history:`);
            op.history.forEach(h => {
                log(`opstate: ${h.state}, timestamp: ${h.timestamp}, txId: ${h.txId}`);
            });
        });
    });


/*After(
    function () {
        return new Promise((resolve, _reject) => {
            log(`Stopping wallets ...`);
            for (const index in scenarioContext.wallets) {
                log(`Stopping wallet ${index} ...`);
            }
            log(`Wallets stopped.`);
            return resolve();
        });
    }
);*/
