const { webcrypto } = require('crypto');
const { ec: EC } = require('elliptic');

const {
    CNFT,
    CNFTCrypto,
    DNA,
    DescriptionField,
    FieldMeta,
    FieldType,
    Gene,
    GeneID,
    HDPathStore,
    TokenTypeMeta,
} = require('wallet-lib');

const Ascii = {
    Reset: '\x1b[0m',

    FgGreen: '\x1b[32m',
    FgYellow: '\x1b[33m',
    FgMagenta: '\x1b[35m',
};

const CNFTGateUrl = 'http://localhost:8981';
const CNFTRemoteWalletUrl = 'http://localhost:8983';
const Chain = CNFT.connect(CNFTGateUrl);

function log(msg) {
    console.log(msg);
}

function toB64(arrayBuffer) {
    return btoa(String.fromCharCode(...new Uint8Array(arrayBuffer)));
}

async function createWalletCrypto() {
    const jsec = new EC('secp256k1');
    return await CNFTCrypto
        .newContext(
            /* identityOpsFactory = */ CNFTCrypto.hdSignatureOperations(webcrypto, jsec, HDPathStore.inMemory()),
            /* encryptionOpsFactory = */ CNFTCrypto.webCryptoEncryption(webcrypto),
            /* accessOpsFactory = */ CNFTCrypto.webCryptoAccessOperations(webcrypto, jsec),
            /* addressOpsFactory = */ CNFTCrypto.hdSignatureOperations(webcrypto, jsec, HDPathStore.inMemory()),
            /* hashFactory = */ CNFTCrypto.hash(webcrypto),
            CNFTCrypto.secureRandomGenerator(webcrypto)
        )
        .create();
}

async function registerMemberHook(crypto) {
    const { FgGreen: FgBase, FgMagenta: FgVal, Reset } = Ascii;
    log(`${FgBase}Connecting to Admin wallet...${Reset}`);

    const adminWallet = await Chain.connectWallet(CNFTRemoteWalletUrl);

    const memberInfo = await crypto.memberInformation();

    try {
        await Chain.getMember(memberInfo.id);
        log(`${FgBase}Member already registered${Reset}`);
    } catch (error) {
        log(`${FgBase}Member is not registered yet (${error})${Reset}`);
        const tx = await adminWallet.registerMember(memberInfo);
        log(`${FgBase}Registered self [${FgVal}${memberInfo.id}${FgBase}] in TX: [${FgVal}${tx.blockNumber}` +
            `${FgBase} : ${FgVal}${tx.txId}${FgBase}]${Reset}`);
    }
}

//

async function registerTokenType(wallet, tokenTypeName, dnaChange = []) {
    const typeId = await wallet.createId;

    await wallet.registerTokenType(
        typeId,
        TokenTypeMeta(
            [ DescriptionField('Value', FieldType.Text, tokenTypeName) ],
            [ FieldMeta('amount', FieldType.Text) ]
        ),
        DNA(
            [ Gene(GeneID.EmissionControlledByIssuer, []) ],
            [ Gene(GeneID.RequireOwnerSignatureForTransfer, []) ],
            [ Gene(GeneID.RequireOwnerSignatureForBurn, []) ],
            dnaChange
        ),
        [],
        []
    );

    return typeId;
}

module.exports = {
    Ascii,
    createWalletCrypto,
    Chain,
    log,
    registerMemberHook,
    registerTokenType,
    toB64,
};
