### Methods

#### Smart contract types

1) listSmartContactTemplates: R[Collection[SmartContractTemplate]]
2) getSmartContractTemplate(smartContractTemplateId: Bytes): R[SmartContractTemplate]
3) registerSmartContractTemplate(feeds: Collection[XFeedType], description: Collection[DescriptionField], attributes:
   Collection[XFieldMeta], stateModel: Collection[XFieldMeta], classImplementation: String): R[
   TxResult[SmartContractTemplate]]

#### Smart contracts

1) listSmartContracts: R[Collection[SmartContractInfo]]
2) createSmartContract(address: Bytes, templateAddress: Bytes, dataFeeds: Collection[Bytes], regulators:
   Collection[RegulationCapabilities], attributes: Collection[String]): R[TxResult[SmartContract]]
3) getSmartContractState(address: Bytes): R[SmartContractState]
4) approveSmartContract(address: Bytes): R[TxResult[Unit]]
5) rejectSmartContract(address: Bytes): R[TxResult[Unit]]
6) onSmartContractListChanged(wallet: CNFTWalletSpec, smartContractAdded: Collection[SmartContract]): R[Unit]
7) onSmartContractStateUpdated(wallet: CNFTWalletSpec, smartContractState: Collection[SmartContractState]): R[Unit]
8) onSmartContractRegulationUpdated(wallet: CNFTWalletSpec, smartContractRegulation:
   Collection[SmartContractRegulation]): R[Unit]
9) onSmartContractRegulationApplied(wallet: CNFTWalletSpec, toRegulate: Collection[Bytes]): R[Unit]
10) onSmartContractRejected(wallet: CNFTWalletSpec, rejected: Collection[SCRejectedResult]): R[Unit]

#### Tokens

1) issue(requests: Collection[IssueTokenRequest]): R[TxResult[Unit]]
2) listTokens: Collection[WalletToken]
3) sendToken(dealId: String, tokenIds: Collection[TokenId], to: Bytes): R[TxResult[Unit]]
4) burnToken(burnTokenRequest: BurnRequest): R[TxResult[Unit]]
5) onTokenListChanged(wallet: CNFTWalletSpec, tokensAdded: Collection[TokenId], tokensRemoved: Collection[TokenId],
   tokensFrozen: Collection[FreezeTokenEvent]):
   R[Unit]

#### Addresses

1) createAddress: R[Bytes]
2) createSingleOwnerAddress: R[TokenOwner]
3) createSmartContractAddress: R[Bytes]

#### Feeds

1) registerDataFeed(description: Collection[DescriptionField], fields: Collection[FieldMeta]): R[TxResult[DataFeed]]
2) listDataFeeds: R[Collection[DataFeed]]
3) submitDataFeedValue(values: Collection[DataFeedValue]): R[TxResult[Unit]]
4) getDataFeedValue(address: Bytes): R[DataFeedValue]

#### Identity

1) getIdentity: R[Bytes]

#### Onboarding

1) requestEndorsement(memberId: Bytes): R[Unit]
2) listEndorsements: R[Collection[Endorsement]]
3) endorseMember(memberId: Bytes, certificate: Bytes): R[Unit]
4) rejectEndorsement(memberId: Bytes, reason: String): R[Unit]
5) onEndorsementRequested(wallet: CNFTWalletSpec, requests: Collection[Bytes]): R[Unit]
6) onEndorsementRejected(wallet: CNFTWalletSpec, requests: Collection[IncomingMessage[RejectEndorsementRequest]]):
   R[Unit]
7) onEndorsementApproved(wallet: CNFTWalletSpec, requests: Collection[ApproveEndorsementRequest]): R[Unit]

#### Regulation

1) approveTransaction(transactionId: Bytes): R[Unit]
2) rejectTransaction(transactionId: String, reason: String): R[TxResult[Unit]]
3) onRegulationNeeded(wallet: CNFTWalletSpec, pendingDeals: Collection[PendingDeal], pendingBurns:
   Collection[PendingBurn], pendingIssue: Collection[PendingIssue]): R[Unit]
4) onRegulationApplied(wallet: CNFTWalletSpec, dealsApproved: Collection[PendingDeal], dealsRejected:
   Collection[PendingDeal], burnsApproved: Collection[PendingBurn], burnsRejected: Collection[PendingBurn],
   issuesApproved: Collection[PendingIssue], issuesRejected: Collection[PendingIssue]): R[Unit] //

### Attributes

#### TXResult

| Attribute name      | Type | Description |
| ----------- | ----------- | ----------- |
| blockNumber      | Long       | Block number the transaction |
| txId   | String        | Unique identifier of the transaction |
| value   | T        | Response itself |

#### SmartContractTemplate

| Attribute name      | Type | Description |
| ----------- | ----------- | ----------- |
| address      | Bytes       | Address of the smart contract templates |
| feeds   | Collection[FeedType]      | The list of  data feed types in the template |
| description      | Collection[DescriptionField]       | Description |
| attributes   | Collection[FieldMeta]        | Fields |
| stateModel    | Collection[FieldMeta]       | State model of the smart contract |
| classImplementation   | String        | Implementation of the smart contract |

#### SmartContract

| Attribute name      | Type | Description |
| ----------- | ----------- | ----------- |
| address      | Bytes       | Address of the smart contract |
| templateAddress   | Bytes        | Address of the smart contract template |
| issuerAddress   | Bytes        | Address of the issuer of smart contract |
| dataFeeds   | Collection[Bytes]        | Addresses of the data feeds the contract is linked to with accordance to smart contract template| 
| regulators   | Collection[RegulatorCapabilities]        | Regulator capabilities of the smart contract | 
| attributes   | Collection[String]        | Attributes of the smart contract instance (e.g. hardcap, softcap etc)|
| endorsements   | Collection[Endorsement]        | Attributes of the smart contract instance (e.g. hardcap, softcap etc)|
| burnExtraData   | Collection[FieldMeta]        | Extra data for burn request|

#### SmartContractInfo

| Attribute name      | Type | Description |
| ----------- | ----------- | ----------- |
| theSmartContract      | SmartContract       | Smart contract |
| owned   | Boolean        | Whether Identity is the owner of smart contract |

#### SmartContractState

| Attribute name      | Type | Description |
| ----------- | ----------- | ----------- |
| address      | Bytes       | Address of the smart contract |
| state   | Collection[String]        | Changes of the smart contract | 

#### SmartContractRegulation

| Attribute name      | Type | Description |
| ----------- | ----------- | ----------- |
| address      | Bytes       | Address of the smart contract |
| approves   | Collection[Approve]        | Approves of the smart contract | 

#### SCRejectedResult

| Attribute name      | Type | Description |
| ----------- | ----------- | ----------- |
| address      | Bytes       | Address of the smart contract |
| reason   | String     | Reason of the rejection | 

#### Token

| Attribute name      | Type | Description |
| ----------- | ----------- | ----------- |
| address      | Bytes       | Address of the token in blockchain |
| dna   | Bytes        | Smart contract of the token it belongs to |
| body   | Collection[String]        | Body of the token |
| restrictions   | Collection[Restriction]        | Restrictions of the token |

#### FieldMeta

| Attribute name      | Type | Description |
| ----------- | ----------- | ----------- |
| id      | String       | Field identifier(as referenced in contract)|
| typeId   | String        | Identifier of type e.g. Numeric, Text, Date |
| description      | String       | Short human readable description, to show in applications |

#### DescriptionField

| Attribute name      | Type | Description |
| ----------- | ----------- | ----------- |
| name      | String       | Name of the attribute |
| typeId   | String        | Type of the attribute e.g. Numeric, Text, Date |
| value   | String        |  Value of the description attribute |

#### RegulatorCapabilities

| Attribute name      | Type | Description |
| ----------- | ----------- | ----------- |
| capabilities      | Collection[String]       | Capability type // e.g. freeze, unfreeze etc |
| regulatorId   | Bytes        | Address of the regulator |

#### DataFeed

| Attribute name      | Type | Description |
| ----------- | ----------- | ----------- |
| address      | Bytes       | Address of the data feed |
| feedOwner      | Bytes       | Owner of the data feed |
| description      | Collection[DescriptionField]       | Description |
| fields   | Collection[FieldMeta]        | Fields |

#### DataFeedValue

| Attribute name      | Type | Description |
| ----------- | ----------- | ----------- |
| feedAddress      | Bytes       | Address of the data feed |
| content   | Collection[String]        | Content of the data feed tick |

#### FeedType

| Attribute name      | Type | Description |
| ----------- | ----------- | ----------- |
| feedId      | String       | Feed identifier(as referenced in contract) |
| attributes   | Collection[FieldMeta]        | Attributes of the data feed type

#### SmartContractState

| Attribute name      | Type | Description |
| ----------- | ----------- | ----------- |
| state   | Collection[String]    | Smart contract State   |
| approves      | Collection[Approve]       | List of approves from regulator  |

#### Approve

| Attribute name      | Type | Description |
| ----------- | ----------- | ----------- |
| regulatorId   | Bytes    | Id of the regulator   |
| approved      | Boolean      | Tick whether regulator approved  |
| reason      | String      | Reason for reject regulation  |

#### Endorsement

| Attribute name      | Type | Description |
| ----------- | ----------- | ----------- |
| regulatorId   | Bytes    | Id of the regulator   |
| certificate      | Bytes      | certificate from the regulator  |

#### IssueTokenRequest

| Attribute name      | Type | Description |
| ----------- | ----------- | ----------- |
| tokenId   | TokenId    | token id to issue   |
| owner      | TokenOwner      | new owner for token  |
| content      | TokenContent      | content of the token  |

#### WalletToken

| Attribute name      | Type | Description |
| ----------- | ----------- | ----------- |
| id   | TokenId    | Token id to issue   |
| content      | Collection[String      | Content of the token  |
| restrictions      | Collectuion[Restriction]      | Restricitions, applied to token  |

#### BurnRequest

| Attribute name      | Type | Description |
| ----------- | ----------- | ----------- |
| tokenId   | TokenId    | Token id to burn   |
| extra      | Bytes      | Extra content of the burn request  |
| extraFields      | Collection[String]     | Extra data, which is connected with burnExtraData of the smart contract  |

#### RejectEndorsementRequest

| Attribute name      | Type | Description |
| ----------- | ----------- | ----------- |
| memberId   | Bytes    | Id of the member  |
| regulatorId      | Bytes      | Id of the regulator  |
| signature      | Bytes     | Regulator signature  |
| reason      | String     | Reason of the rejection  |

#### ApproveEndorsementRequest

| Attribute name      | Type | Description |
| ----------- | ----------- | ----------- |
| memberId   | Bytes    | Id of the member  |
| regulatorId      | Bytes      | Id of the regulator  |
| signature      | Bytes     | Regulator signature  |
| certificate      | Bytes     | Certificate from the regulator  |

#### PendingDeal
| Attribute name      | Type | Description |
| ----------- | ----------- | ----------- |
| operationId   | String    | Id of the operation  |
| deal      | Deal      | Deal of the pending deal  |
| approvals      | Collection[RegulatorApproval]     | Approvals needed for deal  |

#### PendingBurn
| Attribute name      | Type | Description |
| ----------- | ----------- | ----------- |
| operationId   | String    | Id of the operation  |
| burnRequest      | RichBurnRequest      | Burn request to regulate  |
| approvals      | Collection[RegulatorApproval]     | Approvals needed for deal  |

#### PendingIssue
| Attribute name      | Type | Description |
| ----------- | ----------- | ----------- |
| operationId   | String    | Id of the operation  |
| issueRequest      | IssueRequest      | Issue request to regulate  |
| approvals      | Collection[RegulatorApproval]     | Approvals needed for deal  |
