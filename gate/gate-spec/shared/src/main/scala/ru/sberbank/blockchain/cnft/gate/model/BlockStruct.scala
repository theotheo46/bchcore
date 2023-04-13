package ru.sberbank.blockchain.cnft.gate.model

import upickle.default

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
@JSExportTopLevel("BlockJson")
case class BlockJson(
    blockHeader: BlockHeaderJson,
    blockMetaData: BlockMetaDataJson,
    blockData: BlockDataJson,
    fullData: Array[String]
)

object BlockJson {
    implicit val RW: default.ReadWriter[BlockJson] = upickle.default.macroRW
}

@JSExportAll
@JSExportTopLevel("GetProofRequest")
case class GetProofRequest(
    blockNumbersRequest: Array[Long]
)

object GetProofRequest {
    implicit val RW: default.ReadWriter[GetProofRequest] = upickle.default.macroRW
}

@JSExportAll
@JSExportTopLevel("BlocksOrderer")
case class BlocksOrderer(
    block: BlockJson,
)

object BlocksOrderer {
    implicit val RW: default.ReadWriter[BlocksOrderer] = upickle.default.macroRW
}

@JSExportAll
@JSExportTopLevel("BlockDataJson")
case class BlockDataJson(
    data: String,
    transactionsJson: TransactionJson
)

object BlockDataJson {
    implicit val RW: default.ReadWriter[BlockDataJson] = upickle.default.macroRW
}

@JSExportAll
@JSExportTopLevel("TransactionJson")
case class TransactionJson(
    args: Array[String]
)

object TransactionJson {
    implicit val RW: default.ReadWriter[TransactionJson] = upickle.default.macroRW
}

@JSExportAll
@JSExportTopLevel("BlockMetaDataJson")
case class BlockMetaDataJson(
    value: String,
    signature: String,
    signatureHeader: SignatureHeaderJson,
    signatureHeaderB64: String
)

object BlockMetaDataJson {
    implicit val RW: default.ReadWriter[BlockMetaDataJson] = upickle.default.macroRW
}

@JSExportAll
@JSExportTopLevel("SignatureHeaderJson")
case class SignatureHeaderJson(
    creator: CreatorJson
)

object SignatureHeaderJson {
    implicit val RW: default.ReadWriter[SignatureHeaderJson] = upickle.default.macroRW
}

@JSExportAll
@JSExportTopLevel("CreatorJson")
case class CreatorJson(
    mspId: String,
    certHash: String
)

object CreatorJson {
    implicit val RW: default.ReadWriter[CreatorJson] = upickle.default.macroRW
}

@JSExportAll
@JSExportTopLevel("BlockHeaderJson")
case class BlockHeaderJson(
    number: Long,
    dataHashB64: String,
    previousHashB64: String,
    bytesB64: String
)

object BlockHeaderJson {
    implicit val RW: default.ReadWriter[BlockHeaderJson] = upickle.default.macroRW
}

@JSExportAll
@JSExportTopLevel("HeadersCNFT")
case class HeadersCNFT(
    headersCNFT: Array[BlockHeaderJson]
)

object HeadersCNFT {
    implicit val RW: default.ReadWriter[HeadersCNFT] = upickle.default.macroRW
}

@JSExportAll
@JSExportTopLevel("Rules")
case class Rules(
    rules: Array[Rule]
)

object Rules {
    implicit val RW: default.ReadWriter[Rules] = upickle.default.macroRW
}

@JSExportAll
@JSExportTopLevel("Rule")
case class Rule(
    signed_by: Int
)

object Rule {
    implicit val RW: default.ReadWriter[Rule] = upickle.default.macroRW
}

@JSExportAll
@JSExportTopLevel("PolicyJson")
case class PolicyJson(
    n: Int,
    rules: Rules
)


object PolicyJson {
    implicit val RW: default.ReadWriter[PolicyJson] = upickle.default.macroRW
}

@JSExportAll
@JSExportTopLevel("MetaPolicy")
case class MetaPolicy(
    subPolicy: String,
    rule: String
)

object MetaPolicy {
    implicit val RW: default.ReadWriter[MetaPolicy] = upickle.default.macroRW
}

@JSExportAll
@JSExportTopLevel("ProofJson")
case class BlockInfo(
    blocksOrderer: BlocksOrderer,
    headersCNFT: HeadersCNFT,
    policy: ImplicitMetaPolicyAndSignature
)

object BlockInfo {
    implicit val RW: default.ReadWriter[BlockInfo] = upickle.default.macroRW
}

@JSExportAll
@JSExportTopLevel("ImplicitMetaPolicyAndSignature")
case class ImplicitMetaPolicyAndSignature(
    implicitMetaPolicy: MetaPolicy,
    signaturePolicy: PolicyJson
)

object ImplicitMetaPolicyAndSignature {
    implicit val RW: default.ReadWriter[ImplicitMetaPolicyAndSignature] = upickle.default.macroRW
}

//@JSExportAll
//@JSExportTopLevel("ProofArray")
//case class ProofArray(
//    proofArray: Array[ProofJson]
//)
//
//object ProofArray {
//    implicit val RW: default.ReadWriter[ProofArray] = upickle.default.macroRW
//}

@JSExportAll
@JSExportTopLevel("Creator")
case class Creator(
    mspId: String,
    idBytes: String
)

object Creator {
    implicit val RW: default.ReadWriter[Creator] = upickle.default.macroRW
}

//@JSExportAll
//@JSExportTopLevel("Endorsement")
//case class Endorsement(
//    endorser: Creator,
//    signature: String
//)
//
//object Endorsement {
//    implicit val RW: default.ReadWriter[Endorsement] = upickle.default.macroRW
//}

@JSExportAll
@JSExportTopLevel("NsRwset")
case class NsRwset(
    nameSpace: String,
    rwSet: RwSet
)

object NsRwset {
    implicit val RW: default.ReadWriter[NsRwset] = upickle.default.macroRW
}

@JSExportAll
@JSExportTopLevel("RwSet")
case class RwSet(
    reads: Array[Reads]
)

object RwSet {
    implicit val RW: default.ReadWriter[RwSet] = upickle.default.macroRW
}

@JSExportAll
@JSExportTopLevel("Reads")
case class Reads(
    key: String,
    version: String
)

object Reads {
    implicit val RW: default.ReadWriter[Reads] = upickle.default.macroRW
}
