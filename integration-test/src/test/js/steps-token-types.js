const { strictEqual } = require('assert');
const { After, Given, Then, When } = require('@cucumber/cucumber');
const {
    clientRegistersTokenType,
    thereIsAClient,
    tokenTypesContextMixin,
    walletContextMixin,
} = require('./cnft-common-steps');
const { Ascii, log } = require('./cnft-util');

const { FgGreen: FgBase, FgMagenta: FgVal, Reset } = Ascii;

const context = {};
Object.assign(context, walletContextMixin, tokenTypesContextMixin);

Given(
    '[Token Types] There is a client {string}',
    async function (clientName) {
        await thereIsAClient(context, clientName);
    }
);

When(
    '[Token Types] Client {string} registers token type {string}',
    async function (clientName, tokenTypeName) {
        await clientRegistersTokenType(context, clientName, tokenTypeName);
    }
);

Then(
    '[Token Types] Client {string} sees {int} token types in his list',
    async function (clientName, expectedTokenTypesCount) {
        log(`${FgBase}Client ${FgVal}'${clientName}'${FgBase} sees ${FgVal}${expectedTokenTypesCount}` +
            `${FgBase} token types in his list${Reset}`);

        const wallet = context.getWalletByClientName(clientName);
        const actualTokenTypes = await wallet.listOwnedTokenTypes;

        strictEqual(actualTokenTypes.length, expectedTokenTypesCount);

        log(`${FgBase}  Done: correct!${Reset}\n`);
    }
);

After(function () {
/*    return new Promise((resolve, _reject) => {
        log(`Stopping wallets ...`);
        for (const index in context.wallets) {
            log(`Stopping wallet ${index} ...`);
        }
        log(`Wallets stopped.`);
        return resolve();
    });*/
});
