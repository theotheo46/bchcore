const { fail } = require('assert');
const { CNFT, CreateProfileInfo, TokenContent, WalletIssueTokenRequest } = require('wallet-lib');
const { Ascii, createWalletCrypto, log, registerMemberHook, registerTokenType } = require('./cnft-util');

const { FgGreen: FgBase, FgMagenta: FgVal, FgYellow: FgEvt, Reset } = Ascii;

const walletContextMixin = {

    wallets: {},
    walletIdentities: {},

    getWalletByClientName(clientName) {
        const wallet = this.wallets[clientName];
        if (typeof wallet === 'undefined') {
            fail(`Unknown client ${clientName}`);
        }
        return wallet;
    },

    getWalletIdentityByClientName(clientName) {
        const walletIdentity = this.walletIdentities[clientName];
        if (typeof walletIdentity === 'undefined') {
            fail(`Unknown client ${clientName}`);
        }
        return walletIdentity;
    },

};

const tokenTypesContextMixin = {

    tokenTypeIds: {},

    getTokenTypeIdByName(tokenTypeName) {
        const typeId = this.tokenTypeIds[tokenTypeName];
        if (typeof typeId === 'undefined') {
            fail(`Unknown token type ${tokenTypeName}`);
        }
        return typeId;
    },

};

const tokensContextMixin = {

    tokenIds: {},

    getTokenIdByName(tokenName) {
        const token = this.tokenIds[tokenName];
        if (typeof token === 'undefined') {
            fail(`Unknown token name ${tokenName}`);
        }
        return token;
    },

};

const profilesContextMixin = {

    profiles: {},

    getProfileByName(profileName) {
        const profile = this.profiles[profileName];
        if (typeof profile === 'undefined') {
            fail(`Unknown profile name ${profileName}`);
        }
        return profile;
    },

};

const CNFTGateUrl = 'http://localhost:8981';
const Chain = CNFT.connect(CNFTGateUrl);

async function awaitWalletEvents(wallet, f) {
    var running = true;
    var lastSeenBlock = await wallet.chain.getLatestBlockNumber;
    while (running) {
        let height = await wallet.chain.getLatestBlockNumber;
        while (lastSeenBlock <= height) {
            let events = await wallet.events(lastSeenBlock, false);
            let result = f(wallet, events);
            if(result)
                return events;
            lastSeenBlock = (parseInt(lastSeenBlock) + 1);
        }
        await new Promise((resolve) => {
            setTimeout(resolve, 1000);
        });
    }
}

function waitWalletEvent(wallet, eventType) {

    awaitWalletEvents(wallet, (wallet, events) => {
        let eventResult;
        switch (eventType) {
            case 'tokensReceived':
                eventResult = events.owner.tokensReceived;
                break;
            case 'tokensBurn':
                eventResult = events.owner.tokensBurn;
                break;
            case 'created':
                eventResult = events.profiles.created;
                break;
            case 'updated':
                eventResult = events.profiles.updated;
                break;
            case 'tokensLinked':
                eventResult = events.profiles.tokensLinked;
                break;
            case 'tokensUnlinked':
                eventResult = events.profiles.tokensUnlinked;
                break;
            default:
                throw(`Error: "${eventType}" is not correct eventType parameter for waitProfileEvent() function`)
        }

        if (eventResult.length > 0) {
            log(`${FgVal}${eventType}=${Reset}[${eventResult}]\n`);
            return true;
        }
        return false;
    });
}

async function thereIsAClient(context, clientName) {
    log(`${FgBase}There is a client ${FgVal}'${clientName}'${Reset}`);

    log(`${FgBase}Creating a wallet configuration for client ${FgVal}'${clientName}'${FgBase}...${Reset}`);
    log(`${FgBase}  Done: created a wallet configuration for client ${FgVal}'${clientName}'${Reset}`);

    log(`${FgBase}Creating a new wallet for client ${FgVal}'${clientName}'${FgBase}...${Reset}`);

    const crypto = await createWalletCrypto();
    const wallet = await Chain.newWallet(crypto);
    await registerMemberHook(crypto);

    context.wallets[clientName] = wallet;
    context.walletIdentities[clientName] = await wallet.getIdentity;

    log(`${FgBase}  Done: created a new wallet for client ${FgVal}'${clientName}'${Reset}\n`);
}

async function clientRegistersTokenType(context, clientName, tokenTypeName, gene) {
    log(`${FgBase}Client ${FgVal}'${clientName}'${FgBase} registers token type ${FgVal}'${tokenTypeName}'${Reset}`);
    context.tokenTypeIds[tokenTypeName] = await registerTokenType(
        context.getWalletByClientName(clientName),
        tokenTypeName,
        typeof gene !== 'undefined' ? [ gene ] : []
    );
    log(`${FgBase}  Done: typeId=${FgVal}'${context.getTokenTypeIdByName(tokenTypeName)}'${Reset}\n`);
}

async function clientCreatesIdForTokenOfType(context, clientName, tokenName, tokenTypeName) {
    log(`${FgBase}Client ${FgVal}'${clientName}'${FgBase} creates ID for token ${FgVal}'${tokenName}'` +
        `${FgBase} of type ${FgVal}'${tokenTypeName}'${Reset}`);

    const wallet = context.getWalletByClientName(clientName);
    const tokenTypeId = context.getTokenTypeIdByName(tokenTypeName);
    try {
        const tokenId = await wallet.createTokenId(tokenTypeId);
        context.tokenIds[tokenName] = tokenId;
        log(`${FgBase}  Done: tokenId=${FgVal}'${tokenId}'${Reset}\n`);
    } catch (error) {
        log(error);
        throw error;
    }
}

async function clientIssuesTokenForClientWithValue(context, issuerName, clientName, tokenName, tokenValue) {
    const issuerWallet = context.getWalletByClientName(issuerName);
    const clientWallet = context.getWalletByClientName(clientName);
    const tokenId = context.getTokenIdByName(tokenName);

    const owner = await clientWallet.createSingleOwnerAddress;
    const to = await clientWallet.getIdentity;
    const body = TokenContent([ tokenValue ]);
    const request = WalletIssueTokenRequest(tokenId, owner, body, to);
    await issuerWallet.issue([ request ]).catch(function (error){
        log(error);
        throw (error);
    });
}

async function clientCreatesProfile(
    context,
    clientName,
    createProfileInfo = CreateProfileInfo(
        'test_name',
        'test_description',
        'test_avatar',
        'test_background'
    )
) {
    log(`${FgBase}Client ${FgVal}'${clientName}'${FgBase} creates profile ${FgVal}'${createProfileInfo.name}'${Reset}`);
    const wallet = await context.getWalletByClientName(clientName);

    const waitProfilesCreated = awaitWalletEvents(wallet, (wallet, events) => {
        if (events.profiles.created.length > 0) {
            log(`Profiles Created=[${events.profiles.created}]`);
            return true;
        }
        return false;
    })

    const tx = await wallet.createProfile(createProfileInfo);

    context.profiles[createProfileInfo.name] = tx.value;
    log(`${FgBase}  Done: profile.id=${FgVal}'${tx.value.id}'${Reset}\n`);

    return waitProfilesCreated;
}

module.exports = {
    clientCreatesIdForTokenOfType,
    clientCreatesProfile,
    clientIssuesTokenForClientWithValue,
    clientRegistersTokenType,
    thereIsAClient,
    awaitWalletEvents,
    waitWalletEvent,
    profilesContextMixin,
    tokenTypesContextMixin,
    tokensContextMixin,
    walletContextMixin,
};
