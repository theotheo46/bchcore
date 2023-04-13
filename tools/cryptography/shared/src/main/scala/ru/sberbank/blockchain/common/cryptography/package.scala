package ru.sberbank.blockchain.common

import ru.sberbank.blockchain.common.cryptography.Cryptography.KeyIdentifier

/**
 * @author Vladimir Sidorov
 */

package object cryptography {
    type HDPath = (Int, Int)

    object HDPath {
        def apply(input: (Int, Int)): HDPath = input

        def getIncrementFromKeyIdentifier(keyIdentifier: KeyIdentifier): KeyIdentifier = {
            val hdPath = getIncrement(fromKeyIdentifier(keyIdentifier))
            s"${hdPath._1}/${hdPath._2}"
        }

        def getIncrement(p: HDPath): HDPath = {
            if (p._2 + 1 == Integer.MAX_VALUE) {
                (p._1 + 1, 0)
            } else {
                (p._1, p._2 + 1)
            }
        }

        def fromKeyIdentifier(keyIdentifier: KeyIdentifier): HDPath = {
            val splitedKeyId = keyIdentifier.split("/")
            (splitedKeyId(splitedKeyId.length - 2).toInt, splitedKeyId(splitedKeyId.length - 1).toInt)
        }

        def comparePaths(a: HDPath, b: HDPath): HDPath = {
            if (a._1 == b._1) {
                if (a._2 > b._2) {
                    a
                } else {
                    b
                }
            } else if (a._1 > b._1) {
                a
            } else {
                b
            }

        }
    }
}
