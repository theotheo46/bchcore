package ru.sberbank.blockchain.cnft.wallet.blocks

import ru.sberbank.blockchain.cnft.commons.{Logger, ROps, collectionFromArray}
import ru.sberbank.blockchain.cnft.gate.model.BlockEvents
import ru.sberbank.blockchain.cnft.wallet.spec.MemberEvents
import ru.sberbank.blockchain.cnft.wallet.walletmodel.WalletIdentity

import scala.language.higherKinds

class MemberEventsExtractor[R[+_]](
    id: WalletIdentity
)(implicit
    val R: ROps[R],
    val logger: Logger
) {

    def extract(block: BlockEvents, messages: MessagesAggregation): R[MemberEvents] = {
        val endorsementsApproved = block.approveEndorsements.flatMap { a =>
            val approve = a.event
            if (id.id == approve.endorsement.memberId) {
                Some(approve)
            } else None
        }

        val publicEndorsements = block.publicEndorsements.flatMap { a =>
            val e = a.event
            if (id.id == e.endorsement.memberId) {
                Some(e)
            } else None
        }

        val revokedEndorsements = block.revokedEndorsements.flatMap { a =>
            val e = a.event
            if (id.id == e.endorsement.memberId) {
                Some(e)
            } else None
        }

        R(
            MemberEvents(
                registered = collectionFromArray(block.membersRegistered),
                endorsed = collectionFromArray(endorsementsApproved),
                endorsedPublic = collectionFromArray(publicEndorsements),
                revokedEndorsements = collectionFromArray(revokedEndorsements),
                endorsementRequested = messages.endorsementsRequested,
                endorsementRejected = messages.endorsementsRejected,
                genericMessages = messages.genericMessages
            )
        )
    }
}
