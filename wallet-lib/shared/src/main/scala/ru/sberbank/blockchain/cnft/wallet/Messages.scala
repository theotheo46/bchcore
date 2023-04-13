package ru.sberbank.blockchain.cnft.wallet

import ru.sberbank.blockchain.cnft.common.types.{Bytes, BytesOps, Collection}
import ru.sberbank.blockchain.cnft.commons.ROps._
import ru.sberbank.blockchain.cnft.commons.asByteArray
import ru.sberbank.blockchain.cnft.gate.model.TxResult
import ru.sberbank.blockchain.cnft.model.{GenericMessage, MessageRequest, OperationStatus, OwnerType, TokenOwner, TokenRequest, TransferProposal}

import scala.language.higherKinds

/**
 * @author Alexey Polubelov
 */
class Messages[R[+_]](wallet: CNFTWalletInternal[R]) {

    import wallet._

    def proposeToken(to: String, tokenType: String, tokenContent: Collection[String], extraData: Bytes): R[String] =
        for {
            _ <- if (tokenType.nonEmpty) chain.getTokenType(tokenType) else R(())
            endorsements <- listEndorsements
            operationId = generateId
            proposal =
                TransferProposal(
                    operationId = operationId,
                    timestamp = generateTimestamp,
                    from = wallet.id.id,
                    tokenType = tokenType,
                    content = tokenContent,
                    extraData = extraData,
                    endorsements = endorsements
                )
            _ <- publishPlatformMessage(to, proposal)
        } yield operationId

    def acceptTransferProposal(operationId: String, extraData: Bytes): R[Bytes] =
        for {
            maybeOp <- operations.getOperation(operationId)
            operation <- R.fromOption(maybeOp, s"Operation not found $operationId")
            currentState <- R.fromOption(operation.history.lastOption, s"No history for operation $operationId")
            _ <- R.expect(currentState.state == OperationStatus.TransferProposed, s"Operation [$operationId] is not a transfer proposal")
            data <- wallet.getOperationDetails(currentState)
            proposal <- R(data.asInstanceOf[TransferProposal])
            address <- createAddress
            address <-
                _requestToken(
                    operation.operationId, generateTimestamp,
                    proposal.from, address,
                    proposal.tokenType, proposal.content, extraData
                )
        } yield address

    def requestToken(from: String, tokenType: String, tokenContent: Collection[String], extraData: Bytes): R[Bytes] = {
        for {
            address <- createAddress
            response <- _requestToken(generateId, generateTimestamp, from, address, tokenType, tokenContent, extraData)
        } yield response
    }

    def requestToken(from: String, address: Bytes, tokenType: String, tokenContent: Collection[String], extraData: Bytes): R[Bytes] =
        _requestToken(generateId, generateTimestamp, from, address, tokenType, tokenContent, extraData)

    def sendGenericMessage(to: String, systemId: Int, messageType: Int, messageData: Bytes): R[TxResult[Unit]] = {
        for {
            _ <- R.expect(systemId > 1000, s"reserved system id space")
            genericMessage =
                GenericMessage(
                    data = messageData,
                    systemId = systemId,
                    messageType = messageType
                )
            result <- publishGenericMessage(to, genericMessage)
        } yield result
    }

    private def _requestToken(
        operationId: String, timestamp: String,
        from: String, address: Bytes,
        tokenType: String, tokenContent: Collection[String], extraData: Bytes
    ): R[Bytes] =
        for {
            tokenOwner <- R(TokenOwner.parseFrom(asByteArray(address)))
            endorsements <- listEndorsements
            request =
                TokenRequest(
                    operationId,
                    timestamp,
                    from = wallet.id.id,
                    tokenType = tokenType,
                    content = tokenContent,
                    address = tokenOwner.toBytes,
                    extraData = extraData,
                    endorsements = endorsements
                )
            _ <- publishPlatformMessage(from, request)
        } yield address

    def acceptTokenRequest(operationId: String, tokenIds: Collection[String], extraData: Bytes): R[TxResult[Unit]] =
        for {
            maybeOp <- operations.getOperation(operationId)
            operation <- R.fromOption(maybeOp, s"Operation not found $operationId")
            currentState <- R.fromOption(operation.history.lastOption, s"No history for operation $operationId")
            _ <- R.expect(currentState.state == OperationStatus.TokenRequested, s"Operation [$operationId] is not a token request")
            data <- wallet.getOperationDetails(currentState)
            request <- R(data.asInstanceOf[TokenRequest])
            newOwnerAddress <- R(TokenOwner.parseFrom(asByteArray(request.address)))
            _ <- R.expect(newOwnerAddress.ownerType == OwnerType.Signatures, s"Not a valid address: ${request.address.toB64}")
            r <-
                wallet.sendToken(
                    request.operationId, generateTimestamp,
                    request.from, request.endorsements,
                    newOwnerAddress, generateId, tokenIds, extraData
                )
        } yield r

    //def requestIssue(tokenType: Bytes, content: Collection[String], extraData: Bytes): R[String] = ???
    //        for {
    //            tokenId <- reserveId.map(_.value.tokenId)
    //            messageBody = IssueRequest(
    //                token = TokenBody(tokenId = tokenId, tokenType = tokenType, content = content),
    //                extraData = extraData
    //            )
    //            issuerId <- findIssuerByTokenType(tokenType).map(_.id)
    //            _ <- publishMessage(issuerId, messageBody)
    //        } yield tokenId

    def listMessages: R[Collection[MessageRequest]] = chain.listMessages(id.id)


}
