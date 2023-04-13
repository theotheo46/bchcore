const {Given, When, After} = require('@cucumber/cucumber');

const { fail, strictEqual } = require('assert');
const { CreateProfileInfo, Profile } = require('wallet-lib');

const {webcrypto} = require('crypto');
// export WebCrypto to global scope, so it to be accessible by XXXCryptoOperations
global.crypto = webcrypto;
global.Buffer = global.Buffer || require('buffer').Buffer;

const { createPublicApi, log } = require('./cnft-util');
const {
    clientCreatesIdForTokenOfType,
    clientCreatesProfile,
    clientIssuesTokenForClientWithValue,
    clientRegistersTokenType,
    thereIsAClient,
    profilesContextMixin,
    tokenTypesContextMixin,
    tokensContextMixin,
    walletContextMixin,
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
Object.assign(scenarioContext, profilesContextMixin);

Given('[NFT] there is a client {string}',
    async function (clientName) {
        return await thereIsAClient(scenarioContext, clientName);
    }
);

When('[NFT] client {string} can create a profile {string}',  {timeout: 5 * 5000},
    async function (clientName, profileName) {
        const created = await clientCreatesProfile(scenarioContext, clientName);
        log(created);
        const profileActual = scenarioContext.getProfileByName(profileName);
        const walletIdentity = scenarioContext.getWalletIdentityByClientName(clientName);
        const profileExpected = Profile(
            /* id = */          profileActual.id,
            /* name = */        'test_name',
            /* description = */ 'test_description',
            /* avatar = */      'test_avatar',
            /* background = */  'test_background',
            /* memberId = */    walletIdentity
        );
        assertEquals(profileExpected, profileActual);
        await created;
    }
);

When('[NFT] client {string} can get his profile {string}',
    async function (clientName, profileName) {
        log(`Getting an existing profile by ${clientName}`);
        const publicApi = createPublicApi();
        let expectedProfile = scenarioContext.getProfileByName(profileName);
        let actualProfile = await publicApi.getProfile(expectedProfile.id);
        assertEquals(expectedProfile, actualProfile);
        log(`Profile was successfully obtained by ${clientName}`);
    }
);

When('[NFT] client {string} can not update his profile name {string} and member id',
    async function (clientName, profileName) {
        log(`Updating an existing profile name by ${clientName}`);

        let wallet = scenarioContext.getWalletByClientName(clientName);
        let expectedProfile = scenarioContext.getProfileByName(profileName);
        let updatedProfile = Profile(
            /* id = */          expectedProfile.id,
            /* name = */        'updated_test_name',
            /* description = */ expectedProfile.description,
            /* avatar = */      expectedProfile.avatar,
            /* background = */  expectedProfile.background,
            /* memberId = */    'updated_member_id'
        );
        let tx = await wallet.updateProfile(updatedProfile);
        let actualProfile = tx.value;
        assertEquals(expectedProfile, actualProfile);

        const publicApi = createPublicApi();

        actualProfile = await publicApi.getProfile(expectedProfile.id);
        assertEquals(expectedProfile, actualProfile);
        log(`Profile name was not updated by ${clientName}`);
    }
);

When('[NFT] client {string} can update his profile {string}',
    async function (clientName, profileName) {
        log(`Updating an existing profile by ${clientName}`);
        let wallet = scenarioContext.getWalletByClientName(clientName);
        let isProfileUpdated = wallet.waiterFor(
            'onWalletEvents',
            (wallet, { profiles }) => {
                log('Got onWalletEvents:');
                if (profiles.updated.length) {
                    log(`  - profiles.updated=[${profiles.updated}]`);
                    return true;
                }
                return false;
            }
        );
        let profile = scenarioContext.getProfileByName(profileName);
        let updatedProfile = Profile(
            /* id = */          profile.id,
            /* name = */        profile.name,
            /* description = */ 'updated_test_description',
            /* avatar = */      'updated_test_avatar',
            /* background = */  'updated_test_background',
            /* memberId = */    profile.memberId
        );
        let tx = await wallet.updateProfile(updatedProfile);
        let actualProfile = tx.value;
        assertEquals(updatedProfile, actualProfile);
        await isProfileUpdated;

        const publicApi = createPublicApi();

        actualProfile = await publicApi.getProfile(profile.id);

        assertEquals(updatedProfile, actualProfile);
        scenarioContext.profiles[profileName] = actualProfile;
        log(`Profile was successfully updated by ${clientName}`);
    }
);

When('[NFT] client {string} can list his profile {string} and other',
    async function (clientName, profileName) {
        log(`Listing an existing profiles by ${clientName}`);

        let wallet = scenarioContext.getWalletByClientName(clientName);
        let profiles = await wallet.listProfiles;

        let expectedProfile = scenarioContext.getProfileByName(profileName);
        assertEquals(expectedProfile, profiles[0]);

        const created = clientCreatesProfile(
            scenarioContext,
            clientName,
            CreateProfileInfo(
                'another_test_name',
                'another_test_description',
                'another_test_avatar',
                'another_test_background'
            )
        );
        await created;
        profiles = await wallet.listProfiles;
        strictEqual(profiles.length, 2);
        log(`Profiles were successfully obtained by ${clientName}`);
    }
);

When('[NFT] client {string} can register token type {string}', {timeout: 5 * 5000},
    async function (clientName, tokenTypeName) {
        await clientRegistersTokenType(scenarioContext, clientName, tokenTypeName);
        const tokenTypeName2 = "Second Type Name";
        await clientRegistersTokenType(scenarioContext, clientName, tokenTypeName2);
    });

When('[NFT] client {string} can issue token {string} with type {string} and value {string}', {timeout: 4 * 5000},
    async function (clientName, tokenName, tokenTypeName, tokenValue) {
        log(`Issuing token by ${clientName}...`);
        const clientWallet = scenarioContext.getWalletByClientName(clientName);

        // =============================================================================================================

        await clientCreatesIdForTokenOfType(scenarioContext, clientName, tokenName, tokenTypeName);
        const tokenIssued = clientWallet.waiterFor(
            'onWalletEvents',
            (wallet, { owner }) => {
                log(`Got onWalletEvents:`);
                if (owner.tokensReceived.length) {
                    log(`  - tokensReceived: ${owner.tokensReceived}`);
                    return true;
                }
                return false;
            }
        );
        await clientIssuesTokenForClientWithValue(
            scenarioContext,
            clientName,
            clientName,
            tokenName,
            `${tokenName} | 1 ${tokenValue}`
        );
        await tokenIssued;
        log(`Issued token of type ${tokenTypeName} for ${clientName}`);

        log(`Operations: `);
        (await clientWallet.listOperations).forEach(operation =>
            log(`    ` + operation.timestamp + `\n` +
                `    ` + operation.operationId + `\n` +
                `    ` + operation.operationStatus));

        // ========================================================================

        const tokenTypeName2 = "Second Type Name";
        const tokenName2 = "SecondToken";
        await clientCreatesIdForTokenOfType(scenarioContext, clientName, tokenName2, tokenTypeName2);
        const tokenIssued2 = clientWallet.waiterFor(
            'onWalletEvents',
            (wallet, { owner }) => {
                log(`Got onWalletEvents:`);
                if (owner.tokensReceived.length) {
                    log(`  - tokensReceived: ${owner.tokensReceived}`);
                    return true;
                }
                return false;
            }
        );
        await clientIssuesTokenForClientWithValue(
            scenarioContext,
            clientName,
            clientName,
            tokenName2,
            `${tokenName2} | 2 ${tokenValue}`
        );
        await tokenIssued2;
        log(`Issued token of type ${tokenTypeName2} for ${clientName}`);

        log(`Operations: `);
        (await clientWallet.listOperations).forEach(operation =>
            log(`    ` + operation.timestamp + `\n` +
                `    ` + operation.operationId + `\n` +
                `    ` + operation.operationStatus));

        //========================================================================

        const tokenName3 = "ThirdToken";
        await clientCreatesIdForTokenOfType(scenarioContext, clientName, tokenName3, tokenTypeName2);
        const tokenIssued3 = clientWallet.waiterFor(
            'onWalletEvents',
            (wallet, { owner }) => {
                log(`Got onWalletEvents:`);
                if (owner.tokensReceived.length) {
                    log(`  - tokensReceived: ${owner.tokensReceived}`);
                    return true;
                }
                return false;
            }
        );
        await clientIssuesTokenForClientWithValue(
            scenarioContext,
            clientName,
            clientName,
            tokenName3,
            `${tokenName3} | 3 ${tokenValue}`
        );
        await tokenIssued3;
        log(`Issued token 3 of type ${tokenTypeName2} for ${clientName}`);

        //==============================================================================================================

        const allTokens = await clientWallet.listTokens;

        strictEqual(allTokens.length, 3);
        log(`Tokens of all types in amount of (${allTokens.length}) were successfully obtained: ${allTokens}`);

        const tokenTypeId = scenarioContext.getTokenTypeIdByName(tokenTypeName);
        const tokensOfTypeId = await clientWallet.getTokensByTypeId(tokenTypeId);
        strictEqual(tokensOfTypeId.length, 1);
        log(`Tokens of typeId=(${tokenTypeId}) were successfully obtained: ${tokensOfTypeId}`);
    });

When('[NFT] client {string} can link token {string} to his profile {string}', //{timeout: 2 * 5000},
    async function (clientName, tokenName, profileName) {

        log(`Linking token ${tokenName} to profile ${profileName}`);

        const clientWallet = scenarioContext.getWalletByClientName(clientName);

        const profileId = scenarioContext.getProfileByName(profileName).id;

        log(`profile id: ${profileId}`);

        const tokenId = scenarioContext.getTokenIdByName(tokenName);

        const tokenName2 = "SecondToken";
        const tokenId2 = scenarioContext.getTokenIdByName(tokenName2);

        const tokenName3 = "ThirdToken";
        const tokenId3 = scenarioContext.getTokenIdByName(tokenName3);

        const publicApi = createPublicApi();
        let profileTokens = await publicApi.getProfileTokens(profileId);
        strictEqual(profileTokens.length, 0);

        let tokensLinked = clientWallet.waiterFor(
            'onWalletEvents',
            (wallet, { profiles }) => {
                log('Got onWalletEvents:');
                if (profiles.tokensLinked.length) {
                    log(`  - profiles.tokensLinked: ${profiles.tokensLinked.flatMap(tl => tl.tokenIds)}`);
                    return true;
                }
                return false;
            }
        );

        if (tokenId !== undefined && tokenId2 !== undefined && tokenId3 !== undefined) {
            await clientWallet.linkTokensToProfile(profileId, [ tokenId, tokenId2, tokenId3 ]);
        } else fail("TokenIds not defined");

        await tokensLinked;
        profileTokens = await publicApi.getProfileTokens(profileId);

        strictEqual(profileTokens.length, 3);
    });


When(
    '[NFT] client {string} can link token {string} to his profile {string} and get token profiles',
    { timeout: 15000 },
    async function (clientName, tokenName, profileName) {
        const publicApi = createPublicApi();
        const anotherProfileName = `another_${profileName}`;
        const created = clientCreatesProfile(
            scenarioContext,
            clientName,
            CreateProfileInfo(
                anotherProfileName,
                `${profileName}_description`,
                'another_test_avatar',
                'another_test_background'
            )
        );
        await created;

        const clientWallet = scenarioContext.getWalletByClientName(clientName);
        const profiles = await clientWallet.listProfiles;
        strictEqual(profiles.length, 3);

        const tokenId = scenarioContext.getTokenIdByName(tokenName);
        let tokenProfiles = await publicApi.getTokenProfiles(tokenId);
        strictEqual(tokenProfiles.length, 1);

        await clientWallet.linkTokensToProfile(
            scenarioContext.getProfileByName(anotherProfileName).id,
            [ tokenId ]
        );

        tokenProfiles = await publicApi.getTokenProfiles(tokenId);
        strictEqual(tokenProfiles.length, 2);
    }
);

When('[NFT] client {string} can unlink token {string} from his profile {string}', {timeout: 4 * 5000},
    async function (clientName, tokenName, profileName) {
        const publicApi = await createPublicApi();
        const profileId = scenarioContext.getProfileByName(profileName).id;
        const clientWallet = scenarioContext.getWalletByClientName(clientName);

        let linkedTokens = await publicApi.getProfileTokens(profileId);
        assertEquals(3, linkedTokens.length);

        let tokensUnlinked = clientWallet.waiterFor(
            'onWalletEvents',
            (wallet, { profiles }) => {
                log('Got onWalletEvents:');
                if (profiles.tokensUnlinked.length) {
                    log(`  - profiles.tokensUnlinked: ${profiles.tokensUnlinked.map(p => p.tokenIds)}`);
                    return true;
                }
                return false;
            }
        );

        const tokenId = scenarioContext.getTokenIdByName(tokenName);
        if (tokenId !== undefined) {
            await clientWallet.unlinkTokensFromProfile(profileId, [ tokenId ]);
        } else fail("TokenId is not defined");

        await tokensUnlinked;
        linkedTokens = await publicApi.getProfileTokens(profileId);
        assertEquals(2, linkedTokens.length);

        //======================== Unlinking more tokenIds =================================

        const tokenName2 = "SecondToken";
        const tokenId2 = scenarioContext.getTokenIdByName(tokenName2);

        const tokenName3 = "ThirdToken";
        const tokenId3 = scenarioContext.getTokenIdByName(tokenName3);

        let tokensLinked = clientWallet.waiterFor(
            'onWalletEvents',
            (wallet, { profiles }) => {
                log('Got onWalletEvents:');
                if (profiles.tokensLinked.length) {
                    log(`  - profiles.tokensLinked: ${profiles.tokensLinked.flatMap(tl => tl.tokenIds)}`);
                    return true;
                }
                return false;
            }
        );

        log("Linking token to profile again");

        if (tokenId !== undefined) {
            await clientWallet.linkTokensToProfile(profileId, [ tokenId ]);
        } else fail("TokenId is not defined");

        await tokensLinked;

        linkedTokens = await publicApi.getProfileTokens(profileId);
        strictEqual(linkedTokens.length, 3);

        tokensUnlinked = clientWallet.waiterFor(
            'onWalletEvents',
            (wallet, { profiles }) => {
                log('Got onWalletEvents:');
                if (profiles.tokensUnlinked.length) {
                    log(`  - profiles.tokensUnlinked: ${profiles.tokensUnlinked.map(p => p.tokenIds)}`);
                    return true;
                }
                return false;
            }
        );

        log("Unlinking 3 tokens from profile");

        if (tokenId !== undefined && tokenId2 !== undefined && tokenId3 !== undefined) {
            await clientWallet.unlinkTokensFromProfile(profileId, [ tokenId, tokenId2, tokenId3 ]);
        } else fail("TokenIds not defined");

        await tokensUnlinked;
        const resTokens = await publicApi.getProfileTokens(profileId);
        strictEqual(resTokens.length, 0);

        log("Unlinked 3 tokens from profile");

        const tokenProfiles = await publicApi.getTokenProfiles(tokenId);
        strictEqual(tokenProfiles.length, 1);
    });


When('[NFT] client {string} creates a profile {string} and issues token {string} and client {string} creates a profile {string} and issues token {string}', {timeout: 18 * 5000},
    async function (clientName1, profileName1, tokenName1, clientName2, profileName2, tokenName2) {
        const publicApi = await createPublicApi();

        // ========================== DEFINING WALLETS =========================================================

        const clientWallet1 = scenarioContext.getWalletByClientName(clientName1);
        const clientWallet2 = scenarioContext.getWalletByClientName(clientName2);

        // ======================== CREATING PROFILES. BEGIN =====================================================

        const created1 = await clientCreatesProfile(
            scenarioContext,
            clientName1,
            CreateProfileInfo(
                profileName1,
                'test_description',
                'test_avatar',
                'test_background'
            )
        );
        const profileActual1 = scenarioContext.getProfileByName(profileName1);
        const profileExpected1 = Profile(
            /* id = */          profileActual1.id,
            /* name = */        profileName1,
            /* description = */ 'test_description',
            /* avatar = */      'test_avatar',
            /* background = */  'test_background',
            /* memberId = */    profileActual1.memberId
        );
        assertEquals(profileExpected1, profileActual1);
        await created1;

        //

        const created2 = await clientCreatesProfile(
            scenarioContext,
            clientName2,
            CreateProfileInfo(
                profileName2,
                'test_description',
                'test_avatar',
                'test_background'
            )
        );
        const profileActual2 = scenarioContext.getProfileByName(profileName2);
        const profileExpected2 = Profile(
            /* id = */          profileActual2.id,
            /* name = */        profileName2,
            /* description = */ 'test_description',
            /* avatar = */      'test_avatar',
            /* background = */  'test_background',
            /* memberId = */    profileActual2.memberId
        );
        assertEquals(profileExpected2, profileActual2);
        await created2;

        // ======================== CREATING PROFILES. END =======================================================

        // ======================== ISSUING TOKENS. BEGIN  =======================================================

        const tokenTypeName1 = "Token type 1";
        await clientRegistersTokenType(scenarioContext, clientName1, tokenTypeName1);

        const tokenTypeName2 = "Token type 2";
        await clientRegistersTokenType(scenarioContext, clientName2, tokenTypeName2);

        const tokenValue = 100;

        //

        const tokenIssued1 = clientWallet1.waiterFor(
            'onWalletEvents',
            (wallet, { owner }) => {
                log(`Got onWalletEvents:`);
                if (owner.tokensReceived.length) {
                    log(`  - owner.tokensReceived: ${owner.tokensReceived}`);
                    return true;
                }
                return false;
            }
        );
        await clientCreatesIdForTokenOfType(scenarioContext, clientName1, tokenName1, tokenTypeName1);
        await clientIssuesTokenForClientWithValue(
            scenarioContext,
            clientName1,
            clientName1,
            tokenName1,
            `${tokenName1} | 1 ${tokenValue}`
        );
        await tokenIssued1;
        log(`Issued token 1 of type ${tokenTypeName1} for ${clientName1}`);

        //

        const tokenIssued2 = clientWallet2.waiterFor(
            'onWalletEvents',
            (wallet, { owner }) => {
                log(`Got onWalletEvents:`);
                if (owner.tokensReceived.length) {
                    log(`  - owner.tokensReceived: ${owner.tokensReceived}`);
                    return true;
                }
                return false;
            }
        );
        await clientCreatesIdForTokenOfType(scenarioContext, clientName2, tokenName2, tokenTypeName2);
        await clientIssuesTokenForClientWithValue(
            scenarioContext,
            clientName2,
            clientName2,
            tokenName2,
            `${tokenName2} | 2 ${tokenValue}`
        );
        await tokenIssued2;
        log(`Issued token 2 of type ${tokenTypeName2} for ${clientName2}`);

        //

        const tokenName3 = "token_3";
        const tokenIssued3 = clientWallet1.waiterFor(
            'onWalletEvents',
            (wallet, { owner }) => {
                log(`Got onWalletEvents:`);
                if (owner.tokensReceived.length) {
                    log(`  - owner.tokensReceived: ${owner.tokensReceived}`);
                    return true;
                }
                return false;
            }
        );
        await clientCreatesIdForTokenOfType(scenarioContext, clientName1, tokenName3, tokenTypeName1);
        await clientIssuesTokenForClientWithValue(
            scenarioContext,
            clientName1,
            clientName1,
            tokenName3,
            `${tokenName3} | 3 ${tokenValue}`
        );
        await tokenIssued3;
        log(`Issued token 3 of type ${tokenTypeName2} for ${clientName1}`);

        // ======================== ISSUING TOKENS. END  =======================================================

        let tokensLinked = clientWallet1.waiterFor(
            'onWalletEvents',
            (wallet, { profiles }) => {
                log('Got onWalletEvents:');
                if (profiles.tokensLinked.length) {
                    log(`  - profiles.tokensLinked: ${profiles.tokensLinked.flatMap(pt => pt.tokenIds)}`);
                    return true;
                }
                return false;
            }
        );

        const tokenId1 = scenarioContext.getTokenIdByName(tokenName1);
        const tokenId2 = scenarioContext.getTokenIdByName(tokenName2);
        await clientWallet1.linkTokensToProfile(profileActual1.id, [ tokenId1 ]);
        await tokensLinked;

        const linkedTokens = await publicApi.getProfileTokens(profileActual1.id);
        assertEquals(1, linkedTokens.length);


        // Even token and profile variable indexes belong to clientWallet2, odd ones - to clientWallet1
        const profile3 = await clientWallet1.createProfile(
            CreateProfileInfo(
                "Profile Three",
                "test_description",
                "test_avatar",
                "test_background"
            )
        );

        const profile5 = await clientWallet1.createProfile(
            CreateProfileInfo(
                "Profile Five",
                "test_description",
                "test_avatar",
                "test_background"
            )
        );

        await clientWallet1.linkTokensToProfile(profileActual1.id, [ tokenId1 ]);
        await clientWallet2.linkTokensToProfile(profileActual2.id, [ tokenId2 ]);
        await clientWallet1.linkTokensToProfile(profile3.value.id, [ tokenId1 ]);

        await clientWallet1.linkTokensToProfile(profile5.value.id, [ tokenId1 ]);
        await clientWallet1.linkTokensToProfile(profileActual1.id, [ tokenId1 ]);

        const tokenProfiles = await publicApi.getTokenProfiles(tokenId1);
        assertEquals(3, tokenProfiles.length);

        const profileTokens = await publicApi.getProfileTokens(profileActual1.id);
        assertEquals(1, profileTokens.length);

        await clientWallet1.unlinkTokensFromProfile(profile3.value.id, [ tokenId1 ]);
        const tokenProfiles2 = await publicApi.getTokenProfiles(tokenId1);
        assertEquals(2, tokenProfiles2.length);

        log("NEW TOKEN PROFILES: ");
        log(tokenProfiles2);

        await clientWallet1.unlinkTokensFromProfile(profile5.value.id, [tokenId1]);
        const tokenProfiles3 = await publicApi.getTokenProfiles(tokenId1);
        assertEquals(1, tokenProfiles3.length);

        await clientWallet1.unlinkTokensFromProfile(profileActual1.id, [ tokenId1 ]);
        const tokenProfiles4 = await publicApi.getTokenProfiles(tokenId1);
        assertEquals(0, tokenProfiles4.length);

        return 'passed';
    });

After(
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
);
