package ru.sberbank.blockchain.cnft.wallet.crypto

import ru.sberbank.blockchain.cnft.commons.{Bytes, ROps, asByteArray, asBytes}
import ru.sberbank.blockchain.cnft.model.MemberInformation
import ru.sberbank.blockchain.cnft.wallet.walletmodel.{WalletData, WalletIdentity}
import ru.sberbank.blockchain.common.cryptography._

import scala.language.higherKinds
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

/**
 * @author Alexey Polubelov
 */
@JSExportAll
@JSExportTopLevel("WalletCrypto")
case class WalletCrypto[R[+_]](
    identity: WalletIdentity,
    identityOperations: SignatureOperations[R],
    encryptionOperations: EncryptionOperations[R],
    accessOperations: AccessOperations[R],
    addressOperations: SignatureOperations[R],
    hash: Hash[R],
    randomGenerator: SecureRandomGenerator
)(implicit R: ROps[R]) {

    import ROps.summonHasOps

    def memberInformation(): R[MemberInformation] =
        for {
            signingKeyPublic <- identityOperations.publicKey(identity.signingKey)
            encryptionKeyPublic <- encryptionOperations.publicKey(identity.encryptionKey)
            accessKeyPublic <- accessOperations.publicKey()
        } yield
            new MemberInformation(
                identity.id,
                signingKeyPublic,
                encryptionKeyPublic,
                accessKeyPublic,
                false
            )

    def exportData(): R[Bytes] =
        for {
            identityData <- identityOperations.exportData()
            encryptionData <- encryptionOperations.exportData()
            accessData <- accessOperations.exportData()
            addressesData <- addressOperations.exportData()
        } yield
            asBytes(
                WalletData(
                    identity, identityData, encryptionData, accessData, addressesData
                ).toByteArray
            )
}

@JSExportAll
@JSExportTopLevel("CryptographyContext")
class CryptographyContext[R[+_]](
    identityOpsFactory: SignatureOperationsFactory[R],
    encryptionOpsFactory: EncryptionOperationsFactory[R],
    accessOpsFactory: AccessOperationsFactory[R],
    addressOpsFactory: SignatureOperationsFactory[R],
    hashFactory: HashFactory[R],
    randomGeneratorFactory: SecureRandomGeneratorFactory
)(implicit R: ROps[R]) {

    import ROps.summonHasOps

    val randomGenerator = randomGeneratorFactory.create

    def create(): R[WalletCrypto[R]] =
        for {
            id <- generateId

            addressOperations <- addressOpsFactory.create(id)
            identityOperations <- identityOpsFactory.create(id)
            encryptionOperations <- encryptionOpsFactory.create(id)
            accessOperations <- accessOpsFactory.create(id)
            hash <- hashFactory.create

            signingKey <- identityOperations.requestNewKey()
            encryptionKey <- encryptionOperations.requestNewKey()

        } yield
            WalletCrypto(
                WalletIdentity(
                    id, signingKey, encryptionKey
                ),
                identityOperations,
                encryptionOperations,
                accessOperations,
                addressOperations,
                hash,
                randomGeneratorFactory.create
            )

    def importFrom(data: Bytes): R[WalletCrypto[R]] =
        for {
            wd <- R(WalletData.parseFrom(asByteArray(data)))
            identityOperations <- identityOpsFactory.importFrom(wd.id.id, wd.identityData)
            encryptionOperations <- encryptionOpsFactory.importFrom(wd.id.id, wd.encryptionData)
            accessOperations <- accessOpsFactory.importFrom(wd.id.id, wd.accessData)
            addressOperations <- addressOpsFactory.importFrom(wd.id.id, wd.addressesData)
            hash <- hashFactory.create
        } yield
            WalletCrypto(
                wd.id,
                identityOperations,
                encryptionOperations,
                accessOperations,
                addressOperations,
                hash,
                randomGeneratorFactory.create
            )

    def importFromWithUpdate(
        data: Bytes,
        identityOperationsUpdate: Boolean,
        encryptionOperationsUpdate: Boolean,
        accessOperationsUpdate: Boolean,
        addressOperationsUpdate: Boolean
    ): R[WalletCrypto[R]] =
        for {
            wd <- R(WalletData.parseFrom(asByteArray(data)))
            identityOperations <-
                if (identityOperationsUpdate) {
                    identityOpsFactory.create(wd.id.id)
                } else {
                    identityOpsFactory.importFrom(wd.id.id, wd.identityData)
                }
            encryptionOperations <-
                if (encryptionOperationsUpdate) {
                    encryptionOpsFactory.create(wd.id.id)
                } else {
                    encryptionOpsFactory.importFrom(wd.id.id, wd.encryptionData)
                }
            accessOperations <-
                if (accessOperationsUpdate) {
                    accessOpsFactory.create(wd.id.id)
                } else {
                    accessOpsFactory.importFrom(wd.id.id, wd.accessData)
                }
            addressOperations <-
                if (addressOperationsUpdate) {
                    addressOpsFactory.create(wd.id.id)
                } else {
                    addressOpsFactory.importFrom(wd.id.id, wd.addressesData)
                }
            signingKey <-
                if (identityOperationsUpdate) {
                    identityOperations.requestNewKey()
                } else {
                    R(wd.id.signingKey)
                }
            encryptionKey <-
                if (encryptionOperationsUpdate) {
                    encryptionOperations.requestNewKey()
                } else {
                    R(wd.id.encryptionKey)
                }
            hash <- hashFactory.create
        } yield
            WalletCrypto(
                WalletIdentity(
                    wd.id.id, signingKey, encryptionKey
                ),
                identityOperations,
                encryptionOperations,
                accessOperations,
                addressOperations,
                hash,
                randomGeneratorFactory.create
            )

    private def generateId: R[String] = R {
        // TODO: use Random from Context
        val idBytes = asByteArray(randomGenerator.nextBytes(32))
        idBytes.map { b =>
            java.lang.Integer.toString(b.toInt + 128, 36)
        }.mkString
    }
}