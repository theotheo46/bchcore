package ru.sberbank.blockchain.cnft.wallet

import ru.sberbank.blockchain.cnft.common.types.{Bytes, BytesOps, Collection, asBytes, collectionFromIterable}
import ru.sberbank.blockchain.cnft.commons.{Base64R, ROps, asByteArray}
import ru.sberbank.blockchain.cnft.gate.service.ChainServiceSpec
import ru.sberbank.blockchain.cnft.model.{OwnerType, Signatures, TokenOwner}
import ru.sberbank.blockchain.cnft.wallet.crypto.WalletCrypto
import ru.sberbank.blockchain.cnft.wallet.walletmodel.WalletIdentity

import java.nio.charset.StandardCharsets
import java.util.Base64
import scala.language.higherKinds

trait WalletCommonOps[R[+_]] {

    import ROps._

    implicit val ops: ROps[R] = R

    protected def R: ROps[R]

    protected def id: WalletIdentity

    protected def crypto: WalletCrypto[R]

    protected def chain: ChainServiceSpec[R]

    def amIAnOwner(owner: TokenOwner): R[Boolean] = {
        owner.ownerType match {
            case OwnerType.Signatures =>
                for {
                    signatures <- R(Signatures.parseFrom(asByteArray(owner.address)))
                    publicKeys <- R(signatures.keys)
                    privateKeys <- crypto.addressOperations.findKeysByPublic(publicKeys)
                } yield privateKeys.nonEmpty

            case OwnerType.SmartContractId =>
                R(false)
        }
    }.recover(_ => R(false))

    def isMyAddress(address: Bytes): R[Boolean] = {
        for {
            owner <- R(TokenOwner.fromBytes(address))
            result <- amIAnOwner(owner)
        } yield result
    }.recover(_ => R(false)) //there is no token with tokenId


    def isMyBurntToken(tokenId: String): R[Boolean] = {
        for {
            token <- chain.getBurntToken(tokenId)
            result <- amIAnOwner(token.tokenOwner)
        } yield result
    }.recover(_ => R(false)) //there is no token with tokenId

    def decryptText(text: String): R[String] = {
        for {
            bytes <- Base64R.decode(text)
            msg <- crypto.encryptionOperations.decrypt(asBytes(bytes), id.encryptionKey)
        } yield msg.toUTF8
    }

    def encryptText(data: String, members: Collection[String]): R[String] =
        for {
            encryptionKeys <- members.toSeq.mapR { memberId =>
                chain.getMember(memberId).map(_.encryptionPublic)
            }
            encrypted <- crypto.encryptionOperations
                .encrypt(asBytes(data.getBytes(StandardCharsets.UTF_8)), collectionFromIterable(encryptionKeys))
        } yield asBytes(Base64.getEncoder.encode(asByteArray(encrypted))).toUTF8


}
