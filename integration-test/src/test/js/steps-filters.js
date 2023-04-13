const { strictEqual } = require('assert');
const { After, Given, Then, When } = require('@cucumber/cucumber');
const { Gene, GeneID, TokenTypeFilter } = require('wallet-lib');
const {
    clientCreatesIdForTokenOfType,
    clientIssuesTokenForClientWithValue,
    clientRegistersTokenType,
    thereIsAClient,
    tokenTypesContextMixin,
    tokensContextMixin,
    walletContextMixin,
} = require('./cnft-common-steps');
const { Ascii, createPublicApi, log } = require('./cnft-util');

const { FgGreen: FgBase, FgYellow: FgEvt, FgMagenta: FgVal, Reset } = Ascii;

const context = {};
Object.assign(context, walletContextMixin, tokenTypesContextMixin, tokensContextMixin);

Given(
    '[Filters] There is a client {string}',
    async function (clientName) {
        return await thereIsAClient(context, clientName);
    }
);

When(
    '[Filters] Client {string} registers token type {string} with fungible gene',
    async function (clientName, tokenTypeName) {
        log(`${FgBase}Client ${FgVal}'${clientName}'${FgBase} registers token type ${FgVal}'${tokenTypeName}'` +
            `${FgBase} with fungible gene${Reset}`);
        await clientRegistersTokenType(context, clientName, tokenTypeName, Gene(GeneID.Fungible, []));
    }
);

When(
    '[Filters] Client {string} registers token type {string} without fungible gene',
    async function (clientName, tokenTypeName) {
        log(`${FgBase}Client ${FgVal}'${clientName}'${FgBase} registers token type ${FgVal}'${tokenTypeName}'` +
            `${FgBase} without fungible gene${Reset}`);
        await clientRegistersTokenType(context, clientName, tokenTypeName);
    }
);

When(
    '[Filters] Client {string} creates ID for token {string} of type {string}',
    async (clientName, tokenName, tokenTypeName) =>
        await clientCreatesIdForTokenOfType(context, clientName, tokenName, tokenTypeName)
);

When(
    '[Filters] Client {string} issues token {string} for client {string} with value {string}',
    async function (issuerName, tokenName, clientName, tokenValue) {
        log(`${FgBase}Client ${FgVal}'${issuerName}'${FgBase} issues token ${FgVal}'${tokenName}'${FgBase}` +
            ` for client ${FgVal}'${clientName}'${FgBase} with value ${FgVal}'${tokenValue}'${Reset}`);

        const clientWallet = context.getWalletByClientName(clientName);
        const tokenIssued = clientWallet.waiterFor(
            'onWalletEvents',
            (wallet, { owner }) => {
                log(`${FgEvt}Got ${FgVal}'onWalletEvents'${FgEvt}:${Reset}`);
                if (owner.tokensReceived.length) {
                    log(`${FgEvt}  - tokensReceived: ${FgVal}${owner.tokensReceived}${Reset}`);
                    return true;
                }
                return false;
            }
        );

        await clientIssuesTokenForClientWithValue(context, issuerName, clientName, tokenName, tokenValue);

        await tokenIssued;
        log(`${FgBase}  Done: issued token ${FgVal}'${tokenName}'${FgBase} for ${FgVal}'${clientName}'${Reset}\n`);
    }
);

async function clientHaveTokenTypesFiltered(clientName, tokenTypesCountExpected, changeGeneNegation) {
    const publicApi = createPublicApi();
    const walletIdentity = context.getWalletIdentityByClientName(clientName);
    const tokenTypesFiltered = await publicApi.listTokenTypesFiltered(
        TokenTypeFilter(
            GeneID.Fungible,
            changeGeneNegation
        )
    );
    const myTokenTypes = tokenTypesFiltered.filter(tt => tt.issuerId === walletIdentity);
    strictEqual(myTokenTypes.length, tokenTypesCountExpected);
}

Then(
    '[Filters] Client {string} have {int} token types filtered by fungible',
    async function (clientName, expectedTokenTypesCount) {
        log(`${FgBase}There are ${FgVal}${expectedTokenTypesCount}${FgBase} token types filtered by fungible${Reset}`);
        await clientHaveTokenTypesFiltered(clientName, expectedTokenTypesCount, false);
        log(`${FgBase}  Done: correct!${Reset}\n`);
    }
);

Then(
    '[Filters] Client {string} have {int} token types filtered by non-fungible',
    async function (clientName, expectedTokenTypesCount) {
        log(`${FgBase}There are ${FgVal}${expectedTokenTypesCount}${FgBase} token types filtered by non-fungible${Reset}`);
        await clientHaveTokenTypesFiltered(clientName, expectedTokenTypesCount, true);
        log(`${FgBase}  Done: correct!${Reset}\n`);
    }
);

Then(
    '[Filters] Client {string} sees {int} token\\(s) in his list filtered by fungible',
    async function (clientName, expectedTokensCount) {
        log(`${FgBase}Client ${FgVal}'${clientName}'${FgBase} sees ${FgVal}${expectedTokensCount}${FgBase} ` +
            `token(s) in his list filtered by fungible${Reset}`);

        const wallet = context.getWalletByClientName(clientName);
        const tokensFiltered = await wallet.listTokensFiltered(TokenTypeFilter(GeneID.Fungible, false));
        strictEqual(tokensFiltered.length, expectedTokensCount);

        log(`${FgBase}  Done: correct!${Reset}\n`);
    }
);

Then(
    '[Filters] Client {string} sees {int} token\\(s) in his list filtered by non-fungible',
    async function (clientName, expectedTokensCount) {
        log(`${FgBase}Client ${FgVal}'${clientName}'${FgBase} sees ${FgVal}${expectedTokensCount}${FgBase} ` +
            `token(s) in his list filtered by non-fungible${Reset}`);

        const wallet = context.getWalletByClientName(clientName);
        const tokensFiltered = await wallet.listTokensFiltered(TokenTypeFilter(GeneID.Fungible, true));
        strictEqual(tokensFiltered.length, expectedTokensCount);

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
