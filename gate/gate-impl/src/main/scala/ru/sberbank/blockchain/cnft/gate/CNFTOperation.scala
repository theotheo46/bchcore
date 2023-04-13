package ru.sberbank.blockchain.cnft.gate

/**
 * @author Andrew Pudovikov
 */
object CNFTOperation {
    val RegisterDataFeed = "registerDataFeed"
    val RegisterTokenType = "registerTokenType"
    val ReserveTokenIDs = "reserveTokenIDs"
    val RequestIssueToken = "issueToken"
    val MakeDeal = "makeDeal"
    val BurnToken = "burnTokens"
    val ChangeToken = "changeToken"
    val MergeTokens = "mergeTokens"
    val PublishMessages = "publishMessages"
    val PutOffers = "putOffers"
    val CloseOffers = "closeOffers"
    val FreezeToken = "freezeToken"
    val RegulatorBurnToken = "regulatorBurnToken"
    val RegulatorTransfer = "regulatorTransfer"
    val RegulatorChangeToken = "regulatorChangeToken"
    val RegisterMember = "registerMember"

    val EndorseMember = "endorseMember"
    val EndorseMemberPublic = "endorseMemberPublic"
    val RevokePublicEndorsement = "revokePublicEndorsement"
    val CreateSmartContract = "createSmartContract"
    val Execute = "execute"
    val SubmitDataFeedValue = "submitDataFeedValue"
    val ApproveSmartContract = "approveSmartContract"
    val RejectSmartContract = "rejectSmartContract"

    val ApproveTransaction = "approveTransaction"
    val RejectTransaction = "rejectTransaction"

    val AcceptToken = "acceptToken"

    val CreateProfile = "createProfile"
    val UpdateProfile = "updateProfile"

    val LinkTokensToProfile = "linkTokensToProfile"
    val UnlinkTokensFromProfile = "unlinkTokensFromProfile"

}
