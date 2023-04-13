const {Given, Then, When} = require('@cucumber/cucumber');

const {fail} = require('assert');

const {webcrypto} = require('crypto');
// export WebCrypto to global scope, so it to be accessible by XXXCryptoOperations
global.crypto = webcrypto;
global.Buffer = global.Buffer || require('buffer').Buffer;

const {log, Chain} = require('./cnft-util');

const {
    thereIsAClient,
    tokenTypesContextMixin,
    tokensContextMixin,
    walletContextMixin,
    clientRegistersTokenType,
    clientCreatesIdForTokenOfType,
    clientIssuesTokenForClientWithValue,
    waitWalletEvent,
} = require('./cnft-common-steps');

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

function assertEquals(expected, actual) {
    if (!equals(expected, actual)) {
        fail(`Actual value [${actual}] doesn't match the expected [${expected}]`);
    }
}

function equals(expected, actual) {
    return JSON.stringify(expected) === JSON.stringify(actual);
}

const XMLHttpRequest = require("xmlhttprequest").XMLHttpRequest;
// export XMLHttpRequest to global scope, so it to be accessible by GateApi:
global.XMLHttpRequest = XMLHttpRequest;

let scenarioContext = {};
Object.assign(scenarioContext, walletContextMixin);
Object.assign(scenarioContext, tokenTypesContextMixin);
Object.assign(scenarioContext, tokensContextMixin);

Given('[GetTokens] There is a client {string}',
    async function (clientName) {
        return await thereIsAClient(scenarioContext, clientName);
    }
);

When('[GetTokens] client {string} registers token type {string}',
    async function (clientName, tokenTypeName) {
        await clientRegistersTokenType(scenarioContext, clientName, tokenTypeName);
        return 'passed';
    });

When('[GetTokens] client {string} issues token {string} with type {string} and value {string}', {timeout: 8 * 5000},
    async function (clientName, tokenName, tokenTypeName, tokenValue) {
        log(`\nIssuing token ${tokenName} with type ${tokenTypeName} by ${clientName} with value ${tokenValue}`);

        const clientWallet = scenarioContext.getWalletByClientName(clientName);
        await clientCreatesIdForTokenOfType(scenarioContext, clientName, tokenName, tokenTypeName)
        await clientIssuesTokenForClientWithValue(
            scenarioContext,
            clientName,
            clientName,
            tokenName,
            `${tokenName} | 1 ${tokenValue}`
        );
        await waitWalletEvent(clientWallet, 'tokensReceived');
        log(`Issued token of type ${tokenTypeName} for ${clientName}`);

        log(`Operations: `);
        (await clientWallet.listOperations).forEach(operation =>
            log(`    ` + operation.timestamp + `\n` +
                `    ` + operation.operationId + `\n` +
                `    ` + operation.operationStatus));

        return 'passed';
    });

Then('[GetTokens] web users see tokens of type {string}',
    async function (tokenTypeName) {
        log(`Getting tokens with type '${tokenTypeName}'`)

        const typeId = scenarioContext.getTokenTypeIdByName(tokenTypeName);

        const tokensOfType = await Chain.getTokensByTypeId(typeId);
        const count = tokensOfType.length;
        log(`count: ${count}`);
        switch (tokenTypeName) {
            case 'Type_1':
                assertEquals(3, count);
                break;
            case 'Type_2':
                assertEquals(4, count);
                break;
            case 'Type_3':
                assertEquals(5, count);
                break;
        }

        return 'passed';
    });
