package ru.sberbank.blockchain.common.cryptography

import ru.sberbank.blockchain.common.cryptography.Cryptography.KeyIdentifier

import scala.language.higherKinds

/**
 * @author Vladimir Sidorov
 */

trait HDPathStore[R[+_]] {

    def rootId: String

    def saveKeyPath(keyPath: KeyIdentifier): R[Unit]

    def getCurrentKeyPath: R[KeyIdentifier]

}

trait HDPathStoreFactory[R[+_]] {
    def newStoreFor(identity: String): HDPathStore[R]
}