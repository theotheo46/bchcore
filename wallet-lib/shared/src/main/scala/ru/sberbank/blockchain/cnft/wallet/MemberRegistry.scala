package ru.sberbank.blockchain.cnft.wallet

import ru.sberbank.blockchain.cnft.common.types.{Bytes, Collection, collectionFromIterable}
import ru.sberbank.blockchain.cnft.commons.ROps._
import ru.sberbank.blockchain.cnft.gate.model.TxResult
import ru.sberbank.blockchain.cnft.model.{Endorsement, EndorsementRequest, MemberInformation, MemberSignature, PublicEndorsement, RegisterMemberRequest, RejectEndorsementRequest, SignedEndorsement, SignedPublicEndorsement, SignedRejectEndorsementRequest, UpdateMemberInformationRequest}

import scala.language.higherKinds

/**
 * @author Alexey Polubelov
 */
class MemberRegistry[R[+_]](wallet: CNFTWalletInternal[R]) {

    import wallet._


    def registerMember(member: MemberInformation): R[TxResult[String]] =
        for {
            myId <- myWalletIdentity
            _ <- crypto.accessOperations.isPublicValid(member.accessPublic)
            currentBlock <- chain.getLatestBlockNumber
            signature <- crypto.identityOperations.createSignature(myId.signingKey, member.toBytes)
            result <-
                chainTx.registerMember(
                    RegisterMemberRequest(member, myId.id, signature, currentBlock)
                )
        } yield result

    def updateMemberInfo(update: MemberInformation): R[TxResult[Unit]] =
        for {
            myId <- myWalletIdentity
            _ <- crypto.accessOperations.isPublicValid(update.accessPublic)
            _ <- chain.getMember(update.id)
            signature <- crypto.identityOperations.createSignature(myId.signingKey, update.toBytes)
            result <- chainTx.updateMember(
                UpdateMemberInformationRequest(
                    update,
                    MemberSignature(
                        myId.id,
                        signature
                    )
                )
            )
        } yield result

    def listEndorsements: R[Collection[SignedEndorsement]] =
        for {
            myId <- myWalletIdentity
            endorsements <-
                chain
                    .listEndorsements(myId.id)

            decrypted <- endorsements.toSeq.mapR { se =>
                crypto.encryptionOperations
                    .decrypt(se.endorsement.data, myId.encryptionKey)
                    .map { data =>
                        se.withEndorsement(
                            se.endorsement.withData(data)
                        )
                    }
            }.map(collectionFromIterable)
        } yield decrypted

    def requestEndorsement(regulatorId: String, data: Bytes): R[TxResult[Unit]] =
        publishPlatformMessage(
            regulatorId,
            EndorsementRequest(
                data = data
            )
        )

    def endorseMember(memberId: String, certificate: Bytes): R[TxResult[Unit]] =
        for {
            myId <- myWalletIdentity
            memberEncKey <- chain.getMember(memberId).map(_.encryptionPublic)
            encryptedData <- crypto.encryptionOperations.encrypt(certificate, Collection(memberEncKey))
            endorsement =
                Endorsement(
                    regulatorId = myId.id,
                    memberId = memberId,
                    data = encryptedData
                )
            endorsementSignature <- crypto.identityOperations.createSignature(myId.signingKey, endorsement.toBytes)
            result <- chainTx.endorseMember(
                SignedEndorsement(
                    endorsement = endorsement,
                    signature = endorsementSignature
                )
            )
        } yield result

    def endorseMemberPublic(memberId: String, kindId: String, data: Bytes): R[TxResult[Unit]] =
        for {
            myId <- myWalletIdentity
            endorsement =
                PublicEndorsement(
                    endorserId = myId.id,
                    memberId = memberId,
                    kindId = kindId,
                    data = data
                )
            endorsementSignature <- crypto.identityOperations.createSignature(myId.signingKey, endorsement.toBytes)
            result <- chainTx.endorseMemberPublic(
                SignedPublicEndorsement(
                    endorsement = endorsement,
                    signature = endorsementSignature
                )
            )
        } yield result

    def revokePublicEndorsement(memberId: String, kindId: String): R[TxResult[Unit]] =
        for {
            myId <- myWalletIdentity
            endorsement =
                PublicEndorsement(
                    endorserId = myId.id,
                    memberId = memberId,
                    kindId = kindId,
                    data = Bytes.empty
                )
            endorsementSignature <- crypto.identityOperations.createSignature(myId.signingKey, endorsement.toBytes)
            result <- chainTx.revokePublicEndorsement(
                SignedPublicEndorsement(
                    endorsement = endorsement,
                    signature = endorsementSignature
                )
            )
        } yield result


    def rejectEndorsement(memberId: String, reason: String): R[TxResult[Unit]] =
        for {
            myId <- myWalletIdentity
            request = RejectEndorsementRequest(
                memberId = memberId,
                regulatorId = myId.id,
                reason = reason
            )
            signature <- crypto.identityOperations.createSignature(myId.signingKey, request.toBytes)
            signedRequest = SignedRejectEndorsementRequest(
                request = request,
                signature = signature
            )
            result <- publishPlatformMessage(memberId, signedRequest)
        } yield result
}
